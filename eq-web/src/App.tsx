import { useRegion } from './components/RegionContext';
import { RegionSelectView } from './views/RegionSelectView';
import { Shell } from './views/Shell';

// Entry flow: pick a region (Türkiye / Dünya) first, then the console opens.
// No login wall — the app is read-only until an admin signs in from the top bar.
export function App() {
  const { region } = useRegion();
  return region ? <Shell /> : <RegionSelectView />;
}
