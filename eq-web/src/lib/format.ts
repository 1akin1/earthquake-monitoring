// Display formatting. The UI shows numeric data in a monospace face, so these keep
// widths predictable (fixed decimals, padded coordinates).

const dtf = new Intl.DateTimeFormat('tr-TR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

export function fmtDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '—';
  return dtf.format(d);
}

export function fmtRelative(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso).getTime();
  if (isNaN(d)) return '—';
  const diff = Date.now() - d;
  const min = Math.round(diff / 60000);
  if (min < 1) return 'az önce';
  if (min < 60) return `${min} dk önce`;
  const hr = Math.round(min / 60);
  if (hr < 24) return `${hr} sa önce`;
  const day = Math.round(hr / 24);
  return `${day} gün önce`;
}

export function fmtMag(m: number | null | undefined): string {
  if (m === null || m === undefined || isNaN(m)) return '—';
  return m.toFixed(1);
}

export function fmtNum(n: number | null | undefined, decimals = 2): string {
  if (n === null || n === undefined || isNaN(n)) return '—';
  return n.toFixed(decimals);
}

// Seismic amplitudes span orders of magnitude (peak ~10^M), so show large values in
// compact scientific form and small ones with fixed decimals.
export function fmtAmp(n: number | null | undefined): string {
  if (n === null || n === undefined || isNaN(n)) return '—';
  const a = Math.abs(n);
  if (a >= 1000) return a.toExponential(2);
  return a.toFixed(3);
}

export function fmtCoord(lat: number, lon: number): string {
  const ns = lat >= 0 ? 'K' : 'G'; // Kuzey / Güney
  const ew = lon >= 0 ? 'D' : 'B'; // Doğu / Batı
  return `${Math.abs(lat).toFixed(3)}°${ns}, ${Math.abs(lon).toFixed(3)}°${ew}`;
}
