<template>
  <view class="ai-wrap">
    <!-- 顶部欢迎栏 -->
    <view class="ai-header">
      <view class="ai-header-mark"><text class="ai-header-mark-t">聊</text></view>
      <view>
        <text class="ai-header-title">暖心聊天</text>
        <text class="ai-header-sub">语音或文字交流</text>
      </view>
    </view>

    <!-- 聊天消息区 -->
    <view class="chat-list">
      <view
        v-for="msg in messages"
        :key="msg.id"
        class="row"
        :class="msg.role === 'user' ? 'row-right' : 'row-left'"
      >
        <view v-if="msg.role === 'ai'" class="avatar avatar-ai"><text class="avatar-letter">伴</text></view>
        <view class="bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
          <text v-if="msg.text" class="bubble-text">{{ msg.text }}</text>
          <text v-else class="bubble-text">{{ msg.placeholder || '[语音消息]' }}</text>
        </view>
        <view v-if="msg.role === 'user'" class="avatar avatar-user"><text class="avatar-letter">我</text></view>
      </view>

      <!-- 播放 + 重听 -->
      <view v-if="messages.length > 1" class="replay-row">
        <view class="replay-btn" @click="replayLast">再听一遍</view>
      </view>

      <view v-if="thinking" class="thinking">
        <view class="dot-box">
          <text class="dot d1">●</text>
          <text class="dot d2">●</text>
          <text class="dot d3">●</text>
        </view>
        <text class="thinking-text">正在回复…</text>
      </view>
    </view>

    <!-- 底部操作区 -->
    <view class="ai-bottom">
      <view
        class="btn-talk"
        :class="recording ? 'btn-talk-active' : ''"
        @touchstart="startRecord"
        @touchend="stopRecord"
        @touchcancel="stopRecord"
        @mousedown="startRecord"
        @mouseup="stopRecord"
      >
        <text class="btn-talk-text">{{ recording ? '松开发送' : '按住说话' }}</text>
      </view>
      <view class="input-row">
        <input class="text-input" v-model="textDraft" placeholder="也可以打字输入" @confirm="sendText" />
        <button class="btn-send" @click="sendText">发送</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { request, uploadFile } from '@/utils/request.js'
import { BASE_URL } from '@/config.js'

// 编译时平台检测（运行时检测 uni.getRecorderManager 不可靠，H5 返回空壳对象）
let PLATFORM_H5 = false
// #ifdef H5
PLATFORM_H5 = true
// #endif

const messages = ref([])
const recording = ref(false)
const thinking = ref(false)
const textDraft = ref('')

let recorder = null
let innerAudio = null

const scrollToBottom = () => {
  nextTick(() => {
    uni.pageScrollTo({
      scrollTop: 999999, // 设置一个足够大的值
      duration: 100
    })
  })
}

const initWelcome = () => {
  messages.value = [
    {
      id: String(Date.now()),
      role: 'ai',
      text: '你好呀，我在呢。按住下面说话就行～',
      voiceUrl: ''
    }
  ]
}

// H5 浏览器录音相关
let h5MediaRecorder = null
let h5AudioChunks = []
let h5Stream = null
// isH5 已用编译时常量 PLATFORM_H5 替代

