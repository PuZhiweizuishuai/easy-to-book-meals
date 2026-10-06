import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/services/app_logger.dart';
import '../../providers/session_provider.dart';
import '../../providers/settings_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_feedback.dart';
import '../server_config/server_config_page.dart';

/// 服务器地址修改入口
///
/// 登录页与「我的」页共用：以底部表单的形式弹出 [ServerConfigPage]，
/// 保存成功后若当前已登录则强制回到登录页重新登录（换了服务器，token 不再有效）。
class ServerAddressEditor {
  ServerAddressEditor._();

  static Future<void> show(BuildContext context) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (sheetContext) => SafeArea(
        top: false,
        child: SingleChildScrollView(
          child: Padding(
            padding: EdgeInsets.only(
              left: 16,
              right: 16,
              top: 16,
              bottom: 16 + MediaQuery.of(sheetContext).viewInsets.bottom,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    const Expanded(
                      child: Text(
                        '服务器设置',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, size: 20),
                      onPressed: () => Navigator.of(sheetContext).pop(),
                    ),
                  ],
                ),
                const Divider(height: 1),
                const SizedBox(height: 14),
                const ServerConfigPage(embedded: true),
                const _ResetServerSection(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// 「切换服务器」操作：清除登录态并回到配置引导
class _ResetServerSection extends ConsumerWidget {
  const _ResetServerSection();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Column(
        children: [
          SecondaryButton(
            label: '切换服务器 / 重新配置',
            icon: Icons.swap_horiz,
            expand: true,
            color: const Color(0xFF6B7280),
            onPressed: () async {
              final ok = await showDialog<bool>(
                    context: context,
                    builder: (dialogContext) => AlertDialog(
                      title: const Text('重置服务器地址'),
                      content: const Text(
                        '将清除当前服务器地址与登录状态，回到配置引导页。是否继续？',
                      ),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.of(dialogContext).pop(false),
                          child: const Text('取消'),
                        ),
                        FilledButton(
                          onPressed: () => Navigator.of(dialogContext).pop(true),
                          child: const Text('确定'),
                        ),
                      ],
                    ),
                  ) ??
                  false;
              if (!ok || !context.mounted) return;
              AppLogger.i('用户重置服务器地址');
              await ref.read(sessionProvider.notifier).markUnconfigured();
              await ref.read(appSettingsProvider.notifier).resetServer();
              if (!context.mounted) return;
              AppToast.info(context, '已清除服务器配置，请重新填写');
            },
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}
