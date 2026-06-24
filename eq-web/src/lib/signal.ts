// Generates a realistic-looking signal window for the detection panel. The backend's
// detector runs an STA/LTA trigger over amplitudes, so a quiet noise floor followed by
// a SHARP wave packet trips a detection — the synthetic data is meaningful, not
// decorative. A flat/low window stays below threshold (no detection), which is also a
// useful thing to demonstrate.
//
// The packet is intentionally compact: a long, slowly-decaying tail bleeds energy into
// the detector's long-term (background) window and suppresses the STA/LTA ratio, which
// is why earlier tuning never fired. Tight P/S envelopes keep the background quiet so
// the ratio actually crosses the trigger.

export interface SignalSpec {
  durationSec: number;
  sampleRateHz: number;
  // 0 = pure noise (no event), higher = stronger packet
  eventStrength: number;
  // fraction (0..1) of the window where the packet onset sits
  onset: number;
  seed: number;
}

export const DEFAULT_SPEC: SignalSpec = {
  durationSec: 30,
  sampleRateHz: 50,
  eventStrength: 1,
  onset: 0.5,
  seed: 7,
};

// Small deterministic PRNG so a given seed reproduces the same trace.
function mulberry32(seed: number): () => number {
  let a = seed >>> 0;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function generateSignal(spec: SignalSpec): number[] {
  const { durationSec, sampleRateHz, eventStrength, onset, seed } = spec;
  const n = Math.max(1, Math.round(durationSec * sampleRateHz));
  const rand = mulberry32(seed);
  const out = new Array<number>(n);

  const onsetIdx = Math.floor(n * onset);
  // P-wave arrives first (small), S-wave a bit later (large) — classic shape.
  const pIdx = onsetIdx;
  const sIdx = Math.min(n - 1, onsetIdx + Math.floor(sampleRateHz * 1.6));

  for (let i = 0; i < n; i++) {
    // background micro-tremor noise (quiet, so the trigger ratio can build)
    let v = (rand() - 0.5) * 0.5;

    if (eventStrength > 0) {
      // P packet: short, modest amplitude
      const pEnv = Math.exp(-Math.pow((i - pIdx) / (sampleRateHz * 0.25), 2));
      v += pEnv * eventStrength * 1.4 * Math.sin(i * 0.9);
      // S packet: dominant energy, sharp rise + a SHORT decay so it stays compact
      const sRise = Math.exp(-Math.pow((i - sIdx) / (sampleRateHz * 0.22), 2));
      const sDecay = i >= sIdx ? Math.exp(-(i - sIdx) / (sampleRateHz * 0.7)) : 0;
      const sEnv = Math.max(sRise, sDecay);
      v += sEnv * eventStrength * 3.4 * Math.sin(i * 0.55 + 0.4);
    }
    out[i] = Math.round(v * 1000) / 1000;
  }
  return out;
}

// Quick client-side peak read so the panel can preview before the server scores it.
export function peakOf(amplitudes: number[]): number {
  let peak = 0;
  for (const a of amplitudes) {
    const m = Math.abs(a);
    if (m > peak) peak = m;
  }
  return peak;
}

// Rough magnitude estimate from peak amplitude, mirroring the backend's placeholder
// formula (M = log10(peak) + 3, clamped). Used to show an APPROXIMATE reading when the
// detector doesn't formally trigger, instead of a bare "0".
export function approxMagnitude(peak: number): number {
  if (peak <= 0) return 0;
  const m = Math.log10(peak) + 3.0;
  return Math.max(0, Math.min(10, m));
}
