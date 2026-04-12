<template>
  <view class="page">
    <view class="page-header">
      <text class="page-icon">💊</text>
      <view>
        <text class="page-title">今日用药看板</text>
        <text class="page-sub">{{ todayStr }}</text>
      </view>
    </view>

    <view v-if="dashboard.hasLowStock" class="alert-card">
      <text class="alert-text">⚠️ 有药品库存不足，请及时补货！</text>
    </view>

    <view class="med-list">
      <view class="med-card" v-for="item in dashboard.medicines" :key="item.medicineId">
        <view class="med-info">
          <text class="med-name">{{ item.name }}</text>
          <text class="med-dose">{{ item.dosageDesc }}</text>
        </view>
        <view class="med-status">
          <text v-if="item.taken" class="status-ok">✔ 已服</text>
          <text v-else class="status-wait">⏳ 待服</text>
          <text v-if="item.lowStock" class="status-alert">⚠ 缺药</text>
        </view>
      </view>
      <view v-if="!dashboard.medicines || dashboard.medicines.length === 0" class="empty">
        <text class="empty-icon">🎉</text>
        <text class="empty-text">今日暂无用药安排</text>
      </view>
    </view>

    <button class="btn-primary" @click="goAdd">+ 添加新药品</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { request } from '@/utils/request.js'

const dashboard = ref({ medicines: [], hasLowStock: false })
const todayStr = ref(new Date().toISOString().split('T')[0])

const loadData = () => {
  const familyId = uni.getStorageSync('familyId')
  // 假定已经绑定了老人并存下 elderlyId 方便查询
  const elderlyId = uni.getStorageSync('elderlyId') || 1

  request({
    url: '/medicine/family/dashboard',
    method: 'GET',
    data: { elderlyId, date: todayStr.value },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        dashboard.value = res.data.data
      }
    }
  })
}

onShow(() => {
  loadData()
})

const goAdd = () => {
  uni.navigateTo({ url: '/pages/family/add-medicine' })
}
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #EDF4FF 0%, #F8FAFF 30%); padding: 0 16px 30px; }
.page-header { display: flex; align-items: center; gap: 12px; padding: 20px 0 16px; }
.page-icon { font-size: 30px; }
.page-title { font-size: 22px; font-weight: 800; color: #2D2D2D; display: block; }
.page-sub { font-size: 13px; color: #7A9BBF; display: block; margin-top: 2px; }
.alert-card {
  background: #FFF1F0; padding: 14px 16px; border-radius: 14px; margin-bottom: 14px;
  border: 1px solid #FFD6D6;
}
.alert-text { color: #FF4D4F; font-size: 14px; font-weight: 700; }
.med-list { margin-bottom: 20px; display: flex; flex-direction: column; gap: 12px; }
.med-card {
  background: #FFFFFF; padding: 16px; border-radius: 16px;
  display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.med-info { display: flex; flex-direction: column; }
.med-name { font-size: 17px; font-weight: 700; color: #2D2D2D; display: block; margin-bottom: 4px; }
.med-dose { font-size: 13px; color: #7A9BBF; }
.med-status { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
.status-ok { color: #34C759; font-weight: 700; font-size: 14px; }
.status-wait { color: #A0B8CF; font-size: 14px; }
.status-alert {
  background: linear-gradient(135deg, #FF7B7B, #FF4D4F); color: #fff;
  padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;
}
.empty { text-align: center; padding: 40px 0; }
.empty-icon { font-size: 36px; display: block; margin-bottom: 8px; }
.empty-text { font-size: 14px; color: #A0B8CF; }
.btn-primary {
  width: 100%; height: 48px; line-height: 48px;
  background: linear-gradient(135deg, #6DB3F2 0%, #5B9BD5 100%); color: #fff;
  font-size: 16px; font-weight: 700; border-radius: 14px; border: none;
  box-shadow: 0 4px 14px rgba(91,155,213,0.25);
}
</style>
