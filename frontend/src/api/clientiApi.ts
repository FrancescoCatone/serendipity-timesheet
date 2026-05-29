import http from './http';
import type { ResponseMessage } from '../types/common';
import type { ClienteDto, CreaClienteDto } from '../types/cliente';

export async function getClientiApi(): Promise<ResponseMessage<ClienteDto[]>> {
    const response = await http.get<ResponseMessage<ClienteDto[]>>('/clienti');
    return response.data;
}

export async function getClienteByIdApi(id: number): Promise<ResponseMessage<ClienteDto>> {
    const response = await http.get<ResponseMessage<ClienteDto>>(`/clienti/${id}`);
    return response.data;
}

export async function createClienteApi(
    payload: CreaClienteDto
): Promise<ResponseMessage<ClienteDto>> {
    const response = await http.post<ResponseMessage<ClienteDto>>('/clienti', payload);
    return response.data;
}

export async function updateClienteApi(
    id: number,
    payload: CreaClienteDto
): Promise<ResponseMessage<ClienteDto>> {
    const response = await http.put<ResponseMessage<ClienteDto>>(`/clienti/${id}`, payload);
    return response.data;
}

export async function deleteClienteApi(id: number): Promise<ResponseMessage> {
    const response = await http.delete<ResponseMessage>(`/clienti/${id}`);
    return response.data;
}
