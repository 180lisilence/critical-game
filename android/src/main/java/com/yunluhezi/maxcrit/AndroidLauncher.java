package com.yunluhezi.maxcrit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.yunluhezi.maxcrit.core.MaxCritGame;

/** Android 启动器：全屏沉浸 + 竖屏 + 硬件加速。 */
public class AndroidLauncher extends AndroidApplication {

    private static final String TAG = "MaxCritBoot";

    /**
     * 启动分阶段日志。闪退时靠它判断崩在 gdx 初始化前还是游戏 create() 里。
     * 同时写入 /sdcard/Android/data/com.yunluhezi.maxcrit/files/boot_trace.txt，
     * 这样**没有 logcat 也能拿到崩溃发生在哪一步**。
     */
    private static void mark(String s) {
        Log.i(TAG, "[boot] " + s);
        try {
            java.io.File dir = new java.io.File(
                    "/sdcard/Android/data/com.yunluhezi.maxcrit/files");
            if (!dir.exists()) dir.mkdirs();
            java.io.FileWriter w = new java.io.FileWriter(
                    new java.io.File(dir, "boot_trace.txt"), true);
            w.write(s + "\n");
            w.close();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 【关键修复】这里必须**显式**调用 initialize。
     *
     * 反编译确认：LibGDX 1.12.1 的 AndroidApplication **没有 onCreate 方法**，
     * 它继承 android.app.Activity，父类不会替我们初始化 gdx。
     * 早期版本同时重写了 initialize(listener) 与 initialize(listener, config)，
     * 结果配置（useGL30/depth/numSamples/useWakelock 等）从未真正被应用 ——
     * 走的是父类 initialize(listener) 里 new 出来的默认配置。
     * 现在统一为：onCreate 里 super 之后，用自己的 buildConfig() 调一次 initialize。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 崩溃捕获必须在任何 gdx 代码之前就位
        CrashTrap.install(this);
        mark("onCreate enter, api=" + android.os.Build.VERSION.SDK_INT);

        super.onCreate(savedInstanceState);
        mark("super.onCreate done");

        try {
            initialize(new MaxCritGame(), buildConfig());
            mark("gdx initialize done");
        } catch (Throwable t) {
            CrashTrap.log("gdx initialize", t);
            mark("gdx initialize FAILED: " + t);
            throw t;
        }

        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            hideSystemBars();
            mark("onCreate done");
        } catch (Throwable t) {
            // 窗口装饰失败不该让游戏崩
            CrashTrap.log("window setup", t);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
    }

    private void hideSystemBars() {
        try {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } catch (Throwable t) {
            CrashTrap.log("hideSystemBars", t);
        }
    }

    /** 手动构造配置（LibGDX 1.12.1 的 AndroidApplication 已移除 createConfig 钩子）。 */
    static AndroidApplicationConfiguration buildConfig() {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useRotationVectorSensor = false;
        config.useImmersiveMode = true;
        // 割草同屏实体多，16bit 深度缓冲足够且省内存
        config.depth = 16;
        config.stencil = 0;
        config.numSamples = 0;
        // 不自动创建唤醒锁：部分模拟器/设备会因 PowerManager 权限崩溃
        config.useWakelock = false;
        // 明确使用 GL20：模拟器上 GL30 上下文经常拿不到
        config.useGL30 = false;
        return config;
    }
}