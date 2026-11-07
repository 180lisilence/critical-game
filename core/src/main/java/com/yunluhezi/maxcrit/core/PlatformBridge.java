package com.yunluhezi.maxcrit.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.math.MathUtils;

/**
 * 平台桥接：把「读档路径 / 震动」等平台差异隔离出去，core 内部不直接碰 Android API。
 */
public class PlatformBridge {

    private final MaxCritGame game;
    private final Json json = new Json();
    private final JsonReader reader = new JsonReader();
    private String savePath = "maxcrit_save.json";

    public PlatformBridge(MaxCritGame game) {
        this.game = game;
    }

    public void setSavePath(String p) {
        this.savePath = p;
    }

    public String getSavePath() {
        return savePath;
    }

    public boolean fileExists() {
        return Gdx.files.local(savePath).exists();
    }

    public String readRaw() {
        FileHandle f = Gdx.files.local(savePath);
        if (!f.exists()) return null;
        return f.readString("UTF-8");
    }

    public void writeRaw(String content) {
        Gdx.files.local(savePath).writeString(content, false, "UTF-8");
    }

    public Json getJson() {
        return json;
    }

    public JsonReader getReader() {
        return reader;
    }

    /** 轻震动反馈；无对应能力时静默忽略。 */
    public void vibrate(long millis) {
        // Android 端由 AndroidLauncher 注入实现，core 默认空实现
    }

    public void log(String msg) {
        Gdx.app.log("MaxCrit", msg);
    }
}
