import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:material_symbols_icons/material_symbols_icons.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../data/models/models.dart';
import '../../providers/core_providers.dart';
import '../../providers/refresh_bus.dart';
import '../../shared/widgets/app_button.dart';
import '../../shared/widgets/app_feedback.dart';
import '../../shared/widgets/app_filter_chip.dart';
import '../../shared/widgets/image_upload_tile.dart';
import '../../shared/widgets/sku_editor.dart';
import '../../shared/widgets/spec_group_editor.dart';

/// 菜品新增 / 编辑面板
///
/// 与 Web 端 CreateDishesCard 对齐，分四段填写：
/// 1. 基本信息（名称、描述）
/// 2. 图片设置（封面图 1 张 + 详情页图片多张，走 /files/upload）
/// 3. 售卖规则（计价方式、价格、库存、上下架、分类标签）
/// 4. 规格配置（规格组 + 选项，新增随菜品提交，编辑走 /sku/adjust）
class DishEditSheet extends ConsumerStatefulWidget {
  const DishEditSheet({
    super.key,
    required this.storeId,
    this.dish,
    this.onSaved,
  });

  /// 门店 ID
  final int storeId;

  /// 传入表示编辑，为空表示新增
  final Dish? dish;

  Future<bool?> show(BuildContext context) => showModalBottomSheet<bool?>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.white,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        builder: (_) => SizedBox(
          height: MediaQuery.of(context).size.height * 0.94,
          child: this,
        ),
      );

  /// 保存成功回调
  final VoidCallback? onSaved;

  @override
  ConsumerState<DishEditSheet> createState() => _DishEditSheetState();
}

class _DishEditSheetState extends ConsumerState<DishEditSheet> {
  late final TextEditingController _nameController;
  late final TextEditingController _descController;
  late final TextEditingController _priceController;
  late final TextEditingController _stockController;

  int _priceType = 0;
  int _status = 1;
  bool _submitting = false;

  // ==================== 分类 ====================
  bool _categoriesLoading = true;
  List<CategorySummary> _categories = const [];
  final Set<int> _selectedCategoryIds = <int>{};

  // ==================== 图片 ====================
  String? _topImage;
  List<String> _detailImages = const [];
  List<UploadedFile> _uploadedFiles = const [];
  bool _uploadingCover = false;
  bool _uploadingDetail = false;

  // ==================== 规格 ====================
  List<SpecGroupInput> _specGroups = const [];
  bool _loadingDetail = false;

  /// 详情（规格 + 已关联图片）是否加载完成
  ///
  /// `/dishes/update` 会按 fileDataList 做差量更新：列表里没有的已关联文件
  /// 会被删除（含物理文件）。所以编辑场景必须等详情加载完再允许保存，
  /// 否则一次保存就会把菜品图片全清掉。
  bool _detailLoaded = false;

  /// 用户是否改动过规格（未改动就不调用 /sku/adjust，避免无谓重建 SKU）
  bool _specDirty = false;

  // ==================== SKU ====================
  /// 菜品已有的 SKU（价格 / 库存 / 折扣 / 上下架），只有编辑场景才有
  List<SkuInput> _skuList = const [];
  bool _skuDirty = false;

  bool get _isEdit => widget.dish != null;

  @override
  void initState() {
    super.initState();
    final dish = widget.dish;
    _nameController = TextEditingController(text: dish?.name ?? '');
    _descController = TextEditingController(text: dish?.description ?? '');
    _priceController = TextEditingController(
      text: dish == null ? '' : (dish.basePrice ?? 0).toStringAsFixed(2),
    );
    _stockController = TextEditingController(
      text: dish == null ? '-1' : '${dish.stock ?? -1}',
    );
    _priceType = dish?.priceType ?? 0;
    _status = dish?.status ?? 1;
    _topImage = dish?.topImage?.trim().isEmpty == true ? null : dish?.topImage;
    _detailImages = dish?.imageUrls ?? const [];
    for (final category in dish?.categoryIds ?? const []) {
      if (category.id != null) _selectedCategoryIds.add(category.id!);
    }
    _loadCategories();
    if (_isEdit) {
      _loadDishDetail();
    } else {
      _detailLoaded = true;
    }
  }

