export interface AccontoMovimentoDto {
    id: number;
    utenteId: number;
    utenteNome: string;
    utenteCognome: string;
    mese: number;
    anno: number;
    importo: number;
    note: string | null;
    dataMovimento: string;
    createdAt: string;
}

export interface AccontiSummaryDto {
    utenteId: number;
    utenteNome: string;
    utenteCognome: string;
    mese: number;
    anno: number;
    timesheetStato: string | null;
    maturato: number;
    totaleAcconti: number;
    totaleMovimenti: number;
    saldoResiduo: number;
    movimenti: AccontoMovimentoDto[];
}

export interface CreaAccontoMovimentoDto {
    utenteId: number;
    mese: number;
    anno: number;
    importo: number;
    note?: string;
    dataMovimento: string;
}
