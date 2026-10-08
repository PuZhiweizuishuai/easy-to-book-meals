import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/time_format.dart';
import '../../../data/models/models.dart';
import '../../../providers/core_providers.dart';
import '../../../providers/refresh_bus.dart';
import '../../../providers/session_provider.dart';
import '../../../shared/widgets/app_card.dart';
import '../../../shared/widgets/app_feedback.dart';
import '../../../shared/widgets/state_views.dart';

/// 预约时间线区块（可复用）
///
/// 嵌入餐桌管理页底部，按日期分页展示本店预约，支持快捷日期切换、
/// 详情展开与状态操作（确认 / 到店 / 完成 / 取消）。监听数据刷新总线，
/// 在餐桌状态变更或预约创建后自动重新拉取当前页。
class ReservationTimelineSection extends ConsumerStatefulWidget {
  const ReservationTimelineSection({
    super.key,
    required this.storeId,
  });

  final int storeId;

  @override
  ConsumerState<ReservationTimelineSection> createState() =>
      _ReservationTimelineSectionState();
}

class _ReservationTimelineSectionState
    extends ConsumerState<ReservationTimelineSection> {
  DateTime _selectedDate = DateTime.now();
  int _page = 1;
  static const int _pageSize = 10;

  List<TableReservation> _items = const [];
  int _totalPages = 1;
  int _totalElements = 0;
  bool _loading = true;
  String? _error;
  int? _expandedId;
  final Set<int> _busy = <int>{};

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    // 监听刷新总线：餐桌状态变更 / 预约创建都会 bump，这里重拉当前页
    ref.watch(dataRefreshProvider);
    return AppCard(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(),
          const SizedBox(height: 10),
          _buildDateBar(),
          const SizedBox(height: 10),
          _buildBody(),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Container(
          width: 4,
          height: 16,
          decoration: BoxDecoration(
            color: AppColors.warning,
            borderRadius: BorderRadius.circular(2),
          ),
        ),
        const SizedBox(width: 8),
        const Text(
          '预约时间线',
          style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w600),
        ),
        const Spacer(),
        if (_totalElements > 0)
          Text(
            '共 $_totalElements 条',
            style: const TextStyle(fontSize: 12, color: AppColors.textHint),
          ),
      ],
    );
  }

  Widget _buildDateBar() {
    final now = DateTime.now();
    final shortcuts = <_DateShortcut>[
      _DateShortcut(
        label: '昨天',
        date: DateTime(now.year, now.month, now.day - 1),
      ),
      _DateShortcut(
        label: '今天',
        date: DateTime(now.year, now.month, now.day),
      ),
      _DateShortcut(
        label: '明天',
        date: DateTime(now.year, now.month, now.day + 1),
      ),
    ];
    return Wrap(
      spacing: 6,
      runSpacing: 6,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        for (final s in shortcuts)
          _DateChip(
            label: s.label,
            selected: _isSameDay(_selectedDate, s.date),
            onTap: () => _changeDate(s.date),
          ),
        _DateChip(
          label: _formatChipDate(_selectedDate),
          icon: Icons.calendar_today_outlined,
          selected: false,
          onTap: _pickDate,
        ),
      ],
    );
  }

  Widget _buildBody() {
    if (_loading && _items.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20),
        child: Center(
          child: SizedBox(
            width: 22,
            height: 22,
            child: CircularProgressIndicator(strokeWidth: 2.4),
          ),
        ),
      );
    }
    if (_error != null && _items.isEmpty) {
      return ErrorView(
        message: _error!,
        onRetry: _load,
        icon: Icons.cloud_off_outlined,
      );
    }
    if (_items.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 16),
        child: EmptyView(
          title: '当日暂无预约记录',
          compact: true,
          icon: Icons.event_available_outlined,
        ),
      );
    }
    return Column(
      children: [
        for (final r in _items)
          _TimelineTile(
            reservation: r,
            expanded: _expandedId == r.id,
            busy: _busy.contains(r.id),
            onToggle: () => setState(
              () => _expandedId = _expandedId == r.id ? null : r.id,
            ),
            onConfirm: () => _runAction(
              r,
              label: '确认',
              action: (id, storeId) => ref
                  .read(tableReservationApiProvider)
                  .confirm(reservationId: id, storeId: storeId),
            ),
            onSeat: () => _runAction(
              r,
              label: '到店',
              action: (id, storeId) => ref
                  .read(tableReservationApiProvider)
                  .seat(reservationId: id, storeId: storeId),
            ),
            onComplete: () => _runAction(
              r,
              label: '完成',
              action: (id, storeId) => ref
                  .read(tableReservationApiProvider)
                  .complete(reservationId: id, storeId: storeId),
            ),
            onCancel: () => _runAction(
              r,
              label: '取消',
              action: (id, storeId) => ref
                  .read(tableReservationApiProvider)
                  .cancel(reservationId: id, storeId: storeId),
            ),
          ),
        if (_totalPages > 1) _buildPager(),
      ],
    );
  }

  Widget _buildPager() {
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '$_page / $_totalPages 页',
            style: const TextStyle(fontSize: 12, color: AppColors.textHint),
          ),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              IconButton(
                icon: const Icon(Icons.chevron_left, size: 20),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
                onPressed: _page > 1 ? () => _changePage(_page - 1) : null,
              ),
              IconButton(
                icon: const Icon(Icons.chevron_right, size: 20),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
                onPressed: _page < _totalPages
                    ? () => _changePage(_page + 1)
                    : null,
              ),
            ],
          ),
        ],
      ),
    );
  }

  // ==================== 数据加载 ====================

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final range = _dayRange(_selectedDate);
      final page = await ref.read(tableReservationApiProvider).listByDate(
            storeId: widget.storeId,
            startTime: range.start,
            endTime: range.end,
            page: _page,
            size: _pageSize,
          );
      if (!mounted) return;
      setState(() {
        _items = page.content;
        _totalPages = page.totalPages;
        _totalElements = page.totalElements;
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

  Future<void> _runAction(
    TableReservation r, {
    required String label,
    required Future<TableReservation?> Function(int id, int storeId) action,
  }) async {
    if (r.id == null) return;
    setState(() => _busy.add(r.id!));
    try {
      await action(r.id!, widget.storeId);
      if (!mounted) return;
      AppToast.success(context, '已$label');
      // 操作影响餐桌状态，通知常驻页刷新
      ref.read(dataRefreshProvider.notifier).bump();
      await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '操作失败');
    } finally {
      if (mounted) setState(() => _busy.remove(r.id));
    }
  }

  void _changeDate(DateTime date) {
    if (_isSameDay(_selectedDate, date)) return;
    setState(() {
      _selectedDate = date;
      _page = 1;
      _expandedId = null;
    });
    _load();
  }

  void _changePage(int page) {
    if (page == _page) return;
    _page = page;
    _expandedId = null;
    _load();
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2020),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (picked != null) _changeDate(picked);
  }

  // ==================== 日期工具 ====================

  static ({int start, int end}) _dayRange(DateTime date) {
    final start = DateTime(date.year, date.month, date.day);
    final end = start.add(const Duration(days: 1));
    return (
      start: start.millisecondsSinceEpoch,
      end: end.millisecondsSinceEpoch,
    );
  }

  static bool _isSameDay(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  static String _formatChipDate(DateTime d) =>
      '${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}

/// 时间线条目
class _TimelineTile extends StatelessWidget {
  const _TimelineTile({
    required this.reservation,
    required this.expanded,
    required this.busy,
    required this.onToggle,
    required this.onConfirm,
    required this.onSeat,
    required this.onComplete,
    required this.onCancel,
  });

  final TableReservation reservation;
  final bool expanded;
  final bool busy;
  final VoidCallback onToggle;
  final VoidCallback onConfirm;
  final VoidCallback onSeat;
  final VoidCallback onComplete;
  final VoidCallback onCancel;

  @override
  Widget build(BuildContext context) {
    final r = reservation;
    final status = r.statusEnum;
    final dim = status.inactive;
    return Opacity(
      opacity: dim ? 0.6 : 1,
      child: Container(
        margin: const EdgeInsets.only(top: 8),
        padding: const EdgeInsets.fromLTRB(10, 10, 10, 10),
        decoration: BoxDecoration(
          color: const Color(0xFFFAFBFC),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: AppColors.divider),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(top: 3),
              child: Container(
                width: 10,
                height: 10,
                decoration: BoxDecoration(
                  color: status.color.withValues(alpha: 0.25),
                  shape: BoxShape.circle,
                  border: Border.all(color: status.color, width: 2),
                ),
              ),
            ),
            const SizedBox(width: 9),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          r.contactName ?? '预订人',
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      Text(
                        '${r.guestCount ?? 0}人',
                        style: const TextStyle(
                            fontSize: 12, color: AppColors.textHint),
                      ),
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: status.background,
                          borderRadius: BorderRadius.circular(5),
                        ),
                        child: Text(
                          status.label,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: status.color,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 3),
                  Text(
                    '${TimeFormat.dateTime(r.reservedStartTime)}'
                    ' - ${TimeFormat.time(r.reservedEndTime)}',
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                      fontFamily: 'monospace',
                    ),
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
                  if (expanded) ...[
                    const SizedBox(height: 6),
                    const Divider(height: 1),
                    const SizedBox(height: 6),
                    _ExtraRow(label: '预约编号', value: '#${r.id ?? '-'}'),
                    _ExtraRow(
                        label: '餐桌', value: '#${r.tableId ?? '-'}'),
                    _ExtraRow(
                      label: '开始',
                      value: TimeFormat.dateTime(r.reservedStartTime),
                    ),
                    _ExtraRow(
                      label: '结束',
                      value: TimeFormat.dateTime(r.reservedEndTime),
                    ),
                    if (r.remark?.isNotEmpty == true)
                      _ExtraRow(label: '备注', value: r.remark!),
                    if (r.createTime != null)
                      _ExtraRow(
                        label: '创建',
                        value: TimeFormat.dateTime(r.createTime),
                      ),
                  ],
                  const SizedBox(height: 6),
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
                    children: [
                      _MiniTextBtn(
                        label: expanded ? '收起' : '详情',
                        color: AppColors.textSecondary,
                        onTap: onToggle,
                      ),
                      if (!dim) ...[
                        if (status == ReservationStatus.pending)
                          _MiniTextBtn(
                            label: '确认',
                            color: AppColors.success,
                            busy: busy,
                            filled: true,
                            onTap: onConfirm,
                          ),
                        if (status == ReservationStatus.pending ||
                            status == ReservationStatus.confirmed) ...[
                          _MiniTextBtn(
                            label: '到店',
                            color: AppColors.info,
                            busy: busy,
                            onTap: onSeat,
                          ),
                          _MiniTextBtn(
                            label: '取消',
                            color: AppColors.danger,
                            busy: busy,
                            onTap: onCancel,
                          ),
                        ],
                        if (status == ReservationStatus.seated)
                          _MiniTextBtn(
                            label: '完成',
                            color: AppColors.primary,
                            busy: busy,
                            filled: true,
                            onTap: onComplete,
                          ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ExtraRow extends StatelessWidget {
  const _ExtraRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 1.5),
      child: Row(
        children: [
          SizedBox(
            width: 48,
            child: Text(
              label,
              style:
                  const TextStyle(fontSize: 11.5, color: AppColors.textHint),
            ),
          ),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(
                fontSize: 11.5,
                color: AppColors.textSecondary,
                fontFamily: 'monospace',
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MiniTextBtn extends StatelessWidget {
  const _MiniTextBtn({
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
      height: 28,
      child: filled
          ? FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: color,
                foregroundColor: Colors.white,
                minimumSize: const Size(0, 28),
                padding: const EdgeInsets.symmetric(horizontal: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(7),
                ),
              ),
              onPressed: busy ? null : onTap,
              child: busy
                  ? const SizedBox(
                      width: 12,
                      height: 12,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : Text(label, style: const TextStyle(fontSize: 12)),
            )
          : OutlinedButton(
              style: OutlinedButton.styleFrom(
                foregroundColor: color,
                minimumSize: const Size(0, 28),
                padding: const EdgeInsets.symmetric(horizontal: 10),
                side: BorderSide(color: color.withValues(alpha: 0.5)),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(7),
                ),
              ),
              onPressed: busy ? null : onTap,
              child: Text(label, style: const TextStyle(fontSize: 12)),
            ),
    );
  }
}

/// 日期快捷项
class _DateShortcut {
  const _DateShortcut({required this.label, required this.date});

  final String label;
  final DateTime date;
}

/// 日期筛选芯片
class _DateChip extends StatelessWidget {
  const _DateChip({
    required this.label,
    required this.selected,
    required this.onTap,
    this.icon,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final base = AppColors.warning;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: selected ? base.withValues(alpha: 0.12) : const Color(0xFFF2F4F8),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
              color: selected ? base : Colors.transparent, width: 1),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 12, color: selected ? base : AppColors.textHint),
              const SizedBox(width: 4),
            ],
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
                color: selected ? base : AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
