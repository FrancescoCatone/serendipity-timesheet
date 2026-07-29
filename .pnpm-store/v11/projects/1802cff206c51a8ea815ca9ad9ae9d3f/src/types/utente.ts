export type Ruolo = 'ADMIN' | 'DIPENDENTE';

export interface UtenteDto {
    id: number;
    codiceFiscale: string;
    nome: string;
    cognome: string;
    email: string;
    ruolo: Ruolo;
    pagaOraria: number | null;
}

export interface CreaUtenteDto {
    codiceFiscale: string;
    nome: string;
    cognome: string;
    email: string;
    password: string;
    ruolo: Ruolo;
    pagaOraria?: number | null;
}

export interface AggiornaPasswordDto {
    oldPassword: string;
    newPassword: string;
}

export interface ProfiloUtenteDto {
    id: number;
    codiceFiscale: string;
    nome: string;
    cognome: string;
    email: string;
    ruolo: Ruolo;
}
