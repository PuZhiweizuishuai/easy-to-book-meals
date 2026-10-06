import 'package:intl/intl.dart';

/// 金额格式化工具
///
/// 后端金额以 BigDecimal 序列化为数字传输，统一在这里做展示层收敛，
/// 避免不同页面出现 12.5 / 12.50 / ￥12.5 三种写法。
class MoneyFormat {
  MoneyFormat._();

  static final NumberFormat _yuan = NumberFormat.currency(
    locale: 'zh_CN',
    symbol: '¥',
    decimalDigits: 2,
  );

  /// 两位小数的金额，如 ¥12.50
  static String yuan(double value) => _yuan.format(value);

  /// 不带货币符号的金额，如 12.50
  static String plain(double value) => value.toStringAsFixed(2);

  /// 智能金额：整数位数较多或恰好为整数时保留两位，其余保留两位（保持账单一致性）
  @Deprecated('统一使用 yuan()/plain()，保留仅用于兼容')
  static String smart(double value) => plain(value);

  /// 数量展示：称重菜保留 3 位小数并去掉多余的 0，计件菜显示整数
  static String quantity(double value) {
    if (value == value.roundToDouble()) {
      return value.toStringAsFixed(0);
    }
    return NumberFormat('0.###').format(value);
  }

  /// 大额数字紧凑展示（看板用），如 1.2万
  static String compact(double value) {
    if (value >= 10000) {
      return '${NumberFormat('0.0#').format(value / 10000)}万';
    }
    return plain(value);
  }
}
