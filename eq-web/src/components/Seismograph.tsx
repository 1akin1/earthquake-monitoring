import { useMemo, useId } from 'react';
import { fmtAmp } from '../lib/format';

// The signature element. Renders an amplitude window as a polyline over a faint
// instrument grid, with a baseline and a peak marker. It reads like a real drum
// seismograph trace rather than a generic chart — this is the thing the console is
// remembered by.

interface Props {
  amplitudes: number[];
  height?: number;
  color?: string;
  // optional onset index to mark the detected trigger
  triggerIndex?: number | null;
  // animate a sweeping playhead (used on the live/detection panel)
  sweep?: boolean;
}

const W = 1000; // viewBox width; SVG scales to container

export function Seismograph({
  amplitudes,
  height = 200,
  color = 'var(--seismic)',
  triggerIndex = null,
  sweep = false,
}: Props) {
  const id = useId();
  const h = height;
  const mid = h / 2;

  const { path, peak, peakX, peakY } = useMemo(() => {
    const n = amplitudes.length;
    if (n === 0) return { path: '', peak: 0, peakX: 0, peakY: mid };
    let maxAbs = 0;
    for (const a of amplitudes) maxAbs = Math.max(maxAbs, Math.abs(a));
    const scale = maxAbs > 0 ? (h * 0.42) / maxAbs : 1;

    let d = '';
    let peakV = 0;
    let pX = 0;
    let pY = mid;
    // Downsample to at most ~1000 points so very long windows stay crisp & cheap.
    const step = Math.max(1, Math.floor(n / W));
    for (let i = 0, j = 0; i < n; i += step, j++) {
      const x = (i / (n - 1)) * W;
      const y = mid - amplitudes[i] * scale;
      d += j === 0 ? `M${x.toFixed(1)},${y.toFixed(1)}` : `L${x.toFixed(1)},${y.toFixed(1)}`;
      if (Math.abs(amplitudes[i]) > Math.abs(peakV)) {
        peakV = amplitudes[i];
        pX = x;
        pY = y;
      }
    }
    return { path: d, peak: peakV, peakX: pX, peakY: pY };
  }, [amplitudes, h, mid]);

  const trigX =
    triggerIndex != null && amplitudes.length > 1
      ? (triggerIndex / (amplitudes.length - 1)) * W
      : null;

  return (
    <div className="seismo">
      <svg
        viewBox={`0 0 ${W} ${h}`}
        preserveAspectRatio="none"
        className="seismo__svg"
        role="img"
        aria-label="Sismograf sinyal izi"
      >
        {/* horizontal grid lines */}
        {[0.2, 0.4, 0.6, 0.8].map((f) => (
          <line key={f} x1={0} x2={W} y1={h * f} y2={h * f} className="seismo__grid" />
        ))}
        {/* vertical ticks */}
        {Array.from({ length: 11 }, (_, i) => i).map((i) => (
          <line key={i} x1={(W / 10) * i} x2={(W / 10) * i} y1={0} y2={h} className="seismo__grid seismo__grid--v" />
        ))}
        {/* baseline */}
        <line x1={0} x2={W} y1={mid} y2={mid} className="seismo__baseline" />

        {/* trigger marker */}
        {trigX != null && (
          <line x1={trigX} x2={trigX} y1={0} y2={h} className="seismo__trigger" />
        )}

        {/* the trace */}
        <path d={path} className="seismo__trace" style={{ stroke: color }} />

        {/* peak dot */}
        {amplitudes.length > 0 && (
          <>
            <circle cx={peakX} cy={peakY} r={4.5} className="seismo__peak" />
            <circle cx={peakX} cy={peakY} r={9} className="seismo__peak-ring" />
          </>
        )}

        {/* sweeping playhead */}
        {sweep && (
          <line x1={0} x2={0} y1={0} y2={h} className="seismo__sweep">
            <animate attributeName="x1" from="0" to={W} dur="3.5s" repeatCount="indefinite" />
            <animate attributeName="x2" from="0" to={W} dur="3.5s" repeatCount="indefinite" />
          </line>
        )}
      </svg>
      <div className="seismo__readout">
        <span>tepe genlik</span>
        <strong>{fmtAmp(peak)}</strong>
        <span className="seismo__samples">{amplitudes.length} örnek</span>
      </div>
      {/* unique id retained to avoid collisions if multiple traces mount */}
      <span hidden id={id} />
    </div>
  );
}
