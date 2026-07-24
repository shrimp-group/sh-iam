import request from '@/utils/request';

export function loginRecordPage(params) {
  return request({url: '/iam-admin/login-record/page', method: 'get', params})
}
export function loginRecordInfo(params) {
  return request({url: '/iam-admin/login-record/info', method: 'get', params})
}
