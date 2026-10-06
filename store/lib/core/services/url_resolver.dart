import '../config/app_config.dart';

/// 图片地址解析器
///
/// 后端的封面图字段可能返回相对路径（如 `/static/img/a.png`）或完整 URL，
/// 移动端的 [CachedNetworkImage] 只能加载完整地址，这里统一补齐 origin。
///
/// 由于 Objets 不持有 Ref（成千上万的小组件里穿 ref 太重），
/// 用全局单例 + 入口初始化：swapino [update] 在服务器地址变更时调用。
class AppUrlResolver {
  AppUrlResolver._();

  static String _baseUrl = '';

  /// 当前后端根地址（不含 /api）
  static String get baseUrl => _baseUrl;

  /// 由 [PreferencesService] 同步：服务器地址配置或变化时调用
  static void update(String serverAddress) {
    var value = serverAddress.trim();
    while (value.endsWith('/')) {
      value = value.substring(0, value.length - 1);
    }
    if (value.isNotEmpty &&
        !value.toLowerCase().startsWith('http://') &&
        !value.toLowerCase().startsWith('https://')) {
      value = 'http://$value';
    }
    _baseUrl = value;
  }

  /// 规范化图片地址；无法加载时返回 null（调用方展示占位图）
  static String? resolve(String? raw) {
    if (raw == null) return null;
    final value = raw.trim();
    if (value.isEmpty) return null;
    if (value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }
    if (value.startsWith('//')) {
      return value.startsWith('//') ? 'https:$value' : value;
    }
    // 本地 scheme（blob:/file:）不是可加载的网络地址
    if (value.contains(':') && !value.startsWith('/')) return null;
    if (_baseUrl.isEmpty) return null;
    final path = value.startsWith('/') ? value : '/$value';
    return '$_baseUrl$path';
  }
}

/// 默认端口提示（供配置页输入框使用）
const String defaultServerHint = AppConfig.defaultPortHint;
