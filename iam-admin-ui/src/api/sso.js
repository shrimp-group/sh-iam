import request from '@/utils/request';

export const captchaChart = () => {
    return request({url: '/iam-sso/public/captcha/chart', method: 'get'})
}

export const ssoLogin = (data) => {
    return request({url: '/iam-sso/public/sso/login', method: 'post', data})
}

export const ssoLogout = () => {
    return request({url: '/iam-sso/public/sso/logout', method: 'get'})
}

export const userInfo = () => {
  return request({url: '/iam-sso/user/info', method: 'get'})
}

export const userMenuTree = () => {
  return request({url: '/iam-sso/user/menu/tree/ruoyi', method: 'get'})
}

export const ssoRegister = (data) => {
  return request({url: '/iam-sso/public/sso/register', method: 'post', data})
}

