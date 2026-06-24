import { useMemo, useState, useEffect } from 'react';
import { Seismograph } from '../components/Seismograph';
import { Panel, Stat, RiskBadge, Button, EmptyState } from '../components/ui';
import { useToast } from '../components/Toast';
import { useData } from '../components/DataContext';
import { useAuth } from '../auth/AuthContext';
import { useRegion } from '../components/RegionContext';
import { generateSignal, peakOf, approxMagnitude, DEFAULT_SPEC } from '../lib/signal';
import { sourceMatches, defaultSource } from '../lib/region';
import { api } from '../api/endpoints';
import { ApiError } from '../api/client';
import { fmtMag, fmtNum, fmtAmp, fmtDateTime, fmtCoord } from '../lib/format';
import { RISK } from '../lib/risk';
import type { DetectionResponse, EarthquakeResponse } from '../api/types';
import { Zap, ShieldCheck } from 'lucide-react';

const STEPS = [
  'Listeden gerçek bir deprem seç — kayıtlı olaylardan biri.',
  '“Sinyali analiz et”e bas — o olayın istasyonda nasıl göründüğü çözümlenir.',
  'Sistemin tahminini olayın gerçek büyüklüğüyle karşılaştır.',
];

// A real seismogram's peak amplitude grows ~10^M (magnitude is logarithmic), and the
// detector inverts that as M = log10(peak) + 3. So for the system's estimate to actually
// track the real magnitude, the synthetic packet's peak must be ≈ 10^(mag-3). The
// generator's S-wave peak is ≈ eventStrength × 3.4, hence the divisor. Amplitudes now
// span orders of magnitude across events — exactly as real seismic amplitudes do.
function strengthFor(mag: number): number {
  return Math.pow(10, mag - 3) / 3.4;
}

