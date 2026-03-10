import http from './http';
import type { ResponseMessage } from '../types/common';
import type { CreaTimesheetRigaDto, TimesheetRigaDto } from '../types/timesheetRiga';

export async function getTimesheetRigheApi(): Promise<ResponseMessage<TimesheetRigaDto[]>> {
    const response = await http.get<ResponseMessage<TimesheetRigaDto[]>>('/api/timesheet-righe');
    return response.data;
}

export async function getTimesheetRigaByIdApi(
    id: number
): Promise<ResponseMessage<TimesheetRigaDto>> {
    const response = await http.get<ResponseMessage<TimesheetRigaDto>>(
        `/api/timesheet-righe/${id}`
    );
    return response.data;
}

export async function getTimesheetRigheByTimesheetApi(
    timesheetId: number
): Promise<ResponseMessage<TimesheetRigaDto[]>> {
    const response = await http.get<ResponseMessage<TimesheetRigaDto[]>>(
        `/api/timesheet-righe/by-timesheet/${timesheetId}`
    );
    return response.data;
}

export async function createTimesheetRigaApi(
    payload: CreaTimesheetRigaDto
): Promise<ResponseMessage<TimesheetRigaDto>> {
    const response = await http.post<ResponseMessage<TimesheetRigaDto>>(
        '/api/timesheet-righe',
        payload
    );
    return response.data;
}

export async function updateTimesheetRigaApi(
    id: number,
    payload: CreaTimesheetRigaDto
): Promise<ResponseMessage<TimesheetRigaDto>> {
    const response = await http.put<ResponseMessage<TimesheetRigaDto>>(
        `/api/timesheet-righe/${id}`,
        payload
    );
    return response.data;
}

export async function deleteTimesheetRigaApi(id: number): Promise<ResponseMessage> {
    const response = await http.delete<ResponseMessage>(`/api/timesheet-righe/${id}`);
    return response.data;
}

export async function filterTimesheetRigheApi(params: {
    clienteId?: number;
    utenteId?: number;
    data?: string;
}): Promise<ResponseMessage<TimesheetRigaDto[]>> {
    const response = await http.get<ResponseMessage<TimesheetRigaDto[]>>(
        '/api/timesheet-righe/filter',
        {
            params,
        }
    );

    return response.data;
}