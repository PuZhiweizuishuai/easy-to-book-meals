// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

StoreSummary _$StoreSummaryFromJson(Map<String, dynamic> json) => StoreSummary(
      id: json['id'] as int?,
      storeName: json['storeName'] as String?,
      address: json['address'] as String?,
      phone: json['phone'] as String?,
      logoUrl: json['logoUrl'] as String?,
      businessHours: json['businessHours'] as String?,
      description: json['description'] as String?,
      status: json['status'] as int?,
    );

Map<String, dynamic> _$StoreSummaryToJson(StoreSummary instance) =>
    <String, dynamic>{
      'id': instance.id,
      'storeName': instance.storeName,
      'address': instance.address,
      'phone': instance.phone,
      'logoUrl': instance.logoUrl,
      'businessHours': instance.businessHours,
      'description': instance.description,
      'status': instance.status,
    };
