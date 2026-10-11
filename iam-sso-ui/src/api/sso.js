// 门户自有 SSO 接口（0.0.4 起包内公共协议仅保留 ssoLogin/ssoLogout/userMenuTree，其余归属各站点本地）
import request from '@sh/ui-core/utils/request';

export const captchaChart = () => {
  return request({url: '/iam-sso/public/captcha/chart', method: 'get'})
}

export const userInfo = () => {
  return request({url: '/iam-sso/user/info', method: 'get'})
}

export const ssoRegister = (data) => {
  return request({url: '/iam-sso/public/sso/register', method: 'post', data})
}
