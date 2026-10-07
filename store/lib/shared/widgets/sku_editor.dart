import 'package:flutter/material.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import 'app_button.dart';
import 'app_card.dart';
import 'app_filter_chip.dart';

/// SKU 价格与库存编辑器（可复用）
///
/// 参考 Web 端 EditDishView 的「SKU 价格与库存管理」模块，把表格改造成移动端卡片：
/// - 顶部按规格组做「快速筛选」（同组内 OR、跨组 AND，与 Web 端一致）；
/// - 每个 SKU 一张卡片，可改单价 / 库存 / 折扣 / 优惠到期时间 / 描述 / 上下架；
/// - `status == 2`（规格变更后被锁定）的 SKU 只读，避免写入被后端拒绝。
///
/// 组件内部持有输入控制器，任何修改都通过 [onChanged] 回传最新的 [SkuInput] 列表。
class SkuEditor extends StatefulWidget {
  const SkuEditor({
    super.key,
    required this.initialSkus,
    required this.specGroups,
    required this.onChanged,
  });

  /// 初始 SKU 列表（由 /dishes/{id} 返回）
  final List<SkuInput> initialSkus;

  /// 当前规格组，用于筛选器与规格名展示
  final List<SpecGroupInput> specGroups;

  /// 内容变化回调
  final ValueChanged<List<SkuInput>> onChanged;

  @override
  State<SkuEditor> createState() => _SkuEditorState();
}

class _SkuEditorState extends State<SkuEditor> {
  late List<SkuInput> _skus;
  late List<_SkuControllers> _controllers;

  /// 快速筛选：选中的规格值 ID（跨组 AND）
  final Set<int> _filterSpecIds = <int>{};

  /// 规格值 ID → 名称
  Map<int, String> _specNames = const {};

  /// 规格值 ID → 所属规格组下标（同一组用同一颜色，与 Web 端一致）
  Map<int, int> _specGroupIndex = const {};

  @override
  void initState() {
    super.initState();
    _skus = List<SkuInput>.of(widget.initialSkus);
    _controllers = [
      for (final sku in _skus) _SkuControllers.of(sku, onEdit: _notify),
    ];
    _rebuildSpecNames();
  }

  @override
  void didUpdateWidget(covariant SkuEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.specGroups != widget.specGroups) _rebuildSpecNames();
  }

  void _rebuildSpecNames() {
    final names = <int, String>{};
    final groupIndexes = <int, int>{};
    for (var i = 0; i < widget.specGroups.length; i++) {
      final group = widget.specGroups[i];
      for (final value in group.values) {
        final id = value.id;
        if (id == null) continue;
        names[id] = value.name.trim();
        groupIndexes[id] = i;
      }
    }
    _specNames = names;
    _specGroupIndex = groupIndexes;
  }

  @override
  void dispose() {
    for (final item in _controllers) {
      item.dispose();
    }
    super.dispose();
  }

  void _notify() {
    _skus = [
      for (var i = 0; i < _controllers.length; i++)
        _skus[i].copyWith(
          description: _controllers[i].description.text,
          unitPrice: double.tryParse(_controllers[i].price.text.trim()) ?? 0,
          stock: int.tryParse(_controllers[i].stock.text.trim()) ?? -1,
          discount: double.tryParse(_controllers[i].discount.text.trim()) ?? 0,
        ),
    ];
    widget.onChanged(_skus);
  }

  void _patch(int index, {int? status, int? discountEndTime}) {
    setState(() {
      _skus[index] = _skus[index].copyWith(
        status: status,
        discountEndTime: discountEndTime,
      );
    });
    _notify();
  }

  void _toggleFilter(int specId) {
    setState(() {
      if (_filterSpecIds.contains(specId)) {
        _filterSpecIds.remove(specId);
      } else {
        _filterSpecIds.add(specId);
      }
    });
  }

  /// 符合筛选条件的 SKU 下标
  List<int> get _visibleIndexes {
    if (_filterSpecIds.isEmpty) {
      return [for (var i = 0; i < _skus.length; i++) i];
    }
    return [
      for (var i = 0; i < _skus.length; i++)
        if (_filterSpecIds.every((id) => _skus[i].specIds.contains(id))) i,
    ];
  }

  /// 可用于筛选的规格组（至少有 1 个带 ID 的选项）
  List<SpecGroupInput> get _filterableGroups => [
        for (final group in widget.specGroups)
          if (group.values.any((value) => value.id != null)) group,
      ];

  @override
  Widget build(BuildContext context) {
    if (_skus.isEmpty) {
      return const Text(
        '该菜品暂无规格组合，配置规格组后系统会自动生成 SKU',
        style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
      );
    }

    final indexes = _visibleIndexes;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (_filterableGroups.isNotEmpty) ...[
          _buildFilter(),
          const SizedBox(height: 12),
        ],
        Text(
          '共 ${_skus.length} 个规格组合'
          '${_filterSpecIds.isEmpty ? '' : '，筛选出 ${indexes.length} 个'}',
          style: const TextStyle(fontSize: 12, color: AppColors.textHint),
        ),
        const SizedBox(height: 8),
        if (indexes.isEmpty)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 20),
            decoration: BoxDecoration(
              color: const Color(0xFFF7F8FA),
              borderRadius: BorderRadius.circular(AppThemeRadius.md),
            ),
            child: const Text(
              '没有符合该筛选条件的规格组合，试试取消部分条件',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
            ),
          )
        else
          for (final index in indexes)
            _SkuCard(
              sku: _skus[index],
              controllers: _controllers[index],
              specNames: _specNames,
              specGroupIndex: _specGroupIndex,
              onToggleStatus: (on) =>
                  _patch(index, status: on ? SkuStatus.on : SkuStatus.off),
              onPickEndTime: (millis) =>
                  _patch(index, discountEndTime: millis),
            ),
      ],
    );
  }

  Widget _buildFilter() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F9FC),
        borderRadius: BorderRadius.circular(AppThemeRadius.md),
        border: Border.all(color: const Color(0xFFE6EAF0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Symbols.filter_alt, size: 15, color: AppColors.primary),
              const SizedBox(width: 5),
              const Text(
                '快速筛选 SKU',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                ),
              ),
              const Spacer(),
              if (_filterSpecIds.isNotEmpty)
                GestureDetector(
                  onTap: () => setState(() => _filterSpecIds.clear()),
                  child: const Text(
                    '清空筛选',
                    style: TextStyle(fontSize: 12, color: AppColors.danger),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 10),
          for (final group in _filterableGroups) ...[
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Text(
                group.name.trim().isEmpty ? '未命名规格' : group.name.trim(),
                style: const TextStyle(
                  fontSize: 12,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final value in group.values)
                  if (value.id != null)
                    AppFilterChip(
                      label: value.name.trim().isEmpty
                          ? '未命名选项'
                          : value.name.trim(),
                      selected: _filterSpecIds.contains(value.id),
                      onTap: () => _toggleFilter(value.id!),
                    ),
              ],
            ),
            const SizedBox(height: 8),
          ],
        ],
      ),
    );
  }
}

