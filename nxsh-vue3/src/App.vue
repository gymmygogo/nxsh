<script>
import { initSilentGuardian, triggerAction } from './utils/heartbeat.js'
import { request } from '@/utils/request.js'
import { BASE_URL } from '@/config.js'

const WS_BASE_URL = BASE_URL.replace(/^http/, 'ws')

const parseId = (value) => {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) && n > 0 ? n : null
}

const isFamilyBound = (status) => {
  const boundFlag = status?.bound === true || status?.bound === 1 || status?.bound === '1'
  return boundFlag && parseId(status?.elderlyId) !== null
}

export default {
  onLaunch: function () {
    console.log('App Launch')
    initSilentGuardian()

    // 开启用药轮询拦截器逻辑
    startMedicineCheckTask()
    // 开启血压提醒本地轮询
    startBpReminderCheck()

    // Auto login check
    const token = uni.getStorageSync('token')
    const userType = uni.getStorageSync('userType')
    if (token) {
      if (userType === 'elderly') {
        connectWebSocket()
        uni.reLaunch({ url: '/pages/elderly/blood-pressure' })
      } else if (userType === 'family') {
        const familyId = uni.getStorageSync('familyId')
        if (!familyId) {
          uni.reLaunch({ url: '/pages/family/settings' })
        } else {
          request({
            url: '/family/bind/status',
            method: 'GET',
            data: { familyId },
            success: (res) => {
              if (res.statusCode === 200 && res.data.code === 200) {
                const status = res.data.data || {}
                const elderlyId = parseId(status.elderlyId)
                if (isFamilyBound(status) && elderlyId) {
                  uni.setStorageSync('elderlyId', elderlyId)
                  connectWebSocket()
                  uni.reLaunch({ url: '/pages/family/main' })
                } else {
                  uni.removeStorageSync('elderlyId')
                  uni.reLaunch({ url: '/pages/family/settings' })
                }
              } else {
                uni.showToast({ title: '获取绑定状态失败，请稍后重试', icon: 'none' })
                uni.reLaunch({ url: '/pages/family/main' })
              }
            },
            fail: () => {
              uni.showToast({ title: '网络请求失败，请稍后重试', icon: 'none' })
              uni.reLaunch({ url: '/pages/family/main' })
            }
          })
        }
      }
    } else {
      // Default jump to index
      uni.reLaunch({ url: '/pages/index/index' })
    }
  },
  onShow: function () {
    console.log('App Show')
    triggerAction()
  },
  onHide: function () {
    console.log('App Hide')
  },
  methods: {
  }
}

let wsTask = null
function connectWebSocket() {
  const userType = uni.getStorageSync('userType')
  const elderlyId = uni.getStorageSync('elderlyId')
  const familyId = uni.getStorageSync('familyId')

  let wsUrl = null
  if (userType === 'elderly' && elderlyId) {
    wsUrl = `${WS_BASE_URL}/ws/elderly/${elderlyId}`
  } else if (userType === 'family' && familyId) {
    wsUrl = `${WS_BASE_URL}/ws/family/${familyId}`
  }
  if (!wsUrl) return

  if (wsTask) {
    try { wsTask.close() } catch (e) {}
  }

  wsTask = uni.connectSocket({ url: wsUrl, complete: () => {} })
  uni.onSocketOpen(() => { console.log('WebSocket connected:', wsUrl) })
  uni.onSocketMessage((res) => {
    try {
      const data = JSON.parse(res.data)
      handleWsMessage(data, userType)
    } catch (e) { console.warn('WS parse error', e) }
  })
  uni.onSocketClose(() => {
    console.log('WebSocket closed, reconnect in 5s')
    setTimeout(connectWebSocket, 5000)
  })
}

