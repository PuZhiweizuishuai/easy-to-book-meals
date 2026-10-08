/// 统一的网络层异常
///
/// 后端的业务错误（库存不足、状态流转非法等）与 HTTP 层错误（超时、断网）都收敛到这里。
/// 登录态类错误（401/403）由 [ApiClient] 的 onUnauthorized 回调统一登出并跳回登录页，
/// 业务层只需展示 [message]，不必再各自判断 [isUnauthorized]。
library;

/// 标记「非后端业务码」的网络类错误
const int networkErrorCode = -1;

/// 统一的网络层异常
class ApiException implements Exception {
  const ApiException(
    this.message, {
    this.code = 0,
    this.httpStatus,
  });

  /// 后端响应体中的业务状态码（200 为成功）
  final int code;

  /// HTTP 状态码（网络层失败时为 null）
  final int? httpStatus;

  /// 可直接展示给用户的错误信息
  final String message;

  /// 登录态失效或权限不足
  bool get isUnauthorized =>
      code == 401 || code == 403 || httpStatus == 401 || httpStatus == 403;

  /// 网络不可用（超时 / 断网 / DNS 失败 / 服务端返回非 JSON）
  bool get isNetworkError => code == networkErrorCode && httpStatus == null;

  @override
  String toString() => 'ApiException(code: $code, message: $message)';
}

/// 构造网络类异常
ApiException networkException(String message) =>
    ApiException(message, code: networkErrorCode);

/// 构造未登录 / 无权限异常
ApiException unauthorizedException(String message) =>
    ApiException(message, code: 401);
