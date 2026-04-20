<template>
  <view class="page">
    <view class="hero">
      <image class="brand-logo" src="/static/logo.png" mode="aspectFit" />
      <text class="brand-title">暖夕守护</text>
      <text class="brand-sub">家庭健康与安全</text>
    </view>

    <view class="card-area">
      <view class="entry-card" @click="goToElderlyLogin">
        <view class="card-icon-wrap elderly">
          <text class="card-abbr">老</text>
        </view>
        <view class="card-body">
          <text class="card-title">老人端</text>
          <text class="card-desc">血压、用药、呼救与聊天</text>
        </view>
        <text class="card-arrow">›</text>
      </view>

      <view class="entry-card" @click="goToFamilyLogin">
        <view class="card-icon-wrap family">
          <text class="card-abbr">家</text>
        </view>
        <view class="card-body">
          <text class="card-title">家属端</text>
          <text class="card-desc">数据查看、用药与预警</text>
        </view>
        <text class="card-arrow">›</text>
      </view>
    </view>

    <text class="footer">暖夕守护</text>
  </view>
</template>

<script setup>
const goToElderlyLogin = () => {
  uni.navigateTo({ url: '/pages/login/elderly-login' })
}

const goToFamilyLogin = () => {
  uni.navigateTo({ url: '/pages/login/family-login' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f0f1f3;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero {
  padding-top: 72px;
  padding-bottom: 36px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.brand-logo {
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
  border-radius: 24px;
}
.brand-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1d21;
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.brand-sub {
  font-size: 14px;
  color: #6b7280;
  letter-spacing: 0.5px;
}
.card-area {
  width: 100%;
  padding: 0 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}
.entry-card {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 14px;
  padding: 18px 18px;
  border: 1px solid #e6e8ec;
}
.entry-card:active {
  opacity: 0.92;
}
.card-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  flex-shrink: 0;
  border: 1px solid #e6e8ec;
}
.card-icon-wrap.elderly {
  background: #faf8f6;
}
.card-icon-wrap.family {
  background: #f4f6f9;
}
.card-abbr {
  font-size: 17px;
  font-weight: 700;
  color: #374151;
}
.card-body {
  flex: 1;
}
.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1d21;
  display: block;
  margin-bottom: 4px;
}
.card-desc {
  font-size: 13px;
  color: #6b7280;
  display: block;
  line-height: 1.35;
}
.card-arrow {
  font-size: 22px;
  color: #c4c8ce;
  font-weight: 300;
  margin-left: 8px;
}
.footer {
  margin-top: auto;
  padding: 28px 0 36px;
  font-size: 12px;
  color: #9ca3af;
  letter-spacing: 1px;
}
</style>
