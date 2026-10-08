import 'dart:async';

import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// 轻量反馈层
///
/// 统一提示样式与成功/失败语义色。提示用 Overlay 挂在状态栏下方：
/// SnackBar 在底部会被底部导航栏和结算按钮遮住，顶部浮层不遮挡操作、也不挤动页面布局。
class AppToast {
  AppToast._();

  static OverlayEntry? _current;

  static void _show(
    BuildContext context, {
    required String message,
    required Color color,
    required IconData icon,
  }) {
    final overlay = Overlay.maybeOf(context, rootOverlay: true);
    if (overlay == null) return;

    // 新消息顶掉上一条，避免连续操作时排队堆叠
    _current?.remove();
    _current = null;

    late final OverlayEntry entry;
    entry = OverlayEntry(
      builder: (_) => _TopToast(
        message: message,
        color: color,
        icon: icon,
        onDismissed: () {
          // 已被新消息替换时旧条目自己会随 State 销毁，不再重复 remove
          if (_current != entry) return;
          _current = null;
          entry.remove();
        },
      ),
    );
    _current = entry;
    overlay.insert(entry);
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
        icon: Icons.info_outline,
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

/// 顶部提示条：滑入 + 淡入，停留片刻后自动收起，点击可提前关闭
class _TopToast extends StatefulWidget {
  const _TopToast({
    required this.message,
    required this.color,
    required this.icon,
    required this.onDismissed,
  });

  final String message;
  final Color color;
  final IconData icon;
  final VoidCallback onDismissed;

  @override
  State<_TopToast> createState() => _TopToastState();
}

class _TopToastState extends State<_TopToast>
    with SingleTickerProviderStateMixin {
  static const Duration _visibleFor = Duration(milliseconds: 2200);
  static const double _maxWidth = 420;

  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 200),
    reverseDuration: const Duration(milliseconds: 160),
  );
  late final Animation<double> _animation = CurvedAnimation(
    parent: _controller,
    curve: Curves.easeOutCubic,
  );
  Timer? _hideTimer;

  @override
  void initState() {
    super.initState();
    _controller.forward();
    _hideTimer = Timer(_visibleFor, _dismiss);
  }

  Future<void> _dismiss() async {
    _hideTimer?.cancel();
    _hideTimer = null;
    await _controller.reverse();
    if (!mounted) return;
    widget.onDismissed();
  }

  @override
  void dispose() {
    _hideTimer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    final radius = BorderRadius.circular(10);

    return Stack(
      children: [
        Positioned(
          top: topInset + 8,
          left: 16,
          right: 16,
          child: Align(
            alignment: Alignment.topCenter,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: _maxWidth),
              child: FadeTransition(
                opacity: _animation,
                child: SlideTransition(
                  position: Tween<Offset>(
                    begin: const Offset(0, -0.35),
                    end: Offset.zero,
                  ).animate(_animation),
                  child: Material(
                    color: widget.color,
                    borderRadius: radius,
                    elevation: 3,
                    child: InkWell(
                      borderRadius: radius,
                      onTap: _dismiss,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 11,
                        ),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(widget.icon, size: 18, color: Colors.white),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                widget.message,
                                style: const TextStyle(
                                  fontSize: 14,
                                  height: 1.35,
                                  fontWeight: FontWeight.w500,
                                  color: Colors.white,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
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
