import { useMemo, useState } from 'react';
import { Panel, RiskBadge } from '../components/ui';
import { RISK } from '../lib/risk';
import type { RiskLevel } from '../api/types';
import { Info, ListChecks, GitCompare, Activity, Gauge, History, Waves } from 'lucide-react';

// Earthquake-only assessment. Move the magnitude slider and the panel turns into a
// genuinely informative briefing: expected effects, nearby historical references,
// energy/frequency context, expected shaking, and priority actions. Below a small
// floor the panel shows a neutral "no meaningful event" state instead of warning.

const MIN = 0;
const MAX = 10;
const STEP = 0.1;
const DEFAULT = 5;
const FLOOR = 1.0; // below this, there is effectively nothing to assess

interface Band { upTo: number | null; level: RiskLevel }
const BANDS: Band[] = [
  { upTo: 4, level: 'LOW' },
  { upTo: 6, level: 'MEDIUM' },
  { upTo: 8, level: 'HIGH' },
  { upTo: null, level: 'CRITICAL' },
];

const BAND_LADDER: { level: RiskLevel; range: string }[] = [
  { level: 'LOW', range: '0–4' },
  { level: 'MEDIUM', range: '4–6' },
  { level: 'HIGH', range: '6–8' },
  { level: 'CRITICAL', range: '8+' },
];

const INFO: Record<RiskLevel, { impacts: string[]; comparison: string; actions: string[] }> = {
  LOW: {
    impacts: ['Çoğu kişi hissetmez ya da hafif hisseder.', 'Yapısal hasar beklenmez.'],
    comparison: 'M3–4: günlük olağan sismik aktivite.',
    actions: ['Olayı kayda geçir.', 'İzlemeyi sürdür.'],
  },
  MEDIUM: {
    impacts: ['İç mekânda belirgin hissedilir.', 'Zayıf/eski yapılarda hafif hasar olabilir.', 'Onlarca km yarıçapta hissedilir.'],
    comparison: 'M4–6: sık görülür, yıkıcı değil ama dikkat ister.',
    actions: ['Eski/yığma yapıları kontrol et.', 'Yerel yetkilileri bilgilendir.', 'Artçıları izle.'],
  },
  HIGH: {
    impacts: ['Geniş alanda güçlü sarsıntı.', 'Yaygın yapısal hasar.', 'Yüzlerce km’de hissedilir.', 'Çok sayıda artçı beklenir.'],
    comparison: 'M6–8: büyük, yıkıcı deprem sınıfı.',
    actions: ['Bölgesel alarmları tetikle.', 'Arama-kurtarmayı sevk et.', 'Hastane/barınma kapasitesini hazırla.'],
  },
  CRITICAL: {
    impacts: ['Felaket düzeyinde yıkım.', 'Geniş bölgede altyapı çöküşü.', 'Uzun süreli artçı dizisi.'],
    comparison: 'M8+: nesilde birkaç kez görülen büyük felaket.',
    actions: ['Ulusal acil durum ilan et.', 'Çok kurumlu müdahale başlat.', 'Uluslararası yardım talep et.'],
  },
};

// Representative historical earthquakes (Mw, approximate). Turkish events prioritised.
const REF_QUAKES: { mag: number; name: string; year: number; tr: boolean; note?: string }[] = [
  { mag: 9.5, name: 'Valdivia, Şili', year: 1960, tr: false, note: 'kayıtlı en büyük' },
  { mag: 9.1, name: 'Tōhoku, Japonya', year: 2011, tr: false },
  { mag: 9.1, name: 'Sumatra–Andaman', year: 2004, tr: false },
  { mag: 7.8, name: 'Erzincan', year: 1939, tr: true },
  { mag: 7.8, name: 'Kahramanmaraş (Pazarcık)', year: 2023, tr: true },
  { mag: 7.5, name: 'Kahramanmaraş (Elbistan)', year: 2023, tr: true },
  { mag: 7.4, name: 'Gölcük / İzmit', year: 1999, tr: true },
  { mag: 7.2, name: 'Düzce', year: 1999, tr: true },
  { mag: 7.1, name: 'Van', year: 2011, tr: true },
  { mag: 7.0, name: 'Sisam / İzmir', year: 2020, tr: true },
  { mag: 6.8, name: 'Sivrice / Elazığ', year: 2020, tr: true },
  { mag: 6.4, name: 'Bingöl', year: 2003, tr: true },
];

