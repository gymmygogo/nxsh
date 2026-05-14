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

</script>

<style>
/*每个页面公共css */
</style>
