import axios from 'axios';
import { getToken, removeToken } from '../utils/storage';

const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

http.interceptors.request.use((config) => {
    const token = getToken();

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

http.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            removeToken();
        }

        return Promise.reject(error);
    }
);

export default http;
