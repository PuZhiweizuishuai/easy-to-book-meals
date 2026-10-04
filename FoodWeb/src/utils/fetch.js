/**
 * 统一网络请求封装
 *
 * 设计要点：
 * - 所有请求自动携带 token（Header + Cookie 双模式），兼容同域和跨域部署
 * - 后端统一返回 ResponseDetails，status === 200 表示成功
 * - 校验 HTTP 状态码：非 2xx（如 500 返回 HTML 错误页）不会再因 .json() 解析失败产生裸异常
 * - 默认 15s 请求超时（AbortController），可按请求覆盖
 * - 401/403 未授权时自动清除本地状态，并携带 redirect 参数跳转登录页
 */

const BASE_URL = import.meta.env.VITE_SERVER_API_URL || '/api'

/** Sa-Token 请求头名称，与后端 sa-token.token-name 配置一致（application.yml: token-name: Token） */
const TOKEN_HEADER = 'Token'

/** 登录页路径 */
const LOGIN_PATH = '/login'

/** 默认请求超时时间（毫秒） */
const DEFAULT_TIMEOUT = 15000

/** 文件上传超时时间（毫秒），大文件适当放宽 */
const UPLOAD_TIMEOUT = 30000

/**
 * 判断请求是否成功
 * 后端统一返回 ResponseDetails，status === 200 表示成功
 * @param {object} json - 后端返回的响应体
 * @returns {boolean}
 */
export const isSuccess = (json) => {
  return json && json.status === 200
}

/**
 * 解析后端分页数据（兼容 Spring Data Page DTO 序列化结构）
 * VIA_DTO 模式下元数据位于 data.page 下，同时兼容旧版平铺结构
 * @param {object} data - 后端返回的 data 字段（Page 序列化结果）
 * @returns {{content: Array, page: number, totalPages: number, totalElements: number}}
 */
export const parsePage = (data) => {
  if (!data) {
    return { content: [], page: 1, totalPages: 1, totalElements: 0 }
  }
  const p = data.page || {}
  return {
    content: data.content || [],
    page: (p.number ?? 0) + 1,
    totalPages: p.totalPages ?? data.totalPages ?? 1,
    totalElements: p.totalElements ?? data.totalElements ?? 0
  }
}

/**
 * 检查是否为未授权响应（后端可能用响应体 status 表达 401/403）
 * @param {object} json - 后端返回的响应体
 * @returns {boolean}
 */
const isUnauthorized = (json) => {
  return json && (json.status === 401 || json.status === 403)
}

/**
 * 处理未授权：清除本地认证状态，跳转登录页并携带 redirect 参数，
 * 登录成功后可回到原页面（Login.vue 已支持读取该参数）
 */
function handleUnauthorized() {
  localStorage.removeItem('token')
  localStorage.removeItem('user_info')
  if (window.location.pathname !== LOGIN_PATH) {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `${LOGIN_PATH}?redirect=${redirect}`
  }
}

/**
 * 构建请求 Headers
 * 自动附加 token 和 Content-Type
 * @param {object} extraHeaders - 额外的请求头
 * @returns {object}
 */
function buildHeaders(extraHeaders = {}) {
  const headers = {
    'Content-Type': 'application/json; charset=UTF-8',
    ...extraHeaders,
  }
  // 从 localStorage 读取 token，通过 Header 携带（兼容跨域场景）
  const token = localStorage.getItem('token')
  if (token) {
    headers[TOKEN_HEADER] = token
  }
  return headers
}

/**
 * 解析响应体：JSON 响应正常解析；非 JSON（如 Nginx/网关 500 HTML 页）返回带 raw 文本的对象，
 * 避免直接调用 response.json() 抛出无法识别的语法错误
 * @param {Response} response
 * @returns {Promise<object|null>}
 */
async function parseResponseBody(response) {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    try {
      return await response.json()
    } catch {
      return null
    }
  }
  const text = await response.text().catch(() => '')
  if (!text) return null
  // 个别网关可能以 text/plain 返回 JSON，兜底再解析一次
  try {
    return JSON.parse(text)
  } catch {
    return { raw: text }
  }
}

