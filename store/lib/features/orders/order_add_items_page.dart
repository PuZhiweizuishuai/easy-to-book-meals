import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../providers/session_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/search_field.dart';
import '../../shared/widgets/sku_picker_sheet.dart';
import '../../shared/widgets/state_views.dart';
import '../../shared/widgets/status_chip.dart';
import 'order_ui.dart';
import 'widgets/dish_pick_tile.dart';
import 'widgets/order_dish_cart.dart';

/// 订单加菜页（店员为未结算订单追加菜品）
///
/// 对齐 Web 端 OrderManagement.vue 的 useAddItems 流程：
/// 选择菜品（SKU 弹窗选规格 / 数量）→ 底部购物车栏汇总 →
/// 「确认加菜」提交 POST /order/manage/add-items/{id}。
/// 仅待接单 / 已接单 / 制作中 / 已出餐（status < 4）订单允许进入。
/// 提交成功返回 `true`，调用方据此刷新订单详情 / 列表。
class OrderAddItemsPage extends ConsumerStatefulWidget {
  const OrderAddItemsPage({
    super.key,
    required this.orderId,
    required this.storeId,
  });

  final int orderId;
  final int storeId;

  @override
  ConsumerState<OrderAddItemsPage> createState() => _OrderAddItemsPageState();
}

class _OrderAddItemsPageState extends ConsumerState<OrderAddItemsPage> {
  final OrderDishCart _cart = OrderDishCart();

  OrderData? _order;
  List<Dish> _dishes = const [];
  bool _loading = true;
  String? _error;
  String _keyword = '';

  @override
  void initState() {
    super.initState();
    _loadOrderDetail();
    _loadDishes();
  }

  @override
  void dispose() {
    _cart.dispose();
    super.dispose();
  }

  /// 下拉 / 手动刷新：订单摘要与菜品一起重拉
  Future<void> _refresh() async {
    await Future.wait<void>([_loadOrderDetail(), _loadDishes()]);
  }

  Future<void> _loadOrderDetail() async {
    try {
      final order = await ref
          .read(orderApiProvider)
          .detail(orderId: widget.orderId, storeId: widget.storeId);
      if (!mounted) return;
      setState(() => _order = order);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
    } catch (_) {
      // 摘要加载失败不阻塞选菜，保留空头部
    }
  }

  Future<void> _loadDishes() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      // 与 Web 端一致：拉取前 100 条后在前端过滤上架菜品
      final page = await ref.read(dishApiProvider).list(
            storeId: widget.storeId,
            key: _keyword.isEmpty ? null : _keyword,
            page: 1,
            size: 100,
          );
      if (!mounted) return;
      setState(() {
        _dishes = page.content.where((d) => d.onSale).toList();
        _loading = false;
      });
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

  Future<void> _pickSku(Dish dish) async {
    final result = await SkuPickerSheet.show(
      context,
      dish: dish,
      confirmText: '加入加菜清单',
    );
    if (!mounted || result == null) return;
    _cart.add(
      OrderDishCartItem(
        skuId: result.sku.id!,
        dishId: dish.id!,
        dishName: dish.name ?? '未知菜品',
        specNamesText: result.specNamesText,
        unitPrice: result.sku.unitPrice ?? result.dealPrice,
        dealPrice: result.dealPrice,
        quantity: result.quantity,
      ),
    );
    AppToast.success(context, '已加入加菜清单');
  }