  Future<void> _loadCategories() async {
    try {
      final list = await ref.read(categoryApiProvider).list();
      if (!mounted) return;
      setState(() {
        _categories = list;
        _categoriesLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _categoriesLoading = false);
    }
  }

  /// 编辑场景：回填已有规格组与已关联图片
  Future<void> _loadDishDetail() async {
    final dishId = widget.dish?.id;
    if (dishId == null) return;
    setState(() => _loadingDetail = true);
    try {
      final result = await ref.read(dishApiProvider).detail(dishId: dishId);
      if (!mounted || result == null) return;
      setState(() {
        _specGroups = result.specGroups;
        _skuList =
            result.skuList.map((sku) => sku.toInput()).toList(growable: true);
        _uploadedFiles = result.fileDataList;
        if (_topImage == null || _topImage!.isEmpty) {
          _topImage = result.food?.topImage;
        }
        if (_detailImages.isEmpty && result.food != null) {
          _detailImages = result.food!.imageUrls;
        }
        _loadingDetail = false;
        _detailLoaded = true;
      });
    } catch (_) {
      if (mounted) setState(() => _loadingDetail = false);
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descController.dispose();
    _priceController.dispose();
    _stockController.dispose();
    super.dispose();
  }

  // ==================== 图片上传 ====================

  /// 选择并上传图片
  ///
  /// [cover] 为 true 时只选 1 张作为封面，否则多选追加到详情页图片。
  Future<void> _pickAndUpload({required bool cover}) async {
    final picker = ImagePicker();
    final List<XFile> picked;
    try {
      if (cover) {
        final file = await picker.pickImage(
          source: ImageSource.gallery,
          maxWidth: 1440,
          imageQuality: 88,
        );
        picked = file == null ? const <XFile>[] : <XFile>[file];
      } else {
        picked = await picker.pickMultiImage(maxWidth: 1440, imageQuality: 88);
      }
    } catch (e) {
      if (mounted) AppToast.error(context, '无法打开相册：$e');
      return;
    }
    if (picked.isEmpty) return;

    setState(() {
      if (cover) {
        _uploadingCover = true;
      } else {
        _uploadingDetail = true;
      }
    });
    try {
      final files = await ref
          .read(fileApiProvider)
          .upload(picked.map((file) => file.path).toList());
      if (!mounted) return;
      if (files.isEmpty) {
        AppToast.error(context, '上传失败，未返回文件信息');
        return;
      }
      final urls = files
          .map((file) => file.fileUrl)
          .whereType<String>()
          .where((url) => url.isNotEmpty)
          .toList(growable: false);
      setState(() {
        _uploadedFiles = <UploadedFile>[..._uploadedFiles, ...files];
        if (cover) {
          if (urls.isNotEmpty) _topImage = urls.first;
        } else {
          _detailImages = <String>[..._detailImages, ...urls];
        }
      });
      AppToast.success(context, '已上传 ${files.length} 张图片');
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    } catch (_) {
      if (mounted) AppToast.error(context, '图片上传失败，请稍后重试');
    } finally {
      if (mounted) {
        setState(() {
          _uploadingCover = false;
          _uploadingDetail = false;
        });
      }
    }
  }

  void _removeImage(String url, {required bool cover}) {
    setState(() {
      if (cover) {
        _topImage = null;
      } else {
        _detailImages =
            _detailImages.where((item) => item != url).toList(growable: false);
      }
      _uploadedFiles = _uploadedFiles
          .where((file) => file.fileUrl != url)
          .toList(growable: false);
    });
  }

  // ==================== 分类 ====================

  Future<void> _createCategory() async {
    final controller = TextEditingController();
    final name = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('新增分类'),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(hintText: '请输入分类名称'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () {
              final value = controller.text.trim();
              Navigator.of(context).pop(value.isEmpty ? null : value);
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
    if (name == null || !mounted) return;
    try {
      final created = await ref.read(categoryApiProvider).create(name: name);
      if (!mounted) return;
      if (created?.id == null) {
        AppToast.error(context, '分类创建失败');
        return;
      }
      setState(() {
        _categories = <CategorySummary>[..._categories, created!];
        _selectedCategoryIds.add(created.id!);
      });
      AppToast.success(context, '分类已创建');
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
    } catch (_) {
      if (mounted) AppToast.error(context, '分类创建失败，请稍后重试');
    }
  }

  // ==================== 提交 ====================

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    if (_isEdit && !_detailLoaded) {
      // 详情未加载完就保存会带上空的 fileDataList，后端会把菜品图片全部删除
      AppToast.warning(context, '菜品详情尚未加载完成，请稍候或重试');
      return;
    }
    final name = _nameController.text.trim();
    if (name.isEmpty) {
      AppToast.error(context, '请输入菜品名称');
      return;
    }
    final price = double.tryParse(_priceController.text.trim());
    if (price == null || price < 0) {
      AppToast.error(context, '请输入正确的价格');
      return;
    }
    final stock = int.tryParse(_stockController.text.trim()) ?? -1;

    // 规格组校验：填了组名就必须有选项，反之亦然
    final filledGroups =
        _specGroups.where((group) => group.name.trim().isNotEmpty).toList();
    for (final group in filledGroups) {
      if (!group.values.any((value) => value.name.trim().isNotEmpty)) {
        AppToast.error(context, '规格「${group.name.trim()}」至少需要一个选项');
        return;
      }
    }

    final selected = _categories
        .where((category) => _selectedCategoryIds.contains(category.id))
        .toList(growable: false);

    setState(() => _submitting = true);
    try {
      final api = ref.read(dishApiProvider);
      if (_isEdit) {
        final dishId = widget.dish!.id!;
        // 规格不走 /dishes/update（后端明确忽略），改动后单独调用 /sku/adjust。
        // 空列表不提交：后端会据此把旧 SKU 全部锁定，删除规格请在 Web 端操作。
        final specChanged = _specDirty && filledGroups.isNotEmpty;
        if (specChanged) {
          await api.adjustSpecGroups(
            dishId: dishId,
            specGroups: [
              for (var i = 0; i < filledGroups.length; i++)
                filledGroups[i].cleaned.copyWith(sort: i + 1),
            ],
          );
        }
        await api.update(
          UpdateDishPayload(
            id: dishId,
            storefrontId: widget.storeId,
            name: name,
            description: _descController.text.trim(),
            topImage: _topImage,
            imageList: _detailImages,
            basePrice: price,
            priceType: _priceType,
            categoryIds: selected,
            stock: stock,
            status: _status,
            fileDataList: _uploadedFiles,
            // 规格变更后后端会重建 SKU，此时旧 SKU 上的编辑已失效，不能再提交
            skuList: (!specChanged && _skuDirty && _skuList.isNotEmpty)
                ? _skuList
                : null,
          ),
        );
      } else {
        await api.create(
          CreateDishPayload(
            storefrontId: widget.storeId,
            name: name,
            description: _descController.text.trim(),
            topImage: _topImage,
            imageList: _detailImages,
            unitPrice: price,
            priceType: _priceType,
            categoryIds: selected,
            stock: stock,
            status: _status,
            specGroups: filledGroups,
            fileDataList: _uploadedFiles,
          ),
        );
      }
      if (!mounted) return;
      // 菜品变动会影响看板与菜单缓存，通知常驻数据页重新拉取
      ref.read(dataRefreshProvider.notifier).bump();
      AppToast.success(context, _isEdit ? '菜品已更新' : '菜品已创建');
      widget.onSaved?.call();
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      AppToast.error(context, e.message);
      setState(() => _submitting = false);
    } catch (_) {
      if (!mounted) return;
      AppToast.error(context, '保存失败，请稍后重试');
      setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildHeader(),
        const Divider(height: 1),
        Expanded(child: _buildBody()),
        _buildFooter(),
      ],
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 8, 10),
      child: Row(
        children: [
          Expanded(
            child: Text(
              _isEdit ? '编辑菜品' : '新增菜品',
              style: const TextStyle(
                fontSize: 16.5,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 20),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return ListView(
      padding: EdgeInsets.fromLTRB(16, 6, 16, 16 + bottomInset),
      children: [
        // ==================== 基本信息 ====================
        const _SectionTitle(icon: Symbols.info, title: '基本信息'),
        TextField(
          controller: _nameController,
          decoration: const InputDecoration(labelText: '菜品名称 *'),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _descController,
          maxLines: 2,
          decoration: const InputDecoration(labelText: '菜品描述'),
        ),
        const SizedBox(height: 20),

        // ==================== 图片设置 ====================
        const _SectionTitle(icon: Symbols.image, title: '图片设置'),
        const SizedBox(height: 8),
        const Text(
          '封面图',
          style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
        ),
        const SizedBox(height: 8),
        ImageUploadTile(
          urls: _topImage == null ? const [] : <String>[_topImage!],
          max: 1,
          uploading: _uploadingCover,
          onPick: () => _pickAndUpload(cover: true),
          onRemove: (url) => _removeImage(url, cover: true),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            const Text(
              '详情页图片',
              style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
            ),
            const Spacer(),
            Text(
              '已选 ${_detailImages.length} 张',
              style: const TextStyle(fontSize: 11.5, color: AppColors.textHint),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ImageUploadTile(
          urls: _detailImages,
          max: 9,
          uploading: _uploadingDetail,
          hint: '支持多张上传，最多 9 张',
          onPick: () => _pickAndUpload(cover: false),
          onRemove: (url) => _removeImage(url, cover: false),
        ),
        const SizedBox(height: 20),

        // ==================== 售卖规则 ====================
        const _SectionTitle(icon: Symbols.sell, title: '售卖规则'),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _priceController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: '默认价格 *'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: DropdownButtonFormField<int>(
                initialValue: _priceType,
                decoration: const InputDecoration(labelText: '计价方式'),
                items: [
                  for (final code in PriceTypeMeta.codes)
                    DropdownMenuItem<int>(
                      value: code,
                      child: Text('按${PriceTypeMeta.name(code)}'),
                    ),
                ],
                onChanged: (value) => setState(() => _priceType = value ?? 0),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _stockController,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: '库存（-1 表示不限）',
          ),
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            const Text(
              '上架状态',
              style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
            ),
            const Spacer(),
            Switch(
              value: _status == 1,
              activeThumbColor: AppColors.primary,
              onChanged: (value) => setState(() => _status = value ? 1 : 0),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            const Text(
              '分类标签',
              style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
            ),
            const Spacer(),
            TextButton.icon(
              icon: const Icon(Symbols.add, size: 16),
              label: const Text('新增分类', style: TextStyle(fontSize: 13)),
              onPressed: _createCategory,
            ),
          ],
        ),
        const SizedBox(height: 4),
        _buildCategoryChips(),
        const SizedBox(height: 20),

        // ==================== 规格配置 ====================
        const _SectionTitle(icon: Symbols.tune, title: '规格配置'),
        const SizedBox(height: 6),
        const Text(
          '如辣度、份量、温度。新增菜品时会一并生成规格组合（SKU）。',
          style: TextStyle(fontSize: 11.5, color: AppColors.textHint),
        ),
        const SizedBox(height: 10),
        if (_isEdit && _loadingDetail)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 12),
            child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
          )
        else if (_isEdit && !_detailLoaded)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.warningLight,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.info_outline,
                    size: 15, color: AppColors.warning),
                const SizedBox(width: 6),
                const Text(
                  '已有规格与图片未加载',
                  style: TextStyle(fontSize: 12.5, color: Color(0xFF8A5A00)),
                ),
                TextButton(
                  onPressed: _loadDishDetail,
                  child: const Text('重试'),
                ),
              ],
            ),
          )
        else
          SpecGroupEditor(
            initialGroups: _specGroups,
            onChanged: (groups) {
              _specGroups = groups;
              _specDirty = true;
            },
          ),
        const SizedBox(height: 8),

        // ==================== SKU 价格与库存（仅编辑） ====================
        if (_isEdit) ...[
          const _SectionTitle(icon: Symbols.inventory_2, title: 'SKU 价格与库存'),
          const SizedBox(height: 6),
          const Text(
            '按规格组合单独设置价格、库存与折扣；改动规格后旧组合会被锁定。',
            style: TextStyle(fontSize: 11.5, color: AppColors.textHint),
          ),
          const SizedBox(height: 10),
          if (_loadingDetail)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
            )
          else if (!_detailLoaded)
            const Text(
              'SKU 未加载，请先点击上方「重试」',
              style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
            )
          else
            SkuEditor(
              initialSkus: _skuList,
              specGroups: _specGroups,
              onChanged: (skus) {
                _skuList = skus;
                _skuDirty = true;
              },
            ),
        ],
      ],
    );
  }

