import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../../core/services/app_logger.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/server_address.dart';
import '../../data/api/dish_api.dart';
import '../../providers/core_providers.dart';
import '../../providers/session_provider.dart';
import '../../providers/settings_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_feedback.dart';

/// 服务器地址配置页
///
/// App 首次启动时必须配置后端地址，保存后写入本地（SharedPreferences），
/// 之后所有请求都以该地址为基准。已配置过的情况下也可从「我的」进入修改。
class ServerConfigPage extends ConsumerStatefulWidget {
  const ServerConfigPage({super.key, this.embedded = false});

  /// 是否作为设置页内嵌表单使用（不显示全屏背景与标题栏）
  final bool embedded;

  @override
  ConsumerState<ServerConfigPage> createState() => _ServerConfigPageState();
}

class _ServerConfigPageState extends ConsumerState<ServerConfigPage> {
  late final TextEditingController _controller;
  String? _errorText;
  bool _testing = false;
  bool _saving = false;
  String? _testResult;

  @override
  void initState() {
    super.initState();
    final saved = ref.read(preferencesProvider).serverAddress;
    _controller = TextEditingController(text: saved);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  String get _normalizedInput => ServerAddress.normalize(_controller.text);

  /// 测试连通性：用临时 ApiClient 打一个公开接口（菜品分类列表）
  Future<void> _testConnection() async {
    FocusScope.of(context).unfocus();
    final error = ServerAddress.validate(_controller.text);
    if (error != null) {
      setState(() {
        _errorText = error;
        _testResult = null;
      });
      return;
    }
    setState(() {
      _testing = true;
      _testResult = null;
      _errorText = null;
    });

    final tempClient = ApiClient(
      ref.read(preferencesProvider),
      baseUrlOverride: ServerAddress.apiBase(_controller.text),
    );
    try {
      final api = CategoryApi(tempClient);
      await api.list();
      if (!mounted) return;
      setState(() => _testResult = '连接成功');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _testResult = e.message);
    } catch (e, s) {
      AppLogger.e('测试连接失败', error: e, stackTrace: s);
      if (!mounted) return;
      setState(() => _testResult = '连接失败，请检查地址与网络');
    } finally {
      tempClient.close();
      if (mounted) setState(() => _testing = false);
    }
  }

  /// 保存地址并进入登录流程
  Future<void> _save() async {
    FocusScope.of(context).unfocus();
    final error = ServerAddress.validate(_controller.text);
    if (error != null) {
      setState(() {
        _errorText = error;
        _testResult = null;
      });
      return;
    }
    setState(() {
      _saving = true;
      _errorText = null;
      _testResult = null;
    });
    try {
      await ref
          .read(appSettingsProvider.notifier)
          .setServerAddress(_controller.text);
      ref.read(sessionProvider.notifier).markConfigured();
      AppLogger.i('服务器地址已保存：$_normalizedInput');
      if (!mounted) return;
      AppToast.success(context, '服务器地址已保存');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final form = AppCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextFormField(
            controller: _controller,
            keyboardType: TextInputType.url,
            textInputAction: TextInputAction.done,
            decoration: InputDecoration(
              labelText: '服务器地址',
              hintText: '192.168.1.10:${AppConfig.defaultPortHint}',
              prefixIcon: Icon(Symbols.dns, size: 20),
              errorText: _errorText,
              helperText: '支持 http:// / https://，缺省协议时自动补 http://',
              helperMaxLines: 2,
            ),
            onChanged: (_) {
              if (_errorText != null) setState(() => _errorText = null);
            },
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 44,
                  child: OutlinedButton.icon(
                    onPressed: _testing ? null : _testConnection,
                    icon: _testing
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.wifi_tethering, size: 18),
                    label: const Text('测试连接'),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: PrimaryButton(
                  label: '保存并使用',
                  busy: _saving,
                  height: 44,
                  onPressed: _save,
                ),
              ),
            ],
          ),
          if (_testResult != null) ...[
            const SizedBox(height: 12),
            _TestResultBanner(text: _testResult!),
          ],
        ],
      ),
    );

    if (widget.embedded) return form;

    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 48),
              Center(
                child: SvgPicture.asset(
                  'assets/images/logo.svg',
                  width: 108,
                  height: 108,
                ),
              ),
              const SizedBox(height: 20),
              const Center(
                child: Text(
                  AppConfig.appName,
                  style: TextStyle(
                    fontSize: 21,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              const SizedBox(height: 6),
              const Center(
                child: Text(
                  '首次使用请先配置后端服务器地址',
                  style: TextStyle(fontSize: 13.5, color: AppColors.textHint),
                ),
              ),
              const SizedBox(height: 30),
              form,
              const SizedBox(height: 18),
              const _ConfigTips(),
            ],
          ),
        ),
      ),
    );
  }
}

/// 测试结果条
class _TestResultBanner extends StatelessWidget {
  const _TestResultBanner({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final success = text.contains('成功');
    final color = success ? AppColors.success : AppColors.danger;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          Icon(
            success ? Icons.check_circle_outline : Icons.error_outline,
            size: 18,
            color: color,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: TextStyle(fontSize: 13.5, color: color),
            ),
          ),
        ],
      ),
    );
  }
}

/// 配置提示
class _ConfigTips extends StatelessWidget {
  const _ConfigTips();

  @override
  Widget build(BuildContext context) {
    const tips = [
      '手机需要与服务器处于同一局域网，或服务器有公网域名',
      '地址填写 IP + 端口即可，例如 192.168.1.10:${AppConfig.defaultPortHint}',
      '配置完成后随时可在「我的 → 服务器设置」中修改',
    ];
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F8FA),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '配置说明',
            style: TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          for (final tip in tips)
            Padding(
              padding: const EdgeInsets.only(bottom: 5),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '· ',
                    style: TextStyle(color: AppColors.textHint),
                  ),
                  Expanded(
                    child: Text(
                      tip,
                      style: const TextStyle(
                        fontSize: 12.5,
                        height: 1.45,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
