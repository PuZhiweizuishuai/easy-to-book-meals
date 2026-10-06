/// JSON 反序列化辅助函数
///
/// 后端存在不少「同一字段在不同场景下类型不一致」的情况（金额有时是数字有时是字符串、
/// 主键多数是数字但个别快照字段为空），这里统一做宽容转换，避免运行时类型崩溃。
library;

/// 读取 int，失败返回 [fallback]
int readInt(Object? value, {int fallback = 0}) {
  if (value == null) return fallback;
  if (value is int) return value;
  if (value is num) return value.toInt();
  if (value is String) return int.tryParse(value) ?? fallback;
  return fallback;
}

/// 读取 double，失败返回 [fallback]
double readDouble(Object? value, {double fallback = 0}) {
  if (value == null) return fallback;
  if (value is double) return value;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? fallback;
  return fallback;
}

/// 读取 String，失败返回 [fallback]
String readString(Object? value, {String fallback = ''}) {
  if (value == null) return fallback;
  if (value is String) return value;
  if (value is num || value is bool) return value.toString();
  return fallback;
}

/// 读取 bool，失败返回 [fallback]
bool readBool(Object? value, {bool fallback = false}) {
  if (value == null) return fallback;
  if (value is bool) return value;
  if (value is num) return value != 0;
  if (value is String) {
    return value.toLowerCase() == 'true' || value == '1';
  }
  return fallback;
}

/// 读取可空 int（后端返回 null 时保留 null，便于区分「未设置」与「0」）
int? readIntOrNull(Object? value) {
  if (value == null) return null;
  if (value is int) return value;
  if (value is num) return value.toInt();
  if (value is String) return int.tryParse(value);
  return null;
}

/// 读取可空 double
double? readDoubleOrNull(Object? value) {
  if (value == null) return null;
  if (value is double) return value;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value);
  return null;
}

/// 读取可空 String
String? readStringOrNull(Object? value) {
  if (value == null) return null;
  final text = readString(value);
  return text.isEmpty ? null : text;
}
