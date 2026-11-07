# -*- coding: utf-8 -*-
"""
落地 BGM 长循环到 assets/sound/，命名严格对齐 音效方案.txt 第四节。

GameAssets.loadSounds() 分流规则：
    .wav -> Sound（SoundPool，短音效）
    .ogg -> Music（MediaPlayer，长音频，playMusic 使用）
所以 BGM 必须保持 .ogg，不能转 wav（转 wav 会被当 Sound 加载，爆内存）。

统一处理：
  1. 解码 → 单声道 44.1kHz
  2. 峰值归一（BGM 压到 -14dB，远低于音效，符合"音量低于音效"要求）
  3. 改写 Vorbis 编码，质量 q5（约 160kbps），体积可控
  4. 首尾各 30ms 淡入/淡出，消除循环接缝爆音
"""
import os
import sys
import zipfile

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TMP = os.path.join(ROOT, "tools", "tmp")
OUT = os.path.join(ROOT, "assets", "sound")

TARGET_SR = 44100
BGM_PEAK_DB = -14.0

# 目标名 -> 候选源（按优先级）
# 主选来自 OpenGameArt "Dark Sci-Fi Audio Pack"（CC0）
# 兜底来自 OpenGameArt 单曲（CC0）
SOURCES = {
    "bgm_void_gap": [
        ("zip", os.path.join(TMP, "dsp_ogg.zip"), "airy"),
        ("file", os.path.join(TMP, "c_darkplace.ogg"), None),
    ],
    "bgm_star_abyss": [
        ("zip", os.path.join(TMP, "dsp_ogg.zip"), "sector"),
        ("file", os.path.join(TMP, "c_emptycity.ogg"), None),
    ],
    "bgm_chaos_boundary": [
        ("zip", os.path.join(TMP, "dsp_ogg.zip"), "urgent"),
        ("file", os.path.join(TMP, "c_caverns.ogg"), None),
    ],
    "bgm_boss_theme": [
        ("zip", os.path.join(TMP, "dsp_ogg.zip"), "pulse"),
        ("file", os.path.join(TMP, "c_caverns.ogg"), None),
    ],
    "bgm_menu": [
        ("zip", os.path.join(TMP, "dsp_ogg.zip"), "title"),
        ("file", os.path.join(TMP, "c_darkplace.ogg"), None),
    ],
}


def load_from_zip(zpath, keyword):
    z = zipfile.ZipFile(zpath)
    hits = [n for n in z.namelist() if n.lower().endswith((".ogg", ".wav", ".mp3"))
            and keyword.lower() in os.path.basename(n).lower()]
    if not hits:
        return None
    hits.sort(key=lambda x: (0 if "ogg" in x.lower() else 1, len(x)))
    with z.open(hits[0]) as f:
        data, sr = sf.read(f, dtype="float32", always_2d=False)
    return data, sr, hits[0]


def to_mono(x):
    return x.mean(axis=1) if x.ndim == 2 else x


def resample(x, sr, target):
    if sr == target or x.size == 0:
        return x
    n = int(round(x.shape[0] * target / float(sr)))
    idx = np.linspace(0, x.shape[0] - 1, n)
    i0 = np.floor(idx).astype(np.int64)
    i1 = np.minimum(i0 + 1, x.shape[0] - 1)
    frac = (idx - i0).astype(x.dtype)
    return x[i0] * (1 - frac) + x[i1] * frac


def process(x, sr):
    x = to_mono(x).astype(np.float32)
    x = resample(x, sr, TARGET_SR)
    peak = float(np.max(np.abs(x))) if x.size else 0.0
    if peak > 1e-9:
        x = x * ((10 ** (BGM_PEAK_DB / 20.0)) / peak)
    f = int(TARGET_SR * 0.03)
    if x.shape[0] > f * 2:
        x[:f] *= np.linspace(0, 1, f, dtype=x.dtype)
        x[-f:] *= np.linspace(1, 0, f, dtype=x.dtype)
    return x


def main():
    os.makedirs(OUT, exist_ok=True)
    ok, fail = 0, []
    for name, cands in SOURCES.items():
        got = False
        for kind, path, kw in cands:
            if not os.path.exists(path):
                continue
            try:
                if kind == "zip":
                    try:
                        r = load_from_zip(path, kw)
                    except Exception:
                        r = None
                    if r is None:
                        continue
                    data, sr, srcname = r
                else:
                    data, sr = sf.read(path, dtype="float32", always_2d=False)
                    srcname = os.path.basename(path)
            except Exception as e:
                print("[WARN] %s 读取失败(%s): %s" % (name, os.path.basename(path), e))
                continue

            x = process(data, sr)
            dst = os.path.join(OUT, name + ".ogg")
            sf.write(dst, x, TARGET_SR, format="OGG", subtype="VORBIS")
            sz = os.path.getsize(dst)
            print("[OK] %-20s <- %-24s %6.1fKB  %5.1fs" % (
                name + ".ogg", srcname, sz / 1024.0, x.shape[0] / float(TARGET_SR)))
            ok += 1
            got = True
            break
        if not got:
            fail.append(name)

    print("\nBGM 完成 %d/%d" % (ok, len(SOURCES)))
    if fail:
        print("未生成:", ", ".join(fail))
    return 0 if not fail else 2


if __name__ == "__main__":
    sys.exit(main())
