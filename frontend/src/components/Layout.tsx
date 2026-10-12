// 앱 전체 레이아웃. 사이드바 네비게이션과 메인 콘텐츠 영역으로 나뉜다.
import { NavLink, Outlet } from 'react-router-dom';
import styles from './Layout.module.css';

const NAV_ITEMS = [
    {
        heading: '작성',
        items: [
            { to: '/outlines/new', label: '목차 생성', icon: '✦' },
            { to: '/outlines',     label: '저장된 목차', icon: '≡' },
        ],
    },
    {
        heading: '자료',
        items: [
            { to: '/posts',     label: '기존 글',    icon: '◫' },
            { to: '/posts/new', label: '글 등록',    icon: '+' },
        ],
    },
];

export function Layout() {
    return (
        <div className={styles.app}>
            <aside className={styles.sidebar}>
                <div className={styles.brand}>
                    <span className={styles.brandName}>FrameSketch</span>
                    <span className={styles.brandTag}>학습 콘텐츠 설계 Agent</span>
                </div>
                <nav className={styles.nav}>
                    {NAV_ITEMS.map((group) => (
                        <div key={group.heading}>
                            <p className={styles.navGroupHeading}>{group.heading}</p>
                            <ul className={styles.navGroup}>
                                {group.items.map((item) => (
                                    <li key={item.to}>
                                        <NavLink
                                            to={item.to}
                                            end={item.to === '/outlines'}
                                            className={({ isActive }) =>
                                                `${styles.navLink} ${isActive ? styles.active : ''}`
                                            }
                                        >
                                            <span className={styles.navLinkIcon}>{item.icon}</span>
                                            {item.label}
                                        </NavLink>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </nav>
            </aside>
            <main className={styles.main}>
                <div className={styles.content}>
                    <Outlet />
                </div>
            </main>
        </div>
    );
}
