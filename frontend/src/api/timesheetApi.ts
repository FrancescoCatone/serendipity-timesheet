import http from './http';
import type { ResponseMessage } from '../types/common';
import type {
    CreaTimesheetDto,
    TimesheetDto,
    TotaleClienteDto,
    TotaliDto,
} from '../types/timesheet.ts';

function extractFilename(contentDispositionHeader?: string): string {
    if (!contentDispositionHeader) {
        return 'timesheet.pdf';
    }

    const utf8Match = contentDispositionHeader.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
        return decodeURIComponent(utf8Match[1]);
    }

    const asciiMatch = contentDispositionHeader.match(/filename="?([^"]+)"?/i);
    if (asciiMatch?.[1]) {
        return asciiMatch[1];
    }

    return 'timesheet.pdf';
}

export async function getTimesheetApi(): Promise<ResponseMessage<TimesheetDto[]>> {
    const response = await http.get<ResponseMessage<TimesheetDto[]>>('/timesheets');
    return response.data;
}

export async function getTimesheetByIdApi(id: number): Promise<ResponseMessage<TimesheetDto>> {
    const response = await http.get<ResponseMessage<TimesheetDto>>(`/timesheets/${id}`);
    return response.data;
}

export async function createTimesheetApi(
    payload: CreaTimesheetDto
): Promise<ResponseMessage<TimesheetDto>> {
    const response = await http.post<ResponseMessage<TimesheetDto>>('/timesheets', payload);
    return response.data;
}

export async function updateTimesheetApi(
    id: number,
    payload: CreaTimesheetDto
): Promise<ResponseMessage<TimesheetDto>> {
    const response = await http.put<ResponseMessage<TimesheetDto>>(`/timesheets/${id}`, payload);
    return response.data;
}

export async function deleteTimesheetApi(id: number): Promise<ResponseMessage> {
    const response = await http.delete<ResponseMessage>(`/timesheets/${id}`);
    return response.data;
}

export async function searchTimesheetApi(params: {
    mese?: number;
    anno?: number;
    utenteId?: number;
}): Promise<ResponseMessage<TimesheetDto[]>> {
    const response = await http.get<ResponseMessage<TimesheetDto[]>>('/timesheets/search', {
        params,
    });
    return response.data;
}

export async function getAnniTimesheetApi(): Promise<ResponseMessage<number[]>> {
    const response = await http.get<ResponseMessage<number[]>>('/timesheets/anni');
    return response.data;
}

export async function confermaTimesheetApi(id: number): Promise<ResponseMessage<TimesheetDto>> {
    const response = await http.put<ResponseMessage<TimesheetDto>>(`/timesheets/${id}/conferma`);
    return response.data;
}

export async function riapriTimesheetApi(id: number): Promise<ResponseMessage<TimesheetDto>> {
    const response = await http.put<ResponseMessage<TimesheetDto>>(`/timesheets/${id}/riapri`);
    return response.data;
}

export async function chiudiTimesheetApi(id: number): Promise<ResponseMessage<TimesheetDto>> {
    const response = await http.put<ResponseMessage<TimesheetDto>>(`/timesheets/${id}/chiudi`);
    return response.data;
}

export async function getTotaliTimesheetApi(
    id: number,
    perCliente = false
): Promise<ResponseMessage<TotaliDto | TotaleClienteDto[]>> {
    const response = await http.get<ResponseMessage<TotaliDto | TotaleClienteDto[]>>(
        `/timesheets/${id}/totali`,
        { params: { perCliente } }
    );
    return response.data;
}

export async function exportTimesheetPdfApi(id: number): Promise<{
    blob: Blob;
    filename: string;
}> {
    const response = await http.get<Blob>(`/timesheets/${id}/export`, {
        responseType: 'blob',
    });

    return {
        blob: response.data,
        filename: extractFilename(response.headers['content-disposition']),
    };
}
