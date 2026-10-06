import '../../core/utils/json_read.dart';

/// Spring Data 分页结果解析
///
/// 后端不同版本的分页序列化结构存在差异：
/// - VIA_DTO：`{"content": [], "page": {"number": 0, "totalPages": 1, "totalElements": 0}}`
/// - 平铺：`{"content": [], "number": 0, "totalPages": 1, "totalElements": 0}`
///
/// 这里同时兼容两种结构，业务层只认 [page]（1 基）与 [totalPages]。
class PageResult<T> {
  const PageResult({
    required this.content,
    required this.page,
    required this.totalPages,
    required this.totalElements,
  });

  /// 空页常量
  factory PageResult.empty() => PageResult<T>(
        content: const [],
        page: 1,
        totalPages: 1,
        totalElements: 0,
      );

  factory PageResult.fromJson(
    Object? raw,
    T Function(Map<String, dynamic> json) parser,
  ) {
    if (raw == null) return PageResult<T>.empty();
    final map = raw is Map<String, dynamic>
        ? raw
        : (raw is Map
            ? raw.map((k, v) => MapEntry(k.toString(), v))
            : <String, dynamic>{});

    if (map.isEmpty) return PageResult<T>.empty();

    final meta = map['page'];
    final metaMap = meta is Map<String, dynamic> ? meta : <String, dynamic>{};

    final rawNumber = metaMap['number'] ?? map['number'];
    final page = readInt(rawNumber, fallback: 1) + 1;

    final totalPages = readInt(
      metaMap['totalPages'] ?? map['totalPages'],
      fallback: 1,
    );
    final totalElements = readInt(
      metaMap['totalElements'] ?? map['totalElements'],
      fallback: 0,
    );

    final contentRaw = map['content'];
    final List<T> content;
    if (contentRaw is List) {
      content = <T>[
        for (final entry in contentRaw)
          if (entry is Map<String, dynamic>) parser(entry),
      ];
    } else {
      content = <T>[];
    }

    return PageResult<T>(
      content: content,
      page: page < 1 ? 1 : page,
      totalPages: totalPages < 1 ? 1 : totalPages,
      totalElements: totalElements,
    );
  }

  /// 当前页数据
  final List<T> content;

  /// 当前页码（1 基）
  final int page;

  /// 总页数
  final int totalPages;

  /// 总条数
  final int totalElements;

  /// 是否还有下一页
  bool get hasMore => page < totalPages;

  /// 是否为空结果
  bool get isEmpty => content.isEmpty && totalElements == 0;
}
