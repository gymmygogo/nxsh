<template>
  <view class="page">
    <view class="content" :class="{ 'content-tight-bp': activeTab === 'bp' }">
      <!-- 模块：血压记录（家属查看） -->
      <view v-if="activeTab === 'bp'" class="bp-tab">
        <view class="bp-header-compact">
          <view class="bp-h-mark"><text class="bp-h-mark-t">血</text></view>
          <view class="bp-h-text">
            <text class="bp-h-title">血压记录</text>
            <text class="bp-h-range">{{ rangeText }}</text>
          </view>
        </view>

        <view class="bp-strip">
          <view class="bp-seg">
            <view
              v-for="seg in bpSegTabs"
              :key="seg.key"
              :class="['bp-seg-cell', bpSection === seg.key ? 'bp-seg-cell-on' : '']"
              @click="setBpSection(seg.key)"
            >
              <text class="bp-seg-txt">{{ seg.label }}</text>
            </view>
          </view>
          <button class="bp-refresh-btn" @click="loadBloodData">刷新</button>
        </view>

        <view v-show="bpSection === 'records'" class="card card-bp-tight">
          <view v-if="bpRecords.length === 0" class="empty empty-bp-tight">
            <view class="empty-line" />
            <text class="empty-text">暂无记录</text>
          </view>
          <view class="bp-card bp-card-tight" v-for="item in displayedBpRecords" :key="item.id">
            <view class="bp-left">
              <text class="bp-value bp-value-tight">{{ item.sys }}/{{ item.dia }}</text>
              <text class="bp-time bp-time-tight">{{ formatTime(item.recordTime) }}</text>
            </view>
            <view class="bp-badge bp-badge-tight" :class="statusClass(statusForItem(item))">
              {{ statusText(statusForItem(item)) }}
            </view>
          </view>
        </view>

        <!-- v-if：图表在 display:none 下初始化会宽高为 0，切换后空白；每次进入趋势递增 key 强制重绘 -->
        <view v-if="bpSection === 'trend'" class="card card-bp-tight">
          <view class="card-head card-head-inline">
            <text class="card-head-title">趋势</text>
            <text class="card-head-sub card-head-sub-ellipsis">{{ rangeText }}</text>
          </view>
          <view class="chart-box chart-box-family">
            <qiun-data-charts
              :key="trendChartKey"
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
            <view class="empty-line" />
            <text class="empty-text">暂无趋势数据</text>
          </view>
        </view>

        <view v-show="bpSection === 'remind'" class="card card-bp-tight">
          <text class="card-head-title card-head-title-one">测量提醒</text>
          <view class="remind-list">
            <view
              class="remind-item"
              v-for="(cfg, idx) in bpRemindConfigs"
              :key="cfg.id || idx"
            >
              <text class="remind-time">{{ cfg.remindTime }}</text>
              <button class="btn-del-remind" @click="removeRemind(cfg)">删除</button>
            </view>
          </view>
          <view class="remind-add">
            <picker mode="time" :value="newRemindTime" @change="e => newRemindTime = e.detail.value">
              <view class="picker-box">{{ newRemindTime || '选择时间' }}</view>
            </picker>
            <button class="btn-add-remind" @click="addRemind">+ 添加提醒</button>
          </view>
        </view>
      </view>

      <!-- 模块：用药提醒（家属看板） -->
      <view v-if="activeTab === 'medicine'">
        <view class="page-header">
          <view class="page-mark page-mark-med"><text class="page-mark-t">药</text></view>
          <view>
            <text class="page-title">用药看板</text>
            <text class="page-sub">今日服药状态</text>
          </view>
        </view>

        <view class="card">
          <button class="btn-primary" @click="loadMedicineDashboard">刷新看板</button>
          <view v-if="dashboard.medicines.length === 0" class="empty">
            <view class="empty-line" />
            <text class="empty-text">暂无用药安排</text>
          </view>
          <view class="med-card" v-for="item in dashboard.medicines" :key="item.medicineId">
            <view class="med-info">
              <text class="med-name">{{ item.name }}</text>
              <text class="med-dose">{{ item.dosageDesc }}</text>
              <text class="med-stock">库存：{{ item.currentStock ?? '-' }} 粒</text>
            </view>
            <view class="med-status">
              <text v-if="item.taken" class="status-ok">已服</text>
              <text v-else class="status-wait">待服</text>
              <text v-if="item.lowStock" class="status-low-stock">缺药补货</text>
            </view>
          </view>
          <button class="btn-secondary" @click="goAddMedicine">+ 添加药品</button>
        </view>
      </view>

      <!-- 模块：走位防丢失 -->
      <view v-if="activeTab === 'safe'" class="safe-tab">
        <view class="page-header">
          <view class="page-mark page-mark-safe"><text class="page-mark-t">守</text></view>
          <view>
            <text class="page-title">安全守护</text>
            <text class="page-sub">定位与求救记录</text>
          </view>
        </view>

        <view class="card">
          <button class="btn-primary" @click="loadLastLocation">查看最新位置</button>
          <view v-if="lastLocation" class="loc-box">
            <text class="loc-time">{{ formatTime(lastLocation.logTime) }}</text>
            <text class="loc-addr">{{ lastLocation.address || '地址未知' }}</text>
          </view>
        </view>
        <!-- 静默守护设置 -->
        <view class="card">
          <view class="card-head">
            <text class="card-head-title">静默守护</text>
            <text class="card-head-sub">老人长时间未操作时自动预警</text>
          </view>
          <view class="form-row">
            <text class="form-label">开启预警</text>
            <switch :checked="guardianEnabled" @change="e => guardianEnabled = e.detail.value" color="#1e4a72" />
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
            <button class="btn-action" @click="goSosLog">求救记录</button>
            <button class="btn-action" @click="goEmergencyContact">紧急联系人</button>
          </view>
        </view>
      </view>

      <!-- 模块：我的 -->
      <view v-if="activeTab === 'me'" class="me-tab">
        <view class="page-header">
          <view class="page-mark page-mark-me"><text class="page-mark-t">我</text></view>
          <view>
            <text class="page-title">我的</text>
            <text class="page-sub">账号 · 绑定 · 档案</text>
          </view>
        </view>

        <!-- 账号信息 -->
        <view class="card">
          <view class="card-head">
            <text class="card-head-title">账号信息</text>
          </view>
          <view class="info-row">
            <text class="info-label">家属账号</text>
            <text class="info-val">{{ familyPhone }}</text>
          </view>
        </view>

        <!-- 绑定老人 -->
        <view class="card">
          <view class="card-head">
            <text class="card-head-title">绑定老人</text>
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
            <text class="card-head-title">关系设置</text>
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
            <text class="card-head-title">老人档案</text>
          </view>
          <view class="form-row">
            <text class="form-label">性别</text>
            <radio-group @change="e => profileGender = e.detail.value" class="radio-group">
              <label class="radio-item"><radio value="male" :checked="profileGender === 'male'" color="#1e4a72" />男</label>
              <label class="radio-item"><radio value="female" :checked="profileGender === 'female'" color="#1e4a72" />女</label>
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
        <view class="tab-glyph-wrap"><text class="tab-glyph">血</text></view>
        <text class="tab-label">血压</text>
      </view>
      <view class="tab-item" :class="tabClass('medicine')" @click="setTab('medicine')">
        <view class="tab-glyph-wrap"><text class="tab-glyph">药</text></view>
        <text class="tab-label">用药</text>
      </view>
      <view class="tab-item" :class="tabClass('safe')" @click="setTab('safe')">
        <view class="tab-glyph-wrap"><text class="tab-glyph">守</text></view>
        <text class="tab-label">守护</text>
      </view>
      <view class="tab-item" :class="tabClass('me')" @click="setTab('me')">
        <view class="tab-glyph-wrap"><text class="tab-glyph">我</text></view>
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
        if (d.bound) {
          loadElderlyProfile()
          loadBloodData()
          // loadBpRemindConfigs() // 血压提醒由后端 WebSocket 推送，不需要轮询查询
        }
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
/** 家属血压 Tab：首屏尽量少滚动，默认只预览最近几条 */
const BP_PREVIEW_LIMIT = 6
const showAllBpRecords = ref(false)
/** 血压子页：记录 / 趋势 / 提醒 三选一切换 */
const bpSection = ref('records')
const bpSegTabs = [
  { key: 'records', label: '记录' },
  { key: 'trend', label: '趋势' },
  { key: 'remind', label: '提醒' }
]

