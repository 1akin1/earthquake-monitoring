// Thin fetch wrapper. Resolves the API base, attaches the bearer token, and turns
// non-2xx responses into a typed ApiError the UI can surface in plain language.

const RAW_BASE = (import.meta.env.VITE_API_BASE ?? '').trim();
// Empty base => same origin, so the Vite dev proxy (/api -> :8081) handles it.
export const API_BASE = RAW_BASE.replace(/\/+$/, '');

export class ApiError extends Error {
  readonly status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

// The auth layer registers a getter here so every request can read the live token
// without the client importing React state.
let tokenProvider: () => string | null = () => null;
export function setTokenProvider(fn: () => string | null) {
  tokenProvider = fn;
}

// Called when a request comes back 401, so the shell can bounce to the login view.
let onUnauthorized: () => void = () => {};
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  // Some endpoints (report render) return text, not JSON.
  raw?: boolean;
}

async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, raw = false } = opts;
  const headers: Record<string, string> = {};
  const token = tokenProvider();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Sunucuya ulaşılamadı. Backend çalışıyor mu (localhost:8081)?');
  }

  if (res.status === 401) {
    onUnauthorized();
    throw new ApiError(401, 'Oturum süresi doldu. Lütfen tekrar giriş yapın.');
  }
  if (res.status === 403) {
    throw new ApiError(403, 'Bu işlem için yetkiniz yok.');
  }
  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  if (!res.ok) {
    throw new ApiError(res.status, extractMessage(text, res.status));
  }
  if (raw) return text as unknown as T;
  return (text ? JSON.parse(text) : undefined) as T;
}

// Spring's default error body is JSON with a "message"/"error" field; fall back to
// the raw text or a status-based default.
function extractMessage(text: string, status: number): string {
  if (text) {
    try {
      const parsed = JSON.parse(text);
      if (typeof parsed.message === 'string' && parsed.message) return parsed.message;
      if (typeof parsed.error === 'string' && parsed.error) return parsed.error;
    } catch {
      if (text.length < 200) return text;
    }
  }
  return `İstek başarısız (HTTP ${status}).`;
}

export const http = {
  get: <T>(path: string) => request<T>(path),
  getText: (path: string) => request<string>(path, { raw: true }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  del: (path: string) => request<void>(path, { method: 'DELETE' }),
};
