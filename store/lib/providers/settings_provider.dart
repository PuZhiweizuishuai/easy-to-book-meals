import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/config/app_config.dart';
import '../core/services/app_logger.dart';
import '../core/services/preferences_service.dart';
import '../core/services/url_resolver.dart';
import 'core_providers.dart';

/// 应用设置状态
class AppSettingsState {
  const AppSettingsState({
    this.serverAddress = '',
    this.soundEnabled = true,
    this.notifyEnabled = true,
    this.pollSeconds = AppConfig.defaultPollIntervalInSeconds,
  });

  final String serverAddress;
  final bool soundEnabled;
  final bool notifyEnabled;
  final int pollSeconds;
}

/// 应用设置控制器
///
/// 服务器地址、声音、通知、轮询频率都落本地；改动即时生效。
class AppSettingsController extends Notifier<AppSettingsState> {
  @override
  AppSettingsState build() {
    final prefs = ref.read(preferencesProvider);
    return AppSettingsState(
      serverAddress: prefs.serverAddress,
      soundEnabled: prefs.soundEnabled,
      notifyEnabled: prefs.notifyEnabled,
      pollSeconds: prefs.pollSeconds,
    );
  }

  PreferencesService get _prefs => ref.read(preferencesProvider);

  /// 保存服务器地址（同时刷新图片地址解析器与 Dio）
  Future<void> setServerAddress(String address) async {
    await _prefs.setServerAddress(address);
    AppUrlResolver.update(_prefs.serverAddress);
    // 自增版本号以重建网络客户端
    ref.read(serverConfigVersionProvider.notifier).bump();
    state = AppSettingsState(
      serverAddress: _prefs.serverAddress,
      soundEnabled: state.soundEnabled,
      notifyEnabled: state.notifyEnabled,
      pollSeconds: state.pollSeconds,
    );
  }

  /// 切换声音提醒
  Future<void> setSoundEnabled(bool value) async {
    await _prefs.setSoundEnabled(value);
    ref.read(soundServiceProvider).enabled = value;
    state = AppSettingsState(
      serverAddress: state.serverAddress,
      soundEnabled: value,
      notifyEnabled: state.notifyEnabled,
      pollSeconds: state.pollSeconds,
    );
  }

  /// 切换系统通知
  Future<void> setNotifyEnabled(bool value) async {
    await _prefs.setNotifyEnabled(value);
    ref
        .read(notificationServiceProvider)
        .enabled = value;
    if (!value) {
      // 关闭通知时顺手清掉已弹出的提醒
      await ref.read(notificationServiceProvider).cancelAll();
    }
    state = AppSettingsState(
      serverAddress: state.serverAddress,
      soundEnabled: state.soundEnabled,
      notifyEnabled: value,
      pollSeconds: state.pollSeconds,
    );
  }

  /// 设置轮询间隔（秒）
  Future<void> setPollSeconds(int seconds) async {
    final safe = AppConfig.pollIntervalOptions.contains(seconds)
        ? seconds
        : AppConfig.defaultPollInterval.inSeconds;
    await _prefs.setPollSeconds(safe);
    state = AppSettingsState(
      serverAddress: state.serverAddress,
      soundEnabled: state.soundEnabled,
      notifyEnabled: state.notifyEnabled,
      pollSeconds: safe,
    );
  }

  /// 把本地设置同步到服务层（App 启动时调用一次）
  void syncServices() {
    ref.read(soundServiceProvider).enabled = state.soundEnabled;
    ref.read(notificationServiceProvider).enabled = state.notifyEnabled;
    AppLogger.i('应用设置已同步：${state.serverAddress}');
  }

  /// 清空服务器地址（回到配置页重新填写）
  Future<void> resetServer() async {
    await setServerAddress('');
  }
}

/// 应用设置
final appSettingsProvider =
    NotifierProvider<AppSettingsController, AppSettingsState>(
  AppSettingsController.new,
);
