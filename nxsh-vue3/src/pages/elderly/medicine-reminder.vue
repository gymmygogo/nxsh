<template>
  <view class="page">
    <view class="hero">
      <view class="hero-mark"><text class="hero-mark-t">药</text></view>
      <text class="hero-title">用药提醒</text>
    </view>

    <view v-if="dueGroup" class="card">
      <text class="time-display">{{ formatTime(dueGroup.planTime) }} 需服药品</text>

      <view class="med-list">
        <view class="med-card" v-for="item in dueGroup.medicines" :key="item.medicineId">
          <text class="med-name">{{ item.name }}</text>
          <text class="med-dose">{{ item.dosageDesc }}</text>
        </view>
      </view>

      <view class="actions">
        <button class="btn-primary" @click="handleTake">全部已服</button>
        <button class="btn-secondary" @click="handleSnooze">稍后提醒</button>
      </view>
    </view>

    <view v-else class="empty-card">
      <view class="empty-line" />
      <text class="empty-text">暂无需要服用的药品</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '@/utils/request.js'

const dueGroup = ref(null)

onLoad((options) => {
  if (options.data) {
    try {
      dueGroup.value = JSON.parse(decodeURIComponent(options.data))

      // #ifdef APP-PLUS
      // 自动唤醒屏幕
      try {
        plus.device.wakeup()
        plus.navigator.setFullscreen(false)
      } catch (e) { console.warn('唤醒屏幕失败', e) }
      // #endif

      // 先播铃声，铃声结束后再播语音
      playBellThenVoice()
    } catch (e) {
      console.error('Failed to parse due medicines', e)
    }
  }
})

const playBellThenVoice = () => {
  // 尝试播放默认铃声，失败则直接播语音
  try {
    const bell = uni.createInnerAudioContext()
    bell.src = '/static/audio/medicine-bell.mp3'
    bell.onEnded(() => {
      bell.destroy()
      playVoice()
    })
    bell.onError(() => {
      bell.destroy()
      playVoice()
    })
    bell.play()
    return
  } catch (e) {
    // ignored
  }
  playVoice()
}

const playVoice = () => {
  if (!dueGroup.value) return
  let text = '您好，现在是用药时间，请服用：'
  dueGroup.value.medicines.forEach(m => {
    text += `${m.name} ${m.dosageDesc}，`
  })

  if (dueGroup.value.familyVoiceUrl) {
    // 播放家属亲情语音
    const audio = uni.createInnerAudioContext()
    audio.src = dueGroup.value.familyVoiceUrl
    audio.play()
  } else {
    // 系统TTS语音播报药名与剂量
    // #ifdef APP-PLUS
    try {
      if (plus.tts) {
        plus.tts.speak(text)
      }
    } catch (e) {}
    // #endif
  }
}

const formatTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const handleTake = () => {
  const elderlyId = uni.getStorageSync('elderlyId') || 1
  const medicineIds = dueGroup.value.medicines.map(m => m.medicineId)

  request({
    url: '/medicine/take',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId),
      planTime: dueGroup.value.planTime,
      medicineIds
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        uni.showToast({ title: '记录成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1500)
      } else {
        uni.showToast({ title: '请求失败', icon: 'none' })
      }
    }
  })
}

const handleSnooze = () => {
  const elderlyId = uni.getStorageSync('elderlyId') || 1
  const medicineIds = dueGroup.value.medicines.map(m => m.medicineId)

  request({
    url: '/medicine/snooze',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId),
      planTime: dueGroup.value.planTime,
      medicineIds
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        uni.showToast({ title: '已推迟10分钟提醒', icon: 'none' })
        setTimeout(() => uni.navigateBack(), 1500)
      } else {
        uni.showToast({ title: '请求失败', icon: 'none' })
      }
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh; background: #f2f0ee;
  padding: 0 24px; display: flex; flex-direction: column;
}
.hero { padding-top: 56px; padding-bottom: 20px; text-align: center; display: flex; flex-direction: column; align-items: center; }
.hero-mark {
  width: 56px; height: 56px; border-radius: 14px; background: #fff; border: 1px solid #e2ddd8;
  display: flex; align-items: center; justify-content: center; margin-bottom: 12px;
}
.hero-mark-t { font-size: 22px; font-weight: 700; color: #5c4033; }
.hero-title { font-size: 24px; font-weight: 600; color: #1c1917; }
.card {
  background: #ffffff; border-radius: 14px; padding: 24px 20px;
  border: 1px solid #e2ddd8; flex: 1; display: flex; flex-direction: column;
}
.time-display { font-size: 22px; color: #6b4f3c; font-weight: 600; text-align: center; display: block; margin-bottom: 20px; }
.med-list { flex: 1; display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
.med-card {
  background: #fafaf9; border: 1px solid #e2ddd8; border-radius: 12px;
  padding: 18px; display: flex; flex-direction: column;
}
.med-name { font-size: 22px; font-weight: 600; color: #1c1917; margin-bottom: 6px; }
.med-dose { font-size: 17px; color: #78716c; font-weight: 500; }
.actions { display: flex; flex-direction: column; gap: 12px; margin-bottom: 30px; }
.btn-primary {
  background: #166534; color: #fff;
  font-size: 19px; font-weight: 600; height: 64px; line-height: 64px;
  border-radius: 12px; border: 1px solid #14532d; box-shadow: none;
  letter-spacing: 1px;
}
.btn-secondary {
  background: #ffffff; color: #6b4f3c; border: 1px solid #e2ddd8;
  font-size: 17px; font-weight: 600; height: 52px; line-height: 52px; border-radius: 12px;
}
.empty-card { text-align: center; padding: 80px 0; }
.empty-line {
  width: 28px; height: 2px; background: #d6d3d1; border-radius: 1px;
  margin: 0 auto 12px;
}
.empty-text { font-size: 17px; color: #a8a29e; }
</style>
