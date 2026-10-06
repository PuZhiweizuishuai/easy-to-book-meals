// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'table_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DiningTable _$DiningTableFromJson(Map<String, dynamic> json) => DiningTable(
      id: json['id'] as int?,
      storefrontId: json['storefrontId'] as int?,
      creatUser: json['creatUser'] as int?,
      tableNumber: json['tableNumber'] as String?,
      capacity: json['capacity'] as int?,
      status: json['status'] as int?,
      area: json['area'] as String?,
      qrCodeKey: json['qrCodeKey'] as String?,
      currentOrderId: json['currentOrderId'] as int?,
      sortOrder: json['sortOrder'] as int?,
      remark: json['remark'] as String?,
      deleted: json['deleted'] as int?,
      createTime: json['createTime'] as int?,
      updateTime: json['updateTime'] as int?,
    );

Map<String, dynamic> _$DiningTableToJson(DiningTable instance) =>
    <String, dynamic>{
      'id': instance.id,
      'storefrontId': instance.storefrontId,
      'creatUser': instance.creatUser,
      'tableNumber': instance.tableNumber,
      'capacity': instance.capacity,
      'status': instance.status,
      'area': instance.area,
      'qrCodeKey': instance.qrCodeKey,
      'currentOrderId': instance.currentOrderId,
      'sortOrder': instance.sortOrder,
      'remark': instance.remark,
      'deleted': instance.deleted,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
