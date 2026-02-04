import request from "@/utils/request";

// 1. 菜单管理-列表
export const menuList = (params) => {
  return request({url: '/iam-admin/menu/list', method: 'get', params: params})
}

// 2. 菜单管理-树
export const menuTree = (params) => {
  return request({url: '/iam-admin/menu/tree', method: 'get', params: params})
}

// 3. 菜单管理-详情
export const menuInfo = (params) => {
  return request({url: '/iam-admin/menu/info', method: 'get', params: params})
}

// 4. 菜单管理-创建
export const menuCreate = (data) => {
  return request({url: '/iam-admin/menu/create', method: 'post', data: data})
}

// 5. 菜单管理-修改
export const menuUpdate = (data) => {
  return request({url: '/iam-admin/menu/update', method: 'post', data: data})
}

// 6. 菜单管理-删除
export const menuRemove = (params) => {
  return request({url: '/iam-admin/menu/remove', method: 'post', params: params})
}
