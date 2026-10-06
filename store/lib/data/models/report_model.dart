import 'package:json_annotation/json_annotation.dart';

part 'report_model.g.dart';

/// 指定日期经营概览（/api/manage/report/overview）
@JsonSerializable()
class DailyOverview {
  const DailyOverview({
    this.revenue,
    this.orderCount,
    this.completedCount,
    this.cancelledCount,
    this.memberOrderCount,
    this.discountAmount,
    this.avgOrderAmount,
    this.date,
    this.itemCount,
    this.totalRevenue,
    this.totalOrderCount,
  });

  factory DailyOverview.fromJson(Map<String, dynamic> json) =>
      _$DailyOverviewFromJson(json);

  /// 当日营业额（已完成订单实付）
  final double? revenue;

  /// 当日订单总数
  final int? orderCount;

  /// 已完成数
  final int? completedCount;

  /// 已取消数
  final int? cancelledCount;

  /// 会员单量（有手机号结算的订单）
  final int? memberOrderCount;

  /// 优惠合计（菜品折扣 + 券 + 手动减免）
  final double? discountAmount;

  /// 客单价
  final double? avgOrderAmount;

  /// 统计日期 yyyy-MM-dd
  final String? date;

  /// 售出菜品件数（排除退菜）
  final double? itemCount;

  /// 历史累计销售额
  final double? totalRevenue;

  /// 历史累计完成订单数
  final int? totalOrderCount;

  /// 客单价（兼容字段缺失）
  double get avg => avgOrderAmount ?? 0;

  Map<String, dynamic> toJson() => _$DailyOverviewToJson(this);
}

/// 走势图上的一个点（/api/manage/report/trend）
@JsonSerializable()
class TrendPoint {
  const TrendPoint({
    this.label,
    this.date,
    this.revenue,
    this.discount,
    this.orderCount,
    this.completedCount,
    this.cancelledCount,
    this.avgOrderAmount,
  });

  factory TrendPoint.fromJson(Map<String, dynamic> json) =>
      _$TrendPointFromJson(json);

  /// X 轴文案（按天为 MM-dd，按月为 yyyy-MM）
  final String? label;

  /// 完整日期键
  final String? date;

  final double? revenue;
  final double? discount;
  final int? orderCount;
  final int? completedCount;
  final int? cancelledCount;
  final double? avgOrderAmount;

  double get amount => revenue ?? 0;
  int get orders => orderCount ?? 0;

  Map<String, dynamic> toJson() => _$TrendPointToJson(this);
}

/// 走势接口返回结果
class TrendResult {
  const TrendResult({required this.points, this.summary = const {}});

  factory TrendResult.fromJson(Map<String, dynamic> json) {
    final rawPoints = json['points'];
    final points = rawPoints is List
        ? rawPoints
            .whereType<Map<String, dynamic>>()
            .map(TrendPoint.fromJson)
            .toList(growable: false)
        : const <TrendPoint>[];
    final summary = json['summary'];
    return TrendResult(
      points: points,
      summary: summary is Map<String, dynamic> ? summary : const {},
    );
  }

  final List<TrendPoint> points;
  final Map<String, dynamic> summary;

  /// 区间总营收
  double get totalRevenue =>
      points.fold<double>(0, (sum, p) => sum + p.amount);

  /// 区间总单量
  int get totalOrders => points.fold<int>(0, (sum, p) => sum + p.orders);

  /// 营收峰值（用于图表 Y 轴上限）
  double get maxRevenue =>
      points.fold<double>(0, (max, p) => p.amount > max ? p.amount : max);
}

/// 畅销菜品项（/api/manage/report/top-dishes）
@JsonSerializable()
class TopDish {
  const TopDish({
    this.dishId,
    this.dishName,
    this.topImage,
    this.quantity,
    this.revenue,
  });

  factory TopDish.fromJson(Map<String, dynamic> json) =>
      _$TopDishFromJson(json);

  final int? dishId;
  final String? dishName;
  final String? topImage;

  /// 售出数量
  final double? quantity;

  /// 销售额
  final double? revenue;

  Map<String, dynamic> toJson() => _$TopDishToJson(this);
}

/// 畅销榜返回结果
class TopDishResult {
  const TopDishResult({required this.dishes, this.range = 'day'});

  factory TopDishResult.fromJson(Map<String, dynamic> json) {
    final rawDishes = json['dishes'];
    final dishes = rawDishes is List
        ? rawDishes
            .whereType<Map<String, dynamic>>()
            .map(TopDish.fromJson)
            .toList(growable: false)
        : const <TopDish>[];
    return TopDishResult(
      dishes: dishes,
      range: json['range']?.toString() ?? 'day',
    );
  }

  final List<TopDish> dishes;
  final String range;
}
