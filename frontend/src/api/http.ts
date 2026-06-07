import axios from 'axios';
import { getValidToken, logout } from '../utils/auth';

const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

http.interceptors.request.use((config) => {
    const token = getValidToken();

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

http.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            logout();

            const requestUrl = String(error.config?.url ?? '');
            const isAuthRequest = requestUrl.includes('/auth/');

            if (!isAuthRequest && window.location.pathname !== '/login') {
                window.location.replace('/login');
            }
        }

        return Promise.reject(error);
    }
);

export default http;