  /// 打开待加清单（在清单内确认提交）
  Future<void> _openPendingSheet() async {
    if (_cart.items.isEmpty) {
      AppToast.warning(context, '请先选择菜品');
      return;
    }
    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _PendingItemsSheet(
        cart: _cart,
        onConfirm: _submit,
      ),
    );
    // 加菜成功：订单金额 / 状态变化会影响看板统计，通知常驻页面刷新
    if (ok == true && mounted) {
      ref.read(dataRefreshProvider.notifier).bump();
      Navigator.of(context).pop(true);
    }
  }

  /// 提交加菜，成功返回 true
  Future<bool> _submit() async {
    if (_cart.items.isEmpty) return false;
    try {
      await ref.read(orderApiProvider).addItems(
            orderId: widget.orderId,
            storeId: widget.storeId,
            payload: AddOrderItemsPayload(
              items: [
                for (final item in _cart.items)
                  CreateOrderItemPayload(
                    skuId: item.skuId,
                    quantity: item.quantity,
                  ),
              ],
            ),
          );
      if (!mounted) return false;
      AppToast.success(context, '加菜成功');
      return true;
    } on ApiException catch (e) {
      if (!mounted) return false;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return false;
      }
      AppToast.error(context, e.message);
      return false;
    } catch (_) {
      if (!mounted) return false;
      AppToast.error(context, '加菜失败，请稍后重试');
      return false;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('订单加菜'),
        actions: [
          IconButton(
            tooltip: '刷新',
            icon: const Icon(Icons.refresh_rounded, size: 22),
            onPressed: _loading ? null : _refresh,
          ),
        ],
      ),
      body: Column(
        children: [
          if (_order != null) _buildOrderHeader(_order!),
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 8),
            child: SearchField(
              hint: '搜索菜品名称',
              onSearch: (value) {
                _keyword = value.trim();
                _loadDishes();
              },
            ),
          ),
          Expanded(child: _buildBody()),
        ],
      ),
      bottomNavigationBar: ListenableBuilder(
        listenable: _cart,
        builder: (context, _) => _cart.items.isEmpty
            ? const SizedBox.shrink()
            : OrderDishCartBar(
                cart: _cart,
                submitLabel: '确认加菜',
                submitIcon: Icons.post_add,
                clearConfirmTitle: '清空待加菜品？',
                onSubmit: _openPendingSheet,
              ),
      ),
    );
  }

  /// 顶部订单摘要（订单号 / 状态 / 当前件数与金额）
  Widget _buildOrderHeader(OrderData order) {
    final status = order.statusEnum;
    return Padding(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 0),
      child: AppCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    order.orderNo ?? '订单号丢失',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ),
                StatusChip(
                  label: OrderStatusStyle.labelOf(status),
                  foregroundColor: OrderStatusStyle.colorOf(status),
                  backgroundColor: OrderStatusStyle.backgroundOf(status),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text.rich(
              TextSpan(
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
                children: [
                  TextSpan(text: '当前 ${order.totalItemCount} 件 · 应收 '),
                  TextSpan(
                    text: MoneyFormat.yuan(order.payAmount ?? 0),
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) return const LoadingView();
    if (_error != null) {
      return ErrorView(message: _error!, onRetry: _refresh);
    }
    if (_dishes.isEmpty) {
      return const EmptyView(
        title: '暂无可选菜品',
        subtitle: '门店还没有上架菜品，可先在管理页维护',
      );
    }
    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: _refresh,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(14, 4, 14, 16),
        itemCount: _dishes.length,
        separatorBuilder: (_, _) => const SizedBox(height: 9),
        itemBuilder: (context, index) => DishPickTile(
          dish: _dishes[index],
          onTap: () => _pickSku(_dishes[index]),
        ),
      ),
    );
  }
}

/// 待加菜品清单底部弹窗（调整数量 / 确认加菜）
class _PendingItemsSheet extends StatefulWidget {
  const _PendingItemsSheet({required this.cart, required this.onConfirm});

  final OrderDishCart cart;

  /// 执行提交，成功返回 true
  final Future<bool> Function() onConfirm;

  @override
  State<_PendingItemsSheet> createState() => _PendingItemsSheetState();
}

class _PendingItemsSheetState extends State<_PendingItemsSheet> {
  bool _busy = false;

  Future<void> _confirm() async {
    if (widget.cart.items.isEmpty || _busy) return;
    setState(() => _busy = true);
    final ok = await widget.onConfirm();
    if (!mounted) return;
    if (ok) {
      Navigator.of(context).pop(true);
    } else {
      setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Text(
                  '待加菜品',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(width: 8),
                ListenableBuilder(
                  listenable: widget.cart,
                  builder: (context, _) => Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
                    decoration: BoxDecoration(
                      color: AppColors.primaryLight,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      '${widget.cart.totalCount} 件',
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            ConstrainedBox(
              // 弹窗高度无界，用屏幕高度上限约束，菜品过多时内部滚动
              constraints: BoxConstraints(
                maxHeight: MediaQuery.of(context).size.height * 0.45,
              ),
              child: ListenableBuilder(
                listenable: widget.cart,
                builder: (context, _) {
                  if (widget.cart.items.isEmpty) {
                    return const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Text(
                        '尚未选择菜品',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                            fontSize: 13, color: AppColors.textHint),
                      ),
                    );
                  }
                  return ListView(
                    shrinkWrap: true,
                    children: [
                      for (var i = 0; i < widget.cart.items.length; i++)
                        DishCartRow(
                          item: widget.cart.items[i],
                          onDecrease: () =>
                              setState(() => widget.cart.changeQty(i, -1)),
                          onIncrease: () =>
                              setState(() => widget.cart.changeQty(i, 1)),
                        ),
                      const Divider(height: 18),
                      Row(
                        children: [
                          const Text(
                            '加菜合计',
                            style: TextStyle(
                              fontSize: 14,
                              color: AppColors.textSecondary,
                            ),
                          ),
                          const Spacer(),
                          Text(
                            MoneyFormat.yuan(widget.cart.totalPrice),
                            style: const TextStyle(
                              fontSize: 19,
                              fontWeight: FontWeight.w700,
                              color: AppColors.primary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  );
                },
              ),
            ),
            const SizedBox(height: 12),
            ListenableBuilder(
              listenable: widget.cart,
              builder: (context, _) => PrimaryButton(
                label: '确认加菜',
                busy: _busy,
                onPressed: widget.cart.items.isEmpty || _busy
                    ? null
                    : _confirm,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
