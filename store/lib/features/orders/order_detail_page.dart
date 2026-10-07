import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../providers/session_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/app_network_image.dart';
import '../../shared/widgets/state_views.dart';
import 'checkout_sheet.dart';
import 'order_add_items_page.dart';
import 'order_ui.dart';

/// 订单详情页
///
/// 承载菜品级操作（出餐 / 退菜 / 调整数量）与订单级流转，
/// 结算入口为底部的「结算」按钮（结算面板内支持扫码核销优惠券）。
class OrderDetailPage extends ConsumerStatefulWidget {
  const OrderDetailPage({
    super.key,
    required this.orderId,
    required this.storeId,
    this.focusCheckout = false,
  });

  final int orderId;
  final int storeId;

  /// 进入后自动弹出结算面板（列表页点「结算」时传入）
  final bool focusCheckout;

  @override
  ConsumerState<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends ConsumerState<OrderDetailPage> {
  OrderData? _order;
  bool _loading = true;
  String? _error;
  int? _busyItemId;
  bool _orderBusy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final order = await ref
          .read(orderApiProvider)
          .detail(orderId: widget.orderId, storeId: widget.storeId);
      if (!mounted) return;
      setState(() {
        _order = order;
        _loading = false;
      });
      if (widget.focusCheckout && order != null) {
        await Future<void>.delayed(const Duration(milliseconds: 260));
        if (!mounted) return;
        await _checkout(order);
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      setState(() {
        _error = e.message;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = '加载失败，请稍后重试';
        _loading = false;
      });
    }
  }

