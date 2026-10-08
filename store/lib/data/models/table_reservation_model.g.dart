// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'table_reservation_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TableReservation _$TableReservationFromJson(Map<String, dynamic> json) =>
    TableReservation(
      id: json['id'] as int?,
      storeId: json['storeId'] as int?,
      operator: json['operator'] as int?,
      tableId: json['tableId'] as int?,
      userId: json['userId'] as int?,
      contactName: json['contactName'] as String?,
      contactPhone: json['contactPhone'] as String?,
      guestCount: json['guestCount'] as int?,
      status: json['status'] as int?,
      reservedStartTime: json['reservedStartTime'] as int?,
      reservedEndTime: json['reservedEndTime'] as int?,
      remark: json['remark'] as String?,
      createTime: json['createTime'] as int?,
      updateTime: json['updateTime'] as int?,
    );

Map<String, dynamic> _$TableReservationToJson(TableReservation instance) =>
    <String, dynamic>{
      'id': instance.id,
      'storeId': instance.storeId,
      'operator': instance.operator,
      'tableId': instance.tableId,
      'userId': instance.userId,
      'contactName': instance.contactName,
      'contactPhone': instance.contactPhone,
      'guestCount': instance.guestCount,
      'status': instance.status,
      'reservedStartTime': instance.reservedStartTime,
      'reservedEndTime': instance.reservedEndTime,
      'remark': instance.remark,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
