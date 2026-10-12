// 저장된 목차 상세. 복사 버튼으로 마크다운 형태로 복사한다.
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { outlinesApi } from '../api/outlines';
import { ApiCallError } from '../api/client';
import { DETAIL_LEVEL_OPTIONS, type Outline } from '../types';
import { Button } from '../components/Button';
import { ErrorState, LoadingState } from '../components/StatusMessage';
import pageStyles from './Page.module.css';
import styles from './OutlineGeneratePage.module.css';

export function OutlineDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [outline, setOutline] = useState<Outline | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        outlinesApi.get(Number(id), controller.signal)
            .then(setOutline)
            .catch((e: unknown) => {
                if ((e as Error).name === 'AbortError') return;
                setError(e instanceof ApiCallError ? e.message : '목차를 불러오지 못했습니다.');
            });
        return () => controller.abort();
    }, [id]);

    async function handleCopy() {
        if (!outline) return;
        // 마크다운 형태로 복사: # 주제, 그다음에 들여쓴 목록. 바로 블로그에 붙여넣을 수 있다.
        const lines = [`# ${outline.topic}`, ''];
        for (const item of outline.items) {
            const indent = '  '.repeat(Math.max(0, item.level - 1));
            lines.push(`${indent}- ${item.heading}`);
        }
        try {
            await navigator.clipboard.writeText(lines.join('\n'));
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        } catch {
            alert('복사에 실패했습니다. 브라우저가 클립보드 접근을 허용하지 않았습니다.');
        }
    }

    async function handleDelete() {
        if (!outline) return;
        if (!confirm(`'${outline.topic}' 목차를 삭제할까요?`)) return;
        try {
            await outlinesApi.remove(outline.id);
            navigate('/outlines');
        } catch (e) {
            alert(e instanceof ApiCallError ? e.message : '삭제에 실패했습니다.');
        }
    }

    if (error) return <ErrorState message={error} />;
    if (!outline) return <LoadingState />;

    const detailLabel = DETAIL_LEVEL_OPTIONS.find((o) => o.value === outline.detailLevel)?.label ?? outline.detailLevel;

    return (
        <div>
            <header className={pageStyles.header}>
                <div>
                    <h1 className={pageStyles.title}>{outline.topic}</h1>
                    <p className={pageStyles.subtitle}>
                        {detailLabel} · 항목 {outline.items.length}개
                        {outline.createdAt && ` · 저장 ${outline.createdAt}`}
                    </p>
                </div>
                <div className={pageStyles.headerActions}>
                    <Link to="/outlines"><Button variant="ghost">목록으로</Button></Link>
                    <Button variant="ghost" onClick={handleCopy}>{copied ? '복사됨 ✓' : '마크다운 복사'}</Button>
                    <Button variant="danger" onClick={handleDelete}>삭제</Button>
                </div>
            </header>

            <div className={pageStyles.card}>
                {outline.overlapNote && (
                    <div className={styles.overlapNote}>
                        <strong>중복 처리:</strong> {outline.overlapNote}
                    </div>
                )}

                <ul className={styles.outlineList}>
                    {outline.items.map((item, i) => (
                        <li key={i} className={styles.outlineItem} style={{ paddingLeft: `${(item.level - 1) * 20}px` }}>
                            <div className={styles.outlineItemHeading}>
                                <span style={{ color: 'var(--color-text-faint)' }}>•</span>
                                <span>{item.heading}</span>
                            </div>
                            {item.note && <div className={styles.outlineItemNote}>— {item.note}</div>}
                        </li>
                    ))}
                </ul>

                {outline.references.length > 0 && (
                    <div className={styles.references}>
                        <div className={styles.referencesTitle}>참고한 기존 글</div>
                        <ul className={styles.referenceList}>
                            {outline.references.map((ref) => (
                                <li key={ref.postId}>
                                    <Link to={`/posts/${ref.postId}`}>{ref.postTitle}</Link>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </div>
    );
}
