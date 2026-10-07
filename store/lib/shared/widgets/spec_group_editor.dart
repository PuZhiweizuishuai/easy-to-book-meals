import 'package:flutter/material.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import 'app_button.dart';

/// 规格配置编辑器（可复用）
///
/// 一个菜品可配置多组规格（如「辣度」「份量」），每组下含若干选项。
/// 组件内部持有输入控制器，任何编辑都会通过 [onChanged] 把最新的
/// [SpecGroupInput] 列表回传给调用方。
class SpecGroupEditor extends StatefulWidget {
  const SpecGroupEditor({
    super.key,
    required this.initialGroups,
    required this.onChanged,
  });

  /// 初始规格组（编辑菜品时回填已有规格）
  final List<SpecGroupInput> initialGroups;

  /// 内容变化回调
  final ValueChanged<List<SpecGroupInput>> onChanged;

  @override
  State<SpecGroupEditor> createState() => _SpecGroupEditorState();
}

class _SpecGroupEditorState extends State<SpecGroupEditor> {
  final List<int?> _groupIds = <int?>[];
  final List<TextEditingController> _nameControllers =
      <TextEditingController>[];
  final List<List<TextEditingController>> _valueControllers =
      <List<TextEditingController>>[];

  /// 与 [_valueControllers] 一一对应的规格值 ID
  ///
  /// SKU 是按规格值 ID 组合出来的，编辑规格名时不能把 ID 弄丢，
  /// 否则 SKU 列表无法再按规格筛选 / 展示名称。
  final List<List<int?>> _valueIds = <List<int?>>[];

  @override
  void initState() {
    super.initState();
    if (widget.initialGroups.isEmpty) {
      _addGroup();
    } else {
      for (final group in widget.initialGroups) {
        _appendGroup(group);
      }
    }
  }

  @override
  void dispose() {
    for (final controller in _nameControllers) {
      controller.dispose();
    }
    for (final values in _valueControllers) {
      for (final controller in values) {
        controller.dispose();
      }
    }
    super.dispose();
  }

  void _appendGroup(SpecGroupInput group) {
    _groupIds.add(group.id);
    final nameController = TextEditingController(text: group.name);
    nameController.addListener(_notify);
    _nameControllers.add(nameController);
    final values = <TextEditingController>[];
    final ids = <int?>[];
    if (group.values.isEmpty) {
      values.add(_newValueController(''));
      ids.add(null);
    } else {
      for (final value in group.values) {
        values.add(_newValueController(value.name));
        ids.add(value.id);
      }
    }
    _valueControllers.add(values);
    _valueIds.add(ids);
  }

  TextEditingController _newValueController(String text) {
    final controller = TextEditingController(text: text);
    controller.addListener(_notify);
    return controller;
  }

  void _addGroup() {
    setState(() => _appendGroup(const SpecGroupInput()));
    _notify();
  }

  void _removeGroup(int index) {
    final name = _nameControllers.removeAt(index);
    name.dispose();
    for (final controller in _valueControllers.removeAt(index)) {
      controller.dispose();
    }
    _valueIds.removeAt(index);
    _groupIds.removeAt(index);
    setState(() {});
    _notify();
  }

  void _addValue(int index) {
    setState(() {
      _valueControllers[index].add(_newValueController(''));
      _valueIds[index].add(null);
    });
    _notify();
  }

  void _removeValue(int groupIndex, int valueIndex) {
    if (_valueControllers[groupIndex].length <= 1) return;
    final controller = _valueControllers[groupIndex].removeAt(valueIndex);
    controller.dispose();
    _valueIds[groupIndex].removeAt(valueIndex);
    setState(() {});
    _notify();
  }

  /// 收集当前输入并回传
  void _notify() {
    final groups = <SpecGroupInput>[
      for (var i = 0; i < _nameControllers.length; i++)
        SpecGroupInput(
          id: _groupIds[i],
          name: _nameControllers[i].text,
          sort: i + 1,
          values: [
            for (var j = 0; j < _valueControllers[i].length; j++)
              SpecValueInput(
                id: _valueIds[i][j],
                name: _valueControllers[i][j].text,
                sort: j + 1,
              ),
          ],
        ),
    ];
    widget.onChanged(groups);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var i = 0; i < _nameControllers.length; i++)
          _GroupCard(
            index: i,
            nameController: _nameControllers[i],
            valueControllers: _valueControllers[i],
            canRemove: _nameControllers.length > 1,
            onRemoveGroup: () => _removeGroup(i),
            onAddValue: () => _addValue(i),
            onRemoveValue: (valueIndex) => _removeValue(i, valueIndex),
          ),
        const SizedBox(height: 10),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.primary,
              side: BorderSide(
                color: AppColors.primary.withValues(alpha: 0.5),
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppThemeRadius.md),
              ),
            ),
            icon: const Icon(Symbols.add, size: 18),
            label: const Text('添加规格组'),
            onPressed: _addGroup,
          ),
        ),
      ],
    );
  }
}

class _GroupCard extends StatelessWidget {
  const _GroupCard({
    required this.index,
    required this.nameController,
    required this.valueControllers,
    required this.canRemove,
    required this.onRemoveGroup,
    required this.onAddValue,
    required this.onRemoveValue,
  });

  final int index;
  final TextEditingController nameController;
  final List<TextEditingController> valueControllers;
  final bool canRemove;
  final VoidCallback onRemoveGroup;
  final VoidCallback onAddValue;
  final ValueChanged<int> onRemoveValue;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
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
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 8,
                  vertical: 3,
                ),
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  '组 ${index + 1}',
                  style: const TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w700,
                    color: AppColors.primary,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: nameController,
                  decoration: const InputDecoration(
                    hintText: '规格名（例：辣度）',
                    isDense: true,
                    border: InputBorder.none,
                  ),
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              if (canRemove)
                GestureDetector(
                  onTap: onRemoveGroup,
                  child: const Icon(Icons.delete_outline,
                      size: 19, color: AppColors.danger),
                ),
            ],
          ),
          const Divider(height: 14),
          for (var j = 0; j < valueControllers.length; j++)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: valueControllers[j],
                      decoration: const InputDecoration(
                        hintText: '选项名称（例：微辣）',
                        isDense: true,
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 10,
                        ),
                      ),
                      style: const TextStyle(fontSize: 13.5),
                    ),
                  ),
                  if (valueControllers.length > 1)
                    GestureDetector(
                      onTap: () => onRemoveValue(j),
                      child: const Padding(
                        padding: EdgeInsets.only(left: 8),
                        child: Icon(Icons.close,
                            size: 18, color: AppColors.textHint),
                      ),
                    ),
                ],
              ),
            ),
          SizedBox(
            width: double.infinity,
            child: TextButton.icon(
              style: TextButton.styleFrom(
                foregroundColor: AppColors.textSecondary,
                padding: const EdgeInsets.symmetric(vertical: 4),
              ),
              icon: const Icon(Symbols.add, size: 16),
              label: const Text('添加选项', style: TextStyle(fontSize: 13)),
              onPressed: onAddValue,
            ),
          ),
        ],
      ),
    );
  }
}
