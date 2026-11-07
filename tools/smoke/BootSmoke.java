import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;

import java.io.File;

/**
 * 启动期冒烟测试：在无图形环境下，用**真实的 gdx 代码**走一遍
 * 「字体解析 + 图集解码 + 贴图解码」——这三步是真机闪退最常见的来源。
 *
 * 注意：BitmapFont 的构造函数需要 Texture，而 Texture 需要 GL。
 * 因此这里直接用 Pixmap（走 gdx2d native，不需要 GL）验证图集可解码，
 * 字体 .fnt 则用 BitmapFontLoader 的解析逻辑做等价校验。
 */
public class BootSmoke {

    public static void main(String[] args) throws Exception {
        String root = args.length > 0 ? args[0] : "E:/重生之满级暴击";
        try {
            System.load(new File(args.length > 1 ? args[1] : "gdx64.dll").getAbsolutePath());
            System.out.println("native loaded");
        } catch (Throwable t) {
            System.out.println("native load fail: " + t);
        }

        final File assets = new File(root, "assets");

        // 1) 字体图集能否解码
        String fntPath = "assets/ui/font/deskhan.fnt";
        FileHandle fnt = new FileHandle(new File(root, fntPath));
        check("fnt 存在", fnt.exists(), fntPath);

        // 手工解析 fnt，校验字段完整（与 BitmapFontLoader 同样的必需字段）
        String fntText = fnt.readString("UTF-8");
        int glyphs = 0;
        int page = 0;
        String pngName = null;
        int scaleW = -1, scaleH = -1;
        for (String line : fntText.split("\n")) {
            line = line.trim();
            if (line.startsWith("common ")) {
                for (String tok : line.split("\\s+")) {
                    if (tok.startsWith("scaleW=")) scaleW = Integer.parseInt(tok.substring(7));
                    if (tok.startsWith("scaleH=")) scaleH = Integer.parseInt(tok.substring(7));
                }
            } else if (line.startsWith("page ")) {
                page++;
                int i = line.indexOf("file=\"");
                if (i >= 0) {
                    int j = line.indexOf('"', i + 6);
                    pngName = line.substring(i + 6, j);
                }
            } else if (line.startsWith("char ")) {
                glyphs++;
                String[] t = line.split("\\s+");
                if (t.length < 10) {
                    System.out.println("  [FAIL] char 行字段不足: " + line);
                    return;
                }
            }
        }
        System.out.println("  glyphs=" + glyphs + " pages=" + page
                + " scale=" + scaleW + "x" + scaleH + " png=" + pngName);

        // 2) 字体图集解码 + 尺寸一致性
        File pngFile = new File(assets, "ui/font/" + pngName);
        check("字体图集存在", pngFile.exists(), pngFile.getName());
        Pixmap fontPix = new Pixmap(new FileHandle(pngFile));
        System.out.println("  字体图集解码 " + fontPix.getWidth() + "x" + fontPix.getHeight());
        check("字体图集尺寸与 fnt 声明一致",
                fontPix.getWidth() == scaleW && fontPix.getHeight() == scaleH,
                "png=" + fontPix.getWidth() + "x" + fontPix.getHeight()
                        + " fnt=" + scaleW + "x" + scaleH);

        // 3) 所有 UI 贴图解码
        File uiDir = new File(assets, "textures/ui");
        File[] fs = uiDir.listFiles();
        java.util.Arrays.sort(fs);
        int ok = 0, fail = 0;
        for (File f : fs) {
            if (!f.getName().matches(".*\\.(png|jpg|jpeg)$")) continue;
            try {
                Pixmap p = new Pixmap(new FileHandle(f));
                if (p.getWidth() != 1280 || p.getHeight() != 720) {
                    System.out.println("  [FAIL] " + f.getName() + " 尺寸异常 "
                            + p.getWidth() + "x" + p.getHeight());
                    fail++;
                } else ok++;
                p.dispose();
            } catch (Throwable t) {
                System.out.println("  [FAIL] 解码失败 " + f.getName() + " : " + t);
                fail++;
            }
        }
        check("全部 UI 贴图解码 1280x720", fail == 0, "OK=" + ok + " FAIL=" + fail);

        // 4) 图标贴图解码
        File iconDir = new File(assets, "textures/icons");
        File[] ifs = iconDir.listFiles();
        java.util.Arrays.sort(ifs);
        int iok = 0, ifail = 0;
        for (File f : ifs) {
            if (!f.getName().endsWith(".png")) continue;
            try {
                Pixmap p = new Pixmap(new FileHandle(f));
                if (p.getWidth() == 0 || p.getHeight() == 0) {
                    System.out.println("  [FAIL] " + f.getName() + " 尺寸为 0");
                    ifail++;
                } else iok++;
                p.dispose();
            } catch (Throwable t) {
                System.out.println("  [FAIL] 图标解码失败 " + f.getName() + " : " + t);
                ifail++;
            }
        }
        check("全部图标解码成功", ifail == 0, "OK=" + iok + " FAIL=" + ifail);

        // 5) 配置 JSON 可解析且结构完整
        for (String cfgName : new String[]{"skill", "monster", "equip", "talent"}) {
            File cf = new File(assets, "config/" + cfgName + ".json");
            check("配置存在 " + cfgName, cf.exists(), cf.getName());
            try {
                com.badlogic.gdx.utils.JsonReader jr = new com.badlogic.gdx.utils.JsonReader();
                com.badlogic.gdx.utils.JsonValue v = jr.parse(new FileHandle(cf));
                com.badlogic.gdx.utils.JsonValue arr = v.get(listKeyOf(cf.getName()));
                int n = 0;
                if (arr != null) for (com.badlogic.gdx.utils.JsonValue ignored : arr) n++;
                check("配置可解析 " + cfgName, arr != null && n > 0, "条目 " + n);
            } catch (Throwable t) {
                check("配置可解析 " + cfgName, false, "" + t);
            }
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("  通过: " + pass + "   失败: " + fail);
        System.out.println("========================================");
        if (fail > 0) System.exit(1);
    }

    private static String listKeyOf(String fileName) {
        String base = fileName.substring(0, fileName.lastIndexOf('.'));
        return base + "s";
    }

    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean cond, String detail) {
        if (cond) {
            pass++;
            System.out.println("  [PASS] " + name + "  " + detail);
        } else {
            fail++;
            System.out.println("  [FAIL] " + name + "  " + detail);
        }
    }
}