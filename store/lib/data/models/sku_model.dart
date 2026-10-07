import 'dart:convert';

/// 商品 SKU（对应后端 ProductSku）
///
/// 一个菜品按规格组合笛卡尔积生成若干 SKU，每个 SKU 单独设置价格 / 库存 / 折扣 / 上下架。
class ProductSku {
  const ProductSku({
    this.id,
    this.dishId,
    this.storefrontId,
    this.description,
    this.unitPrice,
    this.unit,
    this.stock,
    this.discount,
    this.discountEndTime,
    this.sales,
    this.specs,
    this.specsHash,
    this.specNames,
    this.status,
  });

  factory ProductSku.fromJson(Map<String, dynamic> json) => ProductSku(
        id: _asInt(json['id']),
        dishId: _asInt(json['dishId']),
        storefrontId: _asInt(json['storefrontId']),
        description: json['description']?.toString(),
        unitPrice: (json['unitPrice'] as num?)?.toDouble(),
        unit: json['unit']?.toString(),
        stock: _asInt(json['stock']),
        discount: (json['discount'] as num?)?.toDouble(),
        discountEndTime: _asInt(json['discountEndTime']),
        sales: _asInt(json['sales']),
        specs: json['specs']?.toString(),
        specsHash: json['specsHash']?.toString(),
        specNames: json['specNames']?.toString(),
        status: _asInt(json['status']),
      );

  final int? id;
  final int? dishId;
  final int? storefrontId;

  /// SKU 描述 / 推荐语
  final String? description;

  /// 单价
  final double? unitPrice;

  /// 单位（份 / 斤 / 克）
  final String? unit;

  /// 库存，-1 表示不限
  final int? stock;

  /// 折扣（0~1，如 0.8 表示八折）
  final double? discount;

  /// 优惠截止时间，0 表示永久
  final int? discountEndTime;

  /// 销量
  final int? sales;

  /// 规格值 ID 的 JSON 数组，如 "[1,5]"
  final String? specs;

  final String? specsHash;

  /// 规格值名称缓存
  final String? specNames;

  /// 状态：1-上架 0-下架 2-已失效（规格变更后被锁定，只读）
  final int? status;

  /// 规格值 ID 列表
  List<int> get specIds => _parseIds(specs);

  /// 成交价：0 < discount < 1 时按折扣计算，其余取单价
  double get dealPrice {
    final price = unitPrice ?? 0;
    final d = discount ?? 0;
    return (d > 0 && d < 1) ? price * d : price;
  }

  /// 是否处于有效折扣期（仅 0~1 之间的折扣视为有效，与 Web 端 hasDiscount 一致）
  bool get hasValidDiscount {
    final d = discount ?? 0;
    return d > 0 && d < 1;
  }

  /// 规格名拼接：specNames 为规格值名称的 JSON 数组字符串
  /// （如 `["大份","微辣"]` → `大份/微辣`），解析失败时回退原始文本。
  String get specNamesText {
    final raw = specNames?.trim();
    if (raw == null || raw.isEmpty) return '';
    try {
      final decoded = jsonDecode(raw);
      if (decoded is List) {
        return decoded
            .map((item) => item.toString())
            .where((item) => item.isNotEmpty)
            .join('/');
      }
    } catch (_) {
      // 历史数据可能不是合法 JSON，回退为原始文本
    }
    return raw;
  }

  /// 是否已被锁定（规格组合失效，后端不允许再修改）
  bool get locked => status == SkuStatus.invalid;

  bool get onSale => status == SkuStatus.on;

  /// 转为可编辑的输入项
  SkuInput toInput() => SkuInput(
        id: id,
        description: description ?? '',
        unitPrice: unitPrice ?? 0,
        unit: unit,
        stock: stock ?? -1,
        discount: discount ?? 0,
        discountEndTime: discountEndTime ?? 0,
        status: status ?? SkuStatus.on,
        specIds: specIds,
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        if (id != null) 'id': id,
        if (dishId != null) 'dishId': dishId,
        if (storefrontId != null) 'storefrontId': storefrontId,
        'description': description ?? '',
        'unitPrice': unitPrice ?? 0,
        if (unit != null) 'unit': unit,
        'stock': stock ?? -1,
        'discount': discount ?? 0,
        'discountEndTime': discountEndTime ?? 0,
        'specs': specs ?? '[]',
        if (specsHash != null) 'specsHash': specsHash,
        'specNames': specNames ?? '[]',
        'status': status ?? SkuStatus.on,
      };

  static List<int> _parseIds(String? raw) {
    if (raw == null || raw.trim().isEmpty) return const [];
    try {
      final decoded = jsonDecode(raw);
      if (decoded is List) {
        return decoded.map((item) => (item as num).toInt()).toList();
      }
    } catch (_) {
      // 历史数据可能不是合法 JSON
    }
    return const [];
  }

  static int? _asInt(Object? value) {
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}

/// SKU 状态常量（与后端 WebConstant / SKU 状态约定一致）
class SkuStatus {
  const SkuStatus._();

  /// 下架
  static const int off = 0;

  /// 上架
  static const int on = 1;

  /// 已失效（规格变更导致旧组合被锁定，只读）
  static const int invalid = 2;

  /// 状态文案
  static String name(int? status) => switch (status) {
        SkuStatus.on => '上架',
        SkuStatus.invalid => '已失效',
        _ => '下架',
      };
}

/// SKU 编辑输入项（提交给 /dishes/update 的 skuList）
class SkuInput {
  const SkuInput({
    required this.id,
    this.description = '',
    this.unitPrice = 0,
    this.unit,
    this.stock = -1,
    this.discount = 0,
    this.discountEndTime = 0,
    this.status = SkuStatus.on,
    this.specIds = const [],
  });

  /// SKU ID（新增场景为 null，编辑场景必填）
  final int? id;
  final String description;
  final double unitPrice;
  final String? unit;
  final int stock;
  final double discount;

  /// 0 表示永久优惠
  final int discountEndTime;
  final int status;

  /// 规格值 ID（只用于前端筛选展示，后端按 id 更新）
  final List<int> specIds;

  SkuInput copyWith({
    String? description,
    double? unitPrice,
    int? stock,
    double? discount,
    int? discountEndTime,
    int? status,
  }) =>
      SkuInput(
        id: id,
        description: description ?? this.description,
        unitPrice: unitPrice ?? this.unitPrice,
        unit: unit,
        stock: stock ?? this.stock,
        discount: discount ?? this.discount,
        discountEndTime: discountEndTime ?? this.discountEndTime,
        status: status ?? this.status,
        specIds: specIds,
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        if (id != null) 'id': id,
        'description': description,
        'unitPrice': unitPrice,
        if (unit != null) 'unit': unit,
        'stock': stock,
        'discount': discount,
        'discountEndTime': discountEndTime,
        'status': status,
      };
}
