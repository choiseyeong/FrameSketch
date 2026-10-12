// 저장된 목차 목록 화면.
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { outlinesApi } from '../api/outlines';
import { ApiCallError } from '../api/client';
import { DETAIL_LEVEL_OPTIONS, type OutlineSummary } from '../types';
import { Button } from '../components/Button';
import { EmptyState, ErrorState, LoadingState } from '../components/StatusMessage';
import pageStyles from './Page.module.css';
import listStyles from './PostListPage.module.css';

export function OutlineListPage() {
    const navigate = useNavigate();
    const [outlines, setOutlines] = useState<OutlineSummary[] | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();
        outlinesApi.list(controller.signal)
            .then(setOutlines)
            .catch((e: unknown) => {
                if ((e as Error).name === 'AbortError') return;
                setError(e instanceof ApiCallError ? e.message : '목차 목록을 불러오지 못했습니다.');
            });
        return () => controller.abort();
    }, []);

    async function handleDelete(id: number, topic: string) {
        if (!confirm(`'${topic}' 목차를 삭제할까요?`)) return;
        try {
            await outlinesApi.remove(id);
            setOutlines((current) => current?.filter((o) => o.id !== id) ?? null);
        } catch (e) {
            alert(e instanceof ApiCallError ? e.message : '삭제에 실패했습니다.');
        }
    }

    function detailLevelLabel(value: string) {
        return DETAIL_LEVEL_OPTIONS.find((o) => o.value === value)?.label ?? value;
    }

    return (
        <div>
            <header className={pageStyles.header}>
                <div>
                    <h1 className={pageStyles.title}>저장된 목차</h1>
                    <p className={pageStyles.subtitle}>지금까지 생성해 저장한 목차의 모음입니다.</p>
                </div>
                <div className={pageStyles.headerActions}>
                    <Button onClick={() => navigate('/outlines/new')}>+ 새 목차 생성</Button>
                </div>
            </header>

            {renderBody()}
        </div>
    );

    function renderBody() {
        if (error) return <ErrorState message={error} />;
        if (outlines === null) return <LoadingState />;
        if (outlines.length === 0) {
            return <EmptyState message="저장된 목차가 없습니다." hint="상단의 '새 목차 생성'으로 첫 목차를 만들어 보세요." />;
        }

        return (
            <ul className={listStyles.list}>
                {outlines.map((outline) => (
                    <li key={outline.id} className={listStyles.item}>
                        <Link to={`/outlines/${outline.id}`} className={listStyles.itemBody}>
                            <div className={listStyles.itemTitle}>{outline.topic}</div>
                            <div className={listStyles.itemMeta}>
                                <span>{detailLevelLabel(outline.detailLevel)}</span>
                                <span>항목 {outline.itemCount}개</span>
                                {outline.referenceCount > 0 && <span>참고 글 {outline.referenceCount}개</span>}
                                <span style={{ color: 'var(--color-text-faint)' }}>{outline.createdAt}</span>
                            </div>
                        </Link>
                        <div className={listStyles.itemActions}>
                            <Button variant="danger" size="small" onClick={() => handleDelete(outline.id, outline.topic)}>삭제</Button>
                        </div>
                    </li>
                ))}
            </ul>
        );
    }
}
