// 백엔드 응답 타입. 서버 DTO 와 1:1 로 맞춘다.
// 서버가 바뀌면 여기도 함께 고치고, 두 쪽을 다른 모양으로 쓰지 않는다.

export type DetailLevel = 'SIMPLE' | 'STANDARD' | 'DETAILED';

export const DETAIL_LEVEL_OPTIONS: { value: DetailLevel; label: string; hint: string }[] = [
    { value: 'SIMPLE',    label: '간단', hint: '1단계 · 약 5개' },
    { value: 'STANDARD',  label: '보통', hint: '2단계 · 약 8개' },
    { value: 'DETAILED',  label: '상세', hint: '3단계 · 약 12개' },
];

// --- 글 --------------------------------------------------------------------

export interface PostSummary {
    id: number;
    title: string;
    url: string | null;
    summary: string | null;
    tags: string[];
    sectionCount: number;
    createdAt: string;
}

export interface Post {
    id: number;
    title: string;
    url: string | null;
    summary: string | null;
    sections: PostSectionResponse[];
    tags: string[];
    createdAt: string;
    updatedAt: string;
}

export interface PostSectionResponse {
    heading: string;
    level: number;
}

export interface PostInput {
    title: string;
    url?: string;
    summary?: string;
    sections: { heading: string; level: number }[];
    tags?: string[];
}

// --- 목차 ------------------------------------------------------------------

export interface OutlineSummary {
    id: number;
    topic: string;
    detailLevel: DetailLevel;
    itemCount: number;
    referenceCount: number;
    createdAt: string;
}

export interface Outline {
    id: number;
    topic: string;
    detailLevel: DetailLevel;
    overlapNote: string | null;
    items: OutlineItem[];
    references: OutlineReference[];
    createdAt: string | null;
}

export interface OutlineItem {
    heading: string;
    level: number;
    note: string | null;
}

export interface OutlineReference {
    postId: number;
    postTitle: string;
    overlapMemo: string | null;
}

// --- 오류 ------------------------------------------------------------------

export interface ApiError {
    message: string;
    fieldErrors: { field: string; message: string }[];
    timestamp: string;
}
