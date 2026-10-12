// 목차 생성·저장 API.
import { request } from './client';
import type { DetailLevel, Outline, OutlineSummary } from '../types';

export interface GenerateInput {
    topic: string;
    detailLevel: DetailLevel;
}

export interface SaveInput {
    topic: string;
    detailLevel: DetailLevel;
    overlapNote: string | null;
    items: { heading: string; level: number; note: string | null }[];
    references: { postId: number; postTitle: string; overlapMemo: string | null }[];
}

export const outlinesApi = {
    generate(body: GenerateInput, signal?: AbortSignal): Promise<Outline> {
        return request<Outline>('/api/outlines/generate', { method: 'POST', body, signal });
    },

    save(body: SaveInput): Promise<Outline> {
        return request<Outline>('/api/outlines', { method: 'POST', body });
    },

    list(signal?: AbortSignal): Promise<OutlineSummary[]> {
        return request<OutlineSummary[]>('/api/outlines', { signal });
    },

    get(id: number, signal?: AbortSignal): Promise<Outline> {
        return request<Outline>(`/api/outlines/${id}`, { signal });
    },

    remove(id: number): Promise<void> {
        return request<void>(`/api/outlines/${id}`, { method: 'DELETE' });
    },
};
