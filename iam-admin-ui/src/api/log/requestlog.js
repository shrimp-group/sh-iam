import request from '@/utils/request';

// 1. 接口请求记录分页
export function requestLogPage(params) {
    return request({url: '/iam-admin/request-log/page', method: 'get', params});
}
// 2. 接口请求记录-详情
export function requestLogInfo(params) {
    return request({url: '/iam-admin/request-log/info', method: 'get', params});
}
