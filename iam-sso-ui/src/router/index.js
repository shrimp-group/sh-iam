import {createRouter, createWebHashHistory} from 'vue-router'
/* Layout */
import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  // 个人中心模块
  {
    path: '/personal',
    component: Layout,
    redirect: '/personal/info',
    name: 'PersonalCenter',
    meta: {
      title: '个人中心',
      icon: 'user',
    },
    children: [
      {
        path: 'info',
        component: () => import('@/views/user/profile/index'),
        name: 'PersonalInfo',
        meta: {
          title: '个人信息',
          icon: 'information',
        }
      },
      {
        path: 'change-password',
        component: () => import('@/views/user/change-password'),
        name: 'ChangePassword',
        meta: {
          title: '修改密码',
          icon: 'lock',
        }
      },
      {
        path: 'login-records',
        component: () => import('@/views/user/login-records'),
        name: 'LoginRecords',
        meta: {
          title: '登录记录',
          icon: 'record',
        }
      },
      {
        path: 'operate-logs',
        component: () => import('@/views/user/operate-logs'),
        name: 'OperateLogs',
        meta: {
          title: '操作日志',
          icon: 'log',
        }
      }
    ]
  },
  // 门户网站模块
  {
    path: '/portal',
    component: Layout,
    redirect: '/portal/index',
    name: 'Portal',
    meta: {
      title: '门户网站',
      icon: 'component',
    },
    children: [
      {
        path: 'index',
        component: () => import('@/views/portal/index'),
        name: 'PortalIndex',
        meta: {
          title: '首页数据',
          icon: 'dashboard',
        }
      },
      {
        path: 'notices',
        component: () => import('@/views/portal/notices'),
        name: 'PortalNotices',
        meta: {
          title: '公告列表',
          icon: 'notice',
        }
      },
      {
        path: 'statistics',
        component: () => import('@/views/portal/statistics'),
        name: 'PortalStatistics',
        meta: {
          title: '统计数据',
          icon: 'statistics',
        }
      },
      {
        path: 'todo-list',
        component: () => import('@/views/portal/todo-list'),
        name: 'PortalTodoList',
        meta: {
          title: '待办事项',
          icon: 'todo',
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

export default router
