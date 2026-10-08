import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/alert_provider.dart';
import '../../providers/refresh_bus.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_filter_chip.dart';
import '../../shared/widgets/app_network_image.dart';
import '../../shared/widgets/simple_bar_chart.dart';
import '../../shared/widgets/state_views.dart';
import '../orders/order_create_page.dart';
import 'dashboard_provider.dart';

/// 首页数据看板
///
/// 展示今日核心指标、近 7 天营收走势（柱顶标注金额）与畅销榜
/// （今日 / 本月 / 历史三档切换），顶部提供门店切换；
/// 右下角固定「立即刷新」按钮便于店员手动拉取最新数据。
class DashboardPage extends ConsumerStatefulWidget {
  const DashboardPage({super.key});

  @override
  ConsumerState<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends ConsumerState<DashboardPage>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    // App 从后台回到前台时自动刷新：店员多半是「结算完订单回到看板看营业额」，
    // 页面常驻在 IndexedStack 里不会重建，不刷新的话一直显示进入时的旧数据。
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // 进入首页也算看过提醒，清除订单角标
      ref.read(alertProvider.notifier).clearBadge(AlertKind.newOrder);
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      ref.read(dataRefreshProvider.notifier).bump();
    }
  }

  /// 手动刷新：看板主体 + 畅销榜各区间
  ///
  /// 畅销榜是独立的 family provider，下拉/点刷新时一并失效，
  /// 否则会出现「营收是新的、榜单还是旧的」。
  Future<void> _reload() {
    for (final range in TopDishRange.values) {
      ref.invalidate(topDishesProvider(range));
    }
    return ref.refresh(dashboardProvider.future);
  }

  @override
  Widget build(BuildContext context) {
    final dashboard = ref.watch(dashboardProvider);

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        titleSpacing: 14,
        title: const _StoreSwitcher(),
        actions: [
          IconButton(
            tooltip: '刷新',
            icon: const Icon(Icons.refresh_rounded, size: 22),
            onPressed: _reload,
          ),
        ],
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        onRefresh: _reload,
        child: dashboard.when(
          loading: () => ListView(
            children: const [SizedBox(height: 80), LoadingView()],
          ),
          error: (error, _) => ListView(
            children: [
              const SizedBox(height: 60),
              ErrorView(
                message: error.toString(),
                onRetry: _reload,
              ),
            ],
          ),
          data: (data) => _DashboardBody(data: data),
        ),
      ),
    );
  }
}

/// 门店切换器（多门店店员可下拉切换）
class _StoreSwitcher extends ConsumerWidget {
  const _StoreSwitcher();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final storeContext = ref.watch(storeContextProvider);
    final current = storeContext.current;

    if (storeContext.stores.isEmpty) {
      return Text(
        AppConfig.appName,
        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
      );
    }
    if (storeContext.stores.length == 1) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            current?.displayName ?? AppConfig.appName,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          ),
          Text(
            '${storeStatusName(current?.status)} · ${TimeFormat.date(DateTime.now().millisecondsSinceEpoch)}',
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w400,
              color: AppColors.textHint,
            ),
          ),
        ],
      );
    }

    return DropdownButton<int?>(
      value: current?.id,
      underline: const SizedBox.shrink(),
      icon: const Icon(Icons.keyboard_arrow_down_rounded, size: 20),
      style: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: AppColors.textPrimary,
      ),
      items: [
        for (final store in storeContext.stores)
          DropdownMenuItem<int?>(
            value: store.id,
            child: Text(
              store.displayName,
              overflow: TextOverflow.ellipsis,
            ),
          ),
      ],
      onChanged: (id) async {
        if (id == null) return;
        await ref.read(storeContextProvider.notifier).selectStore(id);
        ref.invalidate(dashboardProvider);
      },
    );
  }
}

class _DashboardBody extends StatelessWidget {
  const _DashboardBody({required this.data});

  final DashboardData data;

