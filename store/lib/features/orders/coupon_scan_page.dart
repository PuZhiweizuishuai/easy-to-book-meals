import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:permission_handler/permission_handler.dart';

import '../../core/theme/app_colors.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_feedback.dart';
import 'coupon_code_extractor.dart';

/// 当前平台是否支持摄像头扫码
///
/// mobile_scanner 只提供 Android / iOS / Web 的原生实现，
/// Windows / macOS / Linux 桌面端没有相机插件，统一切换为「手动输入券码」。
/// （使用 kIsWeb + defaultTargetPlatform 判断而非 dart:io，保证 Web 也能编译）
bool get couponScanSupported {
  if (kIsWeb) return true;
  return defaultTargetPlatform == TargetPlatform.android ||
      defaultTargetPlatform == TargetPlatform.iOS;
}

/// 优惠券核销页
///
/// 移动端：店员扫描顾客出示的券码二维码，解析出券码后返回给结算页，
/// 由结算页调用校验接口自动抵扣；同时保留「手动输入券码」兜底
/// （二维码打印不清或光线不足时使用）。
/// 桌面端：无摄像头能力，直接展示手动输入页，返回契约保持一致（pop 券码字符串）。
class CouponScanPage extends ConsumerStatefulWidget {
  const CouponScanPage({super.key, this.title = '扫描优惠券'});

  final String title;

  @override
  ConsumerState<CouponScanPage> createState() => _CouponScanPageState();
}

class _CouponScanPageState extends ConsumerState<CouponScanPage>
    with WidgetsBindingObserver {
  MobileScannerController? _controller;
  bool _handled = false;
  bool _torchOn = false;
  String? _permissionError;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // 桌面端不初始化相机，直接走手动输入页
    if (couponScanSupported) {
      _requestPermissionAndStart();
    }
  }

  Future<void> _requestPermissionAndStart() async {
    final status = await Permission.camera.request();
    if (!status.isGranted) {
      if (!mounted) return;
      setState(() => _permissionError = '需要相机权限才能扫描优惠券');
      return;
    }
    if (!mounted) return;
    setState(() {
      _controller = MobileScannerController(
        autoStart: true,
        detectionSpeed: DetectionSpeed.noDuplicates,
        formats: const <BarcodeFormat>[BarcodeFormat.qrCode],
      );
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    final controller = _controller;
    if (controller == null) return;
    if (state == AppLifecycleState.resumed) {
      controller.start();
    } else {
      controller.stop();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _controller?.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_handled) return;
    if (capture.barcodes.isEmpty) return;
    final raw = capture.barcodes.first.displayValue;
    if (raw == null || raw.isEmpty) return;
    _handled = true;
    final code = CouponCodeExtractor.extract(raw);
    // 关掉扫描器再返回，避免相机资源占用
    _controller?.stop();
    Navigator.of(context).pop<String>(code);
  }

  Future<void> _manualInput() async {
    final result = await showModalBottomSheet<String>(
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
                const Text(
                  '手动输入券码',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 12),
                _CouponCodeInputForm(
                  buttonLabel: '使用这张券',
                  onSubmit: (code) =>
                      Navigator.of(sheetContext).pop<String>(code),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    if (!mounted || result == null || result.isEmpty) return;
    Navigator.of(context).pop<String>(result);
  }

  Future<void> _toggleTorch() async {
    final controller = _controller;
    if (controller == null) return;
    try {
      await controller.toggleTorch();
      if (!mounted) return;
      setState(() => _torchOn = !_torchOn);
    } catch (_) {
      if (!mounted) return;
      AppToast.warning(context, '该设备不支持闪光灯');
    }
  }

  @override
  Widget build(BuildContext context) {
    // 桌面端：手动输入券码页
    if (!couponScanSupported) {
      return _DesktopCouponPage(title: widget.title);
    }

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: Text(widget.title),
        actions: [
          if (_controller != null)
            IconButton(
              icon: Icon(_torchOn ? Icons.flashlight_on : Icons.flashlight_off),
              onPressed: _toggleTorch,
            ),
        ],
      ),
      body: Column(
        children: [
          Expanded(child: _buildScanner()),
          _buildFooter(),
        ],
      ),
    );
  }

  Widget _buildScanner() {
    if (_permissionError != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.no_photography_outlined,
                  size: 56, color: Colors.white54),
              const SizedBox(height: 14),
              Text(
                _permissionError!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white70, fontSize: 14),
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: openAppSettings,
                icon: const Icon(Icons.settings_outlined, size: 18),
                label: const Text('去系统设置开启'),
              ),
            ],
          ),
        ),
      );
    }
    final controller = _controller;
    if (controller == null) {
      return const Center(
        child: CircularProgressIndicator(color: Colors.white),
      );
    }
    return MobileScanner(
      controller: controller,
      onDetect: _onDetect,
      errorBuilder: (context, error) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            '相机启动失败：${error.errorDetails?.message ?? error.errorCode.name}',
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.white70),
          ),
        ),
      ),
    );
  }

  Widget _buildFooter() {
    return SafeArea(
      top: false,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.fromLTRB(20, 14, 20, 18),
        color: const Color(0xFF1B1B1B),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Symbols.qr_code_scanner, size: 18, color: Colors.white70),
                const SizedBox(width: 8),
                const Text(
                  '对准顾客出示的优惠券二维码',
                  style: TextStyle(fontSize: 13, color: Colors.white70),
                ),
              ],
            ),
            const SizedBox(height: 12),
            SecondaryButton(
              label: '手动输入券码',
              icon: Icons.keyboard_outlined,
              color: Colors.white,
              height: 42,
              onPressed: _manualInput,
            ),
          ],
        ),
      ),
    );
  }
}

