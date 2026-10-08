import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_exception.dart';
import '../../core/services/app_logger.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../providers/store_context_provider.dart';

/// 看板聚合数据
class DashboardData {
  const DashboardData({
    required this.stats,
    required this.overview,
    required this.trend,
    required this.updatedAt,
    this.reportError,
  });

  /// 门店实时统计（/order/manage/stats）
  final StoreStats stats;

  /// 当日经营概览
  final DailyOverview overview;

  /// 近 7 天走势
  final TrendResult trend;

  /// 本次数据的拉取时间（页面上展示「更新于 xx:xx」）
  final DateTime updatedAt;

  /// 报表类接口的错误提示（多数是缺少 stats:store 权限），仅提示不阻塞展示
  final String? reportError;

  /// 今日营业额
  ///
  /// 以实时统计接口为准；该接口未返回有效数值时回退到当日经营概览的
  /// [DailyOverview.revenue]，避免出现「已有营业额却显示 ¥0.00」。
  double get todayRevenue {
    final fromStats = stats.todayRevenue;
    if (fromStats != null && fromStats > 0) return fromStats;
    final fromOverview = overview.revenue;
    if (fromOverview != null && fromOverview > 0) return fromOverview;
    return fromStats ?? fromOverview ?? 0;
  }

  /// 今日订单数（同样做一次兜底）
  int get todayOrderCount {
    final fromStats = stats.todayOrderCount;
    if (fromStats != null && fromStats > 0) return fromStats;
    return stats.todayOrderCount ?? overview.orderCount ?? 0;
  }
}

/// 首页数据看板数据
///
/// 一次性拉取「实时统计 + 当日概览 + 近 7 天走势」：
/// - 实时统计是核心指标，失败直接抛错由页面展示重试入口；
/// - 报表类接口需要 stats:store 权限，店员账号常常没有，失败时降级为空数据，
///   不再因为一个接口失败就让整个看板不可用。
///
/// 畅销榜按 Tab 区间单独拉取（[topDishesProvider]），不在这里。
///
/// 页面本身不再关心各接口细节，下拉时只需 `ref.refresh`。
final dashboardProvider =
    FutureProvider.autoDispose<DashboardData>((ref) async {
  final storeId = ref.watch(currentStoreIdProvider);
  if (storeId == null) {
    throw ApiException('暂无可用门店，请联系店主开通权限');
  }
  // 写操作（结算 / 出餐 / 上下架 / 保存菜品）与「切回看板 Tab」「App 回到前台」
  // 都会 bump 这个计数器；watch 到变化即重新拉取，
  // 解决页面常驻 IndexedStack、切回来还显示进入时旧数据的问题。
  ref.watch(dataRefreshProvider);

  final orderApi = ref.read(orderApiProvider);
  final reportApi = ref.read(reportApiProvider);

  final now = DateTime.now();
  final today = TimeFormat.apiDate(now);
  final weekStart = TimeFormat.apiDate(now.subtract(const Duration(days: 6)));

  final errors = <String>[];

  final List<dynamic> results = await Future.wait<dynamic>([
    orderApi.stats(storeId: storeId),
    _tolerant(
      () => reportApi.overview(storeId: storeId, date: today),
      const DailyOverview(),
      errors,
    ),
    _tolerant(
      () => reportApi.trend(storeId: storeId, start: weekStart, end: today),
      const TrendResult(points: []),
      errors,
    ),
  ]);

  return DashboardData(
    stats: results[0] as StoreStats,
    overview: results[1] as DailyOverview,
    trend: results[2] as TrendResult,
    updatedAt: DateTime.now(),
    reportError: errors.isEmpty ? null : errors.first,
  );
});

/// 畅销榜统计区间，[name] 直接对应后端 top-dishes 的 range 参数
enum TopDishRange {
  day('今日', '今日 · 仅已完成订单', '今日暂无售出菜品'),
  month('本月', '本月累计 · 仅已完成订单', '本月暂无售出菜品'),
  all('历史', '历史累计 · 仅已完成订单', '暂无历史售出菜品');

  const TopDishRange(this.tabLabel, this.hint, this.emptyTitle);

  /// Tab 文案
  final String tabLabel;

  /// 榜单口径说明
  final String hint;

  /// 空数据文案
  final String emptyTitle;
}

/// 畅销榜（区间 Tab 单独拉取，切 Tab 只重发这一个请求）
final topDishesProvider = FutureProvider.autoDispose
    .family<TopDishResult, TopDishRange>((ref, range) async {
  final storeId = ref.watch(currentStoreIdProvider);
  if (storeId == null) {
    throw ApiException('暂无可用门店，请联系店主开通权限');
  }
  // 与看板主体同步刷新（结算 / 出餐 / 回到前台都会 bump）
  ref.watch(dataRefreshProvider);

  final today = TimeFormat.apiDate(DateTime.now());
  return ref.read(reportApiProvider).topDishes(
        storeId: storeId,
        range: range.name,
        // 历史区间后端会忽略 date，这里也不传
        date: range == TopDishRange.all ? null : today,
        limit: 10,
      );
});

/// 可选请求：失败时记录原因并返回兜底值，不打断其它请求
Future<T> _tolerant<T>(
  Future<T> Function() call,
  T fallback,
  List<String> errors,
) async {
  try {
    return await call();
  } catch (e, s) {
    final message = e is ApiException ? e.message : e.toString();
    AppLogger.w('看板报表数据加载失败：$message', error: e, stackTrace: s);
    errors.add(message);
    return fallback;
  }
}