function bandLevel(value: number): RiskLevel {
  for (const b of BANDS) if (b.upTo === null || value < b.upTo) return b.level;
  return 'CRITICAL';
}

function quakesPerYear(mag: number): string {
  if (mag >= 8) return '≈ 1 / yıl';
  if (mag >= 7) return '≈ 15 / yıl';
  if (mag >= 6) return '≈ 130 / yıl';
  if (mag >= 5) return '≈ 1.300 / yıl';
  if (mag >= 4) return '≈ 13.000 / yıl';
  if (mag >= 3) return '> 100.000 / yıl';
  return 'sürekli (aletsel)';
}

function shaking(mag: number): { txt: string; radius: string } {
  if (mag >= 8) return { txt: 'Felaket düzeyi sarsıntı; geniş bölgede toptan yıkım.', radius: '> 1000 km hissedilir' };
  if (mag >= 7) return { txt: 'Yıkıcı; yaygın yapısal çöküş.', radius: '~500 km hissedilir' };
  if (mag >= 6) return { txt: 'Çok güçlü; dayanıksız yapılarda ciddi hasar.', radius: '~200 km hissedilir' };
  if (mag >= 5) return { txt: 'Güçlü; eşyalar düşer, zayıf yapılarda çatlaklar.', radius: '~100 km hissedilir' };
  if (mag >= 4) return { txt: 'Belirgin hissedilir; pencere ve kapılar titreşir.', radius: '~30–50 km hissedilir' };
  return { txt: 'Hafif; çoğu kişi fark etmez.', radius: 'yalnızca yakın çevrede' };
}

function energyVsM4(value: number): string {
  const ratio = Math.pow(10, 1.5 * (value - 4));
  if (ratio >= 1) {
    const r = ratio >= 1000 ? `${Math.round(ratio / 1000)} bin` : `${Math.round(ratio)}`;
    return `M4.0'a kıyasla ~${r}× enerji`;
  }
  return `M4.0'ın ~1/${Math.round(1 / ratio)}'i kadar enerji`;
}

