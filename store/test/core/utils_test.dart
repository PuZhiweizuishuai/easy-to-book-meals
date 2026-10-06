import 'package:flutter_test/flutter_test.dart';
import 'package:store/core/utils/money_format.dart';
import 'package:store/core/utils/server_address.dart';
import 'package:store/core/utils/time_format.dart';

void main() {
  group('MoneyFormat', () {
    test('金额统一保留两位小数', () {
      expect(MoneyFormat.yuan(12.5), '¥12.50');
      expect(MoneyFormat.yuan(0), '¥0.00');
      expect(MoneyFormat.plain(12.5), '12.50');
      expect(MoneyFormat.plain(8), '8.00');
    });

    test('数量：整数不带小数，称重菜保留有效小数', () {
      expect(MoneyFormat.quantity(2), '2');
      expect(MoneyFormat.quantity(0.5), '0.5');
      expect(MoneyFormat.quantity(1.234), '1.234');
    });

    test('大额紧凑展示：超过一万折算成「万」', () {
      expect(MoneyFormat.compact(15000), '1.5万');
      expect(MoneyFormat.compact(999), '999.00');
    });
  });

  group('TimeFormat', () {
    final fixed = DateTime(2026, 10, 6, 12, 30);
    final millis = fixed.millisecondsSinceEpoch;

    test('常规格式化', () {
      expect(TimeFormat.dateTime(millis), '2026-10-06 12:30');
      expect(TimeFormat.time(millis), '12:30');
      expect(TimeFormat.monthDay(millis), '10-06');
      expect(TimeFormat.date(millis), '2026-10-06');
      expect(TimeFormat.apiDate(fixed), '2026-10-06');
    });

    test('空时间戳统一输出 --', () {
      expect(TimeFormat.dateTime(null), '--');
      expect(TimeFormat.dateTime(0), '--');
      expect(TimeFormat.smart(null), '--');
      expect(TimeFormat.smart(0), '--');
    });

    test('今天/昨天识别', () {
      final now = DateTime.now();
      expect(
        TimeFormat.smart(now.millisecondsSinceEpoch),
        startsWith('今天 '),
      );
      final yesterday = now.subtract(const Duration(days: 1));
      expect(
        TimeFormat.smart(yesterday.millisecondsSinceEpoch),
        startsWith('昨天 '),
      );
    });

    test('等待时长文案', () {
      expect(TimeFormat.waited(null), '刚刚');
      expect(TimeFormat.waited(0), '刚刚');
      expect(TimeFormat.waited(45), '45 分钟');
      expect(TimeFormat.waited(60), '1 小时');
      expect(TimeFormat.waited(95), '1 小时35 分');
    });
  });

  group('ServerAddress', () {
    test('缺省协议自动补 http://', () {
      expect(ServerAddress.normalize('192.168.1.5:8081'),
          'http://192.168.1.5:8081');
      expect(ServerAddress.normalize('  shop.example.com  '),
          'http://shop.example.com');
    });

    test('去掉结尾多余的斜杠', () {
      expect(ServerAddress.normalize('http://192.168.1.5:8081/'),
          'http://192.168.1.5:8081');
      expect(ServerAddress.normalize('https://a.com//'), 'https://a.com');
    });

    test('拼出接口根地址', () {
      expect(ServerAddress.apiBase('http://192.168.1.5:8081'),
          'http://192.168.1.5:8081/api');
      expect(ServerAddress.apiBase(''), '/api');
    });

    test('校验：空值与非法格式给出提示，合法地址通过', () {
      expect(ServerAddress.validate(''), isNotNull);
      expect(ServerAddress.validate('   '), isNotNull);
      expect(ServerAddress.validate('http://192.168.1.5:8081'), isNull);
      expect(ServerAddress.validate('https://shop.example.com'), isNull);
    });
  });
}
