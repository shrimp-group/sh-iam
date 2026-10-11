// 管理后台主 SPA 装配：共享层全部由 @sh/ui-core 承担（改造自旧全量手工装配）
// ElementPlus 全局 props 定制（ElTable/ElDialog/ElDrawer/ElDatePicker）与全局属性
// （useDict/download/parseTime 等）及全局组件挂载均已由 @sh/ui-core dist/core/installCommon 完成，此处无需重复
import {createAdminApp} from '@sh/ui-core'
import {runtimeEnv} from '../env.js'
import {constantRoutes, views} from './router'

createAdminApp({
  env: runtimeEnv,
  routes: constantRoutes,
  views,
  // 管理端无登录入口（登录委托 CAS），白名单为空
  whiteList: [],
  menuSource: 'remote'
})
