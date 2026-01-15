/**
 * DocAggregator API 服务
 * 
 * 封装版本管理、Diff 对比等扩展 API
 */
import axios from 'axios'

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: '/',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器 - 统一错误处理
apiClient.interceptors.response.use(
  response => response.data,
  error => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 401:
          console.error('未授权，请登录')
          break
        case 403:
          console.error('拒绝访问')
          break
        case 404:
          console.error('请求的资源不存在')
          break
        case 500:
          console.error('服务器内部错误')
          break
        default:
          console.error(`请求错误: ${response.status}`)
      }
    } else {
      console.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

/**
 * 从 swagger url 解析 serviceName
 * 例如: /user-service/v3/api-docs -> user-service
 */
export function parseServiceName(url) {
  if (!url) return null
  // 去掉开头的 /
  const path = url.startsWith('/') ? url.substring(1) : url
  // 取第一个 / 前的段
  const firstSlash = path.indexOf('/')
  if (firstSlash === -1) return path
  return path.substring(0, firstSlash)
}

/**
 * 获取服务的版本列表
 * @param {string} serviceName 服务名
 * @param {number} page 页码（默认0）
 * @param {number} size 每页数量（默认20）
 * @returns {Promise<Array>} 版本列表
 */
export async function getVersions(serviceName, page = 0, size = 20) {
  try {
    const response = await apiClient.get(`/api/docs/${serviceName}/versions`, {
      params: { page, size }
    })
    // 响应可能是数组或包装对象
    return Array.isArray(response) ? response : (response.data || [])
  } catch (error) {
    console.error(`获取版本列表失败: ${serviceName}`, error)
    return []
  }
}

/**
 * 获取指定版本的文档
 * @param {string} serviceName 服务名
 * @param {number} version 版本号
 * @returns {Promise<Object>} 版本文档详情
 */
export async function getDocByVersion(serviceName, version) {
  try {
    const response = await apiClient.get(`/api/docs/${serviceName}/versions/${version}`)
    return response
  } catch (error) {
    console.error(`获取版本文档失败: ${serviceName} v${version}`, error)
    throw error
  }
}

/**
 * 获取服务的最新文档
 * @param {string} serviceName 服务名
 * @returns {Promise<string>} OpenAPI JSON 字符串
 */
export async function getLatestDoc(serviceName) {
  try {
    const response = await apiClient.get(`/api/docs/${serviceName}`)
    return response
  } catch (error) {
    console.error(`获取最新文档失败: ${serviceName}`, error)
    throw error
  }
}

/**
 * 对比两个版本的差异
 * @param {string} serviceName 服务名
 * @param {number} v1 版本1
 * @param {number} v2 版本2
 * @returns {Promise<Object>} Diff 结果
 */
export async function diffVersions(serviceName, v1, v2) {
  try {
    const response = await apiClient.get(`/api/docs/${serviceName}/diff`, {
      params: { v1, v2 }
    })
    return response
  } catch (error) {
    console.error(`版本对比失败: ${serviceName} v${v1} vs v${v2}`, error)
    throw error
  }
}

/**
 * 获取指定版本的变更明细
 * @param {string} serviceName 服务名
 * @param {number} version 版本号
 * @returns {Promise<Array>} 变更列表
 */
export async function getVersionChanges(serviceName, version) {
  try {
    const response = await apiClient.get(`/api/docs/${serviceName}/versions/${version}/changes`)
    return Array.isArray(response) ? response : (response.data || [])
  } catch (error) {
    console.error(`获取变更明细失败: ${serviceName} v${version}`, error)
    return []
  }
}

/**
 * 手动同步单个服务
 * @param {string} serviceName 服务名
 * @returns {Promise<Object>} 同步结果
 */
export async function syncService(serviceName) {
  try {
    const response = await apiClient.post(`/api/sync/${serviceName}`)
    return response
  } catch (error) {
    console.error(`同步服务失败: ${serviceName}`, error)
    throw error
  }
}

/**
 * 获取所有服务列表
 * @returns {Promise<Array>} 服务列表
 */
export async function getAllServices() {
  try {
    const response = await apiClient.get('/api/services')
    return Array.isArray(response) ? response : (response.data || [])
  } catch (error) {
    console.error('获取服务列表失败', error)
    return []
  }
}

/**
 * 获取最近的变更记录
 * @param {string} serviceName 服务名（可选）
 * @param {number} page 页码
 * @param {number} size 每页数量
 * @returns {Promise<Array>} 变更记录列表
 */
export async function getRecentChanges(serviceName = null, page = 0, size = 50) {
  try {
    const params = { page, size }
    if (serviceName) {
      params.serviceName = serviceName
    }
    const response = await apiClient.get('/api/changes', { params })
    return Array.isArray(response) ? response : (response.data || [])
  } catch (error) {
    console.error('获取变更记录失败', error)
    return []
  }
}

export default {
  parseServiceName,
  getVersions,
  getDocByVersion,
  getLatestDoc,
  diffVersions,
  getVersionChanges,
  syncService,
  getAllServices,
  getRecentChanges
}

