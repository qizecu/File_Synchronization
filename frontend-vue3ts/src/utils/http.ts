import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** 后端统一响应结构，按实际接口调整 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

const TOKEN_KEY = 'token'

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
  },
})

http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 文件流等直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    // 业务成功码：按后端约定改（常见 200 / 0）
    if (res.code === 200 || res.code === 0) {
      return res as unknown as AxiosResponse
    }

    ElMessage.error(res.message || '请求失败')

    // 未登录 / token 失效
    if (res.code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      router.push('/login')
    }

    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status as number | undefined
    const messageMap: Record<number, string> = {
      400: '请求参数错误',
      401: '登录已过期，请重新登录',
      403: '没有权限访问',
      404: '请求资源不存在',
      500: '服务器内部错误',
      502: '网关错误',
      503: '服务不可用',
      504: '网关超时',
    }

    const msg =
      (status && messageMap[status]) ||
      error.message ||
      '网络异常，请稍后重试'

    ElMessage.error(msg)

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      router.push('/login')
    }

    return Promise.reject(error)
  },
)

/** 封装后的请求方法，成功时直接返回 data */
function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return http.request<any, ApiResponse<T>>(config).then((res) => res.data)
}

export function get<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) {
  return request<T>({ ...config, method: 'GET', url, params })
}

export function post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) {
  return request<T>({ ...config, method: 'POST', url, data })
}

export function put<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) {
  return request<T>({ ...config, method: 'PUT', url, data })
}

export function del<T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) {
  return request<T>({ ...config, method: 'DELETE', url, params })
}

export default http
