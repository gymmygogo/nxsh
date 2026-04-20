<template>
  <view class="page">
    <view class="header">
      <view class="header-mark"><text class="header-mark-t">注</text></view>
      <text class="header-title">家属端注册</text>
      <text class="header-sub">创建家属账号</text>
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
    url: '/family/sendCode',
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
    url: '/family/register',
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
        uni.showToast({ title: res.data?.message || res.data?.msg || '注册失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络请求失败，请检查后端服务', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f0f1f3; padding: 0 24px; }
.header { padding-top: 48px; padding-bottom: 24px; display: flex; flex-direction: column; align-items: center; }
.header-mark {
  width: 48px; height: 48px; border-radius: 12px; background: #fff; border: 1px solid #e6e8ec;
  display: flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.header-mark-t { font-size: 18px; font-weight: 700; color: #1e4a72; }
.header-title { font-size: 22px; font-weight: 600; color: #1a1d21; margin-bottom: 4px; }
.header-sub { font-size: 13px; color: #6b7280; }
.form-card { background: #ffffff; border-radius: 14px; padding: 24px 20px; border: 1px solid #e6e8ec; }
.field { margin-bottom: 16px; }
.field-label { font-size: 13px; font-weight: 600; color: #6b7280; display: block; margin-bottom: 6px; }
.field-input {
  height: 48px; font-size: 16px; padding: 0 14px; color: #1a1d21; caret-color: #1e4a72;
  background: #ffffff; border: 1px solid #e6e8ec; border-radius: 10px; -webkit-text-fill-color: #1a1d21;
}
.code-row { display: flex; align-items: center; gap: 10px; }
.code-input { flex: 1; }
.btn-code {
  flex-shrink: 0; width: 120px; height: 48px; line-height: 48px; font-size: 13px; font-weight: 600;
  background: #f4f6f9; color: #1e4a72; border-radius: 10px; border: 1px solid #d8dee6; text-align: center; padding: 0;
}
.btn-code[disabled] { color: #9ca3af; background: #f3f4f6; }
.btn-register {
  width: 100%; height: 50px; line-height: 50px; margin-top: 8px;
  background: #1e4a72; color: #fff;
  font-size: 17px; font-weight: 600; border-radius: 10px; border: 1px solid #1e4a72;
  box-shadow: none; letter-spacing: 2px;
}
</style>
