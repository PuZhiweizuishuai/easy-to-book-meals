"""生成 Flutter 端用到的两声提醒音：new_order.wav / new_item.wav（16bit PCM 单声道）。"""

import math
import struct
import wave

SAMPLE_RATE = 44100


def env(i, length, attack, release):
    """简单的 ADSR 包络，避免爆音"""
    if i < attack:
        return i / attack
    tail = length - i
    if tail < release:
        return max(tail, 0) / release
    return 1.0


def tone(freq, start, dur, frames, length, attack=0.008, release=0.12, gain=0.55):
    n = int(dur * SAMPLE_RATE)
    for i in range(n):
        idx = start + i
        if idx >= length:
            break
        t = i / SAMPLE_RATE
        # 基音 + 一个八度泛音，声音更饱满
        v = math.sin(2 * math.pi * freq * t) * 0.85 + math.sin(2 * math.pi * freq * 2 * t) * 0.15
        frames[idx] += v * env(i, n, int(attack * SAMPLE_RATE), int(release * SAMPLE_RATE)) * gain


def write_wav(path, notes):
    """notes: [(freq, start_sec, dur_sec), ...]"""
    total = max(int((s + d) * SAMPLE_RATE) for _, s, d in notes) + int(0.25 * SAMPLE_RATE)
    frames = [0.0] * total
    for freq, start, dur in notes:
        tone(freq, int(start * SAMPLE_RATE), dur, frames, total)
    frames = [max(-1.0, min(1.0, v)) for v in frames]
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(b"".join(struct.pack("<h", int(v * 32000)) for v in frames))


# 新订单：上行三音，清脆但不刺耳
write_wav("new_order.wav", [
    (784.0, 0.00, 0.18),   # G5
    (987.8, 0.16, 0.18),   # B5
    (1318.5, 0.32, 0.30),  # E6
])

# 新菜品：两声短促的"叮叮"
write_wav("new_item.wav", [
    (1046.5, 0.00, 0.14),  # C6
    (1396.9, 0.13, 0.22),  # F6
])

print("done")
