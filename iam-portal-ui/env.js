const name = "单点登录";
// 地址推导规则：未配置且域名访问时，取当前域名首段替换（api → API 地址）；localhost/IP 等非域名访问需显式配置或用 mock
export const appCode = 'portal';

const configs = {
  "local": {"baseApi": "/api"},
  "dev":   {"baseApi": ""},
  "sit":   {"baseApi": ""},
  "uat":   {"baseApi": ""},
  "prod":  {"baseApi": ""}
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

// 环境标题后缀：prod 无后缀，其余环境带【ENV】标识
export const envSuffix = env === 'prod' ? '' : ('【' + env.toUpperCase() + '】');

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
  c.baseApi = localStorage.getItem('mock-api') || c.baseApi;
  if (!c.baseApi) {c.baseApi = deriveDomainUrl('api')}
  return c;
}

const config = getConfig(env);

// 门户自身即登录上游：portal 恒为空串（guard 无 token 走 SPA 内导航 /login；redirectToLogin 兜底拼相对 /#/login）。
// 缺失时 getRuntimeEnv().portal 为 undefined，redirectToLogin 会拼出 "undefined/#/login?..." 的坏地址
export const portal = '';

// 运行时环境聚合（供 @sh/ui-core 运行时注入）；title 在导出处统一以 name + envSuffix 拼接
export const runtimeEnv = {appCode, env, portal, envSuffix, title: name + envSuffix, ...config};
