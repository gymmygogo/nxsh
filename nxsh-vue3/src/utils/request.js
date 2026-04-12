import { BASE_URL } from '@/config.js'

/**
 * 封装 uni.request，自动拼接 BASE_URL 和 Authorization header
 * @param {Object} options - 同 uni.request 参数，url 只需传路径（如 '/elderly/login'）
 * @returns {Promise}
 */
export const request = (options) => {
  const token = uni.getStorageSync('token')
  const header = { ...(options.header || {}) }

  if (token) {
    header['Authorization'] = token
  }

  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      url: BASE_URL + options.url,
      header,
      success: (res) => {
        // token 过期自动跳转登录
        if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('elderlyId')
          uni.removeStorageSync('elderlyPhone')
          uni.removeStorageSync('familyId')
          uni.removeStorageSync('familyPhone')
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => {
            uni.redirectTo({ url: '/pages/login/elderly-login' })
          }, 1500)
          return
        }
        if (options.success) {
          options.success(res)
        }
        resolve(res)
      },
      fail: (err) => {
        if (options.fail) {
          options.fail(err)
        }
        reject(err)
      }
    })
  })
}

/**
 * 封装 uni.uploadFile，自动拼接 BASE_URL 和 Authorization header
 * @param {Object} options - 同 uni.uploadFile 参数，url 只需传路径
 * @returns {Promise}
 */
export const uploadFile = (options) => {
  const token = uni.getStorageSync('token')
  const header = { ...(options.header || {}) }

  if (token) {
    header['Authorization'] = token
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      ...options,
      url: BASE_URL + options.url,
      header,
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          return
        }
        if (options.success) {
          options.success(res)
        }
        resolve(res)
      },
      fail: (err) => {
        if (options.fail) {
          options.fail(err)
        }
        reject(err)
      }
    })
  })
}
