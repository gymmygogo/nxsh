<template>
  <view class="page">
    <view class="header">
      <text class="header-emoji">👴</text>
      <text class="header-title">老人端登录</text>
      <text class="header-sub">欢迎回来，暖夕一直在</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">手机号</text>
        <input class="field-input" type="number" v-model="phone" placeholder="请输入手机号" maxlength="11" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <input class="field-input" type="password" v-model="password" placeholder="请输入密码" />
      </view>

      <view class="remember-row" @click="toggleRemember">
        <view class="checkbox" :class="remember ? 'checked' : ''">
          <text v-if="remember" class="check-mark">✓</text>
        </view>
        <text class="remember-text">记住账号密码</text>
      </view>

      <button class="btn-login" @click="login">登 录</button>
      <view class="link-row">
        <text class="register-link" @click="goToRegister">没有账号？点击注册</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { request } from '@/utils/request.js'

const phone = ref('')
const password = ref('')
const remember = ref(false)

const rememberKey = 'elderlyRemember'
const accountKey = 'elderlyAccount'

onMounted(() => {
  const r = uni.getStorageSync(rememberKey)
  remember.value = r === true
  if (remember.value) {
    const acc = uni.getStorageSync(accountKey)
    if (acc) {
      phone.value = acc.phone || ''
      password.value = acc.password || ''
    }
  }
})

const toggleRemember = () => {
  remember.value = !remember.value
}

const saveRemember = () => {
  uni.setStorageSync(rememberKey, remember.value)
  if (remember.value) {
    uni.setStorageSync(accountKey, { phone: phone.value, password: password.value })
  } else {
    uni.removeStorageSync(accountKey)
  }
}

const login = () => {
  if (!phone.value || !password.value) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }

  request({
    url: '/elderly/login',
    method: 'POST',
    data: {
      phone: phone.value,
      password: password.value
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        const token = res.data.data?.token || 'mock_elderly_token'
        const elderlyId = res.data.data?.elderlyId
        const elderlyName = res.data.data?.name || ''
        const elderlyNickname = res.data.data?.nickname || ''
        uni.setStorageSync('token', token)
        uni.setStorageSync('userType', 'elderly')
        uni.setStorageSync('elderlyPhone', phone.value)
        uni.setStorageSync('elderlyName', elderlyName)
        uni.setStorageSync('elderlyNickname', elderlyNickname)
        if (elderlyId) {
          uni.setStorageSync('elderlyId', elderlyId)
        }

        saveRemember()
        uni.redirectTo({ url: '/pages/elderly/blood-pressure' })
      } else {
        uni.showToast({ title: res.data?.message || '登录失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络请求失败，请检查后端服务', icon: 'none' })
    }
  })
}

const goToRegister = () => {
  uni.navigateTo({ url: '/pages/login/elderly-register' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF1E6 0%, #FFFFFF 40%);
  padding: 0 24px;
}
.header {
  padding-top: 60px;
  padding-bottom: 36px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.header-emoji { font-size: 48px; margin-bottom: 12px; }
.header-title { font-size: 26px; font-weight: 800; color: #2D2D2D; margin-bottom: 6px; }
.header-sub { font-size: 14px; color: #A0836C; }
.form-card {
  background: #FFFFFF;
  border-radius: 20px;
  padding: 28px 22px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}
.field { margin-bottom: 20px; }
.field-label { font-size: 14px; font-weight: 600; color: #8C7A6B; display: block; margin-bottom: 8px; }
.field-input {
  height: 50px;
  font-size: 17px;
  padding: 0 14px;
  color: #2D2D2D;
  caret-color: #E8825A;
  background: #FFF8F3;
  border: 1px solid #F0E6DE;
  border-radius: 12px;
  -webkit-text-fill-color: #2D2D2D;
}
.remember-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}
.checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid #D9C9BB;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.checked { background: #E8825A; border-color: #E8825A; }
.check-mark { font-size: 13px; color: #fff; }
.remember-text { color: #8C7A6B; font-size: 14px; }
.btn-login {
  width: 100%;
  height: 50px;
  line-height: 50px;
  background: linear-gradient(135deg, #FF9A56 0%, #E8825A 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(232, 130, 90, 0.3);
  letter-spacing: 4px;
}
.link-row { text-align: center; margin-top: 18px; }
.register-link { color: #E8825A; font-size: 14px; }
</style>
