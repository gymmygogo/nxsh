/**
 * ============================================================
 *  暖夕守护 — ESP32-C3 模拟健康采集终端
 * ============================================================
 *
 *  功能：
 *    1. BLE 外设，广播名称 "NXSH-BP"
 *    2. 物理按键检测（单击 / 双击 / 长按）
 *    3. 按键触发后通过 BLE Notify 发送 JSON 健康数据
 *    4. LED 状态反馈（蓝=上传中, 绿=正常, 红=异常）
 *
 *  按键映射：
 *    单击  → pressType = "single"  → 血压偏高 160/100
 *    双击  → pressType = "double"  → 血压偏低  90/60
 *    长按  → pressType = "long"    → 血压正常 120/80
 *
 *  接线（默认引脚，可在下方宏定义修改）：
 *    GPIO9  — 按键（ESP32-C3 BOOT 键，内部上拉，按下为 LOW）
 *    GPIO2  — 蓝色 LED（正极接 GPIO，负极串 220Ω 接 GND）
 *    GPIO3  — 绿色 LED
 *    GPIO4  — 红色 LED
 *
 *  BLE 协议：
 *    Service UUID:        0000FFE0-0000-1000-8000-00805F9B34FB
 *    Characteristic UUID: 0000FFE1-0000-1000-8000-00805F9B34FB
 *    数据方向:  Notify（ESP32 → App）
 *    数据格式:  UTF-8 JSON 字符串
 * ============================================================
 */

#include <Arduino.h>
#include <NimBLEDevice.h>

/* ======================== 引脚配置 ======================== */
#define BTN_PIN       9     // BOOT 按键（Active LOW）
#define LED_BLUE      2     // 蓝色 LED — 上传中
#define LED_GREEN     3     // 绿色 LED — 正常
#define LED_RED       4     // 红色 LED — 异常（偏高/偏低）

/* ======================== BLE 配置 ======================== */
#define BLE_DEVICE_NAME   "NXSH-BP"
#define SERVICE_UUID      "0000FFE0-0000-1000-8000-00805F9B34FB"
#define CHAR_UUID         "0000FFE1-0000-1000-8000-00805F9B34FB"

/* ======================== 按键时序 ======================== */
#define DEBOUNCE_MS       40     // 消抖
#define LONG_PRESS_MS     800    // 长按阈值
#define DOUBLE_GAP_MS     350    // 双击间隔上限
#define CLICK_TIMEOUT_MS  400    // 等待第二次点击超时

/* ======================== LED 时序 ======================== */
#define LED_BLINK_MS      120    // 蓝灯闪烁间隔
#define LED_RESULT_MS     3000   // 结果灯持续时长

/* ======================== 预设数据 ======================== */
// 单击 → 偏高
static const char* JSON_HIGH =
  "{\"pressType\":\"single\",\"sys\":160,\"dia\":100}";

// 双击 → 偏低
static const char* JSON_LOW =
  "{\"pressType\":\"double\",\"sys\":90,\"dia\":60}";

// 长按 → 正常
static const char* JSON_NORMAL =
  "{\"pressType\":\"long\",\"sys\":120,\"dia\":80}";

/* ======================== 全局状态 ======================== */
NimBLEServer*         pServer         = nullptr;
NimBLECharacteristic* pCharacteristic = nullptr;
bool deviceConnected    = false;
bool oldDeviceConnected = false;

// 按键状态机
enum BtnState { IDLE, PRESSED, WAIT_DOUBLE };
BtnState btnState       = IDLE;
bool     lastBtnReading = HIGH;
uint32_t lastDebounceMs = 0;
uint32_t pressStartMs   = 0;
uint32_t releaseMs      = 0;
int      clickCount     = 0;
bool     longPressFired = false;

// LED 控制
enum LedMode { LED_OFF, LED_BLINK_BLUE, LED_SOLID_GREEN, LED_SOLID_RED };
LedMode  currentLedMode  = LED_OFF;
uint32_t ledStartMs      = 0;
bool     ledBlinkState   = false;
uint32_t lastBlinkToggle = 0;

/* =================== BLE 回调 =================== */
class ServerCallbacks : public NimBLEServerCallbacks {
  void onConnect(NimBLEServer* pSvr) override {
    deviceConnected = true;
    Serial.println("[BLE] Client connected");
  }
  void onDisconnect(NimBLEServer* pSvr) override {
    deviceConnected = false;
    Serial.println("[BLE] Client disconnected");
  }
};

/* =================== LED 工具函数 =================== */
void allLedsOff() {
  digitalWrite(LED_BLUE,  LOW);
  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_RED,   LOW);
}

void setLedMode(LedMode mode) {
  currentLedMode  = mode;
  ledStartMs      = millis();
  ledBlinkState   = false;
  lastBlinkToggle = millis();
  allLedsOff();

  switch (mode) {
    case LED_BLINK_BLUE:
      // 立即亮一次
      digitalWrite(LED_BLUE, HIGH);
      ledBlinkState = true;
      break;
    case LED_SOLID_GREEN:
      digitalWrite(LED_GREEN, HIGH);
      break;
    case LED_SOLID_RED:
      digitalWrite(LED_RED, HIGH);
      break;
    default:
      break;
  }
}

void updateLed() {
  if (currentLedMode == LED_OFF) return;

  uint32_t now = millis();

  // 蓝灯闪烁
  if (currentLedMode == LED_BLINK_BLUE) {
    if (now - lastBlinkToggle >= LED_BLINK_MS) {
      ledBlinkState = !ledBlinkState;
      digitalWrite(LED_BLUE, ledBlinkState ? HIGH : LOW);
      lastBlinkToggle = now;
    }
    return; // 蓝灯闪到被替换为结果灯
  }

  // 结果灯自动熄灭
  if (currentLedMode == LED_SOLID_GREEN || currentLedMode == LED_SOLID_RED) {
    if (now - ledStartMs >= LED_RESULT_MS) {
      allLedsOff();
      currentLedMode = LED_OFF;
    }
  }
}

