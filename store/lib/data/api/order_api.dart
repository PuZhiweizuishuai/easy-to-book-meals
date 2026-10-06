import '../../core/network/api_client.dart';
import '../../core/network/api_defaults.dart';
import '../models/models.dart';

/// 订单管理接口（店主 / 店员端）
///
/// 查询类接口需要 order:view 权限，处理类需要 order:manage（出餐额外允许 kitchen:manage）。
class OrderApi {
  const OrderApi(this._client);

  final ApiClient _client;

  // ==================== 列表与详情 ====================

  /// 分页查询门店订单
  Future<PageResult<OrderData>> list({
    required int storeId,
    int? status,
    int? orderType,
    String? key,
    int page = 1,
    int size = 10,
  }) =>
      orEmptyPage(
        _client.get<PageResult<OrderData>>(
          '/order/manage/list',
          queryParameters: {
            'storeId': storeId,
            'status': status,
            'orderType': orderType,
            'key': key,
            'page': page,
            'size': size,
          },
          parser: (raw) => PageResult.fromJson(raw, OrderData.fromJson),
        ),
      );

  /// 订单详情（含明细）
  Future<OrderData?> detail({
    required int orderId,
    required int storeId,
  }) =>
      _client.get<OrderData?>(
        '/order/manage/detail/$orderId',
        queryParameters: {'storeId': storeId},
        parser: (raw) => raw is Map<String, dynamic> ? OrderData.fromJson(raw) : null,
      );

  /// 门店实时统计（今日单量 / 营业额 / 待接单 / 制作中）
  Future<StoreStats> stats({required int storeId}) => orDefault(
        _client.get<StoreStats>(
          '/order/manage/stats',
          queryParameters: {'storeId': storeId},
          parser: (raw) =>
              StoreStats.fromJson(raw is Map<String, dynamic> ? raw : const {}),
        ),
        const StoreStats(),
      );

  // ==================== 订单状态流转 ====================

  /// 接单
  Future<OrderData?> confirm({required int orderId, required int storeId}) =>
      _post('/order/manage/confirm/$orderId', storeId: storeId);

  /// 拒单（取消订单并恢复库存）
  Future<OrderData?> reject({
    required int orderId,
    required int storeId,
    String? reason,
  }) =>
      _post(
        '/order/manage/reject/$orderId',
        storeId: storeId,
        extra: {'reason': reason},
      );

  /// 开始制作
  Future<OrderData?> cooking({required int orderId, required int storeId}) =>
      _post('/order/manage/cooking/$orderId', storeId: storeId);

  /// 出餐
  Future<OrderData?> ready({required int orderId, required int storeId}) =>
      _post('/order/manage/ready/$orderId', storeId: storeId);

  /// 完成订单（结算）
  ///
  /// [phone] 会员手机号（可用于积分累积）、[couponCode] 券码、
  /// [manualDiscount] 手动减免金额。
  Future<OrderData?> complete({
    required int orderId,
    required int storeId,
    String? phone,
    String? couponCode,
    double? manualDiscount,
  }) =>
      _post(
        '/order/manage/complete/$orderId',
        storeId: storeId,
        extra: {
          'phone': phone,
          'couponCode': couponCode,
          'manualDiscount': manualDiscount,
        },
      );

  // ==================== 菜品级操作 ====================

  /// 单个菜品出餐
  Future<OrderData?> serveItem({required int itemId, required int storeId}) =>
      _post('/order/manage/serve-item/$itemId', storeId: storeId);

  /// 整单出餐
  Future<OrderData?> serveOrder({required int orderId, required int storeId}) =>
      _post('/order/manage/serve-order/$orderId', storeId: storeId);

  /// 开始制作（认领菜品）
  Future<OrderData?> startPreparing({
    required int itemId,
    required int storeId,
  }) =>
      _post('/order/manage/start-preparing/$itemId', storeId: storeId);

  /// 释放制作（退回待制作队列）
  Future<OrderData?> releasePreparing({
    required int itemId,
    required int storeId,
  }) =>
      _post('/order/manage/release-preparing/$itemId', storeId: storeId);

  /// 退菜（移除未出餐的菜品）
  Future<OrderData?> removeItem({
    required int itemId,
    required int storeId,
    String? reason,
  }) =>
      _post(
        '/order/manage/remove-item/$itemId',
        storeId: storeId,
        extra: {'reason': reason},
      );

  /// 调整菜品数量 / 重量
  Future<OrderData?> adjustItem({
    required int itemId,
    required int storeId,
    required double quantity,
  }) =>
      _post(
        '/order/manage/adjust-item/$itemId',
        storeId: storeId,
        extra: {'quantity': quantity},
      );

  // ==================== 后厨队列 ====================

  /// 后厨待制作队列（菜品维度）
  Future<KitchenQueueResult> kitchenQueue({required int storeId}) async {
    final result = await _client.get<KitchenQueueResult>(
      '/order/manage/kitchen/queue',
      queryParameters: {'storeId': storeId},
      parser: (raw) => raw is Map<String, dynamic>
          ? KitchenQueueResult.fromJson(raw)
          : const KitchenQueueResult(
              items: [],
              itemCount: 0,
              maxWaitedMinutes: 0,
            ),
    );
    return result ??
        const KitchenQueueResult(
          items: [],
          itemCount: 0,
          maxWaitedMinutes: 0,
        );
  }

  /// 统一的「POST + storeId + 附加 query」请求
  Future<OrderData?> _post(
    String path, {
    required int storeId,
    Map<String, dynamic>? extra,
  }) =>
      _client.post<OrderData?>(
        path,
        queryParameters: {'storeId': storeId, ...?extra},
        parser: (raw) => raw is Map<String, dynamic> ? OrderData.fromJson(raw) : null,
      );
}
