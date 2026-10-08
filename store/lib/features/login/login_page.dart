import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../core/config/app_config.dart';
import '../../core/services/app_logger.dart';
import '../../core/theme/app_colors.dart';
import '../../providers/session_provider.dart';
import '../../providers/settings_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_feedback.dart';
import 'server_address_editor.dart';

/// 登录页
///
/// 手机号 / 邮箱 + 密码登录（与后端 /api/auth/login 一致：账号含 @ 时按邮箱处理）。
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final TextEditingController _accountController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final ValueNotifier<bool> _obscure = ValueNotifier<bool>(true);

  @override
  void dispose() {
    _accountController.dispose();
    _passwordController.dispose();
    _obscure.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    FocusScope.of(context).unfocus();
    final account = _accountController.text.trim();
    final password = _passwordController.text;
    if (account.isEmpty) {
      AppToast.info(context, '请输入手机号或邮箱');
      return;
    }
    if (password.isEmpty) {
      AppToast.info(context, '请输入密码');
      return;
    }

    final ok = await ref
        .read(sessionProvider.notifier)
        .login(username: account, password: password);
    if (!ok && mounted) {
      final error = ref.read(sessionProvider).errorMessage;
      AppToast.error(context, error ?? '登录失败');
    }
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionProvider);
    final serverAddress = ref.watch(appSettingsProvider).serverAddress;

    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 26),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 56),
              Center(
                child: SvgPicture.asset(
                  'assets/images/logo.svg',
                  width: 92,
                  height: 92,
                ),
              ),
              const SizedBox(height: 16),
              const Center(
                child: Text(
                  AppConfig.appName,
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              const SizedBox(height: 6),
              Center(
                child: GestureDetector(
                  onTap: () => ServerAddressEditor.show(context),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.dns_outlined,
                        size: 14,
                        color: AppColors.textHint,
                      ),
                      const SizedBox(width: 4),
                      ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 240),
                        child: Text(
                          serverAddress.isEmpty ? '未配置服务器' : serverAddress,
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.textSecondary,
                            decoration: TextDecoration.underline,
                            decorationColor: AppColors.textHint,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 2),
                      const Icon(
                        Icons.edit_outlined,
                        size: 12,
                        color: AppColors.textHint,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 42),
              if (session.expired) ...[
                const _SessionExpiredBanner(),
                const SizedBox(height: 18),
              ],
              TextField(
                controller: _accountController,
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                decoration: const InputDecoration(
                  labelText: '手机号 / 邮箱',
                  prefixIcon: Icon(Icons.account_circle_outlined, size: 20),
                ),
              ),
              const SizedBox(height: 14),
              ValueListenableBuilder<bool>(
                valueListenable: _obscure,
                builder: (context, obscure, _) => TextField(
                  controller: _passwordController,
                  obscureText: obscure,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _login(),
                  decoration: InputDecoration(
                    labelText: '密码',
                    prefixIcon: const Icon(Icons.lock_outline, size: 20),
                    suffixIcon: IconButton(
                      icon: Icon(
                        obscure ? Icons.visibility_off : Icons.visibility,
                        size: 18,
                      ),
                      onPressed: () => _obscure.value = !obscure,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 26),
              PrimaryButton(
                label: '登录',
                busy: session.isBusy,
                onPressed: session.isBusy ? null : _login,
              ),
              const SizedBox(height: 16),
              const Center(
                child: Text(
                  '请使用店员 / 店主账号登录，联系管理员开通权限',
                  style: TextStyle(fontSize: 12, color: AppColors.textHint),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 记录一次登录失败的local（便于排查）
void logLoginFailure(String reason) =>
    AppLogger.w('登录失败：$reason');

/// 登录态过期提示
///
/// 令牌失效被网络层踢回登录页时展示，让用户知道是「过期」而不是「没登录过」。
class _SessionExpiredBanner extends StatelessWidget {
  const _SessionExpiredBanner();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: AppColors.warningLight,
        borderRadius: BorderRadius.circular(8),
      ),
      child: const Row(
        children: [
          Icon(Icons.lock_clock, size: 16, color: AppColors.warning),
          SizedBox(width: 8),
          Expanded(
            child: Text(
              '登录已过期，请重新登录',
              style: TextStyle(
                fontSize: 12.5,
                color: Color(0xFF8A5A00),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
