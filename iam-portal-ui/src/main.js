// 门户主 SPA 装配：共享层全部由 @sh/ui-core 承担（改造自旧全量手工装配）
import {createAdminApp} from '@sh/ui-core'
import {runtimeEnv} from '../env.js'
import {constantRoutes, views} from './router'

createAdminApp({
  env: runtimeEnv,
  routes: constantRoutes,
  views,
  // 门户白名单：登录页与注册页免登录直达（/login 为门户自持路由，须在 whiteList 声明）
  whiteList: ['/login', '/register'],
  menuSource: 'remote'
})
