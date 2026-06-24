import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { api } from '../api/endpoints';
import type { EarthquakeResponse, ReportResponse, StatsResponse } from '../api/types';
import { ApiError } from '../api/client';
import { useToast } from './Toast';

interface DataState {
  earthquakes: EarthquakeResponse[];
  report: ReportResponse | null;
  stats: StatsResponse | null;
  loading: boolean;
  refresh: (silent?: boolean) => Promise<void>;
}

const DataContext = createContext<DataState | null>(null);

export function DataProvider({ children }: { children: ReactNode }) {
  const toast = useToast();
  const [earthquakes, setEarthquakes] = useState<EarthquakeResponse[]>([]);
  const [report, setReport] = useState<ReportResponse | null>(null);
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    // Each read is independent; stats may be absent in the platform profile, so a
    // failure there shouldn't blank the whole dashboard.
    const [eqRes, repRes, statRes] = await Promise.allSettled([
      api.listEarthquakes(),
      api.report(),
      api.stats(),
    ]);
    if (eqRes.status === 'fulfilled') {
      setEarthquakes([...eqRes.value].sort((a, b) => b.occurredAt.localeCompare(a.occurredAt)));
    } else if (!silent && eqRes.reason instanceof ApiError && eqRes.reason.status !== 401) {
      toast.error(eqRes.reason.message);
    }
    if (repRes.status === 'fulfilled') setReport(repRes.value);
    if (statRes.status === 'fulfilled') setStats(statRes.value);
    else setStats(null); // stats endpoint not present (platform profile) — fine
    if (!silent) setLoading(false);
  }, [toast]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Quietly re-poll every 60s so the console stays current without a manual refresh.
  useEffect(() => {
    const id = setInterval(() => refresh(true), 60000);
    return () => clearInterval(id);
  }, [refresh]);

  return (
    <DataContext.Provider value={{ earthquakes, report, stats, loading, refresh }}>
      {children}
    </DataContext.Provider>
  );
}

export function useData(): DataState {
  const ctx = useContext(DataContext);
  if (!ctx) throw new Error('useData must be used within DataProvider');
  return ctx;
}
