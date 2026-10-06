import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/network/api_exception.dart';
import '../core/services/app_logger.dart';
import '../data/api/auth_api.dart';
import '../data/models/models.dart';
import 'core_providers.dart';
import 'session_provider.dart';

/// 门店上下文状态
class StoreContextState {
  const StoreContextState({
    this.stores = const [],
    this.current,
    this.loading = false,
    this.error,
  });

  factory StoreContextState.initial() => const StoreContextState();

  /// 我管理的门店列表
  final List<StoreSummary> stores;

  /// 当前选中的门店
  final StoreSummary? current;

  /// 是否正在加载门店列表
  final bool loading;

  /// 加载错误文案
  final String? error;

  bool get hasStore => stores.isNotEmpty;

  /// 当前门店 ID（未选中时为 null）
  int? get currentStoreId => current?.id;

  StoreContextState copyWith({
    List<StoreSummary>? stores,
    StoreSummary? current,
    bool clearCurrent = false,
    bool? loading,
    String? error,
    bool clearError = false,
  }) =>
      StoreContextState(
        stores: stores ?? this.stores,
        current: clearCurrent ? null : (current ?? this.current),
        loading: loading ?? this.loading,
        error: clearError ? null : (error ?? this.error),
      );
}

/// 门店上下文控制器
///
/// 登录后加载「我管理的门店」并自动选中第一个（或恢复上次选择）；
/// 所有管理页面的接口都基于 [currentStoreId] 读写数据。
class StoreContextController extends Notifier<StoreContextState> {
  bool _loaded = false;

  @override
  StoreContextState build() {
    ref.listen<SessionState>(sessionProvider, (previous, next) {
      if (next.isAuthenticated && previous?.isAuthenticated != true) {
        unawaited(load());
      } else if (!next.isAuthenticated) {
        _loaded = false;
        state = StoreContextState.initial();
      }
    });
    return StoreContextState.initial();
  }

  AuthApi get _authApi => ref.read(authApiProvider);

  /// 加载门店列表
  Future<List<StoreSummary>> load() async {
    state = state.copyWith(loading: true, clearError: true);
    try {
      final page = await _authApi.myStores();
      final stores = page.content;
      final savedId = ref.read(preferencesProvider).currentStoreId;
      final valid = stores.any((s) => s.id == savedId);
      final fallback = stores.isNotEmpty ? stores.first : null;
      final selected = valid
          ? stores.firstWhere((s) => s.id == savedId)
          : fallback;
      if (selected?.id != null) {
        await ref.read(preferencesProvider).setCurrentStoreId(selected!.id);
      }
      state = state.copyWith(
        stores: stores,
        current: selected,
        clearCurrent: selected == null,
        loading: false,
      );
      _loaded = true;
      return stores;
    } on ApiException catch (e) {
      state = state.copyWith(loading: false, error: e.message);
      return const [];
    } catch (e, s) {
      AppLogger.e('加载门店列表失败', error: e, stackTrace: s);
      state = state.copyWith(loading: false, error: '加载门店失败，请稍后重试');
      return const [];
    }
  }

  /// 切换当前门店
  Future<void> selectStore(int? storeId) async {
    StoreSummary? selected;
    if (storeId != null) {
      for (final store in state.stores) {
        if (store.id == storeId) {
          selected = store;
          break;
        }
      }
    }
    await ref
        .read(preferencesProvider)
        .setCurrentStoreId(selected?.id);
    state = state.copyWith(
      current: selected,
      clearCurrent: selected == null,
    );
  }

  /// 是否已加载过
  bool get loaded => _loaded;
}

/// 门店上下文
final storeContextProvider =
    NotifierProvider<StoreContextController, StoreContextState>(
  StoreContextController.new,
);

/// 当前门店 ID（业务接口统一从它取，避免到处判空）
final currentStoreIdProvider = Provider<int?>(
  (ref) => ref.watch(storeContextProvider).currentStoreId,
);

/// 当前门店
final currentStoreProvider = Provider<StoreSummary?>(
  (ref) => ref.watch(storeContextProvider).current,
);
