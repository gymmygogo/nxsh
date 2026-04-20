<template>
  <view class="page">
    <view class="header-card">
      <view class="header-avatar"><text class="header-avatar-t">{{ userName.charAt(0) || '用' }}</text></view>
      <view class="header-info">
        <text class="header-name">{{ userName }}</text>
        <text class="header-phone">{{ phone }}</text>
      </view>
    </view>

    <view class="sos-wrapper">
      <button class="btn-sos" @click="handleSOS">一键呼救</button>
      <text class="sos-tip">如遇危急情况请立即点击</text>
    </view>

    <view class="actions">
      <view class="module-card card-bp" @click="goBloodPressure">
        <view class="module-abbr">血</view>
        <text class="module-text">血压记录</text>
      </view>
      <view class="module-card card-ai" @click="goAiCare">
        <view class="module-abbr">聊</view>
        <text class="module-text">暖心聊天</text>
      </view>
    </view>

    <button class="btn-logout" @click="handleLogout">退出登录</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { request } from '@/utils/request.js'

const userName = ref('王奶奶')
const phone = ref('138****1234')

const speak = (text) => {
  try {
    if (typeof plus !== 'undefined' && plus.tts) {
      plus.tts.speak(text)
    } else {
      const tts = uni.createInnerAudioContext()
      tts.src = ''
      tts.destroy()
    }
  } catch (e) {}
}

const handleSOS = () => {
  speak('正在发送求救信号，请稍候')
  uni.showLoading({ title: '定位中...', mask: true })

  // 1. Get GPS Location
  uni.getLocation({
    type: 'wgs84',
    success: (res) => {
      const latitude = res.latitude
      const longitude = res.longitude
      const elderlyId = uni.getStorageSync('elderlyId') || 1

      // 2. Report SOS to backend
      request({
        url: '/elderly/location/sos',
        method: 'POST',
        data: {
          elderlyId: Number(elderlyId),
          latitude: latitude,
          longitude: longitude,
          address: '获取地址中...' // Optional based on the backend
        },
        success: (apiRes) => {
          uni.hideLoading()
          if (apiRes.statusCode === 200 && apiRes.data.code === 200) {
            speak('求救已发送，正在拨号')
            const contactPhone = apiRes.data.data?.firstContactPhone
            if (contactPhone) {
               // 3. Make phone call
               uni.makePhoneCall({ phoneNumber: contactPhone })
            } else {
               uni.showToast({ title: '未设置紧急联系人', icon: 'none' })
            }
          } else {
             uni.showToast({ title: '上报失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.hideLoading()
          uni.showToast({ title: '网络异常', icon: 'none' })
        }
      })
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({ title: '无法获取定位', icon: 'none' })
      speak('无法获取您的位置')
    }
  })
}

const goBloodPressure = () => {
  uni.navigateTo({ url: '/pages/elderly/blood-pressure' })
}

const goAiCare = () => {
  uni.navigateTo({ url: '/pages/elderly/blood-pressure?tab=ai' })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '您确定要退出当前账号吗？',
    confirmText: '退出',
    cancelText: '取消',
    confirmColor: '#ff4d4f',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.redirectTo({ url: '/pages/login/elderly-login' })
      }
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh; background: #f2f0ee;
  padding: 20px 20px 30px; display: flex; flex-direction: column;
}
.header-card {
  background: #ffffff; border-radius: 14px; padding: 22px 18px;
  margin-bottom: 18px; border: 1px solid #e2ddd8;
  display: flex; align-items: center; gap: 16px;
}
.header-avatar {
  width: 48px; height: 48px; border-radius: 12px; background: #ede9e6;
  border: 1px solid #e2ddd8; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.header-avatar-t { font-size: 20px; font-weight: 700; color: #5c4033; }
.header-info { display: flex; flex-direction: column; }
.header-name { font-size: 24px; font-weight: 600; color: #1c1917; display: block; }
.header-phone { font-size: 15px; color: #78716c; display: block; margin-top: 4px; }
.sos-wrapper {
  display: flex; flex-direction: column; align-items: center; margin-bottom: 22px;
}
.btn-sos {
  width: 168px; height: 168px; border-radius: 50%;
  background: #b91c1c; color: #fff;
  font-size: 20px; font-weight: 600; display: flex; align-items: center; justify-content: center;
  box-shadow: none; margin-bottom: 10px; border: 1px solid #991b1b;
}
.sos-tip { font-size: 14px; color: #991b1b; font-weight: 500; }
.actions {
  flex: 1; display: flex; flex-direction: column; gap: 12px;
}
.module-card {
  height: 96px; border-radius: 14px; display: flex; align-items: center;
  justify-content: center; gap: 14px; border: 1px solid #e2ddd8;
}
.card-bp { background: #fafaf9; }
.card-ai { background: #f0fdf4; }
.module-abbr {
  width: 36px; height: 36px; border-radius: 10px; background: #ffffff;
  border: 1px solid #e2ddd8; font-size: 16px; font-weight: 700; color: #5c4033;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}
.card-ai .module-abbr { color: #166534; border-color: #bbf7d0; background: #ecfdf3; }
.module-text { font-size: 20px; font-weight: 600; color: #1c1917; }
.btn-logout {
  width: 100%; height: 52px; line-height: 52px;
  background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca;
  font-size: 16px; font-weight: 600; border-radius: 12px; margin-top: 22px;
}
</style>
