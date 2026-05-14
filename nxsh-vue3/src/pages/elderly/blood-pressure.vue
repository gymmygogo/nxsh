<template>
  <view class="page">
    <view class="content">
      <!-- 模块：血压记录 -->
      <view v-if="activeTab === 'bp'">
        <view class="page-header">
          <view class="page-mark"><text class="page-mark-t">血</text></view>
          <view>
            <text class="page-title">血压记录</text>
            <text class="page-sub">先填写数值提交；设备与按键录入在下方展开</text>
          </view>
        </view>

        <view class="card card-bp-main">
          <view class="sysdia-row">
            <view class="sysdia-col">
              <text class="sysdia-lbl">收缩压 SYS</text>
              <input class="input-compact" type="number" v-model="sys" placeholder="120" />
            </view>
            <view class="sysdia-col">
              <text class="sysdia-lbl">舒张压 DIA</text>
              <input class="input-compact" type="number" v-model="dia" placeholder="80" />
            </view>
          </view>
          <button class="btn-primary btn-submit-bp" @click="submitManual">提交记录</button>
        </view>

        <view class="bp-action-btns">
          <button class="btn-bp-extra" :class="{ 'btn-bp-extra-on': showBlePanel }" @click="toggleBlePanel">
            {{ showBlePanel ? '收起蓝牙设备' : '蓝牙设备 (ESP32)' }}
          </button>
          <button class="btn-bp-extra" :class="{ 'btn-bp-extra-on': showQuickPanel }" @click="toggleQuickPanel">
            {{ showQuickPanel ? '收起按键录入' : '按键快捷录入' }}
          </button>
        </view>

        <view v-if="showBlePanel" class="card card-bp-extra">
          <text class="card-label">蓝牙设备</text>
          <view class="ble-status">
            <text :class="['ble-dot', bleConnected ? 'ble-on' : 'ble-off']">●</text>
            <text class="ble-text">{{ bleStatusText }}</text>
          </view>
          <button v-if="!bleConnected" class="btn-primary" @click="startBleScan">连接设备</button>
          <button v-else class="btn-secondary" @click="disconnectBle">断开连接</button>
        </view>

        <view v-if="showQuickPanel" class="card card-bp-extra">
          <text class="card-label">快捷录入</text>
          <text class="card-sub">模拟设备按键上报，真机请用实体键</text>
          <view class="btn-row">
            <view class="btn-quick-wrap">
              <button class="btn-quick btn-high" @click="submitByPress('SHORT')">偏高</button>
              <text class="btn-hint">单击</text>
            </view>
            <view class="btn-quick-wrap">
              <button class="btn-quick btn-normal" @click="submitByPress('LONG')">正常</button>
              <text class="btn-hint">长按</text>
            </view>
            <view class="btn-quick-wrap">
              <button class="btn-quick btn-low" @click="submitByPress('DOUBLE')">偏低</button>
              <text class="btn-hint">双击</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 模块：用药提醒 -->
      <view v-if="activeTab === 'medicine'">
        <view class="page-header">
          <view class="page-mark"><text class="page-mark-t">药</text></view>
          <view>
            <text class="page-title">用药提醒</text>
            <text class="page-sub">到点提醒，按时服药</text>
          </view>
        </view>

        <view class="card">
          <button class="btn-primary" @click="loadDueMedicines">刷新提醒</button>
          <view v-if="dueGroups.length === 0" class="empty">
            <view class="empty-line" />
            <text class="empty-text">暂无需要服用的药品</text>
          </view>
          <view v-for="(group, idx) in dueGroups" :key="idx" class="due-card">
            <text class="due-time">{{ formatTime(group.planTime) }} 需服药</text>
            <view class="due-item" v-for="item in group.medicines" :key="item.medicineId">
              <text class="due-name">{{ item.name }}</text>
              <text class="due-dose">{{ item.dosageDesc }}</text>
            </view>
            <button class="btn-secondary" @click="openReminder(group)">进入提醒</button>
          </view>
        </view>
      </view>

      <!-- 模块：暖心聊天 -->
      <view v-if="activeTab === 'ai'">
        <AiCare />
      </view>

      <!-- 模块：走位防丢失 -->
      <view v-if="activeTab === 'safe'">
        <view class="page-header">
          <view class="page-mark"><text class="page-mark-t">守</text></view>
          <view>
            <text class="page-title">安全守护</text>
            <text class="page-sub">定位上报，紧急呼救</text>
          </view>
        </view>

        <view class="card">
          <button class="btn-primary" @click="reportActive">
            <text>上报当前位置</text>
          </button>
        </view>

        <view class="sos-area">
          <button class="btn-sos" @click="handleSOS">一键呼救</button>
          <text class="sos-tip">如遇危险请立即点击</text>
        </view>
      </view>

      <!-- 模块：我的 -->
      <view v-if="activeTab === 'me'">
        <view class="page-header">
          <view class="page-mark"><text class="page-mark-t">我</text></view>
          <view>
            <text class="page-title">我的</text>
            <text class="page-sub">账号信息</text>
          </view>
        </view>

        <view class="card me-card">
          <text class="me-nickname">{{ elderlyNickname || elderlyName || '暖夕用户' }}</text>
          <text v-if="elderlyNickname && elderlyName" class="me-name">{{ elderlyName }}</text>
          <text class="me-phone">{{ phoneMask }}</text>
        </view>

        <view class="card">
          <button class="btn-logout-big" @click="handleLogout">退出登录</button>
        </view>
      </view>
    </view>

    <!-- 预警提示全屏弹窗 -->
    <view v-if="resultVisible" class="result-mask">
      <view class="result-fullscreen" :class="resultClass">
        <text class="result-label">血压状态</text>
        <text class="result-title">{{ resultText }}</text>
        <text v-if="resultValue" class="result-value">{{ resultValue }} mmHg</text>
        <text class="result-hint">{{ resultStatus === 1 ? '请注意休息，避免情绪波动' : resultStatus === 2 ? '请注意保暖，适当补充营养' : '继续保持良好生活习惯' }}</text>
        <button class="result-close-btn" @click="closeResult">我知道了</button>
      </view>
    </view>

    <!-- 底部 Tab -->
    <view class="tab-bar">
      <view class="tab-item" :class="tabClass('bp')" @click="setTab('bp')">
        <view class="tab-glyph-wrap"><text class="tab-glyph">血</text></view>
        <text class="tab-label">血压</text>
      </view>
      <view class="tab-item" :class="tabClass('medicine')" @click="setTab('medicine')">
        <view class="tab-glyph-wrap"><text class="tab-glyph">药</text></view>
        <text class="tab-label">用药</text>
      </view>
      <view class="tab-item" :class="tabClass('ai')" @click="setTab('ai')">
        <view class="tab-glyph-wrap"><text class="tab-glyph">聊</text></view>
        <text class="tab-label">聊天</text>
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
import { onLoad } from '@dcloudio/uni-app'
import AiCare from './ai-care.vue'
import { request } from '@/utils/request.js'

