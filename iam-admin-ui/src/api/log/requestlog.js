import request from '@/utils/request';

// 1. 接口请求记录分页
export function requestRecordPage(params) {
    return request({url: '/iam-admin/request-log/page', method: 'get', params});
}
// 2. 接口请求记录-详情
export function requestRecordInfo(params) {
    return request({url: '/iam-admin/request-log/info', method: 'get', params});
}
