import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { useRegion } from '../components/RegionContext';
import { regionLabel } from '../lib/region';
import { DataProvider } from '../components/DataContext';
import { AdminLoginModal } from '../components/AdminLoginModal';
import { MonitorView } from './MonitorView';
import { DetectionView } from './DetectionView';
import { DisasterView } from './DisasterView';
import { ReportView } from './ReportView';
import { Activity, Radar, Zap, ShieldAlert, FileText, LogOut, ShieldCheck, Globe2, MapPin, Repeat } from 'lucide-react';

type ViewKey = 'monitor' | 'detection' | 'disaster' | 'report';

interface NavItem {
  key: ViewKey;
  label: string;
  icon: typeof Radar;
}
// All sections are visible to everyone; privileged *actions* inside them gate on admin.
const NAV: NavItem[] = [
  { key: 'monitor', label: 'İzleme', icon: Radar },
  { key: 'detection', label: 'Algılama', icon: Zap },
  { key: 'disaster', label: 'Değerlendirme', icon: ShieldAlert },
  { key: 'report', label: 'Rapor', icon: FileText },
];

export function Shell() {
  const { isAdmin, logout } = useAuth();
  const { region, clearRegion } = useRegion();
  const [view, setView] = useState<ViewKey>('monitor');
  const [loginOpen, setLoginOpen] = useState(false);

  return (
    <DataProvider>
      <div className="shell">
        <nav className="rail">
          <div className="rail__brand">
            <span className="rail__logo"><Activity size={18} /></span>
            <span className="rail__brand-text">Sismik Konsol</span>
          </div>
          <ul className="rail__nav">
            {NAV.map((n) => {
              const Icon = n.icon;
              return (
                <li key={n.key}>
                  <button
                    className={`rail__item ${view === n.key ? 'is-active' : ''}`}
                    onClick={() => setView(n.key)}
                  >
                    <Icon size={18} />
                    <span>{n.label}</span>
                  </button>
                </li>
              );
            })}
          </ul>
          <div className="rail__foot">
            <div className="rail__status">
              <span className="rail__pulse" /> bağlı
            </div>
          </div>
        </nav>

        <div className="workspace">
          <header className="topbar">
            <div className="topbar__title">
              {NAV.find((n) => n.key === view)?.label}
            </div>

            <div className="topbar__user">
              <button className="region-tag" onClick={clearRegion} title="Kapsamı değiştir">
                {region === 'TR' ? <MapPin size={14} /> : <Globe2 size={14} />}
                <span>{region ? regionLabel(region) : ''}</span>
                <Repeat size={13} className="region-tag__swap" />
              </button>

              {isAdmin ? (
                <>
                  <span className="topbar__admin"><ShieldCheck size={14} /> Yönetici</span>
                  <button className="topbar__logout" onClick={logout} title="Çıkış">
                    <LogOut size={16} />
                  </button>
                </>
              ) : (
                <button className="btn btn--ghost btn--sm" onClick={() => setLoginOpen(true)}>
                  <ShieldCheck size={15} /> Yönetici girişi
                </button>
              )}
            </div>
          </header>

          <main className="content">
            {view === 'monitor' && <MonitorView />}
            {view === 'detection' && <DetectionView />}
            {view === 'disaster' && <DisasterView />}
            {view === 'report' && <ReportView />}
          </main>
        </div>
      </div>

      {loginOpen && <AdminLoginModal onClose={() => setLoginOpen(false)} />}
    </DataProvider>
  );
}
