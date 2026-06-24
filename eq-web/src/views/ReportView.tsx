import { useEffect, useMemo, useState } from 'react';
import { Panel, Stat, Button, RiskBadge, EmptyState, Spinner } from '../components/ui';
import { useData } from '../components/DataContext';
import { useRegion } from '../components/RegionContext';
import { useToast } from '../components/Toast';
import { RISK, RISK_ORDER } from '../lib/risk';
import { CONTINENTS, MIN_MAGS, continentOf, type ContinentKey } from '../lib/geo';
import { REGION_SOURCES, sourceMatches, regionUsesContinents, defaultSource } from '../lib/region';
import { fmtMag, fmtDateTime, fmtCoord } from '../lib/format';
import type { EarthquakeResponse, RiskLevel } from '../api/types';
import { FileText, Download } from 'lucide-react';

type Range = '1d' | '1w' | '1m' | '1y' | 'all';
const RANGES: { key: Range; label: string; ms: number | null }[] = [
  { key: '1d', label: 'Günlük', ms: 24 * 3600e3 },
  { key: '1w', label: 'Haftalık', ms: 7 * 24 * 3600e3 },
  { key: '1m', label: 'Aylık', ms: 30 * 24 * 3600e3 },
  { key: '1y', label: 'Yıllık', ms: 365 * 24 * 3600e3 },
  { key: 'all', label: 'Tümü', ms: null },
];

const DEPTH_BANDS = [
  { label: 'Sığ (<10 km)', test: (d: number) => d < 10 },
  { label: 'Orta (10–30 km)', test: (d: number) => d >= 10 && d < 30 },
  { label: 'Derin (30–70 km)', test: (d: number) => d >= 30 && d < 70 },
  { label: 'Çok derin (≥70 km)', test: (d: number) => d >= 70 },
];