const handleRecordResult = (blob, filePath) => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    thinking.value = false
    uni.showToast({ title: '未登录老人账号', icon: 'none' })
    return
  }

  // 先插入一条“语音占位消息”，后续接口成功后再把 text 回填成识别文字
  const voiceMsg = addUserVoiceMsg(filePath || '[语音]')

  if (blob) {
    // H5 模式：用 XMLHttpRequest 上传 Blob
    const formData = new FormData()
    formData.append('voiceFile', blob, 'voice.webm')
    formData.append('elderlyId', Number(elderlyId))

    const token = uni.getStorageSync('token')

    const xhr = new XMLHttpRequest()
    xhr.open('POST', BASE_URL + '/ai/care/chat/voice')
    if (token) xhr.setRequestHeader('Authorization', token)
    xhr.onload = () => {
      thinking.value = false
      try {
        const data = JSON.parse(xhr.responseText)
        if (data.code === 200 && data.data) {
          // 回填语音识别文本，让页面看到老人说了什么
          if (data.data.userText) patchMsgText(voiceMsg.id, data.data.userText)

          addAiMsg({ aiText: data.data.aiText, aiVoiceUrl: data.data.aiVoiceUrl })
          if (data.data.aiVoiceUrl) playAudio(data.data.aiVoiceUrl)
        } else {
          patchMsgPlaceholder(voiceMsg.id, '[语音消息]')
          uni.showToast({ title: data.message || '回复失败', icon: 'none' })
        }
      } catch (e) {
        patchMsgPlaceholder(voiceMsg.id, '[语音消息]')
        uni.showToast({ title: '解析失败', icon: 'none' })
      }
    }
    xhr.onerror = () => {
      thinking.value = false
      patchMsgPlaceholder(voiceMsg.id, '[语音消息]')
      uni.showToast({ title: '上传失败', icon: 'none' })
    }
    xhr.send(formData)
  } else if (filePath) {
    // App 模式：用 uni.uploadFile 上传本地文件
    uploadFile({
      url: '/ai/care/chat/voice',
      filePath,
      name: 'voiceFile',
      formData: { elderlyId: String(elderlyId) },
      success: (uploadRes) => {
        thinking.value = false
        try {
          const data = JSON.parse(uploadRes.data)
          if (data.code === 200 && data.data) {
            if (data.data.userText) patchMsgText(voiceMsg.id, data.data.userText)

            addAiMsg({ aiText: data.data.aiText, aiVoiceUrl: data.data.aiVoiceUrl })
            if (data.data.aiVoiceUrl) playAudio(data.data.aiVoiceUrl)
          } else {
            patchMsgPlaceholder(voiceMsg.id, '[语音消息]')
            uni.showToast({ title: data.message || '回复失败', icon: 'none' })
          }
        } catch (e) {
          patchMsgPlaceholder(voiceMsg.id, '[语音消息]')
          uni.showToast({ title: '解析失败', icon: 'none' })
        }
      },
      fail: () => {
        thinking.value = false
        patchMsgPlaceholder(voiceMsg.id, '[语音消息]')
        uni.showToast({ title: '上传失败', icon: 'none' })
      }
    })
  }
}

onMounted(() => {
  initWelcome()
  innerAudio = uni.createInnerAudioContext()

  // 仅在 App 端初始化 uni 录音管理器（H5 端的 getRecorderManager 返回空壳对象，不可用）
  if (!PLATFORM_H5) {
    try {
      recorder = uni.getRecorderManager()
      recorder.onStop((res) => {
        console.log('[AI-Care] recorder.onStop', res)
        if (!res.tempFilePath) {
          thinking.value = false
          recording.value = false
          uni.showToast({ title: '录音失败', icon: 'none' })
          return
        }
        handleRecordResult(null, res.tempFilePath)
      })
      recorder.onStart(() => {
        console.log('[AI-Care] 录音已启动')
      })
      recorder.onError((err) => {
        console.error('[AI-Care] recorder.onError', err)
        recording.value = false
        thinking.value = false
        uni.showToast({ title: '录音出错：' + (err.errMsg || '未知错误'), icon: 'none' })
      })
      console.log('[AI-Care] 使用 App uni.getRecorderManager 模式')
    } catch (e) {
      console.warn('[AI-Care] uni.getRecorderManager 不可用', e)
      recorder = null
    }
  } else {
    console.log('[AI-Care] 使用 H5 MediaRecorder 模式')
  }
})

const ensureRecordAuth = () => {
  return new Promise((resolve) => {
    uni.getSetting({
      success: (s) => {
        const auth = s.authSetting && s.authSetting['scope.record']
        if (auth === true) {
          resolve(true)
          return
        }
        uni.authorize({
          scope: 'scope.record',
          success: () => resolve(true),
          fail: () => {
            uni.showModal({
              title: '需要录音权限',
              content: '开启录音权限后才能按住说话。是否去设置开启？',
              success: (m) => {
                if (m.confirm) {
                  uni.openSetting({
                    success: (os) => resolve(!!(os.authSetting && os.authSetting['scope.record'])),
                    fail: () => resolve(false)
                  })
                } else {
                  resolve(false)
                }
              }
            })
          }
        })
      },
      fail: () => resolve(false)
    })
  })
}

const addUserTextMsg = (text) => {
  messages.value.push({
    id: String(Date.now()) + '_ut',
    role: 'user',
    text,
    voiceUrl: ''
  })
  scrollToBottom() // 添加消息后滚动到底部
}

const addUserVoiceMsg = (filePath) => {
  const msg = {
    id: String(Date.now()) + '_uv',
    role: 'user',
    text: '',
    placeholder: '识别中…',
    voiceUrl: filePath
  }
  messages.value.push(msg)
  scrollToBottom()
  return msg
}

const patchMsgText = (msgId, text) => {
  const m = messages.value.find((x) => x.id === msgId)
  if (!m) return
  m.text = String(text || '').trim()
  m.placeholder = ''
  scrollToBottom()
}

const patchMsgPlaceholder = (msgId, placeholder) => {
  const m = messages.value.find((x) => x.id === msgId)
  if (!m) return
  m.placeholder = placeholder
  scrollToBottom()
}

