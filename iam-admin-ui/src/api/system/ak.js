import request from "@/utils/request";

// 1. 访问密钥-分页
export const accesskeyPage = (params) => {
  return request({url: '/iam-admin/access-key/page', method: 'get', params})
}

// 2. 访问密钥-详情
export const accesskeyInfo = (params) => {
  return request({url: '/iam-admin/access-key/info', method: 'get', params})
}

// 3. 访问密钥-创建
export const accesskeyCreate = (data) => {
  return request({url: '/iam-admin/access-key/create', method: 'post', data})
}

// 4. 访问密钥-修改
export const accesskeyUpdate = (data) => {
  return request({url: '/iam-admin/access-key/update', method: 'post', data})
}

// 5. 访问密钥-删除
export const accesskeyRemove = (data) => {
  return request({url: '/iam-admin/access-key/remove', method: 'post', data})
}

