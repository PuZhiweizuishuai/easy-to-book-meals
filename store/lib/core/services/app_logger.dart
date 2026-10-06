import 'dart:async';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// 本地日志服务
///
/// 轮询、提醒、异常等信息写入应用文档目录（path_provider），
/// 便于店家反馈问题时导出排查；同时保留最近日志在内存中，可在「关于」页直接查看。
class AppLogger {
  AppLogger._();

  static Directory? _docDir;
  static final List<String> _recent = [];
  static const int _maxRecent = 200;

  /// 最近日志（倒序后由上层展示）
  static List<String> get recent => List.unmodifiable(_recent.reversed);

  /// 初始化日志目录（在 main 中调用一次）
  static Future<void> init() async {
    try {
      _docDir = await getApplicationDocumentsDirectory();
    } catch (_) {
      _docDir = null;
    }
  }

  /// 日志文件路径，可能为 null（权限受限时）
  static String? get logFilePath {
    final dir = _docDir;
    if (dir == null) return null;
    return '${dir.path}${Platform.pathSeparator}app.log';
  }

  static Future<void> _write(String level, String message) async {
    final line = '[${DateTime.now().toIso8601String()}][$level] $message';
    _recent.add(line);
    if (_recent.length > _maxRecent) {
      _recent.removeAt(0);
    }
    final dir = _docDir;
    if (dir == null) return;
    try {
      final file = File('${dir.path}${Platform.pathSeparator}app.log');
      await file.writeAsString('$line\n', mode: FileMode.append);
    } catch (_) {
      // 日志写入失败不能影响主流程
    }
  }

  /// 信息日志
  static void i(String message) {
    // ignore: avoid_print
    print(message);
    unawaited(_write('INFO', message));
  }

  /// 警告日志
  static void w(String message, {Object? error, StackTrace? stackTrace}) {
    final full = '$message ${error ?? ''}\n$stackTrace';
    // ignore: avoid_print
    print(full);
    unawaited(_write('WARN', full));
  }

  /// 错误日志
  static void e(String message, {Object? error, StackTrace? stackTrace}) {
    final full = '$message ${error ?? ''}\n$stackTrace';
    // ignore: avoid_print
    print(full);
    unawaited(_write('ERROR', full));
  }
}
