import '../../core/network/api_client.dart';
import '../../core/network/api_defaults.dart';
import '../../core/network/api_response.dart';
import '../models/models.dart';

/// 认证相关接口
class AuthApi {
  const AuthApi(this._client);

  final ApiClient _client;

  /// 登录：手机号或邮箱 + 密码
  Future<LoginResult> login({
    required String username,
    required String password,
  }) async {
    final result = await _client.post<LoginResult>(
      '/auth/login',
      body: {'username': username, 'password': password},
      parser: (raw) => LoginResult.fromJson(asMap(raw) ?? const {}),
    );
    return result ?? const LoginResult();
  }

  /// 退出登录
  Future<void> logout() async {
    await _client.post<void>(
      '/auth/logout',
      parser: (raw) => raw,
    );
  }

  /// 获取当前登录用户信息（含角色与权限）
  Future<UserData?> info() => _client.get<UserData?>(
        '/auth/info',
        parser: (raw) => raw is Map<String, dynamic> ? UserData.fromJson(raw) : null,
      );

  /// 与当前用户相关的门店列表（门店角色 ∪ 自己创建的门店，含待审核）
  Future<PageResult<StoreSummary>> myStores({int page = 1, int size = 100}) =>
      orEmptyPage(
        _client.get<PageResult<StoreSummary>>(
          '/store/manage/list',
          queryParameters: {'page': page, 'size': size},
          parser: (raw) => PageResult.fromJson(raw, StoreSummary.fromJson),
        ),
      );

  /// 未读通知数量
  Future<int> unreadNotificationCount() => orDefault(
        _client.get<int>(
          '/notification/unread-count',
          parser: (raw) => raw is num ? raw.toInt() : 0,
        ),
        0,
      );
}
