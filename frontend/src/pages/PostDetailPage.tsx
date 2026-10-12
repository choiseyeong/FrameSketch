// 글 상세 보기. 목차 전체를 들여쓰기해서 보여 준다.
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { postsApi } from '../api/posts';
import { ApiCallError } from '../api/client';
import type { Post } from '../types';
import { Button } from '../components/Button';
import { ErrorState, LoadingState } from '../components/StatusMessage';
import pageStyles from './Page.module.css';

export function PostDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const postId = Number(id);

    const [post, setPost] = useState<Post | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();
        setError(null);
        postsApi.get(postId, controller.signal)
            .then(setPost)
            .catch((e: unknown) => {
                if ((e as Error).name === 'AbortError') return;
                setError(e instanceof ApiCallError ? e.message : '글을 불러오지 못했습니다.');
            });
        return () => controller.abort();
    }, [postId]);

    async function handleDelete() {
        if (!post) return;
        if (!confirm(`'${post.title}' 글을 삭제할까요?`)) return;
        try {
            await postsApi.remove(post.id);
            navigate('/posts');
        } catch (e) {
            alert(e instanceof ApiCallError ? e.message : '삭제에 실패했습니다.');
        }
    }

    if (error) return <ErrorState message={error} />;
    if (!post) return <LoadingState />;

    return (
        <div>
            <header className={pageStyles.header}>
                <div>
                    <h1 className={pageStyles.title}>{post.title}</h1>
                    <p className={pageStyles.subtitle}>
                        목차 {post.sections.length}개 · 등록 {post.createdAt}
                    </p>
                </div>
                <div className={pageStyles.headerActions}>
                    <Link to="/posts"><Button variant="ghost">목록으로</Button></Link>
                    <Button variant="danger" onClick={handleDelete}>삭제</Button>
                </div>
            </header>

            <div className={pageStyles.card}>
                {post.url && (
                    <div className={pageStyles.field}>
                        <span className={pageStyles.label}>URL</span>
                        <a href={post.url} target="_blank" rel="noreferrer" style={{ color: 'var(--color-primary-strong)' }}>
                            {post.url}
                        </a>
                    </div>
                )}
                {post.summary && (
                    <div className={pageStyles.field}>
                        <span className={pageStyles.label}>요약</span>
                        <p style={{ color: 'var(--color-text-muted)' }}>{post.summary}</p>
                    </div>
                )}
                <div className={pageStyles.field}>
                    <span className={pageStyles.label}>목차</span>
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {post.sections.map((s, i) => (
                            <li key={i} style={{ paddingLeft: `${(s.level - 1) * 20}px`, color: s.level === 1 ? 'var(--color-text)' : 'var(--color-text-muted)' }}>
                                <span style={{ color: 'var(--color-text-faint)', marginRight: 8 }}>•</span>
                                {s.heading}
                            </li>
                        ))}
                    </ul>
                </div>
                {post.tags.length > 0 && (
                    <div className={pageStyles.field}>
                        <span className={pageStyles.label}>태그</span>
                        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                            {post.tags.map((tag) => (
                                <span key={tag} style={{
                                    padding: '2px 8px',
                                    background: 'var(--color-primary-soft)',
                                    color: '#6a5100',
                                    fontSize: 12,
                                }}>{tag}</span>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
