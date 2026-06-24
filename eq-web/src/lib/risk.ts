import type { RiskLevel } from '../api/types';

// Risk is the data's own language and the loudest color on the page. These four
// values are reused everywhere a risk appears: badges, map circles, the legend.
export interface RiskMeta {
  level: RiskLevel;
  label: string; // Turkish UI label
  color: string; // base
  soft: string; // translucent fill for map / chips
  rank: number; // for sorting / severity comparisons
}

export const RISK: Record<RiskLevel, RiskMeta> = {
  LOW: { level: 'LOW', label: 'Düşük', color: '#3fa66a', soft: 'rgba(63,166,106,0.18)', rank: 0 },
  MEDIUM: { level: 'MEDIUM', label: 'Orta', color: '#e0a93b', soft: 'rgba(224,169,59,0.20)', rank: 1 },
  HIGH: { level: 'HIGH', label: 'Yüksek', color: '#e0683c', soft: 'rgba(224,104,60,0.20)', rank: 2 },
  CRITICAL: { level: 'CRITICAL', label: 'Kritik', color: '#d6324a', soft: 'rgba(214,50,74,0.22)', rank: 3 },
};

export const RISK_ORDER: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function riskMeta(level: RiskLevel | null | undefined): RiskMeta | null {
  if (!level) return null;
  return RISK[level] ?? null;
}

// Magnitude-driven color when no risk score is present (e.g. feed preview rows).
export function magnitudeColor(mag: number): string {
  if (mag >= 6) return RISK.CRITICAL.color;
  if (mag >= 5) return RISK.HIGH.color;
  if (mag >= 4) return RISK.MEDIUM.color;
  return RISK.LOW.color;
}
