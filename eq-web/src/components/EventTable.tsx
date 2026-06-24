import { useState, useMemo } from 'react';
import type { EarthquakeResponse } from '../api/types';
import { RISK } from '../lib/risk';
import { fmtMag, fmtDateTime, fmtCoord } from '../lib/format';
import { RiskBadge } from './ui';
import { Trash2, ArrowUp, ArrowDown } from 'lucide-react';

type SortKey = 'magnitude' | 'occurredAt' | 'depthKm' | 'source';

interface Props {
  earthquakes: EarthquakeResponse[];
  selectedId?: number | null;
  onSelect?: (eq: EarthquakeResponse) => void;
  canDelete?: boolean;
  onDelete?: (eq: EarthquakeResponse) => void;
  deletingId?: number | null;
}

export function EventTable({ earthquakes, selectedId, onSelect, canDelete, onDelete, deletingId }: Props) {
  const [sortKey, setSortKey] = useState<SortKey>('occurredAt');
  const [dir, setDir] = useState<'asc' | 'desc'>('desc');

  const sorted = useMemo(() => {
    const arr = [...earthquakes];
    arr.sort((a, b) => {
      let cmp: number;
      if (sortKey === 'source') cmp = a.source.localeCompare(b.source);
      else if (sortKey === 'occurredAt') cmp = a.occurredAt.localeCompare(b.occurredAt);
      else cmp = a[sortKey] - b[sortKey];
      return dir === 'asc' ? cmp : -cmp;
    });
    return arr;
  }, [earthquakes, sortKey, dir]);

  function toggle(key: SortKey) {
    if (key === sortKey) setDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    else {
      setSortKey(key);
      setDir(key === 'occurredAt' || key === 'magnitude' ? 'desc' : 'asc');
    }
  }

  const Th = ({ k, label, align }: { k: SortKey; label: string; align?: 'right' }) => (
    <th className={align === 'right' ? 'is-right' : ''} onClick={() => toggle(k)}>
      <span className="th-inner">
        {label}
        {sortKey === k && (dir === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />)}
      </span>
    </th>
  );

  return (
    <div className="event-table-wrap">
      <table className="event-table">
        <thead>
          <tr>
            <Th k="magnitude" label="M" align="right" />
            <th>Risk</th>
            <Th k="occurredAt" label="Tarih / saat" />
            <th>Konum</th>
            <Th k="depthKm" label="Derinlik" align="right" />
            <Th k="source" label="Kaynak" />
            {canDelete && <th></th>}
          </tr>
        </thead>
        <tbody>
          {sorted.map((eq) => (
            <tr
              key={eq.id}
              className={selectedId === eq.id ? 'is-selected' : ''}
              onClick={() => onSelect?.(eq)}
            >
              <td className="is-right mag" style={{ color: RISK[eq.riskLevel ?? 'LOW'].color }}>{fmtMag(eq.magnitude)}</td>
              <td><RiskBadge level={eq.riskLevel} size="sm" /></td>
              <td className="mono">{fmtDateTime(eq.occurredAt)}</td>
              <td className="mono">{fmtCoord(eq.latitude, eq.longitude)}</td>
              <td className="is-right mono">{eq.depthKm.toFixed(1)} km</td>
              <td>{eq.source}</td>
              {canDelete && (
                <td className="is-right">
                  <button
                    className="event__del"
                    title="Sil"
                    disabled={deletingId === eq.id}
                    onClick={(e) => { e.stopPropagation(); onDelete?.(eq); }}
                  >
                    <Trash2 size={14} />
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
