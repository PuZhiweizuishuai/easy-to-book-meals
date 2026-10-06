import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/config/app_config.dart';
import '../../core/services/app_logger.dart';
import '../../core/services/sound_service.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import '../../providers/alert_provider.dart';
import '../../providers/settings_provider.dart';
import '../../providers/core_providers.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_button.dart';
import '../login/server_address_editor.dart';

/// 我的（设置）页
///
/// 账号信息、门店切换、提醒偏好（声音 / 通知 / 轮询频率）、
/// 服务器地址修改与退出登录。
class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(currentUserProvider);
    final settings = ref.watch(appSettingsProvider);
    final storeContext = ref.watch(storeContextProvider);

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: const Text('我的'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
        children: [
          // 账号信息
          AppCard(
            child: Row(
              children: [
                CircleAvatar(
                  radius: 26,
                  backgroundColor: AppColors.primaryLight,
                  child: Icon(
                    Symbols.account_circle,
                    size: 30,
                    color: AppColors.primary,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        user?.displayName ?? '未知用户',
                        style: const TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        user?.phone?.isNotEmpty == true
                            ? user!.phone!
                            : (user?.email ?? '未绑定手机号'),
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      if (user?.roleDataList?.isNotEmpty == true) ...[
                        const SizedBox(height: 5),
                        Wrap(
                          spacing: 6,
                          runSpacing: 4,
                          children: [
                            for (final role in user!.roleDataList!)
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 7,
                                  vertical: 2,
                                ),
                                decoration: BoxDecoration(
                                  color: AppColors.infoLight,
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  role.roleName ?? role.roleCode ?? '角色',
                                  style: const TextStyle(
                                    fontSize: 11,
                                    color: AppColors.info,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // 门店切换
          _SettingsSection(
            title: '门店',
            children: [
              if (storeContext.stores.isEmpty)
                const ListTile(
                  title: Text('暂无可管理门店'),
                  subtitle: Text('请联系店主开通门店角色'),
                )
              else
                RadioGroup<int>(
                  groupValue: storeContext.currentStoreId,
                  onChanged: (value) async {
                    if (value == null) return;
                    await ref
                        .read(storeContextProvider.notifier)
                        .selectStore(value);
                    // 门店换了，提醒基线要重建并立刻重新轮询
                    ref.read(alertProvider.notifier).restart();
                  },
                  child: Column(
                    children: [
                      for (final store in storeContext.stores)
                        RadioListTile<int>(
                          value: store.id!,
                          title: Text(store.displayName),
                          subtitle: Text(
                            store.address?.isNotEmpty == true
                                ? store.address!
                                : storeStatusName(store.status),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                  ),
                ),
            ],
          ),
          const SizedBox(height: 14),

          // 提醒设置
          _SettingsSection(
            title: '提醒设置',
            children: [
              SwitchListTile(
                title: const Text('声音提醒'),
                subtitle: const Text('新订单 / 新菜品到达时播放提示音'),
                value: settings.soundEnabled,
                activeThumbColor: AppColors.primary,
                onChanged: (value) async {
                  await ref
                      .read(appSettingsProvider.notifier)
                      .setSoundEnabled(value);
                  if (value) {
                    await ref
                        .read(soundServiceProvider)
                        .play(AlertTone.newOrder);
                  }
                },
              ),
              SwitchListTile(
                title: const Text('系统通知'),
                subtitle: const Text('App 在后台时通过通知栏提醒'),
                value: settings.notifyEnabled,
                activeThumbColor: AppColors.primary,
                onChanged: (value) async {
                  await ref
                      .read(appSettingsProvider.notifier)
                      .setNotifyEnabled(value);
                },
              ),
              ListTile(
                leading: const Icon(Icons.timer_outlined),
                title: const Text('轮询频率'),
                subtitle: Text('当前每 ${settings.pollSeconds} 秒检查一次新订单'),
                trailing: DropdownButton<int>(
                  value: AppConfig.pollIntervalOptions.contains(
                    settings.pollSeconds,
                  )
                      ? settings.pollSeconds
                      : AppConfig.defaultPollIntervalInSeconds,
                  underline: const SizedBox.shrink(),
                  items: [
                    for (final seconds in AppConfig.pollIntervalOptions)
                      DropdownMenuItem<int>(
                        value: seconds,
                        child: Text('$seconds 秒'),
                      ),
                  ],
                  onChanged: (value) async {
                    if (value == null) return;
                    await ref
                        .read(appSettingsProvider.notifier)
                        .setPollSeconds(value);
                    // 重新排下一次轮询，新频率立即生效
                    ref.read(alertProvider.notifier).restart();
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),

          // 服务器
          _SettingsSection(
            title: '服务器',
            children: [
              ListTile(
                leading: const Icon(Icons.dns_outlined),
                title: const Text('服务器地址'),
                subtitle: Text(
                  settings.serverAddress.isEmpty
                      ? '未配置'
                      : settings.serverAddress,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                trailing: const Icon(Icons.chevron_right, size: 18),
                onTap: () => ServerAddressEditor.show(context),
              ),
            ],
          ),
          const SizedBox(height: 14),

          // 关于
          _SettingsSection(
            title: '关于',
            children: [
              ListTile(
                leading: const Icon(Icons.info_outline),
                title: const Text('应用版本'),
                subtitle: const Text('${AppConfig.appName} v1.0.0'),
              ),
              if (AppLogger.logFilePath != null)
                ListTile(
                  leading: const Icon(Icons.description_outlined),
                  title: const Text('日志文件'),
                  subtitle: Text(
                    AppLogger.logFilePath!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 26),

          PrimaryButton(
            label: '退出登录',
            icon: Icons.logout,
            backgroundColor: AppColors.danger,
            onPressed: () async {
              final ok = await showDialog<bool>(
                    context: context,
                    builder: (dialogContext) => AlertDialog(
                      title: const Text('退出登录'),
                      content: const Text('退出后需要重新输入账号密码，确定继续？'),
                      actions: [
                        TextButton(
                          onPressed: () =>
                              Navigator.of(dialogContext).pop(false),
                          child: const Text('取消'),
                        ),
                        FilledButton(
                          onPressed: () =>
                              Navigator.of(dialogContext).pop(true),
                          child: const Text('退出'),
                        ),
                      ],
                    ),
                  ) ??
                  false;
              if (!ok) return;
              await ref.read(sessionProvider.notifier).logout();
            },
          ),
        ],
      ),
    );
  }
}

/// 设置分区容器
class _SettingsSection extends StatelessWidget {
  const _SettingsSection({required this.title, required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 6),
          child: Text(
            title,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: AppColors.textHint,
            ),
          ),
        ),
        AppCard(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Column(children: children),
        ),
      ],
    );
  }
}
