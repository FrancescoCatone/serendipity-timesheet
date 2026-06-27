import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import * as Sentry from '@sentry/react';
import { ToastContainer } from 'react-toastify';
import { syncSentryUser } from './lib/sentry';
import AppRouter from './router/AppRouter';

function SentryUserSync() {
  const location = useLocation();

  useEffect(() => {
    syncSentryUser();
    Sentry.setTag('route', `${location.pathname}${location.search}`);
  }, [location.pathname, location.search]);

  return null;
}

function AppCrashFallback() {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Errore imprevisto</h1>
        <p>La pagina ha riscontrato un problema. Puoi ricaricare l'applicazione e riprovare.</p>
        <button
          type="button"
          className="primary-button"
          onClick={() => window.location.reload()}
        >
          Ricarica applicazione
        </button>
      </div>
    </div>
  );
}

function App() {
  return (
    <>
      <SentryUserSync />
      <Sentry.ErrorBoundary fallback={<AppCrashFallback />}>
        <AppRouter />
      </Sentry.ErrorBoundary>
      <ToastContainer
        position="top-right"
        autoClose={3000}
        closeOnClick
        pauseOnHover
        theme="light"
      />
    </>
  );
}

export default App;