const addAiMsg = ({ aiText, aiVoiceUrl }) => {
  messages.value.push({
    id: String(Date.now()) + '_ai',
    role: 'ai',
    text: aiText || '',
    voiceUrl: aiVoiceUrl || ''
  })
  scrollToBottom() // 添加消息后滚动
}

const sendText = () => {
  // 如果 AI 还在思考，或者正在录音，直接拦截返回
  if (thinking.value || recording.value) return

  const elderlyId = uni.getStorageSync('elderlyId')
  const text = (textDraft.value || '').trim()
  if (!text) return

  addUserTextMsg(text)
  textDraft.value = ''

  if (!elderlyId) {
    uni.showToast({ title: '未登录老人账号', icon: 'none' })
    return
  }

  thinking.value = true
  request({
    url: '/ai/care/chat/text',
    method: 'POST',
    data: { elderlyId: Number(elderlyId), text },
    success: (res) => {
      thinking.value = false
      if (res.statusCode === 200 && res.data && res.data.code === 200 && res.data.data) {
        addAiMsg({ aiText: res.data.data.aiText, aiVoiceUrl: res.data.data.aiVoiceUrl })
        if (res.data.data.aiVoiceUrl) {
          playAudio(res.data.data.aiVoiceUrl)
        }
      } else {
        uni.showToast({ title: res.data?.message || '发送失败', icon: 'none' })
      }
    },
    fail: () => {
      thinking.value = false
      uni.showToast({ title: '网络异常', icon: 'none' })
    }
  })
}

// App 端 Android 运行时权限请求
const requestAppRecordPermission = () => {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    try {
      plus.android.requestPermissions(
        ['android.permission.RECORD_AUDIO'],
        (e) => {
          console.log('[AI-Care] 权限请求结果', JSON.stringify(e))
          if (e.deniedAlways && e.deniedAlways.length > 0) {
            // 用户选择了"不再询问"
            resolve(false)
          } else if (e.deniedPresent && e.deniedPresent.length > 0) {
            // 用户拒绝了
            resolve(false)
          } else {
            // 已授权
            resolve(true)
          }
        },
        (e) => {
          console.error('[AI-Care] requestPermissions error', e)
          // 请求失败也尝试继续，让 recorder.start 自行处理
          resolve(true)
        }
      )
      return
    } catch (e) {
      console.warn('[AI-Care] plus.android.requestPermissions 不可用', e)
    }
    // #endif
    resolve(true)
  })
}

const startRecord = async () => {
  if (recording.value || thinking.value) return

  if (PLATFORM_H5) {
    // H5 浏览器录音：使用 MediaRecorder API
    try {
      h5Stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      h5AudioChunks = []
      h5MediaRecorder = new MediaRecorder(h5Stream)
      h5MediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) h5AudioChunks.push(e.data)
      }
      h5MediaRecorder.onstop = () => {
        // 释放麦克风
        if (h5Stream) {
          h5Stream.getTracks().forEach(t => t.stop())
          h5Stream = null
        }
        const blob = new Blob(h5AudioChunks, { type: 'audio/webm' })
        if (blob.size < 100) {
          thinking.value = false
          uni.showToast({ title: '录音太短，请重试', icon: 'none' })
          return
        }
        handleRecordResult(blob, null)
      }
      h5MediaRecorder.start()
      recording.value = true
    } catch (e) {
      console.error('H5 录音启动失败', e)
      uni.showToast({ title: '无法访问麦克风，请检查浏览器权限', icon: 'none' })
    }
  } else if (recorder) {
    // App 端录音：先请求运行时权限，再启动
    try {
      const permitted = await requestAppRecordPermission()
      if (!permitted) {
        uni.showToast({ title: '未获得录音权限，请在系统设置中开启', icon: 'none' })
        return
      }
      console.log('[AI-Care] App 端开始录音')
      recording.value = true
      recorder.start({ duration: 60000, format: 'mp3', sampleRate: 16000, numberOfChannels: 1 })
    } catch (e) {
      console.error('[AI-Care] App 录音启动失败', e)
      recording.value = false
      uni.showToast({ title: '录音启动失败', icon: 'none' })
    }
  } else {
    uni.showToast({ title: '当前环境不支持录音', icon: 'none' })
  }
}

const stopRecord = () => {
  if (!recording.value) return
  recording.value = false
  thinking.value = true

  if (PLATFORM_H5 && h5MediaRecorder && h5MediaRecorder.state === 'recording') {
    h5MediaRecorder.stop()
  } else if (recorder) {
    recorder.stop()
  } else {
    thinking.value = false
  }
}