const activeTab = ref('bp')

onLoad((options) => {
  if (options && options.tab) {
    activeTab.value = options.tab
  }
})

const elderlyName = ref(uni.getStorageSync('elderlyName') || '')
const elderlyNickname = ref(uni.getStorageSync('elderlyNickname') || '')

const sys = ref('')
const dia = ref('')

const showBlePanel = ref(false)
const showQuickPanel = ref(false)

const toggleBlePanel = () => {
  showBlePanel.value = !showBlePanel.value
  if (showBlePanel.value) showQuickPanel.value = false
}

const toggleQuickPanel = () => {
  showQuickPanel.value = !showQuickPanel.value
  if (showQuickPanel.value) showBlePanel.value = false
}

const resultVisible = ref(false)
const resultText = ref('')
const resultValue = ref('')
const resultStatus = ref(0) // 0正常 1偏高 2偏低

const dueGroups = ref([])

// ===== BLE 蓝牙连接 =====
const BLE_SERVICE    = '0000FFE0-0000-1000-8000-00805F9B34FB'
const BLE_CHAR       = '0000FFE1-0000-1000-8000-00805F9B34FB'
const BLE_NAME       = 'NXSH-BP'

const bleConnected   = ref(false)
const bleStatusText  = ref('未连接')
let bleDeviceId      = ''
let bleServiceId     = ''
let bleCharId        = ''

