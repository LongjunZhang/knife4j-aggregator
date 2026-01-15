/**
 * AI API 服务
 * 
 * 封装 AI 错误解释的 API 调用 (SSE 流式模式)
 * 
 * 注意：后端 (doc-aggregator) 会根据 serviceName + docVersion + path + method
 * 自动从 MongoDB 提取完整的 OpenAPI 定义 (ApiDefinition)，
 * 因此前端只需要传递基本的定位信息即可。
 * 
 * 参数生成功能已改用前端 schemaFakerService.js 实现，不再调用 AI 接口。
 */
import axios from 'axios'

// 创建 axios 实例（仅用于健康检查）
const aiClient = axios.create({
  baseURL: '/',
  timeout: 65000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器
aiClient.interceptors.response.use(
  response => response.data,
  error => {
    console.error('AI API 错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 需要脱敏的 header 名称（小写）
 */
const SENSITIVE_HEADERS = ['authorization', 'cookie', 'set-cookie']

/**
 * 脱敏 headers
 */
function sanitizeHeaders(headers) {
  if (!headers) return {}
  
  const sanitized = {}
  for (const [key, value] of Object.entries(headers)) {
    if (!SENSITIVE_HEADERS.includes(key.toLowerCase())) {
      sanitized[key] = value
    }
  }
  return sanitized
}

function buildExplainErrorRequest(params) {
  return {
    serviceName: params.serviceName || 'unknown',
    docVersion: params.docVersion || '', // 空值表示使用最新版本
    path: params.path,
    method: params.method,
    summary: params.summary,
    // errorId 用于从 doc-aggregator 的 ErrorDetailCentralStore 获取完整错误详情
    errorId: params.errorId || null,
    request: params.request
      ? {
          url: params.request.url,
          contentType: params.request.contentType,
          headers: sanitizeHeaders(params.request.headers),
          query: params.request.query,
          body: params.request.body
        }
      : null,
    response: params.response
      ? {
          status: params.response.status,
          headers: params.response.headers,
          body: params.response.body
        }
      : null
  }
}

/**
 * SSE 方式解释错误（流式返回）
 *
 * 约定：
 * - 后端返回 Content-Type: text/event-stream
 * - 每个事件 data: 为纯文本片段（最终拼接成 JSON 字符串）
 * - 结束标记：data: [DONE]（也兼容 event: done）
 *
 * @param {Object} params 请求参数
 * @param {string} params.serviceName 服务名（必填）
 * @param {string} params.docVersion 文档版本（可选，默认使用最新版本）
 * @param {string} params.path 接口路径（必填）
 * @param {string} params.method HTTP 方法（必填）
 * @param {string} params.summary 接口摘要（可选）
 * @param {string} params.errorId 错误 ID（可选，来自响应头 X-Error-Id，用于获取完整错误详情）
 * @param {Object} params.request 请求信息（包含 url/contentType/headers/query/body）
 * @param {Object} params.response 响应信息（包含 status/headers/body）
 * @param {Object} handlers 回调处理器
 * @param {(chunk: string) => void} handlers.onChunk 每个数据块的回调
 * @param {(fullText: string) => void} handlers.onDone 完成时的回调
 * @param {(err: Error) => void} handlers.onError 错误时的回调
 * @param {AbortSignal} handlers.signal 用于中止请求的信号
 */
export async function explainErrorSse(params, { onChunk, onDone, onError, signal } = {}) {
  const request = buildExplainErrorRequest(params)

  const safeOnChunk = typeof onChunk === 'function' ? onChunk : () => {}
  const safeOnDone = typeof onDone === 'function' ? onDone : () => {}
  const safeOnError = typeof onError === 'function' ? onError : () => {}

  try {
    // 调用 SSE 流式端点
    const resp = await fetch('/api/ai/explain-error/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        'Cache-Control': 'no-cache'
      },
      body: JSON.stringify(request),
      signal
    })

    if (!resp.ok) {
      const text = await resp.text().catch(() => '')
      throw new Error(`SSE 请求失败: ${resp.status} ${resp.statusText}${text ? ' - ' + text : ''}`)
    }

    if (!resp.body) {
      throw new Error('SSE 响应无 body')
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let fullText = ''

    const processEventBlock = (block) => {
      // block: string without trailing \n\n
      const lines = block.split(/\r?\n/)
      let eventType = ''
      const dataLines = []
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trimStart())
        }
      }
      const data = dataLines.join('\n')
      // 处理错误事件
      if (eventType === 'error') {
        safeOnError(new Error(data || 'SSE 服务端错误'))
        return true // done
      }
      if (eventType === 'done' || data === '[DONE]') {
        safeOnDone(fullText)
        return true // done
      }
      if (data) {
        fullText += data
        safeOnChunk(data)
      }
      return false
    }

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE events are separated by blank line
      let idx
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const block = buffer.slice(0, idx).replace(/\r?\n$/, '')
        buffer = buffer.slice(idx + 2)
        if (block.trim() === '') continue
        const isDone = processEventBlock(block)
        if (isDone) {
          try {
            await reader.cancel()
          } catch (_) {}
          return
        }
      }
    }

    // flush remaining (in case server closes without [DONE])
    const tail = buffer.trim()
    if (tail) {
      processEventBlock(tail)
    } else {
      safeOnDone(fullText)
    }
  } catch (err) {
    // AbortError 属于正常中止
    if (err?.name === 'AbortError') {
      return
    }
    console.error('解释错误 SSE 失败:', err)
    safeOnError(err)
  }
}

/**
 * 检查 AI 服务健康状态
 */
export async function checkHealth() {
  try {
    const response = await aiClient.get('/api/ai/health')
    return response
  } catch (error) {
    return { status: 'DOWN', error: error.message }
  }
}

export default {
  explainErrorSse,
  checkHealth
}
