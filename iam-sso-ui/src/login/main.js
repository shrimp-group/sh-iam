// CAS 登录页入口：精简装配，不加载管理壳（layout/守卫均不在登录页使用）
// @sh/ui-core 0.0.4：环境经 setRuntimeEnv 运行时注入，须先于任何包能力调用（含 getToken）
// 登录页使用包内 user store（App.vue setup），必须安装 Pinia
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { setRuntimeEnv } from '@sh/ui-core'
import { runtimeEnv } from '../../env.js'
import App from './App.vue'

setRuntimeEnv(runtimeEnv)

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus)
app.mount('#app')
