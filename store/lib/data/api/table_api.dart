import '../../core/network/api_client.dart';
import '../../core/network/api_defaults.dart';
import '../models/models.dart';

/// 餐桌管理接口
///
/// 查询需要 table:view 权限，增删改与改状态需要 table:manage 权限。
class TableApi {
  const TableApi(this._client);

  final ApiClient _client;

  /// 查询单个餐桌详情
  Future<DiningTable?> detail({required int id, required int storeId}) =>
      _client.get<DiningTable?>(
        '/dining-table/$id',
        queryParameters: {'storefrontId': storeId},
        parser: (raw) => raw is Map<String, dynamic>
            ? DiningTable.fromJson(raw)
            : null,
      );

  /// 查询本店全部餐桌（不分页）
  Future<List<DiningTable>> listAll({required int storeId}) =>
      _client
          .get<List<DiningTable>>(
            '/dining-table/list/all',
            queryParameters: {'storefrontId': storeId},
            parser: (raw) => _parseList(raw),
          )
          .then((value) => value ?? const []);

  /// 分页查询餐桌
  Future<PageResult<DiningTable>> list({
    required int storeId,
    int page = 1,
    int size = 100,
  }) =>
      orEmptyPage(
        _client.get<PageResult<DiningTable>>(
          '/dining-table/list',
          queryParameters: {
            'storefrontId': storeId,
            'page': page,
            'size': size,
          },
          parser: (raw) => PageResult.fromJson(raw, DiningTable.fromJson),
        ),
      );

  /// 按状态筛选
  Future<List<DiningTable>> listByStatus({
    required int storeId,
    required int status,
  }) =>
      _client
          .get<List<DiningTable>>(
            '/dining-table/list/status',
            queryParameters: {'storefrontId': storeId, 'status': status},
            parser: (raw) => _parseList(raw),
          )
          .then((value) => value ?? const []);

  /// 新增餐桌
  Future<DiningTable?> create({
    required int storeId,
    required String tableNumber,
    required int capacity,
    String? area,
    String? remark,
    int status = 0,
  }) =>
      _client.post<DiningTable?>(
        '/dining-table/add',
        body: {
          'storefrontId': storeId,
          'tableNumber': tableNumber,
          'capacity': capacity,
          'area': area,
          'remark': remark,
          'status': status,
        },
        parser: (raw) =>
            raw is Map<String, dynamic> ? DiningTable.fromJson(raw) : null,
      );

  /// 修改餐桌
  Future<DiningTable?> update({
    required int id,
    required int storeId,
    required String tableNumber,
    required int capacity,
    String? area,
    String? remark,
    int? status,
  }) =>
      _client.post<DiningTable?>(
        '/dining-table/update',
        body: {
          'id': id,
          'storefrontId': storeId,
          'tableNumber': tableNumber,
          'capacity': capacity,
          'area': area,
          'remark': remark,
          if (status != null) 'status': status,
        },
        parser: (raw) =>
            raw is Map<String, dynamic> ? DiningTable.fromJson(raw) : null,
      );

  /// 删除餐桌（逻辑删除）
  Future<void> delete({required int id, required int storeId}) =>
      _client.post<void>(
        '/dining-table/delete/$id',
        queryParameters: {'storefrontId': storeId},
        parser: (raw) => raw,
      );

  /// 更新餐桌状态
  Future<DiningTable?> updateStatus({
    required int id,
    required int storeId,
    required int status,
  }) =>
      _client.post<DiningTable?>(
        '/dining-table/status',
        queryParameters: {
          'id': id,
          'storefrontId': storeId,
          'status': status,
        },
        parser: (raw) =>
            raw is Map<String, dynamic> ? DiningTable.fromJson(raw) : null,
      );

  static List<DiningTable> _parseList(Object? raw) {
    if (raw is List) {
      return raw
          .whereType<Map<String, dynamic>>()
          .map(DiningTable.fromJson)
          .toList(growable: false);
    }
    return const [];
  }
}
