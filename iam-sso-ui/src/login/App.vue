<template>
  <div class="login">
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号" :prefix-icon="User" @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="password" size="large" auto-complete="off" placeholder="密码" :prefix-icon="Lock" @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item prop="captchaCode" v-if="loginForm.captchaId">
        <el-input v-model="loginForm.captchaCode" size="large" auto-complete="off" placeholder="验证码" style="width: 63%" :prefix-icon="Key" @keyup.enter="handleLogin" />
        <div class="login-code"><img :src="codeUrl" @click="getCode" class="login-code-img"/></div>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button :loading="loading" size="large" type="primary" style="width:100%;" @click.prevent="handleLogin">
          <span v-if="loading">登 录 中...</span>
          <span v-else>登 录</span>
        </el-button>
        <div style="color: orange;">
            {{loginMessage || '&nbsp;'}}
        </div>
        <el-divider />
        <div style="float: right;" v-if="register">
          <a class="link-type" :href="'/#/register'">立即注册</a>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
// CAS 登录页（原 views/login.vue 改造为独立入口）：
// 登录态经包内 user store 写父域 Cookie；成功后按 redirect 整页回跳（原 hash 路由内 push 无法跳外部子系统）
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getToken } from '@sh/ui-core/utils/auth'
import useUserStore from '@sh/ui-core/store/modules/user'
import { captchaChart } from '@/api/sso'
import { runtimeEnv } from '../../env.js'

const title = runtimeEnv.title
const footerContent = 'Copyright © 2018-2025 wangkc. All Rights Reserved.'
const userStore = useUserStore()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: undefined,
  password: undefined,
  captchaCode: undefined,
  captchaId: undefined
})

const loginMessage = ref(null)

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
}

const codeUrl = ref("")
const loading = ref(false)
// 注册开关
const register = ref(true)
// redirect 取自 location.search（本入口无路由，CAS 协议 {cas}/login?redirect= 落点）
const redirect = new URLSearchParams(window.location.search).get('redirect')

// 回跳地址校验：相对路径放行；绝对地址仅允许 http(s) 且与本站同 host 或同父域，防开放重定向
function safeTarget(target) {
  if (!target) return '/'
  if (target.startsWith('/')) return target
  try {
    const url = new URL(target)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return '/'
    const host = window.location.hostname
    const parentDomain = host.split('.').slice(-2).join('.')
    if (url.hostname === host || url.hostname.endsWith('.' + parentDomain)) {
      return url.href
    }
    return '/'
  } catch {
    return '/'
  }
}

// 已持有登录态：直接回跳，避免重复登录；验证码默认不展示，仅登录失败时拉取
if (getToken()) {
  location.href = safeTarget(redirect)
}

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 调用包内 action 登录（Cookie 写入由包内 auth.js 承担）
      userStore.login(loginForm.value).then((res) => {
        loginMessage.value = res.data?.loginMessage
        if (res.data?.loginStatus !== 0) {
          // 登录失败（后端正常响应）：拉取验证码并展示
          getCode()
          return;
        }
        // 成功，600ms后按 redirect 整页回跳（无 redirect 时回门户首页）
        setTimeout(() => {
          location.href = safeTarget(redirect)
        }, 600)
      }).catch(() => {
        // 接口异常不展示验证码
      }).finally(() => {
        loading.value = false
      })
    }
  })
}

function getCode() {
  captchaChart().then(res => {
    codeUrl.value = res.data.base64Image
    loginForm.value.captchaId = res.data.captchaId
  })
}

</script>

<style lang='scss' scoped>
.login {
  display: flex;
  justify-content: right;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
  padding-right: 15%;
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  z-index: 1;
  .el-input {
    height: 40px;
    input {
      height: 40px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 40px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.el-divider--horizontal {
  margin: 12px 0 0 0;
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 40px;
  padding-left: 12px;
}
</style>
