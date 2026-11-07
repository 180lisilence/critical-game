package com.yunluhezi.maxcrit.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.yunluhezi.maxcrit.core.MaxCritGame;

/** Windows 桌面启动器（core 与 Android 端完全共用）。 */
public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("重生之满级暴击");
        config.setWindowedMode(1280, 720);
        config.setWindowSizeLimits(720, 480, -1, -1);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new MaxCritGame(), config);
    }
}
