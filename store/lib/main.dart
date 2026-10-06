import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app.dart';
import 'core/services/app_logger.dart';
import 'core/services/notification_service.dart';
import 'providers/core_providers.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 中文日期格式化的 locale 数据（intl 使用）
  await initializeDateFormatting('zh_CN', null);

  // 启动期任何服务初始化失败都不能阻断 runApp：Windows runner 仅在首帧
  // 渲染完成后才显示窗口，runApp 之前抛出的未捕获异常会表现为「进程在、无窗口」。
  try {
    // 日志目录（path_provider）与本地通知（含权限申请）
    await AppLogger.init();
    await NotificationService.instance.init();
  } catch (e, s) {
    AppLogger.e('启动服务初始化失败', error: e, stackTrace: s);
  }

  final sharedPreferences = await SharedPreferences.getInstance();

  // 竖屏优先：店员多为单手操作场景（桌面端没有屏幕方向概念，必须跳过，
  // 否则部分桌面引擎会回 MissingPluginException）
  if (Platform.isAndroid || Platform.isIOS) {
    await SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
    ]);
  }

  runApp(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(sharedPreferences),
      ],
      child: const StoreApp(),
    ),
  );
}
