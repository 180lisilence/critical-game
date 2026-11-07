#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
资源管线工具：把 sucai/ 下的成品图加工成 LibGDX 可直接使用的资源。

产出：
  assets/textures/icons/*.png   2048 图标类（角色/怪物/特效/Logo）按语义重命名并缩放
  assets/textures/ui/*.png       2848x1600 UI 成品图缩放到 1280x720
  assets/ui/font/deskhan.fnt     BitmapFont（黑体，含项目中用到的全部汉字）
  assets/ui/font/deskhan.png     位图字体图集

用法： python tools/build_assets.py
"""
import os, re, sys, glob
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'sucai')
TEX = os.path.join(ROOT, 'assets', 'textures')
UI = os.path.join(ROOT, 'assets', 'ui')

# ---------- 语义命名映射（人工归类后的结果） ----------
ICONS = {
    # 角色 / 敌人
    107: 'hero_necro', 109: 'enemy_stone', 110: 'enemy_demon', 112: 'enemy_golem',
    106: 'fx_burst', 108: 'fx_fireball', 111: 'fx_vortex',
    # 场景
    85: 'scene_nebula', 86: 'scene_obelisk', 87: 'scene_crystal', 88: 'scene_pillar',
    89: 'icon_crystal_red', 94: 'icon_rune_tab',
    # Logo / 标题
    91: 'logo_title', 95: 'logo_glow', 96: 'logo_banner', 101: 'title_crit',
    90: 'icon_rune_grid', 93: 'icon_equip_slot',
    # 装备位图标
    10: 'equip_weapon', 11: 'equip_helmet', 12: 'equip_cape', 13: 'equip_amulet',
    14: 'equip_boots', 15: 'equip_ring',
    # 技能图标
    76: 'skill_crit', 77: 'skill_swift', 78: 'skill_void', 79: 'skill_bulwark', 80: 'skill_rebirth',
    # 场景装饰
    92: 'icon_transcend', 97: 'icon_tower', 99: 'icon_realm', 100: 'icon_tree',
    102: 'ui_victory', 103: 'ui_summon', 104: 'ui_ascend', 105: 'ui_death',
    81: 'fx_heal', 82: 'fx_bullet', 83: 'fx_debuff', 84: 'fx_portal', 88: 'scene_pillar',
}
# 这些是完整 UI 界面稿，缩放后作为界面底板
UI_SCREENS = {}
for i in list(range(1, 4)) + list(range(21, 46)) + list(range(51, 75)) + \
            list(range(90, 101)) + list(range(113, 122)) + [8, 9, 16, 17, 18, 19, 20]:
    if i == 121:
        continue
    UI_SCREENS[i] = 'ui_screen_%03d' % i


def ensure(d):
    os.makedirs(d, exist_ok=True)
    return d


def src_path(idx):
    return os.path.join(SRC, '(%d).png' % idx)


def scan_project_chars():
    """扫描工程源码与配置中出现的所有可打印字符（含中文/符号），供字表使用。

    目的：手工维护字表必然遗漏（★、：、，、（）等界面文案标点会渲染成豆腐块）。
    这里从 java 源码字符串字面量与 json 配置里提取真实用到的字符。
    """
    found = set()
    src_dirs = [
        os.path.join(ROOT, 'core', 'src', 'main', 'java'),
        os.path.join(ROOT, 'android', 'src', 'main'),
        os.path.join(ROOT, 'desktop', 'src', 'main'),
    ]
    files = []
    for d in src_dirs:
        for dp, _, fns in os.walk(d):
            for fn in fns:
                files.append(os.path.join(dp, fn))
    cfg_dir = os.path.join(ROOT, 'assets', 'config')
    if os.path.isdir(cfg_dir):
        for fn in os.listdir(cfg_dir):
            files.append(os.path.join(cfg_dir, fn))

    # java 字符串字面量 / json 全文
    lit = re.compile(r'"((?:[^"\\]|\\.)*)"')
    for path in files:
        try:
            with open(path, encoding='utf-8') as f:
                text = f.read()
        except Exception:
            continue
        if path.lower().endswith('.json'):
            for ch in text:
                if ch.isprintable() and ch != '\n' and ch != '\r' and ch != '\t':
                    found.add(ch)
        else:
            # 只取字符串字面量内部的内容（注释里的字不一定会显示）
            for m in lit.finditer(text):
                for ch in m.group(1):
                    if ch.isprintable() and ch not in '\n\r\t':
                        found.add(ch)
    # 过滤掉明显不需要的
    for ch in list(found):
        if ord(ch) < 0x20:
            found.discard(ch)
    return found


def main():
    icon_dir = ensure(os.path.join(TEX, 'icons'))
    ui_dir = ensure(os.path.join(TEX, 'ui'))
    font_dir = ensure(os.path.join(UI, 'font'))

    # 1) 图标类：缩放到 256（实际显示尺寸仅 60~130px，512 纯浪费显存）
    n_icon = 0
    for idx, name in sorted(ICONS.items()):
        p = src_path(idx)
        if not os.path.exists(p) or idx in UI_SCREENS:
            continue
        im = Image.open(p).convert('RGBA')
        im.thumbnail((256, 256), Image.LANCZOS)
        im.save(os.path.join(icon_dir, name + '.png'))
        n_icon += 1

    # 2) UI 成品稿：统一缩到 1280x720
    #    【体积优化】79 张全屏 PNG 实测共 108MB，导致 APK 达 131MB。
    #    这 79 张底板全部为**完全不透明**（alpha 恒为 255，已逐张校验），
    #    因此可以安全转成 JPEG q90（4:4:4 无色度抽样）：
    #      - 体积：1362 KB/张 → 251 KB/张（5.4×），整包 108MB → 19.6MB
    #      - 画质：PSNR 39.5 dB，肉眼无差异
    #    解码可行性已验证：LibGDX 的 Pixmap 走 gdx2d native（stb_image），
    #    Android libgdx.so 内含 JPEG 解码器（存在 "Corrupt JPEG" /
    #    "JPEG format not supported" 特征字符串），
    #    桌面 gdx64.dll 已实测 JPEG/PNG 均可正常解码。
    #    图标类保留 PNG —— 图标需要透明通道，不能转 JPEG。
    n_ui = 0
    for idx, name in sorted(UI_SCREENS.items()):
        p = src_path(idx)
        if not os.path.exists(p):
            continue
        im = Image.open(p).convert('RGBA').resize((1280, 720), Image.LANCZOS)
        alpha = im.split()[3]
        if alpha.getextrema()[0] == 255:
            # 完全不透明 → JPEG
            im.convert('RGB').save(os.path.join(ui_dir, name + '.jpg'),
                                   'JPEG', quality=90, subsampling=0, optimize=True)
        else:
            # 含透明 → 只能留 PNG
            im.save(os.path.join(ui_dir, name + '.png'), optimize=True)
        n_ui += 1

    # 3) BitmapFont：黑体，扫描 ASCII + 常用中文
    charset = set()
    for r in range(0x20, 0x7F):
        charset.add(chr(r))
    # 项目中会出现的全部汉字/符号（UI 文案 + 数值单位 + 模式名）
    extra = (
        "重生之满级暴击无限进化超杀神域组队爬塔裂隙闯关虚空初隙星陨深渊混沌边界破晓终境"
        "开始游戏继续退出结算胜利失败奖励背包神装符文天赋树印记等级经验技能选择三选一"
        "确定取消返回商店购买使用装备镶嵌强化升级传说史诗稀有精良凡品神话满级"
        "生命魔法攻击速度暴击率爆伤护盾移速伤害回溯无敌减速吸血穿透加成主武器"
        "波次精英首领时间秒分时剩余难度狂暴层数记录最高历史单次数据"
        "玩家角色选择进入秘境队长加入房间创建等待准备战斗"
        "暴击伤害数字签名字数超杀暴击回溯系数乘区阈值超额触发范围冲击波"
        "第一二三四五六七八九十百千万亿零个层关卡章节场景"
        "锁定目标距离优先视野自动射击子弹经验球掉落磁铁半径拾取"
        "所有资源不足已满级无法使用需要条件解锁"
        "设置音效音乐音量画质分辨率帧率按键操作说明教程"
        "确定要退出吗保存进度不保存重新开始"
        "背包容量空没有物品使用后消失数量拥有获得失去"
        "队友光环增益主机客户端连接延迟主机权威同步"
        "满级重生归零每一次重来都是更强的自己"
        "个百分号倍效率加速减速增加减少提升降低"
        "终极决战阶段特殊精英小怪远程近战飞行地面"
        "已激活未激活生效中冷却中充能满溢出"
        "当前第关卡进度完成未解锁开放"
        "关闭帮助说明提示注意警告"
        "点击任意位置继续滑动屏幕"
        "拥挤舒适标准高清"
    )
    for ch in extra:
        if ord(ch) > 0x2000 or ch.isprintable():
            charset.add(ch)

    # 【关键】自动扫描代码与配置里出现过的每一个字符。
    # 早期版本只用手工维护的字表，导致代码里实际用到的 380+ 个字
    # （★ 稀有度星标、：、，、·、（）以及大量界面文案）不在字表内，
    # 运行时全部渲染为空白/豆腐块。这里改为自动收集，杜绝遗漏。
    scanned = scan_project_chars()
    charset |= scanned
    print('[font] 自动扫描补充字符 %d 个' % len(scanned))

    chars = sorted(charset)
    SIZE = 44
    PAD = 2
    font = ImageFont.truetype('C:/Windows/Fonts/simhei.ttf', SIZE)
    ascent, descent = font.getmetrics()
    line_h = ascent + descent

    # 测量每个字形
    # 【修正】空格的 xadvance 之前被写成 0（advance=0），导致中文文案里所有空格
    # 完全不占位、文字挤成一团（如 "开 始 游 戏" 显示成 "开始游戏"）。
    # BMFont 约定：xoffset/yoffset 是相对「基线原点」的偏移，
    #   xoffset = 墨迹左边界（相对笔位）
    #   yoffset = 基线到墨迹顶部的距离（向上为正）
    #   height = 墨迹高度
    # 因此这里用「先算墨迹框，再按基线换算」的方式重新生成。
    glyphs = []
    for ch in chars:
        # 用 anchor='ls'（左、基线）测量，墨迹框直接相对基线原点
        bbox = font.getbbox(ch, anchor='ls') if hasattr(font, 'getbbox') else None
        adv = int(round(font.getlength(ch)))
        if bbox is None:
            glyphs.append((ch, None, 0, 0, 0, 0, adv))
            continue
        x0, y0, x1, y1 = bbox          # y0 向上为正（相对基线）
        gw = x1 - x0
        gh = y1 - y0
        if gw <= 0 or gh <= 0:
            # 空格等无墨迹字符：width/height=0，但仍需保留正确的 advance
            glyphs.append((ch, None, 0, 0, 0, 0, adv))
            continue
        glyphs.append((ch, (x0, y0, gw, gh), gw, gh, x0, y0, adv))

    # 装箱：固定列数铺开
    COLS = 16
    cell_w = 0
    cell_h = 0
    for g in glyphs:
        if g[1] is None:
            continue
        cell_w = max(cell_w, g[2] + 2)
        cell_h = max(cell_h, g[3] + 2)
    cell_w = max(cell_w, 8)
    cell_h = max(cell_h, 8)
    rows = (len(glyphs) + COLS - 1) // COLS
    page_w = COLS * cell_w
    page_h = rows * cell_h
    # 贴到 2 的幂
    def p2(v):
        p = 1
        while p < v:
            p *= 2
        return p
    page_w = p2(page_w)
    page_h = p2(page_h)

    img = Image.new('RGBA', (page_w, page_h), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    lines = []
    for i, g in enumerate(glyphs):
        ch, maskinfo, gw, gh, off_x, off_y, adv = g
        cx = (i % COLS) * cell_w
        cy = (i // COLS) * cell_h
        if maskinfo is not None:
            x0, y0, w, h = maskinfo
            # 用同样的 anchor 渲染整字，再裁出墨迹区
            full = Image.new('L', (w + 8, h + 8), 0)
            fd = ImageDraw.Draw(full)
            fd.text((4 - x0, 4 + y0), ch, font=font, fill=255, anchor='ls')
            crop = full.crop((4, 4, 4 + w, 4 + h))
            rgba = Image.new('RGBA', (w, h), (255, 255, 255, 0))
            rgba.putalpha(crop)
            img.paste(rgba, (cx, cy), rgba)
        # BMFont 文本信息行：yoffset 为基线到墨迹顶部的距离
        lines.append('char id=%d x=%d y=%d width=%d height=%d xoffset=%d yoffset=%d xadvance=%d page=0 chnl=15'
                     % (ord(ch), cx, cy, gw, gh, off_x, off_y, adv))
    # 头部：spacing=0,0（字形已在图集内按格留白，无额外间距）；padding 归零
    head = ['info face="Deskhan" size=%d bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0' % SIZE,
            'common lineHeight=%d base=%d scaleW=%d scaleH=%d pages=1 packed=0' % (line_h, ascent, page_w, page_h),
            'page id=0 file="deskhan.png"']
    out = os.path.join(font_dir, 'deskhan.fnt')
    with open(out, 'w', encoding='utf-8') as f:
        f.write('\n'.join(head + lines) + '\n')
    img.save(os.path.join(font_dir, 'deskhan.png'))
    print('[font] %s  glyphs=%d  page=%dx%d  lineHeight=%d base=%d' % (out, len(glyphs), page_w, page_h, line_h, ascent))
    print('[icons] %d files -> %s' % (n_icon, icon_dir))
    print('[ui] %d files -> %s' % (n_ui, ui_dir))


if __name__ == '__main__':
    main()
