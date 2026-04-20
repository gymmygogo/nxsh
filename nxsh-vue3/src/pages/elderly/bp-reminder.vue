<template>
  <view class="page">
    <view class="hero">
      <view class="hero-mark"><text class="hero-mark-t">测</text></view>
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
  min-height: 100vh; background: #f2f0ee;
  padding: 0 24px; display: flex; flex-direction: column;
}
.hero { padding-top: 72px; padding-bottom: 28px; text-align: center; display: flex; flex-direction: column; align-items: center; }
.hero-mark {
  width: 56px; height: 56px; border-radius: 14px; background: #fff; border: 1px solid #e2ddd8;
  display: flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.hero-mark-t { font-size: 22px; font-weight: 700; color: #5c4033; }
.hero-title { font-size: 24px; font-weight: 600; color: #1c1917; }
.card {
  background: #ffffff; border-radius: 14px; padding: 30px 20px; text-align: center;
  border: 1px solid #e2ddd8; margin-bottom: 40px;
}
.time-display { font-size: 32px; color: #6b4f3c; font-weight: 600; display: block; margin-bottom: 12px; }
.remind-msg { font-size: 18px; color: #57534e; display: block; line-height: 28px; }
.actions { display: flex; flex-direction: column; gap: 14px; margin-top: auto; padding-bottom: 40px; }
.btn-primary {
  background: #6b4f3c; color: #fff;
  font-size: 20px; font-weight: 600; height: 72px; line-height: 72px;
  border-radius: 12px; border: 1px solid #5c4033; box-shadow: none;
  letter-spacing: 2px;
}
.btn-secondary {
  background: #ffffff; color: #6b4f3c; border: 1px solid #e2ddd8;
  font-size: 17px; font-weight: 600; height: 56px; line-height: 56px; border-radius: 12px;
}
</style>
