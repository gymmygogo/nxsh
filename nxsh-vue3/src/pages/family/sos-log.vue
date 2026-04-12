<template>
  <view class="page">
    <view class="page-header">
      <text class="page-icon">🚨</text>
      <view>
        <text class="page-title">求救记录</text>
        <text class="page-sub">历史紧急求救信息</text>
      </view>
    </view>
    <view class="log-list">
      <view class="log-card" v-for="(log, index) in logs" :key="index">
        <view class="log-badge">SOS</view>
        <view class="log-body">
          <text class="log-time">{{ formatTime(log.logTime) }}</text>
          <text class="log-address">{{ log.address || '地址未知' }}</text>
        </view>
      </view>
      <view v-if="logs.length === 0" class="empty">
        <text class="empty-icon">✅</text>
        <text class="empty-text">暂无求救记录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { request } from '@/utils/request.js'
import { onShow } from '@dcloudio/uni-app'

const logs = ref([])

const formatTime = (isoTime) => {
  if (!isoTime) return ''
  const d = new Date(isoTime)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

onShow(() => {
  const familyId = uni.getStorageSync('familyId')
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!familyId || !elderlyId) {
    uni.showToast({ title: '请先登录并绑定老人', icon: 'none' })
    return
  }

  request({
    url: '/family/location/sos/list',
    method: 'GET',
    data: { familyId, elderlyId },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        logs.value = res.data.data
      }
    }
  })
})
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #EDF4FF 0%, #F8FAFF 30%); padding: 0 16px 30px; }
.page-header { display: flex; align-items: center; gap: 12px; padding: 20px 0 16px; }
.page-icon { font-size: 30px; }
.page-title { font-size: 22px; font-weight: 800; color: #2D2D2D; display: block; }
.page-sub { font-size: 12px; color: #7A9BBF; display: block; margin-top: 2px; }
.log-list { display: flex; flex-direction: column; gap: 12px; }
.log-card {
  background: #FFFFFF; border-radius: 16px; padding: 16px;
  display: flex; align-items: center; gap: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.log-badge {
  width: 44px; height: 44px; border-radius: 12px;
  background: linear-gradient(135deg, #FF7B7B, #FF4D4F);
  color: #fff; font-size: 12px; font-weight: 800;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.log-body { flex: 1; }
.log-time { font-size: 14px; font-weight: 700; color: #FF4D4F; display: block; margin-bottom: 4px; }
.log-address { font-size: 14px; color: #4A6A8A; display: block; }
.empty { text-align: center; padding: 40px 0; }
.empty-icon { font-size: 36px; display: block; margin-bottom: 8px; }
.empty-text { font-size: 14px; color: #A0B8CF; }
</style>
