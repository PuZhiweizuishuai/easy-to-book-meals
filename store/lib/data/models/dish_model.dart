import 'dart:convert';

import 'package:json_annotation/json_annotation.dart';

import 'category_model.dart';
import 'file_model.dart';
import 'sku_model.dart';

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

  /// 详情页图片地址列表
  ///
  /// 后端 BaseFood.imageList 存的是 JSON 字符串（"[\"a.png\",\"b.png\"]"），
  /// 这里统一解析成地址数组；解析失败时返回空列表。
  List<String> get imageUrls {
    final raw = imageList?.trim();
    if (raw == null || raw.isEmpty) return const [];
    try {
      final decoded = jsonDecode(raw);
      if (decoded is List) {
        return decoded
            .map((item) => item.toString())
            .where((item) => item.isNotEmpty)
            .toList(growable: false);
      }
    } catch (_) {
      // 历史数据可能不是合法 JSON，忽略即可
    }
    return const [];
  }

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

/// 规格值输入（对应后端 SpecValue）
///
/// [price] 目前后端未落库（Web 端表单保留该字段），仅做兼容传递。
class SpecValueInput {
  const SpecValueInput({
    this.id,
    this.groupId,
    this.name = '',
    this.sort = 0,
    this.price = 0,
  });

  factory SpecValueInput.fromJson(Map<String, dynamic> json) =>
      SpecValueInput(
        id: _asInt(json['id']),
        groupId: _asInt(json['groupId']),
        name: json['name']?.toString() ?? '',
        sort: _asInt(json['sort']) ?? 0,
        price: (json['price'] as num?)?.toDouble() ?? 0,
      );

  final int? id;
  final int? groupId;
  final String name;
  final int sort;
  final double price;

  SpecValueInput copyWith({String? name, int? sort, double? price}) =>
      SpecValueInput(
        id: id,
        groupId: groupId,
        name: name ?? this.name,
        sort: sort ?? this.sort,
        price: price ?? this.price,
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        if (id != null) 'id': id,
        if (groupId != null) 'groupId': groupId,
        'name': name,
        'sort': sort,
        'price': price,
      };

  static int? _asInt(Object? value) =>
      value is num ? value.toInt() : int.tryParse(value?.toString() ?? '');
}

/// 规格组输入（对应后端 SpecGroup）
///
/// 一个菜品可配置多组规格（如「辣度」「份量」），每组下含若干选项。
class SpecGroupInput {
  const SpecGroupInput({
    this.id,
    this.dishId,
    this.name = '',
    this.sort = 0,
    this.values = const [],
  });

  factory SpecGroupInput.fromJson(Map<String, dynamic> json) =>
      SpecGroupInput(
        id: _asInt(json['id']),
        dishId: _asInt(json['dishId']),
        name: json['name']?.toString() ?? '',
        sort: _asInt(json['sort']) ?? 0,
        values: (json['values'] is List)
            ? (json['values'] as List)
                .whereType<Map<String, dynamic>>()
                .map(SpecValueInput.fromJson)
                .toList(growable: true)
            : const [],
      );

  final int? id;
  final int? dishId;
  final String name;
  final int sort;
  final List<SpecValueInput> values;

  /// 有效填写（组名非空且至少一个选项有名称）——空组不提交给后端
  bool get isValid =>
      name.trim().isNotEmpty &&
      values.any((value) => value.name.trim().isNotEmpty);

  /// 过滤掉空选项后的副本
  SpecGroupInput get cleaned => SpecGroupInput(
        id: id,
        dishId: dishId,
        name: name.trim(),
        sort: sort,
        values: [
          for (var i = 0; i < values.length; i++)
            if (values[i].name.trim().isNotEmpty)
              values[i].copyWith(sort: i + 1),
        ],
      );

  SpecGroupInput copyWith({
    String? name,
    int? sort,
    List<SpecValueInput>? values,
  }) =>
      SpecGroupInput(
        id: id,
        dishId: dishId,
        name: name ?? this.name,
        sort: sort ?? this.sort,
        values: values ?? this.values,
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        if (id != null) 'id': id,
        if (dishId != null) 'dishId': dishId,
        'name': name,
        'sort': sort,
        'values': values.map((value) => value.toJson()).toList(),
      };

  static int? _asInt(Object? value) =>
      value is num ? value.toInt() : int.tryParse(value?.toString() ?? '');
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
    this.specGroups,
    this.fileDataList,
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

  /// 规格组（新增菜品时一并创建，后端据此生成 SKU 组合）
  final List<SpecGroupInput>? specGroups;

  /// 已上传的文件对象，后端据此把图片与菜品关联
  final List<UploadedFile>? fileDataList;

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
        // 只提交填写完整的规格组，空组会让后端生成异常 SKU
        'specGroups': (specGroups ?? const [])
            .where((group) => group.isValid)
            .map((group) => group.cleaned.toJson())
            .toList(),
        'fileDataList':
            (fileDataList ?? const []).map((file) => file.toJson()).toList(),
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
    this.imageList,
    this.basePrice,
    this.priceType = 0,
    this.categoryIds,
    this.stock,
    required this.status,
    this.fileDataList,
    this.skuList,
  });

  final int id;
  final int storefrontId;
  final String name;
  final String? description;
  final String? topImage;

  /// 详情页图片地址（后端 BaseFood.imageList 存的是 JSON 字符串）
  final List<String>? imageList;

  final double? basePrice;
  final int priceType;
  final List<CategorySummary>? categoryIds;
  final int? stock;
  final int status;

  /// 本次新增上传的文件，后端据此与菜品关联
  final List<UploadedFile>? fileDataList;

  /// SKU 售卖属性（价格 / 库存 / 折扣 / 上下架），为空表示不改动
  final List<SkuInput>? skuList;

  Map<String, dynamic> toJson() => {
        'food': {
          'id': id,
          'storefrontId': storefrontId,
          'name': name,
          'description': description ?? '',
          'topImage': topImage ?? '',
          // 未改图片时也回传，避免后端把已有图片列表清空
          'imageList': jsonEncode(imageList ?? const []),
          'basePrice': basePrice ?? 0,
          'priceType': priceType,
          'stock': stock ?? -1,
          'status': status,
          'categoryIds': (categoryIds ?? const [])
              .map((e) => {'id': e.id, 'name': e.name})
              .toList(),
        },
        // SKU 售卖属性（价格 / 库存 / 折扣 / 上下架），后端按 id 逐个更新
        'skuList': (skuList ?? const []).map((sku) => sku.toJson()).toList(),
        // 规格不走 /dishes/update（后端明确忽略），编辑时由 /sku/adjust 单独提交
        'specGroups': const [],
        'fileDataList':
            (fileDataList ?? const []).map((file) => file.toJson()).toList(),
      };
}
