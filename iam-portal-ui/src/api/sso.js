import request from '@sh/ui-core/utils/request';

export const ssoLogin = (data) => {
    return request({url: '/iam-portal/public/sso/login', method: 'post', data})
}

export const userInfo = () => {
  return request({url: '/iam-portal/user/info', method: 'get'})
}

export const captchaChart = () => {
  return request({url: '/iam-portal/public/captcha/chart', method: 'get'})
}

export const ssoRegister = (data) => {
  return request({url: '/iam-portal/public/sso/register', method: 'post', data})
}
