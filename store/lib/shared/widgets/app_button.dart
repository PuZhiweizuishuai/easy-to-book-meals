import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// 通用主按钮
///
/// 支持加载态（加载中自动禁用点击并显示转圈），避免各页面重复实现。
class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    this.onPressed,
    this.busy = false,
    this.expand = true,
    this.height = 46,
    this.icon,
    this.backgroundColor,
    this.foregroundColor,
  });

  /// 按钮文案
  final String label;

  /// 点击回调（busy 或 null 时自动禁用）
  final VoidCallback? onPressed;

  /// 是否处于加载中
  final bool busy;

  /// 是否撑满父容器宽度
  final bool expand;

  /// 高度
  final double height;

  /// 前缀图标
  final IconData? icon;

  final Color? backgroundColor;
  final Color? foregroundColor;

  @override
  Widget build(BuildContext context) {
    final child = busy
        ? SizedBox(
            width: 18,
            height: 18,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: foregroundColor ?? Colors.white,
            ),
          )
        : Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 18, color: foregroundColor ?? Colors.white),
                const SizedBox(width: 6),
              ],
              Flexible(
                child: Text(
                  label,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: foregroundColor ?? Colors.white,
                  ),
                ),
              ),
            ],
          );

    final disabled = busy || onPressed == null;
    final bg = backgroundColor ?? AppColors.primary;

    return SizedBox(
      width: expand ? double.infinity : null,
      height: height,
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: disabled ? AppColors.disabled : bg,
          foregroundColor: foregroundColor ?? Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppThemeRadius.md),
          ),
          padding: EdgeInsets.zero,
        ),
        onPressed: disabled ? null : onPressed,
        child: child,
      ),
    );
  }
}

/// 圆角半径常量（与 AppTheme 保持一致，便于在无 context 的场景下引用）
class AppThemeRadius {
  const AppThemeRadius._();

  static const double sm = 10;
  static const double md = 14;
  static const double lg = 20;
}

/// 次要按钮（描边样式）
class SecondaryButton extends StatelessWidget {
  const SecondaryButton({
    super.key,
    required this.label,
    this.onPressed,
    this.expand = false,
    this.icon,
    this.color,
    this.height = 46,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool expand;
  final IconData? icon;
  final Color? color;
  final double height;

  @override
  Widget build(BuildContext context) {
    final effectiveColor = color ?? AppColors.primary;
    return SizedBox(
      width: expand ? double.infinity : null,
      height: height,
      child: OutlinedButton(
        style: OutlinedButton.styleFrom(
          foregroundColor: effectiveColor,
          side: BorderSide(color: effectiveColor.withValues(alpha: 0.6)),
          padding: const EdgeInsets.symmetric(horizontal: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppThemeRadius.md),
          ),
        ),
        onPressed: onPressed,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 18),
              const SizedBox(width: 6),
            ],
            Flexible(
              child: Text(
                label,
                overflow: TextOverflow.ellipsis,
                style:
                    const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
