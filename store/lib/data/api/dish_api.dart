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
}

/// `/dishes/update` 的返回体：`{ food, skuList, specGroups, fileDataList }`
class CreateDishesResultWrapper {
  const CreateDishesResultWrapper({this.food});

  factory CreateDishesResultWrapper.fromJson(Map<String, dynamic> json) =>
      CreateDishesResultWrapper(
        food: json['food'] is Map<String, dynamic>
            ? Dish.fromJson(json['food'] as Map<String, dynamic>)
            : null,
      );

  final Dish? food;
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