const startBleScan = () => {
  // #ifdef H5
  uni.showToast({ title: 'H5 环境不支持蓝牙，请使用真机', icon: 'none' })
  return
  // #endif

  // #ifndef H5
  bleStatusText.value = '正在搜索设备...'
  uni.openBluetoothAdapter({
    success: () => {
      uni.startBluetoothDevicesDiscovery({
        allowDuplicatesKey: false,
        success: () => {
          uni.onBluetoothDeviceFound((res) => {
            for (const device of res.devices) {
              if (device.name === BLE_NAME || device.localName === BLE_NAME) {
                uni.stopBluetoothDevicesDiscovery()
                bleStatusText.value = '发现设备，正在连接...'
                connectBle(device.deviceId)
                return
              }
            }
          })
          // 10秒超时
          setTimeout(() => {
            if (!bleConnected.value) {
              uni.stopBluetoothDevicesDiscovery()
              bleStatusText.value = '未找到设备'
              uni.showToast({ title: '未找到 NXSH-BP 设备', icon: 'none' })
            }
          }, 10000)
        },
        fail: () => {
          bleStatusText.value = '搜索失败'
          uni.showToast({ title: '蓝牙搜索失败', icon: 'none' })
        }
      })
    },
    fail: () => {
      bleStatusText.value = '蓝牙未开启'
      uni.showToast({ title: '请开启手机蓝牙', icon: 'none' })
    }
  })
  // #endif
}

const connectBle = (deviceId) => {
  bleDeviceId = deviceId
  uni.createBLEConnection({
    deviceId,
    success: () => {
      bleStatusText.value = '已连接，获取服务...'
      // 延迟获取服务（部分安卓需要）
      setTimeout(() => { discoverServices() }, 800)
    },
    fail: (err) => {
      bleStatusText.value = '连接失败'
      uni.showToast({ title: '连接失败: ' + (err.errMsg || ''), icon: 'none' })
    }
  })

  uni.onBLEConnectionStateChange((state) => {
    if (!state.connected) {
      bleConnected.value = false
      bleStatusText.value = '连接已断开'
    }
  })
}

const discoverServices = () => {
  uni.getBLEDeviceServices({
    deviceId: bleDeviceId,
    success: (res) => {
      const svc = res.services.find(s =>
        s.uuid.toUpperCase() === BLE_SERVICE.toUpperCase()
      )
      if (!svc) {
        bleStatusText.value = '服务不匹配'
        uni.showToast({ title: '未找到健康数据服务', icon: 'none' })
        return
      }
      bleServiceId = svc.uuid
      discoverCharacteristics()
    },
    fail: () => {
      bleStatusText.value = '获取服务失败'
    }
  })
}

const discoverCharacteristics = () => {
  uni.getBLEDeviceCharacteristics({
    deviceId: bleDeviceId,
    serviceId: bleServiceId,
    success: (res) => {
      const ch = res.characteristics.find(c =>
        c.uuid.toUpperCase() === BLE_CHAR.toUpperCase()
      )
      if (!ch) {
        bleStatusText.value = '特征值不匹配'
        return
      }
      bleCharId = ch.uuid
      enableNotify()
    },
    fail: () => {
      bleStatusText.value = '获取特征失败'
    }
  })
}

const enableNotify = () => {
  uni.notifyBLECharacteristicValueChange({
    deviceId: bleDeviceId,
    serviceId: bleServiceId,
    characteristicId: bleCharId,
    state: true,
    success: () => {
      bleConnected.value = true
      bleStatusText.value = '已连接 · 等待按键'

      uni.onBLECharacteristicValueChange((res) => {
        if (res.characteristicId.toUpperCase() !== BLE_CHAR.toUpperCase()) return
        const raw = ab2str(res.value)
        console.log('[BLE] Received:', raw)
        try {
          const data = JSON.parse(raw)
          handleBleData(data)
        } catch (e) {
          console.warn('[BLE] Parse error:', e)
        }
      })
    },
    fail: () => {
      bleStatusText.value = '订阅通知失败'
    }
  })
}

