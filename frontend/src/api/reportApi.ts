import http from './http';
import type { ResponseMessage } from '../types/common';
import type { ReportClienteDto, ReportClienteGiornoDto, ReportDipendenteDto } from '../types/report';

type ReportClienteParams = {
    clienteId: number;
    mese?: number;
    anno?: number;
};

type ReportDipendenteParams = {
    utenteId: number;
    mese?: number;
    anno?: number;
};

type ReportClienteGiornoParams = {
    clienteId: number;
    data: string;
};

function extractFilename(contentDispositionHeader?: string): string {
    if (!contentDispositionHeader) {
        return 'report-cliente.pdf';
    }

    const utf8Match = contentDispositionHeader.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
        return decodeURIComponent(utf8Match[1]);
    }

    const asciiMatch = contentDispositionHeader.match(/filename="?([^"]+)"?/i);
    if (asciiMatch?.[1]) {
        return asciiMatch[1];
    }

    return 'report-cliente.pdf';
}

export async function getReportClienteApi(params: ReportClienteParams) {
    const response = await http.get<ResponseMessage<ReportClienteDto>>('/report/cliente', {
        params,
    });

    return response.data;
}

export async function getReportDipendenteApi(params: ReportDipendenteParams) {
    const response = await http.get<ResponseMessage<ReportDipendenteDto>>('/report/dipendente', {
        params,
    });

    return response.data;
}

export async function getReportClienteGiornoApi(params: ReportClienteGiornoParams) {
    const response = await http.get<ResponseMessage<ReportClienteGiornoDto>>('/report/cliente/giorno', {
        params,
    });

    return response.data;
}

export async function exportReportClientePdfApi(params: {
    clienteId: number;
    mese?: number;
    anno: number;
}): Promise<{ blob: Blob; filename: string }> {
    const response = await http.get<Blob>('/report/cliente/export', {
        params,
        responseType: 'blob',
    });

    return {
        blob: response.data,
        filename: extractFilename(response.headers['content-disposition']),
    };
}
