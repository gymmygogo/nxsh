<template>
  <view class="page">
    <view class="content">
      <!-- 模块：血压记录（家属查看） -->
      <view v-if="activeTab === 'bp'">
        <view class="page-header">
          <text class="page-icon">❤️</text>
          <view>
            <text class="page-title">血压记录</text>
            <text class="page-sub">老人近期测量记录</text>
          </view>
        </view>

        <view class="card">
          <view class="card-head">
            <text class="card-head-title">趋势曲线</text>
            <text class="card-head-sub">{{ rangeText }}</text>
          </view>
          <view class="chart-box">
            <qiun-data-charts
              type="line"
              :opts="trendOpts"
              :chartData="trendReady ? trendData : emptyTrendData"
            />
          </view>
          <view v-if="debugBp" class="debug">
            <text>trendReady={{ trendReady }}</text>
            <text>categories={{ (trendReady ? trendData.categories : emptyTrendData.categories).length }}</text>
            <text>series0={{ (trendReady ? trendData.series?.[0]?.data : emptyTrendData.series?.[0]?.data).length }}</text>
          </view>
          <view v-if="!trendReady" class="empty">
            <text class="empty-icon">📊</text>
            <text class="empty-text">暂无趋势数据</text>
          </view>
        </view>

        <view class="card">
          <text class="card-head-title">⏰ 测量提醒设置</text>
          <view class="remind-list">
            <view class="remind-item" v-for="(cfg, idx) in bpRemindConfigs" :key="cfg.id || idx">
              <text class="remind-time">{{ cfg.remindTime }}</text>
              <switch :checked="cfg.isActive === 1" @change="e => toggleRemind(cfg, e.detail.value)" />
              <button class="btn-icon-del" @click="removeRemind(cfg, idx)">✕</button>
            </view>
          </view>
          <view class="remind-add">
            <picker mode="time" :value="newRemindTime" @change="e => newRemindTime = e.detail.value">
              <view class="picker-box">{{ newRemindTime || '选择时间' }}</view>
            </picker>
            <button class="btn-add-remind" @click="addRemind">+ 添加</button>
          </view>
        </view>

        <view class="card">
          <button class="btn-primary" @click="loadBloodData">刷新数据</button>
          <view v-if="bpRecords.length === 0" class="empty">
            <text class="empty-icon">📋</text>
            <text class="empty-text">暂无记录</text>
          </view>
          <view class="bp-card" v-for="item in bpRecords" :key="item.id">
            <view class="bp-left">
              <text class="bp-value">{{ item.sys }}/{{ item.dia }}</text>
              <text class="bp-time">{{ formatTime(item.recordTime) }}</text>
            </view>
            <view class="bp-badge" :class="statusClass(item.status)">
              {{ statusText(item.status) }}
            </view>
          </view>
        </view>
      </view>

      <!-- 模块：用药提醒（家属看板） -->
      <view v-if="activeTab === 'medicine'">
        <view class="page-header">
          <text class="page-icon">💊</text>
          <view>
            <text class="page-title">用药看板</text>
            <text class="page-sub">今日服药状态</text>
          </view>
        </view>

        <view class="card">
          <button class="btn-primary" @click="loadMedicineDashboard">刷新看板</button>
          <view v-if="dashboard.medicines.length === 0" class="empty">
            <text class="empty-icon">🎉</text>
            <text class="empty-text">暂无用药安排</text>
          </view>
          <view class="med-card" v-for="item in dashboard.medicines" :key="item.medicineId">
            <view class="med-info">
              <text class="med-name">{{ item.name }}</text>
              <text class="med-dose">{{ item.dosageDesc }}</text>
              <text class="med-stock">库存：{{ item.currentStock ?? '-' }} 粒</text>
            </view>
            <view class="med-status">
              <text v-if="item.taken" class="status-ok">✔ 已服</text>
              <text v-else class="status-wait">⏳ 待服</text>
              <text v-if="item.lowStock" class="status-low-stock">缺药补货</text>
            </view>
          </view>
          <button class="btn-secondary" @click="goAddMedicine">+ 添加药品</button>
        </view>
      </view>

      <!-- 模块：走位防丢失 -->
      <view v-if="activeTab === 'safe'">
        <view class="page-header">
          <text class="page-icon">📍</text>
          <view>
            <text class="page-title">安全守护</text>
            <text class="page-sub">定位与求救记录</text>
          </view>
        </view>

        <view class="card">
          <button class="btn-primary" @click="loadLastLocation">📡 查看最新位置</button>
          <view v-if="lastLocation" class="loc-box">
            <text class="loc-time">{{ formatTime(lastLocation.logTime) }}</text>
            <text class="loc-addr">{{ lastLocation.address || '地址未知' }}</text>
          </view>
        </view>
        <!-- 静默守护设置 -->
        <view class="card">
          <view class="card-head">
            <text class="card-head-title">🛡️ 静默守护设置</text>
            <text class="card-head-sub">老人长时间未操作时自动预警</text>
          </view>
          <view class="form-row">
            <text class="form-label">开启预警</text>
            <switch :checked="guardianEnabled" @change="e => guardianEnabled = e.detail.value" color="#5B9BD5" />
          </view>
          <view class="form-row">
            <text class="form-label">预警阈值</text>
            <picker @change="e => guardianThresholdIdx = e.detail.value" :range="guardianThresholds">
              <view class="picker-inline">{{ guardianThresholds[guardianThresholdIdx] }}</view>
            </picker>
          </view>
          <view class="field" style="margin-top: 10px;">
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
          <button class="btn-primary" @click="saveGuardianSettings">保存守护设置</button>
        </view>

        <view class="card">
          <view class="safe-actions">
            <button class="btn-action" @click="goSosLog">🚨 求救记录</button>
            <button class="btn-action" @click="goEmergencyContact">📞 紧急联系人</button>
          </view>
        </view>
      </view>

      <!-- 模块：我的 -->
      <view v-if="activeTab === 'me'">
        <view class="page-header">
          <text class="page-icon">👤</text>
          <view>
            <text class="page-title">我的</text>
            <text class="page-sub">账号 · 绑定 · 档案</text>
          </view>
        </view>

        <!-- 账号信息 -->
        <view class="card">
          <view class="info-row">
            <text class="info-label">家属账号</text>
            <text class="info-val">{{ familyPhone }}</text>
          </view>
        </view>

        <!-- 绑定老人 -->
        <view class="card">
          <view class="card-head">
            <text class="card-head-title">🔗 绑定老人</text>
          </view>
          <view v-if="bindInfo.bound" class="bind-status">
            <view class="info-row">
              <text class="info-label">老人手机号</text>
              <text class="info-val">{{ bindInfo.elderlyPhone || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">当前关系</text>
              <text class="info-val">{{ bindInfo.relationName || '—' }}</text>
            </view>
            <button class="btn-text" @click="showRebind = !showRebind">
              {{ showRebind ? '收起' : '重新绑定' }}
            </button>
          </view>
          <view v-if="!bindInfo.bound || showRebind" class="bind-form">
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
              <picker @change="onRelationPickChange" :range="relationOptions">
                <view class="picker-box">{{ relationOptions[relationPickIndex] }}</view>
              </picker>
            </view>
            <button class="btn-primary" @click="doBind">绑 定</button>
          </view>
        </view>

        <!-- 关系设置 -->
        <view v-if="bindInfo.bound" class="card">
          <view class="card-head">
            <text class="card-head-title">👥 关系设置</text>
            <text class="card-head-sub">此称谓将显示在老人端通知中</text>
          </view>
          <view class="field">
            <text class="field-label">我与老人的关系</text>
            <input class="field-input" v-model="editRelation" placeholder="如：女儿、儿子" />
          </view>
          <button class="btn-primary" @click="saveRelation">保存关系</button>
        </view>

        <!-- 老人档案 -->
        <view v-if="bindInfo.bound" class="card">
          <view class="card-head">
            <text class="card-head-title">📋 老人档案</text>
          </view>
          <view class="form-row">
            <text class="form-label">性别</text>
            <radio-group @change="e => profileGender = e.detail.value" class="radio-group">
              <label class="radio-item"><radio value="male" :checked="profileGender === 'male'" color="#5B9BD5" />男</label>
              <label class="radio-item"><radio value="female" :checked="profileGender === 'female'" color="#5B9BD5" />女</label>
            </radio-group>
          </view>
          <view class="form-row">
            <text class="form-label">年龄</text>
            <input class="form-input" type="number" v-model="profileAge" placeholder="—" />
          </view>
          <view class="form-row">
            <text class="form-label">身高(cm)</text>
            <input class="form-input" type="number" v-model="profileHeight" placeholder="—" />
          </view>
          <view class="form-row">
            <text class="form-label">体重(kg)</text>
            <input class="form-input" type="number" v-model="profileWeight" placeholder="—" />
          </view>
          <view class="field" style="margin-top: 10px;">
            <text class="field-label">健康标签</text>
            <view class="tags">
              <view
                v-for="(tag, idx) in healthTags"
                :key="idx"
                :class="['tag', tag.selected ? 'tag-selected' : '']"
                @click="tag.selected = !tag.selected"
              >{{ tag.name }}</view>
            </view>
          </view>
          <button class="btn-primary" @click="saveProfile">保存档案</button>
        </view>

        <!-- 退出登录 -->
        <view class="card">
          <button class="btn-logout" @click="handleLogout">退出登录</button>
        </view>
      </view>
    </view>

    <!-- 底部切换栏 -->
    <view class="tab-bar">
      <view class="tab-item" :class="tabClass('bp')" @click="setTab('bp')">
        <text class="tab-icon">❤️</text>
        <text class="tab-label">血压</text>
      </view>
      <view class="tab-item" :class="tabClass('medicine')" @click="setTab('medicine')">
        <text class="tab-icon">💊</text>
        <text class="tab-label">用药</text>
      </view>
      <view class="tab-item" :class="tabClass('safe')" @click="setTab('safe')">
        <text class="tab-icon">📍</text>
        <text class="tab-label">守护</text>
      </view>
      <view class="tab-item" :class="tabClass('me')" @click="setTab('me')">
        <text class="tab-icon">👤</text>
        <text class="tab-label">我的</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '@/utils/request.js'


const activeTab = ref('bp')

// ===== 「我的」页面：绑定 / 档案 / 关系 =====
const bindInfo = ref({ bound: false, elderlyPhone: '', relationName: '' })
const showRebind = ref(false)
const bindPhone = ref('')
const bindCode = ref('')
const relationOptions = ['父子', '母子', '父女', '母女', '子女', '其他亲属']
const relationPickIndex = ref(0)
const cooldown = ref(0)
let cdTimer = null

const editRelation = ref('')

const profileGender = ref('')
const profileAge = ref('')
const profileHeight = ref('')
const profileWeight = ref('')
const healthTags = ref([
  { name: '高血压', selected: false },
  { name: '糖尿病', selected: false },
  { name: '心脏病', selected: false },
  { name: '骨质疏松', selected: false }
])

const startCooldown = (sec = 60) => {
  cooldown.value = sec
  if (cdTimer) clearInterval(cdTimer)
  cdTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) { clearInterval(cdTimer); cdTimer = null }
  }, 1000)
}
onBeforeUnmount(() => { if (cdTimer) clearInterval(cdTimer) })

