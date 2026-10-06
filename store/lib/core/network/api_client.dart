import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../services/app_logger.dart';
import '../services/preferences_service.dart';
import 'api_exception.dart';
import 'api_response.dart';

/// Dio 客户端封装
///
/// 职责：
/// - 读取本地保存的服务器地址作为 baseUrl（设置页更换地址后调用 [rebuild]）
/// - 自动携带 Sa-Token 请求头 `Token`
/// - 统一解析 ResponseDetails：非 200 抛 [ApiException]，网络类错误同样转成 [ApiException]
///
/// 业务层只接触 [ApiClient.get/post]，不直接处理 Dio 异常。
class ApiClient {
  ApiClient(this._prefs, {this.baseUrlOverride}) {
    _dio = createDio();
  }

  final PreferencesService _prefs;

  /// 可选的地址覆盖：用于「服务器设置」页在保存前先测试连通性
  final String? baseUrlOverride;

  late Dio _dio;

  /// 当前实际使用的接口根地址（诊断/测试用）
  String get baseUrl => _dio.options.baseUrl;

  /// 构建 Dio 实例
  ///
  /// baseUrl 来源于本地保存的服务器地址。
  Dio createDio() {
    // 首次启动未配置地址时 apiBaseUrl 只是相对前缀（如 /api），Dio 要求绝对 URL，
    // 构造阶段先用合法占位地址兜底；未配置时业务层不会发起任何请求，
    // 地址保存后会通过 [rebuild] 生成真正的 Dio。
    var baseUrl = baseUrlOverride ?? _prefs.apiBaseUrl;
    if (!baseUrl.startsWith('http://') && !baseUrl.startsWith('https://')) {
      baseUrl = 'http://127.0.0.1${AppConfig.apiPrefix}';
    }
    final dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: AppConfig.connectTimeout,
        receiveTimeout: AppConfig.receiveTimeout,
        sendTimeout: AppConfig.uploadTimeout,
        responseType: ResponseType.json,
        contentType: Headers.jsonContentType,
        // 让所有响应（含 4xx/5xx）都进入 parseResponse，由业务统一的 status 字段判断
        validateStatus: (status) => status != null,
      ),
    );
    dio.interceptors.add(_AuthTokenInterceptor(_prefs));
    return dio;
  }

  /// 服务器地址变更后重建底层 Dio
  void rebuild() => _dio = createDio();

  /// 释放底层连接（临时实例用完后调用）
  void close() => _dio.close();

  // ==================== 请求方法 ====================

  /// GET 请求，[path] 不含 baseUrl
  Future<T?> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T? Function(Object? raw) parser,
    CancelToken? cancelToken,
  }) async {
    final response = await _safe(
      () => _dio.get<dynamic>(
        path,
        queryParameters: cleanParams(queryParameters),
        cancelToken: cancelToken,
      ),
    );
    return parseResponse<T>(response, parser).data;
  }

  /// POST 请求（body 为 JSON）
  Future<T?> post<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    required T? Function(Object? raw) parser,
    CancelToken? cancelToken,
  }) async {
    final response = await _safe(
      () => _dio.post<dynamic>(
        path,
        data: body,
        queryParameters: cleanParams(queryParameters),
        cancelToken: cancelToken,
      ),
    );
    return parseResponse<T>(response, parser).data;
  }

  /// DELETE 请求
  Future<T?> delete<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T? Function(Object? raw) parser,
  }) async {
    final response = await _safe(
      () => _dio.delete<dynamic>(
        path,
        queryParameters: cleanParams(queryParameters),
      ),
    );
    return parseResponse<T>(response, parser).data;
  }

  /// 过滤空值参数，避免把 null / 空串拼进 queryString
  static Map<String, dynamic>? cleanParams(Map<String, dynamic>? params) {
    if (params == null) return null;
    final cleaned = <String, dynamic>{};
    for (final entry in params.entries) {
      final value = entry.value;
      if (value == null) continue;
      if (value is String && value.trim().isEmpty) continue;
      cleaned[entry.key] = value;
    }
    return cleaned.isEmpty ? null : cleaned;
  }

  /// 统一兜底 Dio 异常，转换为 [ApiException]
  Future<Response<dynamic>> _safe(
    Future<Response<dynamic>> Function() call,
  ) async {
    try {
      return await call();
    } on DioException catch (e) {
      AppLogger.w('请求失败: ${e.requestOptions.path}', error: e.message);
      throw _mapDioException(e);
    } catch (e, s) {
      AppLogger.e('请求异常', error: e, stackTrace: s);
      throw ApiException(
        '网络异常，请稍后重试',
        code: networkErrorCode,
      );
    }
  }
}

/// 自动携带 Token 的拦截器
class _AuthTokenInterceptor extends Interceptor {
  _AuthTokenInterceptor(this._prefs);

  final PreferencesService _prefs;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _prefs.token;
    if (token.isNotEmpty) {
      options.headers[AppConfig.tokenHeader] = token;
    }
    handler.next(options);
  }
}

/// Dio 异常 → [ApiException]
ApiException _mapDioException(DioException err) {
  if (err.error is ApiException) return err.error! as ApiException;

  final status = err.response?.statusCode;
  final body = err.response?.data;
  final map = asMap(body);
  if (map != null) {
    final code = map['status'] is num
        ? (map['status'] as num).toInt()
        : status ?? 0;
    final message = map['message']?.toString() ?? '';
    return ApiException(
      message.isEmpty ? '请求失败（$code）' : message,
      code: code,
      httpStatus: status,
    );
  }

  String message;
  switch (err.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
      message = '请求超时，请检查网络或服务器地址';
      break;
    case DioExceptionType.connectionError:
      message = '无法连接服务器，请检查服务器地址与网络';
      break;
    case DioExceptionType.badResponse:
      message = '服务器返回异常（${status ?? '-'}）';
      break;
    case DioExceptionType.cancel:
      message = '请求已取消';
      break;
    case DioExceptionType.badCertificate:
      message = '服务器证书校验失败';
      break;
    case DioExceptionType.transformTimeout:
      message = '响应处理超时，请稍后重试';
      break;
    case DioExceptionType.unknown:
      message = '网络异常，请稍后重试';
      break;
  }
  return ApiException(message, httpStatus: status, code: networkErrorCode);
}
