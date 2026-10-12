// 재사용 버튼. variant 로 역할을 구분한다.
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './Button.module.css';

type Variant = 'primary' | 'ghost' | 'subtle' | 'danger';
type Size = 'small' | 'medium' | 'large';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: Variant;
    size?: Size;
    children: ReactNode;
}

export function Button({ variant = 'primary', size = 'medium', className, children, ...rest }: Props) {
    const classes = [
        styles.base,
        styles[variant],
        size === 'small' ? styles.small : size === 'large' ? styles.large : '',
        className ?? '',
    ].filter(Boolean).join(' ');

    return (
        <button className={classes} {...rest}>
            {children}
        </button>
    );
}