export function DisasterView() {
  const [intensity, setIntensity] = useState(DEFAULT);

  const reading = intensity.toFixed(1);
  const below = intensity < FLOOR;
  const level = bandLevel(intensity);
  const info = INFO[level];
  const meterPct = Math.min(100, Math.max(0, ((intensity - MIN) / (MAX - MIN)) * 100));

  const refs = useMemo(() => {
    if (intensity < 5) return [];
    return [...REF_QUAKES]
      .filter((r) => Math.abs(r.mag - intensity) <= 0.8)
      .sort((a, b) => Math.abs(a.mag - intensity) - Math.abs(b.mag - intensity))
      .slice(0, 3);
  }, [intensity]);

  const felt = shaking(intensity);

  return (
    <div className="view disaster">
      <Panel eyebrow="girdi" title="Deprem değerlendirme" className="disaster__input-panel">
        <div className="advisory__reading">
          <span>Seçilen büyüklük</span>
          <strong>M{reading}</strong>
        </div>

        <label className="field">
          <span className="field__label">
            Deprem büyüklüğü
            <span className="field__val">{reading} Mw</span>
          </span>
          <input className="slider" type="range" min={MIN} max={MAX} step={STEP}
            value={intensity} onChange={(e) => setIntensity(Number(e.target.value))} />
          <span className="field__hint">Moment büyüklüğü (Mw), {MIN}–{MAX}</span>
        </label>

        {/* severity meter */}
        <div className="meter">
          <div className="meter__head">
            <RiskBadge level={below ? 'LOW' : level} />
            <span className="meter__next">{below ? 'Eşik altı' : INFO[level].comparison}</span>
          </div>
          <div className="meter__track">
            <span className="meter__fill" style={{ width: `${meterPct}%`, background: RISK[below ? 'LOW' : level].color }} />
          </div>
        </div>

        {/* band ladder */}
        <div className="scale">
          <div className="scale__track">
            {BAND_LADDER.map((b) => (
              <div key={b.level} className="scale__band"
                style={{ borderColor: RISK[b.level].color, background: level === b.level && !below ? RISK[b.level].color + '1a' : 'var(--paper)' }}>
                <span className="scale__band-label" style={{ color: RISK[b.level].color }}>{RISK[b.level].label}</span>
                <span className="scale__band-range">M{b.range}</span>
              </div>
            ))}
            <span className="scale__marker" style={{ left: `${meterPct}%` }} />
          </div>
        </div>
      </Panel>

      <Panel eyebrow="değerlendirme" title="Risk ve bilgilendirme" className="disaster__result-panel">
        {below ? (
          <div className="advisory">
            <div className="advisory__head">
              <span className="advisory__type">Deprem · M{reading}</span>
              <span className="badge badge--muted">değerlendirme yok</span>
            </div>
            <p className="advisory__text">
              Bu büyüklük (M{reading}) anlamlı bir olay sayılmaz; değerlendirilecek bir risk yok.
              Kaydırıcıyı M{FLOOR.toFixed(1)} üzerine taşıyınca etkiler, geçmiş örnekler ve
              önerilen eylemler görünür.
            </p>
          </div>
        ) : (
          <div className="advisory">
            <div className="advisory__head">
              <span className="advisory__type">Deprem · M{reading}</span>
              <RiskBadge level={level} />
            </div>

            <section className="info-block">
              <div className="info-block__title"><Info size={14} /> Tahmini etkiler</div>
              <ul className="advisory__list">{info.impacts.map((x, i) => <li key={i}>{x}</li>)}</ul>
            </section>

            <section className="info-block">
              <div className="info-block__title"><Gauge size={14} /> Ölçek bağlamı</div>
              <div className="fact-grid">
                <div className="fact"><span className="fact__k"><Activity size={12} /> Enerji</span><span className="fact__v">{energyVsM4(intensity)}</span></div>
                <div className="fact"><span className="fact__k"><History size={12} /> Sıklık</span><span className="fact__v">{quakesPerYear(intensity)} (dünya)</span></div>
                <div className="fact"><span className="fact__k"><Waves size={12} /> Hissedilme</span><span className="fact__v">{felt.radius}</span></div>
              </div>
              <p className="advisory__note">Her +1 büyüklük ≈ 32× daha fazla salınan enerji demektir.</p>
            </section>

            <section className="info-block">
              <div className="info-block__title"><GitCompare size={14} /> Beklenen sarsıntı</div>
              <p className="info-block__text">{felt.txt}</p>
            </section>

            {refs.length > 0 && (
              <section className="info-block">
                <div className="info-block__title"><History size={14} /> Yakın geçmişten (≈Mw)</div>
                <ul className="ref-list">
                  {refs.map((r) => (
                    <li key={`${r.name}-${r.year}`} className="ref">
                      <span className="ref__mag" style={{ color: RISK[bandLevel(r.mag)].color }}>M{r.mag.toFixed(1)}</span>
                      <span className="ref__name">{r.name}{r.tr ? ' 🇹🇷' : ''}</span>
                      <span className="ref__year">{r.year}{r.note ? ` · ${r.note}` : ''}</span>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <section className="info-block">
              <div className="info-block__title"><ListChecks size={14} /> Öncelikli eylemler</div>
              <ol className="action-list">{info.actions.map((x, i) => <li key={i}><span className="action-num">{i + 1}</span>{x}</li>)}</ol>
            </section>
          </div>
        )}
      </Panel>
    </div>
  );
}
