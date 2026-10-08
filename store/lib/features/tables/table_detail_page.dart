import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/state_views.dart';
import '../orders/order_ui.dart';
import 'widgets/reservation_sheet.dart';
import 'widgets/table_tile.dart';

/// 餐桌详情页
///
/// 展示单张餐桌的完整信息：基础属性、当前状态与快捷状态切换、
/// 当前关联订单（含菜品明细与金额）、本桌进行中的预约（可确认/到店/完成/取消）。
/// 对应 Web 端 TableInfo.vue + ReservationTimeline 的合并视图。
class TableDetailPage extends ConsumerStatefulWidget {
  const TableDetailPage({
    super.key,
    required this.tableId,
    required this.storeId,
    this.tableNumber,
  });

  final int tableId;
  final int storeId;

  /// 桌号快照（列表跳转时带入，AppBar 先显示，详情加载后更新）
  final String? tableNumber;

  @override
  ConsumerState<TableDetailPage> createState() => _TableDetailPageState();
}

class _TableDetailPageState extends ConsumerState<TableDetailPage> {
  DiningTable? _table;
  OrderData? _currentOrder;
  List<TableReservation> _reservations = const [];
  bool _loading = true;
  String? _error;
  bool _acting = false;
  bool _orderLoading = false;
  final Set<int> _busyReservation = <int>{};

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
      final api = ref.read(tableApiProvider);
      final table = await api.detail(
        id: widget.tableId,
        storeId: widget.storeId,
      );
      if (!mounted) return;
      _table = table;
      _loading = false;
      // 并行：当前订单 + 本桌预约
      if (table?.currentOrderId != null) {
        unawaitedOrder(table!.currentOrderId!);
      } else {
        _currentOrder = null;
      }
      unawaitedReservations();
      if (mounted) setState(() {});
    } on ApiException catch (e) {
      if (!mounted) return;
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

  /// 加载当前关联订单
  void unawaitedOrder(int orderId) {
    _orderLoading = true;
    ref
        .read(orderApiProvider)
        .detail(orderId: orderId, storeId: widget.storeId)
        .then((order) {
      if (!mounted) return;
      setState(() {
        _currentOrder = order;
        _orderLoading = false;
      });
    }).catchError((Object e) {
      if (!mounted) return;
      setState(() {
        _currentOrder = null;
        _orderLoading = false;
      });
    });
  }

  /// 加载本桌进行中的预约（待确认 + 已确认 + 已到店，按开始时间升序）
  ///
  /// 后端 /list/table 只支持单个状态，这里并发拉取三种活跃状态后合并；
  /// 必须包含「已到店」，否则落座后这条预约在本页消失，无法点「完成」。
  void unawaitedReservations() async {
    final api = ref.read(tableReservationApiProvider);
    try {
      final results = await Future.wait(
        [
          ReservationStatus.pending,
          ReservationStatus.confirmed,
          ReservationStatus.seated,
        ].map(
          (status) => api.listByTable(
            tableId: widget.tableId,
            storeId: widget.storeId,
            status: status.code,
          ),
        ),
      );
      final merged = <TableReservation>[
        for (final list in results) ...list,
      ]..sort((a, b) => (a.reservedStartTime ?? 0)
          .compareTo(b.reservedStartTime ?? 0));
      if (!mounted) return;
      setState(() => _reservations = merged);
    } on ApiException {
      if (!mounted) return;
      // 预约加载失败不阻塞整体页面，静默置空
      setState(() => _reservations = const []);
    } catch (_) {
      if (!mounted) return;
      setState(() => _reservations = const []);
    }
  }

  Future<void> _changeStatus(int status) async {
    final table = _table;
    if (table == null || table.status == status) return;
    setState(() => _acting = true);
    try {
      final updated = await ref.read(tableApiProvider).updateStatus(
            id: table.id!,
            storeId: widget.storeId,
            status: status,
          );
      if (!mounted) return;
      setState(() => _table = updated ?? table);
      AppToast.success(context, '状态已更新');
      ref.read(dataRefreshProvider.notifier).bump();
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '操作失败');
    } finally {
      if (mounted) setState(() => _acting = false);
    }
  }

  Future<void> _reservationAction(
    TableReservation r, {
    required String label,
    required Future<TableReservation?> Function(int id, int storeId) action,
  }) async {
    setState(() => _busyReservation.add(r.id!));
    try {
      await action(r.id!, widget.storeId);
      if (!mounted) return;
      AppToast.success(context, '已$label');
      ref.read(dataRefreshProvider.notifier).bump();
      // 刷新本桌订单与预约：到店会绑定订单、完成会清空订单
      final table = _table;
      if (table != null) {
        await _load();
      } else {
        unawaitedReservations();
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '操作失败');
    } finally {
      if (mounted) setState(() => _busyReservation.remove(r.id));
    }
  }

  Future<void> _openReserve() async {
    final table = _table;
    if (table == null) return;
    final ok = await ReservationSheet.show(
      context,
      table: table,
      storeId: widget.storeId,
    );
    if (ok == true && mounted) {
      await _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    final table = _table;
    final title = table?.tableNumber ?? widget.tableNumber ?? '餐桌详情';
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
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
              : table == null
                  ? const EmptyView(title: '餐桌不存在或无权查看')
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView(
                        padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
                        children: [
                          _buildBaseCard(table),
                          const SizedBox(height: 12),
                          _buildOrderCard(table),
                          const SizedBox(height: 12),
                          _buildReservationCard(),
                        ],
                      ),
                    ),
      bottomNavigationBar: table == null
          ? null
          : SafeArea(
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
                decoration: const BoxDecoration(
                  color: Colors.white,
                  border: Border(top: BorderSide(color: AppColors.divider)),
                ),
                child: PrimaryButton(
                  label: '创建预约',
                  icon: Symbols.event_available,
                  backgroundColor: AppColors.warning,
                  onPressed: _acting ? null : _openReserve,
                ),
              ),
            ),
    );
  }

  /// 基础信息 + 状态快捷切换
  Widget _buildBaseCard(DiningTable table) {
    final status = table.statusEnum;
    final color = tableStatusColor(status);
    return AppCard(
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  table.tableNumber ?? '-',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: color.withValues(alpha: 0.5)),
                ),
                child: Text(
                  status.label,
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: color,
                  ),
                ),
              ),
            ],
          ),
          const Divider(height: 16),
          InfoRow(label: '区域', value: table.areaText),
          InfoRow(label: '座位数', value: '${table.capacity ?? 0} 人'),
          InfoRow(label: '桌码', value: table.qrCodeKey ?? '未生成'),
          if (table.remark?.isNotEmpty == true)
            InfoRow(label: '备注', value: table.remark!, showDivider: false),
          const SizedBox(height: 6),
          if (table.currentOrderId != null)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Row(
                children: [
                  const Icon(Symbols.restaurant_menu,
                      size: 16, color: AppColors.warning),
                  const SizedBox(width: 6),
                  Text(
                    '当前有进行中的订单 #${table.currentOrderId}',
                    style: const TextStyle(
                      fontSize: 13,
                      color: AppColors.warning,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
          const Divider(height: 16),
          SizedBox(
            width: double.infinity,
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _statusChip(
                  label: '空闲',
                  selected: table.status == TableStatus.free.code,
                  color: AppColors.success,
                  onTap: _acting ? null : () => _changeStatus(0),
                ),
                _statusChip(
                  label: '清理中',
                  selected: table.status == TableStatus.cleaning.code,
                  color: AppColors.info,
                  onTap: _acting ? null : () => _changeStatus(3),
                ),
                _statusChip(
                  label: table.status == TableStatus.disabled.code
                      ? '已停用'
                      : '停用',
                  selected: table.status == TableStatus.disabled.code,
                  color: AppColors.disabled,
                  onTap: _acting ? null : () => _changeStatus(4),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _statusChip({
    required String label,
    required bool selected,
    required Color color,
    required VoidCallback? onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
        decoration: BoxDecoration(
          color: selected ? color.withValues(alpha: 0.12) : Colors.white,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: selected ? color : AppColors.divider,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
            color: selected ? color : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }

  /// 当前关联订单
  Widget _buildOrderCard(DiningTable table) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Symbols.receipt_long,
                  size: 18, color: AppColors.primary),
              const SizedBox(width: 6),
              const Text(
                '当前订单',
                style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w600),
              ),
              const Spacer(),
              if (table.currentOrderId != null)
                const _Dot(color: AppColors.warning, label: '用餐中')
              else
                const _Dot(color: AppColors.success, label: '无占用'),
            ],
          ),
          const Divider(height: 14),
          if (table.currentOrderId == null)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 18),
              child: EmptyView(
                title: '该餐桌当前没有进行中的订单',
                compact: true,
                icon: Icons.receipt_long_outlined,
              ),
            )
          else if (_orderLoading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 18),
              child: Center(
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            )
          else if (_currentOrder == null)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 18),
              child: EmptyView(
                title: '订单数据加载失败',
                compact: true,
                icon: Icons.error_outline,
              ),
            )
          else
            _buildOrderDetail(_currentOrder!),
        ],
      ),
    );
  }

  Widget _buildOrderDetail(OrderData order) {
    final status = order.statusEnum;
    final items = order.validItems;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                order.orderNo ?? '',
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  fontFamily: 'monospace',
                ),
              ),
            ),
            Container(
              padding:
                  const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: OrderStatusStyle.backgroundOf(status),
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                OrderStatusStyle.labelOf(status),
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: OrderStatusStyle.colorOf(status),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          '下单人：${order.userNickname ?? '匿名顾客'}  ·  ${TimeFormat.dateTime(order.createTime)}',
          style: const TextStyle(fontSize: 12.5, color: AppColors.textHint),
        ),
        if (items.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: Text('暂无菜品明细',
                style: TextStyle(fontSize: 13, color: AppColors.textHint)),
          )
        else ...[
          const SizedBox(height: 8),
          for (final item in items)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      item.dishName ?? '未知菜品',
                      style: const TextStyle(
                          fontSize: 13.5, color: AppColors.textPrimary),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  SizedBox(
                    width: 90,
                    child: Text(
                      item.specNames?.isNotEmpty == true
                          ? item.specNames!
                          : '标准',
                      style: const TextStyle(
                          fontSize: 12, color: AppColors.textHint),
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                    ),
                  ),
                  SizedBox(
                    width: 40,
                    child: Text(
                      '×${item.quantityText}',
                      style: const TextStyle(
                          fontSize: 13, color: AppColors.textSecondary),
                    ),
                  ),
                  SizedBox(
                    width: 70,
                    child: Text(
                      MoneyFormat.yuan(item.amount),
                      style: const TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w600,
                          color: AppColors.primary),
                      textAlign: TextAlign.right,
                    ),
                  ),
                ],
              ),
            ),
        ],
        const Divider(height: 14),
        Row(
          children: [
            if (order.remark?.isNotEmpty == true)
              Expanded(
                child: Text(
                  '备注：${order.remark}',
                  style: const TextStyle(
                      fontSize: 12, color: AppColors.textHint),
                  overflow: TextOverflow.ellipsis,
                ),
              )
            else
              const Spacer(),
            const Text('应付', style: TextStyle(
                fontSize: 13, color: AppColors.textSecondary)),
            const SizedBox(width: 6),
            Text(
              MoneyFormat.yuan(order.payAmount ?? 0),
              style: const TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w700,
                color: AppColors.primary,
              ),
            ),
          ],
        ),
      ],
    );
  }

  /// 本桌进行中的预约
  Widget _buildReservationCard() {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Symbols.event_available,
                  size: 18, color: AppColors.warning),
              const SizedBox(width: 6),
              const Text(
                '本桌预约',
                style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w600),
              ),
              const Spacer(),
              Text(
                '${_reservations.length} 条',
                style: const TextStyle(
                    fontSize: 12.5, color: AppColors.textHint),
              ),
            ],
          ),
          const Divider(height: 14),
          if (_reservations.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 14),
              child: EmptyView(
                title: '暂无进行中的预约',
                compact: true,
                icon: Icons.event_available_outlined,
              ),
            )
          else
            for (final r in _reservations)
              _ReservationTile(
                reservation: r,
                busy: _busyReservation.contains(r.id),
                onConfirm: () => _reservationAction(
                  r,
                  label: '确认',
                  action: (id, storeId) => ref
                      .read(tableReservationApiProvider)
                      .confirm(reservationId: id, storeId: storeId),
                ),
                onSeat: () => _reservationAction(
                  r,
                  label: '到店',
                  action: (id, storeId) => ref
                      .read(tableReservationApiProvider)
                      .seat(reservationId: id, storeId: storeId),
                ),
                onComplete: () => _reservationAction(
                  r,
                  label: '完成',
                  action: (id, storeId) => ref
                      .read(tableReservationApiProvider)
                      .complete(reservationId: id, storeId: storeId),
                ),
                onCancel: () => _reservationAction(
                  r,
                  label: '取消',
                  action: (id, storeId) => ref
                      .read(tableReservationApiProvider)
                      .cancel(reservationId: id, storeId: storeId),
                ),
              ),
        ],
      ),
    );
  }
}

