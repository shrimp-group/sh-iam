import request from "@/utils/request";


// 1. 数据权限维度-分页
export const datadimPage = (params) => {
  return request({url: '/iam-admin/data-dim/page', method: 'get', params: params})
}

// 2. 数据权限维度-详情
export const datadimInfo = (params) => {
  return request({url: '/iam-admin/data-dim/info', method: 'get', params: params})
}

// 3. 数据权限维度-创建
export const datadimCreate = (params) => {
  return request({url: '/iam-admin/data-dim/create', method: 'post', data: params})
}

// 4. 数据权限维度-修改
export const datadimUpdate = (params) => {
  return request({url: '/iam-admin/data-dim/update', method: 'post', data: params})
}

// 5. 数据权限维度-删除
export const datadimRemove = (params) => {
  return request({url: '/iam-admin/data-dim/remove', method: 'post', data: params})
}

