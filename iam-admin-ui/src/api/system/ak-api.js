import request from "@/utils/request";

// 1. 访问密钥-API-列表
export const accesskeyapiList = (params) => {
  return request({url: '/iam-admin/access-key-api/list', method: 'get', params})
}

// 2. 访问密钥-API-创建
export const accesskeyapiBind = (data) => {
  return request({url: '/iam-admin/access-key-api/bind', method: 'post', data})
}

// 3. 访问密钥-API-删除
export const accesskeyapiUnbind = (data) => {
  return request({url: '/iam-admin/access-key-api/unbind', method: 'post', data})
}

