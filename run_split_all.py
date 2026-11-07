# -*- coding: utf-8 -*-
"""全量拆分 + 统计清单"""
import sys, os, re, json
sys.path.insert(0, r'E:\重生之满级暴击')
from split_tool import split_image, imwrite_unicode

src = r'E:\重生之满级暴击\sucai'
out = r'E:\重生之满级暴击\sucai_parts'
os.makedirs(out, exist_ok=True)

MIN_RATIO = 0.0012   # 过滤小噪点
MIN_SIDE = 96        # 最小边，防止碎条

files = sorted([f for f in os.listdir(src)
                if f.lower().endswith('.png') and re.match(r'\(\d+\)', f)],
               key=lambda f: int(re.match(r'\((\d+)\)', f).group(1)))

report = []
total_parts = 0
for f in files:
    n = int(re.match(r'\((\d+)\)', f).group(1))
    parts = split_image(os.path.join(src, f), out, min_area_ratio=MIN_RATIO)
    entry = {'src': f, 'parts': []}
    for idx, (x0, y0, x1, y1, arr) in enumerate(parts, 1):
        if arr.shape[0] < 24 or arr.shape[1] < 24:
            continue
        name = f"p{n:03d}_{idx:02d}.png"
        imwrite_unicode(os.path.join(out, name), arr)
        entry['parts'].append({'file': name, 'w': arr.shape[1], 'h': arr.shape[0],
                               'bbox': [int(x0), int(y0), int(x1), int(y1)]})
        total_parts += 1
    report.append(entry)
    print(f"{f} -> {len(parts)} 块", flush=True)

with open(os.path.join(out, '_split_report.json'), 'w', encoding='utf-8') as fp:
    json.dump(report, fp, ensure_ascii=False, indent=1)
print('完成, 共', total_parts, '个组件')