const trendChartKey = ref(0)

const setBpSection = (key) => {
  if (key === 'trend') {
    trendChartKey.value += 1
  }
  bpSection.value = key
}

const displayedBpRecords = computed(() => {
  if (showAllBpRecords.value) return bpRecords.value
  return bpRecords.value.slice(0, BP_PREVIEW_LIMIT)
})

const dashboard = ref({ medicines: [], hasLowStock: false })
const lastLocation = ref(null)

const trendData = ref({ categories: [], series: [] })
const emptyTrendData = ref({
  categories: [],
  series: [
    { name: '收缩压', data: [] },
    { name: '舒张压', data: [] }
  ]
})
const trendReady = ref(false)
// 后端返回的趋势点原始列表（与 loadBloodRange 联动重算纵轴）
const trendPoints = ref([])
const debugBp = ref(false)

const rangeText = computed(() => {
  if (!bpRange.value) return '参考范围：加载中'
  const { sysLow, sysHigh, diaLow, diaHigh } = bpRange.value
  return `参考范围 SYS ${sysLow}-${sysHigh} / DIA ${diaLow}-${diaHigh}`
})

const trendOpts = ref({
  padding: [15, 15, 0, 5],
  color: ['#1e4a72', '#6b5344'],
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

/** 后端 LocalTime 使用 HH:mm:ss */
const remindTimeToApi = (t) => {
  if (t == null || t === '') return ''
  const s = String(t).trim()
  const m = s.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/)
  if (!m) return ''
  const hh = String(m[1]).padStart(2, '0')
  const mm = String(m[2]).padStart(2, '0')
  const ss = m[3] != null ? String(m[3]).padStart(2, '0') : '00'
  return `${hh}:${mm}:${ss}`
}

const loadBpRemindConfigs = () => {
  if (!elderlyId.value) return Promise.resolve([])
  return new Promise((resolve) => {
    request({
      url: '/api/health/remind/configs',
      method: 'GET',
      data: { elderlyId: Number(elderlyId.value) },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const list = (res.data.data || [])
            .map(c => {
              let rt = c.remindTime
              if (rt && typeof rt !== 'string') rt = String(rt)
              return {
                ...c,
                remindTime: rt && typeof rt === 'string' ? rt.substring(0, 5) : ''
              }
            })
            .filter(c => c.isActive === 1 || c.active === true)
          bpRemindConfigs.value = list
          resolve(list)
          return
        }
        resolve([])
      },
      fail: () => resolve([])
    })
  })
}

