import request from '@/utils/request'


// API字段权限-创建
export function apiFieldCreate(data) {
  return request({ url: '/iam-admin/api-field/create', method: 'post', data })
}

// API字段权限-修改
export function apiFieldUpdate(data) {
  return request({ url: '/iam-admin/api-field/update', method: 'post', data })
}

// API字段权限-删除
export function apiFieldRemove(data) {
  return request({ url: '/iam-admin/api-field/remove', method: 'post', data })
}

// API字段权限-按API查询
export function apiFieldListByApi(params) {
  return request({ url: '/iam-admin/api-field/list-by-api', method: 'get', params })
}

// 实体字段-根据API定位
export function entityFieldResolve(params) {
  return request({ url: '/iam-admin/entity-field/resolve', method: 'get', params })
}

// 菜单字段-列表
export function menuFieldList(params) {
  return request({ url: '/iam-admin/menu-field/list', method: 'get', params })
}

// 菜单字段-绑定
export function menuFieldBind(data) {
  return request({ url: '/iam-admin/menu-field/bind', method: 'post', data })
}

// 菜单字段-批量保存
export function menuFieldSave(data) {
  return request({ url: '/iam-admin/menu-field/save', method: 'post', data })
}

// 菜单字段-解绑
export function menuFieldUnbind(data) {
  return request({ url: '/iam-admin/menu-field/unbind', method: 'post', data })
}
