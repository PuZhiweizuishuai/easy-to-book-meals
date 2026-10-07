import 'package:flutter/material.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../../core/theme/app_colors.dart';
import '../../../data/models/models.dart';
import '../table_page.dart';

/// 餐桌卡片（可复用）
///
/// 状态色块 + 桌号 + 座位数 + 区域；点击改状态，长按复制扫码标识，
/// 右下角二维码图标或右上角菜单可查看扫码点餐二维码，菜单另提供编辑与删除。
class TableTile extends StatelessWidget {
  const TableTile({
    super.key,
    required this.table,
    this.busy = false,
    this.onTap,
    this.onLongPress,
    this.onViewQr,
    this.onEdit,
    this.onDelete,
  });

  final DiningTable table;
  final bool busy;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final VoidCallback? onViewQr;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final status = table.statusEnum;
    final color = tableStatusColor(status);

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
          onTap: busy ? null : onTap,
          onLongPress: busy ? null : onLongPress,
          child: Stack(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(9, 8, 4, 8),
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
                        Text(
                          '${table.capacity ?? 0} 座',
                          style: const TextStyle(
                            fontSize: 11,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                    if (table.area?.isNotEmpty == true)
                      Text(
                        table.area!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 10.5,
                          color: AppColors.textHint,
                        ),
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
                    if (value == 'qr') onViewQr?.call();
                    if (value == 'edit') onEdit?.call();
                    if (value == 'delete') onDelete?.call();
                  },
                  itemBuilder: (context) => const [
                    PopupMenuItem<String>(
                      value: 'qr',
                      child: Text('查看二维码'),
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
            ],
          ),
        ),
      ),
    );
  }
}
