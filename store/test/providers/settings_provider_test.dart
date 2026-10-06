import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:store/core/config/app_config.dart';
import 'package:store/providers/core_providers.dart';
import 'package:store/providers/settings_provider.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SharedPreferences prefs;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    prefs = await SharedPreferences.getInstance();

    // SoundService 会在切换声音开关时构造真实 AudioPlayer，
    // 单元测试环境没有 audioplayers 平台实现，这里打桩避免异步 MissingPluginException
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

  ProviderContainer buildContainer() {
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    return container;
  }

  test('默认配置：轮询 15 秒、声音与通知开启', () {
    final settings = buildContainer().read(appSettingsProvider);
    expect(settings.pollSeconds, AppConfig.defaultPollIntervalInSeconds);
    expect(settings.soundEnabled, isTrue);
    expect(settings.notifyEnabled, isTrue);
    expect(settings.serverAddress, isEmpty);
  });

  test('修改轮询间隔会落本地并更新状态', () async {
    final container = buildContainer();
    await container.read(appSettingsProvider.notifier).setPollSeconds(30);
    expect(container.read(appSettingsProvider).pollSeconds, 30);
    expect(prefs.getInt('poll_seconds'), 30);

    // 不在候选列表内的值回退到默认
    await container.read(appSettingsProvider.notifier).setPollSeconds(7);
    expect(
      container.read(appSettingsProvider).pollSeconds,
      AppConfig.defaultPollIntervalInSeconds,
    );
  });

  test('关闭提醒音会更新状态并落本地', () async {
    final container = buildContainer();
    await container.read(appSettingsProvider.notifier).setSoundEnabled(false);
    expect(container.read(appSettingsProvider).soundEnabled, isFalse);
    expect(prefs.getBool('sound_enabled'), isFalse);
  });

  test('保存服务器地址后接口根地址随之切换，且可清空', () async {
    final container = buildContainer();
    final notifier = container.read(appSettingsProvider.notifier);

    await notifier.setServerAddress('192.168.1.5:8081');
    expect(
      container.read(appSettingsProvider).serverAddress,
      'http://192.168.1.5:8081',
    );
    expect(
      container.read(preferencesProvider).apiBaseUrl,
      'http://192.168.1.5:8081/api',
    );

    await notifier.resetServer();
    expect(container.read(appSettingsProvider).serverAddress, isEmpty);
  });

  test('保存服务器地址后 ApiClient 实例不变且 baseUrl 立即切换', () async {
    // 回归：SessionController 等会在 build 中缓存 authApi/apiClient 引用，
    // 若保存地址时替换了 ApiClient 实例，缓存方仍会请求旧地址
    // （表现为登录一直打到占位地址 127.0.0.1:80，连接被拒绝）。
    final container = buildContainer();

    final before = container.read(apiClientProvider);
    // 模拟 SessionController.build：地址尚未配置时就缓存 authApi 引用
    final authBefore = container.read(authApiProvider);
    expect(before.baseUrl, 'http://127.0.0.1${AppConfig.apiPrefix}');

    await container
        .read(appSettingsProvider.notifier)
        .setServerAddress('127.0.0.1:8081');

    final after = container.read(apiClientProvider);
    expect(identical(after, before), isTrue,
        reason: '地址变更不应替换 ApiClient 实例');
    expect(after.baseUrl, 'http://127.0.0.1:8081${AppConfig.apiPrefix}');

    // SessionController 缓存的旧 authApi 引用必须依旧是同一实例，
    // 其内部 ApiClient 的 Dio 已随 rebuild 切到新地址
    final authAfter = container.read(authApiProvider);
    expect(identical(authAfter, authBefore), isTrue);
  });
}
