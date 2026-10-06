import 'dart:async';

/// 输入防抖，用于搜索框：停止输入 400ms 后才真正发起请求
class Debouncer {
  Debouncer({this.delay = const Duration(milliseconds: 400)});

  final Duration delay;
  Timer? _timer;

  /// 触发回调（会取消上一次未执行的回调）
  void call(void Function() action) {
    _timer?.cancel();
    _timer = Timer(delay, action);
  }

  /// 取消尚未执行的回调
  void cancel() {
    _timer?.cancel();
    _timer = null;
  }

  void dispose() => cancel();
}
