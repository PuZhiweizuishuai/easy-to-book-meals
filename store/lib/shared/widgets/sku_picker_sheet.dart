import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import 'app_button.dart';
import 'state_views.dart';

/// SKU 选择结果
class SkuPickResult {
  const SkuPickResult({
    required this.sku,
    required this.quantity,
    required this.dealPrice,
    required this.specNamesText,
  });

  /// 选中的 SKU
  final ProductSku sku;

  /// 数量
  final int quantity;

  /// 成交价（含 SKU 折扣）
  final double dealPrice;

  /// 规格名文案（如「大份/微辣」，无规格为「标准」）
  final String specNamesText;
}

/// 菜品规格（SKU）选择底部弹窗
///
/// 与 Web 端 SkuPickerModal 行为一致：按规格分组单选 → 匹配 SKU →
/// 选数量 → 确认。组件只负责「选什么、选多少」，加到购物车还是加菜清单
/// 由调用方决定：
///
/// ```dart
/// final result = await SkuPickerSheet.show(context, dish: dish);
/// ```
class SkuPickerSheet extends ConsumerStatefulWidget {
  const SkuPickerSheet({
    super.key,
    required this.dish,
    this.confirmText = '加入',
  });

  /// 弹出选择弹窗，返回选择结果；取消时为 null
  static Future<SkuPickResult?> show(
    BuildContext context, {
    required Dish dish,
    String confirmText = '加入',
  }) {
    return showModalBottomSheet<SkuPickResult>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => SkuPickerSheet(dish: dish, confirmText: confirmText),
    );
  }

  final Dish dish;
  final String confirmText;

  @override
  ConsumerState<SkuPickerSheet> createState() => _SkuPickerSheetState();
}

class _SkuPickerSheetState extends ConsumerState<SkuPickerSheet> {
  bool _loading = true;
  String? _error;
  List<ProductSku> _skus = const [];
  List<SpecGroupInput> _specGroups = const [];

  /// { groupId: specValueId }
  final Map<int, int> _selectedSpec = {};
  int _quantity = 1;