const ab2str = (buf) => {
  const arr = new Uint8Array(buf)
  let str = ''
  for (let i = 0; i < arr.length; i++) {
    str += String.fromCharCode(arr[i])
  }
  return str
}

const handleBleData = (data) => {
  bleStatusText.value = '收到数据，提交中...'
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    uni.showToast({ title: '请先登录老人账号', icon: 'none' })
    return
  }

  // 如果硬件发送了 sys/dia，直接用；否则用 pressType
  const payload = { elderlyId: Number(elderlyId), deviceId: 'esp32-ble', recordTime: formatDateTime(new Date()) }

  if (data.sys && data.dia) {
    payload.sys = data.sys
    payload.dia = data.dia
  } else if (data.pressType) {
    payload.pressType = data.pressType
  }

  recordToServer(payload)
  bleStatusText.value = '已连接 · 等待按键'
}

const disconnectBle = () => {
  if (bleDeviceId) {
    uni.closeBLEConnection({ deviceId: bleDeviceId })
  }
  bleConnected.value = false
  bleStatusText.value = '未连接'
  bleDeviceId = ''
}

onBeforeUnmount(() => {
  if (bleDeviceId) {
    try { uni.closeBLEConnection({ deviceId: bleDeviceId }) } catch (e) {}
  }
  try { uni.closeBluetoothAdapter() } catch (e) {}
})
// ===== END BLE =====

const phoneMask = computed(() => {
  const phone = uni.getStorageSync('elderlyPhone') || ''
  if (phone.length >= 7) {
    return phone.slice(0, 3) + '****' + phone.slice(-4)
  }
  return phone || '未登录'
})

const resultClass = computed(() => {
  if (resultStatus.value === 1) return 'danger'
  if (resultStatus.value === 2) return 'warn'
  return 'ok'
})

const formatDateTime = (d) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const formatLocalDateTimeForApi = (d = new Date()) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const tabClass = (key) => (activeTab.value === key ? 'tab-active' : '')

const setTab = (key) => {
  activeTab.value = key
  if (key !== 'bp') {
    showBlePanel.value = false
    showQuickPanel.value = false
  }
  if (key === 'medicine') {
    loadDueMedicines()
  }
}

const speak = (text) => {
  if (!text) return
  try {
    if (typeof plus !== 'undefined' && plus.tts) {
      plus.tts.speak(text)
    }
  } catch (e) {}
}

const showResult = (resp) => {
  const status = resp?.status ?? 0
  resultStatus.value = status

  if (status === 1) {
    resultText.value = '偏高'
    speak('血压偏高，请注意休息')
  } else if (status === 2) {
    resultText.value = '偏低'
    speak('血压偏低，请注意保暖')
  } else {
    resultText.value = '正常'
    speak('血压正常')
  }

  if (resp?.sys && resp?.dia) {
    resultValue.value = `${resp.sys}/${resp.dia}`
  } else {
    resultValue.value = ''
  }

  resultVisible.value = true
}

const recordToServer = (payload) => {
  console.log('[recordToServer] payload =', JSON.stringify(payload))

  // 1. 获取本地缓存中保存的 token
  const myToken = uni.getStorageSync('token')

  // 2. 核心大招：换成官方原生的 uni.request，绕过那个有 bug 的封装！
  uni.request({
    // 注意：因为不用封装的请求了，这里需要写全完整的后端地址。
    // 如果你是在手机上扫码测试，请把 localhost 换成你的电脑局域网 IP (如 192.168.x.x)
    url: 'http://192.168.3.27:8080/api/health/record',

    // 原生方法绝对会乖乖听话，使用 POST
    method: 'POST',

    header: {
      'content-type': 'application/json', // 微信官方推荐全小写
      'Authorization': myToken
    },
    data: payload,
    success: (res) => {
      console.log('[recordToServer] response =', res.statusCode, JSON.stringify(res.data))
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        uni.showToast({ title: '记录成功！', icon: 'success' })
        // 保证原有功能正常运行
        if(typeof showResult === 'function') {
          showResult(res.data.data)
        }
      } else {
        uni.showToast({ title: res.data?.message || '提交失败', icon: 'none' })
      }
    },
    fail: (err) => {
      console.log('[recordToServer] fail =', JSON.stringify(err))
      uni.showToast({ title: '网络请求失败，请检查后端', icon: 'none' })
    }
  })
}

