<template>
  <view class="page">
    <view class="hero">
      <text class="hero-emoji">❤️</text>
      <text class="hero-title">测量提醒</text>
    </view>

    <view class="card">
      <text class="time-display">{{ remindTime }}</text>
      <text class="remind-msg">{{ msg }}</text>
    </view>

    <view class="actions">
      <button class="btn-primary" @click="goMeasure">去记录</button>
      <button class="btn-secondary" @click="dismiss">稍后</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const remindTime = ref('')
const msg = ref('')

onLoad((options) => {
  if (options.time) remindTime.value = decodeURIComponent(options.time)
  if (options.msg) msg.value = decodeURIComponent(options.msg)
  else msg.value = '时间到了，请测量您的血压'

  // #ifdef APP-PLUS
  // 自动唤醒屏幕
  try {
    plus.device.wakeup()
    plus.navigator.setFullscreen(false)
  } catch (e) { console.warn('唤醒屏幕失败', e) }
  // #endif

  // TTS 语音播报
  try {
    // #ifdef APP-PLUS
    if (plus.tts) {
      plus.tts.speak(msg.value)
    }
    // #endif
  } catch (e) {}
})

const goMeasure = () => {
  uni.redirectTo({ url: '/pages/elderly/blood-pressure' })
}

const dismiss = () => {
  uni.navigateBack()
}
</script>

<style scoped>
.page {
  min-height: 100vh; background: linear-gradient(180deg, #FFF6F0 0%, #FFFFFF 40%);
  padding: 0 24px; display: flex; flex-direction: column;
}
.hero { padding-top: 80px; padding-bottom: 30px; text-align: center; }
.hero-emoji { font-size: 56px; display: block; margin-bottom: 12px; }
.hero-title { font-size: 28px; font-weight: 800; color: #2D2D2D; }
.card {
  background: #FFFFFF; border-radius: 20px; padding: 30px 20px; text-align: center;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06); margin-bottom: 40px;
}
.time-display { font-size: 36px; color: #E8825A; font-weight: 800; display: block; margin-bottom: 12px; }
.remind-msg { font-size: 20px; color: #8C7A6B; display: block; line-height: 30px; }
.actions { display: flex; flex-direction: column; gap: 16px; margin-top: auto; padding-bottom: 40px; }
.btn-primary {
  background: linear-gradient(135deg, #FF9A56, #E8825A); color: #fff;
  font-size: 24px; font-weight: 800; height: 80px; line-height: 80px;
  border-radius: 18px; border: none; box-shadow: 0 6px 20px rgba(232,130,90,0.3);
  letter-spacing: 4px;
}
.btn-secondary {
  background: #FFF1E6; color: #E8825A; border: 1px solid #F0E6DE;
  font-size: 18px; font-weight: 600; height: 60px; line-height: 60px; border-radius: 14px;
}
</style>
