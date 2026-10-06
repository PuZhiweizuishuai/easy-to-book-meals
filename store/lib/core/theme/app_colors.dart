import 'package:flutter/material.dart';

/// 应用配色
///
/// 移动端以橙色（餐饮行业色）为主色，语义色单独定义，
/// 保证状态色（进行中/完成/告警）在全 App 语义一致。
class AppColors {
  const AppColors._();

  /// 主色
  static const Color primary = Color(0xFFF2702F);

  /// 主色深色（用于按下态、渐变收尾）
  static const Color primaryDark = Color(0xFFD8500F);

  /// 主色浅色（用于 tag / 背景）
  static const Color primaryLight = Color(0xFFFFF1E8);

  /// 辅助色
  static const Color accent = Color(0xFF2F80ED);

  /// 背景
  static const Color background = Color(0xFFF5F6F8);

  /// 卡片背景
  static const Color surface = Colors.white;

  /// 分隔线
  static const Color divider = Color(0xFFEDEFF3);

  /// 主文字
  static const Color textPrimary = Color(0xFF1F2329);

  /// 次文字
  static const Color textSecondary = Color(0xFF7A828E);

  /// 弱文字
  static const Color textHint = Color(0xFFA6ADB8);

  /// 成功（已完成 / 已上架）
  static const Color success = Color(0xFF18A058);

  /// 成功浅色背景
  static const Color successLight = Color(0xFFE8F7EF);

  /// 进行中（制作中 / 待处理）
  static const Color warning = Color(0xFFF5A623);

  /// 进行中浅色背景
  static const Color warningLight = Color(0xFFFFF5E3);

  /// 危险（拒单 / 取消 / 下架）
  static const Color danger = Color(0xFFE54545);

  /// 危险浅色背景
  static const Color dangerLight = Color(0xFFFDECEC);

  /// 信息（已接单 / 空闲）
  static const Color info = Color(0xFF2F80ED);

  /// 信息浅色背景
  static const Color infoLight = Color(0xFFE9F1FE);

  /// 已售罄 / 停用灰
  static const Color disabled = Color(0xFF9AA1AC);

  /// 图表：橙色
  static const Color chartPrimary = Color(0xFFF2702F);

  /// 图表：蓝色
  static const Color chartAccent = Color(0xFF2F80ED);

  /// 图表：绿色
  static const Color chartGreen = Color(0xFF18A058);
}
