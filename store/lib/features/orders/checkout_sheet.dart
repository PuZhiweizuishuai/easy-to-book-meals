import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../providers/session_provider.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_feedback.dart';
import 'coupon_scan_page.dart';

/// 结算面板
///
/// 结算金额构成：菜品折后合计 − 优惠券减免 − 手动减免 = 应付金额。
/// 移动端点击「扫码核销优惠券」扫描顾客出示的券码二维码；桌面端不支持相机，
/// 入口自动切换为「输入券码核销优惠券」。校验通过后自动把减免金额计入本单
/// （真正核销发生在点击「确认结算」时）。
class CheckoutSheet extends ConsumerStatefulWidget {
  const CheckoutSheet({
    super.key,
    required this.order,
    required this.storeId,
  });

  /// 当前订单（已含最新明细）
  final OrderData order;

  /// 门店 ID
  final int storeId;

  /// 弹出结算面板，返回 true 表示结算成功
  static Future<bool?> show(
    BuildContext context, {
    required OrderData order,
    required int storeId,
  }) =>
      showModalBottomSheet<bool?>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.white,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        builder: (_) => CheckoutSheet(order: order, storeId: storeId),
      );

  @override
  ConsumerState<CheckoutSheet> createState() => _CheckoutSheetState();
}

class _CheckoutSheetState extends ConsumerState<CheckoutSheet> {
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _manualController = TextEditingController();

  CouponCheckResult? _coupon;
  String? _couponError;
  bool _checkingCoupon = false;
  bool _submitting = false;
  Membership? _member;
  bool _loadingMember = false;

  @override
  void initState() {
    super.initState();
    final phone = widget.order.contactPhone;
    if (phone != null && phone.isNotEmpty) {
      _phoneController.text = phone;
      _lookupMember(phone);
    }
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _manualController.dispose();
    super.dispose();
  }

  /// 菜品折后合计（优惠券试算的基准金额）
  double get _baseAmount => widget.order.basePayable;

  double get _couponDiscount => _coupon?.discount ?? 0;

  double get _manualDiscount {
    final value = double.tryParse(_manualController.text.trim());
    if (value == null || value < 0) return 0;
    return value;
  }

  /// 应付金额（不允许为负）
  double get _payable {
    final value = _baseAmount - _couponDiscount - _manualDiscount;
    return value < 0 ? 0 : value;
  }

  /// 扫码核销优惠券
  Future<void> _scanCoupon() async {
    FocusScope.of(context).unfocus();
    final code = await Navigator.of(context).push<String>(
      MaterialPageRoute<String>(builder: (_) => const CouponScanPage()),
    );
    if (!mounted || code == null || code.isEmpty) return;
    await _checkCoupon(code);
  }