  @override
  Widget build(BuildContext context) {
    final stats = data.stats;
    final overview = data.overview;
    final trend = data.trend;

    return ListView(
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 24),
      children: [
        // 店员协助下单快捷入口（需 order:manage 权限）
        const _StaffOrderBanner(),
        const SizedBox(height: 14),
        // 今日核心指标
        Row(
          children: [
            Expanded(
              child: _MetricTile(
                icon: Symbols.payments,
                color: AppColors.primary,
                label: '今日营业额',
                value: MoneyFormat.yuan(data.todayRevenue),
                alignRight: false,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _MetricTile(
                icon: Symbols.receipt_long,
                color: AppColors.chartAccent,
                label: '今日订单',
                value: '${data.todayOrderCount}',
                unit: '单',
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: _MetricTile(
                icon: Symbols.pending,
                color: AppColors.warning,
                label: '待接单',
                value: '${stats.pendingCount ?? 0}',
                unit: '单',
                highlight: (stats.pendingCount ?? 0) > 0,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _MetricTile(
                icon: Symbols.soup_kitchen,
                color: AppColors.info,
                label: '制作中',
                value: '${stats.cookingCount ?? 0}',
                unit: '单',
              ),
            ),
          ],
        ),
        if (data.reportError != null) ...[
          const SizedBox(height: 10),
          _ReportErrorHint(message: data.reportError!),
        ],
        const SizedBox(height: 14),

        // 当日经营概览
        SectionTitle(
          title: '今日概览',
          subtitle: overview.date ?? '',
          padding: const EdgeInsets.fromLTRB(0, 0, 0, 8),
        ),
        AppCard(
          child: Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: _InfoTile(
                      label: '已完成',
                      value: '${overview.completedCount ?? 0}',
                    ),
                  ),
                  Expanded(
                    child: _InfoTile(
                      label: '已取消',
                      value: '${overview.cancelledCount ?? 0}',
                    ),
                  ),
                  Expanded(
                    child: _InfoTile(
                      label: '会员单',
                      value: '${overview.memberOrderCount ?? 0}',
                    ),
                  ),
                ],
              ),
              const Divider(height: 16),
              Row(
                children: [
                  Expanded(
                    child: _InfoTile(
                      label: '客单价',
                      value: MoneyFormat.yuan(overview.avgOrderAmount ?? 0),
                    ),
                  ),
                  Expanded(
                    child: _InfoTile(
                      label: '售出件数',
                      value: MoneyFormat.quantity(overview.itemCount ?? 0),
                    ),
                  ),
                  Expanded(
                    child: _InfoTile(
                      label: '优惠合计',
                      value: MoneyFormat.yuan(overview.discountAmount ?? 0),
                    ),
                  ),
                ],
              ),
              const Divider(height: 16),
              Row(
                children: [
                  Expanded(
                    child: _InfoTile(
                      label: '历史营业额',
                      value: MoneyFormat.yuan(overview.totalRevenue ?? 0),
                    ),
                  ),
                  Expanded(
                    child: _InfoTile(
                      label: '历史订单',
                      value: '${overview.totalOrderCount ?? 0}',
                    ),
                  ),
                  const Expanded(child: SizedBox()),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),

        // 近 7 天走势
        SectionTitle(
          title: '近 7 天营收趋势',
          subtitle: '合计 ${MoneyFormat.yuan(trend.totalRevenue)}',
          padding: const EdgeInsets.fromLTRB(0, 0, 0, 8),
        ),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '区间总单量 ${trend.totalOrders} 单',
                style: const TextStyle(
                  fontSize: 12,
                  color: AppColors.textHint,
                ),
              ),
              const SizedBox(height: 10),
              SimpleBarChart(
                points: [
                  for (final point in trend.points)
                    BarPoint(label: point.label ?? '', value: point.amount),
                ],
                height: 148,
                // 每根柱子顶部标注金额，不用手点就能读出销售额
                showAllValues: true,
                valueFormatter: MoneyFormat.chartLabel,
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),

        // 畅销榜（今日 / 本月 / 历史）
        const _TopDishSection(),
        const SizedBox(height: 12),
        Center(
          child: Text(
            '数据更新于 ${TimeFormat.time(data.updatedAt.millisecondsSinceEpoch)} · 下拉可刷新',
            style: const TextStyle(fontSize: 11.5, color: AppColors.textHint),
          ),
        ),
      ],
    );
  }
}

/// 畅销榜区块：今日 / 本月 / 历史三档切换
///
/// 与 Web 端数据报表的畅销菜品 Tab 一致；榜单走独立的 [topDishesProvider]，
/// 切 Tab 只重发畅销榜这一个请求。
class _TopDishSection extends ConsumerStatefulWidget {
  const _TopDishSection();

  @override
  ConsumerState<_TopDishSection> createState() => _TopDishSectionState();
}

class _TopDishSectionState extends ConsumerState<_TopDishSection> {
  TopDishRange _range = TopDishRange.day;

