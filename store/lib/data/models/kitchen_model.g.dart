// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'kitchen_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

KitchenQueueItem _$KitchenQueueItemFromJson(Map<String, dynamic> json) =>
    KitchenQueueItem(
      itemId: json['itemId'] as int?,
      dishName: json['dishName'] as String?,
      specNames: json['specNames'] as String?,
      quantity: (json['quantity'] as num?)?.toDouble(),
      topImage: json['topImage'] as String?,
      addedLater: json['addedLater'] as int?,
      itemCreateTime: json['itemCreateTime'] as int?,
      waitedMinutes: json['waitedMinutes'] as int?,
      preparingBy: json['preparingBy'] as int?,
      preparingTime: json['preparingTime'] as int?,
      isMine: json['isMine'] as bool?,
      orderId: json['orderId'] as int?,
      orderNo: json['orderNo'] as String?,
      orderType: json['orderType'] as int?,
      tableNumber: json['tableNumber'] as String?,
      remark: json['remark'] as String?,
      contactPhone: json['contactPhone'] as String?,
      status: json['status'] as int?,
    );

Map<String, dynamic> _$KitchenQueueItemToJson(KitchenQueueItem instance) =>
    <String, dynamic>{
      'itemId': instance.itemId,
      'dishName': instance.dishName,
      'specNames': instance.specNames,
      'quantity': instance.quantity,
      'topImage': instance.topImage,
      'addedLater': instance.addedLater,
      'itemCreateTime': instance.itemCreateTime,
      'waitedMinutes': instance.waitedMinutes,
      'preparingBy': instance.preparingBy,
      'preparingTime': instance.preparingTime,
      'isMine': instance.isMine,
      'orderId': instance.orderId,
      'orderNo': instance.orderNo,
      'orderType': instance.orderType,
      'tableNumber': instance.tableNumber,
      'remark': instance.remark,
      'contactPhone': instance.contactPhone,
      'status': instance.status,
    };
