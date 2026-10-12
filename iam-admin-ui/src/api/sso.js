import request from '@sh/ui-core/utils/request';

export const ssoLogout = () => {
    return request({url: '/iam-portal/public/sso/logout', method: 'get'})
}

export const userInfo = () => {
  return request({url: '/iam-portal/user/info', method: 'get'})
}

export const userMenuTree = () => {
  return request({url: '/iam-portal/user/menu/tree/ruoyi', method: 'get'})
}
