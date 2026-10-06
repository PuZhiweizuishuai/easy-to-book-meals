import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/config/app_config.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/alert_provider.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_network_image.dart';
import '../../shared/widgets/simple_bar_chart.dart';
import '../../shared/widgets/state_views.dart';
import 'dashboard_provider.dart';

/// 首页数据看板
///
/// 展示今日核心指标、近 7 天营收走势与当日畅销榜，
/// 顶部提供门店切换；右下角固定「立即刷新」按钮便于店员手动拉取最新数据。
class DashboardPage extends ConsumerStatefulWidget {
  const DashboardPage({super.key});

  @override
  ConsumerState<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends ConsumerState<DashboardPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // 进入首页也算看过提醒，清除订单角标
      ref.read(alertProvider.notifier).clearBadge(AlertKind.newOrder);
    });
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
            onPressed: () => ref.refresh(dashboardProvider),
          ),
        ],
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        onRefresh: () async => ref.refresh(dashboardProvider.future),
        child: dashboard.when(
          loading: () => ListView(
            children: const [SizedBox(height: 80), LoadingView()],
          ),
          error: (error, _) => ListView(
            children: [
              const SizedBox(height: 60),
              ErrorView(
                message: error.toString(),
                onRetry: () => ref.refresh(dashboardProvider),
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
    final dishes = data.topDishes.dishes;

    return ListView(
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 24),
      children: [
        // 今日核心指标
        Row(
          children: [
            Expanded(
              child: _MetricTile(
                icon: Symbols.payments,
                color: AppColors.primary,
                label: '今日营业额',
                value: MoneyFormat.yuan(stats.todayRevenue ?? 0),
                alignRight: false,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _MetricTile(
                icon: Symbols.receipt_long,
                color: AppColors.chartAccent,
                label: '今日订单',
                value: '${stats.todayOrderCount ?? 0}',
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
                    BarPoint(
                      label: point.label ?? '',
                      value: point.amount,
                      valueText:
                          MoneyFormat.compact(point.amount),
                    ),
                ],
                height: 120,
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),

        // 畅销榜
        const SectionTitle(
          title: '今日畅销榜',
          padding: EdgeInsets.fromLTRB(0, 0, 0, 8),
        ),
        if (dishes.isEmpty)
          AppCard(
            child: EmptyView(
              title: '今日暂无售出菜品',
              compact: true,
              icon: Symbols.lunch_dining,
            ),
          )
        else
          AppCard(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Column(
              children: [
                for (var i = 0; i < dishes.length; i++)
                  _TopDishTile(index: i, dish: dishes[i]),
              ],
            ),
          ),
      ],
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
