import 'package:intl/intl.dart';

/// 时间格式化工具
///
/// 后端时间戳统一为毫秒。
class TimeFormat {
  TimeFormat._();

  static final DateFormat _dateTime = DateFormat('yyyy-MM-dd HH:mm');
  static final DateFormat _time = DateFormat('HH:mm');
  static final DateFormat _day = DateFormat('MM-dd');
  static final DateFormat _full = DateFormat('yyyy-MM-dd');

  /// 完整日期时间：2026-10-06 12:30
  static String dateTime(int? millis) =>
      millis == null || millis <= 0 ? '--' : _dateTime.format(_toDate(millis));

  /// 仅时:分：12:30
  static String time(int? millis) =>
      millis == null || millis <= 0 ? '--' : _time.format(_toDate(millis));

  /// 月-日：10-06
  static String monthDay(int? millis) =>
      millis == null || millis <= 0 ? '--' : _day.format(_toDate(millis));

  /// 完整日期：2026-10-06
  static String date(int? millis) =>
      millis == null || millis <= 0 ? '--' : _full.format(_toDate(millis));

  /// yyyy-MM-dd（报表请求用）
  static String apiDate(DateTime date) => _full.format(date);

  /// 今天 / 昨天 / 具体时间：列表里最常见的展示形式
  static String smart(int? millis) {
    if (millis == null || millis <= 0) return '--';
    final date = _toDate(millis);
    final now = DateTime.now();
    if (date.year == now.year &&
        date.month == now.month &&
        date.day == now.day) {
      return '今天 ${_time.format(date)}';
    }
    final yesterday = now.subtract(const Duration(days: 1));
    if (date.year == yesterday.year &&
        date.month == yesterday.month &&
        date.day == yesterday.day) {
      return '昨天 ${_time.format(date)}';
    }
    if (date.year == now.year) {
      return '${_day.format(date)} ${_time.format(date)}';
    }
    return _dateTime.format(date);
  }

  /// 等待时长文案：>=60 分钟显示「1小时5分」
  static String waited(int? minutes) {
    if (minutes == null || minutes < 0) return '刚刚';
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return '$minutes 分钟';
    final h = minutes ~/ 60;
    final m = minutes % 60;
    return m == 0 ? '$h 小时' : '$h 小时$m 分';
  }

  static DateTime _toDate(int millis) =>
      DateTime.fromMillisecondsSinceEpoch(millis);
}
