export const STRICT_EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;

export function isValidEmail(value: string): boolean {
    return STRICT_EMAIL_REGEX.test(value.trim());
}
