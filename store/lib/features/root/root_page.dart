import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/config/app_config.dart';
import '../../providers/session_provider.dart';
import '../home/home_shell.dart';
import '../login/login_page.dart';
import '../server_config/server_config_page.dart';
import 'splash_view.dart';

/// 启动路由页
///
/// 根据会话状态决定展示哪个页面：
/// - 未配置服务器地址 → [ServerConfigPage]
/// - 已配置但未登录 → [LoginPage]
/// - 已登录 → [HomeShell]
class RootPage extends ConsumerWidget {
  const RootPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(sessionProvider);

    if (session.isChecking) {
      return SplashView(appName: AppConfig.appName);
    }
    if (session.isUnconfigured) {
      return const ServerConfigPage();
    }
    if (session.isAuthenticated) {
      return const HomeShell();
    }
    return const LoginPage();
  }
}
