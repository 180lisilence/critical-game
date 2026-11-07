# -*- coding: utf-8 -*-
"""生成透明底版本(仅去除与边缘连通的外部背景) + 素材清单 + 总览图"""
import os, json, csv
import numpy as np
import cv2

SRC = r"E:\重生之满级暴击\sucai_ui"
ALPHA_DIR = os.path.join(SRC, "alpha")
os.makedirs(ALPHA_DIR, exist_ok=True)

def imread_u(p):
    return cv2.imdecode(np.fromfile(p, dtype=np.uint8), cv2.IMREAD_UNCHANGED)

def imwrite_u(p, img):
    ext = os.path.splitext(p)[1] or ".png"
    ok, buf = cv2.imencode(ext, img)
    if ok:
        buf.tofile(p)
    return ok

def make_transparent(src_path, dst_path, tol=42):
    img = imread_u(src_path)
    if img is None:
        return None, "read fail"
    if img.shape[2] == 4:
        img = cv2.cvtColor(img, cv2.COLOR_BGRA2BGR)
    h, w = img.shape[:2]
    # 背景色 = 四边带中值
    pad = max(8, min(h, w)//40)
    strips = [img[0:pad, :, :].reshape(-1,3), img[h-pad:h, :, :].reshape(-1,3),
              img[:, 0:pad, :].reshape(-1,3), img[:, w-pad:w, :].reshape(-1,3)]
    bg = np.median(np.concatenate(strips), axis=0).astype(np.float32)
    # 与背景距离
    dist = np.abs(img.astype(np.float32) - bg).sum(axis=2)
    bgmask = (dist < tol).astype(np.uint8)
    # 只取与边缘连通的外部背景
    n, labels = cv2.connectedComponents(bgmask, connectivity=4)
    edge_labels = set(labels[0, :]) | set(labels[h-1, :]) | set(labels[:, 0]) | set(labels[:, w-1])
    edge_labels.discard(0)
    external = np.isin(labels, list(edge_labels)).astype(np.uint8)
    alpha = np.where(external == 1, 0, 255).astype(np.uint8)
    # 边缘羽化: 对 alpha 做 3x3 高斯, 只作用于边缘附近
    blurred = cv2.GaussianBlur(alpha, (3, 3), 0)
    edge = cv2.Canny(alpha, 50, 150)
    edge = cv2.dilate(edge, np.ones((3,3), np.uint8), iterations=1)
    alpha = np.where(edge > 0, blurred, alpha)
    out = np.dstack([img, alpha])
    imwrite_u(dst_path, out)
    return (w, h), ""

rows = []
for name in sorted(os.listdir(SRC)):
    if not name.lower().endswith(".png"):
        continue
    src = os.path.join(SRC, name)
    img = imread_u(src)
    h, w = img.shape[:2]
    dst = os.path.join(ALPHA_DIR, name)
    size, err = make_transparent(src, dst)
    # 统计透明比例
    a = imread_u(dst)
    a_ratio = float((a[:, :, 3] == 0).mean() * 100) if a is not None else -1
    rows.append({"file": name, "w": w, "h": h, "alpha_file": f"alpha/{name}",
                 "transparent_pct": round(a_ratio, 1), "err": err})
    print(f"{name} {w}x{h} 透明{a_ratio:.1f}% {err}", flush=True)

with open(os.path.join(SRC, "_sizes.json"), "w", encoding="utf-8") as f:
    json.dump(rows, f, ensure_ascii=False, indent=1)
print("done", len(rows))
