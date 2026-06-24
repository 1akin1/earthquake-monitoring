import { useEffect, useRef } from 'react';
import { useRegion } from '../components/RegionContext';
import { REGIONS } from '../lib/region';
import { Activity, Globe2, MapPin } from 'lucide-react';

// The entry screen the user picks a scope from. Keeps the ambient seismic baseline
// that used to sit behind the login card — same look, now with a real purpose.
export function RegionSelectView() {
  const { setRegion } = useRegion();
  const waveRef = useRef<SVGPathElement>(null);

  useEffect(() => {
    let raf = 0;
    let t = 0;
    const W = 1200;
    const mid = 100;
    function frame() {
      t += 0.02;
      let d = `M0,${mid}`;
      for (let x = 0; x <= W; x += 12) {
        const env = Math.exp(-Math.pow((x - 600) / 260, 2)); // packet centred
        const base = Math.sin(x * 0.02 + t) * 4;
        const burst = env * Math.sin(x * 0.18 + t * 3) * 46;
        d += ` L${x},${(mid + base + burst).toFixed(1)}`;
      }
      waveRef.current?.setAttribute('d', d);
      raf = requestAnimationFrame(frame);
    }
    raf = requestAnimationFrame(frame);
    return () => cancelAnimationFrame(raf);
  }, []);

  return (
    <div className="login">
      <div className="login__trace" aria-hidden>
        <svg viewBox="0 0 1200 200" preserveAspectRatio="none">
          <path ref={waveRef} className="login__wave" d="" />
        </svg>
      </div>

      <div className="login__card region-card">
        <div className="login__brand">
          <span className="login__logo"><Activity size={20} /></span>
          <div>
            <div className="login__title">Sismik İzleme Konsolu</div>
            <div className="login__sub">deprem izleme & sinyal analizi</div>
          </div>
        </div>

        <div className="region-pick__label">İzleme kapsamını seç</div>
        <div className="region-pick">
          {REGIONS.map((r) => (
            <button key={r.key} type="button" className="region-opt" onClick={() => setRegion(r.key)}>
              <span className="region-opt__icon">
                {r.key === 'TR' ? <MapPin size={26} /> : <Globe2 size={26} />}
              </span>
              <span className="region-opt__label">{r.label}</span>
              <span className="region-opt__sub">{r.sub}</span>
            </button>
          ))}
        </div>

        <p className="region-pick__note">
          Türkiye Kandilli ve AFAD kaynaklarını, Dünya ise USGS küresel akışını kullanır.
          İstediğin an üst çubuktan kapsamı değiştirebilirsin.
        </p>
      </div>
    </div>
  );
}
