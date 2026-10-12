const name = "授权中心";
// 地址推导规则：未配置且域名访问时，取当前域名首段替换（api → API 地址，portal → 门户登录地址）；localhost/IP 等非域名访问需显式配置或用 mock
export const appCode = 'auth';

const configs = {
  "dev":   {"portal":"http://localhost", "title": name + "【LOCAL】", "baseApi": "/api"},
//  "dev":   {"portal": "",                 "title": name + "【DEV】",   "baseApi": ""},
  "sit":   {"portal": "",                 "title": name + "【SIT】",   "baseApi": ""},
  "uat":   {"portal": "",                 "title": name + "【UAT】",   "baseApi": ""},
  "prod":  {"portal": "",                 "title": name,               "baseApi": ""}
};

function getEnv() {
  let url = window.location.host;
  let env = 'prod';
  if (url.indexOf('127.0.0.1') > -1) {env = 'local';}
  if (url.indexOf('localhost') > -1) {env = 'local';}
  if (url.indexOf('.dev.') > -1) {env = 'dev';}
  if (url.indexOf('.sit.') > -1) {env = 'sit';}
  if (url.indexOf('.uat.') > -1) {env = 'uat';}
  return env;
}

// 获取环境
export const env = getEnv();

// 将当前域名首段替换为 target，非域名访问返回 ''（不推导，不携带端口）
function deriveDomainUrl(target) {
  const hostname = window.location.hostname;
  if (hostname.indexOf('.') < 0) {return '';}
  if (/^\d{1,3}(\.\d{1,3}){3}$/.test(hostname)) {return '';}
  return window.location.protocol + '//' + target + '.' + hostname.split('.').slice(1).join('.');
}

// 获取配置：mock > 显式配置 > 域名推导
function getConfig(env) {
  let c = configs[env];
  if (c === undefined) {
    c = configs['local'];
  }
  c = {...c};
  c.portal = localStorage.getItem('mock-portal') || c.portal;
  c.baseApi = localStorage.getItem('mock-api') || c.baseApi;
  if (!c.portal) {
    c.portal = deriveDomainUrl('portal');
  }
  if (!c.baseApi) {
    c.baseApi = deriveDomainUrl('api');
  }
  return c;
}

const config = getConfig(env);

// 运行时环境聚合（供 @sh/ui-core 运行时注入，对齐 iam-portal-ui/env.js）
export const runtimeEnv = {appCode, env, ...config};
