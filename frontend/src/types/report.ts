export type ReportMode = 'cliente' | 'dipendente' | 'cliente-giorno';

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

export interface ReportClienteGiornoDipendenteDto {
    utenteId: number;
    nome: string;
    cognome: string;
    oreTotali: number;
    costoTotale: number;
}

export interface ReportClienteGiornoDto {
    clienteId: number;
    clienteNome: string;
    data: string;
    totaleOre: number;
    totaleCosto: number;
    dettaglioDipendenti: ReportClienteGiornoDipendenteDto[];
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
