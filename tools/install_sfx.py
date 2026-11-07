# -*- coding: utf-8 -*-
"""
把 Kenney CC0 音效包（Sci-Fi Sounds / RPG Audio）映射落地为
《重生之满级暴击》音效方案.txt 约定的标准文件名，输出到 assets/sound/。

依赖：pip install soundfile numpy （libsndfile 自带 ogg/vorbis + wav 读写）

为什么输出 .wav 而不是直接拷 .ogg：
  GameAssets.loadSounds() 按扩展名分流 ——
    .wav  -> Gdx.audio.newSound()  -> SoundPool，短音效专用，可并发
    .ogg  -> Gdx.audio.newMusic()  -> MediaPlayer，长音频/循环专用
  战斗音效必须走 Sound 才能叠加播放，所以统一转 PCM16 WAV。
"""
import os
import sys
import zipfile

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TMP = os.path.join(ROOT, "tools", "tmp")
OUT = os.path.join(ROOT, "assets", "sound")

SCI = os.path.join(TMP, "kenney_sci-fi-sounds.zip")
RPG = os.path.join(TMP, "kenney_rpg-audio.zip")

TARGET_SR = 44100

# 目标名(不含扩展) -> (来源包, 源文件名, 目标峰值dB, 最大时长秒)
MAP = [
    # ---- 一、战斗 ----
    ("sfx_hit_normal",     RPG, "knifeSlice.ogg",            -6.0, 0.8),
    ("sfx_critical_hit",   SCI, "impactMetal_000.ogg",       -3.0, 1.2),
    ("sfx_player_hurt",    SCI, "lowFrequency_explosion_000.ogg", -3.0, 1.2),
    ("sfx_enemy_death",    SCI, "explosionCrunch_000.ogg",   -4.0, 1.2),
    ("sfx_boss_hurt",      SCI, "explosionCrunch_002.ogg",   -3.0, 1.2),
    ("sfx_boss_death",     SCI, "lowFrequency_explosion_001.ogg", -2.0, 2.0),
    ("sfx_skill_cast",     SCI, "forceField_000.ogg",        -5.0, 1.0),
    ("sfx_projectile",     SCI, "laserSmall_000.ogg",        -9.0, 0.6),
    # ---- 二、拾取 & 成长 ----
    ("sfx_pickup_exp",     RPG, "handleCoins.ogg",           -8.0, 1.0),
    ("sfx_pickup_crystal", SCI, "impactMetal_002.ogg",       -6.0, 0.8),
    ("sfx_levelup",        SCI, "forceField_002.ogg",        -4.0, 1.5),
    ("sfx_relic_get",      SCI, "computerNoise_000.ogg",     -8.0, 1.5),
    ("sfx_shop_buy",       RPG, "handleCoins2.ogg",          -7.0, 0.8),
    # ---- 三、UI ----
    ("sfx_ui_click",       RPG, "metalClick.ogg",            -10.0, 0.5),
    ("sfx_ui_hover",       RPG, "metalLatch.ogg",            -14.0, 0.4),
    ("sfx_ui_confirm",     SCI, "forceField_004.ogg",        -7.0, 1.0),
    ("sfx_ui_cancel",      RPG, "doorClose_1.ogg",           -10.0, 0.6),
    ("sfx_ui_error",       SCI, "computerNoise_002.ogg",     -8.0, 0.8),
    ("sfx_ui_unlock",      SCI, "forceField_001.ogg",        -5.0, 1.2),
    # ---- 五、特殊状态 ----
    ("sfx_buff_invincible", SCI, "forceField_003.ogg",       -6.0, 1.0),
    ("sfx_buff_heal",      RPG, "bookOpen.ogg",              -9.0, 0.9),
    ("sfx_debuff_slow",    SCI, "computerNoise_003.ogg",     -9.0, 1.2),
    ("sfx_respawn",        SCI, "spaceEngine_000.ogg",       -3.0, 1.5),
    ("sfx_teleport",       SCI, "doorOpen_000.ogg",          -7.0, 1.0),
]

