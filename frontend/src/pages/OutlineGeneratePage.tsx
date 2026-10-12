// 목차 생성 화면. 이 서비스의 주 사용 흐름.
//
// 흐름: 주제 입력 → 상세도 선택 → '생성' → 결과 표시 → ('저장' 또는 '다시 생성')
// 생성과 저장은 서버에서도 두 엔드포인트로 나뉘어 있어 중간 결과를 자유롭게 재생성한다.

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { outlinesApi } from '../api/outlines';
import { ApiCallError } from '../api/client';
import type { DetailLevel, Outline } from '../types';
import { DETAIL_LEVEL_OPTIONS } from '../types';
import { Button } from '../components/Button';
import { LoadingState } from '../components/StatusMessage';
import pageStyles from './Page.module.css';
import styles from './OutlineGeneratePage.module.css';

export function OutlineGeneratePage() {
    const navigate = useNavigate();

    const [topic, setTopic] = useState('');
    const [detailLevel, setDetailLevel] = useState<DetailLevel>('STANDARD');

    const [generating, setGenerating] = useState(false);
    const [saving, setSaving] = useState(false);
    const [result, setResult] = useState<Outline | null>(null);
    const [error, setError] = useState<string | null>(null);

    async function handleGenerate(e: React.FormEvent) {
        e.preventDefault();
        if (!topic.trim()) return;

        setError(null);
        setResult(null);
        setGenerating(true);
        try {
            const outline = await outlinesApi.generate({ topic: topic.trim(), detailLevel });
            setResult(outline);
        } catch (e) {
            if (e instanceof ApiCallError) {
                // Gemini 과부하(503)는 사용자가 재시도하면 되는 경우가 많다.
                const hint = e.status === 503 ? ' 잠시 후 다시 시도해 주세요.' : '';
                setError(e.message + hint);
            } else {
                setError('목차를 생성하지 못했습니다.');
            }
        } finally {
            setGenerating(false);
        }
    }

    async function handleSave() {
        if (!result) return;
        setSaving(true);
        try {
            const saved = await outlinesApi.save({
                topic: result.topic,
                detailLevel: result.detailLevel,
                overlapNote: result.overlapNote,
                items: result.items,
                references: result.references,
            });
            navigate(`/outlines/${saved.id}`);
        } catch (e) {
            alert(e instanceof ApiCallError ? e.message : '저장에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div>
            <header className={pageStyles.header}>
                <div>
                    <h1 className={pageStyles.title}>목차 생성</h1>
                    <p className={pageStyles.subtitle}>
                        주제와 상세도를 입력하면 기존 글과 중복되지 않도록 목차를 설계해 드립니다.
                    </p>
                </div>
            </header>

            <form className={pageStyles.card} onSubmit={handleGenerate}>
                <div className={pageStyles.field}>
                    <label className={pageStyles.label} htmlFor="topic">주제</label>
                    <input
                        id="topic"
                        className={pageStyles.input}
                        value={topic}
                        onChange={(e) => setTopic(e.target.value)}
                        placeholder="예: 운영체제 메모리 관리"
                        maxLength={500}
                    />
                </div>

                <div className={pageStyles.field}>
                    <span className={pageStyles.label}>상세도</span>
                    <div className={styles.detailOptions}>
                        {DETAIL_LEVEL_OPTIONS.map((opt) => (
                            <button
                                key={opt.value}
                                type="button"
                                onClick={() => setDetailLevel(opt.value)}
                                className={`${styles.detailOption} ${detailLevel === opt.value ? styles.selected : ''}`}
                                aria-pressed={detailLevel === opt.value}
                            >
                                <span className={styles.detailOptionLabel}>{opt.label}</span>
                                <span className={styles.detailOptionHint}>{opt.hint}</span>
                            </button>
                        ))}
                    </div>
                </div>

                <div className={pageStyles.formActions}>
                    <Button type="submit" disabled={generating || !topic.trim()} size="large">
                        {generating ? '생성 중... (수초 ~ 1분)' : '목차 생성'}
                    </Button>
                </div>
            </form>

            {error && (
                <div className={`${pageStyles.flash} ${pageStyles.flashError}`} style={{ marginTop: 16 }}>
                    {error}
                </div>
            )}

            {generating && (
                <div style={{ marginTop: 24 }}>
                    <LoadingState message="Gemini 가 목차를 그리는 중입니다. 보통 10~30초 걸립니다." />
                </div>
            )}

            {result && !generating && <ResultView outline={result} onSave={handleSave} saving={saving} onRegenerate={handleGenerate} />}
        </div>
    );
}

interface ResultViewProps {
    outline: Outline;
    saving: boolean;
    onSave: () => void;
    onRegenerate: (e: React.FormEvent) => void;
}

function ResultView({ outline, saving, onSave, onRegenerate }: ResultViewProps) {
    return (
        <div className={styles.resultWrapper}>
            <div className={pageStyles.card}>
                <div className={styles.resultHeader}>
                    <div className={styles.resultTitle}>생성된 목차</div>
                    <div className={styles.resultActions}>
                        <Button variant="ghost" onClick={onRegenerate as unknown as () => void} disabled={saving}>
                            다시 생성
                        </Button>
                        <Button onClick={onSave} disabled={saving}>
                            {saving ? '저장 중...' : '저장'}
                        </Button>
                    </div>
                </div>

                {outline.overlapNote && (
                    <div className={styles.overlapNote}>
                        <strong>중복 처리:</strong> {outline.overlapNote}
                    </div>
                )}

                <ul className={styles.outlineList}>
                    {outline.items.map((item, i) => (
                        <li
                            key={i}
                            className={styles.outlineItem}
                            style={{ paddingLeft: `${(item.level - 1) * 20}px` }}
                        >
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
                                    <a href={`/posts/${ref.postId}`}>{ref.postTitle}</a>
                                    {ref.overlapMemo && <span style={{ color: 'var(--color-text-muted)' }}> — {ref.overlapMemo}</span>}
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </div>
    );
}
