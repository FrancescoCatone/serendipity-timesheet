import http from './http';
import type { ResponseMessage } from '../types/common';
import type { ReportClienteDto, ReportDipendenteDto } from '../types/report';

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

export async function getReportClienteApi(params: ReportClienteParams) {
    const response = await http.get<ResponseMessage<ReportClienteDto>>('/api/report/cliente', {
        params,
    });

    return response.data;
}

export async function getReportDipendenteApi(params: ReportDipendenteParams) {
    const response = await http.get<ResponseMessage<ReportDipendenteDto>>('/api/report/dipendente', {
        params,
    });

    return response.data;
}