function handleWsMessage(data, userType) {
  if (data.type === 'BP_REMINDER' && userType === 'elderly') {
    const msg = encodeURIComponent(data.msg || '请测量血压')
    const time = encodeURIComponent(data.remindTime || '')
    uni.navigateTo({ url: `/pages/elderly/bp-reminder?time=${time}&msg=${msg}` })
  } else if (data.type === 'HEALTH_REFRESH' && userType === 'family') {
    // 老人录入血压后，家属端自动刷新数据
    uni.$emit('healthDataRefresh', data)
  } else if (data.type === 'HEALTH_ALERT' && userType === 'family') {
    // 老人血压异常，弹出健康警报
    const sysdia = (data.sys && data.dia) ? `\n血压值：${data.sys}/${data.dia}` : ''
    uni.showModal({
      title: '健康警报',
      content: (data.msg || '老人血压异常，请及时关注') + sysdia,
      showCancel: false,
      confirmText: '立即查看'
    })
    uni.$emit('healthDataRefresh', data)
  } else if (data.type === 'GUARDIAN_ALERT' && userType === 'family') {
    uni.showModal({
      title: '守护预警',
      content: data.msg || '老人长时间未操作，请及时关注',
      showCancel: false,
      confirmText: '知道了'
    })
  } else if (data.type === 'MEDICINE_REMINDER' && userType === 'elderly') {
    // Already handled by polling, but also respond to push
    if (data.dueMedicines && data.dueMedicines.length > 0) {
      const group = data.dueMedicines[0] || data.dueMedicines
      uni.navigateTo({
        url: `/pages/elderly/medicine-reminder?data=${encodeURIComponent(JSON.stringify(group))}`
      })
    }
  } else if (data.type === 'TAKEN' && userType === 'family') {
    // 老人确认服药，刷新用药看板
    uni.$emit('medicineDashboardRefresh', data)
    uni.showToast({ title: data.msg || '老人已服药', icon: 'none', duration: 2000 })
  } else if (data.type === 'LOW_STOCK' && userType === 'family') {
    // 药品库存不足预警
    uni.showModal({
      title: '备药提醒',
      content: data.msg || '药品库存不足，请及时补货',
      showCancel: false,
      confirmText: '知道了'
    })
    uni.$emit('medicineDashboardRefresh', data)
  } else if (data.type === 'SOS' && userType === 'family') {
    // 老人紧急求救
    uni.showModal({
      title: '紧急求救',
      content: data.msg || '老人发出紧急求救，请立即关注！',
      showCancel: false,
      confirmText: '立即查看'
    })
    uni.$emit('locationRefresh', data)
  } else if (data.type === 'LOCATION_SHARE' && userType === 'family') {
    // 老人分享位置
    uni.showToast({ title: data.msg || '老人分享了位置', icon: 'none', duration: 2000 })
    uni.$emit('locationRefresh', data)
  }
}

// 定义一个基础的时间轮询，简单化处理
let medicineCheckTimer = null
function startMedicineCheckTask() {
  if (medicineCheckTimer) clearInterval(medicineCheckTimer)

  medicineCheckTimer = setInterval(() => {
    const userType = uni.getStorageSync('userType')
    const elderlyId = uni.getStorageSync('elderlyId')
    // 仅针对老人端发起检查并在前台展示
    if (userType === 'elderly' && elderlyId) {
      request({
        url: '/medicine/due',
        method: 'GET',
        data: { elderlyId, time: new Date().toISOString() },
        success: (res) => {
          if (res.statusCode === 200 && res.data.code === 200) {
            const dueGroups = res.data.data
            if (dueGroups && dueGroups.length > 0) {
              const currentGroup = dueGroups[0]
              // 跳转到用药弹窗页，传入数据
              uni.navigateTo({
                url: `/pages/elderly/medicine-reminder?data=${encodeURIComponent(JSON.stringify(currentGroup))}`
              })
            }
          }
        }
      })
    }
  }, 10000) // 开发测试阶段设为 10 秒，生产环境建议 60 秒
}

// 血压提醒本地轮询（补充 WebSocket 推送，确保后台回前台也能触发）
let bpReminderTimer = null
let lastBpReminderMinute = '' // 防止同一分钟重复弹窗
function startBpReminderCheck() {
  if (bpReminderTimer) clearInterval(bpReminderTimer)

  bpReminderTimer = setInterval(() => {
    const userType = uni.getStorageSync('userType')
    const elderlyId = uni.getStorageSync('elderlyId')
    if (userType !== 'elderly' || !elderlyId) return

    const now = new Date()
    const minuteKey = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
    if (minuteKey === lastBpReminderMinute) return // 同一分钟不重复

    request({
      url: '/api/health/remind/configs',
      method: 'GET',
      data: { elderlyId: Number(elderlyId) },
      success: (res) => {
        if (res.statusCode === 200 && res.data?.code === 200) {
          const configs = res.data.data || []
          for (const cfg of configs) {
            if (cfg.isActive !== 1) continue
            // remindTime 格式 "08:00:00" 或 "08:00"
            const cfgTime = (cfg.remindTime || '').substring(0, 5)
            if (cfgTime === minuteKey) {
              lastBpReminderMinute = minuteKey
              const elderlyName = uni.getStorageSync('elderlyName') || ''
              const msg = elderlyName
                ? `${elderlyName}，时间到了，请测量您的血压`
                : '时间到了，请测量您的血压'
              uni.navigateTo({
                url: `/pages/elderly/bp-reminder?time=${encodeURIComponent(cfgTime)}&msg=${encodeURIComponent(msg)}`
              })
              break
            }
          }
        }
      }
    })
  }, 15000) // 每 15 秒检查一次
}
</script>

<style>
/*每个页面公共css */
</style>
