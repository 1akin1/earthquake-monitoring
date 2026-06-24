// Region model. The console is split into two worlds chosen at the entry screen:
//   • TR    → Türkiye feeds (Kandilli + AFAD). Continent filtering is meaningless here
//             (everything is in one country), so it is never shown.
//   • WORLD → the global USGS feed. Continent filtering is offered here.
//
// Centralising this keeps MonitorView / ReportView from re-deriving the same rules.

export type Region = 'TR' | 'WORLD';

export const REGIONS: { key: Region; label: string; sub: string }[] = [
  { key: 'TR', label: 'Türkiye', sub: 'Kandilli + AFAD' },
  { key: 'WORLD', label: 'Dünya', sub: 'USGS' },
];

export function regionLabel(region: Region): string {
  return REGIONS.find((r) => r.key === region)?.label ?? region;
}

// Sources the user can pick within a region. 'ALL' means "all sources of this region".
// WORLD has a single source (USGS), so its bar is effectively informational.
export const REGION_SOURCES: Record<Region, { key: string; label: string }[]> = {
  TR: [
    { key: 'ALL', label: 'Tümü' },
    { key: 'AFAD', label: 'AFAD' },
    { key: 'Kandilli', label: 'Kandilli' },
  ],
  WORLD: [{ key: 'USGS', label: 'USGS' }],
};

const TR_SOURCE_SET = new Set(['AFAD', 'Kandilli']);

// Default picked source when a region is (re)entered.
export function defaultSource(region: Region): string {
  return region === 'WORLD' ? 'USGS' : 'ALL';
}

// Does an event belong to the active region + picked source?
export function sourceMatches(region: Region, picked: string, eventSource: string): boolean {
  if (region === 'WORLD') return eventSource === 'USGS';
  // TR
  if (picked === 'ALL') return TR_SOURCE_SET.has(eventSource);
  return eventSource === picked;
}

// Continent filtering is only meaningful for the global feed.
export function regionUsesContinents(region: Region): boolean {
  return region === 'WORLD';
}
