import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 全局数据刷新总线
///
/// 主框架的五个 Tab 用 IndexedStack 常驻，页面不会被销毁，
/// 仅靠首次加载的数据会出现「订单已结算、看板营业额仍是进入时的旧值」。
/// 写操作（接单 / 出餐 / 结算 / 上下架 / 保存菜品）完成后调用 [bump]，
/// 所有 watch 了该总线的 provider 会重新拉取数据。
final dataRefreshProvider =
    NotifierProvider<DataRefreshTick, int>(DataRefreshTick.new);

/// 刷新计数器（值本身无意义，变化即触发刷新）
class DataRefreshTick extends Notifier<int> {
  @override
  int build() => 0;

  /// 通知所有数据页重新拉取
  void bump() {
    if (!ref.mounted) return;
    state = state + 1;
  }
}
