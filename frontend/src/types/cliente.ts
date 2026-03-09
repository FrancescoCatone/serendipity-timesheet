export interface ClienteDto {
    id: number;
    nome: string;
    tariffaOraria: number;
}

export interface CreaClienteDto {
    nome: string;
    tariffaOraria: number;
}