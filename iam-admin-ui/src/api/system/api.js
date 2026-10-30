import request from "@sh/ui-core/utils/request";

// 1. 接口管理-分页
export const apiPage = (params) => {
  return request({url: '/iam-admin/api/page', method: 'get', params})
}

// 2. 接口管理-详情
export const apiInfo = (params) => {
  return request({url: '/iam-admin/api/info', method: 'get', params})
}

// 3. 接口管理-创建
export const apiCreate = (data) => {
  return request({url: '/iam-admin/api/create', method: 'post', data})
}

// 4. 接口管理-修改
export const apiUpdate = (data) => {
  return request({url: '/iam-admin/api/update', method: 'post', data})
}

// 5. 接口管理-删除
export const apiRemove = (data) => {
  return request({url: '/iam-admin/api/remove', method: 'post', data})
}

// 6. API管理-选项
export const apiOptions = (params) => {
  return request({url: '/iam-admin/api/options', method: 'get', params})
}

// 7. API管理-同步
export const apiSync = (data) => {
  return request({url: '/iam-admin/api/sync', method: 'post', data})
}

// 8. API管理-复制为json
export const apiCopy = (params) => {
  return request({url: '/iam-admin/api/copy', method: 'get', params})
}

// 9. API管理-粘贴JSON
export const apiPaste = (data) => {
  return request({url: '/iam-admin/api/paste', method: 'post', data})
}

// 10. API管理-详情页
export const apiDetail = (params) => {
  return request({url: '/iam-admin/api/detail', method: 'get', params})
}

// 11. API管理-接口文档
export function apiDoc(query) {
  return request({url: '/iam-admin/api/doc', method: 'get', params: query})
}

// 12. API管理-导出（ids 有值导出选中项，否则按筛选条件全量导出）
export const apiExport = (params) => {
  return request({url: '/iam-admin/api/export', method: 'get', params})
}

// 13. API管理-导入（按接口编码存在性更新/新增）
export const apiImport = (data) => {
  return request({url: '/iam-admin/api/import', method: 'post', data})
}

