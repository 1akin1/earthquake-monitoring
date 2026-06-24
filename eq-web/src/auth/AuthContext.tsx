import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { api } from '../api/endpoints';
import { setTokenProvider, setUnauthorizedHandler } from '../api/client';
import type { Role } from '../api/types';

interface Session {
  token: string;
  role: Role;
  username: string;
  expiresAt: string;
}

interface AuthState {
  session: Session | null;
  username: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  // The console is open (read-only) by default; a single admin login unlocks the
  // privileged operations (import feeds, run detection, delete events).
  isAdmin: boolean;
  // Kept as aliases so existing call sites read naturally — both mean "admin".
  canAnalyze: boolean;
  canDelete: boolean;
}

const STORAGE_KEY = 'eq.session';
const AuthContext = createContext<AuthState | null>(null);

function load(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Session;
    if (new Date(s.expiresAt).getTime() <= Date.now()) return null;
    return s;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => load());
  const sessionRef = useRef<Session | null>(session);
  sessionRef.current = session;

  useEffect(() => {
    setTokenProvider(() => sessionRef.current?.token ?? null);
  }, []);

  const logout = useCallback(() => {
    setSession(null);
    localStorage.removeItem(STORAGE_KEY);
  }, []);

  // A 401 means the admin token lapsed — drop back to read-only, do not block the app.
  useEffect(() => {
    setUnauthorizedHandler(() => logout());
  }, [logout]);

  const login = useCallback(async (username: string, password: string) => {
    const res = await api.login(username, password);
    const next: Session = {
      token: res.token,
      role: res.role,
      username,
      expiresAt: res.expiresAt,
    };
    setSession(next);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }, []);

  // Auto-logout exactly when the token expires.
  useEffect(() => {
    if (!session) return;
    const ms = new Date(session.expiresAt).getTime() - Date.now();
    if (ms <= 0) {
      logout();
      return;
    }
    const t = setTimeout(logout, ms);
    return () => clearTimeout(t);
  }, [session, logout]);

  const isAdmin = session?.role === 'ADMIN';

  const value = useMemo<AuthState>(
    () => ({
      session,
      username: session?.username ?? null,
      login,
      logout,
      isAdmin,
      canAnalyze: isAdmin,
      canDelete: isAdmin,
    }),
    [session, login, logout, isAdmin],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
