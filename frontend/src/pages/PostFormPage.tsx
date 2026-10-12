// 글 등록 화면.
// - 제목·URL·요약·태그 입력
// - 목차 항목을 동적으로 추가·삭제, 깊이(level) 선택
// - 저장 성공 시 글 목록으로 이동

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { postsApi } from '../api/posts';
import { ApiCallError } from '../api/client';
import { Button } from '../components/Button';
import pageStyles from './Page.module.css';
import styles from './PostFormPage.module.css';

interface SectionDraft {
    heading: string;
    level: number;
}

const LEVEL_INDENT_CLASS: Record<number, string> = {
    1: '',
    2: styles.levelIndent2,
    3: styles.levelIndent3,
    4: styles.levelIndent4,
    5: styles.levelIndent5,
    6: styles.levelIndent6,
};

export function PostFormPage() {
    const navigate = useNavigate();

    const [title, setTitle]       = useState('');
    const [url, setUrl]           = useState('');
    const [summary, setSummary]   = useState('');
    const [tagsText, setTagsText] = useState('');
    const [sections, setSections] = useState<SectionDraft[]>([{ heading: '', level: 1 }]);

    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

    function updateSection(index: number, patch: Partial<SectionDraft>) {
        setSections((current) => current.map((s, i) => (i === index ? { ...s, ...patch } : s)));
    }

    function addSection() {
        setSections((current) => [...current, { heading: '', level: 1 }]);
    }

    function removeSection(index: number) {
        setSections((current) => (current.length <= 1 ? current : current.filter((_, i) => i !== index)));
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setError(null);
        setFieldErrors({});

        // 빈 항목은 자동으로 걸러내 서버 검증을 거치지 않게 한다.
        const cleanSections = sections
            .map((s) => ({ heading: s.heading.trim(), level: s.level }))
            .filter((s) => s.heading.length > 0);

        if (cleanSections.length === 0) {
            setError('목차 항목을 최소 한 개 입력해 주세요.');
            return;
        }

        const tags = tagsText
            .split(',')
            .map((t) => t.trim())
            .filter(Boolean);

        setSubmitting(true);
        try {
            const saved = await postsApi.create({
                title: title.trim(),
                url: url.trim() || undefined,
                summary: summary.trim() || undefined,
                sections: cleanSections,
                tags,
            });
            navigate(`/posts/${saved.id}`);
        } catch (e) {
            if (e instanceof ApiCallError) {
                setError(e.message);
                const fe: Record<string, string> = {};
                e.fieldErrors.forEach((f) => { fe[f.field] = f.message; });
                setFieldErrors(fe);
            } else {
                setError('저장에 실패했습니다.');
            }
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div>
            <header className={pageStyles.header}>
                <div>
                    <h1 className={pageStyles.title}>새 글 등록</h1>
                    <p className={pageStyles.subtitle}>
                        제목과 목차만 저장합니다. 본문은 보관하지 않으며, 목차 생성 시 중복 검토 재료가 됩니다.
                    </p>
                </div>
            </header>

            {error && <div className={`${pageStyles.flash} ${pageStyles.flashError}`}>{error}</div>}

            <form className={pageStyles.card} onSubmit={handleSubmit}>
                <div className={pageStyles.field}>
                    <label className={pageStyles.label} htmlFor="title">제목</label>
                    <input
                        id="title"
                        className={pageStyles.input}
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="예: 운영체제의 프로세스와 스레드"
                        maxLength={300}
                    />
                    {fieldErrors.title && <p className={pageStyles.fieldError}>{fieldErrors.title}</p>}
                </div>

                <div className={pageStyles.field}>
                    <label className={pageStyles.label} htmlFor="url">
                        URL <span className={pageStyles.labelHint}>선택 · 블로그 링크 등</span>
                    </label>
                    <input
                        id="url"
                        className={pageStyles.input}
                        value={url}
                        onChange={(e) => setUrl(e.target.value)}
                        placeholder="https://..."
                        maxLength={1000}
                    />
                </div>

                <div className={pageStyles.field}>
                    <label className={pageStyles.label} htmlFor="summary">
                        요약 <span className={pageStyles.labelHint}>선택 · 1~2줄로 글의 내용을 요약</span>
                    </label>
                    <textarea
                        id="summary"
                        className={pageStyles.textarea}
                        value={summary}
                        onChange={(e) => setSummary(e.target.value)}
                        maxLength={2000}
                    />
                </div>

                <div className={pageStyles.field}>
                    <label className={pageStyles.label}>
                        목차 <span className={pageStyles.labelHint}>왼쪽 숫자는 들여쓰기 깊이 · 최대 6단계</span>
                    </label>
                    <div className={styles.sections}>
                        {sections.map((section, index) => (
                            <div
                                key={index}
                                className={`${styles.sectionRow} ${LEVEL_INDENT_CLASS[section.level] ?? ''}`}
                            >
                                <select
                                    className={styles.sectionLevelSelect}
                                    value={section.level}
                                    onChange={(e) => updateSection(index, { level: Number(e.target.value) })}
                                    aria-label="깊이"
                                >
                                    {[1, 2, 3, 4, 5, 6].map((n) => (
                                        <option key={n} value={n}>H{n}</option>
                                    ))}
                                </select>
                                <input
                                    className={styles.sectionHeadingInput}
                                    value={section.heading}
                                    onChange={(e) => updateSection(index, { heading: e.target.value })}
                                    placeholder={`${index + 1}번째 항목 제목`}
                                    maxLength={300}
                                />
                                <Button
                                    type="button"
                                    variant="danger"
                                    size="small"
                                    onClick={() => removeSection(index)}
                                    disabled={sections.length <= 1}
                                    aria-label={`${index + 1}번째 항목 삭제`}
                                >
                                    ×
                                </Button>
                            </div>
                        ))}
                    </div>
                    <div className={styles.addSectionRow}>
                        <Button type="button" variant="ghost" size="small" onClick={addSection}>
                            + 항목 추가
                        </Button>
                    </div>
                </div>

                <div className={pageStyles.field}>
                    <label className={pageStyles.label} htmlFor="tags">
                        태그 <span className={pageStyles.labelHint}>선택 · 쉼표로 구분</span>
                    </label>
                    <input
                        id="tags"
                        className={styles.tagsInput}
                        value={tagsText}
                        onChange={(e) => setTagsText(e.target.value)}
                        placeholder="예: 운영체제, CS"
                    />
                </div>

                <div className={pageStyles.formActions}>
                    <Button type="button" variant="ghost" onClick={() => navigate('/posts')}>
                        취소
                    </Button>
                    <Button type="submit" disabled={submitting}>
                        {submitting ? '저장 중...' : '저장'}
                    </Button>
                </div>
            </form>
        </div>
    );
}
