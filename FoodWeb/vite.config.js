import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
//import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    //vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    // echarts 体积较大，单独成块便于浏览器长期缓存，且报表页本身是懒加载路由
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/echarts') || id.includes('node_modules/zrender')) {
            return 'echarts'
          }
        }
      }
    },
    chunkSizeWarningLimit: 700
  },
  server: {
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      '192.168.0.105',
      'test.buguagaoshu.com' // 添加需要允许的主机名
    ],
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      }
    }
  }
})
