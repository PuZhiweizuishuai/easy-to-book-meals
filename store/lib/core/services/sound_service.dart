import 'package:audioplayers/audioplayers.dart';

import 'app_logger.dart';

/// 提醒音类型
enum AlertTone {
  /// 新订单
  newOrder('sounds/new_order.wav'),

  /// 后厨新增待制作菜品
  newItem('sounds/new_item.wav');

  const AlertTone(this.asset);

  /// asset 资源路径
  final String asset;
}

/// 提醒音服务
///
/// 新订单 / 新菜品到达时播放提示音。声音素材放在 assets/sounds 下，
/// 通过 AssetSource 播放，无需读写外部存储；可在「设置」中整体关闭。
class SoundService {
  SoundService();

  final AudioPlayer _player = AudioPlayer();

  bool _enabled = true;

  /// 是否允许播放声音
  bool get enabled => _enabled;

  /// 由外层根据偏好设置同步开关状态
  set enabled(bool value) => _enabled = value;

  /// 预加载付费音到缓存，避免首次提醒时有明显延迟
  Future<void> warmUp() async {
    for (final tone in AlertTone.values) {
      await _player.setSource(AssetSource(tone.asset));
    }
    await _player.stop();
  }

  /// 播放提醒音，重复调用会打断上一次播放
  Future<void> play(AlertTone tone) async {
    if (!_enabled) return;
    try {
      await _player.stop();
      await _player.play(AssetSource(tone.asset));
    } catch (e, s) {
      AppLogger.w('播放提醒音失败', error: e, stackTrace: s);
    }
  }

  void dispose() => _player.dispose();
}
