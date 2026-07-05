import request from "@/utils/request";

// 1. 用户角色-列表
export const userRoleList = (params) => {
  return request({url: '/iam-admin/user-role/list', method: 'get', params})
}

// 2. 用户角色-绑定
export const userRoleBind = (data) => {
  return request({url: '/iam-admin/user-role/bind', method: 'post', data})
}

// 3. 用户角色-解绑
export const userRoleUnbind = (data) => {
  return request({url: '/iam-admin/user-role/unbind', method: 'post', data})
}

// 4. 用户角色-角色树（按应用，含绑定数量标记）
export const userRoleTree = (params) => {
  return request({url: '/iam-admin/user-role/role-tree', method: 'get', params})
}

// 5. 用户角色-菜单来源
export const userMenuSourceList = (params) => {
  return request({url: '/iam-admin/user-role/menu-source', method: 'get', params})
}

// 6. 菜单-已绑定角色列表
export const menuBoundRoles = (params) => {
  return request({url: '/iam-admin/menu/bound-roles', method: 'get', params})
}

// 7. 菜单-关联用户列表
export const menuBoundUsers = (params) => {
  return request({url: '/iam-admin/menu/bound-users', method: 'get', params})
}
