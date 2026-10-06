import '../config/app_config.dart';

/// 服务器地址工具
///
/// 用户输入的地址形态很随意（`192.168.1.5:8081`、`http://host/`、带端口等），
/// 这里统一规范化：去掉末尾斜杠、缺省协议补 http://，并拼出接口根地址。
class ServerAddress {
  ServerAddress._();

  /// 规范化：去空白、去末尾 '/'、缺省协议补 http://
  static String normalize(String raw) {
    var value = raw.trim().replaceAll(RegExp(r'\s+'), '');
    while (value.endsWith('/')) {
      value = value.substring(0, value.length - 1);
    }
    if (value.isEmpty) return value;
    final lower = value.toLowerCase();
    if (!lower.startsWith('http://') && !lower.startsWith('https://')) {
      value = 'http://$value';
    }
    return value;
  }

  /// 拼接接口根地址（追加 /api）
  static String apiBase(String raw) {
    final normalized = normalize(raw);
    if (normalized.isEmpty) return AppConfig.apiPrefix;
    return '$normalized${AppConfig.apiPrefix}';
  }

  /// 基础校验：形如 host[:port] 或完整 URL
  static String? validate(String raw) {
    final value = raw.trim();
    if (value.isEmpty) return '请输入服务器地址';
    final normalized = normalize(value);
    final uri = Uri.tryParse(normalized);
    if (uri == null || uri.host.isEmpty) {
      return '服务器地址格式不正确';
    }
    if (!normalized.startsWith('http://') && !normalized.startsWith('https://')) {
      return '服务器地址必须以 http:// 或 https:// 开头';
    }
    return null;
  }
}
