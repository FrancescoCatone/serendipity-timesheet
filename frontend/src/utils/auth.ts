import { getToken, removeToken } from './storage';

export type AppRole = 'ADMIN' | 'DIPENDENTE' | null;

export interface CurrentUserProfile {
    email: string | null;
    ruolo: AppRole;
    nome: string | null;
    cognome: string | null;
    codiceFiscale: string | null;
}

export function isAuthenticated(): boolean {
    return !!getToken();
}

export function logout(): void {
    removeToken();
}

function parseJwtPayload(token: string): Record<string, unknown> | null {
    try {
        const parts = token.split('.');
        if (parts.length < 2) {
            return null;
        }

        const base64Url = parts[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');

        const json = decodeURIComponent(
            atob(padded)
                .split('')
                .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
                .join('')
        );

        return JSON.parse(json);
    } catch {
        return null;
    }
}

function normalizeRole(value: string): AppRole {
    const cleaned = value.replace('ROLE_', '').trim().toUpperCase();

    if (cleaned === 'ADMIN' || cleaned === 'DIPENDENTE') {
        return cleaned;
    }

    return null;
}

function extractRole(value: unknown): AppRole {
    if (typeof value === 'string') {
        const parts = value.split(/[,\s]+/).filter(Boolean);

        for (const part of parts) {
            const role = normalizeRole(part);
            if (role) {
                return role;
            }
        }
    }

    if (Array.isArray(value)) {
        for (const item of value) {
            if (typeof item === 'string') {
                const role = normalizeRole(item);
                if (role) {
                    return role;
                }
            }

            if (
                item &&
                typeof item === 'object' &&
                'authority' in item &&
                typeof item.authority === 'string'
            ) {
                const role = normalizeRole(item.authority);
                if (role) {
                    return role;
                }
            }
        }
    }

    if (
        value &&
        typeof value === 'object' &&
        'authority' in value &&
        typeof value.authority === 'string'
    ) {
        return normalizeRole(value.authority);
    }

    return null;
}

function getStringClaim(
    payload: Record<string, unknown>,
    keys: string[]
): string | null {
    for (const key of keys) {
        const value = payload[key];
        if (typeof value === 'string' && value.trim()) {
            return value.trim();
        }
    }

    return null;
}

export function getCurrentUserRole(): AppRole {
    const token = getToken();
    if (!token) {
        return null;
    }

    const payload = parseJwtPayload(token);
    if (!payload) {
        return null;
    }

    const claimCandidates = [
        payload.ruolo,
        payload.role,
        payload.roles,
        payload.authorities,
        payload.scope,
        payload.scopes,
    ];

    for (const claim of claimCandidates) {
        const role = extractRole(claim);
        if (role) {
            return role;
        }
    }

    return null;
}

export function hasRole(allowedRoles: Exclude<AppRole, null>[]): boolean {
    const currentRole = getCurrentUserRole();
    return currentRole !== null && allowedRoles.includes(currentRole);
}

export function isAdmin(): boolean {
    return hasRole(['ADMIN']);
}

export function getCurrentUserProfile(): CurrentUserProfile {
    const token = getToken();
    if (!token) {
        return {
            email: null,
            ruolo: null,
            nome: null,
            cognome: null,
            codiceFiscale: null,
        };
    }

    const payload = parseJwtPayload(token);
    if (!payload) {
        return {
            email: null,
            ruolo: null,
            nome: null,
            cognome: null,
            codiceFiscale: null,
        };
    }

    return {
        email: getStringClaim(payload, ['email', 'sub', 'username']),
        ruolo: getCurrentUserRole(),
        nome: getStringClaim(payload, ['nome', 'name', 'given_name', 'firstName']),
        cognome: getStringClaim(payload, ['cognome', 'family_name', 'lastName', 'surname']),
        codiceFiscale: getStringClaim(payload, ['codiceFiscale', 'codice_fiscale', 'cf']),
    };
}