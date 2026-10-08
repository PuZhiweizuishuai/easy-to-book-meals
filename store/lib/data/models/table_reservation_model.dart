import 'package:flutter/material.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../core/theme/app_colors.dart';

part 'table_reservation_model.g.dart';

/// 餐桌预约（对应后端 TableReservation）
///
/// 字段口径与后端实体保持一致：
/// - 时间字段均为 13 位毫秒时间戳
/// - 预约状态见 [ReservationStatus]
/// - operator / userId 可为空（散客电话预约无关联用户）
@JsonSerializable()
class TableReservation {
  const TableReservation({
    this.id,
    this.storeId,
    this.operator,
    this.tableId,
    this.userId,
    this.contactName,
    this.contactPhone,
    this.guestCount,
    this.status,
    this.reservedStartTime,
    this.reservedEndTime,
    this.remark,
    this.createTime,
    this.updateTime,
  });

  factory TableReservation.fromJson(Map<String, dynamic> json) =>
      _$TableReservationFromJson(json);

  final int? id;
  final int? storeId;
  final int? operator;
  final int? tableId;
  final int? userId;

  /// 预订人姓名
  final String? contactName;

  /// 联系电话
  final String? contactPhone;

  /// 就餐人数
  final int? guestCount;

  /// 状态码，见 [ReservationStatus]
  final int? status;

  /// 预约开始时间（毫秒）
  final int? reservedStartTime;

  /// 预约结束时间（毫秒）
  final int? reservedEndTime;

  /// 备注 / 取消原因
  final String? remark;

  final int? createTime;
  final int? updateTime;

  /// 状态枚举
  ReservationStatus get statusEnum => ReservationStatus.fromCode(status);

  /// 是否仍处于可操作状态（未取消 / 未到店 / 未完成）
  bool get active => !statusEnum.inactive;

  Map<String, dynamic> toJson() => _$TableReservationToJson(this);
}

/// 预约状态（与后端 ReservationStatus 一致）
enum ReservationStatus {
  pending(0, '待确认'),
  confirmed(1, '已确认'),
  seated(2, '已到店'),
  cancelled(3, '已取消'),
  noShow(4, '未到店'),
  completed(5, '已完成');

  const ReservationStatus(this.code, this.label);

  /// 状态码
  final int code;

  /// 中文名
  final String label;

  static ReservationStatus fromCode(int? code) {
    for (final value in values) {
      if (value.code == code) return value;
    }
    return ReservationStatus.pending;
  }

  /// 终态：已取消 / 未到店 / 已完成（不再可操作）
  bool get inactive =>
      this == cancelled || this == noShow || this == completed;

  /// 主色
  Color get color => switch (this) {
        ReservationStatus.pending => AppColors.warning,
        ReservationStatus.confirmed => AppColors.success,
        ReservationStatus.seated => AppColors.info,
        ReservationStatus.cancelled => AppColors.disabled,
        ReservationStatus.noShow => AppColors.danger,
        ReservationStatus.completed => AppColors.primary,
      };

  /// 浅色背景
  Color get background => switch (this) {
        ReservationStatus.pending => AppColors.warningLight,
        ReservationStatus.confirmed => AppColors.successLight,
        ReservationStatus.seated => AppColors.infoLight,
        ReservationStatus.cancelled => const Color(0xFFF2F4F8),
        ReservationStatus.noShow => AppColors.dangerLight,
        ReservationStatus.completed => AppColors.primaryLight,
      };
}
