import '../../core/network/api_client.dart';
import '../../core/network/api_defaults.dart';
import '../models/models.dart';

/// 会员管理接口（店主端）
///
/// 会员统一以「门店 + 手机号」为维度，未注册顾客也能按手机号累积积分。
/// 所有接口需要 member:manage 权限。
class MembershipApi {
  const MembershipApi(this._client);

  final ApiClient _client;

  /// 会员列表（支持手机号模糊搜索）
  Future<PageResult<Membership>> list({
    required int storeId,
    String? key,
    int page = 1,
    int size = 10,
  }) =>
      orEmptyPage(
        _client.get<PageResult<Membership>>(
          '/manage/membership/list',
          queryParameters: {
            'storeId': storeId,
            'key': key,
            'page': page,
            'size': size,
          },
          parser: (raw) => PageResult.fromJson(raw, Membership.fromJson),
        ),
      );

  /// 按手机号查询会员（结算时识别会员；无记录返回 null）
  Future<Membership?> byPhone({
    required int storeId,
    required String phone,
  }) =>
      _client.get<Membership?>(
        '/manage/membership/by-phone',
        queryParameters: {'storeId': storeId, 'phone': phone},
        parser: (raw) =>
            raw is Map<String, dynamic> ? Membership.fromJson(raw) : null,
      );

  /// 会员详情
  Future<Membership?> detail({required int id, required int storeId}) =>
      _client.get<Membership?>(
        '/manage/membership/$id',
        queryParameters: {'storeId': storeId},
        parser: (raw) =>
            raw is Map<String, dynamic> ? Membership.fromJson(raw) : null,
      );

  /// 会员积分流水
  Future<PageResult<PointsTransaction>> transactions({
    required int id,
    required int storeId,
    int page = 1,
    int size = 20,
  }) =>
      orEmptyPage(
        _client.get<PageResult<PointsTransaction>>(
          '/manage/membership/$id/transactions',
          queryParameters: {
            'storeId': storeId,
            'page': page,
            'size': size,
          },
          parser: (raw) => PageResult.fromJson(raw, PointsTransaction.fromJson),
        ),
      );

  /// 会员关联订单（按手机号统计的消费记录）
  Future<PageResult<OrderData>> orders({
    required int id,
    required int storeId,
    int page = 1,
    int size = 20,
  }) =>
      orEmptyPage(
        _client.get<PageResult<OrderData>>(
          '/manage/membership/$id/orders',
          queryParameters: {
            'storeId': storeId,
            'page': page,
            'size': size,
          },
          parser: (raw) => PageResult.fromJson(raw, OrderData.fromJson),
        ),
      );

  /// 修改会员昵称（仅本店生效）
  Future<Membership?> updateNickname({
    required int storeId,
    required int membershipId,
    String? nickname,
  }) =>
      _client.post<Membership?>(
        '/manage/membership/nickname',
        queryParameters: {
          'storeId': storeId,
          'membershipId': membershipId,
          'nickname': nickname,
        },
        parser: (raw) =>
            raw is Map<String, dynamic> ? Membership.fromJson(raw) : null,
      );

  /// 手动调整积分（正数增加，负数扣减）
  Future<Membership?> adjustPoints({
    required int storeId,
    required int membershipId,
    required int points,
    String? remark,
  }) async {
    final result = await _client.post<AdjustPointsResult>(
      '/manage/membership/adjust',
      queryParameters: {
        'storeId': storeId,
        'membershipId': membershipId,
        'points': points,
        'remark': remark,
      },
      parser: (raw) => raw is Map<String, dynamic>
          ? AdjustPointsResult.fromJson(raw)
          : const AdjustPointsResult(),
    );
    return result?.membership;
  }
}
