// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'order_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OrderItem _$OrderItemFromJson(Map<String, dynamic> json) => OrderItem(
      id: json['id'] as int?,
      orderId: json['orderId'] as int?,
      dishId: json['dishId'] as int?,
      skuId: json['skuId'] as int?,
      dishName: json['dishName'] as String?,
      topImage: json['topImage'] as String?,
      specNames: json['specNames'] as String?,
      quantity: (json['quantity'] as num?)?.toDouble(),
      unitPrice: (json['unitPrice'] as num?)?.toDouble(),
      dealPrice: (json['dealPrice'] as num?)?.toDouble(),
      totalPrice: (json['totalPrice'] as num?)?.toDouble(),
      status: json['status'] as int?,
      served: json['served'] as int?,
      addedLater: json['addedLater'] as int?,
      preparingBy: json['preparingBy'] as int?,
      preparingTime: json['preparingTime'] as int?,
      createTime: json['createTime'] as int?,
    );

Map<String, dynamic> _$OrderItemToJson(OrderItem instance) => <String, dynamic>{
      'id': instance.id,
      'orderId': instance.orderId,
      'dishId': instance.dishId,
      'skuId': instance.skuId,
      'dishName': instance.dishName,
      'topImage': instance.topImage,
      'specNames': instance.specNames,
      'quantity': instance.quantity,
      'unitPrice': instance.unitPrice,
      'dealPrice': instance.dealPrice,
      'totalPrice': instance.totalPrice,
      'status': instance.status,
      'served': instance.served,
      'addedLater': instance.addedLater,
      'preparingBy': instance.preparingBy,
      'preparingTime': instance.preparingTime,
      'createTime': instance.createTime,
    };

OrderData _$OrderDataFromJson(Map<String, dynamic> json) => OrderData(
      id: json['id'] as int?,
      orderNo: json['orderNo'] as String?,
      storeId: json['storeId'] as int?,
      userId: json['userId'] as int?,
      userNickname: json['userNickname'] as String?,
      tableId: json['tableId'] as int?,
      tableNumber: json['tableNumber'] as String?,
      orderType: json['orderType'] as int?,
      status: json['status'] as int?,
      totalAmount: (json['totalAmount'] as num?)?.toDouble(),
      discountAmount: (json['discountAmount'] as num?)?.toDouble(),
      couponAmount: (json['couponAmount'] as num?)?.toDouble(),
      manualDiscountAmount:
          (json['manualDiscountAmount'] as num?)?.toDouble(),
      couponCode: json['couponCode'] as String?,
      payAmount: (json['payAmount'] as num?)?.toDouble(),
      payStatus: json['payStatus'] as int?,
      confirmTime: json['confirmTime'] as int?,
      completeTime: json['completeTime'] as int?,
      contactPhone: json['contactPhone'] as String?,
      deliveryAddress: json['deliveryAddress'] as String?,
      remark: json['remark'] as String?,
      cancelReason: json['cancelReason'] as String?,
      createTime: json['createTime'] as int?,
      updateTime: json['updateTime'] as int?,
      items: (json['items'] as List<dynamic>?)
          ?.map((e) => OrderItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$OrderDataToJson(OrderData instance) => <String, dynamic>{
      'id': instance.id,
      'orderNo': instance.orderNo,
      'storeId': instance.storeId,
      'userId': instance.userId,
      'userNickname': instance.userNickname,
      'tableId': instance.tableId,
      'tableNumber': instance.tableNumber,
      'orderType': instance.orderType,
      'status': instance.status,
      'totalAmount': instance.totalAmount,
      'discountAmount': instance.discountAmount,
      'couponAmount': instance.couponAmount,
      'manualDiscountAmount': instance.manualDiscountAmount,
      'couponCode': instance.couponCode,
      'payAmount': instance.payAmount,
      'payStatus': instance.payStatus,
      'confirmTime': instance.confirmTime,
      'completeTime': instance.completeTime,
      'contactPhone': instance.contactPhone,
      'deliveryAddress': instance.deliveryAddress,
      'remark': instance.remark,
      'cancelReason': instance.cancelReason,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
      'items': instance.items?.map((e) => e.toJson()).toList(),
    };

StoreStats _$StoreStatsFromJson(Map<String, dynamic> json) => StoreStats(
      todayOrderCount: json['todayOrderCount'] as int?,
      todayRevenue: (json['todayRevenue'] as num?)?.toDouble(),
      pendingCount: json['pendingCount'] as int?,
      cookingCount: json['cookingCount'] as int?,
    );

Map<String, dynamic> _$StoreStatsToJson(StoreStats instance) =>
    <String, dynamic>{
      'todayOrderCount': instance.todayOrderCount,
      'todayRevenue': instance.todayRevenue,
      'pendingCount': instance.pendingCount,
      'cookingCount': instance.cookingCount,
    };
