import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/search_field.dart';
import '../../shared/widgets/sku_picker_sheet.dart';
import '../../shared/widgets/state_views.dart';
import 'widgets/dish_pick_tile.dart';
import 'widgets/order_dish_cart.dart';

/// 协助下单（店员代客下单）
///
/// 流程：选择菜品（SKU 弹窗选规格/数量）→ 底部购物车栏 → 填写订单信息
/// （订单类型 / 餐桌 / 顾客信息）→ 提交。
/// 提交成功返回 `true`，调用方据此刷新订单列表。
class StaffOrderCreatePage extends ConsumerStatefulWidget {
  const StaffOrderCreatePage({super.key});

  @override
  ConsumerState<StaffOrderCreatePage> createState() =>
      _StaffOrderCreatePageState();
}

class _StaffOrderCreatePageState extends ConsumerState<StaffOrderCreatePage> {
  final OrderDishCart _cart = OrderDishCart();

  List<Dish> _dishes = const [];
  bool _loading = true;
  String? _error;
  String _keyword = '';

  @override
  void initState() {
    super.initState();
    _loadDishes();
  }

  @override
  void dispose() {
    _cart.dispose();
    super.dispose();
  }

  int? get _storeId => ref.read(currentStoreIdProvider);

  Future<void> _loadDishes() async {
    final storeId = _storeId;
    if (storeId == null) {
      setState(() {
        _loading = false;
        _error = '暂无可用门店';
      });
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      // 与 Web 端一致：拉取前 100 条后在前端过滤上架菜品
      final page = await ref.read(dishApiProvider).list(
            storeId: storeId,
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
        _error = '加载菜品失败，请稍后重试';
        _loading = false;
      });
    }
  }

  Future<void> _pickSku(Dish dish) async {
    final result = await SkuPickerSheet.show(
      context,
      dish: dish,
      confirmText: '加入已选菜品',
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
    AppToast.success(context, '已加入');
  }

  Future<void> _openConfirm() async {
    final storeId = _storeId;
    if (storeId == null || _cart.items.isEmpty) return;
    final created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => _OrderConfirmPage(storeId: storeId, cart: _cart),
      ),
    );
    if (created == true && mounted) {
      Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('协助下单'),
        actions: [
          IconButton(
            tooltip: '刷新菜品',
            icon: const Icon(Icons.refresh_rounded, size: 22),
            onPressed: _loading ? null : _loadDishes,
          ),
        ],
      ),
      body: Column(
        children: [
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
                submitLabel: '去填写订单',
                onSubmit: _openConfirm,
              ),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) return const LoadingView();
    if (_error != null) {
      return ErrorView(message: _error!, onRetry: _loadDishes);
    }
    if (_dishes.isEmpty) {
      return const EmptyView(
        title: '暂无可选菜品',
        subtitle: '门店还没有上架菜品，可先在管理页维护',
      );
    }
    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: _loadDishes,
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

// ==================== 订单信息确认页 ====================

class _OrderConfirmPage extends ConsumerStatefulWidget {
  const _OrderConfirmPage({required this.storeId, required this.cart});

  final int storeId;
  final OrderDishCart cart;

  @override
  ConsumerState<_OrderConfirmPage> createState() => _OrderConfirmPageState();
}

class _OrderConfirmPageState extends ConsumerState<_OrderConfirmPage> {
  /// 0-堂食 1-外送 2-线上
  int _orderType = 0;
  int? _tableId;
  List<DiningTable> _tables = const [];
  bool _tableLoading = true;

