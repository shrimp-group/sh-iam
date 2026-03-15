import request from "@/utils/request";

// 1. 接口管理-分页
export const apiPage = (params) => {
  return request({url: '/iam-admin/api/page', method: 'get', params})
}

// 2. 接口管理-详情
export const apiInfo = (params) => {
  return request({url: '/iam-admin/api/info', method: 'get', params})
}

// 3. 接口管理-创建
export const apiCreate = (data) => {
  return request({url: '/iam-admin/api/create', method: 'post', data})
}

// 4. 接口管理-修改
export const apiUpdate = (data) => {
  return request({url: '/iam-admin/api/update', method: 'post', data})
}

// 5. 接口管理-删除
export const apiRemove = (data) => {
  return request({url: '/iam-admin/api/remove', method: 'post', data})
}
