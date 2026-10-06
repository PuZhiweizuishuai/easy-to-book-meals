import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/store_context_provider.dart';

/// 看板聚合数据
class DashboardData {
  const DashboardData({
    required this.stats,
    required this.overview,
    required this.trend,
    required this.topDishes,
  });

  /// 门店实时统计
  final StoreStats stats;

  /// 当日经营概览
  final DailyOverview overview;

  /// 近 7 天走势
  final TrendResult trend;

  /// 当日畅销榜
  final TopDishResult topDishes;
}

/// 首页数据看板数据
///
/// 一次性拉取「实时统计 + 当日概览 + 近 7 天走势 + 当日畅销榜」，
/// 页面本身不再关心各接口细节，下拉时只需 `ref.refresh`。
final dashboardProvider =
    FutureProvider.autoDispose<DashboardData>((ref) async {
  final storeId = ref.watch(currentStoreIdProvider);
  if (storeId == null) {
    throw ApiException('暂无可用门店，请联系店主开通权限');
  }

  final orderApi = ref.read(orderApiProvider);
  final reportApi = ref.read(reportApiProvider);

  final now = DateTime.now();
  final today = TimeFormat.apiDate(now);
  final weekStart = TimeFormat.apiDate(now.subtract(const Duration(days: 6)));

  final List<dynamic> results = await Future.wait<dynamic>([
    orderApi.stats(storeId: storeId),
    reportApi.overview(storeId: storeId, date: today),
    reportApi.trend(storeId: storeId, start: weekStart, end: today),
    reportApi.topDishes(storeId: storeId, range: 'day', date: today, limit: 8),
  ]);

  return DashboardData(
    stats: results[0] as StoreStats,
    overview: results[1] as DailyOverview,
    trend: results[2] as TrendResult,
    topDishes: results[3] as TopDishResult,
  );
});
