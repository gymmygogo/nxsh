<template>
  <view class="page">
    <view class="hero">
      <text class="hero-emoji">💊</text>
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
        <button class="btn-primary" @click="handleTake">✔ 全部已服</button>
        <button class="btn-secondary" @click="handleSnooze">稍后提醒</button>
      </view>
    </view>

    <view v-else class="empty-card">
      <text class="empty-icon">🎉</text>
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
  // #ifdef APP-PLUS
  // 播放标准慢节奏铃声（系统通知音）
  try {
    const bell = uni.createInnerAudioContext()
    bell.src = '/static/audio/medicine-bell.mp3'
    bell.onEnded(() => {
      bell.destroy()
      playVoice()
    })
    bell.onError(() => {
      bell.destroy()
      playVoice() // 铃声失败也继续播语音
    })
    bell.play()
    return
  } catch (e) {}
  // #endif
  // H5 或铃声异常直接播语音
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
  min-height: 100vh; background: linear-gradient(180deg, #FFF6F0 0%, #FFFFFF 40%);
  padding: 0 24px; display: flex; flex-direction: column;
}
.hero { padding-top: 60px; padding-bottom: 20px; text-align: center; }
.hero-emoji { font-size: 52px; display: block; margin-bottom: 10px; }
.hero-title { font-size: 28px; font-weight: 800; color: #2D2D2D; }
.card {
  background: #FFFFFF; border-radius: 20px; padding: 24px 20px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06); flex: 1; display: flex; flex-direction: column;
}
.time-display { font-size: 24px; color: #E8825A; font-weight: 800; text-align: center; display: block; margin-bottom: 20px; }
.med-list { flex: 1; display: flex; flex-direction: column; gap: 14px; margin-bottom: 24px; }
.med-card {
  background: #FFF8F3; border: 1.5px solid #F0E6DE; border-radius: 16px;
  padding: 18px; display: flex; flex-direction: column;
}
.med-name { font-size: 24px; font-weight: 800; color: #2D2D2D; margin-bottom: 6px; }
.med-dose { font-size: 18px; color: #A0836C; font-weight: 600; }
.actions { display: flex; flex-direction: column; gap: 14px; margin-bottom: 30px; }
.btn-primary {
  background: linear-gradient(135deg, #5CD97E, #34C759); color: #fff;
  font-size: 22px; font-weight: 800; height: 72px; line-height: 72px;
  border-radius: 18px; border: none; box-shadow: 0 6px 20px rgba(52,199,89,0.3);
  letter-spacing: 2px;
}
.btn-secondary {
  background: #FFF1E6; color: #E8825A; border: 1px solid #F0E6DE;
  font-size: 18px; font-weight: 600; height: 56px; line-height: 56px; border-radius: 14px;
}
.empty-card { text-align: center; padding: 80px 0; }
.empty-icon { font-size: 48px; display: block; margin-bottom: 12px; }
.empty-text { font-size: 18px; color: #B0A090; }
</style>
