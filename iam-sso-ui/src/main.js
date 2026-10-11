// 门户主 SPA 装配：共享层全部由 @sh/ui-core 承担（改造自旧全量手工装配）
import { createAdminApp } from '@sh/ui-core'
import { runtimeEnv } from '../env.js'
import { routes, views } from './router'

createAdminApp({
  env: runtimeEnv,
  routes,
  views,
  // 门户白名单：注册页免登录直达（登录页为独立入口 login.html，不经主 SPA 路由）
  whiteList: ['/register'],
  menuSource: 'remote'
})
