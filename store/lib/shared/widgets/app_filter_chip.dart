import 'package:flutter/material.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/theme/app_colors.dart';

/// 可选中的筛选芯片（可复用）
///
/// 不用 Material 的 [FilterChip]：M3 默认配色下未选中态的文字色与背景色过于接近，
/// 浅色背景上几乎看不清。这里显式指定两种状态的背景 / 文字 / 描边，
/// 与订单筛选、餐桌筛选等处的芯片保持同一套视觉。
class AppFilterChip extends StatelessWidget {
  const AppFilterChip({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
    this.color,
    this.showCheck = true,
    this.height = 34,
  });

  /// 芯片文案
  final String label;

  /// 是否选中
  final bool selected;

  /// 点击回调
  final VoidCallback onTap;

  /// 主题色（选中态使用），默认主色
  final Color? color;

  /// 选中时是否显示对勾
  final bool showCheck;

  /// 高度
  final double height;

  @override
  Widget build(BuildContext context) {
    final base = color ?? AppColors.primary;
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 140),
        height: height,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        decoration: BoxDecoration(
          color: selected ? base.withValues(alpha: 0.12) : const Color(0xFFF2F4F8),
          borderRadius: BorderRadius.circular(height / 2),
          border: Border.all(
            color: selected ? base : const Color(0xFFE3E6EC),
            width: 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (selected && showCheck) ...[
              Icon(Symbols.check, size: 14, color: base, weight: 700),
              const SizedBox(width: 3),
            ],
            Text(
              label,
              style: TextStyle(
                fontSize: 13,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
                // 未选中用次文字色（#7A828E）而非 M3 默认的浅灰，保证在浅灰底上可读
                color: selected ? base : AppColors.textPrimary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
