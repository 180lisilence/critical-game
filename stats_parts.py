# -*- coding: utf-8 -*-
import os, csv, json
from PIL import Image

D = r"E:\重生之满级暴击\sucai_parts"
SEL = r"E:\重生之满级暴击\sucai_parts_selected"
os.makedirs(SEL, exist_ok=True)

# 原图用途分类
def group_of(n):
    if n <= 3: return "场景背景"
    if n <= 20: return "道具/图标"
    if n <= 32: return "战斗界面"
    if n <= 41: return "成长面板"
    if n <= 64: return "功能页/结算"
    if n <= 75: return "设置与弹窗"
    if n <= 89: return "特效/状态图标"
    if n <= 100: return "角色与入口"
    if n <= 112: return "技能与BOSS"
    return "设置与HUD"

files = sorted([f for f in os.listdir(D) if f.endswith(".png")])
rows = []
buckets = {">=512": 0, "256-511": 0, "128-255": 0, "64-127": 0, "40-63": 0, "<40": 0}
big = 0
for f in files:
    im = Image.open(os.path.join(D, f))
    w, h = im.size
    m = min(w, h)
    if m >= 512: k = ">=512"
    elif m >= 256: k = "256-511"
    elif m >= 128: k = "128-255"
    elif m >= 64: k = "64-127"
    elif m >= 40: k = "40-63"
    else: k = "<40"
    buckets[k] += 1
    n = int(f[1:4])
    grp = group_of(n)
    usable = m >= 96
    if usable:
        big += 1
        im.save(os.path.join(SEL, f))
    rows.append({"file": f, "src": f"({n}).png", "group": grp, "w": w, "h": h, "usable": usable})

out = []
out.append("总块数 %d" % len(files))
for k, v in buckets.items():
    out.append("%s: %d" % (k, v))
out.append("可用组件(最小边>=96): %d" % big)

with open(os.path.join(D, "_sizes.csv"), "w", newline="", encoding="utf-8-sig") as fp:
    w = csv.writer(fp)
    w.writerow(["文件名", "来源图", "分类", "宽", "高", "可用"])
    for r in rows:
        w.writerow([r["file"], r["src"], r["group"], r["w"], r["h"], "是" if r["usable"] else "否"])

with open(os.path.join(D, "_stats.json"), "w", encoding="utf-8") as fp:
    json.dump({"total": len(files), "buckets": buckets, "usable": big}, fp, ensure_ascii=False, indent=1)

open(r"E:\重生之满级暴击\sucai_parts\_stats.txt", "w", encoding="utf-8").write("\n".join(out))
print("\n".join(out))
