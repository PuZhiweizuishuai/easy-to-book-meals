import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:store/core/network/api_client.dart';
import 'package:store/core/services/preferences_service.dart';
import 'package:store/core/theme/app_theme.dart';
import 'package:store/data/models/models.dart';
import 'package:store/features/tables/table_page.dart';
import 'package:store/features/tables/widgets/table_tile.dart';
import 'package:store/providers/core_providers.dart';
import 'package:store/providers/refresh_bus.dart';
import 'package:store/providers/store_context_provider.dart';

/// 桩接口返回的餐桌
const Map<String, dynamic> _tableA01 = {
  'id': 1,
  'storefrontId': 1,
  'tableNumber': 'A01',
  'capacity': 4,
  'status': 1,
  'area': '大厅',
  'qrCodeKey': 'qr-a01',
  'currentOrderId': 88,
  'sortOrder': 0,
  'createTime': 1730000000000,
};

const Map<String, dynamic> _tableB02 = {
  'id': 2,
  'storefrontId': 1,
  'tableNumber': 'B02',
  'capacity': 8,
  'status': 2,
  'area': '包间',
  'qrCodeKey': 'qr-b02',
  'sortOrder': 1,
  'createTime': 1730000000000,
};

/// 预约记录桩数据
Map<String, dynamic> _reservation({int status = 0}) => {
      'id': 7,
      'storeId': 1,
      'tableId': 1,
      'contactName': '张三',
      'contactPhone': '13800000000',
      'guestCount': 3,
      'status': status,
      'reservedStartTime': 1730080800000,
      'reservedEndTime': 1730088000000,
      'remark': '靠窗',
      'createTime': 1730000000000,
    };

const Map<String, dynamic> _order88 = {
  'id': 88,
  'orderNo': 'D202610080001',
  'storeId': 1,
  'tableId': 1,
  'tableNumber': 'A01',
  'userNickname': '小明',
  'orderType': 1,
  'status': 1,
  'payAmount': 128.5,
  'remark': '少辣',
  'createTime': 1730000000000,
  'items': [
    {
      'id': 1,
      'orderId': 88,
      'dishName': '宫保鸡丁',
      'specNames': '大份',
      'quantity': 1,
      'totalPrice': 48.0,
      'status': 0,
      'served': 0,
    },
    {
      'id': 2,
      'orderId': 88,
      'dishName': '酸菜鱼',
      'quantity': 2,
      'totalPrice': 80.5,
      'status': 0,
      'served': 1,
    },
  ],
};

/// 已收到的请求，形如 `GET /dining-table/list/all`
final List<String> _requests = <String>[];

/// 已收到的带查询参数的请求，形如 `GET /table-reservation/list/table?tableId=1&storeId=1&status=2`
final List<String> _requestsWithQuery = <String>[];

/// 预约是否已被落座（详情页会据此返回「已到店」状态的预约）
bool _seated = false;

/// 按请求路径返回后端 `data` 字段内容
Object? _stubData(String method, String path, Map<String, dynamic> query) {
  if (path.endsWith('/dining-table/list/all')) {
    return <Map<String, dynamic>>[_tableA01, _tableB02];
  }
  if (method == 'GET' && path.endsWith('/dining-table/1')) {
    return _tableA01;
  }
  if (method == 'POST' && path.endsWith('/dining-table/status')) {
    return <String, dynamic>{
      ..._tableA01,
      'status': int.parse('${query['status']}'),
    };
  }
  if (path.endsWith('/order/manage/detail/88')) {
    return _order88;
  }
  if (path.endsWith('/table-reservation/list/date')) {
    return {
      'content': [_reservation()],
      // Spring Page 元信息嵌套在 page 字段中，与真实后端一致
      'page': {'number': 0, 'totalPages': 1, 'totalElements': 1},
    };
  }
  if (path.endsWith('/table-reservation/list/table')) {
    final status = int.parse('${query['status']}');
    // 详情页按状态分别拉取本桌预约：落座前后命中不同状态
    final currentStatus = _seated ? 2 : 0;
    return status == currentStatus
        ? [_reservation(status: currentStatus)]
        : <Object>[];
  }
  if (method == 'POST' && path.contains('/table-reservation/')) {
    if (path.endsWith('/seat/7')) _seated = true;
    return _reservation(status: switch (path) {
        _ when path.endsWith('/confirm/7') => 1,
        _ when path.endsWith('/seat/7') => 2,
        _ when path.endsWith('/complete/7') => 5,
        _ when path.endsWith('/cancel/7') => 3,
        _ => 0,
      });
  }
  return null;
}

