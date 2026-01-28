import request from '@/utils/request';

export function commonDictsList(params) {
  return request({url: '/micro-dict/common/dicts/list', method: 'get', params})
}
