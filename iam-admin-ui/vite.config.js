// 管理后台 vite 配置（对齐 iam-portal-ui：插件装配交由 @sh/ui-core preset，业务视图/产物命名保持 admin 现状）
import path from 'path'
import {defineConfig} from 'vite'
import {createUiCorePreset} from '@sh/ui-core/vite'

// https://vitejs.dev/config/
export default defineConfig(({command}) => {
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    base: '/',
    plugins: [...createUiCorePreset({isBuild: command === 'build'})],
    // ui-core dist 中 Logo.vue 的 import "/favicon.svg" 为 public 资产语义（由消费方 public 目录提供），
    // 预构建 esbuild 无法按根路径解析，保持 external 后由 dev 静态服务命中
    optimizeDeps: {
      include: ['monaco-editor', '@vueup/vue-quill'],
      esbuildOptions: {external: ['/favicon.svg']}
    },
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置（portal-ui dev 占 80，admin 改用 1024 避免冲突）
    server: {
      port: 1024,
      host: true,
      open: false,
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
