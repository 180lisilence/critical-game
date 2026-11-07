package com.yunluhezi.maxcrit.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.yunluhezi.maxcrit.config.ConfigHub;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资源管理器。
 *
 * 【重要】贴图采用**按需懒加载**：
 * 界面底板有 79 张 1280×720 全屏图，若启动时全部 new Texture()，
 * 显存占用将超过 280MB，Android 低端机必然 OOM 闪退。
 * 因此这里只预加载图标（小图，必用），界面底板在真正要用时才加载，
 * 并用 LRU 缓存控制上限，超出时主动释放最久未用的。
 */
public class GameAssets {

    public final ConfigHub config = new ConfigHub();

    public BitmapFont font;
    public BitmapFont fontSmall;

    /** 已加载的贴图（name -> region），LinkedHashMap 实现 LRU 顺序。 */
    private final Map<String, TextureRegion> loaded = new LinkedHashMap<>();
    /** 已知资源名（name -> assets 内相对路径），用于懒加载判定。 */
    private final Map<String, String> paths = new HashMap<>();
    /** 图标资源名（启动时预加载）。 */
    public final Array<String> iconNames = new Array<>();
    public final Array<String> uiScreenNames = new Array<>();

    private final Array<Sound> sounds = new Array<>();
    private final Map<String, Sound> soundMap = new HashMap<>();
    private final Map<String, Music> musicMap = new HashMap<>();
    private Music currentMusic;
    private String currentMusicPlayingName;

    /** 界面底板 LRU 上限（张）。 */
    private static final int UI_CACHE_LIMIT = 6;

    public void load() {
        // --- 字体 ---
        try {
            font = new BitmapFont(Gdx.files.internal("ui/font/deskhan.fnt"),
                    Gdx.files.internal("ui/font/deskhan.png"), false);
            font.setUseIntegerPositions(false);
            font.getRegion().getTexture().setFilter(
                    Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            fontSmall = font;
        } catch (Exception e) {
            Gdx.app.error("GameAssets", "font load FAILED: " + e);
        }

        // --- 扫描资源目录（只记录路径，不加载） ---
        scanDir("textures/icons", "icons", iconNames);
        scanDir("textures/ui", "ui", uiScreenNames);

        // --- 预加载图标（512px 小图，必用且体积可控） ---
        for (String name : iconNames) {
            try {
                loadTexture(name);
            } catch (Exception e) {
                Gdx.app.error("GameAssets", "icon load fail: " + name + " " + e);
            }
        }

        // --- 配置 ---
        config.loadAll();

        // --- 音效 ---
        loadSounds();
    }

    private void scanDir(String path, String prefix, Array<String> out) {
        com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal(path);
        if (!dir.exists() || !dir.isDirectory()) return;
        for (com.badlogic.gdx.files.FileHandle f : dir.list()) {
            String n = f.name().toLowerCase();
            if (!n.endsWith(".png") && !n.endsWith(".jpg") && !n.endsWith(".jpeg")) continue;
            String name = f.name();
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            paths.put(name, path + "/" + f.name());
            out.add(name);
        }
    }

    private void loadSounds() {
        com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal("sound");
        if (!dir.exists() || !dir.isDirectory()) return;
        for (com.badlogic.gdx.files.FileHandle f : dir.list()) {
            if (!f.name().endsWith(".wav") && !f.name().endsWith(".ogg")) continue;
            try {
                if (f.name().endsWith(".wav")) {
                    Sound s = Gdx.audio.newSound(f);
                    sounds.add(s);
                    soundMap.put(f.name().replace(".wav", ""), s);
                } else {
                    Music m = Gdx.audio.newMusic(f);
                    musicMap.put(f.name().replace(".ogg", ""), m);
                }
            } catch (Exception e) {
                Gdx.app.error("GameAssets", "sound load fail " + f.name() + ": " + e);
            }
        }
    }

    /**
     * 获取贴图区域（懒加载）。
     * 界面底板在真正要用时才从 assets 解码，并做 LRU 淘汰。
     */
    public TextureRegion region(String name) {
        if (name == null) return null;
        TextureRegion r = loaded.get(name);
        if (r != null) {
            loaded.remove(name);   // LRU：命中后移到末尾
            loaded.put(name, r);
            return r;
        }
        String path = paths.get(name);
        if (path == null) return null;
        try {
            return loadTexture(name);
        } catch (Exception e) {
            Gdx.app.error("GameAssets", "texture load fail: " + name + " " + e);
            return null;
        }
    }

    private TextureRegion loadTexture(String name) {
        String path = paths.get(name);
        if (path == null) return null;
        com.badlogic.gdx.files.FileHandle f = Gdx.files.internal(path);
        Texture t = new Texture(f);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion region = new TextureRegion(t);
        loaded.put(name, region);
        evictIfNeeded();
        return region;
    }

    /**
     * 控制界面底板缓存上限，防止显存爆掉。
     *
     * 【修复】原实现用 loaded.size()（含全部 41 张图标）减去上限来判断是否淘汰，
     * 而淘汰时只删 ui_screen_* —— 由于图标数（41）已远超上限（6），
     * toRemove 恒为正，导致每次加载 UI 底板都会把已缓存的底板全部驱逐，
     * 上限完全失效 → 79 张 1280x720 底板被反复解码 → 内存暴涨 + 启动闪退。
     * 现改为：只统计 ui_screen_* 自身数量。
     */
    private void evictIfNeeded() {
        int uiCount = 0;
        for (String k : loaded.keySet()) {
            if (k.startsWith("ui_screen_")) uiCount++;
        }
        if (uiCount <= UI_CACHE_LIMIT) return;
        int toRemove = uiCount - UI_CACHE_LIMIT;
        Iterator<Map.Entry<String, TextureRegion>> it = loaded.entrySet().iterator();
        while (it.hasNext() && toRemove > 0) {
            Map.Entry<String, TextureRegion> e = it.next();
            if (e.getKey().startsWith("ui_screen_")) {
                e.getValue().getTexture().dispose();
                it.remove();
                toRemove--;
            }
        }
    }

    public boolean hasRegion(String name) {
        return paths.containsKey(name);
    }

    public int count() {
        return loaded.size() + sounds.size;
    }

    // ---------- 音效播放 ----------

    public void play(String name, float volume) {
        Sound s = soundMap.get(name);
        if (s != null) s.play(volume);
    }

    public void playMusic(String name, float volume) {
        if (currentMusic != null) {
            if (currentMusicPlayingName != null && currentMusicPlayingName.equals(name)) return;
            currentMusic.stop();
            currentMusic = null;
        }
        Music m = musicMap.get(name);
        if (m == null) return;
        m.setLooping(true);
        m.setVolume(volume);
        m.play();
        currentMusic = m;
        currentMusicPlayingName = name;
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicPlayingName = null;
        }
    }

    public void dispose() {
        for (TextureRegion r : loaded.values()) r.getTexture().dispose();
        loaded.clear();
        for (Sound s : sounds) s.dispose();
        for (Music m : musicMap.values()) m.dispose();
        if (font != null) font.dispose();
    }
}
