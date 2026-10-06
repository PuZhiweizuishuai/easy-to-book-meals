import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';

/// 订单状态 → 配色
class OrderStatusStyle {
  const OrderStatusStyle._();

  static Color colorOf(OrderStatus? status) => switch (status) {
        OrderStatus.pending => AppColors.warning,
        OrderStatus.confirmed => AppColors.info,
        OrderStatus.cooking => AppColors.primary,
        OrderStatus.ready => const Color(0xFF7C4DFF),
        OrderStatus.completed => AppColors.success,
        OrderStatus.cancelled => AppColors.disabled,
        _ => AppColors.textSecondary,
      };

  static Color backgroundOf(OrderStatus? status) => switch (status) {
        OrderStatus.pending => AppColors.warningLight,
        OrderStatus.confirmed => AppColors.infoLight,
        OrderStatus.cooking => AppColors.primaryLight,
        OrderStatus.ready => const Color(0xFFF1ECFF),
        OrderStatus.completed => AppColors.successLight,
        OrderStatus.cancelled => const Color(0xFFF2F4F8),
        _ => const Color(0xFFF2F4F8),
      };

  static String labelOf(OrderStatus? status) => status?.label ?? '未知状态';
}

/// 订单类型 → 图标
IconData orderTypeIcon(OrderType type) => switch (type) {
      OrderType.dineIn => Icons.restaurant_outlined,
      OrderType.takeout => Icons.delivery_dining_outlined,
      OrderType.online => Icons.shopping_bag_outlined,
    };

/// 支付状态 → 标签配色
class PayStatusStyle {
  const PayStatusStyle._();

  static (Color, Color) style(PayStatus status) => switch (status) {
        PayStatus.paid => (AppColors.successLight, AppColors.success),
        PayStatus.unpaid => (AppColors.warningLight, AppColors.warning),
      };

  static String label(PayStatus status) => status.label;
}
