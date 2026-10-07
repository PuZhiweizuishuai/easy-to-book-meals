import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_network_image.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/paged_list_view.dart';
import '../../shared/widgets/search_field.dart';
import '../../shared/widgets/status_chip.dart';
import 'dish_edit_sheet.dart';

/// 菜品管理页
///
/// 支持关键字搜索与上下架筛选，点击卡片进入编辑面板；
/// 右上角「+」新增菜品。库存显示 -1 表示不限库存。
class DishListPage extends ConsumerStatefulWidget {
  const DishListPage({super.key});

  @override
  ConsumerState<DishListPage> createState() => _DishListPageState();
}

class _DishListPageState extends ConsumerState<DishListPage> {
  final PagedListController<Dish> _listController = PagedListController<Dish>();
  String _keyword = '';

  /// 上下架筛选：null 全部 / 1 上架 / 0 下架
  int? _statusFilter;

  @override
  Widget build(BuildContext context) {
    final storeId = ref.watch(currentStoreIdProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('菜品管理（推荐使用网页端进行管理）'),
        actions: [
          IconButton(
            tooltip: '新增菜品',
            icon: const Icon(Icons.add, size: 24),
            onPressed: storeId == null ? null : () => _openEdit(null),
          ),
        ],
      ),
      body: storeId == null
          ? const Center(child: Text('请先选择门店'))
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(14, 10, 14, 6),
                  child: SearchField(
                    hint: '搜索菜品名称',
                    onSearch: (value) => setState(() => _keyword = value.trim()),
                  ),
                ),
                SizedBox(
                  height: 38,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    children: [
                      _StatusChip(
                        label: '全部',
                        selected: _statusFilter == null,
                        onTap: () => setState(() => _statusFilter = null),
                      ),
                      _StatusChip(
                        label: '已上架',
                        selected: _statusFilter == 1,
                        color: AppColors.success,
                        onTap: () => setState(() => _statusFilter = 1),
                      ),
                      _StatusChip(
                        label: '已下架',
                        selected: _statusFilter == 0,
                        color: AppColors.disabled,
                        onTap: () => setState(() => _statusFilter = 0),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 6),
                Expanded(
                  child: PagedListView<Dish>(
                    controller: _listController,
                    reloadKey: '$_keyword#${_statusFilter ?? 'all'}',
                    fetcher: (page) => ref.read(dishApiProvider).list(
                          storeId: storeId,
                          key: _keyword.isEmpty ? null : _keyword,
                          page: page,
                          size: 10,
                        ),
                    emptyTitle: '暂无菜品',
                    emptySubtitle: '点击右上角 + 新增菜品',
                    itemBuilder: (context, dish, index) => DishCard(
                      dish: dish,
                      onTap: () => _openEdit(dish),
                    ),
                  ),
                ),
              ],
            ),
    );
  }

  Future<void> _openEdit(Dish? dish) async {
    final storeId = ref.read(currentStoreIdProvider);
    if (storeId == null) return;
    final ok = await DishEditSheet(
      storeId: storeId,
      dish: dish,
    ).show(context);
    if (ok == true) _listController.refresh();
  }
}

/// 筛选芯片
class _StatusChip extends StatelessWidget {
  const _StatusChip({
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
          color: selected
              ? base.withValues(alpha: 0.12)
              : const Color(0xFFF2F4F8),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: selected ? base : Colors.transparent,
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

/// 菜品卡片（可复用）
class DishCard extends StatelessWidget {
  const DishCard({
    super.key,
    required this.dish,
    this.onTap,
    this.onToggleStatus,
    this.busy = false,
  });

  final Dish dish;

  /// 点击进入编辑
  final VoidCallback? onTap;

  /// 快捷上下架回调（列表页直接切换，无需进编辑页）
  final ValueChanged<bool>? onToggleStatus;

  /// 正在切换上下架
  final bool busy;

  @override
  Widget build(BuildContext context) {
    final onSale = dish.onSale;
    return AppCard(
      onTap: onTap,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Stack(
            children: [
              AppNetworkImage(
                url: dish.topImage,
                size: 62,
                placeholderIcon: Symbols.lunch_dining,
              ),
              if (!onSale)
                Positioned.fill(
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.35),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Center(
                      child: Text(
                        '已下架',
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        dish.name ?? '未命名菜品',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ),
                    StatusChip(
                      label: onSale ? '在售' : '下架',
                      foregroundColor:
                          onSale ? AppColors.success : AppColors.disabled,
                      backgroundColor: onSale
                          ? AppColors.successLight
                          : const Color(0xFFF2F4F8),
                      dense: true,
                    ),
                  ],
                ),
                if (dish.categoryText.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    dish.categoryText,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textHint,
                    ),
                  ),
                ],
                if (dish.descriptionText != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    dish.descriptionText!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
                const SizedBox(height: 8),
                Row(
                  children: [
                    Text(
                      '¥${MoneyFormat.plain(dish.basePrice ?? 0)}',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                    Text(
                      ' / ${dish.unitText}',
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textHint,
                      ),
                    ),
                    const Spacer(),
                    Icon(Icons.inventory_2_outlined,
                        size: 13, color: AppColors.textHint),
                    const SizedBox(width: 3),
                    Text(
                      dish.stockText == '不限' ? '不限库存' : '库存 ${dish.stockText}',
                      style: const TextStyle(
                        fontSize: 11.5,
                        color: AppColors.textHint,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 上下架切换的快捷入口（供后续在列表页直接调用）
Future<void> toggleDishStatus(
  BuildContext context,
  WidgetRef ref,
  Dish dish,
  bool onSale,
) async {
  final storeId = ref.read(currentStoreIdProvider);
  if (storeId == null || dish.id == null) return;
  try {
    // /dishes/update 按 fileDataList 做差量更新，缺失会导致已关联图片被删除，
    // 所以先取一次详情把现有文件带回去。
    final detail = await ref.read(dishApiProvider).detail(dishId: dish.id!);
    await ref.read(dishApiProvider).update(
          UpdateDishPayload(
            id: dish.id!,
            storefrontId: storeId,
            name: dish.name ?? '',
            description: dish.description,
            topImage: dish.topImage,
            basePrice: dish.basePrice,
            priceType: dish.priceType ?? 0,
            categoryIds: dish.categoryIds,
            stock: dish.stock,
            status: onSale ? 1 : 0,
            imageList: dish.imageUrls,
            fileDataList: detail?.fileDataList,
          ),
        );
    if (!context.mounted) return;
    AppToast.success(context, onSale ? '已上架' : '已下架');
  } on ApiException catch (e) {
    if (!context.mounted) return;
    if (e.isUnauthorized) {
      await ref.read(sessionProvider.notifier).handleUnauthorized();
      return;
    }
    AppToast.error(context, e.message);
  }
}
