import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/debouncer.dart';

/// 搜索输入框
///
/// 内置防抖：停止输入 400ms 后才回调 [onSearch]，避免每敲一个字都请求后端。
/// [hint] 与 [leadingIcon] 可自定义，常见于列表页顶部。
class SearchField extends StatefulWidget {
  const SearchField({
    super.key,
    this.hint = '搜索',
    this.onSearch,
    this.onChanged,
    this.controller,
    this.debounceDelay = const Duration(milliseconds: 400),
    this.contentPadding = const EdgeInsets.symmetric(horizontal: 14),
  });

  final String hint;
  final ValueChanged<String>? onSearch;
  final ValueChanged<String>? onChanged;
  final TextEditingController? controller;
  final Duration debounceDelay;
  final EdgeInsetsGeometry contentPadding;

  @override
  State<SearchField> createState() => _SearchFieldState();
}

class _SearchFieldState extends State<SearchField> {
  final Debouncer _debouncer = Debouncer();
  late TextEditingController _controller;
  bool _hasText = false;

  @override
  void initState() {
    super.initState();
    _controller = widget.controller ?? TextEditingController();
    _hasText = _controller.text.isNotEmpty;
    _controller.addListener(_onTextChanged);
  }

  @override
  void dispose() {
    _debouncer.dispose();
    if (widget.controller == null) {
      _controller.dispose();
    }
    super.dispose();
  }

  void _onTextChanged() {
    final text = _controller.text;
    if (_hasText != text.isNotEmpty) {
      setState(() => _hasText = text.isNotEmpty);
    }
    widget.onChanged?.call(text);
    _debouncer
      ..cancel()
      ..call(() => widget.onSearch?.call(text));
  }

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: _controller,
      textInputAction: TextInputAction.search,
      onSubmitted: (value) => widget.onSearch?.call(value),
      decoration: InputDecoration(
        hintText: widget.hint,
        prefixIcon: const Icon(Icons.search, size: 20, color: AppColors.textHint),
        suffixIcon: _hasText
            ? IconButton(
                icon: const Icon(Icons.close, size: 18, color: AppColors.textHint),
                onPressed: () {
                  _controller.clear();
                  widget.onSearch?.call('');
                },
              )
            : null,
        contentPadding: widget.contentPadding,
      ),
    );
  }
}
