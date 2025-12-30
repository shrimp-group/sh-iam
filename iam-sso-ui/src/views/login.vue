<template>
  <div class="login">
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{ title }}</h3>
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号" @keyup.enter="handleLogin">
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="password" size="large" auto-complete="off" placeholder="密码" @keyup.enter="handleLogin">
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="captchaCode" v-if="loginForm.captchaId">
        <el-input v-model="loginForm.captchaCode" size="large" auto-complete="off" placeholder="验证码" style="width: 63%" @keyup.enter="handleLogin">
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="login-code"><img :src="codeUrl" @click="getCode" class="login-code-img"/></div>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button :loading="loading" size="large" type="primary" style="width:100%;" @click.prevent="handleLogin">
          <span v-if="loading">登 录 中...</span>
          <span v-else>登 录</span>
        </el-button>
        <div v-if="loginMessage" style="color: orange;">
          <span>
            {{loginMessage}}
          </span>
        </div>
        <el-divider />
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
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
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import {title} from '~/env';
import {captchaChart} from "../api/sso.js";

const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
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

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
}

const codeUrl = ref("")
const loading = ref(false)
// 注册开关
const register = ref(true)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 调用action的登录方法
      userStore.login(loginForm.value).then((res) => {
        loginMessage.value = res.data?.loginMessage
        if (res.data?.loginStatus !== 0) {
          getCode()
          return;
        }
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        // 成功，600ms后跳转
        setTimeout(() => {
          router.push({ path: redirect.value || "/", query: otherQueryParams })
        }, 600)
      }).catch(() => {
        getCode()
      }).finally(() => {
        loading.value = false
      })
    }
  })
}

function getCode() {
  captchaChart().then(res => {
    codeUrl.value = res.data.captchaImage
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
  margin: 24px 0 0 0;
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
