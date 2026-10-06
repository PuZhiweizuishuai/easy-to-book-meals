import 'package:flutter_test/flutter_test.dart';
import 'package:store/features/orders/coupon_code_extractor.dart';

const String _code = 'a1b2c3d4e5f60718293a4b5c6d7e8f90';
const String _dashed = 'a1b2c3d4-e5f6-0718-293a-4b5c6d7e8f90';

void main() {
  group('CouponCodeExtractor', () {
    test('纯券码直接返回', () {
      expect(CouponCodeExtractor.extract(_code), _code);
    });

    test('带横线的 UUID 会去掉横线', () {
      expect(CouponCodeExtractor.extract(_dashed), _code);
      expect(CouponCodeExtractor.extract('  $_code  '), _code);
    });

    test('领券链接：?code= 参数', () {
      expect(
        CouponCodeExtractor.extract(
            'https://shop.example.com/h5/coupon?code=$_code'),
        _code,
      );
    });

    test('领券链接：couponCode / coupon 参数', () {
      expect(
        CouponCodeExtractor.extract(
            'https://shop.example.com/promo?couponCode=$_dashed'),
        _code,
      );
      expect(
        CouponCodeExtractor.extract(
            'https://shop.example.com/promo?coupon=$_code'),
        _code,
      );
    });

    test('领券链接：路径最后一段是券码', () {
      expect(
        CouponCodeExtractor.extract('https://shop.example.com/coupon/$_code'),
        _code,
      );
    });

    test('coupon:<code> 形式', () {
      expect(CouponCodeExtractor.extract('coupon:$_code'), _code);
    });

    test('空值返回 null', () {
      expect(CouponCodeExtractor.extract(''), isNull);
      expect(CouponCodeExtractor.extract('   '), isNull);
    });

    test('非券码内容原样返回，交由后端判定', () {
      const raw = 'https://shop.example.com/table/A01';
      expect(CouponCodeExtractor.extract(raw), raw);
    });
  });
}
