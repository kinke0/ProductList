import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
  paramsSerializer: {
    serialize: (params) => {
      const parts = []
      for (const [key, value] of Object.entries(params)) {
        if (value == null || value === '') continue
        if (Array.isArray(value)) {
          value.forEach(v => parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(v)))
        } else {
          parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value))
        }
      }
      return parts.join('&')
    }
  }
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    if (response.config.responseType === 'text') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401 || error.response?.status === 403) {
      ElMessage.error(msg)
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request
