import request from '@/utils/request';

export function loginRecordPage(params) {
  return request({url: '/iam-admin/login-log/page', method: 'get', params})
}
export function loginRecordInfo(params) {
  return request({url: '/iam-admin/login-log/info', method: 'get', params})
}
