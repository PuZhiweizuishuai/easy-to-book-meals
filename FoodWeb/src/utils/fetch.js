// src/utils/http.js

// 假设你使用 Vite，环境变量可以通过 import.meta.env 访问
// 如果在其他环境，请调整获取 baseURL 的方式
const BASE_URL = import.meta.env.VITE_SERVER_API_URL || '/api';

/**
 * 判断请求是否成功
 * 后端统一返回 ResponseDetails，status === 200 表示成功
 * @param {object} json - 后端返回的响应体
 * @returns {boolean}
 */
export const isSuccess = (json) => {
  return json && json.status === 200;
};

/**
 * 检查未授权访问
 * 后端未授权时可能返回 401 或通过 Sa-Token 重定向
 * @param {object} json - 后端返回的响应体
 * @returns {boolean}
 */
const checkUnauthorized = function(json) {
  if (json && (json.status === 401 || json.status === 403)) {
    return true;
  }
  return false;
};

/**
 * 通用的请求处理函数，避免代码重复
 * @param {string} url - 请求地址
 * @param {object} options - fetch 的配置项
 * @returns {Promise} - 返回一个 Promise 对象
 */
const request = (url, options) => {
  return fetch(`${BASE_URL}${url}`, {
    credentials: 'include',
    ...options,
  })
    .then((response) => response.json())
    .then((json) => {
      if (checkUnauthorized(json)) {
        // 未授权时清除本地 token 并跳转到登录页
        localStorage.removeItem('token')
        // 避免重复跳转：如果当前不在登录页，则跳转
        if (window.location.pathname !== '/sys-login') {
          window.location.href = '/sys-login'
        }
        return Promise.reject({ type: 'UNAUTHORIZED', data: json });
      }
      return json;
    });
};

// 导出独立的工具函数
export const httpGet = (url) => {
  return request(url, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
    },
  });
};

export const httpPost = (url, data) => {
  return request(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
    },
    body: JSON.stringify(data),
  });
};

export const uploadFiles = (url, form) => {
  return request(url, {
    method: 'POST',
    // 上传文件时，浏览器会自动设置 Content-Type 为 multipart/form-data
    // 所以这里不需要手动设置 headers
    body: form,
  });
};

// 导出一个默认对象，方便按需导入
export default {
  httpGet,
  httpPost,
  uploadFiles,
  isSuccess
};