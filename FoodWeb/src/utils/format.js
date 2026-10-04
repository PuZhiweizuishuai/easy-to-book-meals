/**
 * 格式化时间戳为可读日期字符串
 * @param {number|string} timestamp - 时间戳（毫秒）
 * @returns {string} 格式化后的日期字符串，格式：YYYY/MM/DD HH:mm
 */
export const formatDate = (timestamp) => {
  if (!timestamp) return '-';
  const date = new Date(timestamp);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

/**
 * 将 SKU 规格名（后端存储为 JSON 字符串，数组或对象）格式化为展示文本
 * 例如 '["微辣","大份"]' → '微辣/大份'；'{"辣度":"微辣"}' → '微辣'
 * @param {string|string[]|object} specNames
 * @returns {string} 解析失败时回退为原始字符串，空值返回 ''
 */
export const formatSpecNames = (specNames) => {
  if (!specNames) return '';
  try {
    const parsed = typeof specNames === 'string' ? JSON.parse(specNames) : specNames;
    if (Array.isArray(parsed)) return parsed.join('/');
    if (parsed && typeof parsed === 'object') return Object.values(parsed).join('/');
  } catch { /* 忽略解析失败，走兜底 */ }
  return String(specNames || '');
};
