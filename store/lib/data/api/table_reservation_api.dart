import '../../core/network/api_client.dart';
import '../../core/network/api_defaults.dart';
import '../models/models.dart';

/// 餐桌预约接口
///
/// 对应后端 `/api/table-reservation`：
/// - 查询类需 table:view 权限
/// - 创建 / 确认 / 到店 / 完成 / 取消 / 未到店需 table:manage 权限
class TableReservationApi {
  const TableReservationApi(this._client);

  final ApiClient _client;

  /// 创建预约（电话 / 到店散客代录）
  Future<TableReservation?> create({
    required int storeId,
    required int tableId,
    required String contactName,
    required String contactPhone,
    required int guestCount,
    required int reservedStartTime,
    required int reservedEndTime,
    String? remark,
  }) =>
      _client.post<TableReservation?>(
        '/table-reservation/add',
        body: {
          'storeId': storeId,
          'tableId': tableId,
          'contactName': contactName,
          'contactPhone': contactPhone,
          'guestCount': guestCount,
          'reservedStartTime': reservedStartTime,
          'reservedEndTime': reservedEndTime,
          if (remark != null) 'remark': remark,
        },
        parser: (raw) => raw is Map<String, dynamic>
            ? TableReservation.fromJson(raw)
            : null,
      );

  /// 按门店 + 日期范围分页查询
  Future<PageResult<TableReservation>> listByDate({
    required int storeId,
    required int startTime,
    required int endTime,
    int page = 1,
    int size = 10,
  }) =>
      orEmptyPage(
        _client.get<PageResult<TableReservation>>(
          '/table-reservation/list/date',
          queryParameters: {
            'storeId': storeId,
            'startTime': startTime,
            'endTime': endTime,
            'page': page,
            'size': size,
          },
          parser: (raw) =>
              PageResult.fromJson(raw, TableReservation.fromJson),
        ),
      );

  /// 按餐桌 + 状态查询（不分页，返回该桌某状态下的全部预约）
  Future<List<TableReservation>> listByTable({
    required int tableId,
    required int storeId,
    required int status,
  }) =>
      _client
          .get<List<TableReservation>>(
            '/table-reservation/list/table',
            queryParameters: {
              'tableId': tableId,
              'storeId': storeId,
              'status': status,
            },
            parser: (raw) => _parseList(raw),
          )
          .then((value) => value ?? const []);

  /// 确认预约
  Future<TableReservation?> confirm({
    required int reservationId,
    required int storeId,
  }) =>
      _post('/table-reservation/confirm/$reservationId', storeId: storeId);

  /// 到店落座
  Future<TableReservation?> seat({
    required int reservationId,
    required int storeId,
  }) =>
      _post('/table-reservation/seat/$reservationId', storeId: storeId);

  /// 完成就餐
  Future<TableReservation?> complete({
    required int reservationId,
    required int storeId,
  }) =>
      _post('/table-reservation/complete/$reservationId', storeId: storeId);

  /// 取消预约
  Future<TableReservation?> cancel({
    required int reservationId,
    required int storeId,
    String? reason,
  }) =>
      _post(
        '/table-reservation/cancel/$reservationId',
        storeId: storeId,
        extra: {'reason': reason},
      );

  /// 标记未到店
  Future<TableReservation?> markNoShow({
    required int reservationId,
    required int storeId,
  }) =>
      _post('/table-reservation/no-show/$reservationId', storeId: storeId);

  Future<TableReservation?> _post(
    String path, {
    required int storeId,
    Map<String, dynamic>? extra,
  }) =>
      _client.post<TableReservation?>(
        path,
        queryParameters: {'storeId': storeId, ...?extra},
        parser: (raw) => raw is Map<String, dynamic>
            ? TableReservation.fromJson(raw)
            : null,
      );

  static List<TableReservation> _parseList(Object? raw) {
    if (raw is List) {
      return raw
          .whereType<Map<String, dynamic>>()
          .map(TableReservation.fromJson)
          .toList(growable: false);
    }
    return const [];
  }
}
