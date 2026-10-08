import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/config/app_config.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../dishes/dish_list_page.dart';
import '../dishes/category_page.dart';
import '../members/member_list_page.dart';
import '../tables/table_page.dart';

/// 门店管理入口
///
/// 把「菜品 / 分类 / 餐桌 / 会员」四个管理模块收在一个入口里，
/// 每个模块点进去是独立页面，便于后续继续扩展。
class ManagePage extends ConsumerWidget {
  const ManagePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(currentUserProvider);
    final store = ref.watch(currentStoreProvider);
    final permissionCodes = user?.permissionCodes ?? const [];

    final entries = <_ManageEntry>[
      _ManageEntry(
        title: '菜品管理',
        subtitle: '上下架 · 改价 · 库存',
        icon: Symbols.lunch_dining,
        color: AppColors.primary,
        requiredPermissions: const ['dish:view', 'dish:manage'],
        target: const DishListPage(),
      ),
      _ManageEntry(
        title: '菜品分类',
        subtitle: '菜单分组与排序',
        icon: Symbols.sell,
        color: const Color(0xFF7C4DFF),
        requiredPermissions: const ['dish:view', 'dish:manage'],
        target: const CategoryPage(),
      ),
      _ManageEntry(
        title: '餐桌管理',
        subtitle: '桌号 · 状态 · 预约 · 订单',
        icon: Symbols.table_restaurant,
        color: AppColors.info,
        requiredPermissions: const ['table:view', 'table:manage'],
        target: const TablePage(),
      ),
      _ManageEntry(
        title: '会员管理',
        subtitle: '积分 · 昵称 · 消费记录',
        icon: Symbols.groups,
        color: AppColors.success,
        requiredPermissions: const ['member:manage'],
        target: const MemberListPage(),
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: const Text('门店管理'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
        children: [
          _StoreCard(
            storeName: store?.displayName ?? '未选择门店',
            address: store?.address,
            statusName: store == null ? '' : storeStatusName(store.status),
            permissionText: permissionCodes.isEmpty
                ? '暂无门店权限'
                : '已开通 ${permissionCodes.length} 项权限',
          ),
          const SizedBox(height: 14),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.28,
            children: [
              for (final entry in entries)
                _ManageCard(
                  entry: entry,
                  enabled: user == null
                      ? false
                      : user.isSuperAdmin ||
                          user.hasAnyPermission(entry.requiredPermissions),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => entry.target),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

/// 管理模块定义
class _ManageEntry {
  const _ManageEntry({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.requiredPermissions,
    required this.target,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final List<String> requiredPermissions;
  final Widget target;
}

/// 门店信息卡
class _StoreCard extends StatelessWidget {
  const _StoreCard({
    required this.storeName,
    this.address,
    required this.statusName,
    required this.permissionText,
  });

  final String storeName;
  final String? address;
  final String statusName;
  final String permissionText;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppColors.primary, AppColors.primaryDark],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.store_outlined, size: 20, color: Colors.white),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  storeName,
                  style: const TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              if (statusName.isNotEmpty)
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.22),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    statusName,
                    style: const TextStyle(fontSize: 11.5, color: Colors.white),
                  ),
                ),
            ],
          ),
          if (address?.isNotEmpty == true) ...[
            const SizedBox(height: 8),
            Text(
              address!,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 12.5,
                color: Colors.white.withValues(alpha: 0.9),
              ),
            ),
          ],
          const SizedBox(height: 10),
          Text(
            '$permissionText · ${AppConfig.appName}',
            style: TextStyle(
              fontSize: 11.5,
              color: Colors.white.withValues(alpha: 0.85),
            ),
          ),
        ],
      ),
    );
  }
}

/// 管理模块卡片
class _ManageCard extends StatelessWidget {
  const _ManageCard({
    required this.entry,
    required this.enabled,
    required this.onTap,
  });

  final _ManageEntry entry;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = enabled ? entry.color : AppColors.disabled;
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        boxShadow: const [
          BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: enabled ? onTap : null,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(11),
                  ),
                  child: Icon(entry.icon, size: 21, color: color),
                ),
                const Spacer(),
                Text(
                  entry.title,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  enabled ? entry.subtitle : '暂无权限',
                  style: const TextStyle(
                    fontSize: 11.5,
                    color: AppColors.textHint,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