/// 单个 SKU 的输入控制器集合
class _SkuControllers {
  _SkuControllers({
    required this.price,
    required this.stock,
    required this.discount,
    required this.description,
  });

  factory _SkuControllers.of(SkuInput sku, {required VoidCallback onEdit}) {
    final price = TextEditingController(text: sku.unitPrice.toStringAsFixed(2));
    final stock = TextEditingController(text: '${sku.stock}');
    final discount =
        TextEditingController(text: sku.discount.toStringAsFixed(2));
    final description = TextEditingController(text: sku.description);
    for (final controller in <TextEditingController>[
      price,
      stock,
      discount,
      description,
    ]) {
      controller.addListener(onEdit);
    }
    return _SkuControllers(
      price: price,
      stock: stock,
      discount: discount,
      description: description,
    );
  }

  final TextEditingController price;
  final TextEditingController stock;
  final TextEditingController discount;
  final TextEditingController description;

  void dispose() {
    price.dispose();
    stock.dispose();
    discount.dispose();
    description.dispose();
  }
}

/// SKU 卡片
class _SkuCard extends StatelessWidget {
  const _SkuCard({
    required this.sku,
    required this.controllers,
    required this.specNames,
    required this.specGroupIndex,
    required this.onToggleStatus,
    required this.onPickEndTime,
  });

  final SkuInput sku;
  final _SkuControllers controllers;
  final Map<int, String> specNames;
  final Map<int, int> specGroupIndex;
  final ValueChanged<bool> onToggleStatus;
  final ValueChanged<int> onPickEndTime;

