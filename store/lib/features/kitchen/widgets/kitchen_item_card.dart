import 'package:flutter/material.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/money_format.dart';
import '../../../core/utils/time_format.dart';
import '../../../data/models/models.dart';

/// 后厨菜品卡片（可复用）
///
/// 卡片配色随等待时长升级：正常 → 提醒（15 分钟）→ 紧急（30 分钟），
/// 后厨在忙碌时靠颜色即可判断优先级。
class KitchenItemCard extends StatelessWidget {
  const KitchenItemCard({
    super.key,
    required this.item,
    this.busy = false,
    required this.onServe,
    required this.onStart,
    required this.onRelease,
  });

  /// 队列条目
  final KitchenQueueItem item;

  /// 是否正在处理（按钮 loading）
  final bool busy;

  /// 出餐
  final VoidCallback onServe;

  /// 认领制作
  final VoidCallback onStart;

  /// 释放认领
  final VoidCallback onRelease;

  @override
  Widget build(BuildContext context) {
    final level = item.urgencyLevel;
    final accent = switch (level) {
      2 => AppColors.danger,
      1 => AppColors.warning,
      _ => AppColors.primary,
    };
    final background = switch (level) {
      2 => AppColors.dangerLight,
      1 => AppColors.warningLight,
      _ => Colors.white,
    };
    final preparing = item.myPreparing;

    return Container(
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: level == 0 ? AppColors.divider : accent.withValues(alpha: 0.4),
          width: 1,
        ),
        boxShadow: const [
          BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 头部：菜名 + 数量
          Container(
            padding: const EdgeInsets.fromLTRB(11, 10, 11, 8),
            decoration: BoxDecoration(
              color: accent.withValues(alpha: 0.1),
              borderRadius: const BorderRadius.vertical(
                top: Radius.circular(14),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    if (item.added)
                      Container(
                        margin: const EdgeInsets.only(right: 5),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 4,
                          vertical: 1,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.danger,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: const Text(
                          '加菜',
                          style: TextStyle(
                            fontSize: 9.5,
                            color: Colors.white,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    Expanded(
                      child: Text(
                        item.dishName ?? '未知菜品',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          height: 1.25,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Text(
                      '× ${MoneyFormat.quantity(item.count)}',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: accent,
                      ),
                    ),
                    const Spacer(),
                    Icon(Symbols.timer, size: 13, color: accent),
                    const SizedBox(width: 3),
                    Text(
                      TimeFormat.waited(item.waitedMinutes),
                      style: TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w600,
                        color: accent,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          // 中部：规格 / 桌号 / 备注
          Expanded(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(11, 8, 11, 4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (item.specNames?.isNotEmpty == true)
                    Text(
                      item.specNames!,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(Icons.table_restaurant_outlined,
                          size: 13, color: AppColors.textHint),
                      const SizedBox(width: 3),
                      Expanded(
                        child: Text(
                          item.tableNumber?.isNotEmpty == true
                              ? '${item.tableNumber} 桌'
                              : (item.orderNo ?? '无桌号'),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 11.5,
                            color: AppColors.textHint,
                          ),
                        ),
                      ),
                    ],
                  ),
                  if (item.remark?.isNotEmpty == true) ...[
                    const SizedBox(height: 4),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(Icons.chat_outlined,
                            size: 12, color: AppColors.warning),
                        const SizedBox(width: 3),
                        Expanded(
                          child: Text(
                            item.remark!,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 11.5,
                              height: 1.3,
                              color: AppColors.warning,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                  if (preparing) ...[
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.infoLight,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: const Text(
                        '制作中',
                        style: TextStyle(
                          fontSize: 10.5,
                          fontWeight: FontWeight.w600,
                          color: AppColors.info,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),

          // 底部：操作按钮
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 0, 10, 10),
            child: Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: 34,
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        foregroundColor: preparing
                            ? AppColors.textSecondary
                            : AppColors.info,
                        side: BorderSide(
                          color: preparing
                              ? AppColors.divider
                              : AppColors.info.withValues(alpha: 0.5),
                        ),
                        padding: EdgeInsets.zero,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(9),
                        ),
                      ),
                      onPressed: busy
                          ? null
                          : (preparing ? onRelease : onStart),
                      child: busy
                          ? const SizedBox(
                              width: 13,
                              height: 13,
                              child:
                                  CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(
                              preparing ? '释放' : '认领',
                              style: const TextStyle(fontSize: 12.5),
                            ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: SizedBox(
                    height: 34,
                    child: FilledButton.icon(
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.success,
                        padding: EdgeInsets.zero,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(9),
                        ),
                      ),
                      icon: const Icon(Icons.check_rounded, size: 16),
                      label:
                          const Text('出餐', style: TextStyle(fontSize: 12.5)),
                      onPressed: busy ? null : onServe,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
