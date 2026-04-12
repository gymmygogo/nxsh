# 暖夕守护 — App 打包指南

## 项目说明

本项目基于 **uni-app (Vue 3)** 开发，支持同时编译为：
- H5 网页（开发调试用）
- Android App
- iOS App
- 微信小程序

## 前置准备

### 1. 安装 HBuilderX（推荐方式）

下载地址：https://www.dcloud.io/hbuilderx.html

> HBuilderX 是 DCloud 官方 IDE，内置 App 云打包和真机调试功能，**无需配置 Android Studio**。

### 2. 修改服务器地址

编辑 `src/config.js`，将 `APP-PLUS` 分支中的 IP 改为你后端服务器的实际地址：

```js
// #ifdef APP-PLUS
export const BASE_URL = 'http://192.168.1.100:8080'  // ← 改为你的电脑局域网 IP
// #endif
```

查看本机 IP：
- Windows: `ipconfig`
- Mac/Linux: `ifconfig`

> 手机和电脑必须在同一局域网内。

## 打包方式

### 方式一：HBuilderX（推荐，无需 Android Studio）

1. 用 HBuilderX 打开 `nxsh-vue3` 目录
2. 菜单 → **运行** → **运行到手机或模拟器** → 选择你的设备
3. 首次运行会自动安装基座 App 到手机
4. 正式打包：菜单 → **发行** → **原生App-云打包**

### 方式二：CLI 命令行

```bash
cd nxsh-vue3

# 安装依赖（首次）
npm install

# ===== H5 开发模式 =====
npm run dev:h5

# ===== App 开发模式 =====
# 编译为 App 资源（输出到 dist/dev/app）
npm run dev:app

# 编译完成后，用 HBuilderX 打开 dist/dev/app 运行到手机

# ===== App 正式打包 =====
npm run build:app
# 输出到 dist/build/app，用 HBuilderX 云打包为 APK/IPA
```

## 真机调试步骤

### Android

1. 手机开启 **开发者选项** → **USB 调试**
2. 用 USB 连接电脑
3. HBuilderX → 运行 → 运行到手机 → 选择设备
4. App 自动安装到手机并启动

### iOS

1. 需要 Mac + Xcode
2. Apple 开发者账号
3. HBuilderX → 运行 → 运行到手机 → 选择 iOS 设备

## 已配置的权限

| 权限 | 用途 |
|------|------|
| BLUETOOTH / BLE | 连接 ESP32-C3 血压设备 |
| ACCESS_FINE_LOCATION | 安全守护定位 |
| RECORD_AUDIO | 亲情语音录制 |
| CAMERA | 未来扫码功能 |
| INTERNET / NETWORK | 网络通信 |
| VIBRATE / WAKE_LOCK | 提醒震动与唤醒 |

## 已配置的模块

在 `manifest.json` → `app-plus` → `modules` 中已声明：

- **Bluetooth** — BLE 蓝牙通信
- **Record** — 录音功能
- **Speech** — TTS 语音播报
- **Geolocation** — 定位服务

## 常见问题

### Q: App 端请求失败 / 网络错误
**A:** 检查 `config.js` 中 `APP-PLUS` 的 IP 地址是否正确，手机和电脑是否在同一 WiFi。

### Q: 蓝牙连接不上 ESP32
**A:** 确保 App 已授予蓝牙和定位权限（Android 12+ 扫描 BLE 需要定位权限）。

### Q: 真机上页面白屏
**A:** 检查控制台是否有 JS 报错。常见原因：ES6+ 语法未编译、API 地址不通。

### Q: iOS 打包需要什么
**A:** 需要 Mac 电脑 + Apple Developer 账号 ($99/年)。开发阶段可用 HBuilderX 基座调试，免费。
