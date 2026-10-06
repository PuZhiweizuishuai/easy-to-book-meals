import 'package:json_annotation/json_annotation.dart';

part 'category_model.g.dart';

/// 菜品分类（对应后端 FoodTag）
///
/// 分类是全局共享数据，查询公开、增删改需要店主权限。
@JsonSerializable()
class CategorySummary {
  const CategorySummary({this.id, this.name, this.count, this.sort});

  factory CategorySummary.fromJson(Map<String, dynamic> json) =>
      _$CategorySummaryFromJson(json);

  final int? id;
  final String? name;

  /// 分类下的菜品数量
  final int? count;

  /// 排序权重（小的在前）
  final int? sort;

  /// 展示名
  String get displayName =>
      (name == null || name!.isEmpty) ? '未命名分类' : name!;

  CategorySummary copyWith({int? id, String? name, int? count, int? sort}) =>
      CategorySummary(
        id: id ?? this.id,
        name: name ?? this.name,
        count: count ?? this.count,
        sort: sort ?? this.sort,
      );

  Map<String, dynamic> toJson() => _$CategorySummaryToJson(this);
}