  Future<void> _run({
    required Future<OrderData?> Function() action,
    int? busyItemId,
    String? successText,
  }) async {
    setState(() {
      _busyItemId = busyItemId;
      if (busyItemId == null) _orderBusy = true;
    });
    try {
      final updated = await action();
      if (!mounted) return;
      if (updated != null) {
        setState(() => _order = updated);
      } else {
        await _load();
      }
      // 数据有变化，返回列表时刷新
      _changed = true;
      // 订单状态变化会影响看板营业额 / 待接单数，通知常驻数据页重新拉取
      ref.read(dataRefreshProvider.notifier).bump();
      if (successText != null) AppToast.success(context, successText);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '操作失败，请稍后重试');
    } finally {
      if (mounted) {
        setState(() {
          _busyItemId = null;
          _orderBusy = false;
        });
      }
    }
  }

  Future<void> _serveItem(OrderItem item) => _run(
        busyItemId: item.id,
        successText: '已出餐',
        action: () => ref.read(orderApiProvider).serveItem(
              itemId: item.id!,
              storeId: widget.storeId,
            ),
      );

  Future<void> _removeItem(OrderItem item) async {
    final reason = await _askInput('退菜原因', '店员退菜');
    if (!mounted || reason == null) return;
    await _run(
      busyItemId: item.id,
      successText: '已退菜',
      action: () => ref.read(orderApiProvider).removeItem(
            itemId: item.id!,
            storeId: widget.storeId,
            reason: reason,
          ),
    );
  }

  Future<void> _adjustItem(OrderItem item) async {
    final value = await _askInput(
      '调整数量（${item.dishName ?? '菜品'}）',
      item.quantityText,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
    );
    if (!mounted || value == null) return;
    final quantity = double.tryParse(value);
    if (quantity == null || quantity <= 0) {
      AppToast.error(context, '数量必须大于 0');
      return;
    }
    await _run(
      busyItemId: item.id,
      successText: '数量已调整',
      action: () => ref.read(orderApiProvider).adjustItem(
            itemId: item.id!,
            storeId: widget.storeId,
            quantity: quantity,
          ),
    );
  }

  Future<String?> _askInput(
    String title,
    String defaultValue, {
    TextInputType keyboardType = TextInputType.text,
  }) async {
    final controller = TextEditingController(text: defaultValue);
    return showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (sheetContext) => SafeArea(
        top: false,
        child: Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: 16 + MediaQuery.of(sheetContext).viewInsets.bottom,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: controller,
                autofocus: true,
                keyboardType: keyboardType,
                decoration: const InputDecoration(labelText: '内容'),
              ),
              const SizedBox(height: 14),
              FilledButton(
                onPressed: () =>
                    Navigator.of(sheetContext).pop(controller.text.trim()),
                child: const Text('确定'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _checkout(OrderData order) async {
    final ok = await CheckoutSheet.show(
      context,
      order: order,
      storeId: widget.storeId,
    );
    if (!mounted) return;
    if (ok == true) {
      Navigator.of(context).pop(true);
    } else {
      // 结算面板关闭后刷新，避免金额显示与后端不一致
      await _load();
    }
  }

  /// 打开加菜页，成功后刷新详情并通知列表刷新
  Future<void> _openAddItems() async {
    final order = _order;
    if (order == null || order.id == null) return;
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => OrderAddItemsPage(
          orderId: order.id!,
          storeId: widget.storeId,
        ),
      ),
    );
    if (changed == true && mounted) {
      _changed = true;
      await _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    final order = _order;
    return PopScope<Object?>(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        Navigator.of(context).pop(_changed);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('订单详情'),
          actions: [
            // 与 Web 端一致：未完成 / 未取消订单，且有 order:manage 权限可加菜
            if (order != null &&
                order.statusEnum != OrderStatus.completed &&
                order.statusEnum != OrderStatus.cancelled &&
                (ref.watch(currentUserProvider)?.hasAnyPermission(
                        const ['order:manage']) ??
                    false))
              IconButton(
                tooltip: '加菜',
                icon: const Icon(Icons.post_add, size: 23),
                onPressed: _openAddItems,
              ),
            IconButton(
              icon: const Icon(Icons.refresh_rounded, size: 22),
              onPressed: _load,
            ),
          ],
        ),
        body: _loading
            ? const LoadingView()
            : _error != null
                ? ErrorView(message: _error!, onRetry: _load)
                : order == null
                    ? const EmptyView(title: '订单不存在')
                    : _buildBody(order),
        bottomNavigationBar: order == null ? null : _buildActionBar(order),
      ),
    );
  }

  /// 是否发生过数据变更（返回列表时通知刷新）
  bool _changed = false;

  Widget _buildBody(OrderData order) {
    final items = order.items ?? const [];
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
        children: [
          _buildSummary(order),
          const SizedBox(height: 12),
          SectionTitle(
            title: '菜品明细',
            subtitle: '共 ${items.length} 项',
            padding: const EdgeInsets.fromLTRB(0, 0, 0, 8),
          ),
          if (items.isEmpty)
            const AppCard(
              child: EmptyView(
                title: '暂无菜品明细',
                compact: true,
                icon: Icons.fastfood,
              ),
            )
          else
            AppCard(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Column(
                children: [
                  for (final item in items)
                    _ItemTile(
                      item: item,
                      busy: _busyItemId == item.id,
                      editable: order.statusEnum != OrderStatus.completed &&
                          order.statusEnum != OrderStatus.cancelled,
                      onServe: () => _serveItem(item),
                      onRemove: () => _removeItem(item),
                      onAdjust: () => _adjustItem(item),
                    ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildSummary(OrderData order) {
    final status = order.statusEnum;
    return AppCard(
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  order.orderNo ?? '',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 9,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: OrderStatusStyle.backgroundOf(status),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  OrderStatusStyle.labelOf(status),
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: OrderStatusStyle.colorOf(status),
                  ),
                ),
              ),
            ],
          ),
          const Divider(height: 14),
          _DetailRow(label: '订单类型', value: order.typeEnum.label),
          if (order.tableNumber?.isNotEmpty == true)
            _DetailRow(label: '餐桌', value: '${order.tableNumber} 桌'),
          _DetailRow(label: '下单用户', value: order.userNickname ?? '匿名顾客'),
          if (order.contactPhone?.isNotEmpty == true)
            _DetailRow(label: '联系电话', value: order.contactPhone!),
          _DetailRow(label: '下单时间', value: TimeFormat.dateTime(order.createTime)),
          if (order.confirmTime != null)
            _DetailRow(
              label: '接单时间',
              value: TimeFormat.dateTime(order.confirmTime),
            ),
          if (order.remark?.isNotEmpty == true)
            _DetailRow(label: '订单备注', value: order.remark!, highlight: true),
          if (order.cancelReason?.isNotEmpty == true)
            _DetailRow(
              label: '取消原因',
              value: order.cancelReason!,
              highlight: true,
            ),
          const Divider(height: 14),
          _DetailRow(
            label: '商品原价',
            value: MoneyFormat.yuan(order.totalAmount ?? 0),
          ),
          if ((order.discountAmount ?? 0) > 0)
            _DetailRow(
              label: '菜品折扣',
              value: '-${MoneyFormat.plain(order.discountAmount ?? 0)}',
            ),
          if ((order.couponAmount ?? 0) > 0)
            _DetailRow(
              label: '优惠券减免',
              value: '-${MoneyFormat.plain(order.couponAmount ?? 0)}',
            ),
          if ((order.manualDiscountAmount ?? 0) > 0)
            _DetailRow(
              label: '手动减免',
              value: '-${MoneyFormat.plain(order.manualDiscountAmount ?? 0)}',
            ),
          const SizedBox(height: 6),
          Row(
            children: [
              const Text(
                '应付金额',
                style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
              ),
              const Spacer(),
              Text(
                MoneyFormat.yuan(order.payAmount ?? 0),
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget? _buildActionBar(OrderData order) {
    final busy = _orderBusy;
    final List<Widget> children;
    switch (order.statusEnum) {
      case OrderStatus.pending:
        children = [
          Expanded(
            child: SecondaryButton(
              label: '拒单',
              color: AppColors.danger,
              expand: true,
              onPressed: busy
                  ? null
                  : () async {
                      final reason = await _askInput('拒单原因', '店家拒单');
                      if (!mounted || reason == null) return;
                      await _run(
                        successText: '已拒单',
                        action: () => ref.read(orderApiProvider).reject(
                              orderId: order.id!,
                              storeId: widget.storeId,
                              reason: reason,
                            ),
                      );
                    },
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 2,
            child: PrimaryButton(
              label: '接单',
              busy: busy,
              onPressed: busy
                  ? null
                  : () => _run(
                        successText: '已接单',
                        action: () => ref.read(orderApiProvider).confirm(
                              orderId: order.id!,
                              storeId: widget.storeId,
                            ),
                      ),
            ),
          ),
        ];
        break;
      case OrderStatus.confirmed:
        children = [
          Expanded(
            child: PrimaryButton(
              label: '开始制作',
              busy: busy,
              onPressed: busy
                  ? null
                  : () => _run(
                        successText: '已开始制作',
                        action: () => ref.read(orderApiProvider).cooking(
                              orderId: order.id!,
                              storeId: widget.storeId,
                            ),
                      ),
            ),
          ),
        ];
        break;
      case OrderStatus.cooking:
      case OrderStatus.ready:
        children = [
          Expanded(
            child: SecondaryButton(
              label: '全部出餐',
              expand: true,
              onPressed: busy
                  ? null
                  : () => _run(
                        successText: '已全部出餐',
                        action: () => ref.read(orderApiProvider).serveOrder(
                              orderId: order.id!,
                              storeId: widget.storeId,
                            ),
                      ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 2,
            child: PrimaryButton(
              label: '结算',
              busy: busy,
              onPressed: busy ? null : () => _checkout(order),
            ),
          ),
        ];
        break;
      default:
        return null;
    }
    return SafeArea(
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: AppColors.divider)),
        ),
        child: Row(children: children),
      ),
    );
  }
}

/// 明细行
class _DetailRow extends StatelessWidget {
  const _DetailRow({
    required this.label,
    required this.value,
    this.highlight = false,
  });

  final String label;
  final String value;
  final bool highlight;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 76,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 13,
                color: AppColors.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: 13.5,
                color: highlight ? AppColors.warning : AppColors.textPrimary,
                fontWeight: highlight ? FontWeight.w600 : FontWeight.w400,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 菜品明细条目（可复用：出餐 / 退菜 / 改量）
class _ItemTile extends StatelessWidget {
  const _ItemTile({
    required this.item,
    required this.busy,
    required this.editable,
    required this.onServe,
    required this.onRemove,
    required this.onAdjust,
  });

  final OrderItem item;
  final bool busy;
  final bool editable;
  final VoidCallback onServe;
  final VoidCallback onRemove;
  final VoidCallback onAdjust;

  @override
  Widget build(BuildContext context) {
    final cancelled = !item.isValid;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AppNetworkImage(url: item.topImage, size: 46),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    if (item.isAddedLater)
                      Container(
                        margin: const EdgeInsets.only(right: 5),
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
                        item.dishName ?? '未知菜品',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          color: cancelled ? AppColors.textHint : AppColors.textPrimary,
                          decoration: cancelled ? TextDecoration.lineThrough : null,
                        ),
                      ),
                    ),
                  ],
                ),
                if (item.specNames?.isNotEmpty == true)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      item.specNames!,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textHint,
                      ),
                    ),
                  ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Text(
                      MoneyFormat.yuan(item.dealPrice ?? item.unitPrice ?? 0),
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textHint,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '× ${item.quantityText}',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const Spacer(),
                    Text(
                      MoneyFormat.yuan(item.amount),
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
                      ),
                    ),
                  ],
                ),
                if (editable && !cancelled) ...[
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _MiniAction(
                        label: '改量',
                        color: AppColors.info,
                        busy: busy,
                        onTap: onAdjust,
                      ),
                      const SizedBox(width: 8),
                      _MiniAction(
                        label: '退菜',
                        color: AppColors.danger,
                        busy: busy,
                        onTap: onRemove,
                      ),
                      const SizedBox(width: 8),
                      if (!item.isServed)
                        _MiniAction(
                          label: '出餐',
                          color: AppColors.success,
                          busy: busy,
                          filled: true,
                          onTap: onServe,
                        )
                      else
                        const Icon(Icons.check_circle,
                            size: 18, color: AppColors.success),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 明细行的小按钮
class _MiniAction extends StatelessWidget {
  const _MiniAction({
    required this.label,
    required this.color,
    required this.onTap,
    this.busy = false,
    this.filled = false,
  });

  final String label;
  final Color color;
  final VoidCallback onTap;
  final bool busy;
  final bool filled;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 30,
      child: filled
          ? FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: color,
                // 覆盖主题的 Size.fromHeight(46)（宽度 infinity），Row 内会崩
                minimumSize: const Size(0, 30),
                padding: const EdgeInsets.symmetric(horizontal: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              onPressed: busy ? null : onTap,
              child: busy
                  ? const SizedBox(
                      width: 13,
                      height: 13,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Text(label, style: const TextStyle(fontSize: 12.5)),
            )
          : OutlinedButton(
              style: OutlinedButton.styleFrom(
                foregroundColor: color,
                // 覆盖主题的 Size.fromHeight(46)（宽度 infinity），Row 内会崩
                minimumSize: const Size(0, 30),
                padding: const EdgeInsets.symmetric(horizontal: 12),
                side: BorderSide(color: color.withValues(alpha: 0.5)),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              onPressed: busy ? null : onTap,
              child: Text(label, style: const TextStyle(fontSize: 12.5)),
            ),
    );
  }
}
