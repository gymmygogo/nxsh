// 全局 API 基础地址配置
// H5 开发用 localhost，App 真机需改为电脑局域网 IP 或服务器地址

// #ifdef H5
export const BASE_URL = 'http://localhost:8080'
// #endif

// #ifdef APP-PLUS
export const BASE_URL = 'http://127.0.0.1:8080'
// #endif

// #ifdef MP-WEIXIN
export const BASE_URL = 'http://127.0.0.1:8080'
// #endif
