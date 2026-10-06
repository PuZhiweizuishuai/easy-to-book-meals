import 'package:json_annotation/json_annotation.dart';

part 'coupon_model.g.dart';

/// 优惠券校验 / 试算结果（对应后端 CouponCheckResult）
///
/// 由 `/api/coupon/check` 返回：仅试算优惠金额，不会真正核销券。
@JsonSerializable()
class CouponCheckResult {
  const CouponCheckResult({
    this.userCouponId,
    this.couponId,
    this.code,
    this.name,
    this.type,
    this.typeName,
    this.thresholdAmount,
    this.discountAmount,
    this.payableAmount,
    this.description,
    this.claimed = false,
    this.phone,
  });

  factory CouponCheckResult.fromJson(Map<String, dynamic> json) =>
      _$CouponCheckResultFromJson(json);

  /// 券码 ID
  final int? userCouponId;

  /// 券模板 ID
  final int? couponId;

  /// 券码
  final String? code;

  /// 券名称
  final String? name;

  /// 券类型：1-满减 2-折扣 3-无门槛
  final int? type;

  final String? typeName;

  /// 使用门槛
  final double? thresholdAmount;

  /// 本单可优惠金额
  final double? discountAmount;

  /// 优惠后应付金额
  final double? payableAmount;

  /// 券说明
  final String? description;

  /// 是否已领取（关联用户）
  @JsonKey(defaultValue: false)
  final bool claimed;

  /// 关联手机号
  final String? phone;

  double get discount => discountAmount ?? 0;
  double get payable => payableAmount ?? 0;

  Map<String, dynamic> toJson() => _$CouponCheckResultToJson(this);
}

/// 券类型枚举（与后端 CouponType 一致）
enum CouponTypeCode {
  fullReduce(1, '满减券'),
  discount(2, '折扣券'),
  noThreshold(3, '无门槛券');

  const CouponTypeCode(this.code, this.label);

  final int code;
  final String label;

  static CouponTypeCode fromCode(int? code) {
    for (final value in values) {
      if (value.code == code) return value;
    }
    return CouponTypeCode.fullReduce;
  }
}

/// 券码实体（对应后端 UserCoupon）
@JsonSerializable()
class UserCoupon {
  const UserCoupon({
    this.id,
    this.storeId,
    this.couponId,
    this.code,
    this.userId,
    this.phone,
    this.status,
    this.claimTime,
    this.usedTime,
    this.orderId,
    this.orderNo,
    this.createTime,
    this.updateTime,
  });

  factory UserCoupon.fromJson(Map<String, dynamic> json) =>
      _$UserCouponFromJson(json);

  final int? id;
  final int? storeId;
  final int? couponId;

  /// 32 位无横线 UUID
  final String? code;
  final int? userId;
  final String? phone;

  /// 0-未使用 1-已使用 2-已作废
  final int? status;

  final int? claimTime;
  final int? usedTime;
  final int? orderId;
  final String? orderNo;
  final int? createTime;
  final int? updateTime;

  String get statusName => switch (status) {
        1 => '已使用',
        2 => '已作废',
        _ => '未使用',
      };

  bool get usable => status == 0;

  Map<String, dynamic> toJson() => _$UserCouponToJson(this);
}
