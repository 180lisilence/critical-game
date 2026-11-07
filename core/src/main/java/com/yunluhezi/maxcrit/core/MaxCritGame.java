package com.yunluhezi.maxcrit.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.yunluhezi.maxcrit.asset.GameAssets;
import com.yunluhezi.maxcrit.screen.BootErrorScreen;
import com.yunluhezi.maxcrit.screen.MenuScreen;

/**
 * 《重生之满级暴击》主游戏类。
 * 平台无关：core 同时供 Android 与后续 Windows(lwjgl3) 复用。
 */
public class MaxCritGame extends Game {

    /** 设计基准分辨率。 */
    public static final float DESIGN_W = 1280f;
    public static final float DESIGN_H = 720f;

    public GameAssets assets;
    public FitViewport viewport;
    /** 平台实现，用于存档读写与震动反馈。 */
    public PlatformBridge platform;
    /** 存档管理器（全局唯一）。 */
    public com.yunluhezi.maxcrit.save.SaveManager saveManager;
    /** 启动失败时的错误信息（非空则说明启动异常）。 */
    public String bootError;

    @Override
    public void create() {
        String stage = "enter";
        try {
            Gdx.app.log("MaxCrit", "=== boot begin ===");
            stage = "viewport";
            viewport = new FitViewport(DESIGN_W, DESIGN_H);
            Gdx.app.log("MaxCrit", "step1 viewport ok");

            stage = "save";
            platform = new PlatformBridge(this);
            saveManager = new com.yunluhezi.maxcrit.save.SaveManager(platform);
            saveManager.load();
            Gdx.app.log("MaxCrit", "step2 save ok, marks=" + saveManager.data().echoMarks);

            stage = "assets";
            assets = new GameAssets();
            assets.load();
            Gdx.app.log("MaxCrit", "step3 assets ok, loaded=" + assets.count()
                    + " icons=" + assets.iconNames.size
                    + " uiScreens=" + assets.uiScreenNames.size
                    + " skills=" + assets.config.skills.size());

            stage = "menu";
            setScreen(new MenuScreen(this));
            Gdx.app.log("MaxCrit", "=== boot ok ===");
        } catch (Throwable t) {
            // 启动期异常必须抓住：否则直接杀进程，表现为「点开就闪退」且无日志
            bootError = stage + " -> " + describe(t);
            Gdx.app.error("MaxCrit", "BOOT FAILED at " + stage + ": " + t, t);
            try {
                if (assets == null) assets = new GameAssets();
                setScreen(new BootErrorScreen(this, stage, bootError));
                Gdx.app.log("MaxCrit", "=== boot error screen shown: " + bootError);
            } catch (Throwable t2) {
                Gdx.app.error("MaxCrit", "BOOT FAILED AGAIN: " + t2, t2);
            }
        }
    }

    /** 把异常与其直接原因压成一行可读文本（BootErrorScreen 上只放得下有限宽度）。 */
    private static String describe(Throwable t) {
        if (t == null) return "(null)";
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName()).append(": ").append(String.valueOf(t.getMessage()));
        Throwable c = t.getCause();
        if (c != null && c != t) {
            sb.append("\n  caused by ").append(c.getClass().getName())
              .append(": ").append(String.valueOf(c.getMessage()));
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st != null) {
            for (int i = 0; i < Math.min(4, st.length); i++) {
                sb.append("\n  at ").append(st[i]);
            }
        }
        return sb.toString();
    }

    @Override
    public void render() {
        Screen s = getScreen();
        if (s == null) return;
        try {
            if (s instanceof com.yunluhezi.maxcrit.screen.GameScreen) {
                ((com.yunluhezi.maxcrit.screen.GameScreen) s).syncInput();
            } else if (s instanceof com.yunluhezi.maxcrit.screen.TowerScreen) {
                ((com.yunluhezi.maxcrit.screen.TowerScreen) s).syncInput();
            }
            s.render(Gdx.graphics.getDeltaTime());
        } catch (Throwable t) {
            // 单帧异常不再直接杀进程，转为日志 + 跳帧
            Gdx.app.error("MaxCrit", "render error: " + t, t);
        }
    }

    @Override
    public void resize(int width, int height) {
        // 尺寸变化由框架在任何时刻回调（旋转、键盘弹出、沉浸态切换都会触发），
        // 这里一旦抛异常会直接杀掉 GL 线程，表现为无日志闪退，故必须兜住。
        try {
            if (viewport != null) viewport.update(width, height, true);
        } catch (Throwable t) {
            Gdx.app.error("MaxCrit", "resize viewport failed: " + t, t);
        }
        try {
            Screen s = getScreen();
            if (s != null) s.resize(width, height);
        } catch (Throwable t) {
            Gdx.app.error("MaxCrit", "resize screen failed: " + t, t);
        }
    }

    @Override
    public void pause() {
        super.pause();
        if (getScreen() instanceof com.yunluhezi.maxcrit.screen.GameScreen) {
            ((com.yunluhezi.maxcrit.screen.GameScreen) getScreen()).onPauseGame();
        }
    }
}
