<template>
  <view class="page">
    <view class="header">
      <view class="header-mark"><text class="header-mark-t">家</text></view>
      <text class="header-title">家属端登录</text>
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

const rememberKey = 'familyRemember'
const accountKey = 'familyAccount'

const parseId = (value) => {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) && n > 0 ? n : null
}

const isFamilyBound = (status) => {
  const boundFlag = status?.bound === true || status?.bound === 1 || status?.bound === '1'
  return boundFlag && parseId(status?.elderlyId) !== null
}

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
    url: '/family/login',
    method: 'POST',
    data: {
      phone: phone.value,
      password: password.value
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        // 1. 一次性获取后端返回的所有重要数据
        const token = res.data.data?.token
        const familyId = res.data.data?.familyId
        const hasBound = res.data.data?.hasBound   // 我们新增的绑定状态
        const elderlyId = res.data.data?.elderlyId // 我们新增的老人ID
        if (!token) {
          uni.showToast({ title: '登录失败：缺少令牌', icon: 'none' })
          return
        }
        // 2. 基础信息存入本地缓存
        uni.setStorageSync('token', token)
        uni.setStorageSync('userType', 'family')
        uni.setStorageSync('familyPhone', phone.value)
        if (familyId) {
          uni.setStorageSync('familyId', familyId)
        }
        saveRemember()

        if (!familyId) {
          uni.redirectTo({ url: '/pages/family/settings' })
          return
        }
//  核心修改：直接干掉嵌套请求，用第一次拿到的数据判断！
        if (hasBound && elderlyId) {
          uni.setStorageSync('elderlyId', elderlyId)
          uni.reLaunch({ url: '/pages/family/main' }) // 使用 reLaunch 保证成功跳转主页
        } else {
          uni.removeStorageSync('elderlyId')
          uni.redirectTo({ url: '/pages/family/settings' })
        }

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
  uni.navigateTo({ url: '/pages/login/family-register' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f0f1f3;
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
  border: 1px solid #e6e8ec; display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
}
.header-mark-t { font-size: 22px; font-weight: 700; color: #1e4a72; }
.header-title { font-size: 24px; font-weight: 600; color: #1a1d21; margin-bottom: 6px; }
.header-sub { font-size: 14px; color: #6b7280; }
.form-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 28px 22px;
  border: 1px solid #e6e8ec;
}
.field { margin-bottom: 20px; }
.field-label { font-size: 14px; font-weight: 600; color: #6b7280; display: block; margin-bottom: 8px; }
.field-input {
  height: 50px;
  font-size: 17px;
  padding: 0 14px;
  color: #1a1d21;
  caret-color: #1e4a72;
  background: #ffffff;
  border: 1px solid #e6e8ec;
  border-radius: 10px;
  -webkit-text-fill-color: #1a1d21;
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
  border: 2px solid #B8CCDD;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.checked { background: #1e4a72; border-color: #1e4a72; }
.check-mark { font-size: 13px; color: #fff; }
.remember-text { color: #6b7280; font-size: 14px; }
.btn-login {
  width: 100%;
  height: 50px;
  line-height: 50px;
  background: #1e4a72;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  border-radius: 10px;
  border: 1px solid #1e4a72;
  box-shadow: none;
  letter-spacing: 2px;
}
.link-row { text-align: center; margin-top: 18px; }
.register-link { color: #1e4a72; font-size: 14px; }
</style>