const submitManual = () => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    uni.showToast({ title: '请先登录老人账号', icon: 'none' })
    return
  }
  if (!sys.value || !dia.value) {
    uni.showToast({ title: '请填写SYS和DIA', icon: 'none' })
    return
  }

  recordToServer({
    elderlyId: Number(elderlyId),
    deviceId: 'manual',
    sys: Number(sys.value),
    dia: Number(dia.value),
    recordTime: formatDateTime(new Date())
  })
}

const submitByPress = (pressType) => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    uni.showToast({ title: '请先登录老人账号', icon: 'none' })
    return
  }

  recordToServer({
    elderlyId: Number(elderlyId),
    deviceId: 'esp32-demo',
    pressType,
    recordTime: formatDateTime(new Date())
  })
}

const closeResult = () => {
  resultVisible.value = false
}

const formatTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const loadDueMedicines = () => {
  const elderlyId = uni.getStorageSync('elderlyId')
  if (!elderlyId) {
    dueGroups.value = []
    return
  }

  const queryTime = formatLocalDateTimeForApi(new Date())
  request({
    url: '/medicine/due',
    method: 'GET',
    // 刷新提醒时查询“整天”计划，避免因为当前时刻不在服药点而误判为空
    data: { elderlyId: Number(elderlyId), time: queryTime, windowMinutes: 1439 },
    success: (res) => {
      if (res.statusCode === 200 && res.data && res.data.code === 200) {
        dueGroups.value = res.data.data || []
        console.log('[medicine due] query=', { elderlyId: Number(elderlyId), time: queryTime, windowMinutes: 1439 }, 'result=', res.data.data)
      }
    }
  })
}

const openReminder = (group) => {
  uni.navigateTo({
    url: `/pages/elderly/medicine-reminder?data=${encodeURIComponent(JSON.stringify(group))}`
  })
}

const reportActive = () => {
  uni.showLoading({ title: '定位中...', mask: true })
  uni.getLocation({
    type: 'wgs84',
    success: (res) => {
      const elderlyId = uni.getStorageSync('elderlyId') || 1
      request({
        url: '/elderly/location/active',
        method: 'POST',
        data: {
          elderlyId: Number(elderlyId),
          latitude: res.latitude,
          longitude: res.longitude
        },
        success: () => {
          uni.hideLoading()
          uni.showToast({ title: '位置已上报', icon: 'success' })
        },
        fail: () => {
          uni.hideLoading()
          uni.showToast({ title: '上报失败', icon: 'none' })
        }
      })
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({ title: '无法获取定位', icon: 'none' })
    }
  })
}

const handleSOS = () => {
  speak('正在发送求救信号，请稍候')
  uni.showLoading({ title: '定位中...', mask: true })

  uni.getLocation({
    type: 'wgs84',
    success: (res) => {
      const elderlyId = uni.getStorageSync('elderlyId') || 1

      request({
        url: '/elderly/location/sos',
        method: 'POST',
        data: {
          elderlyId: Number(elderlyId),
          latitude: res.latitude,
          longitude: res.longitude
        },
        success: (apiRes) => {
          uni.hideLoading()
          if (apiRes.statusCode === 200 && apiRes.data.code === 200) {
            const contactPhone = apiRes.data.data?.firstContactPhone
            if (contactPhone) {
              uni.makePhoneCall({ phoneNumber: contactPhone })
            } else {
              uni.showToast({ title: '未设置紧急联系人', icon: 'none' })
            }
          } else {
            uni.showToast({ title: '上报失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.hideLoading()
          uni.showToast({ title: '网络异常', icon: 'none' })
        }
      })
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({ title: '无法获取定位', icon: 'none' })
    }
  })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '您确定要退出当前账号吗？',
    confirmText: '退出',
    cancelText: '取消',
    confirmColor: '#ff4d4f',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('elderlyId')
        uni.removeStorageSync('elderlyPhone')
        uni.removeStorageSync('elderlyName')
        uni.removeStorageSync('elderlyNickname')
        uni.redirectTo({ url: '/pages/login/elderly-login' })
      }
    }
  })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f0ee; }
