<!-- CAS 登录页（应用功能：自 @sh/ui-core LoginView 0.0.8 迁回归属门户）：
     登录态经包内 user store 写父域 Cookie；成功后按 redirect 回跳（子系统整页跳，站内路由跳）；
     验证码默认不展示，仅登录失败（后端正常响应 loginStatus≠0）时拉取 -->
<template>
  <div class="login">
    <!-- 左侧装饰：动画小人（纯装饰，随鼠标/输入状态/密码可见性互动，小屏隐藏） -->
    <EyeBuddies :is-typing="isTyping" :show-password="passwordVisible" :password-length="(loginForm.password || '').length" />
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号" :prefix-icon="User" @focus="isTyping = true" @blur="isTyping = false" @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" :type="passwordVisible ? 'text' : 'password'" size="large" auto-complete="off" placeholder="密码" :prefix-icon="Lock" @focus="isTyping = true" @blur="isTyping = false" @keyup.enter="handleLogin">
          <template #suffix>
            <el-icon class="pwd-toggle" @click="passwordVisible = !passwordVisible">
              <View v-if="passwordVisible" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="captchaCode" v-if="loginForm.captchaId">
        <el-input v-model="loginForm.captchaCode" size="large" auto-complete="off" placeholder="验证码" style="width: 63%" :prefix-icon="Key" @focus="isTyping = true" @blur="isTyping = false" @keyup.enter="handleLogin" />
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
        <div style="float: right;">
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
import { User, Lock, Key, View, Hide } from '@element-plus/icons-vue'
import { getToken, setToken } from '@sh/ui-core/utils/auth'
import { encrypt } from '@sh/ui-core/utils/jsencrypt'
import { safeTarget } from '@sh/ui-core/utils/safe-target'
import { runtimeEnv } from '../../env.js'
import { captchaChart, ssoLogin } from '@/api/sso'
import EyeBuddies from '@/components/eye-buddies.vue'

const title = runtimeEnv.title
const footerContent = 'Copyright © 2018-2025 wangkc. All Rights Reserved.'
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: undefined,
  password: undefined,
  captchaCode: undefined,
  captchaId: undefined
})

const loginMessage = ref(null)

// 动画小人联动状态：账号框聚焦中 / 密码明文可见
const isTyping = ref(false)
const passwordVisible = ref(false)

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
}

const codeUrl = ref("")
const loading = ref(false)
// redirect 取自路由 query（CAS 协议 /#/login?redirect= 落点）
const redirect = route.query.redirect

// 站内相对目标走路由，跨站/绝对目标整页跳
function leaveTo(target) {
  if (/^https?:\/\//i.test(target)) {
    window.location.href = target
  } else {
    router.replace(target || '/')
  }
}

// 已持有登录态：直接回跳，避免重复登录
if (getToken()) {
  leaveTo(safeTarget(redirect))
}

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 门户自持登录：RSA 加密密码调门户登录接口，token 写父域 Cookie（由包内 auth.js 承担）
      ssoLogin({
        username: loginForm.value.username.trim(),
        password: encrypt(loginForm.value.password),
        captchaCode: loginForm.value.captchaCode,
        captchaId: loginForm.value.captchaId
      }).then((res) => {
        if (res.data?.token) {
          setToken(res.data.token)
        }
        loginMessage.value = res.data?.loginMessage
        if (res.data?.loginStatus !== 0) {
          // 登录失败（后端正常响应）：拉取验证码并展示
          getCode()
          return;
        }
        // 成功，600ms后按 redirect 回跳（无 redirect 时回门户首页）
        setTimeout(() => {
          leaveTo(safeTarget(redirect))
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
  position: relative;
  display: flex;
  justify-content: right;
  align-items: center;
  height: 100%;
  // 纯 CSS 渐变背景（替代原背景图，加快加载）：紫→白→橘呼应左侧小人配色，叠加两个柔光斑
  background:
    radial-gradient(circle at 22% 24%, rgba(124, 92, 252, 0.16), transparent 42%),
    radial-gradient(circle at 78% 78%, rgba(255, 155, 107, 0.14), transparent 46%),
    linear-gradient(135deg, #eef2ff 0%, #faf5ff 48%, #fff7ed 100%);
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
  // 浅色渐变背景下改用深灰，保证可读
  color: #94a3b8;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 40px;
  padding-left: 12px;
}
.pwd-toggle {
  cursor: pointer;
}
</style>
