# -*- coding: utf-8 -*-
"""《重生之满级暴击》素材拆分工具
把整图(含多个挤压组件/图标的AI生图)拆成独立透明底PNG。
原理: 背景色键(色距阈值) -> 形态学清理 -> 连通域 -> 逐块裁剪+边缘羽化。
用法: python split_tool.py 输入目录 输出目录 [--min-area 最小面积比例]
"""
import os, re, sys
import numpy as np
import cv2

def estimate_bg_color(img, pad=24):
    """用四周边带的中值估计背景色(抗主体边缘光晕干扰)"""
    h, w = img.shape[:2]
    strips = [
        img[0:pad, :, :].reshape(-1, 3),
        img[h-pad:h, :, :].reshape(-1, 3),
        img[:, 0:pad, :].reshape(-1, 3),
        img[:, w-pad:w, :].reshape(-1, 3),
    ]
    px = np.concatenate(strips)
    return np.median(px, axis=0).astype(np.float32)

def make_alpha_mask(img, bg, color_tol=70, lum_tol=90):
    """前景掩码: 像素与背景色距离超过阈值则为前景(带软过渡alpha)"""
    f = img.astype(np.float32)
    dist = np.abs(f - bg).sum(axis=2)  # 曼哈顿色距
    lum_bg = bg.sum() / 3.0
    lum_fg = f.sum(axis=2) / 3.0
    lum_diff = np.abs(lum_fg - lum_bg)
    # 前景强度 = 色距超出容差的部分 + 亮度差超出容差的部分
    score = np.clip(dist - color_tol, 0, 255) + np.clip(lum_diff - lum_tol, 0, 255)
    # 软alpha: 得分映射 0..120 -> 0..255
    alpha = np.clip(score, 0, 120) / 120.0 * 255.0
    return alpha.astype(np.uint8)

def imread_unicode(path):
    """兼容中文路径读取"""
    data = np.fromfile(path, dtype=np.uint8)
    return cv2.imdecode(data, cv2.IMREAD_UNCHANGED)

def imwrite_unicode(path, img):
    ext = os.path.splitext(path)[1] or '.png'
    ok, buf = cv2.imencode(ext, img)
    if ok:
        buf.tofile(path)
    return ok

def split_image(src_path, out_dir, min_area_ratio=0.002, margin=12):
    """拆分单张图, 返回输出文件列表"""
    img = imread_unicode(src_path)
    if img is None:
        return []
    if img.shape[2] == 4:
        img = cv2.cvtColor(img, cv2.COLOR_BGRA2BGR)
    h, w = img.shape[:2]
    bg = estimate_bg_color(img)
    alpha = make_alpha_mask(img, bg)
    # 形态学: 去噪点+闭合主体内部空洞
    k = max(3, min(h, w) // 200)
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (k, k))
    mask = cv2.morphologyEx(alpha, cv2.MORPH_OPEN, kernel, iterations=1)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel, iterations=1)
    _, binm = cv2.threshold(mask, 30, 255, cv2.THRESH_BINARY)
    # 连通域
    n, labels, stats, _ = cv2.connectedComponentsWithStats(binm, connectivity=8)
    min_area = h * w * min_area_ratio
    results = []
    for i in range(1, n):
        x, y, bw, bh, area = stats[i]
        if area < min_area:
            continue
        x0 = max(0, x - margin); y0 = max(0, y - margin)
        x1 = min(w, x + bw + margin); y1 = min(h, y + bh + margin)
        comp_alpha = alpha[y0:y1, x0:x1].astype(np.float32)
        comp_bgr = img[y0:y1, x0:x1].astype(np.float32)
        # 边缘3px腐蚀alpha, 压掉背景残留
        comp_alpha = cv2.erode(comp_alpha.astype(np.uint8), np.ones((3, 3), np.uint8), iterations=1)
        out = np.dstack([comp_bgr, comp_alpha]).astype(np.uint8)
        results.append((x0, y0, x1, y1, out))
    return results

def main():
    src_dir = sys.argv[1] if len(sys.argv) > 1 else r"E:\重生之满级暴击\sucai"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else r"E:\重生之满级暴击\sucai_split"
    files = sorted([f for f in os.listdir(src_dir)
                    if f.lower().endswith('.png') and re.match(r'\(\d+\)', f)],
                   key=lambda f: int(re.match(r'\((\d+)\)', f).group(1)))
    os.makedirs(out_dir, exist_ok=True)
    total = 0
    for f in files:
        n = int(re.match(r'\((\d+)\)', f).group(1))
        parts = split_image(os.path.join(src_dir, f), out_dir)
        for idx, (x0, y0, x1, y1, out) in enumerate(parts, 1):
            name = f"{n}_{idx}.png"
            imwrite_unicode(os.path.join(out_dir, name), out)
            total += 1
        print(f"({n}) -> {len(parts)} 块")
    print("共拆分:", total, "个组件")

if __name__ == "__main__":
    main()
