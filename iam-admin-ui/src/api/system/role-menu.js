import request from "@/utils/request";

// 1. 角色-菜单-列表（返回 menuCode 列表）
export const roleMenuList = (params) => {
  return request({url: '/iam-admin/role-menu/list', method: 'get', params})
}

// 2. 角色-菜单-保存（批量绑定）
export const roleMenuSave = (data) => {
  return request({url: '/iam-admin/role-menu/save', method: 'post', data})
}

// 3. 角色-菜单-已绑定角色列表（根据菜单编码查询）
export const roleMenuBoundRoles = (params) => {
  return request({url: '/iam-admin/role-menu/bound-roles', method: 'get', params})
}