  Dish get _dish => widget.dish;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final dishId = _dish.id;
    if (dishId == null) {
      setState(() {
        _loading = false;
        _error = '菜品信息异常';
      });
      return;
    }
    try {
      final detail =
          await ref.read(dishApiProvider).detail(dishId: dishId);
      if (!mounted) return;
      final skus = (detail?.skuList ?? const [])
          .where((sku) => sku.status == SkuStatus.on)
          .toList();
      final groups = detail?.specGroups ?? const <SpecGroupInput>[];
      final selected = <int, int>{};
      // 单规格组自动选中第一项，方便直接加购
      if (groups.length == 1) {
        final group = groups.first;
        final firstId = group.values
            .map((v) => v.id)
            .whereType<int>()
            .where((id) => id > 0)
            .firstOrNull;
        if (group.id != null && firstId != null) {
          selected[group.id!] = firstId;
        }
      }
      setState(() {
        _skus = skus;
        _specGroups = groups;
        _selectedSpec
          ..clear()
          ..addAll(selected);
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = '加载规格失败，请稍后重试';
      });
    }
  }

  /// 当前选中的规格组合匹配到的 SKU（无规格菜品取唯一在售 SKU）
  ProductSku? get _matchedSku {
    if (_specGroups.isEmpty) {
      return _skus.isEmpty ? null : _skus.first;
    }
    for (final group in _specGroups) {
      final gid = group.id;
      if (gid == null || _selectedSpec[gid] == null) return null;
    }
    final selectedIds = _selectedSpec.values.toList()..sort();
    for (final sku in _skus) {
      final ids = sku.specIds.toList()..sort();
      if (ids.length == selectedIds.length) {
        var matched = true;
        for (var i = 0; i < ids.length; i++) {
          if (ids[i] != selectedIds[i]) {
            matched = false;
            break;
          }
        }
        if (matched) return sku;
      }
    }
    return null;
  }

  void _selectSpec(int groupId, int valueId) {
    setState(() => _selectedSpec[groupId] = valueId);
  }

  void _changeQuantity(int delta) {
    setState(() {
      _quantity = (_quantity + delta).clamp(1, 99);
    });
  }

  void _confirm() {
    final sku = _matchedSku;
    if (sku == null || sku.id == null) return;
    Navigator.of(context).pop(
      SkuPickResult(
        sku: sku,
        quantity: _quantity,
        dealPrice: sku.dealPrice,
        specNamesText: sku.specNamesText.isEmpty ? '标准' : sku.specNamesText,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return SafeArea(
      top: false,
      child: Padding(
        padding: EdgeInsets.only(bottom: bottomInset),
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.of(context).size.height * 0.82,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _buildHeader(),
              Flexible(
                child: ListView(
                  shrinkWrap: true,
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                  children: [
                    _buildContent(),
                  ],
                ),
              ),
              if (_matchedSku != null) _buildPriceBar(),
              _buildActions(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 14, 8, 8),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _dish.name ?? '选择规格',
                  style: const TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
                if (_dish.descriptionText != null) ...[
                  const SizedBox(height: 3),
                  Text(
                    _dish.descriptionText!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 21),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
    );
  }

  Widget _buildContent() {
    if (_loading) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 36),
        child: LoadingView(message: '规格加载中'),
      );
    }
    if (_error != null) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 20),
        child: Center(
          child: Text(_error!,
              style: const TextStyle(
                fontSize: 13.5,
                color: AppColors.textSecondary,
              )),
        ),
      );
    }
    if (_specGroups.isEmpty && _skus.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 28),
        child: Center(
          child: Text(
            '该菜品暂无可选规格',
            style: TextStyle(fontSize: 13.5, color: AppColors.textHint),
          ),
        ),
      );
    }
    // 无规格菜品无需展示规格区，价格与数量统一由底部面板承载
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final group in _specGroups) ...[
          if (group.id != null) _buildSpecGroup(group),
        ],
        if (_specGroups.isNotEmpty && _matchedSku == null)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 18),
            child: Center(
              child: Text(
                '请选择完整规格组合',
                style: TextStyle(fontSize: 13, color: AppColors.textHint),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildSpecGroup(SpecGroupInput group) {
    final gid = group.id!;
    final values = group.values.where((v) => v.name.trim().isNotEmpty);
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            group.name,
            style: const TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 9),
          Wrap(
            spacing: 9,
            runSpacing: 9,
            children: [
              for (final value in values)
                if (value.id != null)
                  _SpecChip(
                    label: value.name,
                    selected: _selectedSpec[gid] == value.id,
                    onTap: () => _selectSpec(gid, value.id!),
                  ),
            ],
          ),
        ],
      ),
    );
  }

  /// 选中 SKU 后的库存 + 价格 + 数量区
  Widget _buildPriceBar() {
    final sku = _matchedSku!;
    final soldOut = sku.stock != null && sku.stock != -1 && sku.stock! <= 0;
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 4, 16, 10),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
      decoration: BoxDecoration(
        color: const Color(0xFFF5F6F8),
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  sku.stock == -1
                      ? '库存充足'
                      : soldOut
                          ? '已售罄'
                          : '剩余 ${sku.stock}',
                  style: TextStyle(
                    fontSize: 12,
                    color: soldOut ? AppColors.danger : AppColors.textHint,
                  ),
                ),
                const SizedBox(height: 3),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text(
                      MoneyFormat.yuan(sku.dealPrice),
                      style: const TextStyle(
                        fontSize: 21,
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                    if (sku.hasValidDiscount) ...[
                      const SizedBox(width: 7),
                      Text(
                        MoneyFormat.yuan(sku.unitPrice ?? 0),
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.textHint,
                          decoration: TextDecoration.lineThrough,
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              _QtyButton(
                icon: Icons.remove,
                enabled: _quantity > 1,
                onTap: () => _changeQuantity(-1),
              ),
              SizedBox(
                width: 34,
                child: Text(
                  '$_quantity',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              _QtyButton(
                icon: Icons.add,
                enabled: _quantity < 99,
                onTap: () => _changeQuantity(1),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildActions() {
    final sku = _matchedSku;
    final soldOut =
        sku != null && sku.stock != null && sku.stock != -1 && sku.stock! <= 0;
    final enabled = sku != null && !soldOut;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 2, 16, 14),
      child: Row(
        children: [
          Expanded(
            child: SizedBox(
              height: 44,
              child: OutlinedButton(
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textSecondary,
                  side: const BorderSide(color: AppColors.divider),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppThemeRadius.md),
                  ),
                ),
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('关闭'),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 2,
            child: PrimaryButton(
              label: widget.confirmText,
              height: 44,
              onPressed: enabled ? _confirm : null,
            ),
          ),
        ],
      ),
    );
  }
}

/// 规格选项胶囊
class _SpecChip extends StatelessWidget {
  const _SpecChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? AppColors.primaryLight : const Color(0xFFF2F4F8),
          borderRadius: BorderRadius.circular(17),
          border: Border.all(
            color: selected ? AppColors.primary : Colors.transparent,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 13.5,
            fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
            color: selected ? AppColors.primary : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }
}

/// 数量加减圆形按钮
class _QtyButton extends StatelessWidget {
  const _QtyButton({
    required this.icon,
    required this.enabled,
    required this.onTap,
  });

  final IconData icon;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 32,
      height: 32,
      child: Material(
        color: enabled ? Colors.white : const Color(0xFFF2F4F8),
        shape: CircleBorder(
          side: BorderSide(
            color: enabled ? AppColors.divider : AppColors.divider,
          ),
        ),
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: enabled ? onTap : null,
          child: Icon(
            icon,
            size: 16,
            color: enabled ? AppColors.textPrimary : AppColors.textHint,
          ),
        ),
      ),
    );
  }
}
