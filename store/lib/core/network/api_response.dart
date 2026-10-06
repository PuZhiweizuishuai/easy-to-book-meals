import 'dart:convert';

import 'package:dio/dio.dart';

import 'api_exception.dart';

/// 后端统一响应体解析结果
///
/// 后端所有接口均返回 ResponseDetails：
/// ```json
/// { "status": 200, "message": "成功", "timestamp": "...", "data": {...} }
/// ```
/// 只有 status == 200 才认为成功，其余一律抛出 [ApiException]。
class ApiResult<T> {
  const ApiResult({required this.data, required this.message});

  /// 业务数据
  final T data;

  /// 后端返回的提示信息
  final String message;
}

/// 从 Dio 响应中提取统一结果
///
/// [parser] 负责把 data 转换为强类型对象；data 为空时返回 null 由调用方处理。
ApiResult<T?> parseResponse<T>(
  Response<dynamic> response,
  T? Function(Object? raw) parser,
) {
  final body = response.data;
  final map = _asMap(body);
  if (map == null) {
    throw ApiException(
      '服务端返回了无法解析的内容',
      httpStatus: response.statusCode,
    );
  }

  final status = map['status'];
  final code = status is num ? status.toInt() : response.statusCode ?? 0;
  final message = map['message']?.toString() ?? '';

  if (code != 200) {
    throw ApiException(
      message.isEmpty ? '请求失败（$code）' : message,
      code: code,
      httpStatus: response.statusCode,
    );
  }
  return ApiResult<T?>(data: parser(map['data']), message: message);
}

/// 列表型响应解析（data 为数组）
List<T> parseList<T>(Object? raw, T Function(Map<String, dynamic> json) parser) {
  if (raw is List) {
    return raw
        .whereType<Map<String, dynamic>>()
        .map(parser)
        .toList(growable: false);
  }
  if (raw is Map<String, dynamic>) {
    // 少数接口直接把数组放在 content 里
    final content = raw['content'];
    if (content is List) {
      return content
          .whereType<Map<String, dynamic>>()
          .map(parser)
          .toList(growable: false);
    }
  }
  return const [];
}

/// 把响应体转成 Map（兼容已解码的 Map / JSON 字符串）
Map<String, dynamic>? asMap(Object? value) => _asMap(value);

Map<String, dynamic>? _asMap(Object? value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) {
    return value.map((key, val) => MapEntry(key.toString(), val));
  }
  if (value is String && value.isNotEmpty) {
    try {
      final decoded = jsonDecode(value);
      return _asMap(decoded);
    } catch (_) {
      return null;
    }
  }
  return null;
}
