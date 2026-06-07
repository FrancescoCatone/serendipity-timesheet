import type { ClienteDto } from '../types/cliente';
import type { UtenteDto } from '../types/utente';

export const NON_LAVORATO_CLIENT_NAME = 'NON LAVORATO';

export function isAdminUser(utente: Pick<UtenteDto, 'ruolo'>): boolean {
    return utente.ruolo === 'ADMIN';
}

export function excludeAdminUsers<T extends Pick<UtenteDto, 'ruolo'>>(utenti: T[]): T[] {
    return utenti.filter((utente) => !isAdminUser(utente));
}

export function isNonLavoratoCliente(cliente: Pick<ClienteDto, 'nome'>): boolean {
    return cliente.nome.trim().toUpperCase() === NON_LAVORATO_CLIENT_NAME;
}

export function excludeNonLavoratoClienti<T extends Pick<ClienteDto, 'nome'>>(clienti: T[]): T[] {
    return clienti.filter((cliente) => !isNonLavoratoCliente(cliente));
}