/// 桌面端优惠券手动输入页（无摄像头能力时的替代方案）
class _DesktopCouponPage extends StatelessWidget {
  const _DesktopCouponPage({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Icon(Symbols.confirmation_number,
                      size: 36, color: AppColors.primary),
                ),
                const SizedBox(height: 18),
                const Text(
                  '手动输入优惠券券码',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                const Text(
                  '桌面端不支持摄像头扫码，请输入或粘贴顾客出示的优惠券券码'
                  '（支持 32 位券码或领券链接）',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 13,
                    color: AppColors.textSecondary,
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 22),
                _CouponCodeInputForm(
                  buttonLabel: '校验并使用',
                  onSubmit: (code) =>
                      Navigator.of(context).pop<String>(code),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// 券码输入表单（移动端底部弹层与桌面端页面共用）
class _CouponCodeInputForm extends StatefulWidget {
  const _CouponCodeInputForm({
    required this.onSubmit,
    this.buttonLabel = '校验并使用',
  });

  /// 提交券码回调（调用方负责关闭页面/弹层）
  final ValueChanged<String> onSubmit;

  final String buttonLabel;

  @override
  State<_CouponCodeInputForm> createState() => _CouponCodeInputFormState();
}

class _CouponCodeInputFormState extends State<_CouponCodeInputForm> {
  final TextEditingController _controller = TextEditingController();
  String? _error;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() {
    final value = _controller.text.trim();
    if (value.isEmpty) {
      setState(() => _error = '请输入券码');
      return;
    }
    final code = CouponCodeExtractor.extract(value);
    if (code == null || code.isEmpty) {
      setState(() => _error = '券码格式不正确');
      return;
    }
    widget.onSubmit(code);
  }

  /// 粘贴剪贴板内容：可能是完整领券链接，统一过一遍解析器提取券码
  Future<void> _pasteFromClipboard() async {
    final data = await Clipboard.getData(Clipboard.kTextPlain);
    final raw = data?.text?.trim();
    if (raw == null || raw.isEmpty) return;
    final code = CouponCodeExtractor.extract(raw);
    if (code != null && code.isNotEmpty) {
      _controller.text = code;
      if (_error != null) setState(() => _error = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _controller,
          autofocus: true,
          textInputAction: TextInputAction.done,
          onSubmitted: (_) => _submit(),
          onChanged: (_) {
            if (_error != null) setState(() => _error = null);
          },
          decoration: InputDecoration(
            labelText: '券码',
            hintText: '32 位券码或领券链接',
            prefixIcon: const Icon(Symbols.confirmation_number, size: 20),
            suffixIcon: IconButton(
              icon: const Icon(Icons.content_paste, size: 20),
              tooltip: '粘贴',
              onPressed: _pasteFromClipboard,
            ),
            errorText: _error,
          ),
        ),
        const SizedBox(height: 14),
        PrimaryButton(
          label: widget.buttonLabel,
          onPressed: _submit,
        ),
      ],
    );
  }
}
