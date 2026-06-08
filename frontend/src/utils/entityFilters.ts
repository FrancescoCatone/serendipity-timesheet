import type { ClienteDto } from '../types/cliente';
import type { UtenteDto } from '../types/utente';

export const NON_LAVORATO_CLIENT_NAME = 'NON LAVORATO';

export function isAdminUser(utente: Pick<UtenteDto, 'ruolo'>): boolean {
    return utente.ruolo.trim().toUpperCase() === 'ADMIN';
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

export function sortUsersByDisplayName<T extends Pick<UtenteDto, 'nome' | 'cognome'>>(utenti: T[]): T[] {
    return [...utenti].sort((a, b) => {
        const aLabel = `${a.nome} ${a.cognome}`.trim();
        const bLabel = `${b.nome} ${b.cognome}`.trim();
        return aLabel.localeCompare(bLabel, 'it', { sensitivity: 'base' });
    });
}

export function sortClientiByName<T extends Pick<ClienteDto, 'nome'>>(clienti: T[]): T[] {
    return [...clienti].sort((a, b) =>
        a.nome.localeCompare(b.nome, 'it', { sensitivity: 'base' })
    );
}

export function sortClientiForTimesheetRows<T extends Pick<ClienteDto, 'nome'>>(clienti: T[]): T[] {
    return [...clienti].sort((a, b) => {
        const aIsNonLavorato = isNonLavoratoCliente(a);
        const bIsNonLavorato = isNonLavoratoCliente(b);

        if (aIsNonLavorato && !bIsNonLavorato) {
            return -1;
        }

        if (!aIsNonLavorato && bIsNonLavorato) {
            return 1;
        }

        return a.nome.localeCompare(b.nome, 'it', { sensitivity: 'base' });
    });
}
