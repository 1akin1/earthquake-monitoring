import { http } from './client';
import type {
  TokenResponse,
  EarthquakeResponse,
  CreateEarthquakeRequest,
  AnalyzeSignalRequest,
  DetectionResponse,
  EvaluateResponse,
  AssessDisasterRequest,
  DisasterAssessmentResponse,
  ReportResponse,
  ImportResultResponse,
  MonitoringCycleResponse,
  StatsResponse,
} from './types';

// One function per backend route. Paths match SecurityConfig exactly so role gating
// on the server lines up with what the UI offers.
export const api = {
  // open
  login: (username: string, password: string) =>
    http.post<TokenResponse>('/api/auth/login', { username, password }),

  // reads — any authenticated role
  listEarthquakes: () => http.get<EarthquakeResponse[]>('/api/earthquakes'),
  getEarthquake: (id: number) => http.get<EarthquakeResponse>(`/api/earthquakes/${id}`),
  report: () => http.get<ReportResponse>('/api/reports'),
  renderReport: (format: 'text' | 'markdown') =>
    http.getText(`/api/reports/render?format=${format}`),
  stats: () => http.get<StatsResponse>('/api/stats'),
  previewFeeds: () => http.get<EarthquakeResponse[]>('/api/feeds/preview'),

  // analysis / writes — SCIENTIST or ADMIN
  createEarthquake: (body: CreateEarthquakeRequest) =>
    http.post<EarthquakeResponse>('/api/earthquakes', body),
  analyze: (body: AnalyzeSignalRequest) =>
    http.post<DetectionResponse>('/api/detection/analyze', body),
  evaluateAll: (signals: AnalyzeSignalRequest[]) =>
    http.post<EvaluateResponse>('/api/detection/evaluate', { signals }),
  assessDisaster: (body: AssessDisasterRequest) =>
    http.post<DisasterAssessmentResponse>('/api/disasters/assess', body),
  importFeeds: () => http.post<ImportResultResponse>('/api/feeds/import'),
  runCycle: () => http.post<MonitoringCycleResponse>('/api/monitoring/cycle'),

  // destructive — ADMIN only
  deleteEarthquake: (id: number) => http.del(`/api/earthquakes/${id}`),
};
