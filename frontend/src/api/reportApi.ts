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
