import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import 'app_logger.dart';

/// 通知载荷：点击通知后据此跳转到对应页面
class NotificationPayload {
  const NotificationPayload(this.route, {this.argument});

  /// 目标路由，取值见 [NotificationPayload] 工厂
  final String route;

  /// 附加参数（如门店 ID、订单 ID）
  final String? argument;

  factory NotificationPayload.fromRaw(String? raw) {
    if (raw == null || raw.isEmpty) {
      return const NotificationPayload(NotificationPayload.routeOrders);
    }
    try {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      return NotificationPayload(
        map['route'] as String? ?? NotificationPayload.routeOrders,
        argument: map['argument']?.toString(),
      );
    } catch (_) {
      return const NotificationPayload(NotificationPayload.routeOrders);
    }
  }

  String toRaw() => jsonEncode({'route': route, 'argument': argument});

  /// 订单管理页
  static const String routeOrders = 'orders';

  /// 后厨出餐页
  static const String routeKitchen = 'kitchen';
}

/// 本地通知服务
///
/// 轮询到新订单 / 新菜品时，在系统通知栏弹出提醒（即使 App 在后台）。
/// 提示音由 [SoundService] 单独播放，因此这里关闭系统通知自带声音，避免双重播放。
class NotificationService {
  NotificationService._();

  static final NotificationService instance = NotificationService._();

  static const String _channelId = 'store_alert_channel';
  static const String _channelName = '订单与出餐提醒';
  static const String _channelDesc = '新订单、后厨新增菜品的实时提醒';

  final FlutterLocalNotificationsPlugin _plugin =
      FlutterLocalNotificationsPlugin();

  bool _enabled = true;

  /// 是否允许弹出系统通知
  bool get enabled => _enabled;

  /// 由外层根据偏好设置同步开关状态
  set enabled(bool value) => _enabled = value;

  /// 通知点击回调（由 AppRoot 注册，用于跳转到对应页面）
  void Function(NotificationPayload payload)? onNotificationTap;

  Future<void> init() async {
    // flutter_local_notifications 不支持 Windows，桌面端降级为空实现。
    // 注意：此处若在 runApp 之前抛 MissingPluginException，会导致首帧无法渲染、
    // Windows 窗口永不显示（runner 仅在首帧回调里 Show 窗口）。
    if (Platform.isWindows) {
      _enabled = false;
      return;
    }
    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const ios = DarwinInitializationSettings();
    const settings = InitializationSettings(
      android: android,
      iOS: ios,
    );
    await _plugin.initialize(
      settings: settings,
      onDidReceiveNotificationResponse: _handleResponse,
    );
    await _ensureAndroidChannel();
    await requestPermission();
  }

  Future<void> _ensureAndroidChannel() async {
    if (!Platform.isAndroid) return;
    final androidImpl =
        _plugin.resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>();
    await androidImpl?.createNotificationChannel(
      const AndroidNotificationChannel(
        _channelId,
        _channelName,
        description: _channelDesc,
        importance: Importance.max,
        playSound: true,
        enableVibration: true,
      ),
    );
  }

  /// 申请通知权限（Android 13+ / iOS 需要显式申请）
  Future<bool> requestPermission() async {
    if (Platform.isAndroid) {
      final androidImpl =
          _plugin.resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>();
      final granted = await androidImpl?.requestNotificationsPermission();
      return granted ?? true;
    }
    if (Platform.isIOS) {
      final iosImpl =
          _plugin.resolvePlatformSpecificImplementation<
              IOSFlutterLocalNotificationsPlugin>();
      return await iosImpl?.requestPermissions(alert: true, sound: true) ??
          true;
    }
    return true;
  }

  void _handleResponse(NotificationResponse response) {
    final payload = NotificationPayload.fromRaw(response.payload);
    onNotificationTap?.call(payload);
  }

  /// 弹出提醒通知；[id] 相同会覆盖旧通知，避免通知栏堆满
  Future<void> show({
    required int id,
    required String title,
    required String body,
    required NotificationPayload payload,
  }) async {
    if (!_enabled) return;
    final details = NotificationDetails(
      android: AndroidNotificationDetails(
        _channelId,
        _channelName,
        channelDescription: _channelDesc,
        importance: Importance.max,
        priority: Priority.high,
        playSound: true,
        enableVibration: true,
        category: AndroidNotificationCategory.alarm,
        styleInformation: BigTextStyleInformation(body),
      ),
      iOS: const DarwinNotificationDetails(presentAlert: true, presentSound: true),
    );
    try {
      await _plugin.show(
        id: id,
        title: title,
        body: body,
        notificationDetails: details,
        payload: payload.toRaw(),
      );
    } catch (e, s) {
      AppLogger.w('弹出通知失败', error: e, stackTrace: s);
    }
  }

  /// 清空所有已弹出的提醒
  Future<void> cancelAll() async {
    if (Platform.isWindows) return;
    await _plugin.cancelAll();
  }
}
