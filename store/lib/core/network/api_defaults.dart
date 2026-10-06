import '../../data/models/models.dart';

/// 网络接口的空值兜底
///
/// [ApiClient.get] 的 parser 允许返回 null（后端 data 为空时），
/// 但业务侧希望直接拿到非空对象，这里统一做一次收敛，避免每个方法重复判空。
Future<T> orDefault<T>(Future<T?> source, T fallback) async =>
    await source ?? fallback;

/// 分页结果的空值兜底
Future<PageResult<T>> orEmptyPage<T>(Future<PageResult<T>?> source) async =>
    await source ?? PageResult<T>.empty();

/// 列表结果的空值兜底
Future<List<T>> orEmptyList<T>(Future<List<T>?> source) async =>
    await source ?? const [];
