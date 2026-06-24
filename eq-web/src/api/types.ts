// API types mirror the backend DTOs field-for-field. Keeping names identical to
// the Java records means responses deserialize with no remapping.

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type Role = 'PUBLIC' | 'SCIENTIST' | 'ADMIN';
export type DisasterType = 'EARTHQUAKE' | 'FLOOD' | 'WILDFIRE';

export interface TokenResponse {
  token: string;
  tokenType: string; // "Bearer"
  role: Role;
  expiresAt: string; // ISO instant
}

export interface EarthquakeResponse {
  id: number;
  magnitude: number;
  depthKm: number;
  latitude: number;
  longitude: number;
  source: string;
  occurredAt: string; // ISO instant
  riskScore: number | null;
  riskLevel: RiskLevel | null;
}

export interface CreateEarthquakeRequest {
  magnitude: number;
  depthKm: number;
  latitude: number;
  longitude: number;
  source: string;
  occurredAt: string; // ISO instant
}

export interface AnalyzeSignalRequest {
  stationId: string;
  latitude: number;
  longitude: number;
  sampleRateHz: number;
  startTime: string; // ISO instant
  amplitudes: number[];
}

export interface DetectionResponse {
  detected: boolean;
  estimatedMagnitude: number | null;
  peakAmplitude: number;
  staLtaRatio: number;
  triggeredAt: string | null;
  earthquakeId: number | null;
  riskLevel: RiskLevel | null;
}

// Batch, read-only sweep over many signals. Mirrors DetectionResponse minus the
// (never-created) earthquakeId; the backend persists/publishes nothing for these.
export interface EvaluateSignalsRequest {
  signals: AnalyzeSignalRequest[];
}

export interface SignalEvaluationResponse {
  detected: boolean;
  estimatedMagnitude: number | null;
  peakAmplitude: number;
  staLtaRatio: number;
  riskLevel: RiskLevel | null;
}

export interface EvaluateResponse {
  total: number;
  detected: number;
  results: SignalEvaluationResponse[];
}

export interface AssessDisasterRequest {
  type: DisasterType;
  intensity: number;
}

export interface DisasterAssessmentResponse {
  type: string;
  riskLevel: RiskLevel;
  advisory: string;
}

export interface ReportResponse {
  title: string;
  generatedAt: string;
  totalEarthquakes: number;
  maxMagnitude: number;
  averageMagnitude: number;
  averageDepthKm: number;
  riskBreakdown: Record<string, number>;
  earliest: string | null;
  latest: string | null;
  strongestEarthquakeId: number | null;
}

export interface ImportResultResponse {
  imported: number;
  bySource: Record<string, number>;
}

export interface MonitoringCycleResponse {
  imported: ImportResultResponse;
  report: ReportResponse;
}

export interface StatsResponse {
  totalEvents: number;
  maxMagnitude: number;
  byRiskLevel: Record<string, number>;
}
