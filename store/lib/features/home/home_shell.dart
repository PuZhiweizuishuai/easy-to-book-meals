import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/routing/app_navigator.dart';
import '../../core/theme/app_colors.dart';
import '../../providers/alert_provider.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../dashboard/dashboard_page.dart';
import '../kitchen/kitchen_page.dart';
import '../manage/manage_page.dart';
import '../orders/order_list_page.dart';
import '../profile/profile_page.dart';

/// 主框架
///
/// 底部五个 Tab（看板 / 订单 / 后厨 / 管理 / 我的）用 IndexedStack 保活，
/// 切 Tab 不丢失列表滚动位置与筛选条件；全局提醒通过 AppTabJump 跳到对应 Tab。
class HomeShell extends ConsumerStatefulWidget {
  const HomeShell({super.key});

  @override
  ConsumerState<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends ConsumerState<HomeShell> {
  int _index = AppTabIndex.dashboard;

  /// 各 Tab 根页面（懒构建后缓存）
  final List<Widget> _pages = [
    const DashboardPage(),
    const OrderListPage(),
    const KitchenPage(),
    const ManagePage(),
    const ProfilePage(),
  ];

  @override
  void initState() {
    super.initState();
    AppTabJump.target.addListener(_onTabJump);
    // 首次进入时如果已有提醒，把用户带到对应页面
    WidgetsBinding.instance.addPostFrameCallback((_) => _consumeInitialJump());
  }

  void _consumeInitialJump() {
    final target = AppTabJump.target.value;
    if (target != null) {
      _switchTo(target);
      AppTabJump.target.value = null;
    }
  }

  @override
  void dispose() {
    AppTabJump.target.removeListener(_onTabJump);
    super.dispose();
  }

  void _onTabJump() {
    final target = AppTabJump.target.value;
    if (target == null) return;
    _switchTo(target);
    AppTabJump.target.value = null;
  }

  void _switchTo(int index) {
    if (!mounted) return;
    setState(() => _index = index);
    // 进入页面即视为已读，清除对应角标
    if (index == AppTabIndex.orders) {
      ref.read(alertProvider.notifier).clearBadge(AlertKind.newOrder);
    } else if (index == AppTabIndex.kitchen) {
      ref.read(alertProvider.notifier).clearBadge(AlertKind.newKitchenItem);
    }
  }

  @override
  Widget build(BuildContext context) {
    final alert = ref.watch(alertProvider);
    final store = ref.watch(currentStoreProvider);
    // 未选择门店时引导用户先在「我的」里处理
    final storeMissing = store == null;

    return Scaffold(
      body: Stack(
        children: [
          IndexedStack(index: _index, children: _pages),
          if (storeMissing)
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: _NoStoreBanner(
                onConfigure: () => setState(() => _index = AppTabIndex.profile),
              ),
            ),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _index,
        onTap: _switchTo,
        type: BottomNavigationBarType.fixed,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.textHint,
        selectedFontSize: 11,
        unselectedFontSize: 11,
        items: [
          BottomNavigationBarItem(
            icon: const Icon(Symbols.dashboard),
            activeIcon: const Icon(Symbols.dashboard, fill: 1),
            label: '看板',
          ),
          BottomNavigationBarItem(
            icon: BadgeIcon(
              icon: Symbols.receipt_long,
              showBadge: alert.newOrder,
            ),
            activeIcon: BadgeIcon(
              icon: Symbols.receipt_long,
              fill: 1,
              showBadge: alert.newOrder,
            ),
            label: '订单',
          ),
          BottomNavigationBarItem(
            icon: BadgeIcon(
              icon: Symbols.chef_hat,
              showBadge: alert.newKitchenItem,
            ),
            activeIcon: BadgeIcon(
              icon: Symbols.chef_hat,
              showBadge: alert.newKitchenItem,
            ),
            label: '后厨',
          ),
          BottomNavigationBarItem(
            icon: const Icon(Symbols.storefront),
            activeIcon: const Icon(Symbols.storefront, fill: 1),
            label: '管理',
          ),
          BottomNavigationBarItem(
            icon: const Icon(Symbols.manage_accounts),
            activeIcon: const Icon(Symbols.manage_accounts, fill: 1),
            label: '我的',
          ),
        ],
      ),
    );
  }
}

/// 带小红点的导航图标
class BadgeIcon extends StatelessWidget {
  const BadgeIcon({
    super.key,
    required this.icon,
    this.fill,
    this.showBadge = false,
  });

  final IconData icon;
  final double? fill;
  final bool showBadge;

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Icon(icon, fill: fill),
        if (showBadge)
          Positioned(
            right: -3,
            top: -2,
            child: Container(
              width: 8,
              height: 8,
              decoration: const BoxDecoration(
                color: AppColors.danger,
                shape: BoxShape.circle,
              ),
            ),
          ),
      ],
    );
  }
}

/// 未选择门店时的提示条
class _NoStoreBanner extends ConsumerWidget {
  const _NoStoreBanner({required this.onConfigure});

  final VoidCallback onConfigure;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contextState = ref.watch(storeContextProvider);
    final user = ref.watch(currentUserProvider);
    if (contextState.loading) return const SizedBox.shrink();
    return Material(
      elevation: 2,
      child: Container(
        width: double.infinity,
        padding: EdgeInsets.only(
          top: MediaQuery.of(context).padding.top + 8,
          bottom: 10,
          left: 16,
          right: 16,
        ),
        color: AppColors.warningLight,
        child: Row(
          children: [
            const Icon(Icons.info_outline, size: 18, color: AppColors.warning),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                user == null
                    ? '正在加载门店…'
                    : '当前账号暂无可管理的门店，请联系店主开通权限',
                style: const TextStyle(
                  fontSize: 12.5,
                  color: Color(0xFF8A5A00),
                ),
              ),
            ),
            TextButton(
              onPressed: onConfigure,
              child: const Text('查看'),
            ),
          ],
        ),
      ),
    );
  }
}
