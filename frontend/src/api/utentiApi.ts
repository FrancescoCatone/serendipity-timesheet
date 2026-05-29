import http from './http';
import type { ResponseMessage } from '../types/common';
import type {
    AggiornaPasswordDto,
    CreaUtenteDto,
    ProfiloUtenteDto,
    UtenteDto,
} from '../types/utente';

type UtentiMeta = {
    count: number;
};

export async function getUtentiApi(): Promise<ResponseMessage<UtenteDto[], UtentiMeta>> {
    const response = await http.get<ResponseMessage<UtenteDto[], UtentiMeta>>('/utenti');
    return response.data;
}

export async function getUtenteByIdApi(id: number): Promise<ResponseMessage<UtenteDto>> {
    const response = await http.get<ResponseMessage<UtenteDto>>(`/utenti/id/${id}`);
    return response.data;
}

export async function getMyProfileApi(): Promise<ResponseMessage<ProfiloUtenteDto>> {
    const response = await http.get<ResponseMessage<ProfiloUtenteDto>>('/utenti/me');
    return response.data;
}

export async function createUtenteApi(
    payload: CreaUtenteDto
): Promise<ResponseMessage<UtenteDto>> {
    const response = await http.post<ResponseMessage<UtenteDto>>('/utenti', payload);
    return response.data;
}

export async function updateUtenteApi(
    id: number,
    payload: CreaUtenteDto
): Promise<ResponseMessage<UtenteDto>> {
    const response = await http.put<ResponseMessage<UtenteDto>>(`/utenti/${id}`, payload);
    return response.data;
}

export async function deleteUtenteApi(id: number): Promise<ResponseMessage> {
    const response = await http.delete<ResponseMessage>(`/utenti/${id}`);
    return response.data;
}

export async function changeMyPasswordApi(
    payload: AggiornaPasswordDto
): Promise<ResponseMessage> {
    const response = await http.patch<ResponseMessage>('/utenti/me/password', payload);
    return response.data;
}
