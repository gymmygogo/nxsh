<template>
  <view class="page">
    <view class="header">
      <text class="header-emoji">📝</text>
      <text class="header-title">老人端注册</text>
      <text class="header-sub">创建账号，开启暖心守护</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">手机号</text>
        <input class="field-input" type="number" v-model="phone" placeholder="请输入手机号" maxlength="11" />
      </view>

      <view class="field">
        <text class="field-label">验证码</text>
        <view class="code-row">
          <input class="field-input code-input" type="number" v-model="verifyCode" placeholder="请输入验证码" maxlength="6" />
          <button class="btn-code" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </button>
        </view>
      </view>

      <view class="field">
        <text class="field-label">姓名</text>
        <input class="field-input" type="text" v-model="name" placeholder="请输入姓名" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <input class="field-input" type="password" v-model="password" placeholder="请输入密码" />
      </view>
      <view class="field">
        <text class="field-label">确认密码</text>
        <input class="field-input" type="password" v-model="confirmPassword" placeholder="请再次输入密码" />
      </view>

      <button class="btn-register" @click="register">注 册</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { request } from '@/utils/request.js'

const phone = ref('')
const name = ref('')
const password = ref('')
const confirmPassword = ref('')
const verifyCode = ref('')
const countdown = ref(0)
let timer = null

const startCountdown = () => {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const sendCode = () => {
  if (!phone.value || phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  request({
    url: '/elderly/sendCode',
    method: 'POST',
    data: { phone: phone.value },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        startCountdown()
      } else {
        uni.showToast({ title: res.data?.message || '发送失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络请求失败，请检查后端服务', icon: 'none' })
    }
  })
}

const register = () => {
  if (!phone.value || !name.value || !password.value || !confirmPassword.value || !verifyCode.value) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  request({
    url: '/elderly/register',
    method: 'POST',
    data: {
      phone: phone.value,
      name: name.value,
      password: password.value,
      verifyCode: verifyCode.value
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 800)
      } else {
        uni.showToast({ title: res.data?.message || '注册失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络请求失败，请检查后端服务', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #FFF1E6 0%, #FFFFFF 40%); padding: 0 24px; }
.header { padding-top: 50px; padding-bottom: 28px; display: flex; flex-direction: column; align-items: center; }
.header-emoji { font-size: 40px; margin-bottom: 10px; }
.header-title { font-size: 24px; font-weight: 800; color: #2D2D2D; margin-bottom: 4px; }
.header-sub { font-size: 13px; color: #A0836C; }
.form-card { background: #FFFFFF; border-radius: 20px; padding: 24px 20px; box-shadow: 0 4px 24px rgba(0,0,0,0.06); }
.field { margin-bottom: 16px; }
.field-label { font-size: 13px; font-weight: 600; color: #8C7A6B; display: block; margin-bottom: 6px; }
.field-input {
  height: 48px; font-size: 16px; padding: 0 14px; color: #2D2D2D; caret-color: #E8825A;
  background: #FFF8F3; border: 1px solid #F0E6DE; border-radius: 12px; -webkit-text-fill-color: #2D2D2D;
}
.code-row { display: flex; align-items: center; gap: 10px; }
.code-input { flex: 1; }
.btn-code {
  flex-shrink: 0; width: 120px; height: 48px; line-height: 48px; font-size: 13px; font-weight: 600;
  background: #FFF1E6; color: #E8825A; border-radius: 12px; border: 1px solid #F0E6DE; text-align: center; padding: 0;
}
.btn-code[disabled] { color: #C4B0A0; background: #FFF8F3; }
.btn-register {
  width: 100%; height: 50px; line-height: 50px; margin-top: 8px;
  background: linear-gradient(135deg, #FF9A56 0%, #E8825A 100%); color: #fff;
  font-size: 18px; font-weight: 700; border-radius: 14px; border: none;
  box-shadow: 0 4px 16px rgba(232,130,90,0.3); letter-spacing: 4px;
}
</style>
