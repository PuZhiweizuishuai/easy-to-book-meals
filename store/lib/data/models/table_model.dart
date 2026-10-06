import 'package:json_annotation/json_annotation.dart';

part 'table_model.g.dart';

/// 餐桌（对应后端 DiningTable）
@JsonSerializable()
class DiningTable {
  const DiningTable({
    this.id,
    this.storefrontId,
    this.creatUser,
    this.tableNumber,
    this.capacity,
    this.status,
    this.area,
    this.qrCodeKey,
    this.currentOrderId,
    this.sortOrder,
    this.remark,
    this.deleted,
    this.createTime,
    this.updateTime,
  });

  factory DiningTable.fromJson(Map<String, dynamic> json) =>
      _$DiningTableFromJson(json);

  final int? id;
  final int? storefrontId;
  final int? creatUser;

  /// 餐桌号 / 桌名
  final String? tableNumber;

  /// 容纳人数
  final int? capacity;

  /// 状态：0-空闲 1-已占用 2-已预订 3-清理中 4-停用
  final int? status;

  /// 区域（大厅 / 包间 / 露台）
  final String? area;

  /// 扫码点餐二维码标识
  final String? qrCodeKey;

  /// 当前占用订单 ID
  final int? currentOrderId;

  /// 排序权重
  final int? sortOrder;

  /// 备注
  final String? remark;

  /// 逻辑删除标记
  final int? deleted;

  final int? createTime;
  final int? updateTime;

  /// 展示区名
  String get areaText => (area == null || area!.isEmpty) ? '默认区域' : area!;

  /// 是否处于被占用状态
  bool get busy => status == 1;

  Map<String, dynamic> toJson() => _$DiningTableToJson(this);
}

/// 餐桌状态（与后端 TableStatus 一致）
enum TableStatus {
  free(0, '空闲'),
  occupied(1, '已占用'),
  reserved(2, '已预订'),
  cleaning(3, '清理中'),
  disabled(4, '停用');

  const TableStatus(this.code, this.label);

  /// 状态码
  final int code;

  /// 中文名
  final String label;

  static TableStatus fromCode(int? code) {
    for (final value in values) {
      if (value.code == code) return value;
    }
    return TableStatus.free;
  }
}

/// 餐桌状态辅助扩展
extension DiningTableStatusExt on DiningTable {
  TableStatus get statusEnum => TableStatus.fromCode(status);
}
