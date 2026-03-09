export type TimesheetStato = 'APERTO' | 'CONFERMATO' | 'CHIUSO';

export interface TimesheetDto {
    id: number;
    mese: number;
    anno: number;
    dataCompilazione: string | null;
    utenteId: number;
    utenteNomeCompleto: string;
    stato: TimesheetStato;
}

export interface CreaTimesheetDto {
    mese: number;
    anno: number;
    utenteId: number;
}

export interface TotaliDto {
    totaleOrario: number;
    totaleCosto: number;
}

export interface TotaleClienteDto {
    clienteId: number;
    clienteNome: string;
    orario: number;
    costo: number;
}