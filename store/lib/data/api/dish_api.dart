import '../../core/network/api_client.dart';
import '../../core/network/api_defaults.dart';
import '../models/models.dart';

/// 菜品管理接口
///
/// 列表需要 dish:view 权限，新增/修改需要 dish:manage 权限。
class DishApi {
  const DishApi(this._client);

  final ApiClient _client;

  /// 分页查询本店菜品（含下架菜品）
  Future<PageResult<Dish>> list({
    required int storeId,
    String? key,
    int page = 1,
    int size = 10,
  }) =>
      orEmptyPage(
        _client.get<PageResult<Dish>>(
          '/dishes/list',
          queryParameters: {
            'storefrontId': storeId,
            'key': key,
            'page': page,
            'size': size,
          },
          parser: (raw) => PageResult.fromJson(raw, Dish.fromJson),
        ),
      );

  /// 新增菜品
  Future<Dish?> create(CreateDishPayload payload) => _client.post<Dish?>(
        '/dishes/add',
        body: payload.toJson(),
        parser: (raw) => raw is Map<String, dynamic> ? Dish.fromJson(raw) : null,
      );

  /// 菜品详情（含规格组与已关联图片，用于编辑回填）
  ///
  /// 该接口是公开的（顾客端也要用），拿不到时返回 null 由调用方降级。
  Future<CreateDishesResultWrapper?> detail({required int dishId}) =>
      _client.get<CreateDishesResultWrapper>(
        '/dishes/$dishId',
        parser: (raw) => raw is Map<String, dynamic>
            ? CreateDishesResultWrapper.fromJson(raw)
            : null,
      );

  /// 更新菜品基础信息（含分类绑定）
  Future<Dish?> update(UpdateDishPayload payload) async {
    final result = await _client.post<CreateDishesResultWrapper>(
      '/dishes/update',
      body: payload.toJson(),
      parser: (raw) => raw is Map<String, dynamic>
          ? CreateDishesResultWrapper.fromJson(raw)
          : null,
    );
    return result?.food;
  }

  /// 调整菜品规格组（新增 / 改名 / 删除，后端会同步重建 SKU）
  ///
  /// `/dishes/update` 明确忽略 specGroups，规格变更必须走 `/sku/adjust`。
  Future<void> adjustSpecGroups({
    required int dishId,
    required List<SpecGroupInput> specGroups,
  }) =>
      _client.post<void>(
        '/sku/adjust',
        body: {
          'dishesId': dishId,
          'specGroups': specGroups.map((group) => group.toJson()).toList(),
        },
        parser: (raw) => raw,
      );
}

/// `/dishes/{id}` 与 `/dishes/update` 的返回体：`{ food, skuList, specGroups, fileDataList }`
class CreateDishesResultWrapper {
  const CreateDishesResultWrapper({
    this.food,
    this.specGroups = const [],
    this.fileDataList = const [],
    this.skuList = const [],
  });

  factory CreateDishesResultWrapper.fromJson(Map<String, dynamic> json) =>
      CreateDishesResultWrapper(
        food: json['food'] is Map<String, dynamic>
            ? Dish.fromJson(json['food'] as Map<String, dynamic>)
            : null,
        specGroups: _parseSpecGroups(json['specGroups']),
        fileDataList: _parseFiles(json['fileDataList']),
        skuList: _parseSkus(json['skuList']),
      );

  final Dish? food;

  /// 菜品已有的规格组（编辑时回填表单）
  final List<SpecGroupInput> specGroups;

  /// 菜品已关联的图片文件（编辑时回填表单）
  final List<UploadedFile> fileDataList;

  /// 菜品已有的 SKU 列表（按规格组合生成，编辑时可单独改价 / 库存 / 折扣）
  final List<ProductSku> skuList;

  static List<SpecGroupInput> _parseSpecGroups(Object? raw) {
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(SpecGroupInput.fromJson)
        .toList(growable: false);
  }

  static List<UploadedFile> _parseFiles(Object? raw) {
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(UploadedFile.fromJson)
        .toList(growable: false);
  }

  static List<ProductSku> _parseSkus(Object? raw) {
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(ProductSku.fromJson)
        .toList(growable: false);
  }
}

/// 菜品分类（标签）接口
///
/// 分类为全局共享数据，增删改需要店主身份。
class CategoryApi {
  const CategoryApi(this._client);

  final ApiClient _client;

  /// 全部分类列表（公开）
  Future<List<CategorySummary>> list() => _client.get<List<CategorySummary>>(
        '/foodtag/list',
        parser: (raw) => _parseList(raw),
      ).then((value) => value ?? const []);

  /// 新增分类
  Future<CategorySummary?> create({
    required String name,
    int sort = 1,
  }) =>
      _client.post<CategorySummary?>(
        '/foodtag/add',
        body: {'name': name, 'sort': sort},
        parser: (raw) =>
            raw is Map<String, dynamic> ? CategorySummary.fromJson(raw) : null,
      );

  /// 修改分类
  Future<CategorySummary?> update({
    required int id,
    required String name,
    int sort = 1,
  }) =>
      _client.post<CategorySummary?>(
        '/foodtag/update',
        body: {'id': id, 'name': name, 'sort': sort},
        parser: (raw) =>
            raw is Map<String, dynamic> ? CategorySummary.fromJson(raw) : null,
      );

  /// 删除分类
  Future<void> delete({required int id}) => _client.post<void>(
        '/foodtag/delete/$id',
        parser: (raw) => raw,
      );

  static List<CategorySummary> _parseList(Object? raw) {
    if (raw is List) {
      return raw
          .whereType<Map<String, dynamic>>()
          .map(CategorySummary.fromJson)
          .toList(growable: false);
    }
    return const [];
  }
}
