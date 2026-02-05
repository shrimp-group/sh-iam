import request from "@/utils/request";

// 1. 用户-分页
export const userPage = (params) => {
  return request({url: '/iam-admin/user/page', method: 'get', params: params})
}

// 2. 用户-详情
export const userInfo = (params) => {
  return request({url: '/iam-admin/user/info', method: 'get', params: params})
}

// 3. 用户-创建
export const userCreate = (params) => {
  return request({url: '/iam-admin/user/create', method: 'post', data: params})
}

// 4. 用户-修改
export const userUpdate = (params) => {
  return request({url: '/iam-admin/user/update', method: 'post', data: params})
}

// 5. 用户-删除
export const userRemove = (params) => {
  return request({url: '/iam-admin/user/remove', method: 'post', data: params})
}

