import request from "@/utils/request";


// 应用管理
// 1. 应用管理-分页
export const appPage = (params) => {
  return request({url: '/iam-admin/app/page', method: 'get', params: params})
}

// 2. 应用管理-详情
export const appInfo = (params) => {
  return request({url: '/iam-admin/app/info', method: 'get', params: params})
}

// 3. 应用管理-创建
export const appCreate = (data) => {
  return request({url: '/iam-admin/app/create', method: 'post', data: data})
}

// 4. 应用管理-修改
export const appUpdate = (data) => {
  return request({url: '/iam-admin/app/update', method: 'post', data: data})
}

// 5. 应用管理-删除
export const appRemove = (params) => {
  return request({url: '/iam-admin/app/remove', method: 'post', params: params})
}

// 6. 应用管理-应用选项
export const appOptions = (params) => {
  return request({url: '/iam-admin/app/options', method: 'get', params: params})
}

