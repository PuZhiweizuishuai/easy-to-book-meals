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
    required this.topDishes,
    required this.updatedAt,
    this.reportError,
  });

  /// 门店实时统计（/order/manage/stats）
  final StoreStats stats;

  /// 当日经营概览
  final DailyOverview overview;

  /// 近 7 天走势
  final TrendResult trend;

  /// 当日畅销榜
  final TopDishResult topDishes;

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
/// 一次性拉取「实时统计 + 当日概览 + 近 7 天走势 + 当日畅销榜」：
/// - 实时统计是核心指标，失败直接抛错由页面展示重试入口；
/// - 报表类接口需要 stats:store 权限，店员账号常常没有，失败时降级为空数据，
///   不再因为一个接口失败就让整个看板不可用。
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
    _tolerant(
      () => reportApi.topDishes(
        storeId: storeId,
        range: 'day',
        date: today,
        limit: 8,
      ),
      const TopDishResult(dishes: []),
      errors,
    ),
  ]);

  return DashboardData(
    stats: results[0] as StoreStats,
    overview: results[1] as DailyOverview,
    trend: results[2] as TrendResult,
    topDishes: results[3] as TopDishResult,
    updatedAt: DateTime.now(),
    reportError: errors.isEmpty ? null : errors.first,
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
