import 'package:json_annotation/json_annotation.dart';

part 'membership_model.g.dart';

/// 门店会员（对应后端 Membership）
///
/// 会员以「门店 + 手机号」为唯一维度：同一手机号在不同门店各有独立记录，
/// 昵称也是门店级的展示名。
@JsonSerializable()
class Membership {
  const Membership({
    this.id,
    this.storeId,
    this.userId,
    this.nickname,
    this.phone,
    this.remarks,
    this.num,
    this.count,
    this.createTime,
    this.updateTime,
  });

  factory Membership.fromJson(Map<String, dynamic> json) =>
      _$MembershipFromJson(json);

  final int? id;
  final int? storeId;
  final int? userId;

  /// 门店级昵称
  final String? nickname;

  /// 手机号
  final String? phone;

  /// 备注
  final String? remarks;

  /// 积分
  final int? num;

  /// 用餐次数
  final int? count;

  final int? createTime;
  final int? updateTime;

  /// 展示昵称（无昵称时脱敏手机号）
  String get displayName {
    if (nickname != null && nickname!.trim().isNotEmpty) return nickname!;
    return maskedPhone;
  }

  /// 脱敏手机号：138****8888
  String get maskedPhone {
    final value = phone;
    if (value == null || value.length < 7) return value ?? '未知手机号';
    return '${value.substring(0, 3)}****${value.substring(value.length - 4)}';
  }

  int get points => num ?? 0;
  int get orderCount => count ?? 0;

  Map<String, dynamic> toJson() => _$MembershipToJson(this);
}

/// 积分流水
@JsonSerializable()
class PointsTransaction {
  const PointsTransaction({
    this.id,
    this.storeId,
    this.membershipId,
    this.phone,
    this.orderId,
    this.orderNo,
    this.points,
    this.type,
    this.remark,
    this.operatorId,
    this.createTime,
  });

  factory PointsTransaction.fromJson(Map<String, dynamic> json) =>
      _$PointsTransactionFromJson(json);

  final int? id;
  final int? storeId;
  final int? membershipId;
  final String? phone;

  /// 关联订单 ID
  final int? orderId;

  /// 关联订单号
  final String? orderNo;

  /// 变动积分（正数为增加，负数为扣减）
  final int? points;

  /// 变动类型
  final String? type;

  /// 备注
  final String? remark;

  /// 操作人 ID
  final int? operatorId;

  final int? createTime;

  bool get isIncrease => (points ?? 0) >= 0;

  Map<String, dynamic> toJson() => _$PointsTransactionToJson(this);
}

/// 积分调整结果（`{ membership, message }`）
@JsonSerializable(explicitToJson: true)
class AdjustPointsResult {
  const AdjustPointsResult({this.membership, this.message});

  factory AdjustPointsResult.fromJson(Map<String, dynamic> json) =>
      _$AdjustPointsResultFromJson(json);

  final Membership? membership;
  final String? message;

  Map<String, dynamic> toJson() => _$AdjustPointsResultToJson(this);
}