/**
 * 归一化网络层错误
 * - TIMEOUT：超时（AbortController 主动中断）
 * - NETWORK_ERROR：断网/DNS/CORS 等 fetch 层失败
 * - ABORTED：调用方通过外部 signal 主动取消
 * 已归一化的业务/HTTP 错误（带 type）原样抛出
 * @param {Error|object} err
 * @param {boolean} timedOut - 是否由本封装的超时计时器触发中断
 */
function normalizeError(err, timedOut) {
  // .then 中已构造的结构化错误，直接透传
  if (err && typeof err === 'object' && err.type) {
    throw err
  }
  if (err && err.name === 'AbortError') {
    if (timedOut) {
      throw { type: 'TIMEOUT', message: '请求超时，请稍后重试' }
    }
    throw { type: 'ABORTED', message: '请求已取消' }
  }
  throw { type: 'NETWORK_ERROR', message: '网络异常，请检查网络连接', cause: err }
}

/**
 * 通用请求函数
 * @param {string} url - 请求地址（不含 BASE_URL 前缀）
 * @param {object} options - fetch 配置项
 * @param {number} [options.timeout=15000] - 超时时间（毫秒），传 0 表示不超时
 * @param {AbortSignal} [options.signal] - 调用方外部中断信号
 * @returns {Promise<object>} - 后端返回的响应体；失败时 reject 结构化错误
 *   （{ type: 'HTTP_ERROR'|'TIMEOUT'|'NETWORK_ERROR'|'UNAUTHORIZED'|'ABORTED', ... }）
 */
const request = (url, options = {}) => {
  const { timeout = DEFAULT_TIMEOUT, signal: externalSignal, ...fetchOptions } = options

  const controller = new AbortController()
  let timedOut = false
  const timer = timeout > 0
    ? setTimeout(() => {
        timedOut = true
        controller.abort()
      }, timeout)
    : null

  // 外部 signal 触发时联动中断
  if (externalSignal) {
    if (externalSignal.aborted) {
      controller.abort()
    } else {
      externalSignal.addEventListener('abort', () => controller.abort(), { once: true })
    }
  }

  return fetch(`${BASE_URL}${url}`, {
    credentials: 'include',
    ...fetchOptions,
    signal: controller.signal,
  })
    .then(async (response) => {
      const json = await parseResponseBody(response)

      // HTTP 401/403（Sa-Token 拦截器等直接返回的状态码）或响应体携带未授权状态
      if (response.status === 401 || response.status === 403 || isUnauthorized(json)) {
        handleUnauthorized()
        return Promise.reject({ type: 'UNAUTHORIZED', status: response.status, data: json })
      }

      // 其他非 2xx：服务端业务错误/网关错误，统一为结构化错误
      if (!response.ok) {
        return Promise.reject({
          type: 'HTTP_ERROR',
          status: response.status,
          message: (json && json.message) || `请求失败（HTTP ${response.status}）`,
          data: json
        })
      }

      return json
    })
    .catch((err) => normalizeError(err, timedOut))
    .finally(() => {
      if (timer) clearTimeout(timer)
    })
}

/**
 * GET 请求
 * @param {string} url - 请求地址
 * @param {object} [options] - 额外配置（timeout/signal 等）
 * @returns {Promise<object>}
 */
export const httpGet = (url, options) => {
  return request(url, {
    method: 'GET',
    headers: buildHeaders(),
    ...options,
  })
}

/**
 * POST 请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求体数据
 * @param {object} [options] - 额外配置（timeout/signal 等）
 * @returns {Promise<object>}
 */
export const httpPost = (url, data, options) => {
  return request(url, {
    method: 'POST',
    headers: buildHeaders(),
    body: JSON.stringify(data),
    ...options,
  })
}

/**
 * 文件上传请求
 * 浏览器会自动设置 Content-Type 为 multipart/form-data，无需手动指定
 * @param {string} url - 请求地址
 * @param {FormData} form - 表单数据
 * @returns {Promise<object>}
 */
export const uploadFiles = (url, form) => {
  return request(url, {
    method: 'POST',
    headers: buildHeaders({ 'Content-Type': undefined }),
    body: form,
    timeout: UPLOAD_TIMEOUT,
  })
}

export default {
  httpGet,
  httpPost,
  uploadFiles,
  isSuccess
}
