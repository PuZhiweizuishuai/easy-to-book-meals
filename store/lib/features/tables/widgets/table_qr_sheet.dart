import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/app_button.dart';
import '../../../shared/widgets/app_card.dart';
import '../../../shared/widgets/app_feedback.dart';

/// 餐桌扫码点餐二维码查看弹层
///
/// 二维码内容与顾客端 Web 一致：`{服务器地址}/store/{门店ID}?table={qrCodeKey}`，
/// 顾客扫码后进入门店菜单页并自动绑定本桌。这里只做展示与复制，
/// 真正的图片由 [QrImageView] 本地生成，无需后端接口。
class TableQrSheet extends StatelessWidget {
  const TableQrSheet({
    super.key,
    required this.tableNumber,
    required this.scanUrl,
    this.subtitle,
    this.qrCodeKey,
  });

  final String tableNumber;
  final String? subtitle;

  /// 完整的扫码点餐链接；为空表示该桌尚未生成二维码标识
  final String? scanUrl;

  /// 二维码原始标识（qrCodeKey），便于人工核对/复制
  final String? qrCodeKey;

  /// 弹出二维码查看层
  static Future<void> show(
    BuildContext context, {
    required String tableNumber,
    required String? scanUrl,
    String? subtitle,
    String? qrCodeKey,
  }) {
    return showAppBottomSheet<void>(
      context: context,
      title: '餐桌二维码',
      child: TableQrSheet(
        tableNumber: tableNumber,
        scanUrl: scanUrl,
        subtitle: subtitle,
        qrCodeKey: qrCodeKey,
      ),
    );
  }

  Future<void> _copy(BuildContext context, String text, String label) async {
    await Clipboard.setData(ClipboardData(text: text));
    if (!context.mounted) return;
    AppToast.success(context, '已复制$label');
  }

  @override
  Widget build(BuildContext context) {
    final url = scanUrl;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          '$tableNumber 号桌',
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        if (subtitle != null && subtitle!.isNotEmpty) ...[
          const SizedBox(height: 4),
          Text(
            subtitle!,
            style: const TextStyle(fontSize: 12.5, color: AppColors.textHint),
          ),
        ],
        const SizedBox(height: 18),
        if (url == null || url.isEmpty)
          _MissingQr()
        else ...[
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.divider),
            ),
            child: QrImageView(
              data: url,
              version: QrVersions.auto,
              size: 220,
              backgroundColor: Colors.white,
              eyeStyle: const QrEyeStyle(
                color: Colors.black,
                eyeShape: QrEyeShape.square,
              ),
              dataModuleStyle: const QrDataModuleStyle(
                color: Colors.black,
                dataModuleShape: QrDataModuleShape.square,
              ),
            ),
          ),
          const SizedBox(height: 14),
          const Text(
            '顾客扫码后进入本店菜单，下单将自动绑定此餐桌',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
          ),
          const SizedBox(height: 14),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(
              color: const Color(0xFFF2F4F8),
              borderRadius: BorderRadius.circular(10),
            ),
            child: SelectableText(
              url,
              style: const TextStyle(
                fontSize: 11.5,
                color: AppColors.textSecondary,
                fontFamily: 'monospace',
              ),
            ),
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              if (qrCodeKey != null && qrCodeKey!.isNotEmpty) ...[
                Expanded(
                  child: SecondaryButton(
                    label: '复制桌码',
                    expand: true,
                    onPressed: () => _copy(context, qrCodeKey!, '桌码标识'),
                  ),
                ),
                const SizedBox(width: 10),
              ],
              Expanded(
                child: PrimaryButton(
                  label: '复制链接',
                  onPressed: () => _copy(context, url, '点餐链接'),
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

/// 尚未生成二维码标识时的占位
class _MissingQr extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Icon(
          Icons.qr_code_2,
          size: 56,
          color: AppColors.disabled,
        ),
        const SizedBox(height: 12),
        const Text(
          '该餐桌尚未生成扫码标识',
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 6),
        Text(
          '请先在后台重新生成桌码，或刷新后重试',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
        ),
      ],
    );
  }
}
