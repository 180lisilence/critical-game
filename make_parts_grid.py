# -*- coding: utf-8 -*-
"""生成组件总览图与清单"""
import os, csv
from PIL import Image, ImageDraw

D = r"E:\重生之满级暴击\sucai_parts"
SEL = r"E:\重生之满级暴击\sucai_parts_selected"

def grid(paths, cell, cols, out_path, pad=6, label=False):
    rows = (len(paths) + cols - 1) // cols
    W = cols * (cell + pad) + pad
    H = rows * ((cell + (14 if label else 0)) + pad) + pad
    sheet = Image.new("RGB", (W, H), (16, 14, 22))
    dr = ImageDraw.Draw(sheet)
    for i, p in enumerate(paths):
        im = Image.open(p).convert("RGB")
        im.thumbnail((cell, cell))
        cx = pad + (i % cols) * (cell + pad) + (cell - im.width) // 2
        cy = pad + (i // cols) * ((cell + (14 if label else 0)) + pad) + (cell - im.height) // 2
        sheet.paste(im, (cx, cy))
        if label:
            dr.text((pad + (i % cols) * (cell + pad), cy + cell + 1), os.path.basename(p)[:-4], fill=(150, 148, 160))
    sheet.save(out_path)
    return sheet.size

files = sorted([f for f in os.listdir(SEL) if f.endswith(".png")])
big, mid, small = [], [], []
for f in files:
    m = min(Image.open(os.path.join(SEL, f)).size)
    (big if m >= 512 else mid if m >= 200 else small).append(os.path.join(SEL, f))

print("big", len(big), "mid", len(mid), "small", len(small))
print(grid(big, 210, 10, os.path.join(D, "_grid_big.png"), label=True))
print(grid(mid[:180], 110, 18, os.path.join(D, "_grid_mid.png"), label=True))
print(grid(small[:252], 84, 21, os.path.join(D, "_grid_small.png"), label=False))

# 清单
with open(os.path.join(D, "组件清单.csv"), "w", newline="", encoding="utf-8-sig") as fp:
    w = csv.writer(fp)
    w.writerow(["文件名", "来源图", "分类", "宽", "高", "尺寸档", "可用"])
    for f in files:
        im = Image.open(os.path.join(SEL, f))
        ww, hh = im.size
        m = min(ww, hh)
        n = int(f[1:4])
        grp = ("场景背景" if n <= 3 else "道具/图标" if n <= 20 else "战斗界面" if n <= 32
               else "成长面板" if n <= 41 else "功能页/结算" if n <= 64 else "设置与弹窗" if n <= 75
               else "特效/状态图标" if n <= 89 else "角色与入口" if n <= 100 else "技能与BOSS" if n <= 112
               else "设置与HUD")
        tier = "大" if m >= 512 else "中" if m >= 200 else "小"
        w.writerow([f, "(%d).png" % n, grp, ww, hh, tier, "是"])
print("csv done")