/* =================== BLE 发送 =================== */
void bleSend(const char* json, bool isNormal) {
  if (!deviceConnected) {
    Serial.println("[BLE] Not connected, skip send");
    // 即使未连接也给视觉反馈
    setLedMode(LED_SOLID_RED);
    return;
  }

  // 蓝灯闪烁表示"正在上传"
  setLedMode(LED_BLINK_BLUE);

  Serial.printf("[BLE] Sending: %s\n", json);
  pCharacteristic->setValue((const uint8_t*)json, strlen(json));
  pCharacteristic->notify();

  // 短暂延迟模拟上传过程，让蓝灯可见
  delay(300);

  // 切换到结果灯
  if (isNormal) {
    setLedMode(LED_SOLID_GREEN);
    Serial.println("[LED] GREEN — 正常");
  } else {
    setLedMode(LED_SOLID_RED);
    Serial.println("[LED] RED — 异常");
  }
}

/* =================== 按键处理 =================== */
void handlePress(const char* type) {
  Serial.printf("[BTN] Detected: %s\n", type);

  if (strcmp(type, "single") == 0) {
    bleSend(JSON_HIGH, false);    // 偏高 → 红灯
  } else if (strcmp(type, "double") == 0) {
    bleSend(JSON_LOW, false);     // 偏低 → 红灯
  } else if (strcmp(type, "long") == 0) {
    bleSend(JSON_NORMAL, true);   // 正常 → 绿灯
  }
}

/* =================== 按键检测状态机 =================== */
void updateButton() {
  bool reading = digitalRead(BTN_PIN);
  uint32_t now = millis();

  // 消抖
  if (reading != lastBtnReading) {
    lastDebounceMs = now;
  }
  lastBtnReading = reading;

  if (now - lastDebounceMs < DEBOUNCE_MS) return;

  // 当前稳定电平
  bool pressed = (reading == LOW); // Active LOW

  switch (btnState) {
    case IDLE:
      if (pressed) {
        btnState      = PRESSED;
        pressStartMs  = now;
        longPressFired = false;
      }
      break;

    case PRESSED:
      if (pressed) {
        // 还在按着 → 检测长按
        if (!longPressFired && (now - pressStartMs >= LONG_PRESS_MS)) {
          longPressFired = true;
          handlePress("long");
          // 等待松手后回 IDLE
        }
      } else {
        // 松手了
        if (longPressFired) {
          // 长按已处理，直接回 IDLE
          btnState   = IDLE;
          clickCount = 0;
        } else {
          // 短按松手 → 进入等待双击
          clickCount = 1;
          releaseMs  = now;
          btnState   = WAIT_DOUBLE;
        }
      }
      break;

    case WAIT_DOUBLE:
      if (pressed) {
        // 第二次按下 → 双击确认
        clickCount = 2;
        handlePress("double");
        // 等待松手
        while (digitalRead(BTN_PIN) == LOW) { delay(10); }
        btnState   = IDLE;
        clickCount = 0;
      } else if (now - releaseMs >= CLICK_TIMEOUT_MS) {
        // 超时未再按 → 单击
        handlePress("single");
        btnState   = IDLE;
        clickCount = 0;
      }
      break;
  }
}

/* =================== BLE 广播恢复 =================== */
void updateBleAdvertising() {
  if (!deviceConnected && oldDeviceConnected) {
    // 断连后重新开始广播
    delay(500);
    NimBLEDevice::startAdvertising();
    Serial.println("[BLE] Restart advertising");
  }
  oldDeviceConnected = deviceConnected;
}

/* =================== setup =================== */
void setup() {
  Serial.begin(115200);
  Serial.println("\n========================================");
  Serial.println("  暖夕守护 ESP32-C3 健康采集终端");
  Serial.println("========================================");

  // GPIO 初始化
  pinMode(BTN_PIN,   INPUT_PULLUP);
  pinMode(LED_BLUE,  OUTPUT);
  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_RED,   OUTPUT);
  allLedsOff();

  // 开机自检：RGB 依次闪烁
  Serial.println("[INIT] LED self-test...");
  digitalWrite(LED_RED, HIGH);   delay(200); digitalWrite(LED_RED, LOW);
  digitalWrite(LED_GREEN, HIGH); delay(200); digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_BLUE, HIGH);  delay(200); digitalWrite(LED_BLUE, LOW);

  // BLE 初始化
  Serial.println("[BLE] Initializing...");
  NimBLEDevice::init(BLE_DEVICE_NAME);
  NimBLEDevice::setPower(ESP_PWR_LVL_P9); // 最大发射功率

  pServer = NimBLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

  NimBLEService* pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
    CHAR_UUID,
    NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY
  );
  // NimBLE 会自动添加 CCCD (2902) descriptor

  pService->start();

  // 广播配置
  NimBLEAdvertising* pAdv = NimBLEDevice::getAdvertising();
  pAdv->addServiceUUID(SERVICE_UUID);
  pAdv->setScanResponse(true);
  pAdv->setMinPreferred(0x06);
  pAdv->setMaxPreferred(0x12);
  NimBLEDevice::startAdvertising();

  Serial.printf("[BLE] Advertising as \"%s\"\n", BLE_DEVICE_NAME);
  Serial.println("[READY] Waiting for connection...");
  Serial.println("  单击=偏高  双击=偏低  长按=正常");
}

/* =================== loop =================== */
void loop() {
  updateButton();
  updateLed();
  updateBleAdvertising();
  delay(5); // 省电 + 防止 WDT
}
