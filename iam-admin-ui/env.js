const name = "授权中心";
export const appCode = 'iam-admin';

const configs = {
  "local": {"cas": "http://cas.uat.husters.cn", "title": name + "【LOCAL】", "baseApi": "/api"},
//  "local": {"cas": "http://cas.uat.husters.cn", "title": name + "【LOCAL】", "baseApi": "https://api.uat.husters.cn"},
  "uat"  : {"cas": "http://cas.uat.husters.cn", "title": name + "【UAT】",   "baseApi": "https://api.uat.husters.cn"},
  "prod" : {"cas": "http://cas.husters.cn",     "title": name,               "baseApi": "https://api.husters.cn"}
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

// 获取配置
function getConfig(env) {
  let c = configs[env];
  if (c === undefined) {
    c = configs['local'];
  }
  c.cas = localStorage.getItem('mock-cas') || c.cas;
  c.baseApi = localStorage.getItem('mock-api') || c.baseApi;
  return c;
}

const config = getConfig(env);
export const title = config['title'];
export const cas = config['cas'];
export const baseApi = config['baseApi'];
