export interface ResponseMessage<T = unknown, M = Record<string, unknown>> {
    status: number;
    message: string;
    data?: T;
    meta?: M;
}