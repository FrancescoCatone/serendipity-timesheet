export type ReportMode = 'cliente' | 'dipendente';

export interface ReportClienteDipendenteDto {
    utenteId: number;
    nome: string;
    cognome: string;
    oreTotali: number;
    costoTotale: number;
}

export interface ReportClienteDto {
    clienteId: number;
    clienteNome: string;
    mese: number | null;
    anno: number | null;
    totaleOre: number;
    totaleCosto: number;
    dettaglioDipendenti: ReportClienteDipendenteDto[];
}

export interface ReportDipendenteClienteDto {
    clienteId: number;
    clienteNome: string;
    oreTotali: number;
    costoTotale: number;
}

export interface ReportDipendenteDto {
    utenteId: number;
    nome: string;
    cognome: string;
    mese: number | null;
    anno: number | null;
    totaleOre: number;
    totaleCosto: number;
    dettaglioClienti: ReportDipendenteClienteDto[];
}