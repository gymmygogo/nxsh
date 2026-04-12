# 暖夕守护 — ESP32-C3 模拟健康采集终端

基于 ESP32-C3 的 BLE 蓝牙血压模拟采集设备，通过物理按键触发不同类型的模拟血压数据，经 BLE 发送到手机 App 并上传至云端。

## 硬件需求

| 组件 | 说明 |
|------|------|
| ESP32-C3 开发板 | 如 ESP32-C3-DevKitM-1 |
| 按键 | 使用板载 BOOT 键（GPIO9）或外接微动开关 |
| LED × 3 | 蓝色 (GPIO2)、绿色 (GPIO3)、红色 (GPIO4) |
| 220Ω 电阻 × 3 | LED 限流 |
| 面包板 + 杜邦线 | 连接用 |

## 接线图

```
ESP32-C3          外部元件
─────────         ────────
GPIO2  ──────┬──  蓝色 LED 正极
             └──  220Ω ── GND

GPIO3  ──────┬──  绿色 LED 正极
             └──  220Ω ── GND

GPIO4  ──────┬──  红色 LED 正极
             └──  220Ω ── GND

GPIO9  ──────┬──  按键一端（板载 BOOT 键可直接使用）
             └──  另一端接 GND（内部上拉，按下为 LOW）
```

## 按键操作

| 操作 | 动作 | 模拟数据 | LED 反馈 |
|------|------|----------|----------|
| **单击** | 短按后松开 | 血压偏高 160/100 | 蓝闪→红灯 3s |
| **双击** | 快速按两次 | 血压偏低 90/60 | 蓝闪→红灯 3s |
| **长按** | 按住 >0.8秒 | 血压正常 120/80 | 蓝闪→绿灯 3s |

## LED 状态说明

| 颜色 | 状态 | 含义 |
|------|------|------|
| 🔵 蓝色闪烁 | 正在发送 | 数据通过 BLE 上传中 |
| 🟢 绿色常亮 | 结果正常 | 血压正常 (3 秒后熄灭) |
| 🔴 红色常亮 | 结果异常 | 血压偏高或偏低 (3 秒后熄灭) |
| ⚫ 全灭 | 待机 | 等待按键操作 |

## BLE 协议

- **设备名称**: `NXSH-BP`
- **Service UUID**: `0000FFE0-0000-1000-8000-00805F9B34FB`
- **Characteristic UUID**: `0000FFE1-0000-1000-8000-00805F9B34FB`
- **通信方式**: Notify（ESP32 → App）
- **数据格式**: UTF-8 JSON

### JSON 数据示例

```json
// 单击 → 偏高
{"pressType":"single","sys":160,"dia":100}

// 双击 → 偏低
{"pressType":"double","sys":90,"dia":60}

// 长按 → 正常
{"pressType":"long","sys":120,"dia":80}
```

## 开发环境

### 方式一：PlatformIO（推荐）

```bash
# 安装 PlatformIO CLI 或使用 VS Code PlatformIO 插件
cd nxsh-hardware

# 编译
pio run

# 编译并烧录
pio run -t upload

# 打开串口监视器
pio device monitor -b 115200
```

### 方式二：Arduino IDE

1. 安装 ESP32 开发板包（Board Manager → esp32 by Espressif）
2. 安装库：`NimBLE-Arduino`（Library Manager 搜索安装）
3. 选择开发板：`ESP32C3 Dev Module`
4. 将 `src/main.cpp` 重命名为 `main.ino`，放入 `main/` 文件夹
5. 编译上传

## 串口输出示例

```
========================================
  暖夕守护 ESP32-C3 健康采集终端
========================================
[INIT] LED self-test...
[BLE] Initializing...
[BLE] Advertising as "NXSH-BP"
[READY] Waiting for connection...
  单击=偏高  双击=偏低  长按=正常
[BLE] Client connected
[BTN] Detected: single
[BLE] Sending: {"pressType":"single","sys":160,"dia":100}
[LED] RED — 异常
[BTN] Detected: long
[BLE] Sending: {"pressType":"long","sys":120,"dia":80}
[LED] GREEN — 正常
```

## App 端对接

App 老人端 `blood-pressure.vue` 页面已集成 BLE 扫描与数据接收：

1. 打开老人端血压记录页面
2. 点击 "🔗 连接设备" 按钮
3. App 自动扫描并连接名为 `NXSH-BP` 的 BLE 设备
4. 连接成功后，按下 ESP32-C3 按键即可自动录入血压数据

## 注意事项

- ESP32-C3 的 BOOT 键（GPIO9）可直接用作操作按键，无需外接
- LED 需串联 220Ω 限流电阻，直接接会损坏 LED 或 GPIO
- BLE 通信距离约 10 米，确保手机在范围内
- 首次使用需在 App 中授予蓝牙权限