export function ReportView() {
  const { earthquakes, loading } = useData();
  const { region: rawRegion } = useRegion();
  const region = rawRegion ?? 'TR';
  const toast = useToast();
  const [range, setRange] = useState<Range>('all');
  const [minMag, setMinMag] = useState(0);
  const [continent, setContinent] = useState<ContinentKey>('ALL');
  const [source, setSource] = useState(defaultSource(region));
  const [format, setFormat] = useState<'text' | 'markdown'>('text');

  const sources = REGION_SOURCES[region];
  const showContinent = regionUsesContinents(region);

  useEffect(() => {
    setSource(defaultSource(region));
    setContinent('ALL');
  }, [region]);

  const rangeLabel = RANGES.find((r) => r.key === range)!.label;

  // Everything is computed from the selected window + magnitude + region + source filters.
  const events = useMemo(() => {
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

  const stats = useMemo(() => {
    const n = events.length;
    const byRisk: Record<string, number> = {};
    const bySourceMap = new Map<string, number>();
    let maxMag = 0, sumMag = 0, sumDepth = 0;
    let earliest = '', latest = '';
    let strongest: EarthquakeResponse | null = null;
    for (const e of events) {
      maxMag = Math.max(maxMag, e.magnitude);
      sumMag += e.magnitude;
      sumDepth += e.depthKm;
      if (e.riskLevel) byRisk[e.riskLevel] = (byRisk[e.riskLevel] ?? 0) + 1;
      bySourceMap.set(e.source, (bySourceMap.get(e.source) ?? 0) + 1);
      if (!earliest || e.occurredAt < earliest) earliest = e.occurredAt;
      if (!latest || e.occurredAt > latest) latest = e.occurredAt;
      if (!strongest || e.magnitude > strongest.magnitude) strongest = e;
    }
    const depthDist = DEPTH_BANDS.map((b) => ({ label: b.label, count: events.filter((e) => b.test(e.depthKm)).length }));
    const bySource = [...bySourceMap.entries()].sort((a, b) => b[1] - a[1]);
    return {
      total: n,
      maxMag,
      avgMag: n ? sumMag / n : 0,
      avgDepth: n ? sumDepth / n : 0,
      byRisk,
      bySource,
      depthDist,
      earliest: earliest || null,
      latest: latest || null,
      strongest,
    };
  }, [events]);

  function buildReport(fmt: 'text' | 'markdown'): string {
    const md = fmt === 'markdown';
    const h1 = (t: string) => (md ? `# ${t}` : t.toUpperCase());
    const h2 = (t: string) => (md ? `\n## ${t}` : `\n${t}`);
    const li = (t: string) => (md ? `- ${t}` : `  • ${t}`);
    const lines: string[] = [];
    lines.push(h1(`Sismik Rapor — ${rangeLabel}`));
    lines.push(`Oluşturulma: ${fmtDateTime(new Date().toISOString())}`);
    lines.push(h2('Özet'));
    lines.push(li(`Toplam olay: ${stats.total}`));
    lines.push(li(`En yüksek büyüklük: M${fmtMag(stats.maxMag)}`));
    lines.push(li(`Ortalama büyüklük: M${fmtMag(stats.avgMag)}`));
    lines.push(li(`Ortalama derinlik: ${fmtMag(stats.avgDepth)} km`));
    lines.push(li(`Aralık: ${fmtDateTime(stats.earliest)} – ${fmtDateTime(stats.latest)}`));
    lines.push(h2('Risk dağılımı'));
    for (const lvl of RISK_ORDER) lines.push(li(`${RISK[lvl].label}: ${stats.byRisk[lvl] ?? 0}`));
    lines.push(h2('Kaynağa göre'));
    if (stats.bySource.length === 0) lines.push(li('veri yok'));
    for (const [src, n] of stats.bySource) lines.push(li(`${src}: ${n}`));
    lines.push(h2('Derinliğe göre'));
    for (const d of stats.depthDist) lines.push(li(`${d.label}: ${d.count}`));
    if (stats.strongest) {
      const s = stats.strongest;
      lines.push(h2('En güçlü olay'));
      lines.push(li(`M${fmtMag(s.magnitude)} — ${fmtCoord(s.latitude, s.longitude)}`));
      lines.push(li(`${s.depthKm.toFixed(1)} km · ${s.source} · ${fmtDateTime(s.occurredAt)}`));
    }
    return lines.join('\n');
  }

  const rendered = useMemo(() => buildReport(format), [format, stats, rangeLabel]); // eslint-disable-line react-hooks/exhaustive-deps

  function downloadReport() {
    const ext = format === 'markdown' ? 'md' : 'txt';
    const blob = new Blob([rendered], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `sismik-rapor-${range}.${ext}`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    toast.success(`Rapor indirildi (${ext.toUpperCase()})`);
  }

  const breakdownTotal = RISK_ORDER.reduce((s, l) => s + (stats.byRisk[l] ?? 0), 0);
  const maxSource = stats.bySource.length ? Math.max(...stats.bySource.map((s) => s[1])) : 0;
  const maxDepth = Math.max(1, ...stats.depthDist.map((d) => d.count));

  return (
    <div className="view report">
      <Panel eyebrow="özet" title={`Sismik rapor — ${rangeLabel}`} className="report__summary-panel">
        <div className="filter-group" style={{ marginBottom: 18 }}>
          <div className="range-toggle">
            {RANGES.map((r) => (
              <button key={r.key} className={range === r.key ? 'is-active' : ''} onClick={() => setRange(r.key)}>
                {r.label}
              </button>
            ))}
          </div>
          <div className="range-toggle">
            {MIN_MAGS.map((m) => (
              <button key={m.value} className={minMag === m.value ? 'is-active' : ''} onClick={() => setMinMag(m.value)}>
                {m.label}
              </button>
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
              {CONTINENTS.map((c) => (
                <option key={c.key} value={c.key}>{c.label}</option>
              ))}
            </select>
          )}
        </div>

        {loading ? (
          <Spinner label="Rapor yükleniyor…" />
        ) : stats.total === 0 ? (
          <EmptyState title="Bu aralıkta olay yok" hint="Daha geniş bir dönem seçin veya İzleme'den akış içe aktarın." />
        ) : (
          <>
            <div className="report__stats">
              <Stat label="Toplam olay" value={stats.total} />
              <Stat label="En yüksek M" value={fmtMag(stats.maxMag)} accent={RISK.CRITICAL.color} />
              <Stat label="Ort. büyüklük" value={fmtMag(stats.avgMag)} />
              <Stat label="Ort. derinlik" value={fmtMag(stats.avgDepth)} unit="km" />
            </div>

            <div className="breakdown">
              <div className="breakdown__label">Risk dağılımı</div>
              <div className="breakdown__bar">
                {RISK_ORDER.map((lvl) => {
                  const v = stats.byRisk[lvl] ?? 0;
                  const pct = breakdownTotal > 0 ? (v / breakdownTotal) * 100 : 0;
                  if (pct === 0) return null;
                  return <span key={lvl} className="breakdown__seg" style={{ width: `${pct}%`, background: RISK[lvl].color }} title={`${RISK[lvl].label}: ${v}`} />;
                })}
              </div>
              <div className="breakdown__keys">
                {RISK_ORDER.map((lvl) => (
                  <span key={lvl} className="legend-item">
                    <span className="legend-dot" style={{ background: RISK[lvl].color }} />
                    {RISK[lvl].label}
                    <strong>{stats.byRisk[lvl] ?? 0}</strong>
                  </span>
                ))}
              </div>
            </div>

            {stats.strongest && (
              <div className="strongest">
                <div className="strongest__mag" style={{ color: RISK[(stats.strongest.riskLevel ?? 'LOW') as RiskLevel].color }}>
                  M{fmtMag(stats.strongest.magnitude)}
                </div>
                <div className="strongest__body">
                  <div className="strongest__title">En güçlü olay <RiskBadge level={stats.strongest.riskLevel} size="sm" /></div>
                  <div className="strongest__meta">{fmtCoord(stats.strongest.latitude, stats.strongest.longitude)} · {stats.strongest.depthKm.toFixed(1)} km</div>
                  <div className="strongest__meta">{stats.strongest.source} · {fmtDateTime(stats.strongest.occurredAt)}</div>
                </div>
              </div>
            )}

            <div className="dist-grid">
              <div className="dist">
                <div className="dist__label">Kaynağa göre</div>
                {stats.bySource.map(([src, n]) => (
                  <div key={src} className="dist__row">
                    <span className="dist__name">{src}</span>
                    <span className="dist__track"><span className="dist__fill" style={{ width: `${(n / maxSource) * 100}%` }} /></span>
                    <span className="dist__count">{n}</span>
                  </div>
                ))}
              </div>
              <div className="dist">
                <div className="dist__label">Derinliğe göre</div>
                {stats.depthDist.map((d) => (
                  <div key={d.label} className="dist__row">
                    <span className="dist__name">{d.label}</span>
                    <span className="dist__track"><span className="dist__fill dist__fill--alt" style={{ width: `${(d.count / maxDepth) * 100}%` }} /></span>
                    <span className="dist__count">{d.count}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="report__window">
              <span>İlk: {fmtDateTime(stats.earliest)}</span>
              <span>Son: {fmtDateTime(stats.latest)}</span>
            </div>
          </>
        )}
      </Panel>

      <Panel
        eyebrow="dışa aktar"
        title="Biçimli rapor"
        className="report__render-panel"
        actions={
          <div className="row-actions">
            <div className="seg-toggle">
              <button className={format === 'text' ? 'is-active' : ''} onClick={() => setFormat('text')}>Metin</button>
              <button className={format === 'markdown' ? 'is-active' : ''} onClick={() => setFormat('markdown')}>Markdown</button>
            </div>
            <Button variant="ghost" onClick={downloadReport} disabled={stats.total === 0}>
              <Download size={15} /> İndir
            </Button>
          </div>
        }
      >
        {stats.total === 0 ? (
          <EmptyState icon={<FileText size={24} />} title="İçerik yok" hint="Seçili aralıkta olay bulunmuyor." />
        ) : (
          <pre className="rendered">{rendered}</pre>
        )}
      </Panel>
    </div>
  );
}
