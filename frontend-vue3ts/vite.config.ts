import { fileURLToPath, URL } from 'node:url'
import path from 'node:path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const elementPlusIconNames = new Set(Object.keys(ElementPlusIconsVue))

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [
        ElementPlusResolver(),
        // On-demand auto-import @element-plus/icons-vue (e.g. <Edit />, <HomeFilled />)
        (name) => {
          if (elementPlusIconNames.has(name)) {
            return { name, from: '@element-plus/icons-vue' }
          }
        },
      ],
      // Auto-import custom components from src/components
      dirs: ['src/components'],
      dts: 'src/components.d.ts',
    }),
  ],
  server: {
    host: '0.0.0.0',
    port: 8000,
    proxy: {
      '/dev-api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/dev-api/, '/api'),
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
