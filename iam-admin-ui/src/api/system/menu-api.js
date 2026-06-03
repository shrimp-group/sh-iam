import request from "@/utils/request";

// 1. 菜单-API-列表
export const menuApiList = (params) => {
  return request({url: '/iam-admin/menu-api/list', method: 'get', params})
}

// 2. 菜单-API-已绑定列表
export const menuApiBoundList = (params) => {
  return request({url: '/iam-admin/menu-api/bound-list', method: 'get', params})
}

// 3. 菜单-API-未绑定列表
export const menuApiUnboundList = (params) => {
  return request({url: '/iam-admin/menu-api/unbound-list', method: 'get', params})
}

// 4. 菜单-API-绑定
export const menuApiBind = (data) => {
  return request({url: '/iam-admin/menu-api/bind', method: 'post', data})
}

// 5. 菜单-API-解绑
export const menuApiUnbind = (data) => {
  return request({url: '/iam-admin/menu-api/unbind', method: 'post', data})
}
