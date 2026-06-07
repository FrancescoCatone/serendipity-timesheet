export const SYSTEM_OPERATOR_EMAIL = 'system.operator@serendipitycoop.it';

export function isSystemOperatorEmail(email: string | null | undefined): boolean {
    return (email ?? '').trim().toLowerCase() === SYSTEM_OPERATOR_EMAIL;
}