# BGM：长循环，保持 .ogg，GameAssets 会走 Music 分支
BGM = [
    ("bgm_void_gap",       "虚空初隙：低频空旷持续氛围"),
    ("bgm_star_abyss",     "星陨深渊：微弱鼓点，紧张感"),
    ("bgm_chaos_boundary", "混沌边界：快节奏暗流压迫"),
    ("bgm_boss_theme",     "BOSS 战：重低音 + 脉冲"),
    ("bgm_menu",           "主菜单：空灵深空氛围"),
]


def normalize(x, peak_db):
    peak = float(np.max(np.abs(x))) if x.size else 0.0
    if peak <= 1e-9:
        return x
    target = 10 ** (peak_db / 20.0)
    return x * (target / peak)


def trim(x, sr, max_sec):
    """保留前 max_sec，尾部加 15ms 淡出，消除刀切爆音。"""
    n = int(sr * max_sec)
    if x.shape[0] > n:
        x = x[:n].copy()
    fade = max(1, int(sr * 0.015))
    if x.shape[0] > fade:
        ramp = np.linspace(1.0, 0.0, fade, dtype=x.dtype)
        x[-fade:] *= ramp
    # 开头 3ms 淡入，避免波形从非零点起跳
    fi = max(1, int(sr * 0.003))
    if x.shape[0] > fi:
        x[:fi] *= np.linspace(0.0, 1.0, fi, dtype=x.dtype)
    return x


def to_mono(x):
    if x.ndim == 2:
        return x.mean(axis=1)
    return x


def resample_linear(x, sr, target):
    if sr == target or x.size == 0:
        return x, sr
    n = int(round(x.shape[0] * target / float(sr)))
    idx = np.linspace(0, x.shape[0] - 1, n)
    i0 = np.floor(idx).astype(np.int64)
    i1 = np.minimum(i0 + 1, x.shape[0] - 1)
    frac = (idx - i0).astype(x.dtype)
    return x[i0] * (1 - frac) + x[i1] * frac, target


def main():
    os.makedirs(OUT, exist_ok=True)

    packs = {}
    for p in (SCI, RPG):
        if not os.path.exists(p):
            print("[FATAL] 缺少音效包: %s" % p)
            return 1
        packs[p] = zipfile.ZipFile(p)

    done, fail = 0, []
    for name, pack, member, peak_db, max_sec in MAP:
        z = packs[pack]
        cand = [n for n in z.namelist() if n.endswith("/" + member) or n == member]
        if not cand:
            fail.append((name, member, "包里找不到该文件"))
            continue
        src = cand[0]
        try:
            with z.open(src) as f:
                data, sr = sf.read(f, dtype="float32", always_2d=False)
        except Exception as e:
            fail.append((name, member, "解码失败: %s" % e))
            continue

        x = to_mono(data)
        x, sr = resample_linear(x, sr, TARGET_SR)
        x = trim(x, sr, max_sec)
        x = normalize(x, peak_db)

        dst = os.path.join(OUT, name + ".wav")
        sf.write(dst, x.astype(np.float32), sr, subtype="PCM_16")
        sz = os.path.getsize(dst)
        dur = x.shape[0] / float(sr)
        print("[OK] %-22s <- %-28s %6.1fKB  %.2fs" % (name + ".wav", os.path.basename(src), sz / 1024.0, dur))
        done += 1

    print("\n音效完成 %d/%d" % (done, len(MAP)))

    # BGM 检查
    print("\n--- BGM 检查（需 .ogg 长循环，GameAssets 走 Music 分支）---")
    for name, desc in BGM:
        p = os.path.join(OUT, name + ".ogg")
        if os.path.exists(p):
            print("[OK]   %s.ogg  (%.1fKB)" % (name, os.path.getsize(p) / 1024.0))
        else:
            print("[MISS] %s.ogg  — %s" % (name, desc))

    if fail:
        print("\n失败项:")
        for n, m, r in fail:
            print("  %s <- %s: %s" % (n, m, r))
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
