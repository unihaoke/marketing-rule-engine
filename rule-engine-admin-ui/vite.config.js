import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 营销规则引擎运营后台
// dev server 将 /api 代理到后端 Spring Boot（默认 8080 端口）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
