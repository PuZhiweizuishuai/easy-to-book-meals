// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PermissionData _$PermissionDataFromJson(Map<String, dynamic> json) =>
    PermissionData(
      id: json['id'] as int?,
      permissionCode: json['permissionCode'] as String?,
      permissionName: json['permissionName'] as String?,
      description: json['description'] as String?,
    );

Map<String, dynamic> _$PermissionDataToJson(PermissionData instance) =>
    <String, dynamic>{
      'id': instance.id,
      'permissionCode': instance.permissionCode,
      'permissionName': instance.permissionName,
      'description': instance.description,
    };

RoleData _$RoleDataFromJson(Map<String, dynamic> json) => RoleData(
      id: json['id'] as int?,
      roleCode: json['roleCode'] as String?,
      roleName: json['roleName'] as String?,
      storeId: json['storeId'] as int?,
      domain: json['domain'] as String?,
      permissionList: (json['permissionList'] as List<dynamic>?)
          ?.map((e) => PermissionData.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$RoleDataToJson(RoleData instance) => <String, dynamic>{
      'id': instance.id,
      'roleCode': instance.roleCode,
      'roleName': instance.roleName,
      'storeId': instance.storeId,
      'domain': instance.domain,
      'permissionList': instance.permissionList?.map((e) => e.toJson()).toList(),
    };

UserData _$UserDataFromJson(Map<String, dynamic> json) => UserData(
      id: json['id'] as int?,
      nickname: json['nickname'] as String?,
      avatarUrl: json['avatarUrl'] as String?,
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      status: json['status'] as int?,
      roleDataList: (json['roleDataList'] as List<dynamic>?)
          ?.map((e) => RoleData.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$UserDataToJson(UserData instance) => <String, dynamic>{
      'id': instance.id,
      'nickname': instance.nickname,
      'avatarUrl': instance.avatarUrl,
      'phone': instance.phone,
      'email': instance.email,
      'status': instance.status,
      'roleDataList': instance.roleDataList?.map((e) => e.toJson()).toList(),
    };

LoginResult _$LoginResultFromJson(Map<String, dynamic> json) => LoginResult(
      token: json['token'] as String?,
      user: json['user'] == null
          ? null
          : UserData.fromJson(json['user'] as Map<String, dynamic>),
    );

Map<String, dynamic> _$LoginResultToJson(LoginResult instance) =>
    <String, dynamic>{
      'token': instance.token,
      'user': instance.user?.toJson(),
    };
