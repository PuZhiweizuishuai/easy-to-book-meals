import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/config/app_config.dart';
import 'core/routing/app_navigator.dart';
import 'core/services/app_logger.dart';
import 'core/services/notification_service.dart';
import 'core/theme/app_theme.dart';
import 'features/root/root_page.dart';
import 'providers/settings_provider.dart';
import 'providers/alert_provider.dart';

/// 应用根 Widget
class StoreApp extends ConsumerStatefulWidget {
  const StoreApp({super.key});

  @override
  ConsumerState<StoreApp> createState() => _StoreAppState();
}

class _StoreAppState extends ConsumerState<StoreApp>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // 同步本地设置到服务层，并注册通知点击回调
    Future<void>.microtask(() {
      ref.read(appSettingsProvider.notifier).syncServices();
      NotificationService.instance.onNotificationTap = _handleNotificationTap;
      AppLogger.i('应用启动完成');
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    // 回到前台时立刻恢复轮询，保证接单不延迟
    if (state == AppLifecycleState.resumed) {
      ref.read(alertProvider.notifier).restart();
    } else if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      ref.read(alertProvider.notifier).stop();
    }
  }

  /// 点击通知：切换到对应 Tab
  void _handleNotificationTap(NotificationPayload payload) {
    AppTabJump.jumpTo(AppTabJump.indexOf(payload));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: AppConfig.appName,
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      navigatorKey: AppNavigator.navigatorKey,
      builder: (context, child) {
        // 禁止长按/点击时的字体缩放跟随系统，避免大字号机型布局溢出
        final media = MediaQuery.of(context);
        return MediaQuery(
          data: media.copyWith(textScaler: TextScaler.noScaling),
          child: child ?? const SizedBox.shrink(),
        );
      },
      home: const RootPage(),
    );
  }
}
