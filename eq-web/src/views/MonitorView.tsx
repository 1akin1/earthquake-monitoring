import { useEffect, useMemo, useState } from 'react';
import { useData } from '../components/DataContext';
import { useAuth } from '../auth/AuthContext';
import { useRegion } from '../components/RegionContext';
import { useToast } from '../components/Toast';
import { SeismicMap } from '../components/SeismicMap';
import { EventTable } from '../components/EventTable';
import { Panel, Stat, Button, EmptyState, Spinner } from '../components/ui';
import { fmtMag } from '../lib/format';
import { RISK, RISK_ORDER } from '../lib/risk';
import { CONTINENTS, MIN_MAGS, continentOf, type ContinentKey } from '../lib/geo';
import { REGION_SOURCES, sourceMatches, regionUsesContinents, defaultSource } from '../lib/region';
import { api } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { EarthquakeResponse, RiskLevel } from '../api/types';
import { DownloadCloud, RefreshCw, Waves, Map as MapIcon, Table as TableIcon } from 'lucide-react';

type Range = '1d' | '1w' | '1m' | '1y' | 'all';
const RANGES: { key: Range; label: string; ms: number | null }[] = [
  { key: '1d', label: 'Son 1 gün', ms: 24 * 3600e3 },
  { key: '1w', label: 'Son 1 hafta', ms: 7 * 24 * 3600e3 },
  { key: '1m', label: 'Son 1 ay', ms: 30 * 24 * 3600e3 },
  { key: '1y', label: 'Son 1 yıl', ms: 365 * 24 * 3600e3 },
  { key: 'all', label: 'Tümü', ms: null },
];

