<template>
  <view class="page">
    <view class="page-header">
      <text class="page-icon">💊</text>
      <view>
        <text class="page-title">添加药品</text>
        <text class="page-sub">为老人添加用药记录</text>
      </view>
    </view>

    <!-- 常用药快选 -->
    <view class="card">
      <view class="card-head">
        <text class="card-head-title">📋 常用药快选</text>
        <text class="card-head-sub">点击药品自动填入</text>
      </view>
      <view v-for="(group, gIdx) in commonMedicines" :key="gIdx" class="med-group">
        <text class="med-group-label">{{ group.label }}</text>
        <view class="med-group-items">
          <view
            v-for="(med, mIdx) in group.items"
            :key="mIdx"
            :class="['med-chip', form.name === med.name ? 'med-chip-active' : '']"
            @click="selectCommon(med)"
          >{{ med.name }}</view>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="field">
        <text class="field-label">药品名称</text>
        <input class="field-input" type="text" v-model="form.name" placeholder="请输入药品名称" />
      </view>
      <view class="field">
        <text class="field-label">剂量说明</text>
        <input class="field-input" type="text" v-model="form.dosageDesc" placeholder="如：2片" />
      </view>
      <view class="field">
        <text class="field-label">当前库存数量</text>
        <input class="field-input" type="number" v-model="form.currentStock" placeholder="输入当前库存药量" />
      </view>
      <view class="field">
        <text class="field-label">预警阈值</text>
        <input class="field-input" type="number" v-model="form.lowStockThreshold" placeholder="低于该值触发报警" />
      </view>
      <view class="field">
        <text class="field-label">用药时间</text>
        <picker mode="time" :value="baseTime" @change="e => baseTime = e.detail.value">
          <view class="picker-box">{{ baseTime || '请选择时间' }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="field-label">亲情语音录制</text>
        <button class="btn-voice" @click="uploadVoice">🎙️ 点击录音并上传</button>
      </view>

      <button class="btn-primary" @click="saveMedicine">保存药品</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { request, uploadFile } from '@/utils/request.js'

const form = ref({
  name: '',
  dosageDesc: '',
  currentStock: null,
  lowStockThreshold: null,
  voiceRemindUrl: '',
  takeTimes: [],
  startDate: new Date().toISOString().split('T')[0],
  endDate: '2099-12-31',
  frequencyType: 'daily'
})

const baseTime = ref('08:00')

const commonMedicines = [
  {
    label: '降压药',
    items: [
      { name: '硝苯地平缓释片', dosage: '1片/次' },
      { name: '氨氯地平片', dosage: '1片/次' },
      { name: '缬沙坦胶囊', dosage: '1粒/次' },
      { name: '厄贝沙坦片', dosage: '1片/次' },
      { name: '美托洛尔片', dosage: '1片/次' },
      { name: '卡托普利片', dosage: '1片/次' },
      { name: '依那普利片', dosage: '1片/次' },
      { name: '氢氯噻嗪片', dosage: '1片/次' }
    ]
  },
  {
    label: '心脑血管',
    items: [
      { name: '阿司匹林肠溶片', dosage: '1片/次' },
      { name: '氯吡格雷片', dosage: '1片/次' },
      { name: '阿托伐他汀钙片', dosage: '1片/次' },
      { name: '辛伐他汀片', dosage: '1片/次' },
      { name: '速效救心丸', dosage: '4粒/次' }
    ]
  },
  {
    label: '降糖药',
    items: [
      { name: '二甲双胍片', dosage: '1片/次' },
      { name: '格列美脲片', dosage: '1片/次' },
      { name: '阿卡波糖片', dosage: '1片/次' }
    ]
  },
  {
    label: '其他常用',
    items: [
      { name: '钙尔奇D片', dosage: '1片/次' },
      { name: '维生素D滴剂', dosage: '1粒/次' },
      { name: '氨基葡萄糖胶囊', dosage: '2粒/次' },
      { name: '奥美拉唑肠溶胶囊', dosage: '1粒/次' },
      { name: '蒙脱石散', dosage: '1袋/次' }
    ]
  }
]

const selectCommon = (med) => {
  form.value.name = med.name
  form.value.dosageDesc = med.dosage
}
const voiceRecording = ref(false)
let recorder = null

// #ifndef H5
recorder = uni.getRecorderManager()
recorder.onStop((res) => {
  voiceRecording.value = false
  if (!res.tempFilePath) {
    uni.showToast({ title: '录音失败', icon: 'none' })
    return
  }
  uni.showLoading({ title: '上传中...', mask: true })
  uploadFile({
    url: '/medicine/family/upload',
    filePath: res.tempFilePath,
    name: 'file',
    success: (uploadRes) => {
      uni.hideLoading()
      try {
        const data = JSON.parse(uploadRes.data)
        if (data.code === 200 && data.data) {
          form.value.voiceRemindUrl = data.data
          uni.showToast({ title: '录音上传成功', icon: 'success' })
        } else {
          uni.showToast({ title: data.message || '上传失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '解析失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({ title: '上传失败', icon: 'none' })
    }
  })
})
// #endif

const uploadVoice = () => {
  if (!recorder) {
    uni.showToast({ title: '当前环境不支持录音', icon: 'none' })
    return
  }
  if (voiceRecording.value) {
    recorder.stop()
  } else {
    voiceRecording.value = true
    recorder.start({ duration: 30000, format: 'mp3' })
    uni.showToast({ title: '正在录音，再次点击停止', icon: 'none' })
  }
}

const saveMedicine = () => {
  if (!form.value.name || !form.value.dosageDesc) {
    uni.showToast({ title: '药名与剂量必填', icon: 'none' })
    return
  }

  const familyId = uni.getStorageSync('familyId')
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    uni.showToast({ title: '未绑定老人，请先选择/绑定老人', icon: 'none' })
    return
  }

  const currentStock = form.value.currentStock === null || form.value.currentStock === '' ? null : Number(form.value.currentStock)
  const lowStockThreshold = form.value.lowStockThreshold === null || form.value.lowStockThreshold === '' ? null : Number(form.value.lowStockThreshold)

  const payload = {
    ...form.value,
    familyId: familyId ? Number(familyId) : null,
    elderlyId: Number(elderlyId),
    takeTimes: [baseTime.value], // 后端 List<LocalTime>，传 "08:00"
    currentStock,
    lowStockThreshold
  }

  request({
    url: '/medicine/family/save',
    method: 'POST',
    data: payload,
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        uni.showToast({ title: '添加成功', icon: 'success' })
        setTimeout(() => { uni.navigateBack() }, 1200)
      } else {
        uni.showToast({ title: res.data?.message || `保存失败(${res.statusCode})`, icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #EDF4FF 0%, #F8FAFF 30%); padding: 0 16px 30px; }
.page-header { display: flex; align-items: center; gap: 12px; padding: 20px 0 16px; }
.page-icon { font-size: 30px; }
.page-title { font-size: 22px; font-weight: 800; color: #2D2D2D; display: block; }
.page-sub { font-size: 12px; color: #7A9BBF; display: block; margin-top: 2px; }
.card {
  background: #FFFFFF; border-radius: 18px; padding: 20px;
  box-shadow: 0 2px 16px rgba(0,0,0,0.05);
}
.field { margin-bottom: 16px; }
.field-label { font-size: 13px; font-weight: 600; color: #6B8CAA; display: block; margin-bottom: 6px; }
.field-input {
  height: 46px; font-size: 15px; padding: 0 14px; color: #2D2D2D; caret-color: #5B9BD5;
  background: #F5F9FF; border: 1px solid #DEE9F5; border-radius: 12px;
}
.picker-box {
  height: 46px; line-height: 46px; border: 1px solid #DEE9F5; border-radius: 12px;
  padding: 0 14px; font-size: 15px; background: #F5F9FF; color: #2D2D2D;
}
.btn-voice {
  height: 44px; line-height: 44px; font-size: 14px; font-weight: 600;
  background: #EDF4FF; color: #5B9BD5; border-radius: 12px; border: 1px solid #DEE9F5;
}
/* Common Medicine */
.card-head { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; }
.card-head-title { font-size: 17px; font-weight: 700; color: #2D2D2D; display: block; }
.card-head-sub { font-size: 12px; color: #7A9BBF; }
.med-group { margin-bottom: 12px; }
.med-group:last-child { margin-bottom: 0; }
.med-group-label { font-size: 12px; font-weight: 600; color: #7A9BBF; display: block; margin-bottom: 6px; }
.med-group-items { display: flex; flex-wrap: wrap; gap: 8px; }
.med-chip {
  padding: 6px 14px; border: 1.5px solid #DEE9F5; border-radius: 20px;
  font-size: 13px; color: #4A6A8A; background: #F5F9FF;
}
.med-chip-active {
  background: linear-gradient(135deg, #6DB3F2, #5B9BD5); color: #fff; border-color: #5B9BD5;
}
.btn-primary {
  width: 100%; height: 48px; line-height: 48px; margin-top: 8px;
  background: linear-gradient(135deg, #6DB3F2 0%, #5B9BD5 100%); color: #fff;
  font-size: 16px; font-weight: 700; border-radius: 14px; border: none;
  box-shadow: 0 4px 14px rgba(91,155,213,0.25); letter-spacing: 2px;
}
</style>
