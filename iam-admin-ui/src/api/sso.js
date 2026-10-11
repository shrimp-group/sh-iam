import request from '@sh/ui-core/utils/request';

export const ssoLogout = () => {
    return request({url: '/iam-sso/public/sso/logout', method: 'get'})
}

export const userInfo = () => {
  return request({url: '/iam-sso/user/info', method: 'get'})
}

export const userMenuTree = () => {
  return request({url: '/iam-sso/user/menu/tree/ruoyi', method: 'get'})
}
