import axios from 'axios'

// axios 实例：baseURL '/api'，由 Vite dev server 代理到 http://localhost:8080
const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 响应拦截：解包统一响应包装 { code, message, data }
// code === 0 视为成功，直接返回 data；否则 reject(Error(message))
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    // 非包装结构（如文件流等）原样返回
    return body
  },
  (error) => {
    let message = '网络请求失败，请确认后端服务已启动'
    if (error.response) {
      const data = error.response.data
      message = (data && (data.message || data.msg)) || `请求失败（HTTP ${error.response.status}）`
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.message) {
      message = error.message
    }
    return Promise.reject(new Error(message))
  }
)

export default http
