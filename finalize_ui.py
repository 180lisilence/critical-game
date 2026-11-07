# -*- coding: utf-8 -*-
"""最终交付：框架类组件精细透明 + 色底总览 + 清单"""
import os, csv
import numpy as np
import cv2
from PIL import Image, ImageDraw

SRC = r"E:\重生之满级暴击\sucai_ui"
ALPHA = os.path.join(SRC, "alpha")

FRAME = ["btn_primary.png", "btn_secondary.png", "btn_close.png", "panel_dialog.png",
         "bar_hp.png", "bar_energy.png", "slider_volume.png"]

def imread_u(p):
    return cv2.imdecode(np.fromfile(p, dtype=np.uint8), cv2.IMREAD_UNCHANGED)

def imwrite_u(p, img):
    ok, buf = cv2.imencode(".png", img)
    if ok:
        buf.tofile(p)
    return ok

def frame_alpha(src_path, dst_path, tol=58):
    img = imread_u(src_path)
    if img.shape[2] == 4:
        img = cv2.cvtColor(img, cv2.COLOR_BGRA2BGR)
    h, w = img.shape[:2]
    s = max(3, int(min(h, w) / 8) * 2 + 1)
    # 局部背景估计：大核中值模糊（去主体，保留渐变）
    bg = cv2.medianBlur(img, min(99, s if s % 2 == 1 else s + 1))
    diff = np.abs(img.astype(np.float32) - bg.astype(np.float32)).sum(axis=2)
    fgmask = (diff > tol).astype(np.uint8) * 255
    # 形态学：闭合主体、去掉细碎噪点
    k = max(3, min(h, w) // 150)
    kern = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (k, k))
    fgmask = cv2.morphologyEx(fgmask, cv2.MORPH_CLOSE, kern, iterations=2)
    fgmask = cv2.morphologyEx(fgmask, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8), iterations=1)
    # 只保留最大连通域
    n, labels, stats, _ = cv2.connectedComponentsWithStats(fgmask, connectivity=8)
    if n > 1:
        biggest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        fgmask = np.where(labels == biggest, 255, 0).astype(np.uint8)
    # 填洞：从外部flood fill，未填充部分即主体实心
    filled = fgmask.copy()
    ff = np.zeros((h + 2, w + 2), np.uint8)
    cv2.floodFill(filled, ff, (0, 0), 255)
    holes = cv2.bitwise_not(filled)
    solid = cv2.bitwise_or(fgmask, holes)
    alpha = cv2.GaussianBlur(solid, (5, 5), 0)
    alpha = cv2.erode(alpha, np.ones((2, 2), np.uint8), iterations=1)
    out = np.dstack([img, alpha])
    imwrite_u(dst_path, out)
    return float((alpha == 0).mean() * 100)

for f in FRAME:
    p = frame_alpha(os.path.join(SRC, f), os.path.join(ALPHA, f))
    print(f, "透明%.1f%%" % p, flush=True)

# 总览：36张色底
LABELS = {"btn_primary":"主按钮","btn_secondary":"次按钮","btn_close":"关闭","panel_dialog":"弹窗面板",
 "card_item":"物品卡片","bar_hp":"血条","bar_energy":"能量条","slider_volume":"音量滑块",
 "icon_starcrystal":"星晶","icon_gold":"金币","icon_hp":"生命","icon_shield":"护盾","icon_crit":"暴击",
 "icon_chest":"宝箱","icon_key":"钥匙","icon_settings":"设置","icon_achievement":"成就","icon_rank":"排行榜",
 "icon_pause":"暂停","icon_back":"返回","icon_volume":"音量","icon_quality":"画质","icon_save":"存档",
 "icon_help":"帮助","buff_invincible":"无敌","buff_rage":"狂暴","debuff_slow":"减速","debuff_poison":"中毒",
 "buff_heal":"回血","buff_speed":"加速","skill_meteor":"星陨爆破","skill_dash":"暴击突袭",
 "skill_rift":"虚空裂隙","skill_barrier":"星晶屏障","skill_rebirth":"满级重生","skill_chain":"连锁闪电"}
files = sorted([f for f in os.listdir(SRC) if f.lower().endswith(".png") and not f.startswith("_")])
cols, cell = 6, 250
rows = (len(files) + cols - 1) // cols
sheet = Image.new("RGB", (cols*cell, rows*(cell+24)), (16, 14, 22))
dr = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    im = Image.open(os.path.join(SRC, f)).convert("RGB")
    im.thumbnail((cell-16, cell-16))
    cx = (i % cols)*cell + (cell-im.width)//2
    cy = (i // cols)*(cell+24) + (cell-im.height)//2
    sheet.paste(im, (cx, cy))
    dr.text(((i % cols)*cell + 6, (i//cols)*(cell+24) + cell + 3),
            "%s  %s" % (f.replace(".png", ""), LABELS.get(f.replace(".png",""), "")), fill=(175, 172, 185))
sheet.save(os.path.join(SRC, "_overview.png"))

with open(os.path.join(SRC, "UI素材清单.csv"), "w", newline="", encoding="utf-8-sig") as fp:
    wr = csv.writer(fp)
    wr.writerow(["文件名", "中文名", "分组", "尺寸(px)", "透明底", "用途"])
    for f in files:
        name = f.replace(".png", "")
        im = Image.open(os.path.join(SRC, f))
        if name.startswith(("btn", "panel", "bar", "slider")):
            grp, alpha = "框架组件", "alpha/" + f
        elif name.startswith("card"):
            grp, alpha = "卡片模板", "否（带底图）"
        elif name.startswith("buff") or name.startswith("debuff"):
            grp, alpha = "状态图标", "否（圆形徽章底）"
        elif name.startswith("skill"):
            grp, alpha = "技能图标", "否（方形框底）"
        else:
            grp, alpha = "功能图标", "否（深色底）"
        wr.writerow([f, LABELS.get(name, name), grp, "%dx%d" % (im.width, im.height), alpha,
                     "LibGDX Sprite / Scene2D 控件底图"])
print("done")
