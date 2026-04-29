// src/utils/http.js

// 假设你使用 Vite，环境变量可以通过 import.meta.env 访问
// 如果在其他环境，请调整获取 baseURL 的方式
const BASE_URL = import.meta.env.VITE_SERVER_API_URL || '/api';

// 检查未授权访问的函数
const checkUnauthorized = function(json) {
  // 在这里实现你的未授权检查逻辑
  // 例如：if (json && json.status === 0) { ... }
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
        // 如果未授权，可以返回一个特定的错误对象，或者直接在这里处理路由跳转
        // 为了保持函数纯粹，这里只返回一个标记
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
  uploadFiles
};