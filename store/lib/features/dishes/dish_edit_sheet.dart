import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/session_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_feedback.dart';

/// 菜品新增 / 编辑面板
///
/// 移动端只保留高频字段（名称、描述、价格、计价方式、库存、上下架、分类），
/// 规格 SKU 仍在 Web 端维护，避免小屏上操作过于复杂。
class DishEditSheet extends ConsumerStatefulWidget {
  const DishEditSheet({
    super.key,
    required this.storeId,
    this.dish,
    this.onSaved,
  });

  /// 门店 ID
  final int storeId;

  /// 传入表示编辑，为空表示新增
  final Dish? dish;

  Future<bool?> show(BuildContext context) => showModalBottomSheet<bool?>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.white,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        builder: (_) => this,
      );

  /// 保存成功回调
  final VoidCallback? onSaved;

  @override
  ConsumerState<DishEditSheet> createState() => _DishEditSheetState();
}

class _DishEditSheetState extends ConsumerState<DishEditSheet> {
  late final TextEditingController _nameController;
  late final TextEditingController _descController;
  late final TextEditingController _priceController;
  late final TextEditingController _stockController;

  int _priceType = 0;
  int _status = 1;
  bool _submitting = false;
  bool _categoriesLoading = true;
  List<CategorySummary> _categories = const [];
  final Set<int> _selectedCategoryIds = <int>{};

  bool get _isEdit => widget.dish != null;

  @override
  void initState() {
    super.initState();
    final dish = widget.dish;
    _nameController = TextEditingController(text: dish?.name ?? '');
    _descController = TextEditingController(text: dish?.description ?? '');
    _priceController = TextEditingController(
      text: dish == null ? '' : (dish.basePrice ?? 0).toStringAsFixed(2),
    );
    _stockController = TextEditingController(
      text: dish == null ? '-1' : '${dish.stock ?? -1}',
    );
    _priceType = dish?.priceType ?? 0;
    _status = dish?.status ?? 1;
    for (final category in dish?.categoryIds ?? const []) {
      if (category.id != null) _selectedCategoryIds.add(category.id!);
    }
    _loadCategories();
  }

  Future<void> _loadCategories() async {
    try {
      final list = await ref.read(categoryApiProvider).list();
      if (!mounted) return;
      setState(() {
        _categories = list;
        _categoriesLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _categoriesLoading = false);
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descController.dispose();
    _priceController.dispose();
    _stockController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    final name = _nameController.text.trim();
    if (name.isEmpty) {
      AppToast.error(context, '请输入菜品名称');
      return;
    }
    final price = double.tryParse(_priceController.text.trim());
    if (price == null || price < 0) {
      AppToast.error(context, '请输入正确的价格');
      return;
    }
    final stock = int.tryParse(_stockController.text.trim()) ?? -1;

    final selected = _categories
        .where((c) => _selectedCategoryIds.contains(c.id))
        .toList(growable: false);

    setState(() => _submitting = true);
    try {
      final api = ref.read(dishApiProvider);
      if (_isEdit) {
        await api.update(
          UpdateDishPayload(
            id: widget.dish!.id!,
            storefrontId: widget.storeId,
            name: name,
            description: _descController.text.trim(),
            topImage: widget.dish!.topImage,
            basePrice: price,
            priceType: _priceType,
            categoryIds: selected,
            stock: stock,
            status: _status,
          ),
        );
      } else {
        await api.create(
          CreateDishPayload(
            storefrontId: widget.storeId,
            name: name,
            description: _descController.text.trim(),
            topImage: widget.dish?.topImage,
            unitPrice: price,
            priceType: _priceType,
            categoryIds: selected,
            stock: stock,
            status: _status,
          ),
        );
      }
      if (!mounted) return;
      AppToast.success(context, _isEdit ? '菜品已更新' : '菜品已创建');
      widget.onSaved?.call();
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
      setState(() => _submitting = false);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '保存失败，请稍后重试');
      setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return SafeArea(
      top: false,
      child: SingleChildScrollView(
        child: Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 14,
            bottom: 16 + bottomInset,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      _isEdit ? '编辑菜品' : '新增菜品',
                      style: const TextStyle(
                        fontSize: 16.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, size: 20),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const Divider(height: 1),
              const SizedBox(height: 14),
              TextField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: '菜品名称'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _descController,
                maxLines: 2,
                decoration: const InputDecoration(labelText: '菜品描述'),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _priceController,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      decoration: const InputDecoration(labelText: '价格'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: DropdownButtonFormField<int>(
                      initialValue: _priceType,
                      decoration: const InputDecoration(labelText: '计价方式'),
                      items: [
                        for (final code in PriceTypeMeta.codes)
                          DropdownMenuItem<int>(
                            value: code,
                            child: Text('按${PriceTypeMeta.name(code)}'),
                          ),
                      ],
                      onChanged: (value) =>
                          setState(() => _priceType = value ?? 0),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _stockController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: '库存（-1 表示不限）',
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  const Text(
                    '上架状态',
                    style: TextStyle(
                      fontSize: 14,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const Spacer(),
                  Switch(
                    value: _status == 1,
                    activeThumbColor: AppColors.primary,
                    onChanged: (value) =>
                        setState(() => _status = value ? 1 : 0),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              const Text(
                '菜品分类',
                style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
              ),
              const SizedBox(height: 8),
              if (_categoriesLoading)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 8),
                  child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
                )
              else if (_categories.isEmpty)
                const Text(
                  '暂无分类，可先在「菜品分类」中创建',
                  style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
                )
              else
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    for (final category in _categories)
                      FilterChip(
                        selected: _selectedCategoryIds.contains(category.id),
                        label: Text(category.displayName),
                        onSelected: (selected) {
                          setState(() {
                            if (selected) {
                              _selectedCategoryIds.add(category.id!);
                            } else {
                              _selectedCategoryIds.remove(category.id);
                            }
                          });
                        },
                      ),
                  ],
                ),
              const SizedBox(height: 20),
              PrimaryButton(
                label: _isEdit ? '保存修改' : '创建菜品',
                busy: _submitting,
                onPressed: _submitting ? null : _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
