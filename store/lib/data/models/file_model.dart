/// 上传后的文件信息（对应后端 FileData）
///
/// `/api/files/upload` 返回文件数组，前端需要把完整对象回传给
/// `/dishes/add`、`/dishes/update` 的 `fileDataList`，后端据此把文件与菜品关联。
class UploadedFile {
  const UploadedFile({
    this.id,
    this.externalId,
    this.fileUrl,
    this.fileOriginalName,
    this.fileNewName,
    this.size,
    this.uploadTime,
    this.uploadUserId,
    this.type,
    this.suffixName,
    this.height,
    this.width,
  });

  factory UploadedFile.fromJson(Map<String, dynamic> json) => UploadedFile(
        id: _asInt(json['id']),
        externalId: _asInt(json['externalId']),
        fileUrl: json['fileUrl']?.toString(),
        fileOriginalName: json['fileOriginalName']?.toString(),
        fileNewName: json['fileNewName']?.toString(),
        size: _asInt(json['size']),
        uploadTime: _asInt(json['uploadTime']),
        uploadUserId: _asInt(json['uploadUserId']),
        type: _asInt(json['type']),
        suffixName: json['suffixName']?.toString(),
        height: _asInt(json['height']),
        width: _asInt(json['width']),
      );

  final int? id;
  final int? externalId;

  /// 服务器保存的文件地址（相对路径，展示时交给 AppUrlResolver 补齐）
  final String? fileUrl;

  final String? fileOriginalName;
  final String? fileNewName;
  final int? size;
  final int? uploadTime;
  final int? uploadUserId;
  final int? type;
  final String? suffixName;
  final int? height;
  final int? width;

  Map<String, dynamic> toJson() => <String, dynamic>{
        if (id != null) 'id': id,
        if (externalId != null) 'externalId': externalId,
        if (fileUrl != null) 'fileUrl': fileUrl,
        if (fileOriginalName != null) 'fileOriginalName': fileOriginalName,
        if (fileNewName != null) 'fileNewName': fileNewName,
        if (size != null) 'size': size,
        if (uploadTime != null) 'uploadTime': uploadTime,
        if (uploadUserId != null) 'uploadUserId': uploadUserId,
        if (type != null) 'type': type,
        if (suffixName != null) 'suffixName': suffixName,
        if (height != null) 'height': height,
        if (width != null) 'width': width,
      };

  static int? _asInt(Object? value) {
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }
}
