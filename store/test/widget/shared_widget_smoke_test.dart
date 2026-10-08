import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:store/core/theme/app_colors.dart';
import 'package:store/core/theme/app_theme.dart';
import 'package:store/core/utils/money_format.dart';
import 'package:store/shared/widgets/app_button.dart';
import 'package:store/shared/widgets/app_card.dart';
import 'package:store/shared/widgets/app_feedback.dart';
import 'package:store/shared/widgets/simple_bar_chart.dart';
import 'package:store/shared/widgets/state_views.dart';
import 'package:store/shared/widgets/status_chip.dart';

/// 把被测组件包进 MaterialApp + Scaffold，模拟真实页面的渲染环境。
Widget _host(Widget child, {double width = 400}) => MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: SizedBox(
          width: width,
          child: SingleChildScrollView(child: child),
        ),
      ),
    );

List<BarPoint> _samplePoints() => [
      BarPoint(label: '10-04', value: 800),
      BarPoint(label: '10-05', value: 1500),
      BarPoint(label: '10-06', value: 1200),
    ];

void main() {
  testWidgets('通用按钮：文案渲染与禁用态', (WidgetTester tester) async {
    await tester.pumpWidget(
      _host(
        Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            PrimaryButton(label: '接单', onPressed: () {}),
            const SizedBox(height: 8),
            const PrimaryButton(label: '结算中', busy: true),
            SecondaryButton(label: '拒单', onPressed: () {}),
          ],
        ),
      ),
    );

    expect(find.text('接单'), findsOneWidget);
    expect(find.text('拒单'), findsOneWidget);

    // busy 态用转圈替换文案，同时按钮自动禁用
    expect(find.text('结算中'), findsNothing);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    final enabled = tester.widget<ElevatedButton>(find.byType(ElevatedButton).first);
    expect(enabled.enabled, isTrue);
    final busy = tester.widget<ElevatedButton>(find.byType(ElevatedButton).last);
    expect(busy.enabled, isFalse);
  });

  testWidgets('卡片与分区标题渲染', (WidgetTester tester) async {
    await tester.pumpWidget(
      _host(
        Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppCard(
              onTap: () {},
              child: const Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SectionTitle(title: '今日概览', subtitle: '10-06'),
                  InfoRow(label: '营业额', value: '¥1,234.00'),
                  InfoRow(label: '订单数', value: '32'),
                ],
              ),
            ),
          ],
        ),
      ),
    );

    expect(find.text('今日概览'), findsOneWidget);
    expect(find.text('10-06'), findsOneWidget);
    expect(find.text('营业额'), findsOneWidget);
    expect(find.text('¥1,234.00'), findsOneWidget);
    expect(find.byType(Divider), findsNWidgets(2));
  });

  testWidgets('状态标签支持图标与紧凑模式', (WidgetTester tester) async {
    await tester.pumpWidget(
      _host(
        Row(
          children: const [
            StatusChip(
              label: '待接单',
              icon: Icons.schedule,
              backgroundColor: AppColors.warningLight,
              foregroundColor: AppColors.warning,
            ),
            StatusChip(label: '已支付', dense: true),
          ],
        ),
      ),
    );

    expect(find.text('待接单'), findsOneWidget);
    expect(find.text('已支付'), findsOneWidget);
    expect(find.byIcon(Icons.schedule), findsOneWidget);
  });

  testWidgets('加载状态一览', (WidgetTester tester) async {
    await tester.pumpWidget(
      _host(
        const Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            LoadingView(message: '加载中…'),
            LoadingMoreFooter(hasMore: true),
            LoadingMoreFooter(hasMore: false),
            LoadingMoreFooter(hasMore: true, error: '网络异常'),
          ],
        ),
      ),
    );

    expect(find.text('加载中…'), findsOneWidget);
    expect(find.text('没有更多了'), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsNWidgets(2));
    expect(find.text('网络异常，点击重试'), findsOneWidget);
  });

  testWidgets('柱状图：空数据、渲染、选中回调与数值标注', (WidgetTester tester) async {
    // 1、空数据占位
    await tester.pumpWidget(_host(const SimpleBarChart(points: [])));
    expect(find.text('暂无趋势数据'), findsOneWidget);

    // 2、有数据：柱状 + X 轴文案
    int? selected;
    await tester.pumpWidget(
      _host(
        SimpleBarChart(
          points: _samplePoints(),
          selectedIndex: selected,
          onSelect: (index) => selected = index,
          valueFormatter: (value) => value.toStringAsFixed(0),
        ),
      ),
    );
    expect(find.text('10-04'), findsOneWidget);
    expect(find.text('10-05'), findsOneWidget);

    // 3、点击柱子触发回调并按最大值归一化
    await tester.tap(find.byType(GestureDetector).at(1));
    await tester.pump();
    expect(selected, 1);

    await tester.pumpWidget(
      _host(
        SimpleBarChart(
          points: _samplePoints(),
          selectedIndex: 1,
          valueFormatter: (value) => value.toStringAsFixed(0),
        ),
      ),
    );
    // 选中柱子顶部会显示数值
    expect(find.text('1500'), findsOneWidget);

    // 4、showAllValues：近 7 天每根柱子顶部都标注金额
    Widget sevenDays() => SimpleBarChart(
          points: [
            for (var i = 1; i <= 7; i++)
              BarPoint(label: '10-0$i', value: i * 1111.0),
          ],
          height: 148,
          showAllValues: true,
          valueFormatter: MoneyFormat.chartLabel,
        );

    await tester.pumpWidget(_host(sevenDays()));
    expect(find.text('1,111'), findsOneWidget);
    expect(find.text('4,444'), findsOneWidget);
    expect(find.text('7,777'), findsOneWidget);

    // 5、窄屏也放得下柱顶金额（放不下会抛 RenderFlex 溢出）
    await tester.pumpWidget(_host(sevenDays(), width: 300));
    expect(find.text('7,777'), findsOneWidget);
  });
}
