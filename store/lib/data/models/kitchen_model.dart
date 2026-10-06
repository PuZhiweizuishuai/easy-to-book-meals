import 'package:json_annotation/json_annotation.dart';

part 'kitchen_model.g.dart';

/// 后厨待制作队列中的一条「菜品」
///
/// 后厨 KDS 以菜品为粒度，每条记录带所属订单信息（订单号、桌号、备注），
/// 便于后厨联系前厅。
@JsonSerializable()
class KitchenQueueItem {
  const KitchenQueueItem({
    this.itemId,
    this.dishName,
    this.specNames,
    this.quantity,
    this.topImage,
    this.addedLater,
    this.itemCreateTime,
    this.waitedMinutes,
    this.preparingBy,
    this.preparingTime,
    this.isMine,
    this.orderId,
    this.orderNo,
    this.orderType,
    this.tableNumber,
    this.remark,
    this.contactPhone,
    this.status,
  });

  factory KitchenQueueItem.fromJson(Map<String, dynamic> json) =>
      _$KitchenQueueItemFromJson(json);

  /// 订单明细 ID（出餐/认领都以它为参数）
  final int? itemId;
  final String? dishName;

  /// 规格名
  final String? specNames;
  final double? quantity;
  final String? topImage;

  /// 是否加菜追加
  final int? addedLater;

  final int? itemCreateTime;

  /// 已等待分钟数
  final int? waitedMinutes;

  /// 认领人 ID
  final int? preparingBy;
  final int? preparingTime;

  /// 是否为当前登录用户认领
  final bool? isMine;

  final int? orderId;
  final String? orderNo;

  /// 订单类型码
  final int? orderType;

  /// 桌号快照
  final String? tableNumber;
  final String? remark;
  final String? contactPhone;

  /// 所属订单状态码
  final int? status;

  // ==================== 派生属性 ====================

  bool get myPreparing => isMine == true || preparingBy != null;
  bool get added => addedLater == 1;

  double get count => quantity ?? 0;

  /// 等待等级：0 正常(<15min) 1 提醒(15~30) 2 紧急(>=30)
  int get urgencyLevel {
    final minutes = waitedMinutes ?? 0;
    if (minutes >= 30) return 2;
    if (minutes >= 15) return 1;
    return 0;
  }

  Map<String, dynamic> toJson() => _$KitchenQueueItemToJson(this);
}

/// 后厨队列接口返回值
class KitchenQueueResult {
  const KitchenQueueResult({
    required this.items,
    required this.itemCount,
    required this.maxWaitedMinutes,
  });

  factory KitchenQueueResult.fromJson(
    Map<String, dynamic> json, {
    List<KitchenQueueItem>? items,
  }) {
    final rawItems = json['items'];
    final parsed = rawItems is List
        ? rawItems
            .whereType<Map<String, dynamic>>()
            .map(KitchenQueueItem.fromJson)
            .toList(growable: false)
        : items ?? const [];
    return KitchenQueueResult(
      items: parsed,
      itemCount: json['itemCount'] is num
          ? (json['itemCount'] as num).toInt()
          : parsed.length,
      maxWaitedMinutes: json['maxWaitedMinutes'] is num
          ? (json['maxWaitedMinutes'] as num).toInt()
          : 0,
    );
  }

  /// 菜品列表（按等待时长升序，我认领的在前）
  final List<KitchenQueueItem> items;

  /// 菜品总数
  final int itemCount;

  /// 最长等待分钟数
  final int maxWaitedMinutes;
}
