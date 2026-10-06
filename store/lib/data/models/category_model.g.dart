// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'category_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CategorySummary _$CategorySummaryFromJson(Map<String, dynamic> json) =>
    CategorySummary(
      id: json['id'] as int?,
      name: json['name'] as String?,
      count: json['count'] as int?,
      sort: json['sort'] as int?,
    );

Map<String, dynamic> _$CategorySummaryToJson(CategorySummary instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'count': instance.count,
      'sort': instance.sort,
    };
