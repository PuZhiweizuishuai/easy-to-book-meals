import 'package:json_annotation/json_annotation.dart';

part 'user_model.g.dart';

/// 权限项
@JsonSerializable()
class PermissionData {
  const PermissionData({
    this.id,
    this.permissionCode,
    this.permissionName,
    this.description,
  });

  factory PermissionData.fromJson(Map<String, dynamic> json) =>
      _$PermissionDataFromJson(json);

  final int? id;
  final String? permissionCode;
  final String? permissionName;
  final String? description;

  Map<String, dynamic> toJson() => _$PermissionDataToJson(this);
}

/// 角色（含门店归属与权限列表）
@JsonSerializable(explicitToJson: true)
class RoleData {
  const RoleData({
    this.id,
    this.roleCode,
    this.roleName,
    this.storeId,
    this.domain,
    this.permissionList,
  });

  factory RoleData.fromJson(Map<String, dynamic> json) =>
      _$RoleDataFromJson(json);

  final int? id;
  final String? roleCode;
  final String? roleName;

  /// 门店级角色才会有门店 ID（超级管理员角色为空）
  final int? storeId;

  /// 角色域：system / store
  final String? domain;

  /// 该角色下的权限列表
  final List<PermissionData>? permissionList;

  Map<String, dynamic> toJson() => _$RoleDataToJson(this);
}

/// 登录用户信息
@JsonSerializable(explicitToJson: true)
class UserData {
  const UserData({
    this.id,
    this.nickname,
    this.avatarUrl,
    this.phone,
    this.email,
    this.status,
    this.roleDataList,
  });

  factory UserData.fromJson(Map<String, dynamic> json) =>
      _$UserDataFromJson(json);

  final int? id;
  final String? nickname;
  final String? avatarUrl;
  final String? phone;
  final String? email;

  /// 账号状态：0-禁用 1-启用
  final int? status;

  /// 角色列表（每个角色含自己的权限列表）
  final List<RoleData>? roleDataList;

  /// 展示用昵称（昵称缺失时退回手机号/邮箱）
  String get displayName {
    if (nickname != null && nickname!.trim().isNotEmpty) return nickname!;
    if (phone != null && phone!.isNotEmpty) return phone!;
    if (email != null && email!.isNotEmpty) return email!;
    return '未命名账号';
  }

  /// 是否为超级管理员
  bool get isSuperAdmin =>
      roleDataList?.any((r) => r.roleCode == _superAdminCode) ?? false;

  /// 所有权限编码（去重）
  List<String> get permissionCodes {
    final codes = <String>{};
    for (final role in roleDataList ?? const []) {
      for (final perm in role.permissionList ?? const []) {
        final code = perm.permissionCode;
        if (code != null && code.isNotEmpty) codes.add(code);
      }
    }
    return codes.toList(growable: false);
  }

  /// 是否拥有任一指定权限（超级管理员恒为 true）
  bool hasAnyPermission(List<String> codes) =>
      isSuperAdmin || permissionCodes.toSet().intersection(codes.toSet()).isNotEmpty;

  Map<String, dynamic> toJson() => _$UserDataToJson(this);
}

/// 登录接口返回体：`{ token, user }`
@JsonSerializable(explicitToJson: true)
class LoginResult {
  const LoginResult({this.token, this.user});

  factory LoginResult.fromJson(Map<String, dynamic> json) =>
      _$LoginResultFromJson(json);

  /// Sa-Token 令牌
  final String? token;

  /// 登录用户信息（含角色与权限）
  final UserData? user;

  Map<String, dynamic> toJson() => _$LoginResultToJson(this);
}

/// 超级管理员角色编码
const String _superAdminCode = 'SUPER_ADMIN';
