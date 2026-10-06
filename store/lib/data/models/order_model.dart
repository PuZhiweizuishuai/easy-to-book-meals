import 'package:json_annotation/json_annotation.dart';

import '../../core/utils/money_format.dart';

part 'order_model.g.dart';

/// 订单状态（与后端 OrderStatus 对应）
enum OrderStatus {
  pending(0, '待接单'),
  confirmed(1, '已接单'),
  cooking(2, '制作中'),
  ready(3, '已出餐'),
  completed(4, '已完成'),
  cancelled(5, '已取消');

  const OrderStatus(this.code, this.label);

  /// 状态码
  final int code;

  /// 中文名
  final String label;

  static OrderStatus? fromCode(int? code) {
    if (code == null) return null;
    for (final value in values) {
      if (value.code == code) return value;
    }
    return null;
  }

  /// 是否属于「还在进行中」的订单（需店员处理）
  bool get isActive =>
      this == OrderStatus.pending ||
      this == OrderStatus.confirmed ||
      this == OrderStatus.cooking ||
      this == OrderStatus.ready;
}

/// 订单类型（与后端 OrderType 对应）
enum OrderType {
  dineIn(0, '堂食'),
  takeout(1, '外送'),
  online(2, '线上');

  const OrderType(this.code, this.label);

  final int code;
  final String label;

  static OrderType fromCode(int? code) {
    for (final value in values) {
      if (value.code == code) return value;
    }
    return OrderType.dineIn;
  }
}

/// 支付状态
enum PayStatus {
  unpaid(0, '未支付'),
  paid(1, '已支付');

  const PayStatus(this.code, this.label);

  final int code;
  final String label;

  static PayStatus fromCode(int? code) {
    for (final value in values) {
      if (value.code == code) return value;
    }
    return PayStatus.unpaid;
  }
}

/// 订单明细条目
@JsonSerializable()
class OrderItem {
  const OrderItem({
    this.id,
    this.orderId,
    this.dishId,
    this.skuId,
    this.dishName,
    this.topImage,
    this.specNames,
    this.quantity,
    this.unitPrice,
    this.dealPrice,
    this.totalPrice,
    this.status,
    this.served,
    this.addedLater,
    this.preparingBy,
    this.preparingTime,
    this.createTime,
  });

  factory OrderItem.fromJson(Map<String, dynamic> json) =>
      _$OrderItemFromJson(json);

  final int? id;
  final int? orderId;
  final int? dishId;
  final int? skuId;

  /// 菜品名称快照
  final String? dishName;

  /// 菜品封面快照
  final String? topImage;

  /// 规格名快照，如「大份 / 微辣」
  final String? specNames;

  /// 数量（称重菜为小数）
  final double? quantity;

  /// SKU 原价快照
  final double? unitPrice;

  /// 成交单价快照
  final double? dealPrice;

  /// 小计金额
  final double? totalPrice;

  /// 明细状态：0-正常 1-已取消
  final int? status;

  /// 是否已出餐：0-未 1-已
  final int? served;

  /// 是否加菜追加：0-初始 1-加菜
  final int? addedLater;

  /// 开始制作的操作人 ID
  final int? preparingBy;

  final int? preparingTime;
  final int? createTime;

  // ==================== 派生属性 ====================

  bool get isServed => served == 1;
  bool get isValid => status == 0;
  bool get isAddedLater => addedLater == 1;
  bool get isPreparing => preparingBy != null;

  double get count => quantity ?? 0;
  double get amount => totalPrice ?? 0;

  String get quantityText => MoneyFormat.quantity(count);

  Map<String, dynamic> toJson() => _$OrderItemToJson(this);
}

/// 订单主体
@JsonSerializable(explicitToJson: true)
class OrderData {
  const OrderData({
    this.id,
    this.orderNo,
    this.storeId,
    this.userId,
    this.userNickname,
    this.tableId,
    this.tableNumber,
    this.orderType,
    this.status,
    this.totalAmount,
    this.discountAmount,
    this.couponAmount,
    this.manualDiscountAmount,
    this.couponCode,
    this.payAmount,
    this.payStatus,
    this.confirmTime,
    this.completeTime,
    this.contactPhone,
    this.remark,
    this.cancelReason,
    this.createTime,
    this.updateTime,
    this.items,
  });

  factory OrderData.fromJson(Map<String, dynamic> json) =>
      _$OrderDataFromJson(json);

  final int? id;
  final String? orderNo;
  final int? storeId;
  final int? userId;
  final String? userNickname;

  /// 餐桌 ID
  final int? tableId;

  /// 餐桌号快照
  final String? tableNumber;

  /// 订单类型码
  final int? orderType;

  /// 订单状态码
  final int? status;

  /// 商品原价合计
  final double? totalAmount;

  /// 菜品/SKU 级折扣合计
  final double? discountAmount;

  /// 优惠券减免
  final double? couponAmount;

  /// 手动减免（抹零/议价）
  final double? manualDiscountAmount;

  /// 使用的券码
  final String? couponCode;

  /// 应付金额
  final double? payAmount;

  /// 支付状态码
  final int? payStatus;

  final int? confirmTime;
  final int? completeTime;

  /// 结算联系电话
  final String? contactPhone;

  /// 订单备注
  final String? remark;

  /// 取消原因
  final String? cancelReason;

  final int? createTime;
  final int? updateTime;

  /// 订单明细（查询详情时填充）
  final List<OrderItem>? items;

  // ==================== 派生属性 ====================

  OrderStatus? get statusEnum => OrderStatus.fromCode(status);
  OrderType get typeEnum => OrderType.fromCode(orderType);
  PayStatus get payStatusEnum => PayStatus.fromCode(payStatus);

  /// 菜品折后合计（未扣优惠券与手动减免）—— 优惠券试算的基准金额
  double get basePayable =>
      (totalAmount ?? 0) - (discountAmount ?? 0);

  double get manualDiscount => manualDiscountAmount ?? 0;
  double get couponDiscount => couponAmount ?? 0;

  /// 已出餐 / 正常菜品总数
  int get totalItemCount => validItems.length;
  int get servedItemCount => validItems.where((i) => i.isServed).length;

  /// 正常明细（排除已退菜）
  List<OrderItem> get validItems =>
      (items ?? const []).where((i) => i.isValid).toList();

  Map<String, dynamic> toJson() => _$OrderDataToJson(this);
}

/// 门店订单实时统计（/api/order/manage/stats）
@JsonSerializable()
class StoreStats {
  const StoreStats({
    this.todayOrderCount,
    this.todayRevenue,
    this.pendingCount,
    this.cookingCount,
  });

  factory StoreStats.fromJson(Map<String, dynamic> json) =>
      _$StoreStatsFromJson(json);

  final int? todayOrderCount;
  final double? todayRevenue;

  /// 待接单数
  final int? pendingCount;

  /// 制作中（已接单 + 制作中）
  final int? cookingCount;

  Map<String, dynamic> toJson() => _$StoreStatsToJson(this);
}
