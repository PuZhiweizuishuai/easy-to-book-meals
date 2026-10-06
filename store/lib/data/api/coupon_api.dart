import '../../core/network/api_client.dart';
import '../models/models.dart';

/// 优惠券接口
///
/// 移动端的核心能力是「扫码核销」：扫描券码二维码 → 校验是否可用 → 自动抵扣订单金额。
class CouponApi {
  const CouponApi(this._client);

  final ApiClient _client;

  /// 校验券码在当前订单金额下是否可用（仅试算，不核销）
  Future<CouponCheckResult> check({
    required int storeId,
    required String code,
    required double amount,
  }) async {
    final result = await _client.get<CouponCheckResult>(
      '/coupon/check',
      queryParameters: {
        'storeId': storeId,
        'code': code,
        'amount': amount,
      },
      parser: (raw) => raw is Map<String, dynamic>
          ? CouponCheckResult.fromJson(raw)
          : const CouponCheckResult(),
    );
    return result ?? const CouponCheckResult();
  }

  /// 按券码查询券详情（未登录也可查询）
  Future<UserCoupon?> detail({required String code}) =>
      _client.get<UserCoupon?>(
        '/coupon/detail/$code',
        parser: (raw) =>
            raw is Map<String, dynamic> ? UserCoupon.fromJson(raw) : null,
      );
}
