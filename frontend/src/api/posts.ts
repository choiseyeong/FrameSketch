// 기존 글 관리 API.
import { request } from './client';
import type { Post, PostInput, PostSummary } from '../types';

export const postsApi = {
    list(query?: string, signal?: AbortSignal): Promise<PostSummary[]> {
        const path = query && query.trim()
            ? `/api/posts?q=${encodeURIComponent(query.trim())}`
            : '/api/posts';
        return request<PostSummary[]>(path, { signal });
    },

    get(id: number, signal?: AbortSignal): Promise<Post> {
        return request<Post>(`/api/posts/${id}`, { signal });
    },

    create(body: PostInput): Promise<Post> {
        return request<Post>('/api/posts', { method: 'POST', body });
    },

    update(id: number, body: PostInput): Promise<Post> {
        return request<Post>(`/api/posts/${id}`, { method: 'PUT', body });
    },

    remove(id: number): Promise<void> {
        return request<void>(`/api/posts/${id}`, { method: 'DELETE' });
    },
};
