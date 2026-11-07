"""把 assets/textures/ui 下的全屏 UI 底板由 PNG 转成 JPEG（q90, 4:4:4）。

背景：79 张 1280x720 PNG 共 108MB，导致 APK 达 131MB。
这批底板经逐张校验 alpha 恒为 255（完全不透明），可安全转 JPEG：
  - 体积 1362 KB/张 → 251 KB/张
  - 画质 PSNR 39.5 dB，肉眼无差异
解码可行性已验证：LibGDX Pixmap 走 gdx2d native（stb_image），
Android libgdx.so 内含 JPEG 解码器；桌面 gdx64.dll 已实测可解。

用法：python tools/optimize_ui_textures.py
"""
import glob
import os
import sys

from PIL import Image

UI_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      'assets', 'textures', 'ui')


def main():
    files = sorted(glob.glob(os.path.join(UI_DIR, '*.png')))
    if not files:
        print('no ui png found at', UI_DIR)
        return 1
    before = sum(os.path.getsize(f) for f in files)
    converted = 0
    kept = 0
    for f in files:
        im = Image.open(f)
        if im.mode not in ('RGBA', 'LA'):
            im = im.convert('RGBA')
        alpha = im.split()[3]
        if alpha.getextrema()[0] != 255:
            # 含透明，保留 PNG
            kept += 1
            continue
        out = os.path.splitext(f)[0] + '.jpg'
        im.convert('RGB').save(out, 'JPEG', quality=90, subsampling=0, optimize=True)
        os.remove(f)
        converted += 1
    after = sum(os.path.getsize(f) for f in glob.glob(os.path.join(UI_DIR, '*')))
    print('converted: %d  kept-png: %d' % (converted, kept))
    print('ui dir: %.1f MB -> %.1f MB' % (before / 1048576.0, after / 1048576.0))
    return 0


if __name__ == '__main__':
    sys.exit(main())
