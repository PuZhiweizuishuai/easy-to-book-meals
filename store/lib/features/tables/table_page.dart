import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/state_views.dart';
import 'table_detail_page.dart';
import 'widgets/reservation_sheet.dart';
import 'widgets/reservation_timeline_section.dart';
import 'widgets/table_qr_sheet.dart';
import 'widgets/table_tile.dart';

/// 餐桌管理页
///
/// 网格展示本店餐桌，颜色代表状态；支持新增、编辑、删除与状态切换，
/// 点击卡片右下角二维码图标或菜单「查看二维码」可查看扫码点餐二维码，
/// 长按卡片可复制该桌的扫码点餐标识（qrCodeKey）。
class TablePage extends ConsumerStatefulWidget {
  const TablePage({super.key});

  @override
  ConsumerState<TablePage> createState() => _TablePageState();
}

class _TablePageState extends ConsumerState<TablePage> {
  List<DiningTable> _tables = const [];
  bool _loading = true;
  String? _error;
  int? _statusFilter;
  final Set<int> _busy = <int>{};

  @override
  void initState() {
    super.initState();
    _load();
  }

  int? get _storeId => ref.read(currentStoreIdProvider);

  Future<void> _load() async {
    final storeId = _storeId;
    if (storeId == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await ref.read(tableApiProvider).listAll(storeId: storeId);
      if (!mounted) return;
      setState(() {
        _tables = list;
        _loading = false;
      });
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

  Future<void> _showEditor({DiningTable? table}) async {
    final storeId = _storeId;
    if (storeId == null) return;
    final isEdit = table != null;
    final numberController =
        TextEditingController(text: table?.tableNumber ?? '');
    final capacityController =
        TextEditingController(text: table == null ? '4' : '${table.capacity ?? 4}');
    final areaController = TextEditingController(text: table?.area ?? '');
    final remarkController = TextEditingController(text: table?.remark ?? '');

    final result = await showAppBottomSheet<bool>(
      context: context,
      title: isEdit ? '编辑餐桌' : '新增餐桌',
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: numberController,
                  autofocus: !isEdit,
                  decoration: const InputDecoration(labelText: '餐桌号'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: capacityController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: '座位数'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            controller: areaController,
            decoration: const InputDecoration(labelText: '区域（大厅 / 包间）'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: remarkController,
            decoration: const InputDecoration(labelText: '备注'),
          ),
          const SizedBox(height: 18),
          PrimaryButton(
            label: isEdit ? '保存修改' : '创建餐桌',
            onPressed: () {
              if (numberController.text.trim().isEmpty) {
                AppToast.error(context, '请输入餐桌号');
                return;
              }
              final capacity =
                  int.tryParse(capacityController.text.trim()) ?? 0;
              if (capacity < 1 || capacity > 100) {
                AppToast.error(context, '座位数需在 1~100 之间');
                return;
              }
              Navigator.of(context).pop(true);
            },
          ),
        ],
      ),
    );

    final number = numberController.text.trim();
    final capacity = int.tryParse(capacityController.text.trim()) ?? 4;
    final area = areaController.text.trim();
    final remark = remarkController.text.trim();
    numberController.dispose();
    capacityController.dispose();
    areaController.dispose();
    remarkController.dispose();

    if (!mounted || result != true || number.isEmpty) return;

    setState(() => _loading = true);
    try {
      final api = ref.read(tableApiProvider);
      if (isEdit) {
        await api.update(
          id: table.id!,
          storeId: storeId,
          tableNumber: number,
          capacity: capacity,
          area: area,
          remark: remark,
          status: table.status,
        );
      } else {
        await api.create(
          storeId: storeId,
          tableNumber: number,
          capacity: capacity,
          area: area,
          remark: remark,
        );
      }
      if (!mounted) return;
      AppToast.success(context, isEdit ? '餐桌已更新' : '餐桌已创建');
      await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
      await _load();
    }
  }

  Future<void> _delete(DiningTable table) async {
    final storeId = _storeId;
    if (storeId == null || table.id == null) return;
    final ok = await showConfirmDialog(
      context,
      title: '删除餐桌',
      content: '确定删除「${table.tableNumber}」吗？删除后该桌的扫码点餐二维码将失效。',
      danger: true,
    );
    if (ok != true) return;
    setState(() => _busy.add(table.id!));
    try {
      await ref
          .read(tableApiProvider)
          .delete(id: table.id!, storeId: storeId);
      if (!mounted) return;
      AppToast.success(context, '餐桌已删除');
      await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    } finally {
      if (mounted) setState(() => _busy.remove(table.id));
    }
  }

  Future<void> _changeStatus(DiningTable table, TableStatus status) async {
    final storeId = _storeId;
    if (storeId == null || table.id == null) return;
    setState(() => _busy.add(table.id!));
    try {
      await ref.read(tableApiProvider).updateStatus(
            id: table.id!,
            storeId: storeId,
            status: status.code,
          );
      if (!mounted) return;
      AppToast.success(context, '已置为「${status.label}」');
      await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    } finally {
      if (mounted) setState(() => _busy.remove(table.id));
    }
  }

  /// 打开餐桌详情页（当前状态 / 关联订单 / 本桌预约）
  Future<void> _openDetail(DiningTable table) async {
    if (table.id == null) return;
    await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => TableDetailPage(
          tableId: table.id!,
          storeId: _storeId!,
          tableNumber: table.tableNumber,
        ),
      ),
    );
    // 详情页内可能改状态 / 预约，返回后刷新列表
    if (mounted) _load();
  }

  /// 创建预约
  Future<void> _openReserve(DiningTable table) async {
    final storeId = _storeId;
    if (storeId == null || table.id == null) return;
    final ok = await ReservationSheet.show(
      context,
      table: table,
      storeId: storeId,
    );
    if (ok == true && mounted) {
      await _load();
    }
  }

  /// 弹出状态切换菜单
  Future<void> _showStatusMenu(DiningTable table) async {
    final selected = await showModalBottomSheet<TableStatus>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (sheetContext) => SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 14),
              child: Text(
                '设置餐桌状态',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
            ),
            const Divider(height: 1),
            for (final status in TableStatus.values)
              ListTile(
                title: Text(status.label),
                trailing: table.status == status.code
                    ? const Icon(Icons.check, size: 18, color: AppColors.primary)
                    : null,
                onTap: () => Navigator.of(sheetContext).pop(status),
              ),
          ],
        ),
      ),
    );
    if (!mounted || selected == null) return;
    await _changeStatus(table, selected);
  }

  /// 复制扫码标识
  Future<void> _copyQrKey(DiningTable table) async {
    final key = table.qrCodeKey;
    if (key == null || key.isEmpty) {
      AppToast.warning(context, '该餐桌尚未生成扫码标识');
      return;
    }
    await Clipboard.setData(ClipboardData(text: key));
    if (!mounted) return;
    AppToast.success(context, '已复制扫码标识');
  }

  /// 查看该桌的扫码点餐二维码
  ///
  /// 链接格式与顾客端 Web 保持一致：
  /// `{服务器地址}/store/{门店ID}?table={qrCodeKey}`
  void _showQr(DiningTable table) {
    final serverAddress = ref.read(preferencesProvider).serverAddress;
    final key = table.qrCodeKey;
    final storeId = table.storefrontId ?? _storeId;

    String? scanUrl;
    if (key != null &&
        key.isNotEmpty &&
        storeId != null &&
        serverAddress.isNotEmpty) {
      final base = serverAddress.endsWith('/')
          ? serverAddress.substring(0, serverAddress.length - 1)
          : serverAddress;
      scanUrl = '$base/store/$storeId?table=$key';
    }

    final subtitle = <String>[
      if ((table.capacity ?? 0) > 0) '${table.capacity} 人桌',
      if (table.area?.isNotEmpty == true) table.area!,
    ].join(' · ');

    TableQrSheet.show(
      context,
      tableNumber: table.tableNumber ?? '-',
      scanUrl: scanUrl,
      subtitle: subtitle.isEmpty ? null : subtitle,
      qrCodeKey: key,
    );
  }

  @override
  Widget build(BuildContext context) {
    // 预约创建 / 餐桌状态变更会 bump 总线，重新拉取餐桌列表。
    // ref.listen 只能在 build 中调用（放 initState 会触发 debugDoingBuild 断言）
    ref.listen(dataRefreshProvider, (_, _) => _load());

    final visible = _statusFilter == null
        ? _tables
        : _tables.where((t) => t.status == _statusFilter).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('餐桌管理'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add, size: 24),
            onPressed: () => _showEditor(),
          ),
          IconButton(
            icon: const Icon(Icons.refresh_rounded, size: 22),
            onPressed: _load,
          ),
        ],
      ),
      body: Column(
        children: [
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 14),
              children: [
                _TableFilterChip(
                  label: '全部 (${_tables.length})',
                  selected: _statusFilter == null,
                  onTap: () => setState(() => _statusFilter = null),
                ),
                for (final status in TableStatus.values)
                  _TableFilterChip(
                    label:
                        '${status.label} (${_tables.where((t) => t.status == status.code).length})',
                    selected: _statusFilter == status.code,
                    color: tableStatusColor(status),
                    onTap: () => setState(() => _statusFilter = status.code),
                  ),
              ],
            ),
          ),
          Expanded(child: _buildBody(visible)),
        ],
      ),
    );
  }

  Widget _buildBody(List<DiningTable> visible) {
    final storeId = _storeId;
    // 初始加载 / 全店无数据：用占位视图，不展示时间线（无 storeId）
    if (_loading && _tables.isEmpty) return const LoadingView();
    if (_error != null && _tables.isEmpty) {
      return ErrorView(message: _error!, onRetry: _load);
    }
    if (storeId == null) return const EmptyView(title: '未选择门店');

    final freeCount = _tables.where((t) => t.status == 0).length;
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(14, 10, 14, 24),
        children: [
          _TableSummary(
            total: _tables.length,
            free: freeCount,
            busy: _tables.length - freeCount,
          ),
          const SizedBox(height: 12),
          if (visible.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 18),
              child: EmptyView(
                title: '没有符合条件的餐桌',
                subtitle: '切换筛选或点击右上角 + 新增餐桌',
                compact: true,
                icon: Icons.table_restaurant_outlined,
              ),
            )
          else
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 0.88,
              ),
              itemCount: visible.length,
              itemBuilder: (context, index) {
                final table = visible[index];
                return TableTile(
                  table: table,
                  busy: _busy.contains(table.id),
                  onViewDetail: () => _openDetail(table),
                  onLongPress: () => _copyQrKey(table),
                  onViewQr: () => _showQr(table),
                  onReserve: () => _openReserve(table),
                  onStatus: () => _showStatusMenu(table),
                  onEdit: () => _showEditor(table: table),
                  onDelete: () => _delete(table),
                );
              },
            ),
          const SizedBox(height: 14),
          SectionTitle(
            title: '预约',
            subtitle: '按日期查看与处理本店预约',
            padding: const EdgeInsets.fromLTRB(0, 0, 0, 6),
          ),
          ReservationTimelineSection(storeId: storeId),
        ],
      ),
    );
  }
}

