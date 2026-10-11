// 门户站点 vite 配置（对齐 cas-demo：@sh/ui-core preset + 单入口，登录路由 /login 由包 constantRoutes 内置）
import {fileURLToPath, URL} from 'node:url'
import path from 'path'
import {defineConfig} from 'vite'
import {createUiCorePreset} from '@sh/ui-core/vite'

// https://vitejs.dev/config/
export default defineConfig(({command}) => {
  return {
    base: '/',
    plugins: [...createUiCorePreset({isBuild: command === 'build'})],
    // ui-core dist 中 Logo.vue 的 import "/favicon.svg" 为 public 资产语义（由消费方 public 目录提供），
    // 预构建 esbuild 无法按根路径解析，保持 external 后由 dev 静态服务命中
    optimizeDeps: {
      esbuildOptions: {external: ['/favicon.svg']}
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
        // 单入口：index.html 门户主 SPA（登录页为包内置 /login 路由，非独立入口）
        input: {
          index: fileURLToPath(new URL('./index.html', import.meta.url))
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
