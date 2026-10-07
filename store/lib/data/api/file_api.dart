import '../../core/network/api_client.dart';
import '../../core/network/api_defaults.dart';
import '../models/models.dart';

/// 文件 / 图片上传接口
///
/// 后端 `/api/files/upload` 接收 multipart 表单，字段名为 `files[]`，
/// 返回文件数组（含 id 与 fileUrl）。菜品新增/修改时需要把完整文件对象
/// 放进 `fileDataList`，后端才会把文件与菜品关联起来。
class FileApi {
  const FileApi(this._client);

  final ApiClient _client;

  /// 上传图片
  ///
  /// [paths] 为本地文件绝对路径；返回后端保存的文件信息列表。
  Future<List<UploadedFile>> upload(List<String> paths) => orEmptyList(
        _client.upload<List<UploadedFile>>(
          '/files/upload',
          filePaths: paths,
          parser: (raw) => raw is List
              ? raw
                  .whereType<Map<String, dynamic>>()
                  .map(UploadedFile.fromJson)
                  .toList(growable: false)
              : const <UploadedFile>[],
        ),
      );
}
