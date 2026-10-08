import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/network/api_exception.dart';
import '../core/services/app_logger.dart';
import '../core/services/preferences_service.dart';
import '../data/api/auth_api.dart';
import '../data/models/models.dart';
import 'core_providers.dart';

/// 会话状态
class SessionState {
  const SessionState({
    required this.status,
    this.user,
    this.token,
    this.errorMessage,
    this.expired = false,
  });

  /// 初始态（正在读取本地缓存）
  factory SessionState.initial() => const SessionState(status: SessionStatus.checking);

  /// 未配置服务器地址
  factory SessionState.unconfigured() =>
      const SessionState(status: SessionStatus.unconfigured);

  /// 已配置服务器但未登录
  factory SessionState.loggedOut() =>
      const SessionState(status: SessionStatus.loggedOut);

  /// 登录中
  factory SessionState.authenticating() =>
      const SessionState(status: SessionStatus.authenticating);

  /// 已登录
  factory SessionState.authenticated({
    required String token,
    UserData? user,
  }) =>
      SessionState(
        status: SessionStatus.authenticated,
        token: token,
        user: user,
      );

  /// 令牌已失效（过期 / 被服务端踢下线），需要重新登录
  factory SessionState.expired() => const SessionState(
        status: SessionStatus.loggedOut,
        errorMessage: '登录已过期，请重新登录',
        expired: true,
      );

  factory SessionState.failure(String message) => SessionState(
        status: SessionStatus.loggedOut,
        errorMessage: message,
      );

  final SessionStatus status;
  final UserData? user;
  final String? token;
  final String? errorMessage;

  /// 登录态失效导致的未登录：登录页据此提示「已过期」
  final bool expired;

  bool get isChecking => status == SessionStatus.checking;
  bool get isAuthenticated => status == SessionStatus.authenticated;
  bool get isLoggedOut => status == SessionStatus.loggedOut;
  bool get isUnconfigured => status == SessionStatus.unconfigured;
  bool get isBusy => status == SessionStatus.authenticating;

  SessionState copyWith({
    SessionStatus? status,
    UserData? user,
    String? token,
    String? errorMessage,
  }) =>
      SessionState(
        status: status ?? this.status,
        user: user ?? this.user,
        token: token ?? this.token,
        errorMessage: errorMessage,
      );
}

/// 会话阶段
enum SessionStatus {
  /// 正在读取本地缓存判断状态
  checking,

  /// 未配置服务器地址（首次启动）
  unconfigured,

  /// 已配置服务器，待登录
  loggedOut,

  /// 登录请求进行中
  authenticating,

  /// 已登录
  authenticated,
}

/// 会话控制器
///
/// 负责：启动时恢复登录态、登录、登出、权限判断。
class SessionController extends Notifier<SessionState> {
  late AuthApi _authApi;

  @override
  SessionState build() {
    _authApi = ref.read(authApiProvider);
    // 启动时先恢复本地缓存的登录态
    Future<void>.delayed(Duration.zero, restore);
    return SessionState.initial();
  }

  PreferencesService get _prefs => ref.read(preferencesProvider);

  /// 读取本地缓存恢复会话
  ///
  /// 缓存里的令牌可能已在服务端过期（Sa-Token 超时），所以先用缓存乐观进入
  /// 首页，再立即向后端校验一次：校验失败会走 [handleUnauthorized] 跳回登录页，
  /// 而网络不可用时保留本地会话，避免离线时被误登出。
  Future<void> restore() async {
    final prefs = _prefs;
    if (!prefs.hasServerAddress) {
      state = SessionState.unconfigured();
      return;
    }
    final cachedUser = prefs.userMap;
    if (!prefs.hasToken || cachedUser == null) {
      state = SessionState.loggedOut();
      return;
    }
    state = SessionState.authenticated(
      token: prefs.token,
      user: UserData.fromJson(cachedUser),
    );
    await validateSession();
  }

  /// 登录
  Future<bool> login({
    required String username,
    required String password,
  }) async {
    state = SessionState.authenticating();
    try {
      final result = await _authApi.login(
        username: username.trim(),
        password: password,
      );
      final token = result.token;
      if (token == null || token.isEmpty) {
        state = SessionState.failure('登录失败：未获取到令牌');
        return false;
      }
      await _persist(token, result.user);
      state = SessionState.authenticated(token: token, user: result.user);
      return true;
    } on ApiException catch (e) {
      state = SessionState.failure(e.message);
      return false;
    } catch (e, s) {
      AppLogger.e('登录异常', error: e, stackTrace: s);
      state = SessionState.failure('登录失败，请稍后重试');
      return false;
    }
  }

  /// 校验令牌是否仍然有效，并顺带刷新角色/权限缓存
  Future<void> validateSession() async {
    if (!state.isAuthenticated) return;
    try {
      final user = await _authApi.info();
      if (user != null && state.isAuthenticated) {
        await _prefs.setUserMap(user.toJson());
        state = state.copyWith(user: user);
      }
    } on ApiException catch (e) {
      // 401/403 已由网络层回调统一登出，这里只需保留离线场景的缓存
      if (!e.isUnauthorized) {
        AppLogger.w('校验登录态失败，保留本地会话', error: e);
      }
    } catch (e) {
      AppLogger.w('校验登录态失败，保留本地会话', error: e);
    }
  }

  /// 登出
  Future<void> logout() async {
    try {
      await _authApi.logout();
    } catch (_) {
      // 后端登出失败也要清理本地状态
    }
    await _prefs.clearSession();
    state = SessionState.loggedOut();
  }

  /// 登录态失效（401/403）时由网络层回调与业务页调用
  ///
  /// 幂等：并发请求同时收到 401 时只有第一次会真正清态。
  Future<void> handleUnauthorized() async {
    if (!state.isAuthenticated) return;
    await _prefs.clearSession();
    state = SessionState.expired();
  }

  /// 服务器地址配置完成后调用
  void markConfigured() {
    if (state.isUnconfigured || state.isChecking) {
      state = SessionState.loggedOut();
    }
  }

  /// 回到「未配置服务器」引导（用户主动重置地址时调用）
  Future<void> markUnconfigured() async {
    await _prefs.clearSession();
    state = SessionState.unconfigured();
  }

  Future<void> _persist(String token, UserData? user) async {
    await _prefs.setToken(token);
    if (user != null) {
      await _prefs.setUserMap(user.toJson());
    }
  }
}

/// 会话状态
final sessionProvider =
    NotifierProvider<SessionController, SessionState>(SessionController.new);

/// 当前登录用户
final currentUserProvider = Provider<UserData?>(
  (ref) => ref.watch(sessionProvider).user,
);

/// 是否拥有任一指定权限（超级管理员恒为 true）
bool hasPermission(WidgetRef ref, List<String> codes) {
  final user = ref.read(currentUserProvider);
  if (user == null) return false;
  return user.hasAnyPermission(codes);
}
