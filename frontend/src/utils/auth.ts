import { getToken, removeToken } from './storage';

export function isAuthenticated(): boolean {
    return !!getToken();
}

export function logout(): void {
    removeToken();
}