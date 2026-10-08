import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:store/app.dart';
import 'package:store/core/routing/app_navigator.dart';
import 'package:store/providers/alert_provider.dart';
import 'package:store/providers/core_providers.dart';
import 'package:store/providers/session_provider.dart';

/// 登录过期后的跳转
///
/// 覆盖两个易漏点：
/// 1. 缓存令牌被后端判为过期时，RootPage 必须切到登录页并给出原因提示；
/// 2. 用户此时停在已 push 的二级页面，出栈才能看到登录页。
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SharedPreferences prefs;
  late _StubAdapter adapter;

  setUp(() async {
    SharedPreferences.setMockInitialValues({
      'server_address': 'http://192.168.1.10:8081',
      'token': 'cached-token',
      'user_info': jsonEncode({'id': 7, 'nickname': '店主'}),
    });
    prefs = await SharedPreferences.getInstance();
    adapter = _StubAdapter();

    // SoundService 会构造真实播放器，测试环境没有 audioplayers 平台实现
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(
      const MethodChannel('xyz.luan/audioplayers.global'),
      (call) async => call.method == 'create' ? 'test-player' : null,
    );
    messenger.setMockMethodCallHandler(
      const MethodChannel('xyz.luan/audioplayers'),
      (call) async => null,
    );
  });

  Future<ProviderContainer> pumpApp(WidgetTester tester) async {
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    container.read(apiClientProvider).dio.httpClientAdapter = adapter;
    await tester.pumpWidget(
      UncontrolledProviderScope(container: container, child: const StoreApp()),
    );
    // splash（checking）→ 缓存会话渲染 HomeShell → 后台校验令牌
    for (var i = 0; i < 20 && !adapter.validated; i++) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    return container;
  }

  testWidgets('本地令牌已被后端判定过期：启动后停在登录页并提示重新登录',
      (tester) async {
    adapter.authExpired = true;
    await pumpApp(tester);
    await tester.pump(const Duration(milliseconds: 200));

    expect(find.text('登录已过期，请重新登录'), findsOneWidget);
    expect(find.text('手机号 / 邮箱'), findsOneWidget);
  });

  testWidgets('在二级页面遇到登录过期：回到根路由并展示登录页', (tester) async {
    final container = await pumpApp(tester);
    expect(container.read(sessionProvider).isAuthenticated, isTrue);

    AppNavigator.push(const _DetailStubPage());
    for (var i = 0; i < 5; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }
    expect(find.text('二级页面'), findsOneWidget);

    // 网络层命中 403 后由会话控制器清态
    adapter.authExpired = true;
    await container.read(sessionProvider.notifier).handleUnauthorized();
    for (var i = 0; i < 8; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }

    expect(find.text('二级页面'), findsNothing);
    expect(find.text('登录已过期，请重新登录'), findsOneWidget);

    // 首页挂载时启动的 15s 轮询定时器会在测试结束时被判为泄漏，这里主动关掉
    container.read(alertProvider.notifier).stop();
  });
}

/// 只用于验证出栈的占位二级页面
class _DetailStubPage extends StatelessWidget {
  const _DetailStubPage();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: Text('二级页面')));
  }
}

/// 回放后端 ResponseDetails 的桩适配器
///
/// 默认让 /auth/info 校验通过（保持登录态），[authExpired] 置 true 后
/// 按后端 Sa-Token 异常的行为返回业务码 403。
class _StubAdapter implements HttpClientAdapter {
  bool authExpired = false;

  /// 是否已经发起过 /auth/info 校验
  bool validated = false;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final path = options.path;
    if (path.endsWith('/auth/info')) {
      validated = true;
    }
    final businessStatus = authExpired && path.endsWith('/auth/info') ? 403 : 200;
    final data = switch (path) {
      _ when path.endsWith('/auth/info') => {'id': 7, 'nickname': '店主'},
      _ when path.endsWith('/store/manage/list') => {
          'content': [
            {'id': 1, 'storeName': '测试门店', 'status': 0},
          ],
          'page': {'number': 0, 'totalPages': 1, 'totalElements': 1},
        },
      _ => null,
    };
    return ResponseBody.fromString(
      jsonEncode({
        'status': businessStatus,
        'message': businessStatus == 403 ? '没有权限' : '成功',
        'data': data,
      }),
      200,
      headers: {'content-type': ['application/json']},
    );
  }

  @override
  void close({bool force = false}) {}
}
