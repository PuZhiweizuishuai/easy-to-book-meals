import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/money_format.dart';
import '../../../core/utils/time_format.dart';
import '../../../data/models/models.dart';
import '../../../shared/widgets/status_chip.dart';
import '../order_ui.dart';

/// 订单卡片（可复用）
///
/// 订单列表、会员消费记录等多处复用同一个卡片，
/// 通过 [onTap] 与 [actions] 决定交互，保持视觉与信息密度一致。
class OrderCard extends StatelessWidget {
  const OrderCard({
    super.key,
    required this.order,
    this.onTap,
    this.actions = const [],
    this.showCustomer = true,
  });

  /// 订单数据
  final OrderData order;

  /// 点击整卡查看明细
  final VoidCallback? onTap;

  /// 底部操作按钮（由上层按状态提供）
  final List<Widget> actions;

  /// 是否展示顾客信息（会员消费记录里不需要重复展示）
  final bool showCustomer;

  @override
  Widget build(BuildContext context) {
    final status = order.statusEnum;
    final fg = OrderStatusStyle.colorOf(status);
    final bg = OrderStatusStyle.backgroundOf(status);
    final pay = PayStatusStyle.style(order.payStatusEnum);

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        boxShadow: const [
          BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 顶部：订单号 + 状态 + 支付状态
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        order.orderNo ?? '订单号丢失',
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    StatusChip(
                      label: OrderStatusStyle.labelOf(status),
                      foregroundColor: fg,
                      backgroundColor: bg,
                    ),
                  ],
                ),
                const SizedBox(height: 8),

                // 类型 / 桌号 / 下单人
                Wrap(
                  spacing: 6,
                  runSpacing: 4,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    StatusChip(
                      label: order.typeEnum.label,
                      icon: orderTypeIcon(order.typeEnum),
                      foregroundColor: AppColors.textSecondary,
                      backgroundColor: const Color(0xFFF2F4F8),
                      dense: true,
                    ),
                    if (order.tableNumber?.isNotEmpty == true)
                      StatusChip(
                        label: '${order.tableNumber} 桌',
                        foregroundColor: AppColors.info,
                        backgroundColor: AppColors.infoLight,
                        dense: true,
                      ),
                    StatusChip(
                      label: PayStatusStyle.label(order.payStatusEnum),
                      foregroundColor: pay.$2,
                      backgroundColor: pay.$1,
                      dense: true,
                    ),
                    Text(
                      TimeFormat.smart(order.createTime),
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textHint,
                      ),
                    ),
                  ],
                ),

                if (showCustomer &&
                    (order.userNickname?.isNotEmpty == true ||
                        order.contactPhone?.isNotEmpty == true)) ...[
                  const SizedBox(height: 6),
                  Text(
                    '${order.userNickname ?? '匿名顾客'}${order.contactPhone?.isNotEmpty == true ? ' · ${order.contactPhone}' : ''}',
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],

                if (order.items != null && order.items!.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  const Divider(height: 1),
                  const SizedBox(height: 8),
                  ..._buildItemPreview(),
                ],

                if (order.remark?.isNotEmpty == true) ...[
                  const SizedBox(height: 6),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.chat_outlined,
                          size: 13, color: AppColors.warning),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          order.remark!,
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.warning,
                            height: 1.4,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],

                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: RichText(
                        text: TextSpan(
                          children: [
                            const TextSpan(
                              text: '应收 ',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.textHint,
                              ),
                            ),
                            TextSpan(
                              text: MoneyFormat.yuan(order.payAmount ?? 0),
                              style: const TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.w700,
                                color: AppColors.primary,
                              ),
                            ),
                            if ((order.couponDiscount +
                                        order.manualDiscount) >
                                    0) ...[
                              TextSpan(
                                text:
                                    '  (已优惠 ${MoneyFormat.plain(order.couponDiscount + order.manualDiscount)})',
                                style: const TextStyle(
                                  fontSize: 11.5,
                                  color: AppColors.success,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                    ...actions,
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  List<Widget> _buildItemPreview() {
    final items = order.validItems;
    if (items.isEmpty) {
      return const [
        Text(
          '该订单菜品已全部退菜',
          style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
        ),
      ];
    }
    final shown = items.take(3).toList();
    final rest = items.length - shown.length;
    return [
      for (final item in shown)
        Padding(
          padding: const EdgeInsets.only(bottom: 4),
          child: Row(
            children: [
              if (item.isAddedLater)
                Container(
                  margin: const EdgeInsets.only(right: 4),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 4,
                    vertical: 1,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.dangerLight,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: const Text(
                    '加菜',
                    style: TextStyle(
                      fontSize: 10,
                      color: AppColors.danger,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              Expanded(
                child: Text(
                  '${item.dishName ?? '未知菜品'}${item.specNames?.isNotEmpty == true ? '（${item.specNames}）' : ''}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 13,
                    color: item.isServed
                        ? AppColors.textHint
                        : AppColors.textPrimary,
                    decoration: item.isServed
                        ? TextDecoration.lineThrough
                        : null,
                  ),
                ),
              ),
              Text(
                '×${item.quantityText}',
                style:
                    const TextStyle(fontSize: 13, color: AppColors.textSecondary),
              ),
              const SizedBox(width: 10),
              Text(
                MoneyFormat.yuan(item.amount),
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      if (rest > 0)
        Padding(
          padding: const EdgeInsets.only(top: 2),
          child: Text(
            '还有 $rest 道菜…',
            style: const TextStyle(fontSize: 12, color: AppColors.textHint),
          ),
        ),
    ];
  }
}
