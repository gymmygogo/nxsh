<template>
  <view class="page">
    <view class="page-header">
      <view class="page-mark"><text class="page-mark-t">救</text></view>
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
        <view class="empty-line" />
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
.page { min-height: 100vh; background: #f0f1f3; padding: 0 16px 30px; }
.page-header { display: flex; align-items: center; gap: 12px; padding: 20px 0 16px; }
.page-mark {
  width: 44px; height: 44px; border-radius: 12px; background: #fff; border: 1px solid #e6e8ec;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.page-mark-t { font-size: 17px; font-weight: 700; color: #1e4a72; }
.page-title { font-size: 20px; font-weight: 600; color: #1a1d21; display: block; }
.page-sub { font-size: 12px; color: #6b7280; display: block; margin-top: 2px; }
.log-list { display: flex; flex-direction: column; gap: 10px; }
.log-card {
  background: #ffffff; border-radius: 12px; padding: 16px;
  display: flex; align-items: center; gap: 14px;
  border: 1px solid #e6e8ec;
}
.log-badge {
  width: 44px; height: 44px; border-radius: 12px;
  background: #b91c1c;
  color: #fff; font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  letter-spacing: 0.5px;
}
.log-body { flex: 1; }
.log-time { font-size: 14px; font-weight: 600; color: #991b1b; display: block; margin-bottom: 4px; }
.log-address { font-size: 14px; color: #4b5563; display: block; }
.empty { text-align: center; padding: 40px 0; }
.empty-line {
  width: 28px; height: 2px; background: #d1d5db; border-radius: 1px;
  margin: 0 auto 10px;
}
.empty-text { font-size: 14px; color: #9ca3af; }
</style>
