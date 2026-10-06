import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_config.dart';
import '../utils/server_address.dart';

/// 本地持久化服务
///
/// 统一收口 SharedPreferences 读写：服务器地址、登录态、当前门店、提醒偏好。
/// 业务层只读 settings dict，不直接持有 SharedPreferences 实例，便于替换与测试。
class PreferencesService {
  PreferencesService(this._prefs);

  final SharedPreferences _prefs;

  // ==================== 键名 ====================
  static const _kServerAddress = 'server_address';
  static const _kToken = 'token';
  static const _kUserInfo = 'user_info';
  static const _kCurrentStoreId = 'current_store_id';
  static const _kSoundEnabled = 'sound_enabled';
  static const _kNotifyEnabled = 'notify_enabled';
  static const _kPollSeconds = 'poll_seconds';

  // ==================== 服务器地址 ====================

  /// 用户填写的后端地址，形如 http://192.168.1.10:8081，未配置时为空
  String get serverAddress => _prefs.getString(_kServerAddress) ?? '';

  /// 是否已配置服务器地址
  bool get hasServerAddress => serverAddress.isNotEmpty;

  /// 拼接后的接口根地址
  String get apiBaseUrl => ServerAddress.apiBase(serverAddress);

  Future<void> setServerAddress(String address) =>
      _prefs.setString(_kServerAddress, ServerAddress.normalize(address));

  // ==================== 登录态 ====================

  String get token => _prefs.getString(_kToken) ?? '';

  bool get hasToken => token.isNotEmpty;

  Future<void> setToken(String value) => _prefs.setString(_kToken, value);

  /// 用户信息 JSON 字符串（登录成功后缓存，避免每次启动请求 /auth/info）
  String get userJson => _prefs.getString(_kUserInfo) ?? '';

  Future<void> setUserJson(String value) =>
      _prefs.setString(_kUserInfo, value);

  Map<String, dynamic>? get userMap {
    final raw = userJson;
    if (raw.isEmpty) return null;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) return decoded;
    } catch (_) {
      // 缓存损坏时按未登录处理
    }
    return null;
  }

  Future<void> setUserMap(Map<String, dynamic> map) =>
      setUserJson(jsonEncode(map));

  /// 清除登录态（保留服务器地址与提醒偏好）
  Future<void> clearSession() async {
    await _prefs.remove(_kToken);
    await _prefs.remove(_kUserInfo);
    await _prefs.remove(_kCurrentStoreId);
  }

  // ==================== 当前门店 ====================

  int? get currentStoreId => _prefs.getInt(_kCurrentStoreId);

  Future<void> setCurrentStoreId(int? id) async {
    if (id == null) {
      await _prefs.remove(_kCurrentStoreId);
    } else {
      await _prefs.setInt(_kCurrentStoreId, id);
    }
  }

  // ==================== 提醒偏好 ====================

  /// 是否开启声音提醒
  bool get soundEnabled => _prefs.getBool(_kSoundEnabled) ?? true;

  Future<void> setSoundEnabled(bool value) =>
      _prefs.setBool(_kSoundEnabled, value);

  /// 是否开启系统通知
  bool get notifyEnabled => _prefs.getBool(_kNotifyEnabled) ?? true;

  Future<void> setNotifyEnabled(bool value) =>
      _prefs.setBool(_kNotifyEnabled, value);

  /// 轮询间隔（秒）
  int get pollSeconds =>
      _prefs.getInt(_kPollSeconds) ?? AppConfig.defaultPollInterval.inSeconds;

  Future<void> setPollSeconds(int seconds) =>
      _prefs.setInt(_kPollSeconds, seconds);
}