export function DetectionView() {
  const toast = useToast();
  const { earthquakes, refresh } = useData();
  const { isAdmin } = useAuth();
  const { region: rawRegion } = useRegion();
  const region = rawRegion ?? 'TR';

  // Offer the strongest recent events of THIS region first.
  const options = useMemo(
    () =>
      earthquakes
        .filter((e) => sourceMatches(region, defaultSource(region), e.source))
        .sort((a, b) => b.magnitude - a.magnitude)
        .slice(0, 80),
    [earthquakes, region],
  );

  const [eventId, setEventId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<DetectionResponse | null>(null);

  // Default to the strongest event once data is available; reset if region changes it away.
  useEffect(() => {
    if (options.length === 0) {
      setEventId(null);
      return;
    }
    if (eventId == null || !options.some((o) => o.id === eventId)) {
      setEventId(options[0].id);
      setResult(null);
    }
  }, [options, eventId]);

  const selected: EarthquakeResponse | undefined = options.find((e) => e.id === eventId) ?? earthquakes.find((e) => e.id === eventId);

  const amplitudes = useMemo(() => {
    if (!selected) return generateSignal({ ...DEFAULT_SPEC, eventStrength: 0 });
    return generateSignal({ ...DEFAULT_SPEC, eventStrength: strengthFor(selected.magnitude), seed: selected.id });
  }, [selected]);
  const peak = useMemo(() => peakOf(amplitudes), [amplitudes]);

  async function analyze() {
    if (!selected) return;
    setBusy(true);
    try {
      const res = await api.analyze({
        stationId: `EV-${selected.id}`,
        latitude: selected.latitude,
        longitude: selected.longitude,
        sampleRateHz: DEFAULT_SPEC.sampleRateHz,
        startTime: new Date().toISOString(),
        amplitudes,
      });
      setResult(res);
      if (res.detected) {
        toast.success(`Algılandı — tahmin M${fmtMag(res.estimatedMagnitude)} · gerçek M${fmtMag(selected.magnitude)}`);
        await refresh();
      } else {
        toast.info('Eşik aşılmadı — yaklaşık değer gösteriliyor.');
      }
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Analiz başarısız.');
    } finally {
      setBusy(false);
    }
  }

  // When the detector formally triggers, use its estimate; otherwise fall back to an
  // approximate reading from the peak amplitude instead of a hard "0".
  const estimate = result
    ? result.detected && result.estimatedMagnitude != null
      ? result.estimatedMagnitude
      : approxMagnitude(result.peakAmplitude)
    : null;
  const isApprox = result != null && !result.detected;

  const diff = estimate != null && selected ? estimate - selected.magnitude : null;

  return (
    <div className="view detection">
      <Panel
        eyebrow={selected ? `olay #${selected.id} · ${selected.source}` : 'olay seçilmedi'}
        title="Gerçek olayı oynat"
        className="detection__trace-panel"
        actions={
          isAdmin ? (
            <Button onClick={analyze} loading={busy} disabled={!selected}>
              <Zap size={15} /> Sinyali analiz et
            </Button>
          ) : (
            <span className="lock-hint"><ShieldCheck size={14} /> Analiz için yönetici girişi</span>
          )
        }
      >
        <div className="tutorial">
          <div className="tutorial__title">Nasıl çalışır?</div>
          <ol className="tutorial__steps">
            {STEPS.map((s, i) => <li key={i}><span className="tutorial__num">{i + 1}</span>{s}</li>)}
          </ol>
        </div>

        {options.length === 0 ? (
          <EmptyState icon={<Zap size={26} />} title="Önce olay gerekli" hint="İzleme ekranından “Akışları içe aktar” ile deprem verisi yükleyin, sonra buradan birini oynatın." />
        ) : (
          <>
            <div className="event-pick">
              <label className="field" style={{ flex: 1 }}>
                <span className="field__label">Deprem seç (en güçlüden)</span>
                <select className="filter-select" value={eventId ?? ''} onChange={(e) => { setEventId(Number(e.target.value)); setResult(null); }}>
                  {options.map((e) => (
                    <option key={e.id} value={e.id}>
                      M{fmtMag(e.magnitude)} · {fmtDateTime(e.occurredAt)} · {e.source}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <Seismograph amplitudes={amplitudes} height={210} sweep={busy} />

            {selected && (
              <div className="event-meta-row">
                <span>{fmtCoord(selected.latitude, selected.longitude)}</span>
                <span>{selected.depthKm.toFixed(1)} km</span>
                <span>gerçek M{fmtMag(selected.magnitude)}</span>
                <RiskBadge level={selected.riskLevel} size="sm" />
              </div>
            )}
          </>
        )}
      </Panel>

      <Panel eyebrow="sonuç" title="Algılama ve karşılaştırma" className="detection__result-panel">
        {!result ? (
          <EmptyState icon={<Zap size={26} />} title="Sonuç bekleniyor" hint="Bir deprem seçip “Sinyali analiz et”e basın. Sistem sinyali çözer, büyüklüğü tahmin eder ve gerçek değerle karşılaştırır." />
        ) : (
          <div className="result">
            <div className={`result__verdict ${result.detected ? 'is-hit' : 'is-miss'}`}>
              {result.detected ? 'DEPREM ALGILANDI' : 'EŞİK ALTI — YAKLAŞIK DEĞER'}
            </div>
            <div className="result__grid">
              <Stat
                label={isApprox ? 'Sistem tahmini (yaklaşık)' : 'Sistem tahmini'}
                value={`${isApprox ? '≈ ' : ''}M${fmtMag(estimate)}`}
              />
              <Stat label="Gerçek büyüklük" value={`M${fmtMag(selected?.magnitude)}`} accent={RISK_HINT(selected)} />
              <Stat label="Fark" value={diff == null ? '—' : `${diff >= 0 ? '+' : ''}${fmtNum(diff, 1)}`} />
              <Stat label="Sinyal/gürültü" value={fmtNum(result.staLtaRatio)} />
            </div>
            {isApprox && (
              <p className="result__hint">
                STA/LTA oranı tetik eşiğinin altında kaldı, bu yüzden resmî bir algılama üretilmedi.
                Yukarıdaki değer tepe genlikten türetilen kaba bir tahmindir.
              </p>
            )}
            <div className="result__foot">
              <div>
                <span className="result__foot-label">Tepe genlik</span>
                <span className="mono-tag">{fmtAmp(result.peakAmplitude)} (yerel {fmtAmp(peak)})</span>
              </div>
              <RiskBadge level={result.riskLevel} />
            </div>
          </div>
        )}
      </Panel>
    </div>
  );
}

// Tiny helper so the "real magnitude" stat picks up the event's risk color.
function RISK_HINT(e?: EarthquakeResponse): string | undefined {
  return e?.riskLevel ? RISK[e.riskLevel].color : undefined;
}
