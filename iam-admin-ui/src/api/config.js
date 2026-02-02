import request from '@/utils/request.js'


// 根据参数键名查询参数值
export function getConfigKey(configKey) {
  return request({
    url: '/system/config/configKey/' + configKey,
    method: 'get'
  })
}

