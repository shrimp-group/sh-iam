import request from '@/utils/request';

export function loginLogPage(params) {
  return request({url: '/iam-admin/login-log/page', method: 'get', params})
}
export function loginLogInfo(params) {
  return request({url: '/iam-admin/login-log/info', method: 'get', params})
}
