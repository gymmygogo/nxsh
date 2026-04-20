<template>
  <view class="page">
    <view class="header">
      <view class="header-mark"><text class="header-mark-t">老</text></view>
      <text class="header-title">老人端登录</text>
      <text class="header-sub">账号登录</text>
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
  background: #f2f0ee;
  padding: 0 24px;
}
.header {
  padding-top: 56px;
  padding-bottom: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.header-mark {
  width: 56px; height: 56px; border-radius: 14px; background: #ffffff;
  border: 1px solid #e2ddd8; display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
}
.header-mark-t { font-size: 22px; font-weight: 700; color: #5c4033; }
.header-title { font-size: 24px; font-weight: 600; color: #1c1917; margin-bottom: 6px; }
.header-sub { font-size: 14px; color: #78716c; }
.form-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 28px 22px;
  border: 1px solid #e2ddd8;
}
.field { margin-bottom: 20px; }
.field-label { font-size: 14px; font-weight: 600; color: #57534e; display: block; margin-bottom: 8px; }
.field-input {
  height: 50px;
  font-size: 17px;
  padding: 0 14px;
  color: #1c1917;
  caret-color: #6b4f3c;
  background: #ffffff;
  border: 1px solid #e2ddd8;
  border-radius: 10px;
  -webkit-text-fill-color: #1c1917;
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
.checked { background: #6b4f3c; border-color: #6b4f3c; }
.check-mark { font-size: 13px; color: #fff; }
.remember-text { color: #78716c; font-size: 14px; }
.btn-login {
  width: 100%;
  height: 50px;
  line-height: 50px;
  background: #6b4f3c;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  border-radius: 10px;
  border: 1px solid #5c4033;
  box-shadow: none;
  letter-spacing: 2px;
}
.link-row { text-align: center; margin-top: 18px; }
.register-link { color: #6b4f3c; font-size: 14px; }
</style>
