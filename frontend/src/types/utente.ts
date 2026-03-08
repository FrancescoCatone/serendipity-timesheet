export type Ruolo = 'ADMIN' | 'DIPENDENTE';

export interface UtenteDto {
    id: number;
    codiceFiscale: string;
    nome: string;
    cognome: string;
    email: string;
    ruolo: Ruolo;
}

export interface CreaUtenteDto {
    codiceFiscale: string;
    nome: string;
    cognome: string;
    email: string;
    password: string;
    ruolo: Ruolo;
}

export interface AggiornaPasswordDto {
    oldPassword: string;
    newPassword: string;
}

export interface ProfiloUtenteDto {
    codiceFiscale: string;
    nome: string;
    cognome: string;
    email: string;
    ruolo: Ruolo;
}