.content { padding: 16px 16px 90px; }

/* Page Header */
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 8px 0; }
.page-mark {
  width: 44px; height: 44px; border-radius: 12px; background: #ffffff;
  border: 1px solid #e2ddd8; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.page-mark-t { font-size: 17px; font-weight: 700; color: #5c4033; }
.page-title { font-size: 20px; font-weight: 600; color: #1c1917; display: block; }
.page-sub { font-size: 13px; color: #6b6560; display: block; margin-top: 2px; line-height: 1.4; }

/* Cards */
.card {
  background: #ffffff; border-radius: 14px; padding: 18px;
  margin-bottom: 12px; border: 1px solid #e2ddd8; box-shadow: none;
}
.card-bp-main { padding-bottom: 14px; }
.card-label { font-size: 16px; font-weight: 600; color: #57534e; display: block; margin: 10px 0 8px; }
.card-label:first-child { margin-top: 0; }
.input-big {
  height: 64px; font-size: 28px; border: 1px solid #e2ddd8; border-radius: 12px;
  padding: 0 16px; background: #fafaf9; color: #1c1917; caret-color: #6b4f3c;
}
.sysdia-row { display: flex; gap: 12px; margin-bottom: 4px; }
.sysdia-col { flex: 1; min-width: 0; }
.sysdia-lbl {
  font-size: 14px; font-weight: 600; color: #57534e; display: block; margin-bottom: 6px;
}
.input-compact {
  width: 100%; box-sizing: border-box;
  height: 56px; font-size: 24px; font-weight: 600; border: 1px solid #e2ddd8; border-radius: 12px;
  padding: 0 12px; background: #fafaf9; color: #1c1917; caret-color: #6b4f3c;
}
.btn-submit-bp { margin-top: 14px; }

/* 血压 Tab：次要入口（用药 Tab 同款思路） */
.bp-action-btns { display: flex; flex-direction: column; gap: 10px; margin-bottom: 14px; }
.btn-bp-extra {
  height: 50px; line-height: 50px; font-size: 15px; font-weight: 600;
  border-radius: 12px; border: 1px solid #e2ddd8; background: #ffffff; color: #57534e;
  padding: 0 16px; text-align: center;
}
.btn-bp-extra-on {
  border-color: #6b4f3c; color: #6b4f3c; background: #f5f0eb;
}

/* Primary Button */
.btn-primary {
  margin-top: 12px; height: 60px; line-height: 60px; font-size: 18px; font-weight: 600;
  background: #6b4f3c; color: #fff;
  border-radius: 12px; border: 1px solid #6b4f3c; box-shadow: none;
}
.card-bp-extra .btn-primary { margin-top: 8px; }

/* Quick Buttons */
.btn-row { display: flex; gap: 10px; margin-top: 6px; }
.btn-quick-wrap { flex: 1; display: flex; flex-direction: column; align-items: center; }
.btn-quick {
  width: 100%; height: 64px; line-height: 64px; font-size: 22px; font-weight: 800;
  border-radius: 16px; border: none; letter-spacing: 2px;
}
.btn-hint { font-size: 12px; color: #78716c; margin-top: 4px; }
.btn-high { background: #b91c1c; color: #fff; }
.btn-normal { background: #166534; color: #fff; }
.btn-low { background: #ca8a04; color: #fff; }

/* Empty State */
.empty { text-align: center; padding: 20px 0; }
.empty-line {
  width: 28px; height: 2px; background: #d6d3d1; border-radius: 1px;
  margin: 0 auto 10px;
}
.empty-text { font-size: 15px; color: #a8a29e; }

/* Due Cards */
.due-card { background: #fafaf9; border: 1px solid #e2ddd8; border-radius: 12px; padding: 14px; margin-top: 12px; }
.due-time { font-size: 16px; font-weight: 600; color: #6b4f3c; display: block; margin-bottom: 8px; }
.due-item { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; }
.due-name { font-size: 17px; font-weight: 600; color: #1c1917; }
.due-dose { font-size: 14px; color: #78716c; }
.btn-secondary {
  margin-top: 10px; height: 48px; line-height: 48px; font-size: 16px; font-weight: 600;
  border-radius: 10px; background: #f5f0eb; color: #6b4f3c; border: 1px solid #e2ddd8;
}

/* SOS Area */
.sos-area { display: flex; flex-direction: column; align-items: center; margin-top: 20px; }
.btn-sos {
  width: 100%; height: 80px; line-height: 80px; font-size: 20px; font-weight: 600;
  background: #b91c1c; color: #fff;
  border-radius: 12px; border: 1px solid #991b1b; box-shadow: none;
}
.sos-tip { margin-top: 10px; font-size: 14px; color: #991b1b; font-weight: 500; }

/* Info Row */
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #e7e5e4; margin-bottom: 16px; }
.info-label { font-size: 16px; color: #78716c; }
.info-val { font-size: 18px; font-weight: 600; color: #1c1917; }
/* 我的 - 老人端极简展示 */
.me-card { text-align: center; padding: 30px 20px; }
.me-nickname { font-size: 32px; font-weight: 700; color: #1c1917; display: block; margin-bottom: 6px; }
.me-name { font-size: 18px; color: #78716c; display: block; margin-bottom: 10px; }
.me-phone { font-size: 22px; color: #6b4f3c; font-weight: 600; display: block; letter-spacing: 1px; }
.btn-logout-big {
  height: 64px; line-height: 64px; font-size: 18px; font-weight: 600;
  background: #b91c1c; color: #fff; border-radius: 12px; border: 1px solid #991b1b;
  box-shadow: none; letter-spacing: 2px;
}

.card-sub { font-size: 13px; color: #78716c; display: block; margin-bottom: 10px; }

/* 预警提示全屏弹窗 */
.result-mask {
  position: fixed; left: 0; top: 0; right: 0; bottom: 0;
  z-index: 200;
}
.result-fullscreen {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 40px 30px;
}
.result-fullscreen.ok { background: #ecfdf3; }
.result-fullscreen.danger { background: #fef2f2; }
.result-fullscreen.warn { background: #fffbeb; }
.result-label { font-size: 14px; color: #57534e; display: block; margin-bottom: 8px; font-weight: 500; letter-spacing: 2px; }
.result-title { font-size: 44px; font-weight: 700; color: #1c1917; display: block; margin-bottom: 16px; letter-spacing: 4px; }
.result-fullscreen.danger .result-title { color: #991b1b; }
.result-fullscreen.warn .result-title { color: #854d0e; }
.result-fullscreen.ok .result-title { color: #166534; }
.result-value { font-size: 26px; font-weight: 600; color: #44403c; display: block; margin-bottom: 20px; }
.result-hint { font-size: 16px; color: #57534e; display: block; line-height: 26px; text-align: center; margin-bottom: 40px; max-width: 300px; }
.result-close-btn {
  width: 260px; height: 56px; line-height: 56px; font-size: 17px; font-weight: 600;
  border-radius: 12px; background: #ffffff; color: #1c1917;
  border: 1px solid #d6d3d1; letter-spacing: 2px;
}

/* Tab Bar */
.tab-bar {
  position: fixed; left: 0; right: 0; bottom: 0; height: 68px;
  background: #ffffff; border-top: 1px solid #e7e5e4;
  display: flex; align-items: center; justify-content: space-around;
  padding-bottom: env(safe-area-inset-bottom); z-index: 50;
}
.tab-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 4px 0; min-width: 44px;
}
.tab-glyph-wrap {
  width: 28px; height: 28px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
}
.tab-glyph { font-size: 13px; font-weight: 600; color: #a8a29e; }
.tab-label { font-size: 10px; color: #a8a29e; font-weight: 500; }
.tab-active .tab-label { color: #6b4f3c; font-weight: 600; }
.tab-active .tab-glyph-wrap { background: #ede9e6; }
.tab-active .tab-glyph { color: #5c4033; }

/* BLE Status */
.ble-status { display: flex; align-items: center; gap: 8px; margin: 8px 0 12px; }
.ble-dot { font-size: 14px; }
.ble-on { color: #34C759; }
.ble-off { color: #CCCCCC; }
.ble-text { font-size: 14px; color: #78716c; }
</style>
