export type TimesheetStato = 'APERTO' | 'CONFERMATO' | 'CHIUSO';

export interface TimesheetListMeta {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

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
