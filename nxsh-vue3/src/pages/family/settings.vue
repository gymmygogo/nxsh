<template>
  <view class="page">
    <view class="page-header">
      <text class="page-icon">⚙️</text>
      <view>
        <text class="page-title">家属设置</text>
        <text class="page-sub">绑定老人 · 档案管理 · 守护配置</text>
      </view>
    </view>

    <!-- 绑定老人 -->
    <view class="card">
      <view class="card-title">
        <text class="card-title-icon">🔗</text>
        <text class="card-title-text">绑定老人</text>
      </view>
      <view class="field">
        <text class="field-label">老人手机号</text>
        <input class="field-input" type="number" v-model="bindPhone" placeholder="请输入老人手机号" />
      </view>
      <view class="field">
        <text class="field-label">验证码</text>
        <view class="code-row">
          <input class="field-input code-input" type="number" v-model="bindCode" placeholder="请输入验证码" />
          <button class="btn-code" :disabled="cooldown > 0" @click="getBindCode">
            {{ cooldown > 0 ? cooldown + 's' : '获取验证码' }}
          </button>
        </view>
      </view>
      <view class="field">
        <text class="field-label">与老人关系</text>
        <picker @change="onRelationChange" :range="relations">
          <view class="picker-box">{{ relations[relationIndex] }}</view>
        </picker>
      </view>
      <button class="btn-primary" @click="bindElderly">绑 定</button>
    </view>

    <!-- 老人档案 -->
    <view class="card">
      <view class="card-title">
        <text class="card-title-icon">📋</text>
        <text class="card-title-text">老人档案</text>
      </view>
      <view class="form-row">
        <text class="form-label">性别</text>
        <radio-group @change="genderChange" class="radio-group">
          <label class="radio-item"><radio value="male" color="#5B9BD5" />男</label>
          <label class="radio-item"><radio value="female" color="#5B9BD5" />女</label>
        </radio-group>
      </view>
      <view class="form-row">
        <text class="form-label">年龄</text>
        <input class="form-input" type="number" v-model="age" placeholder="—" />
      </view>
      <view class="form-row">
        <text class="form-label">身高(cm)</text>
        <input class="form-input" type="number" v-model="height" placeholder="—" />
      </view>
      <view class="form-row">
        <text class="form-label">体重(kg)</text>
        <input class="form-input" type="number" v-model="weight" placeholder="—" />
      </view>
      <view class="field">
        <text class="field-label">健康标签</text>
        <view class="tags">
          <view
            v-for="(tag, index) in healthTags"
            :key="index"
            :class="['tag', tag.selected ? 'tag-selected' : '']"
            @click="toggleTag(index)"
          >
            {{ tag.name }}
          </view>
        </view>
      </view>
      <button class="btn-primary" @click="saveProfile">保存档案</button>
    </view>

    <!-- 静默守护 -->
    <view class="card">
      <view class="card-title">
        <text class="card-title-icon">🛡️</text>
        <text class="card-title-text">静默守护设置</text>
      </view>
      <view class="form-row">
        <text class="form-label">开启预警</text>
        <switch :checked="alertEnabled" @change="e => alertEnabled = e.detail.value" color="#5B9BD5" />
      </view>
      <view class="form-row">
        <text class="form-label">预警阈值</text>
        <picker @change="onThresholdChange" :range="thresholds">
          <view class="picker-inline">{{ thresholds[thresholdIndex] }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="field-label">免打扰时段</text>
        <view class="time-picker">
          <picker mode="time" :value="dndStart" @change="e => dndStart = e.detail.value">
            <view class="time-box">{{ dndStart }}</view>
          </picker>
          <text class="time-sep">至</text>
          <picker mode="time" :value="dndEnd" @change="e => dndEnd = e.detail.value">
            <view class="time-box">{{ dndEnd }}</view>
          </picker>
        </view>
      </view>
      <button class="btn-primary" @click="saveSettings">保存设置</button>
    </view>
  </view>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { request } from '@/utils/request.js'

const bindPhone = ref('')
const bindCode = ref('')
const relations = ['父子', '母子', '子女', '其他亲属']
const relationIndex = ref(0)

const gender = ref('')
const age = ref('')
const height = ref('')
const weight = ref('')
const healthTags = ref([
  { name: '高血压', selected: false },
  { name: '糖尿病', selected: false },
  { name: '心脏病', selected: false },
  { name: '骨质疏松', selected: false }
])

const alertEnabled = ref(true)
const thresholds = ['12小时无操作', '24小时无操作', '48小时无操作']
const thresholdIndex = ref(1)

const dndStart = ref('22:00')
const dndEnd = ref('06:00')

const cooldown = ref(0)
let timer = null

const startCooldown = (seconds = 60) => {
  cooldown.value = seconds
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

const getBindCode = () => {
  if (!bindPhone.value) {
    uni.showToast({ title: '请输入老人手机号', icon: 'none' })
    return
  }

  request({
    url: '/family/sendVerify',
    method: 'POST',
    data: { elderlyPhone: bindPhone.value },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        startCooldown(60)
      } else {
        uni.showToast({ title: res.data?.message || '发送失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络请求失败', icon: 'none' })
    }
  })
}

const onRelationChange = (e) => {
  relationIndex.value = e.detail.value
}

const bindElderly = () => {
  if (!bindPhone.value || !bindCode.value) {
    uni.showToast({ title: '参数不完整', icon: 'none' })
    return
  }

  const familyId = uni.getStorageSync('familyId')
  if (!familyId) {
    uni.showToast({ title: '请先登录并完成家属ID存储', icon: 'none' })
    return
  }

  request({
    url: '/family/bind',
    method: 'POST',
    data: {
      familyId: Number(familyId),
      elderlyPhone: bindPhone.value,
      verifyCode: bindCode.value,
      relationName: relations[relationIndex.value],
      isPrimary: 1
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '绑定成功', icon: 'success' })

        request({
          url: '/family/bind/status',
          method: 'GET',
          data: { familyId: Number(familyId) },
          success: (bindRes) => {
            if (bindRes.statusCode === 200 && bindRes.data.code === 200) {
              const elderlyId = bindRes.data.data?.elderlyId
              if (elderlyId) {
                uni.setStorageSync('elderlyId', elderlyId)
                uni.redirectTo({ url: '/pages/family/main' })
              }
            }
          }
        })
      } else {
        uni.showToast({ title: res.data?.message || '绑定失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '网络请求失败', icon: 'none' })
    }
  })
}

const genderChange = (e) => {
  gender.value = e.detail.value
}
const toggleTag = (index) => {
  healthTags.value[index].selected = !healthTags.value[index].selected
}
const saveProfile = () => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    uni.showToast({ title: '请先绑定老人', icon: 'none' })
    return
  }
  const selectedTags = healthTags.value.filter(t => t.selected).map(t => t.name).join(',')
  request({
    url: '/elderly/profile/update',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId),
      gender: gender.value === 'male' ? 1 : gender.value === 'female' ? 2 : 0,
      age: age.value ? Number(age.value) : null,
      height: height.value ? Number(height.value) : null,
      weight: weight.value ? Number(weight.value) : null,
      chronicDiseases: selectedTags
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '档案已保存', icon: 'success' })
      } else {
        uni.showToast({ title: res.data?.message || '保存失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}
const onThresholdChange = (e) => {
  thresholdIndex.value = e.detail.value
}
const saveSettings = () => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    uni.showToast({ title: '请先绑定老人', icon: 'none' })
    return
  }
  const thresholdMap = { 0: 12, 1: 24, 2: 48 }
  request({
    url: '/elderly/guardian/settings',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId),
      guardianActive: alertEnabled.value ? 1 : 0,
      guardianThreshold: thresholdMap[thresholdIndex.value] || 24,
      dndStartTime: dndStart.value + ':00',
      dndEndTime: dndEnd.value + ':00'
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '设置已保存', icon: 'success' })
      } else {
        uni.showToast({ title: res.data?.message || '保存失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}

const loadGuardianSettings = () => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) return
  request({
    url: '/elderly/guardian/settings',
    method: 'GET',
    data: { elderlyId: Number(elderlyId) },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        const data = res.data.data
        if (data) {
          alertEnabled.value = data.guardianActive === 1
          const thresholdReverseMap = { 12: 0, 24: 1, 48: 2 }
          thresholdIndex.value = thresholdReverseMap[data.guardianThreshold] ?? 1
          if (data.dndStartTime) dndStart.value = data.dndStartTime.substring(0, 5)
          if (data.dndEndTime) dndEnd.value = data.dndEndTime.substring(0, 5)
          if (data.gender === 1) gender.value = 'male'
          else if (data.gender === 2) gender.value = 'female'
          if (data.age) age.value = String(data.age)
          if (data.height) height.value = String(data.height)
          if (data.weight) weight.value = String(data.weight)
          if (data.chronicDiseases) {
            const tags = data.chronicDiseases.split(',')
            healthTags.value.forEach(t => {
              t.selected = tags.includes(t.name)
            })
          }
        }
      }
    }
  })
}

