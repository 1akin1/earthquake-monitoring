import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import { AuthProvider } from './auth/AuthContext';
import { RegionProvider } from './components/RegionContext';
import { ToastProvider } from './components/Toast';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ToastProvider>
      <AuthProvider>
        <RegionProvider>
          <App />
        </RegionProvider>
      </AuthProvider>
    </ToastProvider>
  </React.StrictMode>,
);
