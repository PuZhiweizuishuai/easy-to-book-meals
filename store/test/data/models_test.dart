import 'package:flutter_test/flutter_test.dart';
import 'package:store/data/models/models.dart';

/// 32 位无横线 UUID 样例
const String _code = 'a1b2c3d4e5f60718293a4b5c6d7e8f90';
const String _codeWithDash =
    'a1b2c3d4-e5f6-0718-293a-4b5c6d7e8f90';

void main() {
  group('PageResult 分页兼容', () {
    test('兼容 page 嵌套结构，页码换算成 1 基', () {
      final result = PageResult.fromJson(
        {
          'content': [
            {'id': 1},
            {'id': 2},
          ],
          'page': {
            'number': 0,
            'totalPages': 3,
            'totalElements': 25,
          },
        },
        (json) => json['id'] as int,
      );

      expect(result.page, 1);
      expect(result.totalPages, 3);
      expect(result.totalElements, 25);
      expect(result.content, [1, 2]);
      expect(result.hasMore, isTrue);
    });

    test('兼容平铺结构（无 page 字段）', () {
      final result = PageResult.fromJson(
        {
          'content': [],
          'number': 1,
          'totalPages': 1,
          'totalElements': 0,
        },
        (json) => json['id'] as int,
      );

      expect(result.page, 2);
      expect(result.hasMore, isFalse);
      expect(result.isEmpty, isTrue);
    });

    test('空载荷与脏数据不会抛异常', () {
      final empty = PageResult.fromJson(null, (json) => json['id'] as int);
      expect(empty.isEmpty, isTrue);

      final dirty = PageResult.fromJson(
        {
          'content': ['不是对象', 123, {'id': 7}],
        },
        (json) => json['id'] as int,
      );
      expect(dirty.content, [7]);
    });
  });

  group('订单模型', () {
    test('明细派生属性与退菜过滤', () {
      final order = OrderData.fromJson({
        'id': 10,
        'orderNo': 'D20261006001',
        'status': 2,
        'orderType': 0,
        'totalAmount': 120,
        'discountAmount': 20,
        'couponAmount': 5,
        'items': [
          {
            'id': 1,
            'dishName': '宫保鸡丁',
            'quantity': 2,
            'unitPrice': 30,
            'totalPrice': 60,
            'status': 0,
            'served': 1,
          },
          {
            'id': 2,
            'dishName': '米饭',
            'quantity': 1,
            'totalPrice': 3,
            'status': 1, // 已退菜
            'served': 0,
          },
        ],
      });

      expect(order.statusEnum, OrderStatus.cooking);
      expect(order.statusEnum?.isActive, isTrue);
      expect(order.typeEnum, OrderType.dineIn);
      expect(order.basePayable, 100);
      expect(order.couponDiscount, 5);
      expect(order.totalItemCount, 1);
      expect(order.servedItemCount, 1);
      expect(order.validItems.single.dishName, '宫保鸡丁');
      expect(order.validItems.single.isServed, isTrue);
      expect(order.items!.last.isValid, isFalse);
    });

    test('状态/类型/支付状态解析兜底', () {
      expect(OrderStatus.fromCode(0), OrderStatus.pending);
      expect(OrderStatus.fromCode(99), isNull);
      expect(OrderType.fromCode(null), OrderType.dineIn);
      expect(PayStatus.fromCode(1), PayStatus.paid);
      expect(OrderStatus.completed.isActive, isFalse);
    });
  });

  group('后厨队列', () {
    test('等待等级按阈值升级', () {
      int level(int minutes) => KitchenQueueItem.fromJson({
            'itemId': 1,
            'waitedMinutes': minutes,
          }).urgencyLevel;

      expect(level(0), 0);
      expect(level(14), 0);
      expect(level(15), 1);
      expect(level(29), 1);
      expect(level(30), 2);
    });

    test('认领与加菜标记', () {
      final mine = KitchenQueueItem.fromJson({
        'itemId': 2,
        'isMine': true,
        'addedLater': 1,
        'quantity': 2,
      });
      expect(mine.myPreparing, isTrue);
      expect(mine.added, isTrue);
      expect(mine.count, 2);

      final idle = KitchenQueueItem.fromJson({'itemId': 3});
      expect(idle.myPreparing, isFalse);
      expect(idle.added, isFalse);
      expect(idle.count, 0);
    });

    test('队列结果可统计菜品数与最长等待', () {
      final result = KitchenQueueResult.fromJson({
        'items': [
          {'itemId': 1, 'waitedMinutes': 5},
          {'itemId': 2, 'waitedMinutes': 40},
        ],
        'itemCount': 2,
        'maxWaitedMinutes': 40,
      });
      expect(result.items.length, 2);
      expect(result.itemCount, 2);
      expect(result.maxWaitedMinutes, 40);
    });
  });

  group('优惠券', () {
    test('claimed 缺省为 false，金额支持整型', () {
      final result = CouponCheckResult.fromJson({
        'code': _code,
        'type': 1,
        'thresholdAmount': 50,
        'discountAmount': 10,
        'payableAmount': 90,
      });

      expect(result.claimed, isFalse);
      expect(result.thresholdAmount, 50.0);
      expect(result.discount, 10.0);
      expect(result.payable, 90.0);
      expect(CouponTypeCode.fromCode(2).label, '折扣券');
    });

    test('券码状态与可用性', () {
      final used = UserCoupon.fromJson({'code': _codeWithDash, 'status': 1});
      expect(used.statusName, '已使用');
      expect(used.usable, isFalse);

      final fresh = UserCoupon.fromJson({'code': _codeWithDash, 'status': 0});
      expect(fresh.usable, isTrue);
      expect(fresh.statusName, '未使用');
    });
  });

  group('会员与权限', () {
    test('手机号脱敏与昵称兜底', () {
      final member = Membership.fromJson({
        'id': 1,
        'phone': '13812348888',
        'num': 120,
        'count': 5,
      });
      expect(member.displayName, '138****8888');
      expect(member.points, 120);
      expect(member.orderCount, 5);

      final named = Membership.fromJson({'phone': '13812348888', 'nickname': '老李'});
      expect(named.displayName, '老李');
    });

    test('超级管理员恒拥有全部权限', () {
      final admin = UserData.fromJson({
        'id': 1,
        'nickname': '老板',
        'roleDataList': [
          {
            'roleCode': 'SUPER_ADMIN',
            'permissionList': [],
          },
        ],
      });
      expect(admin.isSuperAdmin, isTrue);
      expect(admin.hasAnyPermission(['order:manage', 'dish:manage']), isTrue);

      final waiter = UserData.fromJson({
        'roleDataList': [
          {
            'roleCode': 'WAITER',
            'permissionList': [
              {'permissionCode': 'order:manage'},
            ],
          },
        ],
      });
      expect(waiter.isSuperAdmin, isFalse);
      expect(waiter.hasAnyPermission(['order:manage']), isTrue);
      expect(waiter.hasAnyPermission(['coupon:manage']), isFalse);
      expect(waiter.displayName, '未命名账号');
    });

    test('登录结果', () {
      final login = LoginResult.fromJson({
        'token': 'satoken',
        'user': {'id': 9, 'nickname': '店长'},
      });
      expect(login.token, 'satoken');
      expect(login.user?.displayName, '店长');
    });
  });

  group('菜品与餐桌', () {
    test('价格、计价单位与库存文案', () {
      final dish = Dish.fromJson({
        'id': 5,
        'name': '干锅花菜',
        'basePrice': 38.5,
        'priceType': 1,
        'stock': -1,
        'status': 1,
        'categoryIds': [
          {'id': 1, 'name': '热菜'},
        ],
      });
      expect(dish.basePrice, 38.5);
      expect(dish.unitText, '克');
      expect(dish.stockText, '不限');
      expect(dish.onSale, isTrue);
      expect(dish.categoryText, '热菜');
    });

    test('餐桌状态解析', () {
      final table = DiningTable.fromJson({
        'id': 3,
        'tableNumber': 'A01',
        'status': 1,
        'qrCodeKey': 'key-001',
      });
      expect(table.statusEnum, TableStatus.occupied);
      expect(table.busy, isTrue);
      expect(table.areaText, '默认区域');

      expect(TableStatus.fromCode(4), TableStatus.disabled);
      expect(TableStatus.fromCode(null), TableStatus.free);
    });
  });

  group('报表聚合', () {
    test('趋势结果统计总营收与峰值', () {
      final trend = TrendResult.fromJson({
        'points': [
          {'label': '10-05', 'revenue': 1000.5, 'orderCount': 20},
          {'label': '10-06', 'revenue': 2500, 'orderCount': 35},
        ],
      });
      expect(trend.points.length, 2);
      expect(trend.totalRevenue, 3500.5);
      expect(trend.totalOrders, 55);
      expect(trend.maxRevenue, 2500);
    });

    test('畅销榜与概览缺省值', () {
      final top = TopDishResult.fromJson({
        'dishes': [
          {'dishId': 1, 'dishName': '水煮鱼', 'quantity': 12, 'revenue': 600},
        ],
      });
      expect(top.dishes.length, 1);
      expect(top.range, 'day');

      final overview = DailyOverview.fromJson({'revenue': 800, 'orderCount': 10});
      expect(overview.revenue, 800);
      expect(overview.avg, 0);
    });
  });
}
