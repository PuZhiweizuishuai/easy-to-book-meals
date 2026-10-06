import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// 状态标签
///
/// 订单支付方式/订单状态/餐桌状态/菜品上下架等都复用这一个组件，
/// 只传文案与背景+前景色，保证全 App 的标签视觉一致。
class StatusChip extends StatelessWidget {
  const StatusChip({
    super.key,
    required this.label,
    this.backgroundColor,
    this.foregroundColor,
    this.icon,
    this.dense = false,
    this.radius = 8,
  });

  final String label;
  final Color? backgroundColor;
  final Color? foregroundColor;
  final IconData? icon;

  /// 紧凑模式（更小的内边距与字号）
  final bool dense;

  /// 圆角
  final double radius;

  @override
  Widget build(BuildContext context) {
    final fg = foregroundColor ?? AppColors.primary;
    final bg = backgroundColor ?? AppColors.primaryLight;
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: dense ? 6 : 9,
        vertical: dense ? 1.5 : 3.5,
      ),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(radius),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: dense ? 12 : 14, color: fg),
            SizedBox(width: dense ? 2 : 4),
          ],
          Text(
            label,
            style: TextStyle(
              fontSize: dense ? 11 : 12.5,
              height: 1.2,
              fontWeight: FontWeight.w600,
              color: fg,
            ),
          ),
        ],
      ),
    );
  }
}
