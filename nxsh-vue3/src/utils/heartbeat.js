import { request } from '@/utils/request.js'

let heartbeatTimer = null;

const sendHeartbeat = () => {
  if (heartbeatTimer) {
    clearTimeout(heartbeatTimer);
  }

  heartbeatTimer = setTimeout(() => {
    const userType = uni.getStorageSync('userType');
    const elderlyId = uni.getStorageSync('elderlyId');
    if (userType === 'elderly' && elderlyId) {
      request({
        url: '/elderly/heartbeat',
        method: 'POST',
        data: { elderlyId: Number(elderlyId) },
        header: { 'content-type': 'application/x-www-form-urlencoded' },
        success: (res) => {
          console.log('Heartbeat sent', res.statusCode);
        }
      });
    }
  }, 1000);
};

export const initSilentGuardian = () => {
  uni.$on('userAction', sendHeartbeat);

  // #ifdef H5
  // 全局监听点击/触摸，老人端任何交互均视为"活跃"
  document.addEventListener('click', triggerAction, true);
  // #endif

  // #ifdef APP-PLUS
  // App 端：监听页面切换和触摸事件
  plus.globalEvent.addEventListener('resume', triggerAction);
  // 每次页面显示时触发心跳
  uni.addInterceptor('navigateTo', { success: triggerAction });
  uni.addInterceptor('switchTab', { success: triggerAction });
  uni.addInterceptor('navigateBack', { success: triggerAction });
  // #endif
};

export const triggerAction = () => {
  uni.$emit('userAction');
};
