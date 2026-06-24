import http from './http';
import type { ResponseMessage } from '../types/common';
import type { AccontiSummaryDto, AccontoMovimentoDto, CreaAccontoMovimentoDto } from '../types/acconti';

export async function getAccontiSummaryApi(params: {
    utenteId: number;
    mese: number;
    anno: number;
}): Promise<ResponseMessage<AccontiSummaryDto>> {
    const response = await http.get<ResponseMessage<AccontiSummaryDto>>('/acconti/summary', {
        params,
    });
    return response.data;
}

export async function createAccontoMovimentoApi(
    payload: CreaAccontoMovimentoDto
): Promise<ResponseMessage<AccontoMovimentoDto>> {
    const response = await http.post<ResponseMessage<AccontoMovimentoDto>>('/acconti/movimenti', payload);
    return response.data;
}

export async function deleteAccontoMovimentoApi(id: number): Promise<ResponseMessage> {
    const response = await http.delete<ResponseMessage>(`/acconti/movimenti/${id}`);
    return response.data;
}