export function MonitorView() {
  const { earthquakes, stats, loading, refresh } = useData();
  const { canAnalyze, canDelete } = useAuth();
  const { region: rawRegion } = useRegion();
  const region = rawRegion ?? 'TR';
  const toast = useToast();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [range, setRange] = useState<Range>('all');
  const [minMag, setMinMag] = useState(0);
  const [continent, setContinent] = useState<ContinentKey>('ALL');
  const [source, setSource] = useState(defaultSource(region));
  const [view, setView] = useState<'map' | 'table'>('map');
  const [importing, setImporting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const sources = REGION_SOURCES[region];
  const showContinent = regionUsesContinents(region);

  // When the region changes, reset source + continent to sensible defaults.
  useEffect(() => {
    setSource(defaultSource(region));
    setContinent('ALL');
  }, [region]);

  const filtered = useMemo(() => {
    const ms = RANGES.find((r) => r.key === range)?.ms ?? null;
    const cutoff = ms === null ? null : Date.now() - ms;
    return earthquakes.filter((e) => {
      if (!sourceMatches(region, source, e.source)) return false;
      if (cutoff !== null && new Date(e.occurredAt).getTime() < cutoff) return false;
      if (e.magnitude < minMag) return false;
      if (showContinent && continent !== 'ALL' && continentOf(e.latitude, e.longitude) !== continent) return false;
      return true;
    });
  }, [earthquakes, region, source, range, minMag, continent, showContinent]);

  const metrics = useMemo(() => {
    const n = filtered.length;
    if (n === 0) return { total: 0, maxMag: 0, avgMag: 0, avgDepth: 0, byRisk: {} as Record<string, number> };
    let maxMag = 0, sumMag = 0, sumDepth = 0;
    const byRisk: Record<string, number> = {};
    for (const e of filtered) {
      maxMag = Math.max(maxMag, e.magnitude);
      sumMag += e.magnitude;
      sumDepth += e.depthKm;
      if (e.riskLevel) byRisk[e.riskLevel] = (byRisk[e.riskLevel] ?? 0) + 1;
    }
    return { total: n, maxMag, avgMag: sumMag / n, avgDepth: sumDepth / n, byRisk };
  }, [filtered]);

  async function importFeeds() {
    setImporting(true);
    try {
      const res = await api.importFeeds();
      const sourcesMsg = Object.entries(res.bySource).map(([k, v]) => `${k}: ${v}`).join(', ');
      toast.success(`${res.imported} olay içe aktarıldı${sourcesMsg ? ` (${sourcesMsg})` : ''}`);
      await refresh();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'İçe aktarma başarısız.');
    } finally {
      setImporting(false);
    }
  }

  async function remove(eq: EarthquakeResponse) {
    if (!confirm(`M${fmtMag(eq.magnitude)} olayı (#${eq.id}) silinsin mi?`)) return;
    setDeletingId(eq.id);
    try {
      await api.deleteEarthquake(eq.id);
      toast.success(`Olay #${eq.id} silindi`);
      if (selectedId === eq.id) setSelectedId(null);
      await refresh();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Silme başarısız.');
    } finally {
      setDeletingId(null);
    }
  }

  const actions = (
    <div className="row-actions">
      <div className="seg-toggle">
        <button className={view === 'map' ? 'is-active' : ''} onClick={() => setView('map')}><MapIcon size={14} /> Harita</button>
        <button className={view === 'table' ? 'is-active' : ''} onClick={() => setView('table')}><TableIcon size={14} /> Tablo</button>
      </div>
      {canAnalyze && (
        <Button variant="ghost" onClick={importFeeds} loading={importing}>
          <DownloadCloud size={15} /> Akışları içe aktar
        </Button>
      )}
      <Button variant="ghost" onClick={() => refresh()}>
        <RefreshCw size={15} /> Yenile
      </Button>
    </div>
  );

  return (
    <div className="view monitor">
      <div className="metric-strip">
        <Stat label="Olay (seçili)" value={metrics.total} />
        <Stat label="En yüksek M" value={fmtMag(metrics.maxMag)} accent={RISK.CRITICAL.color} />
        <Stat label="Ort. büyüklük" value={fmtMag(metrics.avgMag)} />
        <Stat label="Ort. derinlik" value={fmtMag(metrics.avgDepth)} unit="km" />
        {stats && <Stat label="İşlenen (akış)" value={stats.totalEvents} />}
        <div className="metric-strip__legend">
          {RISK_ORDER.map((lvl: RiskLevel) => (
            <span key={lvl} className="legend-item">
              <span className="legend-dot" style={{ background: RISK[lvl].color }} />
              {RISK[lvl].label}
              <strong>{metrics.byRisk[lvl] ?? 0}</strong>
            </span>
          ))}
        </div>
      </div>

      <div className="range-bar">
        <div className="filter-group">
          <div className="range-toggle">
            {RANGES.map((r) => (
              <button key={r.key} className={range === r.key ? 'is-active' : ''} onClick={() => setRange(r.key)}>{r.label}</button>
            ))}
          </div>
          <div className="range-toggle">
            {MIN_MAGS.map((m) => (
              <button key={m.value} className={minMag === m.value ? 'is-active' : ''} onClick={() => setMinMag(m.value)}>{m.label}</button>
            ))}
          </div>
          {sources.length > 1 && (
            <div className="range-toggle">
              {sources.map((s) => (
                <button key={s.key} className={source === s.key ? 'is-active' : ''} onClick={() => setSource(s.key)}>{s.label}</button>
              ))}
            </div>
          )}
          {showContinent && (
            <select className="filter-select" value={continent} onChange={(e) => setContinent(e.target.value as ContinentKey)}>
              {CONTINENTS.map((c) => <option key={c.key} value={c.key}>{c.label}</option>)}
            </select>
          )}
        </div>
        <span className="range-bar__count">{filtered.length} olay · otomatik yenileme açık</span>
      </div>

      {view === 'table' ? (
        <Panel eyebrow={`${filtered.length} kayıt`} title="Olay tablosu" className="monitor__table-panel" actions={actions}>
          {loading ? (
            <Spinner label="Olaylar yükleniyor…" />
          ) : filtered.length === 0 ? (
            <EmptyState icon={<Waves size={28} />} title="Bu filtrede olay yok" hint="Filtreleri genişletin veya akış içe aktarın." />
          ) : (
            <EventTable
              earthquakes={filtered}
              selectedId={selectedId}
              onSelect={(e) => setSelectedId(e.id)}
              canDelete={canDelete}
              onDelete={remove}
              deletingId={deletingId}
            />
          )}
        </Panel>
      ) : (
        <Panel eyebrow="canlı görünüm" title="Olay haritası" className="monitor__map-panel" actions={actions}>
          {filtered.length === 0 && !loading ? (
            <EmptyState
              icon={<Waves size={28} />}
              title={earthquakes.length === 0 ? 'Henüz olay yok' : 'Bu filtrede olay yok'}
              hint={earthquakes.length === 0
                ? (canAnalyze ? 'Akışları içe aktararak başlayın.' : 'Bir yönetici akış içe aktardığında olaylar görünür.')
                : 'Filtreleri genişletin.'}
            />
          ) : (
            <SeismicMap earthquakes={filtered} selectedId={selectedId} onSelect={(e) => setSelectedId(e.id)} />
          )}
        </Panel>
      )}
    </div>
  );
}
