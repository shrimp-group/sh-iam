import request from "@/utils/request";

// 1. 用户-分页
export const userPage = (params) => {
  return request({url: '/iam-admin/user/page', method: 'get', params})
}

// 2. 用户-详情
export const userInfo = (params) => {
  return request({url: '/iam-admin/user/info', method: 'get', params})
}

// 3. 用户-创建
export const userCreate = (data) => {
  return request({url: '/iam-admin/user/create', method: 'post', data})
}

// 4. 用户-修改
export const userUpdate = (data) => {
  return request({url: '/iam-admin/user/update', method: 'post', data})
}

// 5. 用户-删除
export const userRemove = (data) => {
  return request({url: '/iam-admin/user/remove', method: 'post', data})
}

// 6. 用户认证-重置密码
export const userAuthResetPassword = (data) => {
  return request({url: '/iam-admin/user-auth/reset-password', method: 'post', data})
}
