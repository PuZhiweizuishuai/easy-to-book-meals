// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'report_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DailyOverview _$DailyOverviewFromJson(Map<String, dynamic> json) =>
    DailyOverview(
      revenue: (json['revenue'] as num?)?.toDouble(),
      orderCount: json['orderCount'] as int?,
      completedCount: json['completedCount'] as int?,
      cancelledCount: json['cancelledCount'] as int?,
      memberOrderCount: json['memberOrderCount'] as int?,
      discountAmount: (json['discountAmount'] as num?)?.toDouble(),
      avgOrderAmount: (json['avgOrderAmount'] as num?)?.toDouble(),
      date: json['date'] as String?,
      itemCount: (json['itemCount'] as num?)?.toDouble(),
      totalRevenue: (json['totalRevenue'] as num?)?.toDouble(),
      totalOrderCount: json['totalOrderCount'] as int?,
    );

Map<String, dynamic> _$DailyOverviewToJson(DailyOverview instance) =>
    <String, dynamic>{
      'revenue': instance.revenue,
      'orderCount': instance.orderCount,
      'completedCount': instance.completedCount,
      'cancelledCount': instance.cancelledCount,
      'memberOrderCount': instance.memberOrderCount,
      'discountAmount': instance.discountAmount,
      'avgOrderAmount': instance.avgOrderAmount,
      'date': instance.date,
      'itemCount': instance.itemCount,
      'totalRevenue': instance.totalRevenue,
      'totalOrderCount': instance.totalOrderCount,
    };

TrendPoint _$TrendPointFromJson(Map<String, dynamic> json) => TrendPoint(
      label: json['label'] as String?,
      date: json['date'] as String?,
      revenue: (json['revenue'] as num?)?.toDouble(),
      discount: (json['discount'] as num?)?.toDouble(),
      orderCount: json['orderCount'] as int?,
      completedCount: json['completedCount'] as int?,
      cancelledCount: json['cancelledCount'] as int?,
      avgOrderAmount: (json['avgOrderAmount'] as num?)?.toDouble(),
    );

Map<String, dynamic> _$TrendPointToJson(TrendPoint instance) =>
    <String, dynamic>{
      'label': instance.label,
      'date': instance.date,
      'revenue': instance.revenue,
      'discount': instance.discount,
      'orderCount': instance.orderCount,
      'completedCount': instance.completedCount,
      'cancelledCount': instance.cancelledCount,
      'avgOrderAmount': instance.avgOrderAmount,
    };

TopDish _$TopDishFromJson(Map<String, dynamic> json) => TopDish(
      dishId: json['dishId'] as int?,
      dishName: json['dishName'] as String?,
      topImage: json['topImage'] as String?,
      quantity: (json['quantity'] as num?)?.toDouble(),
      revenue: (json['revenue'] as num?)?.toDouble(),
    );

Map<String, dynamic> _$TopDishToJson(TopDish instance) => <String, dynamic>{
      'dishId': instance.dishId,
      'dishName': instance.dishName,
      'topImage': instance.topImage,
      'quantity': instance.quantity,
      'revenue': instance.revenue,
    };
