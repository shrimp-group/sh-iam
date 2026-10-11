// 门户站点 vite 配置（对齐 cas-demo：@sh/ui-core preset + 双入口 + /login 映射）
import { fileURLToPath, URL } from 'node:url'
import path from 'path'
import { defineConfig } from 'vite'
import { createUiCorePreset } from '@sh/ui-core/vite'

// dev 下将 CAS 协议路径 /login 映射到登录页入口（构建产物由 nginx 做 /login → login.html）
const mockPageMap = { '/login': '/login.html' }

function mockPages() {
  return {
    name: 'mock-pages',
    configureServer(server) {
      server.middlewares.use((req, _res, next) => {
        const [reqPath, query] = req.url.split('?')
        if (mockPageMap[reqPath]) {
          req.url = mockPageMap[reqPath] + (query ? '?' + query : '')
        }
        next()
      })
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig(({ command }) => {
  return {
    base: '/',
    plugins: [...createUiCorePreset({ isBuild: command === 'build' }), mockPages()],
    // ui-core dist 中 Logo.vue 的 import "/favicon.svg" 为 public 资产语义（由消费方 public 目录提供），
    // 预构建 esbuild 无法按根路径解析，保持 external 后由 dev 静态服务命中
    optimizeDeps: {
      esbuildOptions: { external: ['/favicon.svg'] }
    },
    resolve: {
      alias: {
        // 业务视图相对引用保持现状
        '@': path.resolve(__dirname, './src')
      },
      extensions: ['.mjs', '.js', '.jsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        // 双入口：index.html 门户主 SPA；login.html CAS 登录页
        input: {
          index: fileURLToPath(new URL('./index.html', import.meta.url)),
          login: fileURLToPath(new URL('./login.html', import.meta.url))
        },
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置
    server: {
      port: 80,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/api/, '')
        },
      }
    },
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})