/// 餐桌概览条
class _TableSummary extends StatelessWidget {
  const _TableSummary({
    required this.total,
    required this.free,
    required this.busy,
  });

  final int total;
  final int free;
  final int busy;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: [
          _SummaryItem(
            icon: Symbols.table_restaurant,
            label: '餐桌总数',
            value: '$total',
            color: AppColors.textPrimary,
          ),
          const SizedBox(width: 12),
          _SummaryItem(
            icon: Icons.check_circle_outline,
            label: '空闲',
            value: '$free',
            color: AppColors.success,
          ),
          const SizedBox(width: 12),
          _SummaryItem(
            icon: Icons.remove_circle_outline,
            label: '非空闲',
            value: '$busy',
            color: AppColors.danger,
          ),
        ],
      ),
    );
  }
}

class _SummaryItem extends StatelessWidget {
  const _SummaryItem({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  final IconData icon;
  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Row(
        children: [
          Icon(icon, size: 17, color: color),
          const SizedBox(width: 6),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 11.5,
                    color: AppColors.textHint,
                  ),
                ),
                Text(
                  value,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: color,
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

/// 状态筛选芯片
class _TableFilterChip extends StatelessWidget {
  const _TableFilterChip({
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
        padding: const EdgeInsets.symmetric(horizontal: 13),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected
              ? base.withValues(alpha: 0.12)
              : const Color(0xFFF2F4F8),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: selected ? base : Colors.transparent),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
            color: selected ? base : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }
}
