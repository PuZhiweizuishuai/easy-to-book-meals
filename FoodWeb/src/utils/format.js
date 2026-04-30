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