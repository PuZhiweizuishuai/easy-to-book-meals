import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:store/core/network/api_client.dart';
import 'package:store/core/network/api_exception.dart';
import 'package:store/core/services/preferences_service.dart';
import 'package:store/providers/core_providers.dart';
import 'package:store/providers/session_provider.dart';

/// 会话恢复与登录态失效
///
/// 测试环境的 HttpClient 会被 flutter_test 拦截并统一返回 400，拿不到后端真实的
/// 业务码，所以这里给 [ApiClient] 注入桩适配器，直接回放 ResponseDetails。
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SharedPreferences prefs;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    prefs = await SharedPreferences.getInstance();
  });

  void cacheSession() {
    prefs.setString('server_address', 'http://192.168.1.10:8081');
    prefs.setString('token', 'cached-token');
    prefs.setString('user_info', jsonEncode({'id': 7, 'nickname': '店主'}));
  }

  /// build() 里的 restore 是异步排队的，轮询等到目标状态出现再断言
  Future<SessionState> settle(
    ProviderContainer container, {
    bool Function(SessionState)? until,
    int maxMs = 800,
  }) async {
    for (var waited = 0; waited < maxMs; waited += 10) {
      await Future<void>.delayed(const Duration(milliseconds: 10));
      final session = container.read(sessionProvider);
      if (until?.call(session) ?? false) return session;
    }
    return container.read(sessionProvider);
  }

  /// 建容器：先拿到装配好 onUnauthorized 回调的 ApiClient，再替换其网络层
  Future<(ProviderContainer, SessionState)> startupWithStub(
    int businessStatus, {
    bool Function(SessionState)? until,
  }) async {
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    container.read(apiClientProvider).dio.httpClientAdapter =
        _StubAdapter(businessStatus);
    container.read(sessionProvider);
    return (container, await settle(container, until: until));
  }

  test('未配置服务器地址时进入配置引导', () async {
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    container.read(sessionProvider);
    final session = await settle(container, until: (s) => !s.isChecking);
    expect(session.isUnconfigured, isTrue);
  });

  test('配置了地址但没有本地令牌时直接进入登录页', () async {
    prefs.setString('server_address', 'http://192.168.1.10:8081');
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    container.read(sessionProvider);
    final session = await settle(container, until: (s) => !s.isChecking);
    expect(session.isLoggedOut, isTrue);
    expect(session.expired, isFalse);
  });

  test('缓存令牌被后端判为无权限时清掉登录态并标记过期', () async {
    cacheSession();
    final (container, session) = await startupWithStub(
      403,
      until: (s) => s.isLoggedOut,
    );

    expect(session.expired, isTrue);
    expect(session.errorMessage, '登录已过期，请重新登录');
    expect(prefs.getString('token'), isNull);
    expect(prefs.getString('user_info'), isNull);
  });

  test('后端异常不是登录问题时保留本地会话，避免误登出', () async {
    cacheSession();
    final (container, session) = await startupWithStub(500);

    expect(session.isAuthenticated, isTrue);
    expect(session.expired, isFalse);
    expect(prefs.getString('token'), 'cached-token');
  });

  test('会话已失效后重复的 401 不再改写状态', () async {
    cacheSession();
    final (container, _) = await startupWithStub(
      403,
      until: (s) => s.isLoggedOut,
    );

    final notifier = container.read(sessionProvider.notifier);
    await Future.wait([
      notifier.handleUnauthorized(),
      notifier.handleUnauthorized(),
    ]);

    final session = container.read(sessionProvider);
    expect(session.isLoggedOut, isTrue);
    expect(session.expired, isTrue);
  });

  test('ApiClient 对 401/403 触发失效回调，其余错误不打扰会话', () async {
    final client = ApiClient(
      PreferencesService(prefs),
      baseUrlOverride: 'http://127.0.0.1:8081',
    );
    addTearDown(client.close);

    var fired = 0;
    client.onUnauthorized = () => fired++;

    client.dio.httpClientAdapter = _StubAdapter(401);
    ApiException? unauthorized;
    try {
      await client.get<Object?>('/auth/info', parser: (raw) => raw);
    } on ApiException catch (e) {
      unauthorized = e;
    }
    expect(unauthorized?.isUnauthorized, isTrue);
    expect(fired, 1);

    client.dio.httpClientAdapter = _StubAdapter(500);
    ApiException? serverError;
    try {
      await client.get<Object?>('/order/list', parser: (raw) => raw);
    } on ApiException catch (e) {
      serverError = e;
    }
    expect(serverError?.isUnauthorized, isFalse);
    expect(fired, 1);
  });
}

/// 固定回放后端 ResponseDetails 的桩适配器（HTTP 始终 200，业务码由参数决定）
class _StubAdapter implements HttpClientAdapter {
  _StubAdapter(this.businessStatus);

  final int businessStatus;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    return ResponseBody.fromString(
      jsonEncode({
        'status': businessStatus,
        'message': businessStatus == 403 ? '没有权限' : '服务异常',
        'data': null,
      }),
      200,
      headers: {
        HttpHeaders.contentTypeHeader: [ContentType.json.toString()],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
