<template>
  <view class="page">
    <view class="page-header">
      <view class="page-mark"><text class="page-mark-t">药</text></view>
      <view>
        <text class="page-title">今日用药看板</text>
        <text class="page-sub">{{ todayStr }}</text>
      </view>
    </view>

    <view v-if="dashboard.hasLowStock" class="alert-card">
      <text class="alert-text">有药品库存不足，请及时补货</text>
    </view>

    <view class="med-list">
      <view class="med-card" v-for="item in dashboard.medicines" :key="item.medicineId">
        <view class="med-info">
          <text class="med-name">{{ item.name }}</text>
          <text class="med-dose">{{ item.dosageDesc }}</text>
        </view>
        <view class="med-status">
          <text v-if="item.taken" class="status-ok">已服</text>
          <text v-else class="status-wait">待服</text>
          <text v-if="item.lowStock" class="status-alert">缺药</text>
        </view>
      </view>
      <view v-if="!dashboard.medicines || dashboard.medicines.length === 0" class="empty">
        <view class="empty-line" />
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
.page { min-height: 100vh; background: #f0f1f3; padding: 0 16px 30px; }
.page-header { display: flex; align-items: center; gap: 12px; padding: 20px 0 16px; }
.page-mark {
  width: 44px; height: 44px; border-radius: 12px; background: #fff; border: 1px solid #e6e8ec;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.page-mark-t { font-size: 17px; font-weight: 700; color: #1e4a72; }
.page-title { font-size: 20px; font-weight: 600; color: #1a1d21; display: block; }
.page-sub { font-size: 13px; color: #6b7280; display: block; margin-top: 2px; }
.alert-card {
  background: #fef2f2; padding: 14px 16px; border-radius: 12px; margin-bottom: 12px;
  border: 1px solid #fecaca;
}
.alert-text { color: #991b1b; font-size: 14px; font-weight: 600; }
.med-list { margin-bottom: 20px; display: flex; flex-direction: column; gap: 10px; }
.med-card {
  background: #ffffff; padding: 16px; border-radius: 12px;
  display: flex; justify-content: space-between; align-items: center;
  border: 1px solid #e6e8ec;
}
.med-info { display: flex; flex-direction: column; }
.med-name { font-size: 17px; font-weight: 600; color: #1a1d21; display: block; margin-bottom: 4px; }
.med-dose { font-size: 13px; color: #6b7280; }
.med-status { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
.status-ok { color: #166534; font-weight: 600; font-size: 14px; }
.status-wait { color: #9ca3af; font-size: 14px; }
.status-alert {
  background: #b91c1c; color: #fff;
  padding: 2px 8px; border-radius: 8px; font-size: 11px; font-weight: 600;
}
.empty { text-align: center; padding: 40px 0; }
.empty-line {
  width: 28px; height: 2px; background: #d1d5db; border-radius: 1px;
  margin: 0 auto 10px;
}
.empty-text { font-size: 14px; color: #9ca3af; }
.btn-primary {
  width: 100%; height: 48px; line-height: 48px;
  background: #1e4a72; color: #fff;
  font-size: 16px; font-weight: 600; border-radius: 10px; border: 1px solid #1e4a72;
  box-shadow: none;
}
</style>
