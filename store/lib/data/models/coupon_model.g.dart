// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'coupon_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CouponCheckResult _$CouponCheckResultFromJson(Map<String, dynamic> json) =>
    CouponCheckResult(
      userCouponId: json['userCouponId'] as int?,
      couponId: json['couponId'] as int?,
      code: json['code'] as String?,
      name: json['name'] as String?,
      type: json['type'] as int?,
      typeName: json['typeName'] as String?,
      thresholdAmount: (json['thresholdAmount'] as num?)?.toDouble(),
      discountAmount: (json['discountAmount'] as num?)?.toDouble(),
      payableAmount: (json['payableAmount'] as num?)?.toDouble(),
      description: json['description'] as String?,
      claimed: (json['claimed'] as bool?) ?? false,
      phone: json['phone'] as String?,
    );

Map<String, dynamic> _$CouponCheckResultToJson(CouponCheckResult instance) =>
    <String, dynamic>{
      'userCouponId': instance.userCouponId,
      'couponId': instance.couponId,
      'code': instance.code,
      'name': instance.name,
      'type': instance.type,
      'typeName': instance.typeName,
      'thresholdAmount': instance.thresholdAmount,
      'discountAmount': instance.discountAmount,
      'payableAmount': instance.payableAmount,
      'description': instance.description,
      'claimed': instance.claimed,
      'phone': instance.phone,
    };

UserCoupon _$UserCouponFromJson(Map<String, dynamic> json) => UserCoupon(
      id: json['id'] as int?,
      storeId: json['storeId'] as int?,
      couponId: json['couponId'] as int?,
      code: json['code'] as String?,
      userId: json['userId'] as int?,
      phone: json['phone'] as String?,
      status: json['status'] as int?,
      claimTime: json['claimTime'] as int?,
      usedTime: json['usedTime'] as int?,
      orderId: json['orderId'] as int?,
      orderNo: json['orderNo'] as String?,
      createTime: json['createTime'] as int?,
      updateTime: json['updateTime'] as int?,
    );

Map<String, dynamic> _$UserCouponToJson(UserCoupon instance) =>
    <String, dynamic>{
      'id': instance.id,
      'storeId': instance.storeId,
      'couponId': instance.couponId,
      'code': instance.code,
      'userId': instance.userId,
      'phone': instance.phone,
      'status': instance.status,
      'claimTime': instance.claimTime,
      'usedTime': instance.usedTime,
      'orderId': instance.orderId,
      'orderNo': instance.orderNo,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
