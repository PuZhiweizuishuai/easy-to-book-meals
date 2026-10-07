import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/money_format.dart';
import '../../../shared/widgets/app_button.dart';
import '../../../shared/widgets/app_card.dart';

/// 选菜购物车中的一项（协助下单 / 订单加菜共用）
class OrderDishCartItem {
  OrderDishCartItem({
    required this.skuId,
    required this.dishId,
    required this.dishName,
    required this.specNamesText,
    required this.unitPrice,
    required this.dealPrice,
    required this.quantity,
  });

  final int skuId;
  final int dishId;
  final String dishName;
  final String specNamesText;
  final double unitPrice;
  final double dealPrice;
  int quantity;
}

/// 选菜购物车（同 SKU 合并，数量上限 99）
class OrderDishCart extends ChangeNotifier {
  final List<OrderDishCartItem> items = [];

  int get totalCount => items.fold(0, (sum, i) => sum + i.quantity);

  double get totalPrice =>
      items.fold(0, (sum, i) => sum + i.dealPrice * i.quantity);

  /// 加入菜品：同 SKU 合并数量（上限 99）
  void add(OrderDishCartItem item) {
    for (final existing in items) {
      if (existing.skuId == item.skuId) {
        existing.quantity = (existing.quantity + item.quantity).clamp(1, 99);
        notifyListeners();
        return;
      }
    }
    items.add(item);
    notifyListeners();
  }

  /// 增减数量，减到 0 自动移除
  void changeQty(int index, int delta) {
    if (index < 0 || index >= items.length) return;
    final item = items[index];
    item.quantity += delta;
    if (item.quantity <= 0) {
      items.removeAt(index);
    }
    notifyListeners();
  }

  void clear() {
    if (items.isEmpty) return;
    items.clear();
    notifyListeners();
  }
}

/// 底部购物车栏（件数 / 合计 / 清空 / 提交按钮）
class OrderDishCartBar extends StatelessWidget {
  const OrderDishCartBar({
    super.key,
    required this.cart,
    required this.submitLabel,
    required this.onSubmit,
    this.submitIcon = Icons.receipt_long_outlined,
    this.clearConfirmTitle = '清空已选菜品？',
  });

  final OrderDishCart cart;
  final String submitLabel;
  final VoidCallback onSubmit;
  final IconData submitIcon;
  final String clearConfirmTitle;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 14, 10),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: AppColors.divider)),
        ),
        child: Row(
          children: [
            GestureDetector(
              onTap: () => _confirmClear(context),
              child: const Padding(
                padding: EdgeInsets.only(right: 6),
                child: Icon(Icons.delete_outline_rounded,
                    size: 21, color: AppColors.textHint),
              ),
            ),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '已选 ${cart.totalCount} 件',
                    style: const TextStyle(
                      fontSize: 13,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  Text(
                    MoneyFormat.yuan(cart.totalPrice),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(
              height: 42,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  // 覆盖主题 Size.fromHeight(46)（宽度 infinity）：
                  // 该按钮在 Row 内未被 Expanded 包裹，无限宽会导致布局崩溃
                  minimumSize: const Size(0, 42),
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppThemeRadius.md),
                  ),
                ),
                onPressed: onSubmit,
                icon: Icon(submitIcon, size: 18),
                label: Text(
                  submitLabel,
                  style: const TextStyle(
                      fontSize: 14.5, fontWeight: FontWeight.w600),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _confirmClear(BuildContext context) async {
    final ok = await showConfirmDialog(
      context,
      title: clearConfirmTitle,
      confirmText: '清空',
      danger: true,
    );
    if (ok == true) cart.clear();
  }
}

/// 购物车明细行（菜名 / 规格 / 单价 + 数量加减器）
class DishCartRow extends StatelessWidget {
  const DishCartRow({
    super.key,
    required this.item,
    required this.onDecrease,
    required this.onIncrease,
  });

  final OrderDishCartItem item;
  final VoidCallback onDecrease;
  final VoidCallback? onIncrease;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.dishName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  '${item.specNamesText} · ${MoneyFormat.plain(item.dealPrice)}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.textHint,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          MiniStepper(
            quantity: item.quantity,
            onDecrease: onDecrease,
            onIncrease: item.quantity >= 99 ? null : onIncrease,
          ),
        ],
      ),
    );
  }
}

/// 数量加减器
class MiniStepper extends StatelessWidget {
  const MiniStepper({
    super.key,
    required this.quantity,
    required this.onDecrease,
    this.onIncrease,
  });

  final int quantity;
  final VoidCallback onDecrease;
  final VoidCallback? onIncrease;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _StepButton(icon: Icons.remove, onTap: onDecrease),
        SizedBox(
          width: 32,
          child: Text(
            '$quantity',
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
          ),
        ),
        _StepButton(icon: Icons.add, onTap: onIncrease),
      ],
    );
  }
}

class _StepButton extends StatelessWidget {
  const _StepButton({required this.icon, this.onTap});

  final IconData icon;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final enabled = onTap != null;
    return SizedBox(
      width: 28,
      height: 28,
      child: Material(
        color: enabled ? const Color(0xFFF2F4F8) : const Color(0xFFF7F8FA),
        shape: const CircleBorder(),
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: onTap,
          child: Icon(
            icon,
            size: 15,
            color: enabled ? AppColors.textPrimary : AppColors.textHint,
          ),
        ),
      ),
    );
  }
}
