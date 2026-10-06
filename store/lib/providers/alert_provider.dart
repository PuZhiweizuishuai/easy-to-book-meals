import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/config/app_config.dart';
import '../core/network/api_exception.dart';
import '../core/services/app_logger.dart';
import '../core/services/notification_service.dart';
import '../core/services/sound_service.dart';
import '../data/api/order_api.dart';
import '../data/models/models.dart';
import 'core_providers.dart';
import 'session_provider.dart';
import 'settings_provider.dart';
import 'store_context_provider.dart';

/// 提醒模式：决定轮询哪些数据
enum AlertKind {
  /// 新订单（待接单订单新增）
  newOrder,

  /// 后厨新增待制作菜品
  newKitchenItem,
}

/// 提醒状态（供 UI 展示「最近一次提醒」与轮询开关）
class AlertState {
  const AlertState({
    this.lastOrderAlertAt,
    this.lastKitchenAlertAt,
    this.newOrder = false,
    this.newKitchenItem = false,
    this.error,
  });

  /// 上次新订单提醒时间
  final DateTime? lastOrderAlertAt;

  /// 上次新菜品提醒时间
  final DateTime? lastKitchenAlertAt;

  /// 是否有未处理的新订单提醒（进入订单页后清除）
  final bool newOrder;

  /// 是否有未处理的新菜品提醒（进入后厨页后清除）
  final bool newKitchenItem;

  /// 轮询过程中的错误（仅提示，不阻塞轮询）
  final String? error;

  AlertState copyWith({
    DateTime? lastOrderAlertAt,
    DateTime? lastKitchenAlertAt,
    bool? newOrder,
    bool? newKitchenItem,
    String? error,
    bool clearError = false,
  }) =>
      AlertState(
        lastOrderAlertAt: lastOrderAlertAt ?? this.lastOrderAlertAt,
        lastKitchenAlertAt: lastKitchenAlertAt ?? this.lastKitchenAlertAt,
        newOrder: newOrder ?? this.newOrder,
        newKitchenItem: newKitchenItem ?? this.newKitchenItem,
        error: clearError ? null : (error ?? this.error),
      );
}

/// 新订单 / 新菜品提醒控制器
///
/// 后端没有 WebSocket/SSE，因此采用定时轮询：
/// - 待接单订单列表：发现新 orderId 即视为新订单
/// - 后厨队列：发现新 itemId 即视为新增待制作菜品
///
/// 首次轮询只建立基线（不提醒），避免刚进 App 就被历史数据轰亲。
class AlertController extends Notifier<AlertState> {
  Timer? _timer;
  bool _polling = false;

  /// 已知订单 ID（基线）
  final Set<int> _knownOrderIds = <int>{};

  /// 已知菜品明细 ID（基线）
  final Set<int> _knownItemIds = <int>{};

  /// 基线是否已建立
  bool _initialized = false;

  @override
  AlertState build() {
    // 门店切换时重置基线，避免把其它门店的历史订单当成新订单
    ref.listen(currentStoreIdProvider, (previous, next) => _resetBaseline());
    ref.onDispose(stop);
    _scheduleNext();
    return const AlertState();
  }

  OrderApi get _orderApi => ref.read(orderApiProvider);
  SoundService get _sound => ref.read(soundServiceProvider);
  NotificationService get _notify => ref.read(notificationServiceProvider);

  int? get _storeId => ref.read(currentStoreIdProvider);

  void _resetBaseline() {
    _knownOrderIds.clear();
    _knownItemIds.clear();
    _initialized = false;
  }

  /// 重启轮询（App 从后台回到前台时调用）
  void restart() {
    stop();
    _scheduleNext();
  }

  /// 暂停轮询
  void stop() {
    _timer?.cancel();
    _timer = null;
  }

  /// 读取当前轮询间隔
  int get _pollSeconds {
    try {
      return ref.read(appSettingsProvider).pollSeconds;
    } catch (_) {
      return AppConfig.defaultPollIntervalInSeconds;
    }
  }

  /// 排下一次轮询：每次都重新读取间隔，改设置后立即生效
  void _scheduleNext() {
    _timer?.cancel();
    _timer = Timer(Duration(seconds: _pollSeconds), _tick);
  }

  Future<void> _tick() async {
    await poll();
    _scheduleNext();
  }

