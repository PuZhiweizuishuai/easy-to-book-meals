import 'package:json_annotation/json_annotation.dart';

part 'store_model.g.dart';

/// 门店概要信息
@JsonSerializable()
class StoreSummary {
  const StoreSummary({
    this.id,
    this.storeName,
    this.address,
    this.phone,
    this.logoUrl,
    this.businessHours,
    this.description,
    this.status,
  });

  factory StoreSummary.fromJson(Map<String, dynamic> json) =>
      _$StoreSummaryFromJson(json);

  final int? id;
  final String? storeName;
  final String? address;
  final String? phone;
  final String? logoUrl;
  final String? businessHours;
  final String? description;

  /// 门店状态：0-待审核 1-营业中 2-休息中 3-已停业
  final int? status;

  /// 展示名
  String get displayName =>
      (storeName == null || storeName!.isEmpty) ? '未命名门店' : storeName!;

  Map<String, dynamic> toJson() => _$StoreSummaryToJson(this);
}

/// 门店状态文案映射
extension StoreStatusText on StoreSummary {
  /// 状态名称
  String get statusName => storeStatusName(status);

  bool get isRunning => status == 1;
}

/// 门店状态码 → 文案
String storeStatusName(int? status) => switch (status) {
      -1 => '待审核',
      0 => '营业中',
      1 => '休息中',
      2 => '已停业',
      _ => '未知',
    };
