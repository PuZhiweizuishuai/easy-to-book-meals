/// 全局静态配置
///
/// 集中放置与应用身份、网络、轮询、分页相关的常量，
/// 避免在业务代码里散落魔法值。
library;

/// 应用级常量
class AppConfig {
  const AppConfig._();

  /// 应用名
  static const String appName = '店家工作台';

  /// 后端接口统一前缀（用户只需配置 http(s)://host:port）
  static const String apiPrefix = '/api';

  /// 建议的后端端口提示文案
  static const String defaultPortHint = '8081';

  /// 连接超时
  static const Duration connectTimeout = Duration(seconds: 10);

  /// 读取超时（报表类查询较慢，适当放宽）
  static const Duration receiveTimeout = Duration(seconds: 20);

  /// 上传超时
  static const Duration uploadTimeout = Duration(seconds: 30);

  /// 默认轮询间隔（秒）
  static const int defaultPollIntervalInSeconds = 15;
  static const Duration defaultPollInterval =
      Duration(seconds: defaultPollIntervalInSeconds);

  /// 轮询可选档位（秒）
  static const List<int> pollIntervalOptions = [5, 10, 15, 30, 60];

  /// 列表默认分页大小
  static const int defaultPageSize = 10;

  /// 门店列表一次拉取上限（门店数量远小于该值）
  static const int storePageSize = 100;

  /// Sa-Token 请求头名称，与后端 application.yml 的 sa-token.token-name 保持一致
  static const String tokenHeader = 'Token';
}
