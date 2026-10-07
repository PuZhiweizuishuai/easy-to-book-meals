import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import '../../providers/alert_provider.dart';
import '../../providers/core_providers.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/paged_list_view.dart';
import '../../shared/widgets/search_field.dart';
import '../../shared/widgets/state_views.dart';
import 'order_ui.dart';
import 'order_add_items_page.dart';
import 'order_create_page.dart';
import 'order_detail_page.dart';
import 'widgets/order_card.dart';

/// 订单管理页
///
/// 支持按状态筛选与订单号搜索，列表内的操作按钮随订单状态变化：
/// 待接单 → 接单 / 拒单；已接单 → 开始制作；制作中/已出餐 → 明细中出餐；
/// 已出餐 → 结算（结算页支持扫码核销优惠券）。
class OrderListPage extends ConsumerStatefulWidget {
  const OrderListPage({super.key});

  @override
  ConsumerState<OrderListPage> createState() => _OrderListPageState();
}

class _OrderListPageState extends ConsumerState<OrderListPage> {
  final PagedListController<OrderData> _listController =
      PagedListController<OrderData>();

  /// 当前筛选状态（null 表示全部）
  int? _statusFilter;

  /// 搜索关键字（订单号 / 手机号）
  String _keyword = '';

  /// 正在处理的订单 ID（用于按钮 loading）
  int? _busyOrderId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // 进入列表视为已读
      ref.read(alertProvider.notifier).clearBadge(AlertKind.newOrder);
    });
  }

  int? get _storeId => ref.read(currentStoreIdProvider);

  Future<PageResult<OrderData>> _fetch(int page) async {
    final storeId = _storeId;
    if (storeId == null) {
      throw ApiException('暂无可用门店');
    }
    return ref.read(orderApiProvider).list(
          storeId: storeId,
          status: _statusFilter,
          key: _keyword.isEmpty ? null : _keyword,
          page: page,
          size: 10,
        );
  }

  /// 执行一个订单操作，成功后刷新列表
  Future<void> _run(
    int orderId,
    Future<OrderData?> Function() action, {
    String? successText,
  }) async {
    setState(() => _busyOrderId = orderId);
    try {
      await action();
      if (!mounted) return;
      if (successText != null) AppToast.success(context, successText);
      _listController.refresh();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
    } catch (e) {
      if (!mounted) return;
      AppToast.error(context, '操作失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _busyOrderId = null);
    }
  }

  Future<void> _confirm(OrderData order) async {
    final storeId = _storeId;
    if (storeId == null || order.id == null) return;
    await _run(
      order.id!,
      () => ref
          .read(orderApiProvider)
          .confirm(orderId: order.id!, storeId: storeId),
      successText: '已接单',
    );
  }

  Future<void> _reject(OrderData order) async {
    final storeId = _storeId;
    if (storeId == null || order.id == null) return;
    final reason = await _askReason('拒单原因', '店家拒单');
    if (!mounted || reason == null) return;
    await _run(
      order.id!,
      () => ref.read(orderApiProvider).reject(
            orderId: order.id!,
            storeId: storeId,
            reason: reason,
          ),
      successText: '已拒单',
    );
  }

  Future<void> _cooking(OrderData order) async {
    final storeId = _storeId;
    if (storeId == null || order.id == null) return;
    await _run(
      order.id!,
      () => ref
          .read(orderApiProvider)
          .cooking(orderId: order.id!, storeId: storeId),
      successText: '已开始制作',
    );
  }

  /// 弹出原因输入
  Future<String?> _askReason(String title, String defaultValue) async {
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
                maxLines: 2,
                decoration: const InputDecoration(labelText: '原因'),
              ),
              const SizedBox(height: 14),
              FilledButton(
                onPressed: () => Navigator.of(sheetContext)
                    .pop(controller.text.trim().isEmpty
                        ? defaultValue
                        : controller.text.trim()),
                child: const Text('确定'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 底部操作按钮
  List<Widget> _actionsFor(OrderData order) {
    final busy = _busyOrderId == order.id;
    // 与 Web 端 canManage 一致：未完成订单（status < 4）展示加菜入口
    final canAddItems = order.statusEnum != OrderStatus.completed &&
        order.statusEnum != OrderStatus.cancelled &&
        (ref.read(currentUserProvider)?.hasAnyPermission(
                const ['order:manage']) ??
            false);
    final addItemsAction = canAddItems
        ? Padding(
            padding: const EdgeInsets.only(right: 10),
            child: _TextAction(
              label: '加菜',
              color: AppColors.info,
              busy: false,
              onPressed: () => _openAddItems(order),
            ),
          )
        : const SizedBox.shrink();
    switch (order.statusEnum) {
      case OrderStatus.pending:
        return [
          addItemsAction,
          _TextAction(
            label: '拒单',
            color: AppColors.danger,
            busy: busy,
            onPressed: () => _reject(order),
          ),
          const SizedBox(width: 10),
          _FilledAction(
            label: '接单',
            busy: busy,
            onPressed: () => _confirm(order),
          ),
        ];
      case OrderStatus.confirmed:
        return [
          addItemsAction,
          _FilledAction(
            label: '开始制作',
            busy: busy,
            onPressed: () => _cooking(order),
          ),
        ];
      case OrderStatus.cooking:
        return [addItemsAction];
      case OrderStatus.ready:
        return [
          addItemsAction,
          _FilledAction(
            label: '结算',
            busy: busy,
            onPressed: () => _openDetail(order, focusCheckout: true),
          ),
        ];
      default:
        return const [];
    }
  }

  Future<void> _openDetail(OrderData order, {bool focusCheckout = false}) async {
    final storeId = _storeId;
    final orderId = order.id;
    if (storeId == null || orderId == null) return;
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => OrderDetailPage(
          orderId: orderId,
          storeId: storeId,
          focusCheckout: focusCheckout,
        ),
      ),
    );
    if (changed == true) _listController.refresh();
  }

  /// 打开协助下单页，下单成功后刷新列表
  Future<void> _openStaffCreate() async {
    final created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(builder: (_) => const StaffOrderCreatePage()),
    );
    if (created == true) _listController.refresh();
  }

  /// 打开加菜页，加菜成功后刷新列表
  Future<void> _openAddItems(OrderData order) async {
    final storeId = _storeId;
    if (storeId == null || order.id == null) return;
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) =>
            OrderAddItemsPage(orderId: order.id!, storeId: storeId),
      ),
    );
    if (changed == true) _listController.refresh();
  }

  @override
  Widget build(BuildContext context) {
    final storeId = ref.watch(currentStoreIdProvider);
    // 与 Web 端 canManage 一致：仅拥有 order:manage 权限时展示协助下单入口
    final canStaffCreate =
        ref.watch(currentUserProvider)?.hasAnyPermission(
                const ['order:manage']) ??
            false;

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: const Text('订单管理'),
        actions: [
          if (canStaffCreate)
            IconButton(
              tooltip: '协助下单',
              icon: const Icon(Icons.post_add, size: 24),
              onPressed: _openStaffCreate,
            ),
          IconButton(
            tooltip: '刷新',
            icon: const Icon(Icons.refresh_rounded, size: 22),
            onPressed: () => _listController.refresh(),
          ),
        ],
      ),
      body: storeId == null
          ? const EmptyView(title: '请先选择门店')
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(14, 10, 14, 6),
                  child: SearchField(
                    hint: '搜索订单号 / 手机号',
                    onSearch: (value) {
                      setState(() => _keyword = value.trim());
                    },
                  ),
                ),
                SizedBox(
                  height: 38,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    children: [
                      _FilterChip(
                        label: '全部',
                        selected: _statusFilter == null,
                        onTap: () => setState(() => _statusFilter = null),
                      ),
                      for (final status in [
                        OrderStatus.pending,
                        OrderStatus.confirmed,
                        OrderStatus.cooking,
                        OrderStatus.ready,
                        OrderStatus.completed,
                        OrderStatus.cancelled,
                      ])
                        _FilterChip(
                          label: status.label,
                          selected: _statusFilter == status.code,
                          color: OrderStatusStyle.colorOf(status),
                          onTap: () =>
                              setState(() => _statusFilter = status.code),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 6),
                Expanded(
                  child: PagedListView<OrderData>(
                    controller: _listController,
                    reloadKey: '${_statusFilter ?? 'all'}#$_keyword',
                    fetcher: _fetch,
                    emptyTitle: '暂无订单',
                    emptySubtitle: '换个筛选条件或下拉刷新试试',
                    itemBuilder: (context, order, index) => OrderCard(
                      order: order,
                      actions: _actionsFor(order),
                      onTap: () => _openDetail(order),
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}

/// 筛选芯片
class _FilterChip extends StatelessWidget {
  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
    this.color,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final base = color ?? AppColors.primary;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 14),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? base.withValues(alpha: 0.12) : const Color(0xFFF2F4F8),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: selected ? base : Colors.transparent,
            width: 1,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 13,
            fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
            color: selected ? base : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }
}

/// 卡片底部的文字操作
class _TextAction extends StatelessWidget {
  const _TextAction({
    required this.label,
    required this.color,
    required this.onPressed,
    this.busy = false,
  });

  final String label;
  final Color color;
  final VoidCallback onPressed;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 34,
      child: OutlinedButton(
        style: OutlinedButton.styleFrom(
          foregroundColor: color,
          side: BorderSide(color: color.withValues(alpha: 0.5)),
          // 覆盖主题里的 Size.fromHeight(46)（宽度为 infinity），
          // 否则放进 Row 时会被迫无限宽导致布局崩溃
          minimumSize: const Size(0, 34),
          padding: const EdgeInsets.symmetric(horizontal: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(9)),
        ),
        onPressed: busy ? null : onPressed,
        child: busy
            ? SizedBox(
                width: 14,
                height: 14,
                child: CircularProgressIndicator(strokeWidth: 2, color: color),
              )
            : Text(label, style: const TextStyle(fontSize: 13)),
      ),
    );
  }
}

/// 卡片底部的实心操作
class _FilledAction extends StatelessWidget {
  const _FilledAction({
    required this.label,
    required this.onPressed,
    this.busy = false,
  });

  final String label;
  final VoidCallback onPressed;
  final bool busy;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 34,
      child: FilledButton(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.primary,
          // 覆盖主题里的 Size.fromHeight(46)（宽度为 infinity），
          // 否则放进 Row 时会被迫无限宽导致布局崩溃
          minimumSize: const Size(0, 34),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(9)),
        ),
        onPressed: busy ? null : onPressed,
        child: busy
            ? const SizedBox(
                width: 14,
                height: 14,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              )
            : Text(label, style: const TextStyle(fontSize: 13)),
      ),
    );
  }
}
