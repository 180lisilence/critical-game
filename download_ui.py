# -*- coding: utf-8 -*-
"""下载生成的36张UI组件/图标，按LibGDX规范命名"""
import os, urllib.request, json

OUT = r"E:\重生之满级暴击\sucai_ui"
os.makedirs(OUT, exist_ok=True)

ASSETS = [
    # 批次1 基础组件
    ("btn_primary.png", "https://aka.doubaocdn.com/s/MhD23t2nWu", "主按钮", "基础组件"),
    ("btn_secondary.png", "https://aka.doubaocdn.com/s/0XDbbP7CV0", "次按钮", "基础组件"),
    ("btn_close.png", "https://aka.doubaocdn.com/s/t0Y5p5EAfh", "关闭按钮", "基础组件"),
    ("panel_dialog.png", "https://aka.doubaocdn.com/s/75gNNWOjIX", "弹窗面板", "基础组件"),
    ("card_item.png", "https://aka.doubaocdn.com/s/YT4Uj0t4Mv", "物品卡片", "基础组件"),
    ("bar_hp.png", "https://aka.doubaocdn.com/s/UJVFmAu4Hi", "血条", "基础组件"),
    ("bar_energy.png", "https://aka.doubaocdn.com/s/Uy87KaCibB", "能量条", "基础组件"),
    ("slider_volume.png", "https://aka.doubaocdn.com/s/FCH3QrTjmt", "音量滑块", "基础组件"),
    # 批次2 功能图标
    ("icon_starcrystal.png", "https://aka.doubaocdn.com/s/o1E8GVn1hU", "星晶货币", "功能图标"),
    ("icon_gold.png", "https://aka.doubaocdn.com/s/C2CdfGLKYV", "金币", "功能图标"),
    ("icon_hp.png", "https://aka.doubaocdn.com/s/K1yBnOwmYC", "生命值", "功能图标"),
    ("icon_shield.png", "https://aka.doubaocdn.com/s/CZ7gRLULJ0", "护盾", "功能图标"),
    ("icon_crit.png", "https://aka.doubaocdn.com/s/l8k5xAyL0S", "暴击", "功能图标"),
    ("icon_chest.png", "https://aka.doubaocdn.com/s/mUZ9dys7mX", "宝箱", "功能图标"),
    ("icon_key.png", "https://aka.doubaocdn.com/s/pKxiV4CTRK", "钥匙", "功能图标"),
    ("icon_settings.png", "https://aka.doubaocdn.com/s/jqqxiRca0V", "设置", "功能图标"),
    # 批次3 系统图标
    ("icon_achievement.png", "https://aka.doubaocdn.com/s/FNtJfUj9QU", "成就", "系统图标"),
    ("icon_rank.png", "https://aka.doubaocdn.com/s/TNmhXqcpni", "排行榜", "系统图标"),
    ("icon_pause.png", "https://aka.doubaocdn.com/s/FUm59iLOkX", "暂停", "系统图标"),
    ("icon_back.png", "https://aka.doubaocdn.com/s/LBIA3BZgBm", "返回", "系统图标"),
    ("icon_volume.png", "https://aka.doubaocdn.com/s/HphirEmTCq", "音量", "系统图标"),
    ("icon_quality.png", "https://aka.doubaocdn.com/s/XfIvclUcN7", "画质", "系统图标"),
    ("icon_save.png", "https://aka.doubaocdn.com/s/TFt3HCB8YU", "存档", "系统图标"),
    ("icon_help.png", "https://aka.doubaocdn.com/s/UBv1SKxGb2", "帮助", "系统图标"),
    # 批次4 状态图标
    ("buff_invincible.png", "https://aka.doubaocdn.com/s/jeFR2KOb3Q", "无敌", "状态图标"),
    ("buff_rage.png", "https://aka.doubaocdn.com/s/2Xm23AQ7ma", "狂暴", "状态图标"),
    ("debuff_slow.png", "https://aka.doubaocdn.com/s/DQOUbpK3Un", "减速", "状态图标"),
    ("debuff_poison.png", "https://aka.doubaocdn.com/s/guGr2nluIV", "中毒", "状态图标"),
    ("buff_heal.png", "https://aka.doubaocdn.com/s/hAT22H6KRI", "回血", "状态图标"),
    ("buff_speed.png", "https://aka.doubaocdn.com/s/rU5Cm8kcCe", "加速", "状态图标"),
    # 批次5 技能图标
    ("skill_meteor.png", "https://aka.doubaocdn.com/s/nYK6T4jXCF", "星陨爆破", "技能图标"),
    ("skill_dash.png", "https://aka.doubaocdn.com/s/yOYsPT9Vz5", "暴击突袭", "技能图标"),
    ("skill_rift.png", "https://aka.doubaocdn.com/s/VnRDE4SwL2", "虚空裂隙", "技能图标"),
    ("skill_barrier.png", "https://aka.doubaocdn.com/s/dwLN23rBp4", "星晶屏障", "技能图标"),
    ("skill_rebirth.png", "https://aka.doubaocdn.com/s/DyiNjmAwAC", "满级重生", "技能图标"),
    ("skill_chain.png", "https://aka.doubaocdn.com/s/S9VOGa6VeH", "连锁闪电", "技能图标"),
]

manifest = []
for name, url, label, group in ASSETS:
    dst = os.path.join(OUT, name)
    ok = False
    err = ""
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=60) as r:
            data = r.read()
        if len(data) > 10000:
            with open(dst, "wb") as f:
                f.write(data)
            ok = True
        else:
            err = f"too small: {len(data)} bytes"
    except Exception as e:
        err = repr(e)
    size = os.path.getsize(dst) if os.path.exists(dst) else 0
    manifest.append({"file": name, "group": group, "label": label, "url": url,
                     "bytes": size, "ok": ok, "err": err})
    print(f"{'OK ' if ok else 'FAIL'} {name} {size//1024}KB {err}", flush=True)

with open(os.path.join(OUT, "_manifest.json"), "w", encoding="utf-8") as f:
    json.dump(manifest, f, ensure_ascii=False, indent=1)
print("完成:", sum(1 for m in manifest if m["ok"]), "/", len(manifest))
