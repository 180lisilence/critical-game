# -*- coding: utf-8 -*-
"""用 grabCut 重做透明底：单主体居中，保留发光边缘，去掉渐变背景残留"""
import os
import numpy as np
import cv2

SRC = r"E:\重生之满级暴击\sucai_ui"
OUT = os.path.join(SRC, "alpha")

def imread_u(p):
    return cv2.imdecode(np.fromfile(p, dtype=np.uint8), cv2.IMREAD_UNCHANGED)

def imwrite_u(p, img):
    ok, buf = cv2.imencode(".png", img)
    if ok:
        buf.tofile(p)
    return ok

def grabcut_alpha(src, dst):
    img = imread_u(src)
    if img is None:
        return -1
    if img.shape[2] == 4:
        img = cv2.cvtColor(img, cv2.COLOR_BGRA2BGR)
    h, w = img.shape[:2]
    # 缩小到长边900做grabCut，再放大掩码，提速
    scale = 900.0 / max(h, w)
    sw, sh = max(1, int(w*scale)), max(1, int(h*scale))
    small = cv2.resize(img, (sw, sh), interpolation=cv2.INTER_AREA)
    mask = np.zeros((sh, sw), np.uint8)
    bgd, fgd = np.zeros((1, 65), np.float64), np.zeros((1, 65), np.float64)
    m = int(min(sw, sh) * 0.04)
    rect = (m, m, sw - 2*m, sh - 2*m)
    try:
        cv2.grabCut(small, mask, rect, bgd, fgd, 5, cv2.GC_INIT_WITH_RECT)
    except Exception:
        return -1
    # 前景=确定/可能前景
    fg = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0).astype(np.uint8)
    # 只保留最大连通域，去掉边角碎块
    n, labels, stats, _ = cv2.connectedComponentsWithStats(fg, connectivity=8)
    if n > 1:
        biggest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        fg = np.where(labels == biggest, 255, 0).astype(np.uint8)
    # 放大回原尺寸
    alpha_small = cv2.GaussianBlur(fg, (3, 3), 0)
    alpha = cv2.resize(alpha_small, (w, h), interpolation=cv2.INTER_LINEAR)
    # 收边1px去毛刺 + 羽化
    alpha = cv2.erode(alpha, np.ones((3, 3), np.uint8), iterations=1)
    alpha = cv2.GaussianBlur(alpha, (5, 5), 0)
    out = np.dstack([img, alpha])
    imwrite_u(dst, out)
    return float((alpha == 0).mean() * 100)

files = sorted([f for f in os.listdir(SRC) if f.lower().endswith(".png") and not f.startswith("_")])
for f in files:
    p = grabcut_alpha(os.path.join(SRC, f), os.path.join(OUT, f))
    print(f, "透明%.1f%%" % p if p >= 0 else "FAIL", flush=True)
print("done", len(files))
