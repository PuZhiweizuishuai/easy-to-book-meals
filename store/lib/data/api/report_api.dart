import '../../core/network/api_client.dart';
import '../models/models.dart';

/// 门店报表接口（店主端，需要 stats:store 权限）
class ReportApi {
  const ReportApi(this._client);

  final ApiClient _client;

  /// 指定日期经营概览
  Future<DailyOverview> overview({
    required int storeId,
    String? date,
  }) async {
    final result = await _client.get<DailyOverview>(
      '/manage/report/overview',
      queryParameters: {'storeId': storeId, 'date': date},
      parser: (raw) =>
          DailyOverview.fromJson(raw is Map<String, dynamic> ? raw : const {}),
    );
    return result ?? const DailyOverview();
  }

  /// 历史累计概览
  Future<Map<String, dynamic>> total({required int storeId}) =>
      _client
          .get<Map<String, dynamic>>(
            '/manage/report/total',
            queryParameters: {'storeId': storeId},
            parser: (raw) => raw is Map<String, dynamic> ? raw : const {},
          )
          .then((value) => value ?? const {});

  /// 销售额 / 订单量走势
  Future<TrendResult> trend({
    required int storeId,
    String? start,
    String? end,
    String granularity = 'day',
  }) async {
    final result = await _client.get<TrendResult>(
      '/manage/report/trend',
      queryParameters: {
        'storeId': storeId,
        'start': start,
        'end': end,
        'granularity': granularity,
      },
      parser: (raw) =>
          TrendResult.fromJson(raw is Map<String, dynamic> ? raw : const {}),
    );
    return result ?? const TrendResult(points: []);
  }

  /// 畅销菜品榜
  Future<TopDishResult> topDishes({
    required int storeId,
    String range = 'day',
    String? date,
    int limit = 10,
  }) async {
    final result = await _client.get<TopDishResult>(
      '/manage/report/top-dishes',
      queryParameters: {
        'storeId': storeId,
        'range': range,
        'date': date,
        'limit': limit,
      },
      parser: (raw) =>
          TopDishResult.fromJson(raw is Map<String, dynamic> ? raw : const {}),
    );
    return result ?? const TopDishResult(dishes: []);
  }
}