  /// 手动触发一次轮询
  Future<void> poll() async {
    if (_polling) return;
    final storeId = _storeId;
    final session = ref.read(sessionProvider);
    if (storeId == null || !session.isAuthenticated) return;

    _polling = true;
    try {
      await Future.wait<void>([
        _checkNewOrders(storeId),
        _checkKitchenQueue(storeId),
      ]);
      state = state.copyWith(clearError: true);
    } catch (e) {
      if (e is ApiException && e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        stop();
      }
      state = state.copyWith(error: e.toString());
    } finally {
      _polling = false;
    }
  }

  /// 检查新订单
  Future<void> _checkNewOrders(int storeId) async {
    final page = await _orderApi.list(
      storeId: storeId,
      status: OrderStatus.pending.code,
      page: 1,
      size: 50,
    );
    final currentIds =
        page.content.map((o) => o.id).whereType<int>().toSet();

    if (!_initialized) {
      _knownOrderIds.addAll(currentIds);
      return;
    }
    final newIds = currentIds.difference(_knownOrderIds);
    _knownOrderIds
      ..clear()
      ..addAll(currentIds);

    if (newIds.isEmpty) return;

    final newOrders = page.content
        .where((o) => o.id != null && newIds.contains(o.id))
        .toList();

    state = state.copyWith(
      newOrder: true,
      lastOrderAlertAt: DateTime.now(),
    );
    await _sound.play(AlertTone.newOrder);
    for (final order in newOrders) {
      await _notify.show(
        id: 10000 + (order.id ?? 0) % 1000,
        title: '新订单待处理',
        body: _orderAlertBody(order),
        payload: NotificationPayload(NotificationPayload.routeOrders),
      );
    }
    AppLogger.i('检测到新订单 ${newIds.length} 单');
  }

  /// 检查后厨新增待制作菜品
  Future<void> _checkKitchenQueue(int storeId) async {
    final queue = await _orderApi.kitchenQueue(storeId: storeId);
    final currentIds =
        queue.items.map((i) => i.itemId).whereType<int>().toSet();

    if (!_initialized) {
      _knownItemIds.addAll(currentIds);
      _initialized = true;
      return;
    }
    final newIds = currentIds.difference(_knownItemIds);
    _knownItemIds
      ..clear()
      ..addAll(currentIds);

    if (newIds.isEmpty) return;
    final items = queue.items
        .where((i) => i.itemId != null && newIds.contains(i.itemId))
        .toList();

    state = state.copyWith(
      newKitchenItem: true,
      lastKitchenAlertAt: DateTime.now(),
    );
    await _sound.play(AlertTone.newItem);
    if (items.isNotEmpty) {
      final first = items.first;
      await _notify.show(
        id: 20000 + (first.itemId ?? 0) % 1000,
        title: '后厨新增 ${items.length} 道菜',
        body: items.length == 1
            ? '${first.dishName ?? '未知菜品'} × ${first.count.toStringAsFixed(0)}'
            : items
                .take(3)
                .map((i) => i.dishName ?? '未知菜品')
                .join('、'),
        payload: NotificationPayload(NotificationPayload.routeKitchen),
      );
    }
    AppLogger.i('检测到新待制作菜品 ${newIds.length} 道');
  }

  /// 构造通知正文：订单号 + 桌号/类型 + 金额
  String _orderAlertBody(OrderData order) {
    final buffer = StringBuffer();
    buffer.write(order.orderNo ?? '新订单');
    final target = order.tableNumber?.isNotEmpty == true
        ? ' · ${order.tableNumber}桌'
        : ' · ${order.typeEnum.label}';
    buffer.write(target);
    buffer.write(' · ¥${(order.payAmount ?? 0).toStringAsFixed(2)}');
    return buffer.toString();
  }

  /// 用户进入某页面后清除对应角标
  void clearBadge(AlertKind kind) {
    if (kind == AlertKind.newOrder) {
      state = state.copyWith(newOrder: false);
    } else {
      state = state.copyWith(newKitchenItem: false);
    }
  }
}

/// 全局提醒状态
final alertProvider = NotifierProvider<AlertController, AlertState>(
  AlertController.new,
);

/// 是否有新订单提醒
final hasNewOrderAlertProvider = Provider<bool>(
  (ref) => ref.watch(alertProvider).newOrder,
);

/// 是否有新菜品提醒
final hasNewKitchenAlertProvider = Provider<bool>(
  (ref) => ref.watch(alertProvider).newKitchenItem,
);
