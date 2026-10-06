import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import 'state_views.dart';

/// 分页数据加载器：接收页码，返回该页结果
typedef PageFetcher<T> = Future<PageResult<T>> Function(int page);

/// 分页列表的手动刷新句柄
///
/// 页面持有它并传给 [PagedListView]，即可在任何时候调用 [refresh] 重新加载首页数据。
class PagedListController<T> {
  VoidCallback? _refresh;

  /// 重新加载第一页
  void refresh() => _refresh?.call();
}

/// 通用分页列表
///
/// 封装了以下能力，避免每个列表页重复实现：
/// - 首次加载 / 下拉刷新 / 触底加载更多 / 加载更多失败重试
/// - 空状态、错误状态统一展示
/// - [reloadKey] 变化时自动重新加载（用于筛选条件切换）
class PagedListView<T> extends StatefulWidget {
  const PagedListView({
    super.key,
    required this.fetcher,
    required this.itemBuilder,
    this.controller,
    this.reloadKey,
    this.emptyTitle = '暂无数据',
    this.emptySubtitle,
    this.padding = const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
    this.itemPadding,
    this.shrinkWrap = false,
    this.enableLoadMore = true,
  });

  /// 数据加载回调（页码从 1 开始）
  final PageFetcher<T> fetcher;

  /// 单项构建器
  final Widget Function(BuildContext context, T item, int index) itemBuilder;

  /// 外部持有的刷新句柄
  final PagedListController<T>? controller;

  /// 该值变化时自动重新加载（例如筛选状态、搜索词）
  final Object? reloadKey;

  final String emptyTitle;
  final String? emptySubtitle;

  /// 列表整体内边距
  final EdgeInsetsGeometry padding;

  /// 单项之间的间距
  final EdgeInsetsGeometry? itemPadding;

  /// 是否由父容器的高度决定（用于嵌套滚动场景）
  final bool shrinkWrap;

  final bool enableLoadMore;

  @override
  State<PagedListView<T>> createState() => _PagedListViewState<T>();
}

class _PagedListViewState<T> extends State<PagedListView<T>> {
  final List<T> _items = <T>[];
  final ScrollController _scrollController = ScrollController();

  int _page = 0;
  int _totalPages = 1;
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    widget.controller?._refresh = _refresh;
    _scrollController.addListener(_onScroll);
    _load(reset: true);
  }

  @override
  void didUpdateWidget(covariant PagedListView<T> oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.reloadKey != widget.reloadKey) {
      _load(reset: true);
      return;
    }
    if (oldWidget.controller != widget.controller && widget.controller != null) {
      widget.controller!._refresh = _refresh;
    }
  }

  @override
  void dispose() {
    widget.controller?._refresh = null;
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!widget.enableLoadMore || _loadingMore || _loading) return;
    if (!_hasMore) return;
    final position = _scrollController.position;
    // 距底部 320 像素时提前加载下一页
    if (position.extentAfter < 320) {
      _load(reset: false);
    }
  }

  bool get _hasMore => _page < _totalPages;

  Future<void> _refresh() => _load(reset: true);

  Future<void> _load({required bool reset}) async {
    if (reset) {
      setState(() {
        _loading = true;
        _error = null;
        if (_items.isNotEmpty) _items.clear();
        _page = 0;
        _totalPages = 1;
      });
    } else {
      setState(() => _loadingMore = true);
    }

    final targetPage = reset ? 1 : _page + 1;
    try {
      final result = await widget.fetcher(targetPage);
      if (!mounted) return;
      setState(() {
        _page = result.page;
        _totalPages = result.totalPages;
        if (reset) {
          _items
            ..clear()
            ..addAll(result.content);
        } else {
          _items.addAll(result.content);
        }
        _loading = false;
        _loadingMore = false;
        _error = null;
      });
      // 首屏数据不足一屏时继续尝试加载下一页
      if (_hasMore && _scrollController.hasClients) {
        final extentAfter = _scrollController.position.extentAfter;
        if (extentAfter < 200) {
          await _load(reset: false);
        }
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _loadingMore = false;
        _error = e.toString();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _items.isEmpty && _error == null) {
      return const LoadingView(height: 260);
    }
    if (_error != null && _items.isEmpty) {
      return ErrorView(
        message: _cleanError(_error!),
        onRetry: _refresh,
      );
    }
    if (!_loading && _items.isEmpty) {
      return EmptyView(title: widget.emptyTitle, subtitle: widget.emptySubtitle);
    }

    final listView = ListView.builder(
      controller: _scrollController,
      shrinkWrap: widget.shrinkWrap,
      physics: widget.shrinkWrap ? const NeverScrollableScrollPhysics() : null,
      padding: widget.padding,
      itemCount: _items.length + 1,
      itemBuilder: (context, index) {
        if (index == _items.length) {
          return LoadingMoreFooter(
            hasMore: widget.enableLoadMore && _hasMore,
            error: _error,
            onRetry: () => _load(reset: false),
          );
        }
        final child = widget.itemBuilder(context, _items[index], index);
        final spacing = widget.itemPadding ?? const EdgeInsets.only(bottom: 10);
        return Padding(padding: spacing, child: child);
      },
    );

    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: _refresh,
      child: listView,
    );
  }

  static String _cleanError(String raw) {
    var text = raw;
    const prefix = 'ApiException(';
    if (text.startsWith(prefix)) {
      final match = RegExp(r"message: ([^,]+)\)").firstMatch(text);
      if (match != null) return match.group(1)!.trim();
    }
    return text.length > 80 ? '${text.substring(0, 80)}…' : text;
  }
}
