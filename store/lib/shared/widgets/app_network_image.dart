import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../../core/services/url_resolver.dart';
import '../../core/theme/app_colors.dart';

/// 网络图片组件
///
/// 统一处理：占位图、加载失败兜底、圆角裁剪。
/// 菜品封面可能为空字符串，空 URL 时直接展示占位，避免 CachedNetworkImage 报错。
class AppNetworkImage extends StatelessWidget {
  const AppNetworkImage({
    super.key,
    this.url,
    this.size = 52,
    this.width,
    this.height,
    this.radius = 8,
    this.placeholderIcon = Icons.fastfood,
    this.fit = BoxFit.cover,
  });

  /// 图片地址（为空或非法时展示占位）
  final String? url;

  /// 正方形边长（不指定 width/height 时使用）
  final double size;

  final double? width;
  final double? height;

  /// 圆角
  final double radius;

  /// 占位图标
  final IconData placeholderIcon;

  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    // 图片可能为相对路径，统一交由 AppUrlResolver 补齐 origin
    final resolved = AppUrlResolver.resolve(url);
    final effectiveWidth = width ?? size;
    final effectiveHeight = height ?? size;

    Widget buildPlaceholder() => Container(
          width: effectiveWidth,
          height: effectiveHeight,
          color: const Color(0xFFF2F4F8),
          child: Icon(
            placeholderIcon,
            size: effectiveWidth * 0.45,
            color: AppColors.textHint.withValues(alpha: 0.7),
          ),
        );

    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: resolved == null
          ? buildPlaceholder()
          : CachedNetworkImage(
              imageUrl: resolved,
              width: effectiveWidth,
              height: effectiveHeight,
              fit: fit,
              memCacheWidth: (effectiveWidth * 3).round(),
              fadeInDuration: const Duration(milliseconds: 160),
              placeholder: (context, url) => buildPlaceholder(),
              errorWidget: (context, url, error) => buildPlaceholder(),
            ),
    );
  }
}
