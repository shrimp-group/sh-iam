import router from '@/router'
import { getToken, setToken, removeToken } from '@/utils/auth'
import defAva from '@/assets/images/profile.jpg'
import {ssoLogin, ssoLogout} from "@/api/sso";
import {Base64} from 'js-base64';

const useUserStore = defineStore(
  'user',
  {
    state: () => ({
      userCode: '',
      username: '',
      nickname: '',
      avatar: '',
    }),
    actions: {
      // 登录
      login(userInfo) {
        return new Promise((resolve, reject) => {
            ssoLogin({
              username: userInfo.username.trim(),
              password: userInfo.password,
              captchaCode: userInfo.captchaCode,
              captchaId: userInfo.captchaId
          }).then(res => {
            if (res.data?.token) {
              setToken(res.data?.token)
            }
            resolve(res)
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 获取用户信息
      getInfo() {
        return new Promise((resolve, reject) => {
          const token = getToken();
          if (!token) {
              reject()
          }
          const jwt = token.split(".");
          if (jwt.length !== 3) {
              reject("token 格式不正确");
              return;
          }
          const payload = jwt[1];
          let userInfo = Base64.decode(payload);
          userInfo = JSON.parse(userInfo);

          this.userCode = userInfo.userCode
          this.username = userInfo.username
          this.nickname = userInfo.nickname
          this.avatar = userInfo.avatar || defAva;

          /* 初始密码提示 */
          /*
          if(res.isDefaultModifyPwd) {
            ElMessageBox.confirm('您的密码还是初始密码，请修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
              router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
            }).catch(() => {})
          }
          */
          /* 过期密码提示 */
          /*
          if(!res.isDefaultModifyPwd && res.isPasswordExpired) {
            ElMessageBox.confirm('您的密码已过期，请尽快修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
              router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
            }).catch(() => {})
          }
          */

          resolve(userInfo)
        })
      },
      // 退出系统
      logOut() {
        return new Promise((resolve, reject) => {
          ssoLogout(this.token).then(() => {
            this.token = ''
            removeToken()
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      }
    }
  })

export default useUserStore
