// 등록한 글 목록 화면.
// - 검색어로 필터링 (서버의 FTS5 검색)
// - 각 항목에서 삭제
// - 상단에서 새 글 등록 이동

import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { postsApi } from '../api/posts';
import { ApiCallError } from '../api/client';
import type { PostSummary } from '../types';
import { Button } from '../components/Button';
import { EmptyState, ErrorState, LoadingState } from '../components/StatusMessage';
import pageStyles from './Page.module.css';
import styles from './PostListPage.module.css';

export function PostListPage() {
    const navigate = useNavigate();
    const [posts, setPosts] = useState<PostSummary[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [query, setQuery] = useState('');

    useEffect(() => {
        // query 를 디바운스해서 입력할 때마다 서버를 두드리지 않게 한다.
        const controller = new AbortController();
        const timer = setTimeout(() => {
            setError(null);
            postsApi.list(query, controller.signal)
                .then(setPosts)
                .catch((e: unknown) => {
                    if ((e as Error).name === 'AbortError') return;
                    setError(e instanceof ApiCallError ? e.message : '글 목록을 불러오지 못했습니다.');
                });
        }, 200);

        return () => {
            controller.abort();
            clearTimeout(timer);
        };
    }, [query]);

    async function handleDelete(id: number, title: string) {
        if (!confirm(`'${title}' 글을 삭제할까요? 되돌릴 수 없습니다.`)) return;
        try {
            await postsApi.remove(id);
            setPosts((current) => current?.filter((p) => p.id !== id) ?? null);
        } catch (e) {
            alert(e instanceof ApiCallError ? e.message : '삭제에 실패했습니다.');
        }
    }

    return (
        <div>
            <header className={pageStyles.header}>
                <div>
                    <h1 className={pageStyles.title}>기존 글</h1>
                    <p className={pageStyles.subtitle}>
                        지금까지 등록한 학습 글의 제목과 목차 모음. 목차 생성 시 여기서 중복을 검토합니다.
                    </p>
                </div>
                <div className={pageStyles.headerActions}>
                    <Button onClick={() => navigate('/posts/new')}>+ 새 글 등록</Button>
                </div>
            </header>

            <div className={styles.searchRow}>
                <input
                    type="search"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="제목, 목차, 태그 검색"
                />
            </div>

            {renderBody()}
        </div>
    );

    function renderBody() {
        if (error) return <ErrorState message={error} hint="백엔드 서버가 떠 있는지 확인해 주세요." />;
        if (posts === null) return <LoadingState />;

        if (posts.length === 0) {
            return query
                ? <EmptyState message={`'${query}' 와 일치하는 글이 없습니다.`} hint="다른 검색어를 입력해 보세요." />
                : <EmptyState message="아직 등록된 글이 없습니다." hint="상단의 '새 글 등록'으로 첫 글을 추가해 보세요." />;
        }

        return (
            <ul className={styles.list}>
                {posts.map((post) => (
                    <li key={post.id} className={styles.item}>
                        <Link to={`/posts/${post.id}`} className={styles.itemBody}>
                            <div className={styles.itemTitle}>{post.title}</div>
                            <div className={styles.itemMeta}>
                                <span>목차 {post.sectionCount}개</span>
                                {post.tags.length > 0 && (
                                    <span className={styles.itemTags}>
                                        {post.tags.map((tag) => (
                                            <span key={tag} className={styles.tag}>{tag}</span>
                                        ))}
                                    </span>
                                )}
                            </div>
                        </Link>
                        <div className={styles.itemActions}>
                            <Button
                                variant="danger"
                                size="small"
                                onClick={() => handleDelete(post.id, post.title)}
                            >
                                삭제
                            </Button>
                        </div>
                    </li>
                ))}
            </ul>
        );
    }
}
