import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../core/config/app_config.dart';
import '../../core/theme/app_colors.dart';

/// 启动闪屏
///
/// 仅在读取本地会话状态的短暂过程中展示。
class SplashView extends StatelessWidget {
  const SplashView({super.key, required this.appName});

  final String appName;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            SvgPicture.asset(
              'assets/images/logo.svg',
              width: 96,
              height: 96,
            ),
            const SizedBox(height: 18),
            Text(
              appName,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 40),
            const SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          ],
        ),
      ),
    );
  }
}

/// 默认后端端口提示，用于配置页的输入框 hint
String get defaultPortHint => AppConfig.defaultPortHint;
