<template>
  <view class="page">
    <view class="header-card">
      <text class="header-emoji">👴</text>
      <view class="header-info">
        <text class="header-name">{{ userName }}</text>
        <text class="header-phone">{{ phone }}</text>
      </view>
    </view>

    <view class="sos-wrapper">
      <button class="btn-sos" @click="handleSOS">🆘 一键呼救</button>
      <text class="sos-tip">如遇危急情况请立即点击</text>
    </view>

    <view class="actions">
      <view class="module-card card-bp" @click="goBloodPressure">
        <text class="module-icon">❤️</text>
        <text class="module-text">血压记录</text>
      </view>
      <view class="module-card card-ai" @click="goAiCare">
        <text class="module-icon">💬</text>
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
  min-height: 100vh; background: linear-gradient(180deg, #FFF6F0 0%, #FFFFFF 35%);
  padding: 20px 20px 30px; display: flex; flex-direction: column;
}
.header-card {
  background: #FFFFFF; border-radius: 18px; padding: 24px 20px;
  margin-bottom: 20px; box-shadow: 0 2px 16px rgba(0,0,0,0.05);
  display: flex; align-items: center; gap: 16px;
}
.header-emoji { font-size: 40px; }
.header-info { display: flex; flex-direction: column; }
.header-name { font-size: 28px; font-weight: 800; color: #2D2D2D; display: block; }
.header-phone { font-size: 16px; color: #A0836C; display: block; margin-top: 4px; }
.sos-wrapper {
  display: flex; flex-direction: column; align-items: center; margin-bottom: 24px;
}
.btn-sos {
  width: 180px; height: 180px; border-radius: 50%;
  background: linear-gradient(135deg, #FF7B7B, #FF4D4F); color: #fff;
  font-size: 28px; font-weight: 800; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 30px rgba(255,77,79,0.4); margin-bottom: 12px; border: none;
}
.sos-tip { font-size: 14px; color: #FF4D4F; font-weight: 600; }
.actions {
  flex: 1; display: flex; flex-direction: column; gap: 14px;
}
.module-card {
  height: 100px; border-radius: 18px; display: flex; align-items: center;
  justify-content: center; gap: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.card-bp { background: linear-gradient(135deg, #FFF1E6, #FFE4D4); }
.card-ai { background: linear-gradient(135deg, #E8F8E8, #D4F0D4); }
.module-icon { font-size: 28px; }
.module-text { font-size: 22px; font-weight: 700; color: #2D2D2D; }
.btn-logout {
  width: 100%; height: 56px; line-height: 56px;
  background: #FFF1F0; color: #FF4D4F; border: 1px solid #FFD6D6;
  font-size: 18px; font-weight: 600; border-radius: 14px; margin-top: 24px;
}
</style>