const onRelationPickChange = (e) => { relationPickIndex.value = e.detail.value }

const loadBindInfo = () => {
  if (!familyId.value) return
  request({
    url: '/family/bind/status',
    method: 'GET',
    data: { familyId: Number(familyId.value) },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        const d = res.data.data || {}
        bindInfo.value = {
          bound: !!d.bound,
          elderlyPhone: d.elderlyPhone || '',
          relationName: d.relationName || ''
        }
        if (d.elderlyId) {
          elderlyId.value = d.elderlyId
          uni.setStorageSync('elderlyId', d.elderlyId)
        }
        editRelation.value = d.relationName || ''
        if (d.bound) loadElderlyProfile()
      }
    }
  })
}

const loadElderlyProfile = () => {
  if (!elderlyId.value) return
  request({
    url: '/elderly/guardian/settings',
    method: 'GET',
    data: { elderlyId: Number(elderlyId.value) },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        const d = res.data.data
        if (!d) return
        profileGender.value = d.gender === 1 ? 'male' : d.gender === 2 ? 'female' : ''
        profileAge.value = d.age ? String(d.age) : ''
        profileHeight.value = d.height ? String(d.height) : ''
        profileWeight.value = d.weight ? String(d.weight) : ''
        if (d.chronicDiseases) {
          const tags = d.chronicDiseases.split(',')
          healthTags.value.forEach(t => { t.selected = tags.includes(t.name) })
        }
      }
    }
  })
}

