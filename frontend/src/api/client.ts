// 공통 fetch 래퍼.
//
// - 호출부가 응답 코드/파싱을 매번 신경 쓰지 않도록 묶는다.
// - 서버가 돌려준 한국어 에러 메시지를 그대로 UI 에 노출할 수 있게 ApiError 를 던진다.
// - 개발 중에는 Vite 가 /api 를 백엔드로 프록시하므로 호스트를 쓰지 않는다.

import type { ApiError } from '../types';

export class ApiCallError extends Error {
    status: number;
    fieldErrors: { field: string; message: string }[];

    constructor(status: number, body: ApiError | undefined) {
        super(body?.message ?? '서버와 통신 중 문제가 발생했습니다.');
        this.status = status;
        this.fieldErrors = body?.fieldErrors ?? [];
    }
}

type RequestOptions = {
    method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
    body?: unknown;
    signal?: AbortSignal;
};

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const { method = 'GET', body, signal } = options;

    const init: RequestInit = {
        method,
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        signal,
    };
    if (body !== undefined) {
        init.body = JSON.stringify(body);
    }

    let response: Response;
    try {
        response = await fetch(path, init);
    } catch (e) {
        // 네트워크가 끊기거나 백엔드가 꺼져 있을 때.
        if ((e as Error).name === 'AbortError') throw e;
        throw new ApiCallError(0, { message: '백엔드에 접속할 수 없습니다.', fieldErrors: [], timestamp: '' });
    }

    if (response.status === 204) {
        // 삭제 성공 등 본문이 없는 응답.
        return undefined as T;
    }

    const text = await response.text();
    const data = text ? (JSON.parse(text) as unknown) : undefined;

    if (!response.ok) {
        throw new ApiCallError(response.status, data as ApiError);
    }
    return data as T;
}
