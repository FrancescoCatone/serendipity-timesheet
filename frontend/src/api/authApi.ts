import http from './http';
import type { AuthRequest, AuthResponse } from '../types/auth';

export async function loginApi(payload: AuthRequest): Promise<AuthResponse> {
    const response = await http.post<AuthResponse>('/auth/login', payload);
    return response.data;
}