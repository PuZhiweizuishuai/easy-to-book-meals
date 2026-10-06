import 'package:json_annotation/json_annotation.dart';

import 'category_model.dart';

part 'dish_model.g.dart';

/// 菜品（对应后端 BaseFood）
@JsonSerializable(explicitToJson: true)
class Dish {
  const Dish({
    this.id,
    this.name,
    this.createUser,
    this.storefrontId,
    this.description,
    this.topImage,
    this.imageList,
    this.priceType,
    this.basePrice,
    this.categoryIds,
    this.monthlySales,
    this.allSales,
    this.stock,
    this.status,
    this.createTime,
    this.updateTime,
  });

  factory Dish.fromJson(Map<String, dynamic> json) => _$DishFromJson(json);

  final int? id;
  final String? name;
  final int? createUser;
  final int? storefrontId;

  /// 菜品描述（后端为 longtext，可能为空字符串）
  final String? description;

  /// 封面图 URL
  final String? topImage;

  /// 其它图片 JSON 字符串（"["url1","url2"]"）
  final String? imageList;

  /// 计价方式：0-份 1-克 2-斤
  final int? priceType;

  /// 基础价格
  final double? basePrice;

  /// 关联分类
  final List<CategorySummary>? categoryIds;

  /// 月销量
  final int? monthlySales;

  /// 总销量
  final int? allSales;

  /// 库存：-1 表示不限制
  final int? stock;

  /// 状态：0-下架 1-上架
  final int? status;

  final int? createTime;
  final int? updateTime;

  // ==================== 派生属性 ====================

  bool get onSale => status == 1;

  /// 计价单位文案
  String get unitText => PriceTypeMeta.name(priceType ?? 0);

  /// 库存文案
  String get stockText {
    final value = stock;
    if (value == null || value < 0) return '不限';
    return '$value';
  }

  /// 分类名拼接，如「热菜 / 招牌」
  String get categoryText =>
      (categoryIds ?? const [])
          .map((e) => e.name ?? '')
          .where((e) => e.isNotEmpty)
          .join(' · ');

  /// 描述（过滤掉空白）
  String? get descriptionText {
    final text = description?.trim();
    return (text == null || text.isEmpty) ? null : text;
  }

  Map<String, dynamic> toJson() => _$DishToJson(this);
}

/// 菜品计价方式
class PriceTypeMeta {
  const PriceTypeMeta._();

  /// 全部计价方式
  static const List<int> codes = [0, 1, 2];

  /// 单位名称
  static String name(int code) => switch (code) {
        1 => '克',
        2 => '斤',
        _ => '份',
      };
}

/// 新建菜品请求体（对应后端 CreateDishes）
class CreateDishPayload {
  CreateDishPayload({
    required this.storefrontId,
    required this.name,
    this.description,
    this.topImage,
    this.imageList,
    this.unitPrice,
    this.priceType = 0,
    this.categoryIds,
    this.stock,
    this.status = 1,
  });

  final int storefrontId;
  final String name;
  final String? description;
  final String? topImage;
  final List<String>? imageList;
  final double? unitPrice;
  final int priceType;
  final List<CategorySummary>? categoryIds;
  final int? stock;
  final int status;

  Map<String, dynamic> toJson() => {
        'storefrontId': storefrontId,
        'name': name,
        'description': description ?? '',
        'topImage': topImage ?? '',
        if (imageList != null) 'imageList': imageList,
        'unitPrice': unitPrice ?? 0,
        'priceType': priceType,
        'categoryIds': (categoryIds ?? const [])
            .map((e) => {'id': e.id, 'name': e.name})
            .toList(),
        'stock': stock ?? -1,
        'status': status,
      };
}

/// 更新菜品请求体（对应后端 CreateDishesResult）
class UpdateDishPayload {
  UpdateDishPayload({
    required this.id,
    required this.storefrontId,
    required this.name,
    this.description,
    this.topImage,
    this.basePrice,
    this.priceType = 0,
    this.categoryIds,
    this.stock,
    required this.status,
  });

  final int id;
  final int storefrontId;
  final String name;
  final String? description;
  final String? topImage;
  final double? basePrice;
  final int priceType;
  final List<CategorySummary>? categoryIds;
  final int? stock;
  final int status;

  Map<String, dynamic> toJson() => {
        'food': {
          'id': id,
          'storefrontId': storefrontId,
          'name': name,
          'description': description ?? '',
          'topImage': topImage ?? '',
          'basePrice': basePrice ?? 0,
          'priceType': priceType,
          'stock': stock ?? -1,
          'status': status,
          'categoryIds': (categoryIds ?? const [])
              .map((e) => {'id': e.id, 'name': e.name})
              .toList(),
        },
        'skuList': const [],
        'specGroups': const [],
        'fileDataList': const [],
      };
}
