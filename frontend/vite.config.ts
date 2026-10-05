import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 중 /api 요청을 Spring Boot 로 넘긴다. 프론트에서는 호스트 없이
    // '/api/...' 로만 호출하므로 배포 환경이 바뀌어도 코드를 고칠 필요가 없다.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