const addRemind = () => {
  if (!elderlyId.value || !newRemindTime.value) return
  const remindTime = remindTimeToApi(newRemindTime.value)
  if (!remindTime) {
    uni.showToast({ title: '请选择有效时间', icon: 'none' })
    return
  }
  request({
    url: '/api/health/remind/config',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId.value),
      remindTime,
      active: true
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '已添加', icon: 'success' })
        loadBpRemindConfigs()
      } else {
        uni.showToast({ title: res.data?.message || '添加失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}

const removeRemind = (cfg) => {
  const remindTime = remindTimeToApi(cfg.remindTime)
  if (!remindTime || !elderlyId.value) return
  request({
    url: '/api/health/remind/config',
    method: 'POST',
    data: {
      elderlyId: Number(elderlyId.value),
      remindTime,
      active: false
    },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        loadBpRemindConfigs().then((list) => {
          const stillActive = list.some(item => item.remindTime === cfg.remindTime)
          if (stillActive) {
            uni.showToast({ title: '删除未生效，请稍后重试', icon: 'none' })
          } else {
            uni.showToast({ title: '已删除', icon: 'success' })
          }
        })
      } else {
        uni.showToast({ title: res.data?.message || '删除失败', icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '网络请求失败', icon: 'none' })
  })
}

const tabClass = (key) => (activeTab.value === key ? 'tab-active' : '')

const setTab = (key) => {
  if (key !== 'bp') {
    bpSection.value = 'records'
    showAllBpRecords.value = false
  }
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

const updateTrendChart = (rawPoints) => {
  // 1. 终极解包：兼容后端返回了分页对象(Page)或者直接返回列表的情况
  let points = rawPoints
  if (rawPoints && Array.isArray(rawPoints.records)) {
    points = rawPoints.records // 掏出 MyBatis Plus 分页对象里的数据
  } else if (rawPoints && Array.isArray(rawPoints.list)) {
    points = rawPoints.list
  } else if (rawPoints && Array.isArray(rawPoints.rows)) {
    points = rawPoints.rows
  }

  // 2. 检查掏出来的数据到底是不是数组
  if (!points || !Array.isArray(points) || points.length === 0) {
    trendReady.value = false
    trendData.value = { categories: [], series: [] }
    return
  }

  const pickSys = (p) => {
    const v = p?.avgSys ?? p?.sys ?? p?.systolic ?? p?.sbp
    return v != null ? Number(v) : null
  }
  const pickDia = (p) => {
    const v = p?.avgDia ?? p?.dia ?? p?.diastolic ?? p?.dbp
    return v != null ? Number(v) : null
  }

  // 过滤出有效的血压记录（兼容多种字段名）
  const cleanPoints = points.filter((p) => {
    const sys = pickSys(p)
    const dia = pickDia(p)
    return Number.isFinite(sys) || Number.isFinite(dia)
  })
  if (cleanPoints.length === 0) {
    trendReady.value = false
    trendData.value = { categories: [], series: [] }
    return
  }

  trendPoints.value = cleanPoints

  // 3. 提取日期
  const categories = cleanPoints.map((item) => {
    // 👉 关键修改：把 item.day 放在最前面
    let t = item.day || item.recordTime || item.measureTime || item.createdAt || item.createTime || ''

    let dateStr = ''
    if (Array.isArray(t)) {
      dateStr = `${t[0]}-${String(t[1]).padStart(2, '0')}-${String(t[2]).padStart(2, '0')}`
    } else {
      let s = String(t)
      if (s.includes('T')) s = s.split('T')[0]
      if (s.includes(' ')) s = s.split(' ')[0]
      dateStr = s
    }
    const parts = dateStr.split('-')
    return parts.length >= 3 ? `${Number(parts[1])}-${Number(parts[2])}` : dateStr
  })

  // 4. 提取数值
  const sysData = cleanPoints.map((item) => {
    const n = pickSys(item)
    return Number.isFinite(n) ? n : null
  })
  const diaData = cleanPoints.map((item) => {
    const n = pickDia(item)
    return Number.isFinite(n) ? n : null
  })

  setTimeout(() => {
    trendData.value = JSON.parse(JSON.stringify({
      categories,
      series: [
        { name: '收缩压', data: sysData },
        { name: '舒张压', data: diaData }
      ]
    }))

    const nums = [...sysData, ...diaData].filter((v) => typeof v === 'number' && !isNaN(v))
    if (bpRange.value) {
      nums.push(Number(bpRange.value.sysLow), Number(bpRange.value.sysHigh), Number(bpRange.value.diaLow), Number(bpRange.value.diaHigh))
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
  }, 50)
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
      // 增加终极打印，直接把底层数据拍在控制台上
      console.log('=== 接口完整返回 ===', res.data)

      let points = []
      // 暴力提取数组：无视格式，掘地三尺把数组找出来
      if (Array.isArray(res.data)) {
        points = res.data
      } else if (res.data && Array.isArray(res.data.data)) {
        points = res.data.data
      } else if (res.data && Array.isArray(res.data.records)) {
        points = res.data.records
      } else if (res.data && res.data.data && Array.isArray(res.data.data.records)) {
        points = res.data.data.records
      }

      trendPoints.value = points
      updateTrendChart(points)
    },
    fail: (err) => {
      console.error('=== 请求失败 ===', err)
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

const calcStatusFromValue = (sys, dia) => {
  const s = Number(sys)
  const d = Number(dia)
  if (!Number.isFinite(s) || !Number.isFinite(d)) return null
  if (s >= 140 || d >= 90) return 1
  if (s < 90 || d < 60) return 2
  return 0
}

const statusForItem = (item) => {
  const derived = calcStatusFromValue(item?.sys, item?.dia)
  return derived != null ? derived : item?.status ?? 0
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
  loadBindInfo()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f0f1f3; }
.content { padding: 16px 16px 90px; }
/* 血压 Tab：整体上移出刘海区（下移观感），底部紧贴 Tab 高度，减少栏上空白 */
.content-tight-bp {
  padding-top: max(36px, calc(env(safe-area-inset-top, 0px) + 14px));
  padding-left: 12px;
  padding-right: 12px;
  padding-bottom: calc(68px + env(safe-area-inset-bottom, 0px));
}

/* Page Header */
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 8px 0; }
.page-mark {
  width: 44px; height: 44px; border-radius: 12px; background: #ffffff;
  border: 1px solid #e6e8ec; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.page-mark-t { font-size: 17px; font-weight: 700; color: #1a1d21; }
.page-mark-med .page-mark-t { color: #1e4a72; }
.page-mark-safe .page-mark-t { color: #1e4a72; }
.page-mark-me .page-mark-t { color: #1a1d21; }
.page-title { font-size: 20px; font-weight: 600; color: #1a1d21; display: block; letter-spacing: 0.3px; }
.page-sub { font-size: 13px; color: #6b7280; display: block; margin-top: 2px; }

/* 家属 · 血压 Tab：压缩首屏，减少滑动 */
.bp-tab { margin-top: 0; }
.bp-header-compact {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px; padding: 0 2px;
}
.bp-h-mark {
  width: 36px; height: 36px; border-radius: 10px; background: #ffffff;
  border: 1px solid #e6e8ec; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.bp-h-mark-t { font-size: 15px; font-weight: 700; color: #1e4a72; }
.bp-h-text { flex: 1; min-width: 0; }
.bp-h-title { font-size: 16px; font-weight: 600; color: #1a1d21; display: block; line-height: 1.15; }
.bp-h-range {
  font-size: 10px; color: #6b7280; display: block; margin-top: 2px;
  line-height: 1.25; word-break: break-all;
}

/* 记录 / 趋势 / 提醒 分段切换 + 刷新 */
.bp-strip {
  display: flex; flex-direction: row; align-items: stretch;
  gap: 8px; margin-bottom: 10px;
}
.bp-seg {
  flex: 1; min-width: 0; display: flex; flex-direction: row;
  background: #e8eaee; border-radius: 10px; padding: 3px;
  border: 1px solid #e0e3e8;
}
.bp-seg-cell {
  flex: 1; display: flex; align-items: center; justify-content: center;
  border-radius: 10px; padding: 8px 4px;
}
.bp-seg-cell-on {
  background: #fff;
  box-shadow: none;
}
.bp-seg-txt {
  font-size: 14px; font-weight: 600; color: #6b7280;
}
.bp-seg-cell-on .bp-seg-txt { color: #1e4a72; }
.bp-refresh-btn {
  flex-shrink: 0; width: 56px; height: auto; min-height: 46px;
  padding: 0 4px; font-size: 13px; font-weight: 600; line-height: 1.2;
  border-radius: 10px; border: 1px solid #1e4a72;
  background: #1e4a72;
  color: #fff;
  box-shadow: none;
}

.card-bp-tight {
  padding: 8px 10px; margin-bottom: 8px; border-radius: 12px;
}
.empty-bp-tight { padding: 6px 0; }
.empty-bp-tight .empty-line { margin-bottom: 6px; }
.empty-bp-tight .empty-text { font-size: 12px; }
.bp-card-tight {
  padding: 5px 8px; margin-top: 4px; border-radius: 8px;
}
.bp-value-tight { font-size: 14px; font-weight: 700; }
.bp-time-tight { font-size: 10px; margin-top: 0; color: #9ca3af; }
.bp-badge-tight { font-size: 10px; padding: 1px 6px; border-radius: 10px; }
/* 覆盖全局 .bp-card 的 margin-top，避免记录行间距过大 */
.bp-tab .bp-card.bp-card-tight { margin-top: 4px; }
.bp-tab .card-bp-tight > .bp-card-tight:first-child { margin-top: 0; }
.btn-bp-more {
  margin-top: 5px; height: 32px; line-height: 32px; font-size: 12px;
}
.card-head-inline {
  display: flex; flex-direction: row; align-items: center; justify-content: space-between;
  gap: 8px; margin-bottom: 6px;
}
.card-head-inline .card-head-title { margin: 0; flex-shrink: 0; font-size: 15px; }
.card-head-sub-ellipsis {
  flex: 1; min-width: 0; text-align: right; font-size: 10px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.card-head-title-one { display: block; margin-bottom: 6px; font-size: 15px; }
.bp-tab .remind-list { margin: 4px 0 6px; gap: 6px; }
.bp-tab .remind-item { padding: 6px 10px; }
.bp-tab .remind-add { margin-top: 6px; }
.bp-tab .remind-add .picker-box { height: 36px; line-height: 36px; font-size: 14px; }
.bp-tab .btn-add-remind { height: 36px; line-height: 36px; font-size: 13px; }

/* Cards */
.card {
  background: #ffffff; border-radius: 14px; padding: 18px;
  margin-bottom: 12px; border: 1px solid #e6e8ec; box-shadow: none;
}
.card-head { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.card-head-title { font-size: 16px; font-weight: 600; color: #1a1d21; display: block; }
.card-head-sub { font-size: 12px; color: #6b7280; }
.chart-box { width: 100%; height: 260px; }
.chart-box-family { height: 180px; }

/* Buttons */
.btn-primary {
  margin-top: 8px; height: 48px; line-height: 48px; font-size: 15px; font-weight: 600;
  background: #1e4a72; color: #fff;
  border-radius: 10px; border: 1px solid #1e4a72; box-shadow: none;
}
.btn-secondary {
  margin-top: 10px; height: 44px; line-height: 44px; font-size: 15px; font-weight: 600;
  border-radius: 10px; background: #f4f6f9; color: #1e4a72; border: 1px solid #d8dee6;
}
.btn-action {
  height: 48px; line-height: 48px; font-size: 15px; font-weight: 600;
  border-radius: 10px; background: #ffffff; color: #1e4a72; border: 1px solid #d8dee6;
}

/* Empty */
.empty { text-align: center; padding: 16px 0; }
.empty-line {
  width: 28px; height: 2px; background: #d1d5db; border-radius: 1px;
  margin: 0 auto 10px;
}
.empty-text { font-size: 14px; color: #9ca3af; }

/* BP Cards */
.bp-card {
  background: #f9fafb; border: 1px solid #e6e8ec; border-radius: 12px;
  padding: 14px; margin-top: 10px; display: flex; justify-content: space-between; align-items: center;
}
.bp-left { display: flex; flex-direction: column; }
.bp-value { font-size: 22px; font-weight: 700; color: #1a1d21; }
.bp-time { font-size: 13px; color: #6b7280; margin-top: 2px; }
.bp-badge {
  font-size: 13px; font-weight: 700; padding: 4px 12px; border-radius: 20px;
}
.bp-normal { background: #ecfdf3; color: #166534; }
.bp-high { background: #fef2f2; color: #b91c1c; }
.bp-low { background: #fffbeb; color: #a16207; }

/* Medicine Cards */
.med-card {
  background: #f9fafb; border: 1px solid #e6e8ec; border-radius: 12px;
  padding: 14px; margin-top: 10px; display: flex; justify-content: space-between; align-items: center;
}
.med-info { display: flex; flex-direction: column; }
.med-name { font-size: 17px; font-weight: 600; color: #1a1d21; }
.med-dose { font-size: 13px; color: #6b7280; margin-top: 2px; }
.med-stock { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.med-status { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.status-ok { color: #166534; font-weight: 600; font-size: 14px; }
.status-wait { color: #9ca3af; font-size: 14px; }
.status-low-stock {
  background: #b91c1c; color: #fff;
  font-size: 12px; padding: 3px 10px; border-radius: 8px; font-weight: 600;
  letter-spacing: 0;
}

/* Location */
.loc-box {
  background: #f9fafb; border: 1px solid #e6e8ec; border-radius: 12px;
  padding: 14px; margin-top: 10px;
}
.loc-time { font-size: 13px; color: #6b7280; display: block; }
.loc-addr { font-size: 16px; color: #1a1d21; font-weight: 600; margin-top: 4px; display: block; }
.safe-actions { display: flex; flex-direction: column; gap: 10px; }

/* Info Row */
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 0; border-bottom: 1px solid #e6e8ec; margin-bottom: 16px;
}
.info-label { font-size: 15px; color: #6b7280; }
.info-val { font-size: 17px; font-weight: 600; color: #1a1d21; }
.btn-logout {
  height: 44px; line-height: 44px; font-size: 15px; font-weight: 600;
  background: #fef2f2; color: #b91c1c; border-radius: 10px; border: 1px solid #fecaca;
}

/* Remind */
.remind-list { display: flex; flex-direction: column; gap: 8px; margin: 10px 0; }
.remind-item {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  background: #f9fafb; padding: 10px 14px; border-radius: 10px; border: 1px solid #e6e8ec;
}
.btn-del-remind {
  background: #fef2f2; color: #b91c1c;
  font-size: 12px; font-weight: 600; height: 30px; line-height: 30px;
  border-radius: 8px; padding: 0 10px; border: 1px solid #fecaca;
}
.remind-add { display: flex; gap: 10px; align-items: center; margin-top: 10px; }
.remind-add .picker-box {
  border: 1px solid #e6e8ec; height: 40px; line-height: 40px; border-radius: 10px;
  padding: 0 14px; font-size: 15px; background: #ffffff; min-width: 100px; text-align: center; color: #1a1d21;
}
.btn-add-remind {
  background: #1e4a72; color: #fff;
  font-size: 14px; font-weight: 600; height: 40px; line-height: 40px; border-radius: 10px; padding: 0 16px; border: 1px solid #1e4a72;
}

/* Tab Bar */
.tab-bar {
  position: fixed; left: 0; right: 0; bottom: 0; height: 68px;
  background: #ffffff; border-top: 1px solid #e6e8ec;
  display: flex; align-items: center; justify-content: space-around;
  padding-bottom: env(safe-area-inset-bottom); z-index: 50;
}
.tab-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 4px 0; min-width: 50px;
}
.tab-glyph-wrap {
  width: 28px; height: 28px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  background: transparent;
}
.tab-glyph { font-size: 13px; font-weight: 600; color: #9ca3af; }
.tab-label { font-size: 11px; color: #9ca3af; font-weight: 500; }
.tab-active .tab-label { color: #1e4a72; font-weight: 600; }
.tab-active .tab-glyph-wrap { background: #e8ecf2; }
.tab-active .tab-glyph { color: #1e4a72; }

.debug { margin-top: 10px; color: #999; font-size: 12px; display: flex; flex-direction: column; gap: 4px; }

/* Bind / Profile / Relation — 我的 Tab */
.bind-status { margin-bottom: 6px; }
.bind-status .info-row { margin-bottom: 0; }
.bind-form { margin-top: 6px; }
.field { margin-bottom: 14px; }
.field-label { font-size: 13px; font-weight: 600; color: #6b7280; display: block; margin-bottom: 6px; }
.field-input {
  height: 46px; font-size: 15px; padding: 0 14px; color: #1a1d21; caret-color: #1e4a72;
  background: #ffffff; border: 1px solid #e6e8ec; border-radius: 10px; -webkit-text-fill-color: #1a1d21;
}
.code-row { display: flex; align-items: center; gap: 10px; }
.code-input { flex: 1; }
.btn-code {
  flex-shrink: 0; width: 110px; height: 46px; line-height: 46px; font-size: 13px; font-weight: 600;
  background: #f4f6f9; color: #1e4a72; border-radius: 10px; border: 1px solid #d8dee6; text-align: center; padding: 0;
}
.btn-code[disabled] { color: #9ca3af; background: #f3f4f6; }
.picker-box {
  height: 46px; line-height: 46px; border: 1px solid #e6e8ec; border-radius: 10px;
  padding: 0 14px; font-size: 15px; background: #ffffff; color: #1a1d21;
}
.btn-text {
  margin-top: 6px; height: 36px; line-height: 36px; font-size: 13px; font-weight: 600;
  background: transparent; color: #1e4a72; border: none; padding: 0; text-align: left;
}
.form-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0; border-bottom: 1px solid #f0f1f3;
}
.form-label { font-size: 15px; color: #4b5563; font-weight: 600; }
.form-input {
  width: 100px; height: 36px; font-size: 15px; text-align: right;
  padding: 0 10px; border: 1px solid #e6e8ec; border-radius: 10px;
  background: #ffffff; color: #1a1d21; caret-color: #1e4a72;
}
.radio-group { display: flex; gap: 16px; }
.radio-item { font-size: 15px; color: #1a1d21; display: flex; align-items: center; gap: 4px; }
.tags { display: flex; flex-wrap: wrap; gap: 10px; }
.tag {
  padding: 6px 14px; border: 1px solid #e6e8ec; border-radius: 999px;
  font-size: 13px; color: #6b7280; background: #ffffff;
}
.tag-selected { background: #1e4a72; color: #fff; border-color: #1e4a72; }

/* Guardian Settings */
.picker-inline {
  height: 36px; line-height: 36px; padding: 0 12px; border: 1px solid #e6e8ec;
  border-radius: 10px; background: #ffffff; font-size: 14px; color: #1a1d21;
}
.time-picker { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
.time-box {
  height: 40px; line-height: 40px; padding: 0 16px; border: 1px solid #e6e8ec;
  border-radius: 10px; background: #ffffff; font-size: 16px; font-weight: 600; color: #1a1d21;
}
.time-sep { font-size: 14px; color: #6b7280; }

/* 安全守护 Tab：收紧上下间距，尽量减少滚动 */
.safe-tab .page-header { margin-bottom: 8px; padding: 4px 0; }
.safe-tab .card { padding: 12px; margin-bottom: 10px; }
.safe-tab .btn-primary,
.safe-tab .btn-action { height: 40px; line-height: 40px; font-size: 14px; }
.safe-tab .form-row { padding: 8px 0; }
.safe-tab .loc-box { padding: 10px; margin-top: 8px; }
.safe-tab .safe-actions { gap: 8px; }

/* 我的 Tab：更紧凑的间距与层级 */
.me-tab .page-header { margin-bottom: 10px; padding: 4px 0; }
.me-tab .page-title { font-size: 20px; }
.me-tab .page-sub { font-size: 12px; color: #6b7280; }
.me-tab .card { padding: 14px; margin-bottom: 10px; border-radius: 12px; }
.me-tab .card-head { margin-bottom: 8px; }
.me-tab .card-head-title { font-size: 15px; }
.me-tab .card-head-sub { font-size: 11px; color: #9ca3af; }
.me-tab .info-row { padding: 10px 0; margin-bottom: 10px; }
.me-tab .field { margin-bottom: 10px; }
.me-tab .field-label { font-size: 12px; margin-bottom: 5px; }
.me-tab .field-input { height: 40px; font-size: 14px; border-radius: 10px; }
.me-tab .form-row { padding: 8px 0; }
.me-tab .form-input { height: 34px; font-size: 14px; }
.me-tab .btn-primary { height: 42px; line-height: 42px; font-size: 14px; }
.me-tab .btn-secondary { height: 40px; line-height: 40px; font-size: 14px; }
.me-tab .btn-code { height: 40px; line-height: 40px; }
.me-tab .picker-box { height: 40px; line-height: 40px; font-size: 14px; }
.me-tab .btn-text { height: 32px; line-height: 32px; font-size: 12px; }
.me-tab .radio-item { font-size: 14px; }
.me-tab .tag { font-size: 12px; padding: 5px 12px; }
.me-tab .btn-logout { height: 40px; line-height: 40px; font-size: 14px; }
</style>
