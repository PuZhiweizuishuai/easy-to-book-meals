import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/money_format.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_card.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/paged_list_view.dart';
import '../../shared/widgets/state_views.dart';

/// 会员详情页
///
/// 展示会员积分、用餐次数，并支持：
/// - 修改门店级昵称（只影响本店，不污染顾客账号昵称）
/// - 手动调整积分（可正可负，带备注）
/// - 查看积分流水与消费记录
/// - 一键拨号（url_launcher）
class MemberDetailPage extends ConsumerStatefulWidget {
  const MemberDetailPage({
    super.key,
    required this.memberId,
    required this.storeId,
  });

  final int memberId;
  final int storeId;

  @override
  ConsumerState<MemberDetailPage> createState() => _MemberDetailPageState();
}

class _MemberDetailPageState extends ConsumerState<MemberDetailPage> {
  Membership? _member;
  bool _loading = true;
  String? _error;
  bool _changed = false;

  final PagedListController<PointsTransaction> _txController =
      PagedListController<PointsTransaction>();
  final PagedListController<OrderData> _orderController =
      PagedListController<OrderData>();

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final member = await ref.read(membershipApiProvider).detail(
            id: widget.memberId,
            storeId: widget.storeId,
          );
      if (!mounted) return;
      setState(() {
        _member = member;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.message;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = '加载失败，请稍后重试';
        _loading = false;
      });
    }
  }

  /// 修改门店级昵称
  Future<void> _editNickname() async {
    final member = _member;
    if (member == null) return;
    final controller = TextEditingController(text: member.nickname ?? '');
    final result = await showAppBottomSheet<String>(
      context: context,
      title: '修改会员昵称',
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: controller,
            autofocus: true,
            decoration: const InputDecoration(
              labelText: '昵称',
              helperText: '仅在本店生效，不影响顾客账号昵称',
              helperMaxLines: 2,
            ),
          ),
          const SizedBox(height: 18),
          PrimaryButton(
            label: '保存',
            onPressed: () => Navigator.of(context).pop(controller.text.trim()),
          ),
        ],
      ),
    );
    final value = controller.text.trim();
    controller.dispose();
    if (!mounted || result == null) return;

    try {
      final updated = await ref.read(membershipApiProvider).updateNickname(
            storeId: widget.storeId,
            membershipId: widget.memberId,
            nickname: value.isEmpty ? null : value,
          );
      if (!mounted) return;
      setState(() {
        _member = updated ?? member;
        _changed = true;
      });
      AppToast.success(context, '昵称已更新');
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    }
  }

  /// 手动调整积分
  Future<void> _adjustPoints() async {
    final pointsController = TextEditingController();
    final remarkController = TextEditingController();

    final result = await showAppBottomSheet<bool>(
      context: context,
      title: '调整积分',
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: pointsController,
            autofocus: true,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: '积分变动（正数为增加，负数为扣减）',
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: remarkController,
            decoration: const InputDecoration(labelText: '备注'),
          ),
          const SizedBox(height: 18),
          PrimaryButton(
            label: '确认调整',
            onPressed: () {
              if (int.tryParse(pointsController.text.trim()) == null) {
                AppToast.error(context, '请输入整数积分');
                return;
              }
              Navigator.of(context).pop(true);
            },
          ),
        ],
      ),
    );
    final points = int.tryParse(pointsController.text.trim());
    final remark = remarkController.text.trim();
    pointsController.dispose();
    remarkController.dispose();

    if (!mounted || result != true || points == null) return;
    try {
      final updated = await ref.read(membershipApiProvider).adjustPoints(
            storeId: widget.storeId,
            membershipId: widget.memberId,
            points: points,
            remark: remark.isEmpty ? null : remark,
          );
      if (!mounted) return;
      setState(() {
        _member = updated ?? _member;
        _changed = true;
      });
      AppToast.success(context, '积分已调整');
      _txController.refresh();
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    }
  }

  Future<void> _dial() async {
    final phone = _member?.phone;
    if (phone == null || phone.isEmpty) return;
    final uri = Uri(scheme: 'tel', path: phone);
    if (!await canLaunchUrl(uri)) {
      if (!mounted) return;
      AppToast.warning(context, '当前设备不支持拨号');
      return;
    }
    await launchUrl(uri);
  }

  void _pop() => Navigator.of(context).pop(_changed);

  @override
  Widget build(BuildContext context) {
    final member = _member;
    return PopScope<Object?>(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        _pop();
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('会员详情'),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh_rounded, size: 22),
              onPressed: _load,
            ),
          ],
        ),
        body: _loading
            ? const LoadingView()
            : _error != null
                ? ErrorView(message: _error!, onRetry: _load)
                : member == null
                    ? const EmptyView(title: '会员不存在')
                    : _buildBody(member),
      ),
    );
  }

  Widget _buildBody(Membership member) {
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          _MemberHeader(
            member: member,
            onEditNickname: _editNickname,
            onAdjustPoints: _adjustPoints,
            onDial: _dial,
          ),
          const TabBar(
            tabs: [
              Tab(text: '积分流水'),
              Tab(text: '消费记录'),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                // 积分流水
                PagedListView<PointsTransaction>(
                  controller: _txController,
                  fetcher: (page) =>
                      ref.read(membershipApiProvider).transactions(
                            id: widget.memberId,
                            storeId: widget.storeId,
                            page: page,
                            size: 20,
                          ),
                  emptyTitle: '暂无积分流水',
                  itemBuilder: (context, tx, index) =>
                      _TransactionTile(tx: tx),
                ),
                // 消费记录
                PagedListView<OrderData>(
                  controller: _orderController,
                  fetcher: (page) => ref.read(membershipApiProvider).orders(
                        id: widget.memberId,
                        storeId: widget.storeId,
                        page: page,
                        size: 20,
                      ),
                  emptyTitle: '暂无消费记录',
                  itemBuilder: (context, order, index) => _OrderTile(
                    order: order,
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

/// 会员信息头部
class _MemberHeader extends StatelessWidget {
  const _MemberHeader({
    required this.member,
    required this.onEditNickname,
    required this.onAdjustPoints,
    required this.onDial,
  });

  final Membership member;
  final VoidCallback onEditNickname;
  final VoidCallback onAdjustPoints;
  final VoidCallback onDial;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(bottom: BorderSide(color: AppColors.divider)),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Icon(Symbols.account_circle,
                  size: 46, color: AppColors.primary),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            member.displayName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        GestureDetector(
                          onTap: onEditNickname,
                          child: const Icon(Icons.edit_outlined,
                              size: 15, color: AppColors.textHint),
                        ),
                      ],
                    ),
                    const SizedBox(height: 3),
                    Row(
                      children: [
                        Text(
                          member.phone ?? '未知手机号',
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        if (member.phone?.isNotEmpty == true) ...[
                          const SizedBox(width: 6),
                          GestureDetector(
                            onTap: onDial,
                            child: const Icon(Icons.phone_outlined,
                                size: 14, color: AppColors.primary),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _MetricBox(
                  icon: Symbols.star,
                  label: '积分',
                  value: '${member.points}',
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _MetricBox(
                  icon: Symbols.receipt_long,
                  label: '用餐次数',
                  value: '${member.orderCount}',
                  color: AppColors.info,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: GestureDetector(
                  onTap: onAdjustPoints,
                  child: _MetricBox(
                    icon: Symbols.add_card,
                    label: '调整积分',
                    value: '去调整',
                    color: AppColors.success,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MetricBox extends StatelessWidget {
  const _MetricBox({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  final IconData icon;
  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.07),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        children: [
          Icon(icon, size: 17, color: color),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: color,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: const TextStyle(fontSize: 11, color: AppColors.textHint),
          ),
        ],
      ),
    );
  }
}

/// 积分流水条目
class _TransactionTile extends StatelessWidget {
  const _TransactionTile({required this.tx});

  final PointsTransaction tx;

  @override
  Widget build(BuildContext context) {
    final increase = tx.isIncrease;
    return AppCard(
      child: Row(
        children: [
          Icon(
            increase ? Icons.arrow_upward : Icons.arrow_downward,
            size: 17,
            color: increase ? AppColors.success : AppColors.danger,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  tx.type ?? (increase ? '积分增加' : '积分扣减'),
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                if (tx.remark?.isNotEmpty == true)
                  Text(
                    tx.remark!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textHint,
                    ),
                  ),
                if (tx.orderNo?.isNotEmpty == true)
                  Text(
                    '订单 ${tx.orderNo}',
                    style: const TextStyle(
                      fontSize: 11.5,
                      color: AppColors.textHint,
                    ),
                  ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '${increase ? '+' : ''}${tx.points ?? 0}',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  color: increase ? AppColors.success : AppColors.danger,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                TimeFormat.smart(tx.createTime),
                style: const TextStyle(
                  fontSize: 11,
                  color: AppColors.textHint,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// 会员消费记录条目
class _OrderTile extends StatelessWidget {
  const _OrderTile({required this.order});

  final OrderData order;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  order.orderNo ?? '',
                  style: const TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  '${TimeFormat.smart(order.createTime)} · ${order.typeEnum.label}${order.tableNumber?.isNotEmpty == true ? ' · ${order.tableNumber}桌' : ''}',
                  style: const TextStyle(
                    fontSize: 11.5,
                    color: AppColors.textHint,
                  ),
                ),
              ],
            ),
          ),
          Text(
            MoneyFormat.yuan(order.payAmount ?? 0),
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w700,
              color: AppColors.primary,
            ),
          ),
        ],
      ),
    );
  }
}
