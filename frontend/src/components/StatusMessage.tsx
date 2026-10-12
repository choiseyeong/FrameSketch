// 세 가지 공통 상태(로딩 / 에러 / 비어 있음)를 하나의 레이아웃으로 보여 준다.
//
// 각 페이지가 자체적인 "불러오는 중...", "에러" 처리를 만들지 않도록 모아 둔다.
// 디자인 일관성이 유지되고, 상태 표시만 바꿔야 할 때 한 곳만 고치면 된다.

import styles from './StatusMessage.module.css';

export function LoadingState({ message = '불러오는 중...' }: { message?: string }) {
    return (
        <div className={styles.wrapper} role="status" aria-live="polite">
            <div className={styles.spinner} />
            <p className={styles.message}>{message}</p>
        </div>
    );
}

export function ErrorState({ message, hint }: { message: string; hint?: string }) {
    return (
        <div className={`${styles.wrapper} ${styles.error}`} role="alert">
            <div className={styles.icon}>!</div>
            <p className={styles.message}>{message}</p>
            {hint && <p className={styles.hint}>{hint}</p>}
        </div>
    );
}

export function EmptyState({ message, hint }: { message: string; hint?: string }) {
    return (
        <div className={styles.wrapper}>
            <div className={styles.icon}>·</div>
            <p className={styles.message}>{message}</p>
            {hint && <p className={styles.hint}>{hint}</p>}
        </div>
    );
}
