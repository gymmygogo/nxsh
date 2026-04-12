<template>
  <view class="page">
    <view class="hero">
      <view class="brand-icon">
        <text class="brand-emoji">🌅</text>
      </view>
      <text class="brand-title">暖夕守护</text>
      <text class="brand-sub">用温暖科技 守护最珍贵的人</text>
    </view>

    <view class="card-area">
      <view class="entry-card card-elderly" @click="goToElderlyLogin">
        <view class="card-icon-wrap elderly-icon">
          <text class="card-emoji">👴</text>
        </view>
        <view class="card-body">
          <text class="card-title">老人端</text>
          <text class="card-desc">一键呼救 · 血压记录 · 暖心聊天</text>
        </view>
        <text class="card-arrow">›</text>
      </view>

      <view class="entry-card card-family" @click="goToFamilyLogin">
        <view class="card-icon-wrap family-icon">
          <text class="card-emoji">👨‍👩‍👧</text>
        </view>
        <view class="card-body">
          <text class="card-title">家属端</text>
          <text class="card-desc">远程守护 · 用药管理 · 实时预警</text>
        </view>
        <text class="card-arrow">›</text>
      </view>
    </view>

    <text class="footer">暖夕守护 — 让爱不掉线</text>
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
  background: linear-gradient(180deg, #FFF1E6 0%, #FFFFFF 50%);
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero {
  padding-top: 80px;
  padding-bottom: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.brand-icon {
  width: 100px;
  height: 100px;
  border-radius: 28px;
  background: linear-gradient(135deg, #FF9A56 0%, #E8825A 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(232, 130, 90, 0.3);
}
.brand-emoji { font-size: 48px; }
.brand-title {
  font-size: 34px;
  font-weight: 800;
  color: #2D2D2D;
  letter-spacing: 4px;
  margin-bottom: 10px;
}
.brand-sub {
  font-size: 15px;
  color: #A0836C;
  letter-spacing: 1px;
}
.card-area {
  width: 100%;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 30px;
}
.entry-card {
  display: flex;
  align-items: center;
  background: #FFFFFF;
  border-radius: 18px;
  padding: 22px 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
}
.entry-card:active {
  transform: scale(0.98);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.card-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}
.elderly-icon { background: linear-gradient(135deg, #FFE0CC 0%, #FFDAB9 100%); }
.family-icon { background: linear-gradient(135deg, #D6EBFF 0%, #BDDCFF 100%); }
.card-emoji { font-size: 28px; }
.card-body { flex: 1; }
.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #2D2D2D;
  display: block;
  margin-bottom: 4px;
}
.card-desc {
  font-size: 13px;
  color: #A0A0A0;
  display: block;
}
.card-arrow {
  font-size: 28px;
  color: #D0D0D0;
  font-weight: 300;
  margin-left: 8px;
}
.footer {
  margin-top: auto;
  padding: 30px 0 40px;
  font-size: 13px;
  color: #C4B0A0;
  letter-spacing: 1px;
}
</style>
