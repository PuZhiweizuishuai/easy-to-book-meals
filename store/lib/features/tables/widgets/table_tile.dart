import 'package:flutter/material.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';

/// 餐桌状态 → 颜色
///
/// 餐桌列表、卡片、详情页共用，集中在此避免多处重复定义。
Color tableStatusColor(TableStatus status) => switch (status) {
      TableStatus.free => AppColors.success,
      TableStatus.occupied => AppColors.danger,
      TableStatus.reserved => AppColors.warning,
      TableStatus.cleaning => AppColors.info,
      TableStatus.disabled => AppColors.disabled,
    };

/// 餐桌卡片（可复用）
///
/// 状态色块 + 桌号 + 座位数 + 区域；点击进入详情（查看当前订单 / 预约），
/// 长按复制扫码标识，右下角二维码图标或菜单「查看二维码」可查看扫码点餐二维码，
/// 菜单另提供「详情 / 切换状态 / 预约 / 编辑 / 删除」。
class TableTile extends StatelessWidget {
  const TableTile({
    super.key,
    required this.table,
    this.busy = false,
    this.onTap,
    this.onViewDetail,
    this.onLongPress,
    this.onViewQr,
    this.onReserve,
    this.onStatus,
    this.onEdit,
    this.onDelete,
  });

  final DiningTable table;
  final bool busy;

  /// 整卡点击（向后兼容：未提供 [onViewDetail] 时由本回调处理）
  final VoidCallback? onTap;

  /// 进入餐桌详情（查看当前状态 / 关联订单 / 本桌预约）
  final VoidCallback? onViewDetail;

  final VoidCallback? onLongPress;
  final VoidCallback? onViewQr;

  /// 创建预约
  final VoidCallback? onReserve;

  /// 切换餐桌状态（弹出状态选择）
  final VoidCallback? onStatus;

  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final status = table.statusEnum;
    final color = tableStatusColor(status);
    // 座位数与区域合并成一行，避免两行文字被底部的预约/二维码图标压住
    final infoText = <String>[
      '${table.capacity ?? 0} 座',
      if (table.area?.isNotEmpty == true) table.area!,
    ].join(' · ');

    return Container(
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.07),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withValues(alpha: 0.45), width: 1),
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: busy ? null : (onViewDetail ?? onTap),
          onLongPress: busy ? null : onLongPress,
          child: Stack(
            children: [
              Padding(
                // 底部留出图标带：左下角预约图标、右下角二维码图标各占一角
                // （图标 16 + 内边距 6 = 22，再留 6 的呼吸空间）
                padding: const EdgeInsets.fromLTRB(9, 8, 6, 28),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: color,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        status.label,
                        style: const TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    const Spacer(),
                    Text(
                      table.tableNumber ?? '-',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Row(
                      children: [
                        Icon(Icons.event_seat_outlined,
                            size: 12, color: AppColors.textHint),
                        const SizedBox(width: 2),
                        Expanded(
                          child: Text(
                            infoText,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 11,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Positioned(
                top: 0,
                right: 0,
                child: PopupMenuButton<String>(
                  iconSize: 18,
                  padding: EdgeInsets.zero,
                  icon: const Icon(Icons.more_vert,
                      size: 16, color: AppColors.textHint),
                  onSelected: (value) {
                    if (value == 'detail') onViewDetail?.call();
                    if (value == 'qr') onViewQr?.call();
                    if (value == 'reserve') onReserve?.call();
                    if (value == 'status') onStatus?.call();
                    if (value == 'edit') onEdit?.call();
                    if (value == 'delete') onDelete?.call();
                  },
                  itemBuilder: (context) => const [
                    PopupMenuItem<String>(
                      value: 'detail',
                      child: Text('查看详情'),
                    ),
                    PopupMenuItem<String>(
                      value: 'qr',
                      child: Text('查看二维码'),
                    ),
                    PopupMenuItem<String>(
                      value: 'status',
                      child: Text('切换状态'),
                    ),
                    PopupMenuItem<String>(
                      value: 'reserve',
                      child: Text('创建预约'),
                    ),
                    PopupMenuItem<String>(value: 'edit', child: Text('编辑')),
                    PopupMenuItem<String>(
                      value: 'delete',
                      child: Text(
                        '删除',
                        style: TextStyle(color: AppColors.danger),
                      ),
                    ),
                  ],
                ),
              ),
              if (busy)
                const Positioned.fill(
                  child: Center(
                    child: SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  ),
                ),
              if (table.qrCodeKey?.isNotEmpty == true)
                Positioned(
                  bottom: 0,
                  right: 0,
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: (busy || onViewQr == null) ? null : onViewQr,
                    child: Padding(
                      padding: const EdgeInsets.all(6),
                      child: Icon(
                        Symbols.qr_code,
                        size: 16,
                        color: color.withValues(alpha: 0.85),
                      ),
                    ),
                  ),
                ),
              if (onReserve != null)
                Positioned(
                  bottom: 0,
                  left: 0,
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: busy ? null : onReserve,
                    child: Padding(
                      padding: const EdgeInsets.all(6),
                      child: Icon(
                        Symbols.event_available,
                        size: 16,
                        color: AppColors.warning.withValues(alpha: 0.9),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