  Widget _buildCategoryChips() {
    if (_categoriesLoading) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 10),
        child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
      );
    }
    if (_categories.isEmpty) {
      return const Text(
        '暂无分类，点击右上角「新增分类」创建',
        style: TextStyle(fontSize: 12.5, color: AppColors.textHint),
      );
    }
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final category in _categories)
          AppFilterChip(
            label: category.displayName,
            selected: _selectedCategoryIds.contains(category.id),
            onTap: () {
              setState(() {
                if (_selectedCategoryIds.contains(category.id)) {
                  _selectedCategoryIds.remove(category.id);
                } else {
                  _selectedCategoryIds.add(category.id!);
                }
              });
            },
          ),
      ],
    );
  }

  Widget _buildFooter() {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: Color(0xFFEDEFF3))),
        ),
        child: Row(
          children: [
            Expanded(
              child: SecondaryButton(
                label: '取消',
                onPressed: () => Navigator.of(context).pop(),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 2,
              child: PrimaryButton(
                label: _isEdit ? '保存修改' : '确认添加',
                busy: _submitting,
                onPressed: _submitting || (_isEdit && !_detailLoaded)
                    ? null
                    : _submit,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 分段标题（与 Web 端的 section 标题对应）
class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.icon, required this.title});

  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Container(
            width: 3,
            height: 14,
            decoration: BoxDecoration(
              color: AppColors.primary,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: 7),
          Icon(icon, size: 16, color: AppColors.primary),
          const SizedBox(width: 5),
          Text(
            title,
            style: const TextStyle(
              fontSize: 14.5,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