  /// 校验券码在当前金额下是否可用
  Future<void> _checkCoupon(String code) async {
    setState(() {
      _checkingCoupon = true;
      _couponError = null;
    });
    try {
      final result = await ref.read(couponApiProvider).check(
            storeId: widget.storeId,
            code: code,
            amount: _baseAmount,
          );
      if (!mounted) return;
      setState(() {
        _coupon = result;
        _checkingCoupon = false;
      });
      if (result.discount <= 0) {
        AppToast.warning(context, '该券在当前金额下不可用');
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      setState(() {
        _coupon = null;
        _couponError = e.message;
        _checkingCoupon = false;
      });
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _coupon = null;
        _couponError = '校验失败，请重试';
        _checkingCoupon = false;
      });
    }
  }

  /// 手机号 → 会员识别（用于积分累积）
  Future<void> _lookupMember(String phone) async {
    if (phone.length != 11) return;
    setState(() => _loadingMember = true);
    try {
      final member = await ref
          .read(membershipApiProvider)
          .byPhone(storeId: widget.storeId, phone: phone);
      if (!mounted) return;
      setState(() {
        _member = member;
        _loadingMember = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loadingMember = false);
    }
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    final phone = _phoneController.text.trim();
    if (phone.isNotEmpty && !RegExp(r'^1[3-9]\d{9}$').hasMatch(phone)) {
      AppToast.error(context, '手机号格式不正确');
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref.read(orderApiProvider).complete(
            orderId: widget.order.id!,
            storeId: widget.storeId,
            phone: phone.isEmpty ? null : phone,
            couponCode: _coupon?.code,
            manualDiscount: _manualDiscount > 0 ? _manualDiscount : null,
          );
      if (!mounted) return;
      // 结算完成后看板「今日营业额」必须同步，通知常驻数据页重新拉取
      ref.read(dataRefreshProvider.notifier).bump();
      AppToast.success(context, '结算完成');
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
      setState(() => _submitting = false);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '结算失败，请稍后重试');
      setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return SafeArea(
      top: false,
      child: SingleChildScrollView(
        child: Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 14,
            bottom: 16 + bottomInset,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      '订单结算',
                      style: TextStyle(
                        fontSize: 16.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, size: 20),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const Divider(height: 1),
              const SizedBox(height: 14),

              // 金额构成
              _AmountRow(label: '菜品合计', value: _baseAmount),
              const SizedBox(height: 8),
              if (_couponDiscount > 0)
                _AmountRow(
                  label: '优惠券减免（${_coupon?.name ?? ''}）',
                  value: -_couponDiscount,
                  color: AppColors.success,
                ),
              const SizedBox(height: 10),

              // 优惠券行
              _CouponSection(
                coupon: _coupon,
                error: _couponError,
                checking: _checkingCoupon,
                onScan: _scanCoupon,
                onRemove: () => setState(() {
                  _coupon = null;
                  _couponError = null;
                }),
              ),
              const SizedBox(height: 12),

              // 手动减免
              TextField(
                controller: _manualController,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: const InputDecoration(
                  labelText: '手动减免（抹零 / 议价）',
                  prefixIcon: Icon(Icons.price_change_outlined, size: 20),
                ),
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 12),

              // 手机号 / 会员
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                maxLength: 11,
                decoration: InputDecoration(
                  labelText: '顾客手机号（可空，用于积分）',
                  prefixIcon: const Icon(Icons.phone_outlined, size: 20),
                  suffix: _loadingMember
                      ? const SizedBox(
                          width: 14,
                          height: 14,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : null,
                  counterText: '',
                ),
                onChanged: (value) {
                  if (value.length == 11) {
                    _lookupMember(value.trim());
                  } else if (_member != null) {
                    setState(() => _member = null);
                  }
                },
              ),
              if (_member != null)
                Padding(
                  padding: const EdgeInsets.only(top: 6),
                  child: Row(
                    children: [
                      Icon(Symbols.groups,
                          size: 15, color: AppColors.primary),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          '会员：${_member!.displayName} · 当前 ${_member!.points} 积分',
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.primary,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              const SizedBox(height: 16),

              // 应付金额
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  color: AppColors.primaryLight,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    const Text(
                      '应付金额',
                      style: TextStyle(
                        fontSize: 14,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const Spacer(),
                    Text(
                      MoneyFormat.yuan(_payable),
                      style: const TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              PrimaryButton(
                label: '确认结算',
                busy: _submitting,
                onPressed: _submitting ? null : _submit,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 金额行
class _AmountRow extends StatelessWidget {
  const _AmountRow({required this.label, required this.value, this.color});

  final String label;
  final double value;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary),
        ),
        const Spacer(),
        Text(
          MoneyFormat.yuan(value),
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: color ?? AppColors.textPrimary,
          ),
        ),
      ],
    );
  }
}

/// 优惠券区块（未使用 / 已校验 / 校验失败 三种形态）
class _CouponSection extends StatelessWidget {
  const _CouponSection({
    this.coupon,
    this.error,
    required this.checking,
    required this.onScan,
    required this.onRemove,
  });

  final CouponCheckResult? coupon;
  final String? error;
  final bool checking;
  final VoidCallback onScan;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    if (coupon != null && coupon!.discount > 0) {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: AppColors.successLight,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            Icon(Symbols.confirmation_number,
                size: 22, color: AppColors.success),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    coupon!.name ?? '优惠券',
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: AppColors.success,
                    ),
                  ),
                  Text(
                    '券码 ${coupon!.code ?? ''} · 减 ${MoneyFormat.plain(coupon!.discount)}',
                    style: const TextStyle(
                      fontSize: 11.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            IconButton(
              icon: const Icon(Icons.close, size: 18),
              onPressed: onRemove,
            ),
          ],
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        OutlinedButton.icon(
          style: OutlinedButton.styleFrom(
            minimumSize: const Size.fromHeight(44),
            foregroundColor: AppColors.primary,
            side: const BorderSide(color: AppColors.primary),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          icon: checking
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Icon(
                  couponScanSupported
                      ? Symbols.qr_code_scanner
                      : Symbols.keyboard,
                  size: 18,
                ),
          label: Text(couponScanSupported ? '扫码核销优惠券' : '输入券码核销优惠券'),
          onPressed: checking ? null : onScan,
        ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(top: 6, left: 4),
            child: Text(
              error!,
              style: const TextStyle(fontSize: 12, color: AppColors.danger),
            ),
          ),
      ],
    );
  }
}
