import request from "@/utils/request";

// 1. 角色管理-列表
export const roleList = (params) => {
  return request({url: '/iam-admin/role/list', method: 'get', params: params})
}

// 2. 角色管理-详情
export const roleInfo = (params) => {
  return request({url: '/iam-admin/role/info', method: 'get', params: params})
}

// 3. 角色管理-创建
export const roleCreate = (data) => {
  return request({url: '/iam-admin/role/create', method: 'post', data: data})
}

// 4. 角色管理-修改
export const roleUpdate = (data) => {
  return request({url: '/iam-admin/role/update', method: 'post', data: data})
}

// 5. 角色管理-删除
export const roleRemove = (params) => {
  return request({url: '/iam-admin/role/remove', method: 'post', params: params})
}
