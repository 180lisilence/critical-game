# -*- coding: utf-8 -*-
"""透明底效果验证 + 总览图 + 素材清单CSV"""
import os, csv
import numpy as np
import cv2
from PIL import Image, ImageDraw

SRC = r"E:\重生之满级暴击\sucai_ui"
ALPHA = os.path.join(SRC, "alpha")

LABELS = {
 "btn_primary":"主按钮","btn_secondary":"次按钮","btn_close":"关闭","panel_dialog":"弹窗面板",
 "card_item":"物品卡片","bar_hp":"血条","bar_energy":"能量条","slider_volume":"音量滑块",
 "icon_starcrystal":"星晶","icon_gold":"金币","icon_hp":"生命","icon_shield":"护盾",
 "icon_crit":"暴击","icon_chest":"宝箱","icon_key":"钥匙","icon_settings":"设置",
 "icon_achievement":"成就","icon_rank":"排行榜","icon_pause":"暂停","icon_back":"返回",
 "icon_volume":"音量","icon_quality":"画质","icon_save":"存档","icon_help":"帮助",
 "buff_invincible":"无敌","buff_rage":"狂暴","debuff_slow":"减速","debuff_poison":"中毒",
 "buff_heal":"回血","buff_speed":"加速",
 "skill_meteor":"星陨爆破","skill_dash":"暴击突袭","skill_rift":"虚空裂隙",
 "skill_barrier":"星晶屏障","skill_rebirth":"满级重生","skill_chain":"连锁闪电",
}
GROUPS = {"btn":"基础组件","panel":"基础组件","card":"基础组件","bar":"基础组件","slider":"基础组件",
          "icon":"图标","buff":"状态","debuff":"状态","skill":"技能"}

files = sorted([f for f in os.listdir(ALPHA) if f.lower().endswith(".png")])

# 棋盘格背景验证：随机抽 6 张贴上去
def checker(w, h, s=16):
    a = np.indices((h, w)).sum(axis=0) // s % 2
    base = np.where(a[..., None] == 0, 60, 95).astype(np.uint8)
    return np.dstack([base, base, base])

check_files = ["icon_starcrystal.png", "icon_gold.png", "skill_rebirth.png",
               "btn_primary.png", "buff_invincible.png", "icon_settings.png"]
tiles = []
for f in check_files:
    im = cv2.imdecode(np.fromfile(os.path.join(ALPHA, f), dtype=np.uint8), cv2.IMREAD_UNCHANGED)
    im = cv2.resize(im, (420, 420), interpolation=cv2.INTER_AREA)
    b, g, r, a = cv2.split(im)
    fg = cv2.merge([b, g, r]).astype(np.float32)
    af = (a.astype(np.float32) / 255.0)[..., None]
    bg = checker(420, 420).astype(np.float32)
    comp = fg * af + bg * (1 - af)
    tiles.append(comp.astype(np.uint8))
row1 = np.hstack(tiles[:3])
row2 = np.hstack(tiles[3:])
check = np.vstack([row1, row2])
cv2.imencode(".png", check)[1].tofile(os.path.join(SRC, "_alpha_check.png"))

# 总览：全部36张，棋盘格3列网格
cols = 6
cell = 260
rows = (len(files) + cols - 1) // cols
W, H = cols * cell, rows * (cell + 26)
sheet = Image.new("RGB", (W, H), (18, 16, 24))
dr = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    im = Image.open(os.path.join(ALPHA, f)).convert("RGBA")
    im.thumbnail((cell - 20, cell - 20))
    # 棋盘格小底
    tw, th = im.size
    base = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    chk = Image.new("RGB", (tw, th))
    d2 = ImageDraw.Draw(chk)
    s = 14
    for y in range(0, th, s):
        for x in range(0, tw, s):
            c = (55, 52, 62) if ((x // s + y // s) % 2 == 0) else (82, 78, 90)
            d2.rectangle([x, y, x + s - 1, y + s - 1], fill=c)
    chk = chk.convert("RGBA")
    chk.alpha_composite(im)
    cx = (i % cols) * cell + (cell - tw) // 2
    cy = (i // cols) * (cell + 26) + (cell - th) // 2
    sheet.paste(chk.convert("RGB"), (cx, cy))
    name = f.replace(".png", "")
    dr.text(((i % cols) * cell + 8, (i // cols) * (cell + 26) + cell + 4),
            name, fill=(180, 180, 190))
sheet.save(os.path.join(SRC, "_overview.png"))

# CSV 清单
with open(os.path.join(SRC, "UI素材清单.csv"), "w", newline="", encoding="utf-8-sig") as fp:
    w = csv.writer(fp)
    w.writerow(["文件名", "中文名", "分组", "尺寸(px)", "透明底版本", "用途"])
    for f in files:
        name = f.replace(".png", "")
        img = Image.open(os.path.join(SRC, f))
        label = LABELS.get(name, name)
        grp = next((v for k, v in GROUPS.items() if name.startswith(k)), "其他")
        w.writerow([f, label, grp, f"{img.width}x{img.height}", f"alpha/{f}",
                    "LibGDX直接加载/Sprite或Scene2D控件底图"])
print("overview + csv done,", len(files), "files")
