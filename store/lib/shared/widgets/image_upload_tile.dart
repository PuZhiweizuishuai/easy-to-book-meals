import 'package:flutter/material.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/theme/app_colors.dart';
import 'app_network_image.dart';

/// 图片上传选择区（可复用）
///
/// 展示已选择的图片地址，末尾是「添加」格子：
/// - 图片选取与上传由外部 [onPick] 完成（组件不持有 Api，方便在别的页面复用）；
/// - [onRemove] 负责把地址从外部状态里移除。
///
/// 菜品封面（max=1）与详情页图片（max>1）都用它。
class ImageUploadTile extends StatelessWidget {
  const ImageUploadTile({
    super.key,
    required this.urls,
    required this.onPick,
    required this.onRemove,
    this.max = 1,
    this.uploading = false,
    this.size = 80,
    this.hint,
  });

  /// 已选择的图片地址（服务器返回的相对路径或完整 URL）
  final List<String> urls;

  /// 点击「添加」：选取并上传，成功后自行刷新外部状态
  final VoidCallback onPick;

  /// 删除某张图片
  final ValueChanged<String> onRemove;

  /// 最多可选数量
  final int max;

  /// 是否正在上传
  final bool uploading;

  /// 单个格子边长
  final double size;

  /// 补充说明文案
  final String? hint;

  @override
  Widget build(BuildContext context) {
    final canAdd = urls.length < max;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: [
            for (final url in urls)
              _ImageTile(
                url: url,
                size: size,
                onRemove: () => onRemove(url),
              ),
            if (canAdd)
              _AddTile(
                size: size,
                uploading: uploading,
                onTap: uploading ? null : onPick,
              ),
          ],
        ),
        if (hint != null) ...[
          const SizedBox(height: 6),
          Text(
            hint!,
            style: const TextStyle(fontSize: 11.5, color: AppColors.textHint),
          ),
        ],
      ],
    );
  }
}

class _ImageTile extends StatelessWidget {
  const _ImageTile({
    required this.url,
    required this.size,
    required this.onRemove,
  });

  final String url;
  final double size;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        AppNetworkImage(
          url: url,
          size: size,
          radius: 10,
          placeholderIcon: Symbols.image,
        ),
        Positioned(
          right: -6,
          top: -6,
          child: GestureDetector(
            onTap: onRemove,
            child: Container(
              width: 22,
              height: 22,
              decoration: const BoxDecoration(
                color: AppColors.danger,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.close, size: 14, color: Colors.white),
            ),
          ),
        ),
      ],
    );
  }
}

class _AddTile extends StatelessWidget {
  const _AddTile({
    required this.size,
    required this.uploading,
    required this.onTap,
  });

  final double size;
  final bool uploading;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: const Color(0xFFF2F4F8),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: AppColors.primary.withValues(alpha: 0.35),
          ),
        ),
        child: uploading
            ? const Center(
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Symbols.add_photo_alternate,
                    size: size * 0.34,
                    color: AppColors.primary,
                  ),
                  const SizedBox(height: 3),
                  const Text(
                    '上传',
                    style: TextStyle(
                      fontSize: 11,
                      color: AppColors.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