  @override
  Widget build(BuildContext context) {
    final locked = sku.status == SkuStatus.invalid;
    final permanent = sku.discountEndTime == 0;
    final colorIndex = sku.specIds.isEmpty
        ? 0
        : (specGroupIndex[sku.specIds.first] ?? sku.specIds.first);

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Opacity(
        opacity: locked ? 0.55 : 1,
        child: AppCard(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 规格组合
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  if (sku.specIds.isEmpty)
                    _SpecBadge(label: '默认规格', color: AppColors.disabled)
                  else
                    for (final id in sku.specIds)
                      _SpecBadge(
                        label: specNames[id]?.isNotEmpty == true
                            ? specNames[id]!
                            : '规格 #$id',
                        color: _colorOf(colorIndex),
                      ),
                  if (locked) _SpecBadge(label: '规格已失效', color: AppColors.danger),
                ],
              ),
              const Divider(height: 14),
              Row(
                children: [
                  Expanded(
                    child: _NumberField(
                      controller: controllers.price,
                      label: '单价 (¥)',
                      enabled: !locked,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _NumberField(
                      controller: controllers.stock,
                      label: '库存 (-1 不限)',
                      enabled: !locked,
                      decimal: false,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  SizedBox(
                    width: 110,
                    child: _NumberField(
                      controller: controllers.discount,
                      label: '折扣 (0~1)',
                      enabled: !locked,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _InlineSwitch(
                      label: '永久优惠',
                      value: permanent,
                      enabled: !locked,
                      // 0 表示永久；取消永久时给一个默认截止时间，否则值仍是 0
                      onChanged: (value) => onPickEndTime(
                        value
                            ? 0
                            : DateTime.now()
                                .add(const Duration(days: 7))
                                .millisecondsSinceEpoch,
                      ),
                    ),
                  ),
                ],
              ),
              if (!permanent) ...[
                const SizedBox(height: 8),
                _EndTimeTile(
                  millis: sku.discountEndTime,
                  enabled: !locked,
                  onPick: onPickEndTime,
                ),
              ],
              const SizedBox(height: 10),
              TextField(
                controller: controllers.description,
                enabled: !locked,
                maxLines: 2,
                minLines: 1,
                decoration: const InputDecoration(
                  labelText: '描述 / 推荐',
                  isDense: true,
                ),
                style: const TextStyle(fontSize: 13),
              ),
              const SizedBox(height: 4),
              Row(
                children: [
                  const Text(
                    '上架销售',
                    style: TextStyle(
                      fontSize: 13,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    SkuStatus.name(sku.status),
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: locked
                          ? AppColors.danger
                          : (sku.status == SkuStatus.on
                              ? AppColors.success
                              : AppColors.textHint),
                    ),
                  ),
                  Switch(
                    value: sku.status == SkuStatus.on,
                    activeThumbColor: AppColors.primary,
                    onChanged: locked ? null : onToggleStatus,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 同一规格组用同一种颜色（模仿 Web 端按组着色的效果）
  Color _colorOf(int seed) => const [
        AppColors.primary,
        AppColors.accent,
        AppColors.success,
        AppColors.warning,
        AppColors.info,
      ][seed.abs() % 5];
}

class _SpecBadge extends StatelessWidget {
  const _SpecBadge({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withValues(alpha: 0.45)),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11.5,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}

class _NumberField extends StatelessWidget {
  const _NumberField({
    required this.controller,
    required this.label,
    required this.enabled,
    this.decimal = true,
  });

  final TextEditingController controller;
  final String label;
  final bool enabled;
  final bool decimal;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      enabled: enabled,
      keyboardType: TextInputType.numberWithOptions(decimal: decimal),
      decoration: InputDecoration(
        labelText: label,
        isDense: true,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      ),
      style: const TextStyle(fontSize: 13.5),
    );
  }
}

class _InlineSwitch extends StatelessWidget {
  const _InlineSwitch({
    required this.label,
    required this.value,
    required this.enabled,
    required this.onChanged,
  });

  final String label;
  final bool value;
  final bool enabled;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
        ),
        Switch(
          value: value,
          activeThumbColor: AppColors.primary,
          onChanged: enabled ? onChanged : null,
        ),
      ],
    );
  }
}

/// 优惠到期时间选择
class _EndTimeTile extends StatelessWidget {
  const _EndTimeTile({
    required this.millis,
    required this.enabled,
    required this.onPick,
  });

  final int millis;
  final bool enabled;
  final ValueChanged<int> onPick;

  Future<void> _pick(BuildContext context) async {
    final now = DateTime.now();
    final initial = millis > 0
        ? DateTime.fromMillisecondsSinceEpoch(millis)
        : now.add(const Duration(days: 7));
    final date = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: now,
      lastDate: now.add(const Duration(days: 365 * 3)),
    );
    if (date == null || !context.mounted) return;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(initial),
    );
    if (time == null) return;
    onPick(
      DateTime(
        date.year,
        date.month,
        date.day,
        time.hour,
        time.minute,
      ).millisecondsSinceEpoch,
    );
  }

  @override
  Widget build(BuildContext context) {
    final expired = millis > 0 && millis < DateTime.now().millisecondsSinceEpoch;
    return GestureDetector(
      onTap: enabled ? () => _pick(context) : null,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
        decoration: BoxDecoration(
          color: const Color(0xFFF7F8FA),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: AppColors.divider),
        ),
        child: Row(
          children: [
            const Icon(Symbols.schedule, size: 15, color: AppColors.textHint),
            const SizedBox(width: 6),
            Expanded(
              child: Text(
                millis > 0
                    ? '优惠截止 ${TimeFormat.dateTime(millis)}'
                    : '未设置截止时间',
                style: TextStyle(
                  fontSize: 12.5,
                  color: expired ? AppColors.danger : AppColors.textPrimary,
                ),
              ),
            ),
            const Text(
              '修改',
              style: TextStyle(fontSize: 12, color: AppColors.primary),
            ),
          ],
        ),
      ),
    );
  }
}
