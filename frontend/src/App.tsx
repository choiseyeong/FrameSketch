// 라우팅 정의. 레이아웃을 공유하는 중첩 라우트 구조.
import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { PostListPage } from './pages/PostListPage';
import { PostFormPage } from './pages/PostFormPage';
import { PostDetailPage } from './pages/PostDetailPage';
import { OutlineGeneratePage } from './pages/OutlineGeneratePage';
import { OutlineListPage } from './pages/OutlineListPage';
import { OutlineDetailPage } from './pages/OutlineDetailPage';

export default function App() {
    return (
        <Routes>
            <Route element={<Layout />}>
                {/* 첫 화면은 목차 생성 - 서비스의 주 사용 흐름이다. */}
                <Route index element={<Navigate to="/outlines/new" replace />} />

                <Route path="posts"      element={<PostListPage />} />
                <Route path="posts/new"  element={<PostFormPage />} />
                <Route path="posts/:id"  element={<PostDetailPage />} />

                <Route path="outlines"       element={<OutlineListPage />} />
                <Route path="outlines/new"   element={<OutlineGeneratePage />} />
                <Route path="outlines/:id"   element={<OutlineDetailPage />} />

                <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
        </Routes>
    );
}