  @override
  Widget build(BuildContext context) {
    final topDishes = ref.watch(topDishesProvider(_range));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SectionTitle(
          title: '畅销榜',
          subtitle: _range.hint,
          padding: const EdgeInsets.fromLTRB(0, 0, 0, 8),
        ),
        Row(
          children: [
            for (final range in TopDishRange.values) ...[
              if (range != TopDishRange.values.first)
                const SizedBox(width: 8),
              AppFilterChip(
                label: range.tabLabel,
                selected: range == _range,
                showCheck: false,
                height: 30,
                onTap: () => setState(() => _range = range),
              ),
            ],
          ],
        ),
        const SizedBox(height: 10),
        topDishes.when(
          loading: () => const AppCard(child: LoadingView(height: 90)),
          error: (error, _) => AppCard(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: Row(
              children: [
                const Icon(
                  Icons.error_outline,
                  size: 16,
                  color: AppColors.danger,
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    error is ApiException ? error.message : '畅销榜加载失败',
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: () => ref.invalidate(topDishesProvider(_range)),
                  child: const Text('重试', style: TextStyle(fontSize: 12.5)),
                ),
              ],
            ),
          ),
          data: (result) => result.dishes.isEmpty
              ? AppCard(
                  child: EmptyView(
                    title: _range.emptyTitle,
                    compact: true,
                    icon: Symbols.lunch_dining,
                  ),
                )
              : AppCard(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Column(
                    children: [
                      for (var i = 0; i < result.dishes.length; i++)
                        _TopDishTile(index: i, dish: result.dishes[i]),
                    ],
                  ),
                ),
        ),
      ],
    );
  }
}

/// 看板顶部的「协助下单」快捷入口
///
/// 仅拥有 order:manage 权限的店员可见；下单成功后协助下单页会 bump
/// 刷新总线，看板指标自动重拉。
class _StaffOrderBanner extends ConsumerWidget {
  const _StaffOrderBanner();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final canStaffCreate = ref
            .watch(currentUserProvider)
            ?.hasAnyPermission(const ['order:manage']) ??
        false;
    if (!canStaffCreate) return const SizedBox.shrink();

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute<bool>(
            builder: (_) => const StaffOrderCreatePage(),
          ),
        ),
        child: Ink(
          padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 14),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [AppColors.primary, AppColors.primaryDark],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Symbols.post_add,
                  size: 23,
                  color: Colors.white,
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '协助下单',
                      style: TextStyle(
                        fontSize: 15.5,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      '店员代客下单：选菜品 → 填信息 → 提交',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.white70,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right_rounded,
                  size: 22, color: Colors.white),
            ],
          ),
        ),
      ),
    );
  }
}

/// 报表接口不可用时的提示（多为缺少 stats:store 权限）
class _ReportErrorHint extends StatelessWidget {
  const _ReportErrorHint({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
      decoration: BoxDecoration(
        color: AppColors.warningLight,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.info_outline, size: 15, color: AppColors.warning),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              '经营概览数据未加载：$message',
              style: const TextStyle(fontSize: 12, color: Color(0xFF8A5A00)),
            ),
          ),
        ],
      ),
    );
  }
}

/// 顶部大指标卡
class _MetricTile extends StatelessWidget {
  const _MetricTile({
    required this.icon,
    required this.color,
    required this.label,
    required this.value,
    this.unit,
    this.highlight = false,
    this.alignRight = true,
  });

  final IconData icon;
  final Color color;
  final String label;
  final String value;
  final String? unit;
  final bool highlight;
  final bool alignRight;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.fromLTRB(14, 13, 14, 13),
      color: highlight ? color.withValues(alpha: 0.06) : Colors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 17, color: color),
              const SizedBox(width: 6),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 12.5,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          RichText(
            text: TextSpan(
              children: [
                TextSpan(
                  text: value,
                  style: TextStyle(
                    fontSize: 21,
                    fontWeight: FontWeight.w700,
                    color: color,
                  ),
                ),
                if (unit != null)
                  TextSpan(
                    text: ' $unit',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textHint,
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

/// 概览小格
class _InfoTile extends StatelessWidget {
  const _InfoTile({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 12, color: AppColors.textHint),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }
}

/// 畅销榜条目
class _TopDishTile extends StatelessWidget {
  const _TopDishTile({required this.index, required this.dish});

  final int index;
  final TopDish dish;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Row(
        children: [
          SizedBox(
            width: 22,
            child: Text(
              '${index + 1}',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: index < 3
                    ? AppColors.primary
                    : AppColors.textHint,
              ),
            ),
          ),
          AppNetworkImage(
            url: dish.topImage,
            size: 40,
            placeholderIcon: Symbols.lunch_dining,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              dish.dishName ?? '未知菜品',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '${MoneyFormat.quantity(dish.quantity ?? 0)} 份',
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
              ),
              Text(
                MoneyFormat.yuan(dish.revenue ?? 0),
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
