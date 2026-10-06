import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/time_format.dart';
import '../../data/models/models.dart';
import '../../providers/alert_provider.dart';
import '../../providers/core_providers.dart';
import '../../providers/session_provider.dart';
import '../../providers/store_context_provider.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/state_views.dart';
import 'widgets/kitchen_item_card.dart';

/// 后厨出餐管理（KDS）
///
/// 以「菜品」为粒度展示待制作队列：
/// - 按等待时长升序，我认领的排在最前
/// - 卡片支持「认领制作 / 释放 / 出餐」
/// - 页面内定时刷新（20s），配合全局轮询实现新菜品声音与通知提醒
class KitchenPage extends ConsumerStatefulWidget {
  const KitchenPage({super.key});

  @override
  ConsumerState<KitchenPage> createState() => _KitchenPageState();
}

class _KitchenPageState extends ConsumerState<KitchenPage> {
  List<KitchenQueueItem> _items = const [];
  int _maxWaitedMinutes = 0;
  bool _loading = true;
  String? _error;
  bool _onlyMine = false;
  final Set<int> _busyItems = <int>{};
  Timer? _refreshTimer;

  /// 保证切换筛选/刷新时不会并发覆盖数据
  int _requestToken = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(alertProvider.notifier).clearBadge(AlertKind.newKitchenItem);
      _load();
      _startAutoRefresh();
    });
  }

  void _startAutoRefresh() {
    _refreshTimer?.cancel();
    _refreshTimer = Timer.periodic(
      const Duration(seconds: 20),
      (_) => _load(silent: true),
    );
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    super.dispose();
  }

  int? get _storeId => ref.read(currentStoreIdProvider);

  Future<void> _load({bool silent = false}) async {
    final storeId = _storeId;
    if (storeId == null) return;
    final token = ++_requestToken;
    if (!silent) {
      if (mounted) {
        setState(() {
          _loading = true;
          _error = null;
        });
      }
    }
    try {
      final result = await ref.read(orderApiProvider).kitchenQueue(
            storeId: storeId,
          );
      if (!mounted || token != _requestToken) return;
      setState(() {
        _items = result.items;
        _maxWaitedMinutes = result.maxWaitedMinutes;
        _loading = false;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted || token != _requestToken) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      if (_items.isEmpty) {
        setState(() {
          _loading = false;
          _error = e.message;
        });
      }
    } catch (_) {
      if (!mounted || token != _requestToken) return;
      if (_items.isEmpty) {
        setState(() {
          _loading = false;
          _error = '加载失败，请稍后重试';
        });
      }
    }
  }

  Future<void> _runItem(
    KitchenQueueItem item,
    Future<OrderData?> Function(int itemId, int storeId) action,
  ) async {
    final storeId = _storeId;
    final itemId = item.itemId;
    if (storeId == null || itemId == null) return;
    setState(() => _busyItems.add(itemId));
    try {
      await action(itemId, storeId);
      if (!mounted) return;
      await _load(silent: true);
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '操作失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _busyItems.remove(itemId));
    }
  }

  List<KitchenQueueItem> get _visibleItems => _onlyMine
      ? _items.where((i) => i.myPreparing).toList(growable: false)
      : _items;

  @override
  Widget build(BuildContext context) {
    final storeId = ref.watch(currentStoreIdProvider);
    final visible = _visibleItems;

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        titleSpacing: 14,
        title: Row(
          children: [
            Icon(Symbols.chef_hat, size: 20, color: AppColors.primary),
            const SizedBox(width: 8),
            const Text('后厨出餐'),
            const SizedBox(width: 8),
            if (_items.isNotEmpty)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                decoration: BoxDecoration(
                  color: AppColors.primaryLight,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  '${_items.length} 道',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: AppColors.primary,
                  ),
                ),
              ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: '刷新',
            icon: const Icon(Icons.refresh_rounded, size: 22),
            onPressed: () => _load(),
          ),
        ],
      ),
      body: storeId == null
          ? const EmptyView(title: '请先选择门店')
          : Column(
              children: [
                _KitchenHeader(
                  itemCount: _items.length,
                  maxWaitedMinutes: _maxWaitedMinutes,
                  onlyMine: _onlyMine,
                  onToggleMine: () =>
                      setState(() => _onlyMine = !_onlyMine),
                ),
                Expanded(
                  child: _buildBody(visible),
                ),
              ],
            ),
    );
  }

  Widget _buildBody(List<KitchenQueueItem> visible) {
    if (_loading && _items.isEmpty) return const LoadingView();
    if (_error != null && _items.isEmpty) {
      return ErrorView(message: _error!, onRetry: () => _load());
    }
    if (visible.isEmpty) {
      return EmptyView(
        title: _onlyMine ? '你还没有认领的菜品' : '当前没有待制作菜品',
        subtitle: _onlyMine ? '点击卡片上的「认领制作」开始处理' : '有新订单时会自动提醒',
        compact: false,
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: GridView.builder(
        padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 10,
          crossAxisSpacing: 10,
          childAspectRatio: 0.92,
        ),
        itemCount: visible.length,
        itemBuilder: (context, index) {
          final item = visible[index];
          return KitchenItemCard(
            item: item,
            busy: _busyItems.contains(item.itemId),
            onServe: () => _runItem(
              item,
              (itemId, storeId) => ref
                  .read(orderApiProvider)
                  .serveItem(itemId: itemId, storeId: storeId),
            ),
            onStart: () => _runItem(
              item,
              (itemId, storeId) => ref
                  .read(orderApiProvider)
                  .startPreparing(itemId: itemId, storeId: storeId),
            ),
            onRelease: () => _runItem(
              item,
              (itemId, storeId) => ref
                  .read(orderApiProvider)
                  .releasePreparing(itemId: itemId, storeId: storeId),
            ),
          );
        },
      ),
    );
  }
}

/// 后厨页顶部统计条
class _KitchenHeader extends StatelessWidget {
  const _KitchenHeader({
    required this.itemCount,
    required this.maxWaitedMinutes,
    required this.onlyMine,
    required this.onToggleMine,
  });

  final int itemCount;
  final int maxWaitedMinutes;
  final bool onlyMine;
  final VoidCallback onToggleMine;

  @override
  Widget build(BuildContext context) {
    final urgent = maxWaitedMinutes >= 15;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(bottom: BorderSide(color: AppColors.divider)),
      ),
      child: Row(
        children: [
          Icon(Symbols.timer,
              size: 17,
              color: urgent ? AppColors.danger : AppColors.textSecondary),
          const SizedBox(width: 6),
          Text(
            '最长等待 ${TimeFormat.waited(maxWaitedMinutes)}',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: urgent ? AppColors.danger : AppColors.textSecondary,
            ),
          ),
          const Spacer(),
          GestureDetector(
            onTap: onToggleMine,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              decoration: BoxDecoration(
                color: onlyMine ? AppColors.primaryLight : const Color(0xFFF2F4F8),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Text(
                '只看我的',
                style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: onlyMine ? AppColors.primary : AppColors.textSecondary,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
