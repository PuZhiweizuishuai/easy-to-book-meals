import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/session_provider.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/state_views.dart';

/// 菜品分类管理
///
/// 分类为全局共享数据（跨门店），增删改需要店主身份；
/// 移动端做轻量维护：名称、排序与删除，排序值小的展示在前。
class CategoryPage extends ConsumerStatefulWidget {
  const CategoryPage({super.key});

  @override
  ConsumerState<CategoryPage> createState() => _CategoryPageState();
}

class _CategoryPageState extends ConsumerState<CategoryPage> {
  List<CategorySummary> _categories = const [];
  bool _loading = true;
  String? _error;

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
      final list = await ref.read(categoryApiProvider).list();
      if (!mounted) return;
      final sorted = [...list]
        ..sort((a, b) => (a.sort ?? 0).compareTo(b.sort ?? 0));
      setState(() {
        _categories = sorted;
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

  Future<void> _showEditor({CategorySummary? category}) async {
    final nameController = TextEditingController(text: category?.name ?? '');
    final sortController =
        TextEditingController(text: '${category?.sort ?? 1}');
    final isEdit = category != null;

    final result = await showAppBottomSheet<bool>(
      context: context,
      title: isEdit ? '编辑分类' : '新增分类',
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: nameController,
            autofocus: !isEdit,
            decoration: const InputDecoration(labelText: '分类名称'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: sortController,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: '排序值（越小越靠前）',
            ),
          ),
          const SizedBox(height: 18),
          PrimaryButton(
            label: isEdit ? '保存修改' : '创建分类',
            onPressed: () {
              final name = nameController.text.trim();
              if (name.isEmpty) {
                AppToast.error(context, '请输入分类名称');
                return;
              }
              Navigator.of(context).pop(true);
            },
          ),
        ],
      ),
    );

    // 先取值再释放，保证提前 return 时也不会泄漏控制器
    final name = nameController.text.trim();
    final sort = int.tryParse(sortController.text.trim()) ?? 1;
    nameController.dispose();
    sortController.dispose();

    if (!mounted || result != true) return;

    setState(() => _loading = true);
    try {
      final api = ref.read(categoryApiProvider);
      if (isEdit) {
        await api.update(id: category.id!, name: name, sort: sort);
      } else {
        await api.create(name: name, sort: sort);
      }
      if (!mounted) return;
      AppToast.success(context, isEdit ? '分类已更新' : '分类已创建');
      await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
      await _load();
    }
  }

  Future<void> _delete(CategorySummary category) async {
    final ok = await showConfirmDialog(
      context,
      title: '删除分类',
      content: '确定删除「${category.displayName}」吗？菜品上的该分类绑定会一并解除。',
      danger: true,
    );
    if (ok != true) return;
    try {
      await ref.read(categoryApiProvider).delete(id: category.id!);
      if (!mounted) return;
      AppToast.success(context, '分类已删除');
      await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('菜品分类'),
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
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading && _categories.isEmpty) return const LoadingView();
    if (_error != null && _categories.isEmpty) {
      return ErrorView(message: _error!, onRetry: _load);
    }
    if (_categories.isEmpty) {
      return const EmptyView(
        title: '还没有分类',
        subtitle: '点击右上角 + 创建第一个菜品分类',
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
        itemCount: _categories.length,
        itemBuilder: (context, index) {
          final category = _categories[index];
          return Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: AppCard(
              child: Row(
                children: [
                  Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      color: AppColors.primaryLight,
                      borderRadius: BorderRadius.circular(11),
                    ),
                    child: Icon(Symbols.sell,
                        size: 19, color: AppColors.primary),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          category.displayName,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '排序 ${category.sort ?? 1}${category.count != null ? ' · ${category.count} 道菜' : ''}',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.textHint,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.edit_outlined, size: 19),
                    onPressed: () => _showEditor(category: category),
                  ),
                  IconButton(
                    icon: const Icon(
                      Icons.delete_outline,
                      size: 19,
                      color: AppColors.danger,
                    ),
                    onPressed: () => _delete(category),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
