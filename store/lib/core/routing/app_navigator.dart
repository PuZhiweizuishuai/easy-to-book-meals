import 'package:flutter/material.dart';

import '../../core/services/notification_service.dart';

/// 全局导航器
///
/// 通知点击后需要跳转到指定 Tab / 页面，但回调发生在没有任何可用 context 的地方，
/// 因此统一走这里持有的全局 NavigatorState。
class AppNavigator {
  AppNavigator._();

  /// MaterialApp 注入的全局 key
  static final GlobalKey<NavigatorState> navigatorKey =
      GlobalKey<NavigatorState>();

  /// 当前导航上下文
  static BuildContext? get context => navigatorKey.currentContext;

  /// 压入新页面
  static Future<T?> push<T>(Widget page) {
    final state = navigatorKey.currentState;
    if (state == null) return Future<T?>.value(null);
    return state.push<T>(MaterialPageRoute<T>(builder: (_) => page));
  }

  /// 弹出当前页面
  static void pop<T extends Object?>([T? result]) {
    navigatorKey.currentState?.pop<T>(result);
  }

  /// 回到根路由（RootPage）
  ///
  /// RootPage 变化只影响 home 子树，已 push 的二级页面/弹层仍盖在上面，
  /// 登录态失效时必须先出栈，用户才会真正看到登录页。
  static void popToRoot() {
    navigatorKey.currentState?.popUntil((route) => route.isFirst);
  }
}

/// Tab 跳转通道
///
/// 通知回调 → 这里 → [HomeShell] 监听并切换到对应 Tab。
/// 用 ValueNotifier 而不是 Provider，是为了在不持有 ref 的静态回调里也能发消息。
class AppTabJump {
  AppTabJump._();

  /// 目标 Tab 索引（消费后置 null）
  static final ValueNotifier<int?> target = ValueNotifier<int?>(null);

  /// 请求跳转到某个 Tab
  static void jumpTo(int index) => target.value = index;

  /// 通知载荷 → Tab 索引
  static int indexOf(NotificationPayload payload) {
    return payload.route == NotificationPayload.routeKitchen
        ? AppTabIndex.kitchen
        : AppTabIndex.orders;
  }
}

/// 底部导航 Tab 索引常量
class AppTabIndex {
  const AppTabIndex._();

  /// 首页看板
  static const int dashboard = 0;

  /// 订单管理
  static const int orders = 1;

  /// 后厨出餐
  static const int kitchen = 2;

  /// 门店管理（菜品 / 分类 / 餐桌 / 会员）
  static const int manage = 3;

  /// 我的（设置）
  static const int profile = 4;
}
