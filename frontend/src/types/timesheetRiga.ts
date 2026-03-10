export interface TimesheetRigaDto {
    id: number;
    timesheetId: number;
    clienteId: number;
    clienteNome: string;
    data: string;
    ore: number;
    minuti: number;
    orario: number;
    costoOrario: number;
}

export interface CreaTimesheetRigaDto {
    timesheetId: number;
    clienteId: number;
    data: string;
    ore: number;
    minuti: number;
}