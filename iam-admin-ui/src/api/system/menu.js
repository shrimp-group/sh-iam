import request from "@sh/ui-core/utils/request";

// 1. 菜单管理-列表
export const menuList = (params) => {
  return request({url: '/iam-admin/menu/list', method: 'get', params})
}

// 2. 菜单管理-树
export const menuTree = (params) => {
  return request({url: '/iam-admin/menu/tree', method: 'get', params})
}

// 3. 菜单管理-详情
export const menuInfo = (params) => {
  return request({url: '/iam-admin/menu/info', method: 'get', params})
}

// 4. 菜单管理-创建
export const menuCreate = (data) => {
  return request({url: '/iam-admin/menu/create', method: 'post', data})
}

// 5. 菜单管理-修改
export const menuUpdate = (data) => {
  return request({url: '/iam-admin/menu/update', method: 'post', data})
}

// 6. 菜单管理-删除
export const menuRemove = (data) => {
  return request({url: '/iam-admin/menu/remove', method: 'post', data})
}

// 7. 菜单管理-详情页
export const menuDetail = (params) => {
  return request({url: '/iam-admin/menu/detail', method: 'get', params})
}

// 8. 菜单管理-导出（ids 有值导出选中子树，否则按 appCode 全量导出）
export const menuExport = (params) => {
  return request({url: '/iam-admin/menu/export', method: 'get', params})
}

// 9. 菜单管理-导入（按菜单编码存在性更新/新增）
export const menuImport = (data) => {
  return request({url: '/iam-admin/menu/import', method: 'post', data})
}
