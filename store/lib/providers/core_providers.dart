import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../core/network/api_client.dart';
import '../core/services/app_logger.dart';
import '../core/services/notification_service.dart';
import '../core/services/preferences_service.dart';
import '../core/services/sound_service.dart';
import '../core/services/url_resolver.dart';
import '../data/api/auth_api.dart';
import '../data/api/coupon_api.dart';
import '../data/api/dish_api.dart';
import '../data/api/file_api.dart';
import '../data/api/membership_api.dart';
import '../data/api/order_api.dart';
import '../data/api/report_api.dart';
import '../data/api/table_api.dart';

/// 在 main 中通过 override 注入的 SharedPreferences 实例
final sharedPreferencesProvider =
    Provider<SharedPreferences>((ref) => throw UnimplementedError());

/// 本地存储服务
final preferencesProvider = Provider<PreferencesService>((ref) {
  final prefs = ref.watch(sharedPreferencesProvider);
  final service = PreferencesService(prefs);
  AppUrlResolver.update(service.serverAddress);
  return service;
});

/// 服务器地址变更版本号
///
/// 每次在「服务器设置」里保存新地址都会自增，
/// 从而触发 [apiClientProvider] 重建 Dio（重新读取新的 baseUrl）。
final serverConfigVersionProvider =
    NotifierProvider<ServerConfigVersion, int>(ServerConfigVersion.new);

/// 服务器配置版本（自增计数器）
class ServerConfigVersion extends Notifier<int> {
  @override
  int build() => 0;

  /// 自增：通知依赖者重建网络客户端
  void bump() => state = state + 1;
}

/// 网络客户端
///
/// 服务器地址变更时通过 [serverConfigVersionProvider] 通知，调用
/// [ApiClient.rebuild] 只替换底层 Dio；ApiClient 实例本身保持稳定，
/// 这样长期持有 API 对象的 Notifier（如 SessionController 中缓存的
/// authApi）无需重建即可拿到新地址，避免继续请求旧地址。
final apiClientProvider = Provider<ApiClient>((ref) {
  final prefs = ref.watch(preferencesProvider);
  final client = ApiClient(prefs);
  ref.listen(serverConfigVersionProvider, (_, _) => client.rebuild());
  ref.onDispose(client.close);
  return client;
});

/// 提醒音服务
final soundServiceProvider = Provider<SoundService>((ref) {
  final prefs = ref.watch(preferencesProvider);
  final service = SoundService();
  service.enabled = prefs.soundEnabled;
  ref.onDispose(service.dispose);
  return service;
});

/// 本地通知服务
final notificationServiceProvider = Provider<NotificationService>((ref) {
  final prefs = ref.watch(preferencesProvider);
  final service = NotificationService.instance;
  service.enabled = prefs.notifyEnabled;
  return service;
});

// ==================== API ====================

final authApiProvider = Provider<AuthApi>(
  (ref) => AuthApi(ref.watch(apiClientProvider)),
);

final orderApiProvider = Provider<OrderApi>(
  (ref) => OrderApi(ref.watch(apiClientProvider)),
);

final dishApiProvider = Provider<DishApi>(
  (ref) => DishApi(ref.watch(apiClientProvider)),
);

final fileApiProvider = Provider<FileApi>(
  (ref) => FileApi(ref.watch(apiClientProvider)),
);

final categoryApiProvider = Provider<CategoryApi>(
  (ref) => CategoryApi(ref.watch(apiClientProvider)),
);

final tableApiProvider = Provider<TableApi>(
  (ref) => TableApi(ref.watch(apiClientProvider)),
);

final membershipApiProvider = Provider<MembershipApi>(
  (ref) => MembershipApi(ref.watch(apiClientProvider)),
);

final couponApiProvider = Provider<CouponApi>(
  (ref) => CouponApi(ref.watch(apiClientProvider)),
);

final reportApiProvider = Provider<ReportApi>(
  (ref) => ReportApi(ref.watch(apiClientProvider)),
);

/// 在 main 中调用的初始化：同步图片地址解析器与日志目录
Future<void> bootstrapServices() async {
  await AppLogger.init();
  await NotificationService.instance.init();
}
