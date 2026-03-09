import http from './http';
import type { ResponseMessage } from '../types/common';
import type { ClienteDto, CreaClienteDto } from '../types/cliente';

export async function getClientiApi(): Promise<ResponseMessage<ClienteDto[]>> {
    const response = await http.get<ResponseMessage<ClienteDto[]>>('/api/clienti');
    return response.data;
}

export async function getClienteByIdApi(id: number): Promise<ResponseMessage<ClienteDto>> {
    const response = await http.get<ResponseMessage<ClienteDto>>(`/api/clienti/${id}`);
    return response.data;
}

export async function createClienteApi(
    payload: CreaClienteDto
): Promise<ResponseMessage<ClienteDto>> {
    const response = await http.post<ResponseMessage<ClienteDto>>('/api/clienti', payload);
    return response.data;
}

export async function updateClienteApi(
    id: number,
    payload: CreaClienteDto
): Promise<ResponseMessage<ClienteDto>> {
    const response = await http.put<ResponseMessage<ClienteDto>>(`/api/clienti/${id}`, payload);
    return response.data;
}

export async function deleteClienteApi(id: number): Promise<ResponseMessage> {
    const response = await http.delete<ResponseMessage>(`/api/clienti/${id}`);
    return response.data;
}