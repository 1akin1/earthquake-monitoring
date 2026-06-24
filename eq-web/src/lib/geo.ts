// Approximate continent classification from coordinates. Bounding boxes overlap at
// borders so this is a rough bucket, not authoritative geography — good enough to filter
// a global feed by region without any external geocoding service.

export type ContinentKey = 'ALL' | 'EU' | 'AS' | 'AF' | 'NA' | 'SA' | 'OC' | 'AN';

export const CONTINENTS: { key: ContinentKey; label: string }[] = [
  { key: 'ALL', label: 'Tüm kıtalar' },
  { key: 'EU', label: 'Avrupa' },
  { key: 'AS', label: 'Asya' },
  { key: 'AF', label: 'Afrika' },
  { key: 'NA', label: 'Kuzey Amerika' },
  { key: 'SA', label: 'Güney Amerika' },
  { key: 'OC', label: 'Okyanusya' },
  { key: 'AN', label: 'Antarktika' },
];

export function continentOf(lat: number, lon: number): ContinentKey {
  if (lat <= -60) return 'AN';
  // Western hemisphere → Americas
  if (lon <= -30) {
    if (lat >= 13) return 'NA';
    if (lat >= -57 && lon >= -82) return 'SA';
    return 'NA';
  }
  // Eastern hemisphere
  if (lon >= 110 && lat <= 0) return 'OC';
  if (lon >= 150) return 'OC';
  if (lat >= 36 && lon >= -25 && lon <= 60) return 'EU';
  if (lat < 36 && lon >= -20 && lon <= 52) return 'AF';
  if (lon > 40) return 'AS';
  return 'EU';
}

// Minimum-magnitude filter options shared by the monitor and report views.
export const MIN_MAGS: { value: number; label: string }[] = [
  { value: 0, label: 'Tümü' },
  { value: 3, label: 'M3+' },
  { value: 4, label: 'M4+' },
  { value: 5, label: 'M5+' },
  { value: 6, label: 'M6+' },
];