  final TextEditingController _nameCtrl = TextEditingController();
  final TextEditingController _phoneCtrl = TextEditingController();
  final TextEditingController _addressCtrl = TextEditingController();
  final TextEditingController _remarkCtrl = TextEditingController();

  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _loadTables();
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _phoneCtrl.dispose();
    _addressCtrl.dispose();
    _remarkCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadTables() async {
    try {
      final tables =
          await ref.read(tableApiProvider).listAll(storeId: widget.storeId);
      if (!mounted) return;
      setState(() {
        // 停用桌台不允许绑定（后端条件更新也会拒绝），其余都展示
        _tables = tables.where((t) => t.status != TableStatus.disabled.code).toList();
        _tableLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _tableLoading = false);
    }
  }

  void _changeOrderType(int type) {
    setState(() {
      _orderType = type;
      if (type != 0) _tableId = null;
    });
  }

  Future<void> _submit() async {
    if (widget.cart.items.isEmpty) {
      AppToast.warning(context, '请先选择菜品');
      return;
    }
    if (_orderType == 0 && _tableId == null) {
      AppToast.warning(context, '堂食订单请选择餐桌');
      return;
    }
    final phone = _phoneCtrl.text.trim();
    final address = _addressCtrl.text.trim();
    if (_orderType == 1) {
      if (phone.isEmpty) {
        AppToast.warning(context, '外送订单请填写联系电话');
        return;
      }
      if (address.isEmpty) {
        AppToast.warning(context, '外送订单请填写收货地址');
        return;
      }
    }

    final name = _nameCtrl.text.trim();
    final remark = _remarkCtrl.text.trim();
    final payload = StaffCreateOrderPayload(
      storeId: widget.storeId,
      orderType: _orderType,
      tableId: _orderType == 0 ? _tableId : null,
      customerName: name.isEmpty ? null : name,
      customerPhone: phone.isEmpty ? null : phone,
      // 联系电话随单提交：外送用于配送，堂食/线上用于关联会员积分
      contactPhone: phone.isEmpty ? null : phone,
      deliveryAddress: _orderType == 1 && address.isNotEmpty ? address : null,
      remark: remark.isEmpty ? null : remark,
      items: [
        for (final item in widget.cart.items)
          CreateOrderItemPayload(
              skuId: item.skuId, quantity: item.quantity),
      ],
    );

    setState(() => _submitting = true);
    try {
      await ref.read(orderApiProvider).staffCreate(payload);
      if (!mounted) return;
      // 新订单会影响看板待接单 / 营业额统计，通知常驻页面刷新
      ref.read(dataRefreshProvider.notifier).bump();
      AppToast.success(context, '下单成功');
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '下单失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('填写订单信息')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 20),
        children: [
          _buildInfoCard(),
          const SizedBox(height: 12),
          _buildItemsCard(),
        ],
      ),
      bottomNavigationBar: ListenableBuilder(
        listenable: widget.cart,
        builder: (context, _) => SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
            child: PrimaryButton(
              label: '提交订单',
              busy: _submitting,
              onPressed:
                  widget.cart.items.isEmpty || _submitting ? null : _submit,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildInfoCard() {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            '订单信息',
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),
          const _FieldLabel('订单类型'),
          const SizedBox(height: 7),
          Row(
            children: [
              for (final type in const [
                (0, '堂食'),
                (1, '外送'),
                (2, '线上'),
              ]) ...[
                if (type.$1 != 0) const SizedBox(width: 9),
                Expanded(
                  child: _TypeButton(
                    label: type.$2,
                    selected: _orderType == type.$1,
                    onTap: () => _changeOrderType(type.$1),
                  ),
                ),
              ],
            ],
          ),
          if (_orderType == 0) ...[
            const SizedBox(height: 14),
            const _FieldLabel('选择餐桌', required: true),
            const SizedBox(height: 7),
            _TableDropdown(
              tables: _tables,
              value: _tableId,
              loading: _tableLoading,
              onChanged: (id) => setState(() => _tableId = id),
            ),
            const SizedBox(height: 6),
            const Text(
              '堂食订单需绑定餐桌，下单后餐桌自动置为占用',
              style: TextStyle(fontSize: 11.5, color: AppColors.textHint),
            ),
          ],
          const SizedBox(height: 14),
          TextField(
            controller: _nameCtrl,
            maxLength: 50,
            textInputAction: TextInputAction.next,
            decoration: const InputDecoration(
              labelText: '顾客称呼',
              hintText: '如：到店顾客',
              counterText: '',
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _phoneCtrl,
            keyboardType: TextInputType.phone,
            maxLength: 11,
            textInputAction:
                _orderType == 1 ? TextInputAction.next : TextInputAction.done,
            decoration: InputDecoration(
              labelText: _orderType == 1 ? '联系电话 *' : '联系电话（选填）',
              hintText: _orderType == 1
                  ? '外送必填'
                  : '选填，填写后结算可累积会员积分',
              counterText: '',
            ),
          ),
          if (_orderType == 1) ...[
            const SizedBox(height: 12),
            TextField(
              controller: _addressCtrl,
              maxLines: 2,
              maxLength: 255,
              textInputAction: TextInputAction.next,
              decoration: const InputDecoration(
                labelText: '收货地址',
                hintText: '外送配送地址',
                alignLabelWithHint: true,
              ),
            ),
          ],
          const SizedBox(height: 12),
          TextField(
            controller: _remarkCtrl,
            maxLines: 2,
            maxLength: 255,
            decoration: const InputDecoration(
              labelText: '备注',
              hintText: '如：少辣、不要香菜',
              alignLabelWithHint: true,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildItemsCard() {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const Text(
                '已选菜品',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                ),
              ),
              const Spacer(),
              Container(
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
            ],
          ),
          const SizedBox(height: 8),
          if (widget.cart.items.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 22),
              child: Center(
                child: Text(
                  '尚未选择菜品',
                  style: TextStyle(fontSize: 13, color: AppColors.textHint),
                ),
              ),
            )
          else ...[
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
                  '合计',
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
        ],
      ),
    );
  }
}

/// 表单字段标题（红色 * 表示必填）
class _FieldLabel extends StatelessWidget {
  const _FieldLabel(this.text, {this.required = false});

  final String text;
  final bool required;

  @override
  Widget build(BuildContext context) {
    return RichText(
      text: TextSpan(
        style: const TextStyle(
          fontSize: 13.5,
          color: AppColors.textSecondary,
        ),
        children: [
          TextSpan(text: text),
          if (required)
            const TextSpan(
              text: ' *',
              style: TextStyle(color: AppColors.danger),
            ),
        ],
      ),
    );
  }
}

/// 订单类型选择按钮
class _TypeButton extends StatelessWidget {
  const _TypeButton({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 38,
      child: FilledButton(
        style: FilledButton.styleFrom(
          backgroundColor:
              selected ? AppColors.primary : Colors.white,
          foregroundColor:
              selected ? Colors.white : AppColors.textSecondary,
          elevation: 0,
          padding: EdgeInsets.zero,
          // 覆盖主题 Size.fromHeight(46)（宽度 infinity），显式收敛为内容宽
          minimumSize: const Size(0, 38),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
            side: BorderSide(
              color: selected ? AppColors.primary : AppColors.divider,
            ),
          ),
        ),
        onPressed: onTap,
        child: Text(
          label,
          style: TextStyle(
            fontSize: 13.5,
            fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
          ),
        ),
      ),
    );
  }
}

/// 餐桌下拉选择
class _TableDropdown extends StatelessWidget {
  const _TableDropdown({
    required this.tables,
    required this.value,
    required this.loading,
    required this.onChanged,
  });

  final List<DiningTable> tables;
  final int? value;
  final bool loading;
  final ValueChanged<int?> onChanged;

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const SizedBox(
        height: 46,
        child: Align(
          alignment: Alignment.centerLeft,
          child: SizedBox(
            width: 18,
            height: 18,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        ),
      );
    }
    return DropdownButtonFormField<int?>(
      initialValue: value,
      isExpanded: true,
      decoration: const InputDecoration(
        hintText: '请选择餐桌',
        contentPadding: EdgeInsets.symmetric(horizontal: 12),
      ),
      items: [
        for (final table in tables)
          DropdownMenuItem<int?>(
            value: table.id,
            child: Text(
              '${table.tableNumber ?? '-'}号桌 · '
              '${(table.area?.trim().isEmpty ?? true) ? '未分区' : table.area!.trim()} · '
              '${table.capacity ?? 0}人'
              '${table.busy ? ' · 已占用' : ''}',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 13.5,
                color: AppColors.textPrimary,
              ),
            ),
          ),
      ],
      onChanged: onChanged,
    );
  }
}
