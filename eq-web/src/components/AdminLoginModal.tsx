import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { useToast } from './Toast';
import { Button, Field } from './ui';
import { ApiError } from '../api/client';
import { ShieldCheck, X } from 'lucide-react';

// Admin sign-in surfaced from the top bar. The console works read-only without it;
// signing in unlocks importing feeds, running detection, and deleting events.
export function AdminLoginModal({ onClose }: { onClose: () => void }) {
  const { login } = useAuth();
  const toast = useToast();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (!username || !password) return;
    setBusy(true);
    try {
      await login(username, password);
      toast.success('Yönetici girişi yapıldı');
      onClose();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Giriş başarısız.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal__head">
          <div className="modal__title"><ShieldCheck size={18} /> Yönetici girişi</div>
          <button className="modal__close" onClick={onClose} title="Kapat"><X size={16} /></button>
        </div>

        <form
          className="modal__form"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <Field
            label="Kullanıcı adı"
            value={username}
            autoComplete="username"
            onChange={(e) => setUsername(e.target.value)}
            placeholder="admin"
          />
          <Field
            label="Parola"
            type="password"
            value={password}
            autoComplete="current-password"
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />
          <Button type="submit" loading={busy} disabled={!username || !password}>
            Giriş yap
          </Button>
        </form>

        <p className="modal__note">
          Giriş yapmadan izleme, rapor ve değerlendirme açıktır. Yönetici girişi içe aktarma,
          algılama ve silme işlemlerini açar.
        </p>
      </div>
    </div>
  );
}
