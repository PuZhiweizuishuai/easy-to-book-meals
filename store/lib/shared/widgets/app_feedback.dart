import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// 轻量反馈层
///
/// 统一 SnackBar 样式与成功/失败语义色，避免各处直接调 ScaffoldMessenger。
class AppToast {
  AppToast._();

  static void _show(
    BuildContext context, {
    required String message,
    required Color color,
    IconData? icon,
  }) {
    final messenger = ScaffoldMessenger.maybeOf(context);
    if (messenger == null) return;
    messenger
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Row(
            children: [
              if (icon != null) ...[
                Icon(icon, size: 18, color: Colors.white),
                const SizedBox(width: 8),
              ],
              Expanded(
                child: Text(
                  message,
                  style: const TextStyle(fontSize: 14, height: 1.35),
                ),
              ),
            ],
          ),
          backgroundColor: color,
          duration: const Duration(seconds: 2),
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.fromLTRB(16, 0, 16, 20),
        ),
      );
  }

  /// 成功提示
  static void success(BuildContext context, String message) => _show(
        context,
        message: message,
        color: const Color(0xFF1F9254),
        icon: Icons.check_circle_outline,
      );

  /// 普通提示
  static void info(BuildContext context, String message) => _show(
        context,
        message: message,
        color: const Color(0xFF2B3038),
      );

  /// 错误提示
  static void error(BuildContext context, String message) => _show(
        context,
        message: message,
        color: AppColors.danger,
        icon: Icons.error_outline,
      );

  /// 警告提示
  static void warning(BuildContext context, String message) => _show(
        context,
        message: message,
        color: const Color(0xFFD97706),
        icon: Icons.warning_amber_outlined,
      );
}

/// 信息行：左标签 + 右内容的行布局
class InfoRow extends StatelessWidget {
  const InfoRow({
    super.key,
    required this.label,
    this.value,
    this.valueWidget,
    this.valueColor,
    this.showDivider = true,
    this.padding = const EdgeInsets.symmetric(vertical: 8),
  });

  final String label;
  final String? value;
  final Widget? valueWidget;
  final Color? valueColor;
  final bool showDivider;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: padding,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                width: 76,
                child: Text(
                  label,
                  style: const TextStyle(
                    fontSize: 13.5,
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
              Expanded(
                child: valueWidget ??
                    Text(
                      value ?? '-',
                      textAlign: TextAlign.right,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: valueColor ?? AppColors.textPrimary,
                      ),
                    ),
              ),
            ],
          ),
        ),
        if (showDivider) const Divider(height: 1),
      ],
    );
  }
}