const replayLast = () => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    uni.showToast({ title: '未登录老人账号', icon: 'none' })
    return
  }
  request({
    url: '/ai/care/chat/last',
    method: 'GET',
    data: { elderlyId: Number(elderlyId) },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200 && res.data.data) {
        const d = res.data.data
        if (d.aiVoiceUrl) {
          playAudio(d.aiVoiceUrl)
        } else {
          uni.showToast({ title: '暂无语音可重听', icon: 'none' })
        }
      } else {
        uni.showToast({ title: res.data?.message || '暂无可重听内容', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络异常', icon: 'none' })
    }
  })
}

const playAudio = (url) => {
  if (!url || !innerAudio) return
  innerAudio.stop() // 停止当前播放
  innerAudio.src = url
  innerAudio.play()
}

onUnmounted(() => {
  if (innerAudio) {
    innerAudio.stop()
    innerAudio.destroy() // 仅在页面卸载时销毁
    innerAudio = null
  }
})
</script>

<style scoped>
/* === 容器 === */
.ai-wrap {
  display: flex;
  flex-direction: column;
}

/* === 顶部欢迎栏 === */
.ai-header {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 16px 12px;
}
.ai-header-mark {
  width: 44px; height: 44px; border-radius: 12px; background: #ecfdf3;
  border: 1px solid #bbf7d0; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ai-header-mark-t { font-size: 18px; font-weight: 700; color: #166534; }
.ai-header-title { font-size: 20px; font-weight: 600; color: #1c1917; display: block; }
.ai-header-sub { font-size: 14px; color: #78716c; display: block; margin-top: 2px; }

/* === 聊天列表 === */
.chat-list {
  padding: 0 14px 180px;
}

.row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 8px;
}
.row-left { justify-content: flex-start; }
.row-right { justify-content: flex-end; }

/* === 头像 === */
.avatar {
  flex-shrink: 0;
  width: 38px; height: 38px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.avatar-letter { font-size: 15px; font-weight: 700; }
.avatar-ai { background: #ecfdf3; border: 1px solid #bbf7d0; color: #166534; }
.avatar-user { background: #fafaf9; border: 1px solid #e2ddd8; color: #5c4033; }

/* === 气泡 === */
.bubble {
  max-width: 70%;
  padding: 14px 16px;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.bubble-ai {
  background: #FFFFFF;
  color: #2D2D2D;
  border-top-left-radius: 6px;
}
.bubble-user {
  background: #6b4f3c;
  color: #fff;
  border-top-right-radius: 6px;
}
.bubble-text {
  font-size: 24px;
  line-height: 36px;
}

/* === 再听一遍 === */
.replay-row {
  display: flex;
  justify-content: center;
  margin: 4px 0 12px;
}
.replay-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 28px;
  border-radius: 999px;
  background: #ffffff;
  color: #6b4f3c;
  border: 1px solid #e2ddd8;
  font-size: 16px;
  font-weight: 600;
}

/* === 思考动画 === */
.thinking {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0 0 46px;
}
.dot-box { display: flex; gap: 4px; }
.dot { font-size: 12px; color: #78716c; opacity: 0.3; }
.d1 { animation: blink 1.4s 0s infinite; }
.d2 { animation: blink 1.4s 0.2s infinite; }
.d3 { animation: blink 1.4s 0.4s infinite; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}
.thinking-text { font-size: 14px; color: #a8a29e; }

/* === 底部操作区 === */
.ai-bottom {
  position: fixed;
  left: 0; right: 0;
  bottom: calc(68px + env(safe-area-inset-bottom));
  padding: 10px 14px 10px;
  background: #ffffff;
  border-top: 1px solid #e7e5e4;
  z-index: 40;
}

.input-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}
.text-input {
  flex: 1;
  min-width: 0;
  height: 46px;
  padding: 0 14px;
  border-radius: 12px;
  background: #fafaf9;
  border: 1px solid #e2ddd8;
  font-size: 16px;
  color: #1c1917;
}
.btn-send {
  flex: 0 0 72px;
  height: 46px;
  line-height: 46px;
  font-size: 16px;
  font-weight: 600;
  background: #6b4f3c;
  color: #fff;
  border-radius: 12px;
  border: 1px solid #5c4033;
}

.btn-talk {
  width: 100%;
  height: 54px;
  line-height: 54px;
  text-align: center;
  font-size: 17px;
  font-weight: 600;
  border-radius: 12px;
  background: #6b4f3c;
  color: #fff;
  border: 1px solid #5c4033;
  box-shadow: none;
  letter-spacing: 1px;
  user-select: none;
  -webkit-user-select: none;
}
.btn-talk-text {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}
.btn-talk-active {
  background: #b91c1c !important;
  border-color: #991b1b !important;
  box-shadow: none !important;
}
</style>
