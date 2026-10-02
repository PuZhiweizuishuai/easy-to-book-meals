/**
 * 统一网络请求封装
 *
 * 设计要点：
 * - 所有请求自动携带 token（Header + Cookie 双模式），兼容同域和跨域部署
 * - 后端统一返回 ResponseDetails，status === 200 表示成功
 * - 401/403 未授权时自动清除本地状态并跳转登录页
 */

const BASE_URL = import.meta.env.VITE_SERVER_API_URL || '/api'

/** Sa-Token 请求头名称，与后端 sa-token.token-name 配置一致（application.yml: token-name: Token） */
const TOKEN_HEADER = 'Token'

/** 登录页路径 */
const LOGIN_PATH = '/login'

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
 * 检查是否为未授权响应
 * @param {object} json - 后端返回的响应体
 * @returns {boolean}
 */
const isUnauthorized = (json) => {
  return json && (json.status === 401 || json.status === 403)
}

/**
 * 处理未授权：清除本地认证状态，跳转登录页
 */
function handleUnauthorized() {
  localStorage.removeItem('token')
  localStorage.removeItem('user_info')
  if (window.location.pathname !== LOGIN_PATH) {
    window.location.href = LOGIN_PATH
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
 * 通用请求函数
 * @param {string} url - 请求地址（不含 BASE_URL 前缀）
 * @param {object} options - fetch 配置项
 * @returns {Promise<object>} - 后端返回的响应体
 */
const request = (url, options) => {
  return fetch(`${BASE_URL}${url}`, {
    credentials: 'include',
    ...options,
  })
    .then((response) => response.json())
    .then((json) => {
      if (isUnauthorized(json)) {
        handleUnauthorized()
        return Promise.reject({ type: 'UNAUTHORIZED', data: json })
      }
      return json
    })
}

/**
 * GET 请求
 * @param {string} url - 请求地址
 * @returns {Promise<object>}
 */
export const httpGet = (url) => {
  return request(url, {
    method: 'GET',
    headers: buildHeaders(),
  })
}

/**
 * POST 请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求体数据
 * @returns {Promise<object>}
 */
export const httpPost = (url, data) => {
  return request(url, {
    method: 'POST',
    headers: buildHeaders(),
    body: JSON.stringify(data),
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
  })
}

export default {
  httpGet,
  httpPost,
  uploadFiles,
  isSuccess
}
