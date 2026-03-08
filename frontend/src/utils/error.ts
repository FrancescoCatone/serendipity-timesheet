import axios from 'axios';
import type { ResponseMessage } from '../types/common';

export function getErrorMessage(
    error: unknown,
    fallback = 'Si è verificato un errore'
): string {
    if (axios.isAxiosError(error)) {
        const responseData = error.response?.data as ResponseMessage | string | undefined;

        if (typeof responseData === 'string' && responseData.trim()) {
            return responseData;
        }

        if (
            responseData &&
            typeof responseData === 'object' &&
            'message' in responseData &&
            typeof responseData.message === 'string' &&
            responseData.message.trim()
        ) {
            return responseData.message;
        }
    }

    if (error instanceof Error && error.message.trim()) {
        return error.message;
    }

    return fallback;
}