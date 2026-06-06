import request from "@/utils/request";

// 1. 角色用户-分页
export const roleUserPage = (params) => {
  return request({url: '/iam-admin/role-user/page', method: 'get', params})
}

// 2. 角色用户-批量绑定
export const roleUserBind = (data) => {
  return request({url: '/iam-admin/role-user/bind', method: 'post', data})
}

// 3. 角色用户-批量解绑
export const roleUserUnbind = (data) => {
  return request({url: '/iam-admin/role-user/unbind', method: 'post', data})
}
