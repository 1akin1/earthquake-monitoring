import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react';
import { riskMeta } from '../lib/risk';
import type { RiskLevel } from '../api/types';

/* ---- Button ---- */
interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost' | 'danger';
  loading?: boolean;
}
export function Button({ variant = 'primary', loading, children, disabled, className = '', ...rest }: ButtonProps) {
  return (
    <button
      className={`btn btn--${variant} ${className}`}
      disabled={disabled || loading}
      {...rest}
    >
      {loading && <span className="btn__spinner" aria-hidden />}
      <span>{children}</span>
    </button>
  );
}

/* ---- Field (labelled input) ---- */
interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  hint?: string;
}
export function Field({ label, hint, id, ...rest }: FieldProps) {
  const fid = id ?? `f-${label.replace(/\s+/g, '-').toLowerCase()}`;
  return (
    <label className="field" htmlFor={fid}>
      <span className="field__label">{label}</span>
      <input id={fid} className="field__input" {...rest} />
      {hint && <span className="field__hint">{hint}</span>}
    </label>
  );
}

/* ---- Panel ---- */
export function Panel({
  title,
  eyebrow,
  actions,
  children,
  className = '',
}: {
  title?: string;
  eyebrow?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      {(title || actions) && (
        <header className="panel__head">
          <div>
            {eyebrow && <div className="panel__eyebrow">{eyebrow}</div>}
            {title && <h2 className="panel__title">{title}</h2>}
          </div>
          {actions && <div className="panel__actions">{actions}</div>}
        </header>
      )}
      <div className="panel__body">{children}</div>
    </section>
  );
}

/* ---- Stat readout ---- */
export function Stat({ label, value, unit, accent }: { label: string; value: ReactNode; unit?: string; accent?: string }) {
  return (
    <div className="stat">
      <div className="stat__label">{label}</div>
      <div className="stat__value" style={accent ? { color: accent } : undefined}>
        {value}
        {unit && <span className="stat__unit">{unit}</span>}
      </div>
    </div>
  );
}

/* ---- Risk badge ---- */
export function RiskBadge({ level, size = 'md' }: { level: RiskLevel | null | undefined; size?: 'sm' | 'md' }) {
  const meta = riskMeta(level);
  if (!meta) return <span className="badge badge--muted">Puanlanmadı</span>;
  return (
    <span
      className={`badge badge--${size}`}
      style={{ color: meta.color, background: meta.soft, borderColor: meta.color }}
    >
      <span className="badge__dot" style={{ background: meta.color }} />
      {meta.label}
    </span>
  );
}

/* ---- Empty state ---- */
export function EmptyState({ icon, title, hint }: { icon?: ReactNode; title: string; hint?: string }) {
  return (
    <div className="empty">
      {icon && <div className="empty__icon">{icon}</div>}
      <div className="empty__title">{title}</div>
      {hint && <div className="empty__hint">{hint}</div>}
    </div>
  );
}

/* ---- Inline spinner ---- */
export function Spinner({ label }: { label?: string }) {
  return (
    <div className="loading">
      <span className="loading__ring" aria-hidden />
      {label && <span>{label}</span>}
    </div>
  );
}
