package com.yunluhezi.maxcrit;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * 崩溃现场捕获器。
 *
 * 【为什么必须有】APK 闪退在模拟器/真机上只表现为「点开就没了」，
 * 没有 logcat 就完全拿不到原因。所以这里把崩溃堆栈**写到外部存储的固定文件**：
 *   /sdcard/Android/data/com.yunluhezi.maxcrit/files/crash.log
 * 用户用文件管理器或 adb pull 就能看到真实原因，不必反复复现。
 *
 * 【覆盖范围】只装在 Android 端（core 模块无 android 依赖），
 * 在 {@link AndroidLauncher} 里于任何 LibGDX 初始化之前安装。
 */
public final class CrashTrap implements UncaughtExceptionHandler {

    private static final String TAG = "MaxCrit";
    private static final Thread.UncaughtExceptionHandler PREV = Thread.getDefaultUncaughtExceptionHandler();

    private final File logDir;

    private CrashTrap(File logDir) {
        this.logDir = logDir;
    }

    /** 在应用最开始调用（onCreate 第一行）。 */
    public static void install(android.content.Context ctx) {
        File dir = ctx.getExternalFilesDir(null);
        if (dir == null) dir = new File(ctx.getFilesDir(), "logs");
        if (!dir.exists() && !dir.mkdirs()) dir = ctx.getFilesDir();
        Thread.setDefaultUncaughtExceptionHandler(new CrashTrap(dir));
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            write("=== FATAL on thread: " + t.getName() + " ===\n" + sw + "\n\n");
        } catch (Throwable ignored) {
            // 兜底逻辑本身不能再抛
        }
        // 交给系统默认处理器，保证进程仍以正常方式退出（保留系统行为）
        if (PREV != null) PREV.uncaughtException(t, e);
        else System.exit(2);
    }

    /** 记录非致命错误（被捕获住的异常），用于定位「哪一步开始出问题」。 */
    public static void log(String stage, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            File dir = new File("/sdcard/Android/data/com.yunluhezi.maxcrit/files");
            File f = new File(dir, "crash.log");
            if (!dir.exists()) dir.mkdirs();
            FileWriter w = new FileWriter(f, true);
            w.write("=== STAGE[" + stage + "] ===\n" + sw + "\n\n");
            w.close();
        } catch (Throwable ignored) {
        }
        Log.e(TAG, "stage " + stage + " failed: " + e);
    }

    private void write(String text) {
        File f = new File(logDir, "crash.log");
        FileWriter w = null;
        try {
            w = new FileWriter(f, true);
            w.write(text);
            w.write("--- device: " + Build.MANUFACTURER + " " + Build.MODEL
                    + " android " + Build.VERSION.RELEASE + " (api " + Build.VERSION.SDK_INT + ")\n");
            w.write("--- abi: " + (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                    ? Build.SUPPORTED_ABIS[0] : "?") + "\n\n");
        } catch (Throwable ignored) {
        } finally {
            try { if (w != null) w.close(); } catch (Throwable ignored) { }
        }
    }
}