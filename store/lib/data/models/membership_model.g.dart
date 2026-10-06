// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'membership_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Membership _$MembershipFromJson(Map<String, dynamic> json) => Membership(
      id: json['id'] as int?,
      storeId: json['storeId'] as int?,
      userId: json['userId'] as int?,
      nickname: json['nickname'] as String?,
      phone: json['phone'] as String?,
      remarks: json['remarks'] as String?,
      num: json['num'] as int?,
      count: json['count'] as int?,
      createTime: json['createTime'] as int?,
      updateTime: json['updateTime'] as int?,
    );

Map<String, dynamic> _$MembershipToJson(Membership instance) =>
    <String, dynamic>{
      'id': instance.id,
      'storeId': instance.storeId,
      'userId': instance.userId,
      'nickname': instance.nickname,
      'phone': instance.phone,
      'remarks': instance.remarks,
      'num': instance.num,
      'count': instance.count,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };

PointsTransaction _$PointsTransactionFromJson(Map<String, dynamic> json) =>
    PointsTransaction(
      id: json['id'] as int?,
      storeId: json['storeId'] as int?,
      membershipId: json['membershipId'] as int?,
      phone: json['phone'] as String?,
      orderId: json['orderId'] as int?,
      orderNo: json['orderNo'] as String?,
      points: json['points'] as int?,
      type: json['type'] as String?,
      remark: json['remark'] as String?,
      operatorId: json['operatorId'] as int?,
      createTime: json['createTime'] as int?,
    );

Map<String, dynamic> _$PointsTransactionToJson(PointsTransaction instance) =>
    <String, dynamic>{
      'id': instance.id,
      'storeId': instance.storeId,
      'membershipId': instance.membershipId,
      'phone': instance.phone,
      'orderId': instance.orderId,
      'orderNo': instance.orderNo,
      'points': instance.points,
      'type': instance.type,
      'remark': instance.remark,
      'operatorId': instance.operatorId,
      'createTime': instance.createTime,
    };

AdjustPointsResult _$AdjustPointsResultFromJson(Map<String, dynamic> json) =>
    AdjustPointsResult(
      membership: json['membership'] == null
          ? null
          : Membership.fromJson(json['membership'] as Map<String, dynamic>),
      message: json['message'] as String?,
    );

Map<String, dynamic> _$AdjustPointsResultToJson(AdjustPointsResult instance) =>
    <String, dynamic>{
      'membership': instance.membership?.toJson(),
      'message': instance.message,
    };
