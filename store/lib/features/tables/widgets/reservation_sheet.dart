import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/time_format.dart';
import '../../../data/models/models.dart';
import '../../../providers/core_providers.dart';
import '../../../providers/refresh_bus.dart';
import '../../../providers/session_provider.dart';
import '../../../shared/widgets/app_button.dart';
import '../../../shared/widgets/app_feedback.dart';

/// 预约餐桌表单弹层
///
/// 录入预订人 / 电话 / 人数 / 起止时间 / 备注，
/// 校验通过后调用 `/table-reservation/add` 创建预约，成功后返回 true
/// 并触发数据刷新总线，调用方据此刷新餐桌列表与预约时间线。
class ReservationSheet extends ConsumerStatefulWidget {
  const ReservationSheet({
    super.key,
    required this.table,
    required this.storeId,
  });

  /// 被预约的餐桌
  final DiningTable table;

  /// 门店 ID
  final int storeId;

  /// 弹出预约表单，返回 true 表示预约创建成功
  static Future<bool?> show(
    BuildContext context, {
    required DiningTable table,
    required int storeId,
  }) =>
      showModalBottomSheet<bool?>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.white,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        builder: (_) => ReservationSheet(table: table, storeId: storeId),
      );

  @override
  ConsumerState<ReservationSheet> createState() => _ReservationSheetState();
}

class _ReservationSheetState extends ConsumerState<ReservationSheet> {
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _guestController = TextEditingController();
  final _remarkController = TextEditingController();

  /// 默认开始时间：当前整点 + 1 小时；结束时间：开始时间 + 2 小时
  late DateTime _start = _defaultStart();
  late DateTime _end = _defaultEnd();

  bool _submitting = false;

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _guestController.dispose();
    _remarkController.dispose();
    super.dispose();
  }

  DateTime _defaultStart() {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day, now.hour + 1, 0);
  }

  DateTime _defaultEnd() =>
      DateTime(_start.year, _start.month, _start.day, _start.hour + 2, 0);

  int get _capacity => widget.table.capacity ?? 0;

  Future<void> _pickDateTime({required bool isStart}) async {
    final initial = isStart ? _start : _end;
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime.now().subtract(const Duration(minutes: 1)),
      lastDate: DateTime.now().add(const Duration(days: 60)),
    );
    if (!mounted || pickedDate == null) return;
    final pickedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(initial),
      helpText: isStart ? '选择开始时间' : '选择结束时间',
    );
    if (!mounted || pickedTime == null) return;
    final combined = DateTime(
      pickedDate.year,
      pickedDate.month,
      pickedDate.day,
      pickedTime.hour,
      pickedTime.minute,
    );
    setState(() {
      if (isStart) {
        _start = combined;
        // 开始时间后移时，若结束时间早于等于开始时间，自动顺延 2 小时
        if (!_end.isAfter(_start)) {
          _end = _start.add(const Duration(hours: 2));
        }
      } else {
        _end = combined;
      }
    });
  }

  Future<void> _submit() async {
    final name = _nameController.text.trim();
    if (name.isEmpty) {
      AppToast.warning(context, '请输入预订人姓名');
      return;
    }
    final phone = _phoneController.text.trim();
    if (phone.isEmpty) {
      AppToast.warning(context, '请输入联系电话');
      return;
    }
    final guest = int.tryParse(_guestController.text.trim());
    if (guest == null || guest < 1) {
      AppToast.warning(context, '就餐人数至少为 1');
      return;
    }
    if (_capacity > 0 && guest > _capacity) {
      AppToast.warning(context, '该餐桌最多容纳 $_capacity 人');
      return;
    }
    final startMs = _start.millisecondsSinceEpoch;
    final endMs = _end.millisecondsSinceEpoch;
    if (startMs >= endMs) {
      AppToast.warning(context, '开始时间必须早于结束时间');
      return;
    }
    if (startMs < DateTime.now().millisecondsSinceEpoch) {
      AppToast.warning(context, '开始时间不能早于当前时间');
      return;
    }

    setState(() => _submitting = true);
    try {
      await ref.read(tableReservationApiProvider).create(
            storeId: widget.storeId,
            tableId: widget.table.id!,
            contactName: name,
            contactPhone: phone,
            guestCount: guest,
            reservedStartTime: startMs,
            reservedEndTime: endMs,
            remark: _remarkController.text.trim().isEmpty
                ? null
                : _remarkController.text.trim(),
          );
      if (!mounted) return;
      AppToast.success(context, '预约创建成功');
      // 预约会影响餐桌状态（空闲→已预订）与时间线，通知常驻数据页刷新
      ref.read(dataRefreshProvider.notifier).bump();
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.isUnauthorized) {
        await ref.read(sessionProvider.notifier).handleUnauthorized();
        return;
      }
      AppToast.error(context, e.message);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '创建预约失败，请稍后重试');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final table = widget.table;
    return SafeArea(
      top: false,
      child: SingleChildScrollView(
        padding: EdgeInsets.only(
          left: 16,
          right: 16,
          top: 14,
          bottom: 16 + MediaQuery.of(context).viewInsets.bottom,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Container(
                  width: 4,
                  height: 20,
                  decoration: BoxDecoration(
                    color: AppColors.warning,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  '预约餐桌 · ${table.tableNumber ?? '-'}',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close, size: 20),
                  onPressed: _submitting
                      ? null
                      : () => Navigator.of(context).pop(),
                ),
              ],
            ),
            const Divider(height: 1),
            const SizedBox(height: 14),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _nameController,
                    autofocus: true,
                    inputFormatters: [
                      LengthLimitingTextInputFormatter(30),
                    ],
                    decoration: const InputDecoration(
                      labelText: '预订人 *',
                      hintText: '姓名',
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    inputFormatters: [
                      LengthLimitingTextInputFormatter(20),
                    ],
                    decoration: const InputDecoration(
                      labelText: '联系电话 *',
                      hintText: '手机号',
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _guestController,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                labelText: '就餐人数 *',
                hintText: '1',
                helperText: _capacity > 0
                    ? '该餐桌最多容纳 $_capacity 人'
                    : '请输入就餐人数',
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _DateTimeField(
                    label: '开始时间 *',
                    value: TimeFormat.dateTime(_start.millisecondsSinceEpoch),
                    onTap: _submitting
                        ? null
                        : () => _pickDateTime(isStart: true),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _DateTimeField(
                    label: '结束时间 *',
                    value: TimeFormat.dateTime(_end.millisecondsSinceEpoch),
                    onTap: _submitting
                        ? null
                        : () => _pickDateTime(isStart: false),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _remarkController,
              maxLines: 3,
              inputFormatters: [
                LengthLimitingTextInputFormatter(255),
              ],
              decoration: const InputDecoration(
                labelText: '备注',
                hintText: '特殊需求或备注（选填）',
                alignLabelWithHint: true,
              ),
            ),
            const SizedBox(height: 18),
            PrimaryButton(
              label: '确认预约',
              icon: Symbols.event_available,
              busy: _submitting,
              backgroundColor: AppColors.warning,
              onPressed: _submit,
            ),
          ],
        ),
      ),
    );
  }
}

/// 日期时间选择展示字段（点击触发选择器）
class _DateTimeField extends StatelessWidget {
  const _DateTimeField({
    required this.label,
    required this.value,
    required this.onTap,
  });

  final String label;
  final String value;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(10),
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          suffixIcon: const Icon(Icons.event_outlined, size: 18),
        ),
        child: Text(
          value,
          style: const TextStyle(fontSize: 14, color: AppColors.textPrimary),
        ),
      ),
    );
  }
}