/// 状态圆点 + 文案
class _Dot extends StatelessWidget {
  const _Dot({required this.color, required this.label});

  final Color color;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 7,
          height: 7,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 5),
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w500,
            color: color,
          ),
        ),
      ],
    );
  }
}

/// 预约条目（含操作按钮）
class _ReservationTile extends StatelessWidget {
  const _ReservationTile({
    required this.reservation,
    required this.busy,
    required this.onConfirm,
    required this.onSeat,
    required this.onComplete,
    required this.onCancel,
  });

  final TableReservation reservation;
  final bool busy;
  final VoidCallback onConfirm;
  final VoidCallback onSeat;
  final VoidCallback onComplete;
  final VoidCallback onCancel;

  @override
  Widget build(BuildContext context) {
    final r = reservation;
    final status = r.statusEnum;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: BoxDecoration(
                  color: status.color,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 7),
              Expanded(
                child: Text(
                  r.contactName ?? '预订人',
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              Text(
                '${r.guestCount ?? 0} 人',
                style: const TextStyle(
                    fontSize: 12.5, color: AppColors.textHint),
              ),
              const SizedBox(width: 8),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                decoration: BoxDecoration(
                  color: status.background,
                  borderRadius: BorderRadius.circular(5),
                ),
                child: Text(
                  status.label,
                  style: TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w600,
                    color: status.color,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '${TimeFormat.dateTime(r.reservedStartTime)}'
            ' - ${TimeFormat.time(r.reservedEndTime)}',
            style: const TextStyle(
                fontSize: 12.5,
                color: AppColors.textSecondary,
                fontFamily: 'monospace'),
          ),
          if (r.contactPhone?.isNotEmpty == true)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(
                r.contactPhone!,
                style: const TextStyle(
                    fontSize: 12, color: AppColors.textHint),
              ),
            ),
          if (r.remark?.isNotEmpty == true)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(
                r.remark!,
                style: const TextStyle(
                    fontSize: 12, color: AppColors.textHint),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          if (r.active) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: [
                if (status == ReservationStatus.pending)
                  _MiniBtn(
                    label: '确认',
                    color: AppColors.success,
                    busy: busy,
                    onTap: onConfirm,
                  ),
                if (status == ReservationStatus.pending ||
                    status == ReservationStatus.confirmed) ...[
                  _MiniBtn(
                    label: '到店',
                    color: AppColors.info,
                    busy: busy,
                    onTap: onSeat,
                  ),
                  _MiniBtn(
                    label: '取消',
                    color: AppColors.danger,
                    busy: busy,
                    onTap: onCancel,
                  ),
                ],
                if (status == ReservationStatus.seated)
                  _MiniBtn(
                    label: '完成',
                    color: AppColors.primary,
                    busy: busy,
                    filled: true,
                    onTap: onComplete,
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _MiniBtn extends StatelessWidget {
  const _MiniBtn({
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
                          strokeWidth: 2, color: Colors.white),
                    )
                  : Text(label, style: const TextStyle(fontSize: 12.5)),
            )
          : OutlinedButton(
              style: OutlinedButton.styleFrom(
                foregroundColor: color,
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
