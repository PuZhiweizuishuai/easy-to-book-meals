/// 券码解析
///
/// 顾客出示的二维码里可能直接是券码（32 位无横线 UUID），也可能是
/// 带网址的领券链接（.../coupon/<code> 或 ?code=<code>），这里统一把券码抠出来。
class CouponCodeExtractor {
  CouponCodeExtractor._();

  /// 32 位十六进制（可能带横线）视作券码
  static final RegExp _uuidPattern = RegExp(
    r'^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$',
  );

  /// 从扫码结果解析券码
  static String? extract(String raw) {
    final value = raw.trim();
    if (value.isEmpty) return null;

    // 1、纯券码
    if (_looksLikeCode(value)) return _stripDashes(value);

    // 2、带 URI 的内容
    final uri = Uri.tryParse(value);
    if (uri != null) {
      // 查询参数：code / couponCode / coupon
      for (final key in const ['code', 'couponCode', 'coupon']) {
        final param = uri.queryParameters[key];
        if (param != null && _looksLikeCode(param)) {
          return _stripDashes(param);
        }
      }
      // 路径最后一段
      final segments = uri.pathSegments;
      if (segments.isNotEmpty) {
        final last = segments.last;
        if (_looksLikeCode(last)) return _stripDashes(last);
        final only = uri.queryParameters['value'];
        if (only != null && only.isNotEmpty) return _stripDashes(only);
      }
    }

    // 3、形如 "coupon:<code>"
    final colonIndex = value.indexOf(':');
    if (colonIndex > 0 && colonIndex < value.length - 1) {
      final tail = value.substring(colonIndex + 1).trim();
      if (_looksLikeCode(tail)) return _stripDashes(tail);
    }

    // 4、兜底：原样返回，由后端判断是否合法
    return value;
  }

  static bool _looksLikeCode(String value) {
    final text = _stripDashes(value);
    return _uuidPattern.hasMatch(_stripDashes(value)) ||
        (text.length == 32 && RegExp(r'^[0-9a-fA-F]{32}$').hasMatch(text));
  }

  static String _stripDashes(String value) => value.replaceAll('-', '');
}
