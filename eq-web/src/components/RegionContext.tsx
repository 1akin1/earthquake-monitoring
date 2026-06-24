import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { Region } from '../lib/region';

interface RegionState {
  region: Region | null;
  setRegion: (r: Region) => void;
  clearRegion: () => void;
}

const STORAGE_KEY = 'eq.region';
const RegionContext = createContext<RegionState | null>(null);

function load(): Region | null {
  const v = localStorage.getItem(STORAGE_KEY);
  return v === 'TR' || v === 'WORLD' ? v : null;
}

export function RegionProvider({ children }: { children: ReactNode }) {
  const [region, setRegionState] = useState<Region | null>(() => load());

  const setRegion = useCallback((r: Region) => {
    setRegionState(r);
    localStorage.setItem(STORAGE_KEY, r);
  }, []);

  const clearRegion = useCallback(() => {
    setRegionState(null);
    localStorage.removeItem(STORAGE_KEY);
  }, []);

  const value = useMemo<RegionState>(
    () => ({ region, setRegion, clearRegion }),
    [region, setRegion, clearRegion],
  );

  return <RegionContext.Provider value={value}>{children}</RegionContext.Provider>;
}

export function useRegion(): RegionState {
  const ctx = useContext(RegionContext);
  if (!ctx) throw new Error('useRegion must be used within RegionProvider');
  return ctx;
}