const getBindCode = () => {
  if (!bindPhone.value) {
    uni.showToast({ title: '请输入老人手机号', icon: 'none' }); return
  }
  request({
    url: '/family/sendVerify',
    method: 'POST',
    data: { elderlyPhone: bindPhone.value },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        startCooldown(60)
      } else {
        uni.showToast({ title: res.data?.message || '发送失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}

const doBind = () => {
  if (!bindPhone.value || !bindCode.value) {
    uni.showToast({ title: '请填写手机号和验证码', icon: 'none' }); return
  }
  if (!familyId.value) {
    uni.showToast({ title: '请先登录', icon: 'none' }); return
  }
  request({
    url: '/family/bind',
    method: 'POST',
    data: {
      familyId: Number(familyId.value),
      elderlyPhone: bindPhone.value,
      verifyCode: bindCode.value,
      relationName: relationOptions[relationPickIndex.value],
      isPrimary: 1
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        uni.showToast({ title: '绑定成功', icon: 'success' })
        showRebind.value = false
        bindPhone.value = ''
        bindCode.value = ''
        loadBindInfo()
      } else {
        uni.showToast({ title: res.data?.message || '绑定失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}

const saveRelation = () => {
  if (!editRelation.value.trim()) {
    uni.showToast({ title: '请输入关系称谓', icon: 'none' }); return
  }
  request({
    url: '/family/bind/updateRelation',
    method: 'POST',
    data: {
      familyId: Number(familyId.value),
      elderlyId: Number(elderlyId.value),
      relationName: editRelation.value.trim()
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        uni.showToast({ title: '关系已更新', icon: 'success' })
        bindInfo.value.relationName = editRelation.value.trim()
      } else {
        uni.showToast({ title: res.data?.message || '更新失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}

const saveProfile = () => {
  if (!elderlyId.value) {
    uni.showToast({ title: '请先绑定老人', icon: 'none' }); return
  }
  const selectedTags = healthTags.value.filter(t => t.selected).map(t => t.name).join(',')
  request({
    url: '/elderly/profile/update',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId.value),
      gender: profileGender.value === 'male' ? 1 : profileGender.value === 'female' ? 2 : 0,
      age: profileAge.value ? Number(profileAge.value) : null,
      height: profileHeight.value ? Number(profileHeight.value) : null,
      weight: profileWeight.value ? Number(profileWeight.value) : null,
      chronicDiseases: selectedTags
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        uni.showToast({ title: '档案已保存', icon: 'success' })
      } else {
        uni.showToast({ title: res.data?.message || '保存失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}
// ===== END 我的 =====

// ===== 守护设置 =====
const guardianEnabled = ref(false)
const guardianThresholds = ['12小时无操作', '24小时无操作', '48小时无操作']
const guardianThresholdIdx = ref(1)
const dndStart = ref('22:00')
const dndEnd = ref('06:00')

const loadGuardianSettings = () => {
  if (!elderlyId.value) return
  request({
    url: '/elderly/guardian/settings',
    method: 'GET',
    data: { elderlyId: Number(elderlyId.value) },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        const d = res.data.data
        if (!d) return
        guardianEnabled.value = d.guardianActive === 1
        const revMap = { 12: 0, 24: 1, 48: 2 }
        guardianThresholdIdx.value = revMap[d.guardianThreshold] ?? 1
        if (d.dndStartTime) dndStart.value = d.dndStartTime.substring(0, 5)
        if (d.dndEndTime) dndEnd.value = d.dndEndTime.substring(0, 5)
      }
    }
  })
}

const saveGuardianSettings = () => {
  if (!elderlyId.value) {
    uni.showToast({ title: '请先绑定老人', icon: 'none' }); return
  }
  const thresholdMap = { 0: 12, 1: 24, 2: 48 }
  request({
    url: '/elderly/guardian/settings',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId.value),
      guardianActive: guardianEnabled.value ? 1 : 0,
      guardianThreshold: thresholdMap[guardianThresholdIdx.value] || 24,
      dndStartTime: dndStart.value + ':00',
      dndEndTime: dndEnd.value + ':00'
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data?.code === 200) {
        uni.showToast({ title: '守护设置已保存', icon: 'success' })
      } else {
        uni.showToast({ title: res.data?.message || '保存失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}
// ===== END 守护设置 =====

const bpRecords = ref([])
const dashboard = ref({ medicines: [], hasLowStock: false })
const lastLocation = ref(null)

const trendData = ref({ categories: [], series: [] })
const emptyTrendData = ref({
  categories: [''],
  series: [
    { name: '收缩压', data: [0] },
    { name: '舒张压', data: [0] }
  ]
})
const trendReady = ref(false)
const trendPoints = ref([]) // 存后端返回的趋势点
const debugBp = ref(false)

const rangeText = computed(() => {
  if (!bpRange.value) return '参考范围：加载中'
  const { sysLow, sysHigh, diaLow, diaHigh } = bpRange.value
  return `参考范围 SYS ${sysLow}-${sysHigh} / DIA ${diaLow}-${diaHigh}`
})

const trendOpts = ref({
  padding: [15, 15, 0, 5],
  color: ['#2979ff', '#ff9500'],
  enableScroll: false,
  legend: { show: true, position: 'top' },
  xAxis: { disableGrid: false },
  yAxis: { gridType: 'dash', splitNumber: 4, min: 40, max: 180 },
  extra: { line: { type: 'curve', width: 2 } }
})
const bpRange = ref(null)

const familyPhone = ref(uni.getStorageSync('familyPhone') || '')
const familyId = ref(uni.getStorageSync('familyId'))
const elderlyId = ref(uni.getStorageSync('elderlyId'))

const refreshIds = () => {
  familyPhone.value = uni.getStorageSync('familyPhone') || ''
  familyId.value = uni.getStorageSync('familyId')
  elderlyId.value = uni.getStorageSync('elderlyId')
}

const bpRemindConfigs = ref([])
const newRemindTime = ref('08:00')

const loadBpRemindConfigs = () => {
  if (!elderlyId.value) return
  request({
    url: '/api/health/remind/configs',
    method: 'GET',
    data: { elderlyId: Number(elderlyId.value) },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        bpRemindConfigs.value = (res.data.data || []).map(c => ({
          ...c,
          remindTime: c.remindTime ? c.remindTime.substring(0, 5) : ''
        }))
      }
    }
  })
}

const addRemind = () => {
  if (!elderlyId.value || !newRemindTime.value) return
  request({
    url: '/api/health/remind/config',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId.value),
      type: 1,
      remindTime: newRemindTime.value + ':00',
      isActive: 1
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '已添加', icon: 'success' })
        loadBpRemindConfigs()
      } else {
        uni.showToast({ title: res.data?.message || '添加失败', icon: 'none' })
      }
    }
  })
}

const toggleRemind = (cfg, active) => {
  request({
    url: '/api/health/remind/config',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId.value),
      remindTime: cfg.remindTime + ':00',
      active: active
    },
    success: () => loadBpRemindConfigs()
  })
}

const removeRemind = (cfg, idx) => {
  // 切换为关闭状态代替删除（服务端无删除接口，关闭即可）
  toggleRemind(cfg, false)
}

const tabClass = (key) => (activeTab.value === key ? 'tab-active' : '')

const setTab = (key) => {
  activeTab.value = key
  if (key === 'bp') loadBloodData()
  if (key === 'medicine') loadMedicineDashboard()
  if (key === 'safe') { loadLastLocation(); loadGuardianSettings() }
  if (key === 'me') loadBindInfo()
}

const loadBloodData = () => {
  loadBloodRecords()
  loadBloodTrend()
  loadBloodRange()
}

const loadBloodRecords = () => {
  if (!elderlyId.value) {
    bpRecords.value = []
    return
  }
  request({
    url: '/api/health/records',
    method: 'GET',
    data: { elderlyId: Number(elderlyId.value), page: 1, size: 20 },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        bpRecords.value = res.data.data || []
      }
    }
  })
}

const toYmd = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const formatDay = (day) => {
  if (!day) return ''
  const parts = String(day).split('-')
  if (parts.length !== 3) return String(day)
  return `${Number(parts[1])}-${Number(parts[2])}`
}

const updateTrendChart = (points) => {
  if (!points || points.length === 0) {
    trendReady.value = false
    trendData.value = { categories: [], series: [] }
    trendPoints.value = []
    return
  }

  // 过滤掉后端没算出均值的天（avgSys/avgDia 都为空会导致一条全 null 的线）
  const cleanPoints = points.filter((p) => p && (typeof p.avgSys === 'number' || typeof p.avgDia === 'number'))
  if (cleanPoints.length === 0) {
    trendReady.value = false
    trendData.value = { categories: [], series: [] }
    trendPoints.value = []
    return
  }

  trendPoints.value = cleanPoints

  const categories = cleanPoints.map((item) => formatDay(item.day))
  const sysData = cleanPoints.map((item) => (item.avgSys ?? null))
  const diaData = cleanPoints.map((item) => (item.avgDia ?? null))

  trendData.value = {
    categories,
    series: [
      { name: '收缩压', data: sysData },
      { name: '舒张压', data: diaData }
    ]
  }

  const nums = [...sysData, ...diaData].filter((v) => typeof v === 'number')
  if (bpRange.value) {
    nums.push(bpRange.value.sysLow, bpRange.value.sysHigh, bpRange.value.diaLow, bpRange.value.diaHigh)
  }
  if (nums.length > 0) {
    const min = Math.min(...nums)
    const max = Math.max(...nums)
    trendOpts.value = {
      ...trendOpts.value,
      yAxis: {
        ...trendOpts.value.yAxis,
        min: Math.max(40, min - 10),
        max: max + 10
      }
    }
  }

  trendReady.value = true
}

const loadBloodTrend = () => {
  if (!elderlyId.value) {
    trendReady.value = false
    trendData.value = { categories: [], series: [] }
    trendPoints.value = []
    return
  }

  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)

  request({
    url: '/api/health/records/trend',
    method: 'GET',
    data: { elderlyId: Number(elderlyId.value), from: toYmd(start), to: toYmd(end) },
    success: (res) => {
      const ok = res.statusCode === 200 && res.data && res.data.code === 200
      const points = ok ? (res.data.data || []) : []
      trendPoints.value = points
      updateTrendChart(points)
    },
    fail: () => {
      trendPoints.value = []
      updateTrendChart([])
    }
  })
}

const loadBloodRange = () => {
  request({
    url: '/api/health/records/range',
    method: 'GET',
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        bpRange.value = res.data.data
        if (trendPoints.value.length > 0) {
          updateTrendChart(trendPoints.value)
        }
      }
    }
  })
}

const loadMedicineDashboard = () => {
  if (!elderlyId.value) {
    dashboard.value = { medicines: [], hasLowStock: false }
    return
  }
  request({
    url: '/medicine/family/dashboard',
    method: 'GET',
    data: { elderlyId: Number(elderlyId.value) },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        dashboard.value = res.data.data
      }
    }
  })
}

const loadLastLocation = () => {
  if (!familyId.value || !elderlyId.value) {
    lastLocation.value = null
    return
  }
  request({
    url: '/family/location/last',
    method: 'GET',
    data: { familyId: Number(familyId.value), elderlyId: Number(elderlyId.value) },
    success: (res) => {
      if (res.statusCode === 200 && res.data.code === 200) {
        lastLocation.value = res.data.data
      }
    }
  })
}

const goAddMedicine = () => {
  uni.navigateTo({ url: '/pages/family/add-medicine' })
}

const goSosLog = () => {
  uni.navigateTo({ url: '/pages/family/sos-log' })
}

const goEmergencyContact = () => {
  uni.navigateTo({ url: '/pages/family/emergency-contact' })
}

const statusText = (status) => {
  if (status === 1) return '偏高'
  if (status === 2) return '偏低'
  return '正常'
}

const statusClass = (status) => {
  if (status === 1) return 'bp-high'
  if (status === 2) return 'bp-low'
  return 'bp-normal'
}

const formatTime = (isoTime) => {
  if (!isoTime) return ''
  const d = new Date(isoTime)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定退出吗？',
    confirmText: '退出',
    cancelText: '取消',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('familyId')
        uni.removeStorageSync('familyPhone')
        uni.removeStorageSync('elderlyId')
        uni.redirectTo({ url: '/pages/login/family-login' })
      }
    }
  })
}

onMounted(() => {
  uni.$on('healthDataRefresh', () => {
    loadBloodData()
  })
  uni.$on('medicineDashboardRefresh', () => {
    loadMedicineDashboard()
  })
  uni.$on('locationRefresh', () => {
    loadLastLocation()
  })
})

onBeforeUnmount(() => {
  uni.$off('healthDataRefresh')
  uni.$off('medicineDashboardRefresh')
  uni.$off('locationRefresh')
})

onShow(() => {
  refreshIds()
  loadBloodData()
  loadBpRemindConfigs()
})
</script>

<style scoped>
.page { min-height: 100vh; background: linear-gradient(180deg, #EDF4FF 0%, #FFFFFF 30%); }
.content { padding: 16px 16px 90px; }

/* Page Header */
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 8px 0; }
.page-icon { font-size: 30px; }
.page-title { font-size: 22px; font-weight: 800; color: #2D2D2D; display: block; }
.page-sub { font-size: 13px; color: #7A9BBF; display: block; margin-top: 2px; }

/* Cards */
.card {
  background: #FFFFFF; border-radius: 18px; padding: 18px;
  margin-bottom: 14px; box-shadow: 0 2px 16px rgba(0,0,0,0.05);
}
.card-head { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.card-head-title { font-size: 17px; font-weight: 700; color: #2D2D2D; display: block; }
.card-head-sub { font-size: 12px; color: #7A9BBF; }
.chart-box { width: 100%; height: 260px; }

/* Buttons */
.btn-primary {
  margin-top: 8px; height: 48px; line-height: 48px; font-size: 16px; font-weight: 700;
  background: linear-gradient(135deg, #6DB3F2 0%, #5B9BD5 100%); color: #fff;
  border-radius: 14px; border: none; box-shadow: 0 4px 14px rgba(91,155,213,0.25);
}
.btn-secondary {
  margin-top: 10px; height: 44px; line-height: 44px; font-size: 15px; font-weight: 600;
  border-radius: 12px; background: #EDF4FF; color: #5B9BD5; border: 1px solid #DEE9F5;
}
.btn-action {
  height: 48px; line-height: 48px; font-size: 15px; font-weight: 600;
  border-radius: 14px; background: #F5F9FF; color: #5B9BD5; border: 1px solid #DEE9F5;
}

/* Empty */
.empty { text-align: center; padding: 16px 0; }
.empty-icon { font-size: 32px; display: block; margin-bottom: 6px; }
.empty-text { font-size: 14px; color: #A0B8CF; }

/* BP Cards */
.bp-card {
  background: #F5F9FF; border: 1px solid #DEE9F5; border-radius: 14px;
  padding: 14px; margin-top: 10px; display: flex; justify-content: space-between; align-items: center;
}
.bp-left { display: flex; flex-direction: column; }
.bp-value { font-size: 22px; font-weight: 800; color: #2D2D2D; }
.bp-time { font-size: 13px; color: #7A9BBF; margin-top: 2px; }
.bp-badge {
  font-size: 13px; font-weight: 700; padding: 4px 12px; border-radius: 20px;
}
.bp-normal { background: #E8F8E8; color: #34C759; }
.bp-high { background: #FFF1F0; color: #FF4D4F; }
.bp-low { background: #FFFBE6; color: #D4A017; }

/* Medicine Cards */
.med-card {
  background: #F5F9FF; border: 1px solid #DEE9F5; border-radius: 14px;
  padding: 14px; margin-top: 10px; display: flex; justify-content: space-between; align-items: center;
}
.med-info { display: flex; flex-direction: column; }
.med-name { font-size: 17px; font-weight: 700; color: #2D2D2D; }
.med-dose { font-size: 13px; color: #7A9BBF; margin-top: 2px; }
.med-stock { font-size: 12px; color: #A0B8CF; margin-top: 2px; }
.med-status { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.status-ok { color: #34C759; font-weight: 700; font-size: 14px; }
.status-wait { color: #A0B8CF; font-size: 14px; }
.status-low-stock {
  background: #FF4D4F; color: #fff;
  font-size: 12px; padding: 3px 10px; border-radius: 10px; font-weight: 700;
  letter-spacing: 1px;
}

/* Location */
.loc-box {
  background: #F5F9FF; border: 1px solid #DEE9F5; border-radius: 14px;
  padding: 14px; margin-top: 10px;
}
.loc-time { font-size: 13px; color: #7A9BBF; display: block; }
.loc-addr { font-size: 16px; color: #2D2D2D; font-weight: 600; margin-top: 4px; display: block; }
.safe-actions { display: flex; flex-direction: column; gap: 10px; }

/* Info Row */
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 0; border-bottom: 1px solid #DEE9F5; margin-bottom: 16px;
}
.info-label { font-size: 15px; color: #7A9BBF; }
.info-val { font-size: 17px; font-weight: 700; color: #2D2D2D; }
.btn-logout {
  height: 44px; line-height: 44px; font-size: 15px; font-weight: 600;
  background: #FFF1F0; color: #FF4D4F; border-radius: 12px; border: 1px solid #FFD6D6;
}

/* Remind */
.remind-list { display: flex; flex-direction: column; gap: 8px; margin: 10px 0; }
.remind-item {
  display: flex; align-items: center; gap: 10px;
  background: #F5F9FF; padding: 10px 14px; border-radius: 12px; border: 1px solid #DEE9F5;
}
.remind-time { font-size: 18px; font-weight: 700; color: #2D2D2D; flex: 1; }
.btn-icon-del {
  width: 30px; height: 30px; line-height: 30px; padding: 0;
  font-size: 13px; background: #FFF1F0; color: #FF4D4F; border-radius: 8px; border: none;
}
.remind-add { display: flex; gap: 10px; align-items: center; margin-top: 10px; }
.remind-add .picker-box {
  border: 1px solid #DEE9F5; height: 40px; line-height: 40px; border-radius: 10px;
  padding: 0 14px; font-size: 15px; background: #F5F9FF; min-width: 100px; text-align: center; color: #2D2D2D;
}
.btn-add-remind {
  background: linear-gradient(135deg, #6DB3F2, #5B9BD5); color: #fff;
  font-size: 14px; font-weight: 600; height: 40px; line-height: 40px; border-radius: 10px; padding: 0 16px; border: none;
}

/* Tab Bar */
.tab-bar {
  position: fixed; left: 0; right: 0; bottom: 0; height: 68px;
  background: #FFFFFF; border-top: 1px solid #DEE9F5;
  display: flex; align-items: center; justify-content: space-around;
  padding-bottom: env(safe-area-inset-bottom); z-index: 50;
}
.tab-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 4px 0; min-width: 50px;
}
.tab-icon { font-size: 22px; }
.tab-label { font-size: 11px; color: #A0A0A0; }
.tab-active .tab-label { color: #5B9BD5; font-weight: 700; }

.debug { margin-top: 10px; color: #999; font-size: 12px; display: flex; flex-direction: column; gap: 4px; }

/* Bind / Profile / Relation — 我的 Tab */
.bind-status { margin-bottom: 6px; }
.bind-status .info-row { margin-bottom: 0; }
.bind-form { margin-top: 6px; }
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
  padding: 0 14px; font-size: 15px; background: #F5F9FF; color: #2D2D2D;
}
.btn-text {
  margin-top: 6px; height: 36px; line-height: 36px; font-size: 13px; font-weight: 600;
  background: transparent; color: #5B9BD5; border: none; padding: 0; text-align: left;
}
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
.tags { display: flex; flex-wrap: wrap; gap: 10px; }
.tag {
  padding: 6px 14px; border: 1.5px solid #DEE9F5; border-radius: 20px;
  font-size: 13px; color: #6B8CAA; background: #F5F9FF;
}
.tag-selected { background: linear-gradient(135deg, #6DB3F2, #5B9BD5); color: #fff; border-color: #5B9BD5; }

/* Guardian Settings */
.picker-inline {
  height: 36px; line-height: 36px; padding: 0 12px; border: 1px solid #DEE9F5;
  border-radius: 10px; background: #F5F9FF; font-size: 14px; color: #2D2D2D;
}
.time-picker { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
.time-box {
  height: 40px; line-height: 40px; padding: 0 16px; border: 1px solid #DEE9F5;
  border-radius: 10px; background: #F5F9FF; font-size: 16px; font-weight: 600; color: #2D2D2D;
}
.time-sep { font-size: 14px; color: #7A9BBF; }
</style>
