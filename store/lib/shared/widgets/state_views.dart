import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../core/theme/app_colors.dart';

/// 空状态占位
///
/// 列表无数据时的统一表现（SVG 插画 + 文案 + 可选操作按钮），
/// 避免每个页面各自用 Center + Column 拼一套。
class EmptyView extends StatelessWidget {
  const EmptyView({
    super.key,
    this.title = '暂无数据',
    this.subtitle,
    this.icon = Icons.inbox_outlined,
    this.actionText,
    this.onAction,
    this.compact = false,
  });

  /// 主文案
  final String title;

  /// 辅助说明
  final String? subtitle;

  /// 兜底图标（不使用插画时）
  final IconData icon;

  final String? actionText;
  final VoidCallback? onAction;

  /// 紧凑模式（用于列表内嵌）
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final illustration = SizedBox(
      height: compact ? 72 : 130,
      child: SvgPicture.asset(
        'assets/images/empty_box.svg',
        fit: BoxFit.contain,
      ),
    );

    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(
          horizontal: 32,
          vertical: compact ? 16 : 48,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            illustration,
            SizedBox(height: compact ? 10 : 18),
            Text(
              title,
              style: TextStyle(
                fontSize: compact ? 14 : 16,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
            if (subtitle != null && subtitle!.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(
                subtitle!,
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textHint,
                  height: 1.5,
                ),
                textAlign: TextAlign.center,
              ),
            ],
            if (actionText != null) ...[
              SizedBox(height: compact ? 12 : 20),
              OutlinedButton.icon(
                onPressed: onAction,
                icon: const Icon(Icons.refresh_outlined, size: 18),
                label: Text(actionText!),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// 加载中占位
class LoadingView extends StatelessWidget {
  const LoadingView({super.key, this.message, this.height});

  final String? message;
  final double? height;

  @override
  Widget build(BuildContext context) {
    final child = Column(
      mainAxisAlignment: MainAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: [
        const CircularProgressIndicator(strokeWidth: 2.4),
        if (message != null) ...[
          const SizedBox(height: 12),
          Text(
            message!,
            style: const TextStyle(fontSize: 13, color: AppColors.textHint),
          ),
        ],
      ],
    );
    return SizedBox(height: height, width: double.infinity, child: Center(child: child));
  }
}

/// 错误状态占位
///
/// 统一展示错误信息并提供「重试」，网络异常时自动切换为断网插画。
class ErrorView extends StatelessWidget {
  const ErrorView({
    super.key,
    required this.message,
    this.onRetry,
    this.icon,
    this.retryText = '重试',
  });

  final String message;
  final VoidCallback? onRetry;
  final IconData? icon;
  final String retryText;

  @override
  Widget build(BuildContext context) {
    final useIllustration = icon == null;
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            useIllustration
                ? SizedBox(
                    height: 110,
                    child: SvgPicture.asset(
                      'assets/images/no_network.svg',
                      fit: BoxFit.contain,
                    ),
                  )
                : Icon(icon, size: 56, color: AppColors.danger),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 14,
                color: AppColors.textSecondary,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 18),
            if (onRetry != null)
              OutlinedButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh, size: 18),
                label: Text(retryText),
              ),
          ],
        ),
      ),
    );
  }
}

/// 列表底部「加载更多」占位条
class LoadingMoreFooter extends StatelessWidget {
  const LoadingMoreFooter({
    super.key,
    required this.hasMore,
    this.onRetry,
    this.error,
  });

  final bool hasMore;
  final VoidCallback? onRetry;
  final String? error;

  @override
  Widget build(BuildContext context) {
    if (error != null) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Center(
          child: TextButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.error_outline, size: 16),
            label: Text('$error，点击重试'),
          ),
        ),
      );
    }
    if (!hasMore) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 14),
        child: Center(
          child: Text(
            '没有更多了',
            style: TextStyle(fontSize: 12, color: AppColors.textHint),
          ),
        ),
      );
    }
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 12),
      child: Center(
        child: SizedBox(
          width: 20,
          height: 20,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      ),
    );
  }
}