/// 直接返回桩响应的 Dio 适配器（避免真实网络与 FakeAsync 计时器冲突）
class _StubAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    _requests.add('${options.method} ${options.path}');
    _requestsWithQuery.add('${options.method} ${options.uri}');
    final body = jsonEncode({
      'status': 200,
      'message': '成功',
      'data': _stubData(
        options.method,
        options.path,
        options.queryParameters,
      ),
    });
    return ResponseBody.fromString(
      body,
      200,
      headers: {
        'content-type': ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _StubApiClient extends ApiClient {
  _StubApiClient(super.prefs);

  @override
  Dio createDio() => Dio(
        BaseOptions(
          baseUrl: 'http://stub.local/api',
          responseType: ResponseType.json,
        ),
      )..httpClientAdapter = _StubAdapter();
}

/// 固定门店上下文的桩控制器
class _StubStoreContext extends StoreContextController {
  @override
  StoreContextState build() => const StoreContextState(
        stores: [StoreSummary(id: 1, storeName: '测试门店')],
        current: StoreSummary(id: 1, storeName: '测试门店'),
      );
}

int get _tableListCalls =>
    _requests.where((r) => r.endsWith('/dining-table/list/all')).length;

void main() {
  late SharedPreferences prefs;

  setUp(() async {
    _requests.clear();
    _requestsWithQuery.clear();
    _seated = false;
    SharedPreferences.setMockInitialValues({'token': 'test-token'});
    prefs = await SharedPreferences.getInstance();
  });

  /// 挂载餐桌管理页，返回 ProviderContainer 便于操作刷新总线
  ///
  /// [surface] 默认加高视口，让餐桌网格下方的预约时间线、以及详情页底部的
  /// 预约卡片都被 ListView 构建出来（懒加载只渲染视口内的子项）。
  Future<ProviderContainer> pumpTablePage(
    WidgetTester tester, {
    Size surface = const Size(900, 2200),
  }) async {
    tester.view.physicalSize = surface;
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final container = ProviderContainer(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        apiClientProvider.overrideWith((ref) => _StubApiClient(
              PreferencesService(prefs),
            )),
        storeContextProvider.overrideWith(_StubStoreContext.new),
      ],
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(theme: AppTheme.light(), home: const TablePage()),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  testWidgets('餐桌页可正常挂载：列表与预约时间线都渲染', (tester) async {
    await pumpTablePage(tester);

    expect(find.text('餐桌管理'), findsOneWidget);
    expect(find.text('A01'), findsOneWidget);
    expect(find.text('B02'), findsOneWidget);
    // 座位数与区域合并为一行：'4 座 · 大厅'
    expect(find.text('4 座 · 大厅'), findsOneWidget);
    expect(find.text('预约时间线'), findsOneWidget);
    expect(find.text('张三'), findsOneWidget);
    expect(
      _requests.any((r) => r.endsWith('/table-reservation/list/date')),
      isTrue,
    );
  });

  testWidgets('窄屏手机尺寸下餐桌卡不溢出、文字不被图标遮挡', (tester) async {
    await pumpTablePage(tester, surface: const Size(360, 690));

    expect(find.text('A01'), findsOneWidget);
    expect(find.text('4 座 · 大厅'), findsOneWidget);
    // 溢出断言会由 flutter_test 捕获并让用例失败，这里只需正常渲染完成
    expect(tester.takeException(), isNull);

    // 回归：卡片左下角的预约图标不能压住座位数/区域文字
    final tile = find.byType(TableTile).first;
    final info = tester.getRect(
      find.descendant(of: tile, matching: find.text('4 座 · 大厅')),
    );
    final reserveIcon = tester.getRect(
      find.descendant(of: tile, matching: find.byIcon(Symbols.event_available)),
    );
    final qrIcon = tester.getRect(
      find.descendant(of: tile, matching: find.byIcon(Symbols.qr_code)),
    );
    expect(reserveIcon.overlaps(info), isFalse);
    expect(qrIcon.overlaps(info), isFalse);
  });

  testWidgets('刷新总线 bump 后重新拉取餐桌列表', (tester) async {
    final container = await pumpTablePage(tester);
    expect(_tableListCalls, 1);

    container.read(dataRefreshProvider.notifier).bump();
    await tester.pumpAndSettle();

    expect(_tableListCalls, 2);
  });

  testWidgets('点击餐桌卡进入详情：展示当前订单与本桌预约', (tester) async {
    await pumpTablePage(tester);

    await tester.tap(find.text('A01'));
    await tester.pumpAndSettle();

    expect(find.text('当前订单'), findsOneWidget);
    expect(find.text('D202610080001'), findsOneWidget);
    expect(find.text('宫保鸡丁'), findsOneWidget);
    expect(find.text('酸菜鱼'), findsOneWidget);
    expect(find.text('本桌预约'), findsOneWidget);
    expect(find.textContaining('靠窗'), findsWidgets);
    expect(_requests.any((r) => r.endsWith('/dining-table/1')), isTrue);
  });

  testWidgets('详情页切换餐桌状态会调用状态接口', (tester) async {
    await pumpTablePage(tester);
    await tester.tap(find.text('A01'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('清理中'));
    await tester.pumpAndSettle();

    expect(
      _requests.any(
        (r) => r.startsWith('POST') && r.endsWith('/dining-table/status'),
      ),
      isTrue,
    );
  });

  testWidgets('详情页预约覆盖已到店状态：落座后仍可完成', (tester) async {
    await pumpTablePage(tester);
    await tester.tap(find.text('A01'));
    await tester.pumpAndSettle();

    // 详情页会同时查询待确认 / 已确认 / 已到店三种状态
    for (final status in ['0', '1', '2']) {
      expect(
        _requestsWithQuery.any(
          (r) =>
              r.contains('/table-reservation/list/table') &&
              r.contains('status=$status'),
        ),
        isTrue,
        reason: '缺少 status=$status 的本桌预约查询',
      );
    }

    await tester.tap(find.text('到店'));
    await tester.pumpAndSettle();

    expect(
      _requests.any(
        (r) => r.startsWith('POST') && r.endsWith('/table-reservation/seat/7'),
      ),
      isTrue,
    );
    // 落座后这条预约仍显示在本桌预约里，并给出「完成」入口
    expect(find.text('张三'), findsOneWidget);
    expect(find.text('完成'), findsOneWidget);
  });

  testWidgets('时间线确认预约：调用接口并刷新餐桌列表', (tester) async {
    final container = await pumpTablePage(tester);

    await tester.tap(find.text('确认'));
    await tester.pumpAndSettle();

    expect(
      _requests.any(
        (r) => r.startsWith('POST') && r.endsWith('/table-reservation/confirm/7'),
      ),
      isTrue,
    );
    expect(container.read(dataRefreshProvider), greaterThan(0));
    expect(_tableListCalls, greaterThan(1));
  });
}
