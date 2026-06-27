import * as Sentry from '@sentry/react';
import { getCurrentUserProfile } from '../utils/auth';

const dsn = (import.meta.env.VITE_SENTRY_DSN ?? '').trim();

export function initializeSentry() {
    if (!import.meta.env.PROD || !dsn) {
        return;
    }

    Sentry.init({
        dsn,
        environment: import.meta.env.VITE_SENTRY_ENVIRONMENT || import.meta.env.MODE,
        release: import.meta.env.VITE_SENTRY_RELEASE || undefined,
        sendDefaultPii: false,
    });
}

export function syncSentryUser() {
    if (!dsn) {
        return;
    }

    const profile = getCurrentUserProfile();

    if (!profile.email && !profile.ruolo) {
        Sentry.setUser(null);
        return;
    }

    Sentry.setUser({
        email: profile.email ?? undefined,
        username: [profile.nome, profile.cognome].filter(Boolean).join(' ') || undefined,
    });

    if (profile.ruolo) {
        Sentry.setTag('role', profile.ruolo);
    }
}
