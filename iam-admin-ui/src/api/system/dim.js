import request from "@sh/ui-core/utils/request";


// 1. 数据权限维度-分页
export const datadimPage = (params) => {
  return request({url: '/iam-admin/data-dim/page', method: 'get', params})
}

// 2. 数据权限维度-详情
export const datadimInfo = (params) => {
  return request({url: '/iam-admin/data-dim/info', method: 'get', params})
}

// 3. 数据权限维度-创建
export const datadimCreate = (data) => {
  return request({url: '/iam-admin/data-dim/create', method: 'post', data})
}

// 4. 数据权限维度-修改
export const datadimUpdate = (data) => {
  return request({url: '/iam-admin/data-dim/update', method: 'post', data})
}

// 5. 数据权限维度-删除
export const datadimRemove = (data) => {
  return request({url: '/iam-admin/data-dim/remove', method: 'post', data})
}

// 6. 数据维度-导出（ids 有值导出勾选项，否则按筛选条件全量导出）
export const datadimExport = (params) => {
  return request({url: '/iam-admin/data-dim/export', method: 'get', params})
}

// 7. 数据维度-导入（按维度编码存在性更新/新增）
export const datadimImport = (data) => {
  return request({url: '/iam-admin/data-dim/import', method: 'post', data})
}

