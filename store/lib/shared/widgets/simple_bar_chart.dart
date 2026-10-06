import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// 极简柱状图（可复用）
///
/// 不引入三方图表库：门店端只需要看趋势高低，自绘足够且体积更可控。
/// 数据点下方标注 X 轴文案，柱子高度按区间最大值归一化。
class SimpleBarChart extends StatelessWidget {
  const SimpleBarChart({
    super.key,
    required this.points,
    this.height = 130,
    this.barColor = AppColors.chartPrimary,
    this.selectedColor,
    this.selectedIndex,
    this.onSelect,
    this.valueFormatter,
  });

  /// 数据点（label + amount）
  final List<BarPoint> points;

  /// 图表高度
  final double height;

  final Color barColor;
  final Color? selectedColor;

  /// 高亮项索引
  final int? selectedIndex;

  final ValueChanged<int>? onSelect;

  /// 数值展示格式化
  final String Function(double value)? valueFormatter;

  @override
  Widget build(BuildContext context) {
    if (points.isEmpty) {
      return SizedBox(
        height: height,
        child: const Center(
          child: Text(
            '暂无趋势数据',
            style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
          ),
        ),
      );
    }
    final maxValue = points
        .map((p) => p.value)
        .fold<double>(0, (max, v) => v > max ? v : max);
    final scale = maxValue <= 0 ? 1.0 : maxValue;

    return SizedBox(
      height: height,
      child: Column(
        children: [
          Expanded(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                for (var i = 0; i < points.length; i++)
                  Expanded(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: onSelect == null ? null : () => onSelect!(i),
                      child: _Bar(
                        point: points[i],
                        ratio: (points[i].value / scale).clamp(0.02, 1.0),
                        color: selectedIndex == i
                            ? (selectedColor ?? AppColors.chartAccent)
                            : barColor,
                        showValue: selectedIndex == i,
                        valueText: valueFormatter?.call(points[i].value),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              for (final point in points)
                Expanded(
                  child: Text(
                    point.label,
                    textAlign: TextAlign.center,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 10.5,
                      color: AppColors.textHint,
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Bar extends StatelessWidget {
  const _Bar({
    required this.point,
    required this.ratio,
    required this.color,
    required this.showValue,
    this.valueText,
  });

  final BarPoint point;
  final double ratio;
  final Color color;
  final bool showValue;
  final String? valueText;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 3),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final total = constraints.hasBoundedHeight ? constraints.maxHeight : 100.0;
          const valueHeight = 15.0;
          final available = showValue
              ? (total - valueHeight).clamp(4.0, total)
              : total;
          return Column(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              if (showValue &&
                  (valueText ?? point.valueText).isNotEmpty)
                SizedBox(
                  height: valueHeight,
                  child: Center(
                    child: Text(
                      valueText ?? point.valueText,
                      style: TextStyle(
                        fontSize: 9.5,
                        fontWeight: FontWeight.w600,
                        color: color,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ),
              AnimatedContainer(
                duration: const Duration(milliseconds: 320),
                curve: Curves.easeOutCubic,
                width: double.infinity,
                height: (available * ratio).clamp(3.0, available),
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

/// 柱状图数据点
class BarPoint {
  BarPoint({required this.label, required this.value, this.valueText = ''});

  /// X 轴文案
  final String label;

  /// 数值
  final double value;

  /// 数值展示文案
  final String valueText;
}