loadGuardianSettings()
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #EDF4FF 0%, #F8FAFF 30%); padding: 0 16px 30px; }

/* Page Header */
.page-header { display: flex; align-items: center; gap: 12px; padding: 20px 0 16px; }
.page-icon { font-size: 30px; }
.page-title { font-size: 22px; font-weight: 800; color: #2D2D2D; display: block; }
.page-sub { font-size: 12px; color: #7A9BBF; display: block; margin-top: 2px; }

/* Cards */
.card {
  background: #FFFFFF; border-radius: 18px; padding: 20px;
  margin-bottom: 14px; box-shadow: 0 2px 16px rgba(0,0,0,0.05);
}
.card-title { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.card-title-icon { font-size: 20px; }
.card-title-text { font-size: 17px; font-weight: 700; color: #2D2D2D; }

/* Fields */
.field { margin-bottom: 14px; }
.field-label { font-size: 13px; font-weight: 600; color: #6B8CAA; display: block; margin-bottom: 6px; }
.field-input {
  height: 46px; font-size: 15px; padding: 0 14px; color: #2D2D2D; caret-color: #5B9BD5;
  background: #F5F9FF; border: 1px solid #DEE9F5; border-radius: 12px; -webkit-text-fill-color: #2D2D2D;
}
.code-row { display: flex; align-items: center; gap: 10px; }
.code-input { flex: 1; }
.btn-code {
  flex-shrink: 0; width: 110px; height: 46px; line-height: 46px; font-size: 13px; font-weight: 600;
  background: #EDF4FF; color: #5B9BD5; border-radius: 12px; border: 1px solid #DEE9F5; text-align: center; padding: 0;
}
.btn-code[disabled] { color: #A0B8CF; background: #F5F9FF; }
.picker-box {
  height: 46px; line-height: 46px; border: 1px solid #DEE9F5; border-radius: 12px;
  padding: 0 14px; font-size: 15px; background: #F5F9FF; color: #2D2D2D; margin-bottom: 14px;
}

/* Form Rows */
.form-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0; border-bottom: 1px solid #F0F5FC;
}
.form-label { font-size: 15px; color: #4A6A8A; font-weight: 600; }
.form-input {
  width: 100px; height: 36px; font-size: 15px; text-align: right;
  padding: 0 10px; border: 1px solid #DEE9F5; border-radius: 10px;
  background: #F5F9FF; color: #2D2D2D; caret-color: #5B9BD5;
}
.radio-group { display: flex; gap: 16px; }
.radio-item { font-size: 15px; color: #2D2D2D; display: flex; align-items: center; gap: 4px; }
.picker-inline {
  height: 36px; line-height: 36px; padding: 0 12px; border: 1px solid #DEE9F5;
  border-radius: 10px; background: #F5F9FF; font-size: 14px; color: #2D2D2D;
}

/* Tags */
.tags { display: flex; flex-wrap: wrap; gap: 10px; }
.tag {
  padding: 6px 14px; border: 1.5px solid #DEE9F5; border-radius: 20px;
  font-size: 13px; color: #6B8CAA; background: #F5F9FF;
}
.tag-selected { background: linear-gradient(135deg, #6DB3F2, #5B9BD5); color: #fff; border-color: #5B9BD5; }

/* Time Picker */
.time-picker { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
.time-box {
  height: 40px; line-height: 40px; padding: 0 16px; border: 1px solid #DEE9F5;
  border-radius: 10px; background: #F5F9FF; font-size: 16px; font-weight: 600; color: #2D2D2D;
}
.time-sep { font-size: 14px; color: #7A9BBF; }

/* Primary Button */
.btn-primary {
  width: 100%; height: 48px; line-height: 48px; margin-top: 8px;
  background: linear-gradient(135deg, #6DB3F2 0%, #5B9BD5 100%); color: #fff;
  font-size: 16px; font-weight: 700; border-radius: 14px; border: none;
  box-shadow: 0 4px 14px rgba(91,155,213,0.25); letter-spacing: 2px;
}
</style>
