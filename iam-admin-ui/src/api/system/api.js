import request from "@/utils/request";


// 接口管理
// 1. 接口管理-分页
export const apiPage = (params) => {
  return request({url: '/iam-admin/api/page', method: 'get', params: params})
}

// 2. 接口管理-详情
export const apiInfo = (params) => {
  return request({url: '/iam-admin/api/info', method: 'get', params: params})
}

// 3. 接口管理-创建
export const apiCreate = (data) => {
  return request({url: '/iam-admin/api/create', method: 'post', data: data})
}

// 4. 接口管理-修改
export const apiUpdate = (data) => {
  return request({url: '/iam-admin/api/update', method: 'post', data: data})
}

// 5. 接口管理-删除
export const apiRemove = (params) => {
  return request({url: '/iam-admin/api/remove', method: 'post', params: params})
}
