/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 从 natives jar（桌面端）或 libs/&lt;abi&gt; 目录（Android）加载本地库。
 *
 * <p><b>【为什么这个类存在于本项目源码里】</b>
 * LibGDX 自 1.12.0 起把 {@code SharedLibraryLoader} 从 {@code gdx.jar} 拆到独立 artifact
 * {@code com.badlogic.gdx:gdx-jnigen-loader}。本项目为了脱离 Gradle 依赖解析、直接锁定版本，
 * 用的是 {@code files('../libs/xxx.jar')} 直连依赖 —— 这种写法<b>不具备传递依赖能力</b>，
 * 于是 {@code gdx-jnigen-loader} 从未进入 APK / classpath。
 *
 * <p>而 {@code gdx-1.12.1.jar} 内的 {@code GdxNativesLoader.load()} 字节码仍硬引用本类：
 * <pre>
 *   new com/badlogic/gdx/utils/SharedLibraryLoader
 *   invokevirtual load:(Ljava/lang/String;)V
 * </pre>
 * 结果：Android 启动时 {@code AndroidApplication.init()} →
 * {@code AndroidApplicationConfiguration$1.load()} → {@code GdxNativesLoader.load()} →
 * {@code NoClassDefFoundError}，{@code initialize()} 中断导致 {@code AndroidApplication.input}
 * 始终为 null，随后 {@code onResume()} 抛 NPE，表现为「一打开就闪退」。
 *
 * <p>Maven 通道在当前环境不可达（repo1 / 阿里 / 腾讯 / 华为 / GCS 全部被拦截），
 * 无法引入官方 jar，因此在本项目内提供一份<b>与 1.12.1 语义等价</b>的实现补齐该缺口。
 * 代码遵循上游 Apache-2.0：{@code libgdx/gdx-jnigen} 的 gdx-jnigen-loader 模块，
 * 去掉了对 {@code jnigen-commons}（Os / Architecture / HostDetection）的依赖，
 * 改为内联平台判定，避免再引入新的缺失类。
 *
 * <p>行为要点：
 * <ul>
 *   <li>Android / iOS：不做文件释放，Android 直接 {@code System.loadLibrary}，
 *       iOS 直接 return（静态链接，无需 dlopen）。</li>
 *   <li>桌面端：从 classpath 或 natives jar 释放到临时目录，校验 CRC，再 {@code System.load}
 *       绝对路径（JVM 已不再支持相对路径加载）。</li>
 * </ul>
 *
 * @author mzechner
 * @author Nathan Sweet
 */
public class SharedLibraryLoader {

    /** 平台判定：全部为静态常量，供 gdx 内部（UIUtils / Lwjgl3* 等）直接读字段。 */
    static public final boolean isWindows = System.getProperty("os.name").contains("Windows");
    static public final boolean isLinux = System.getProperty("os.name").contains("Linux")
            || System.getProperty("os.name").contains("FreeBSD");
    static public final boolean isMac = System.getProperty("os.name").contains("Mac");
    static public final boolean isIos = false;
    static public final boolean isAndroid = isAndroidRuntime();

    /** 是否为 64 位（Android 上通过 {@code os.arch} 判断）。 */
    static public final boolean is64Bit = is64BitRuntime();

    static public final boolean isARM = System.getProperty("os.arch").startsWith("arm")
            || System.getProperty("os.arch").startsWith("aarch");

    static private final HashSet<String> loadedLibraries = new HashSet<String>();
    static private final Random random = new Random();

    private String nativesJar;

    public SharedLibraryLoader () {
    }

    static private boolean isAndroidRuntime () {
        // Android 上 os.name 为 "Linux"，不能用它区分，必须看虚拟机实现类
        String vm = System.getProperty("java.vm.name");
        if (vm != null && vm.equalsIgnoreCase("Dalvik")) return true;
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static private boolean is64BitRuntime () {
        String arch = System.getProperty("os.arch");
        if (arch == null) return false;
        return arch.contains("64") || arch.contains("aarch64");
    }

    static String randomUUID () {
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }

    /** 从指定的 natives jar 提取本地库（用于测试时临时加载某个 so/dll）。 */
    public SharedLibraryLoader (String nativesJar) {
        this.nativesJar = nativesJar;
    }

    /** 返回流中剩余字节的 CRC。 */
    public String crc (InputStream input) {
        if (input == null) throw new IllegalArgumentException("input cannot be null.");
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[4096];
        try {
            while (true) {
                int length = input.read(buffer);
                if (length == -1) break;
                crc.update(buffer, 0, length);
            }
        } catch (Exception ignored) {
        } finally {
            closeQuietly(input);
        }
        return Long.toString(crc.getValue(), 16);
    }

    /** 把平台无关的库名映射为平台相关的文件名。 */
    public String mapLibraryName (String libraryName) {
        if (isAndroid) return libraryName;
        if (isWindows) {
            if (is64Bit) return libraryName + "64.dll";
            return libraryName + ".dll";
        }
        if (isMac) {
            if (is64Bit) return "lib" + libraryName + (isARM ? "arm64" : "64") + ".dylib";
            return "lib" + libraryName + ".dylib";
        }
        if (isLinux) {
            if (is64Bit) return "lib" + libraryName + (isARM ? "arm64" : "64") + ".so";
            return "lib" + libraryName + (isARM ? "arm" : "") + ".so";
        }
        return System.mapLibraryName(libraryName);
    }

    /** 加载当前平台对应的本地库。
     * @param libraryName 平台无关库名（不带 lib 前缀与扩展名），例如 "gdx"、"gdx-freetype"。 */
    public void load (String libraryName) {
        // iOS 静态链接，无需 dlopen
        if (isIos) return;

        synchronized (SharedLibraryLoader.class) {
            if (isLoaded(libraryName)) return;
            String platformName = mapLibraryName(libraryName);
            try {
                if (isAndroid) {
                    System.loadLibrary(platformName);
                } else {
                    loadFile(platformName);
                }
                setLoaded(libraryName);
            } catch (Throwable ex) {
                throw new GdxRuntimeException("Couldn't load shared library '" + platformName + "' for target: "
                        + (isAndroid ? "Android"
                                : (System.getProperty("os.name") + ", " + System.getProperty("os.arch")
                                        + (is64Bit ? ", 64-bit" : ", 32-bit"))),
                        ex);
            }
        }
    }

    private InputStream readFile (String path) {
        if (nativesJar == null) {
            InputStream input = SharedLibraryLoader.class.getClassLoader().getResourceAsStream(path);
            if (input == null) throw new GdxRuntimeException("Unable to read file for extraction: " + path);
            return input;
        }

        try {
            ZipFile file = new ZipFile(nativesJar);
            ZipEntry entry = file.getEntry(path);
            if (entry == null) throw new GdxRuntimeException("Couldn't find '" + path + "' in JAR: " + nativesJar);
            return file.getInputStream(entry);
        } catch (IOException ex) {
            throw new GdxRuntimeException("Error reading '" + path + "' in JAR: " + nativesJar, ex);
        }
    }

    /** 把指定文件释放到指定目录（若不存在或 CRC 不符）。释放失败时回退到 java.library.path。 */
    public File extractFile (String sourcePath, String dirName) throws IOException {
        try {
            String sourceCrc = crc(readFile(sourcePath));
            if (dirName == null) dirName = sourceCrc;

            File extractedFile = getExtractedFile(dirName, new File(sourcePath).getName());
            if (extractedFile == null) {
                extractedFile = getExtractedFile(randomUUID(), new File(sourcePath).getName());
                if (extractedFile == null)
                    throw new GdxRuntimeException("Unable to find writable path to extract file. "
                            + "Is the user home directory writable?");
            }
            return extractFile(sourcePath, sourceCrc, extractedFile);
        } catch (RuntimeException ex) {
            File file = new File(System.getProperty("java.library.path"), sourcePath);
            if (file.exists()) return file;
            throw ex;
        }
    }

    /** 把指定文件释放到目标目录。 */
    public void extractFileTo (String sourcePath, File dir) throws IOException {
        extractFile(sourcePath, crc(readFile(sourcePath)), new File(dir, new File(sourcePath).getName()));
    }

    /** 返回一个可写入的文件路径；找不到可写位置时返回 null。 */
    private File getExtractedFile (String dirName, String fileName) {
        String user = System.getProperty("user.name");
        if (user == null) user = "user";

        File idealFile = new File(System.getProperty("java.io.tmpdir") + "/libgdx" + user + "/" + dirName, fileName);
        if (canWrite(idealFile)) return idealFile;

        try {
            File file = File.createTempFile(dirName, null);
            if (file.delete()) {
                file = new File(file, fileName);
                if (canWrite(file)) return file;
            }
        } catch (IOException ignored) {
        }

        String home = System.getProperty("user.home");
        if (home != null) {
            File file = new File(home + "/.libgdx/" + dirName, fileName);
            if (canWrite(file)) return file;
        }

        File file = new File(".temp/" + dirName, fileName);
        if (canWrite(file)) return file;

        if (System.getenv("APP_SANDBOX_CONTAINER_ID") != null) return idealFile;
        return null;
    }

    /** 父目录可创建且文件可写时返回 true。 */
    private boolean canWrite (File file) {
        File parent = file.getParentFile();
        if (parent == null) return false;
        File testFile;
        if (file.exists()) {
            if (!file.canWrite() || !canExecute(file)) return false;
            testFile = new File(parent, randomUUID());
        } else {
            parent.mkdirs();
            if (!parent.isDirectory()) return false;
            testFile = file;
        }
        try {
            new FileOutputStream(testFile).close();
            if (!canExecute(testFile)) return false;
            return true;
        } catch (Throwable ex) {
            return false;
        } finally {
            testFile.delete();
        }
    }

    private boolean canExecute (File file) {
        try {
            Method canExecute = File.class.getMethod("canExecute");
            if ((Boolean) canExecute.invoke(file)) return true;

            Method setExecutable = File.class.getMethod("setExecutable", boolean.class, boolean.class);
            setExecutable.invoke(file, true, false);

            return (Boolean) canExecute.invoke(file);
        } catch (Exception ignored) {
        }
        return false;
    }

    private File extractFile (String sourcePath, String sourceCrc, File extractedFile) throws IOException {
        String extractedCrc = null;
        if (extractedFile.exists()) {
            try {
                extractedCrc = crc(new FileInputStream(extractedFile));
            } catch (FileNotFoundException ignored) {
            }
        }

        if (extractedCrc == null || !extractedCrc.equals(sourceCrc)) {
            InputStream input = null;
            FileOutputStream output = null;
            try {
                input = readFile(sourcePath);
                File p = extractedFile.getParentFile();
                if (p != null) p.mkdirs();
                output = new FileOutputStream(extractedFile);
                byte[] buffer = new byte[4096];
                while (true) {
                    int length = input.read(buffer);
                    if (length == -1) break;
                    output.write(buffer, 0, length);
                }
            } catch (IOException ex) {
                throw new GdxRuntimeException(
                        "Error extracting file: " + sourcePath + "\nTo: " + extractedFile.getAbsolutePath(), ex);
            } finally {
                closeQuietly(input);
                closeQuietly(output);
            }
        }

        return extractedFile;
    }

    /** 释放并 System.load，多位置依次尝试，全失败才抛异常。 */
    private void loadFile (String sourcePath) {
        String sourceCrc = crc(readFile(sourcePath));
        String fileName = new File(sourcePath).getName();
        String user = System.getProperty("user.name");
        if (user == null) user = "user";

        File file = new File(System.getProperty("java.io.tmpdir") + "/libgdx" + user + "/" + sourceCrc, fileName);
        Throwable ex = loadFile(sourcePath, sourceCrc, file);
        if (ex == null) return;

        try {
            file = File.createTempFile(sourceCrc, null);
            if (file.delete() && loadFile(sourcePath, sourceCrc, file) == null) return;
        } catch (Throwable ignored) {
        }

        String home = System.getProperty("user.home");
        if (home != null) {
            file = new File(home + "/.libgdx/" + sourceCrc, fileName);
            if (loadFile(sourcePath, sourceCrc, file) == null) return;
        }

        file = new File(".temp/" + sourceCrc, fileName);
        if (loadFile(sourcePath, sourceCrc, file) == null) return;

        String libPath = System.getProperty("java.library.path");
        if (libPath != null) {
            file = new File(libPath, sourcePath);
            if (file.exists()) {
                System.load(file.getAbsolutePath());
                return;
            }
        }

        throw new GdxRuntimeException(ex);
    }

    /** @return 释放并加载成功返回 null，否则返回捕获到的异常。 */
    private Throwable loadFile (String sourcePath, String sourceCrc, File extractedFile) {
        try {
            System.load(extractFile(sourcePath, sourceCrc, extractedFile).getAbsolutePath());
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }

    /** 标记为已加载（供应用自行加载库后同步状态）。 */
    static public synchronized void setLoaded (String libraryName) {
        loadedLibraries.add(libraryName);
    }

    static public synchronized boolean isLoaded (String libraryName) {
        return loadedLibraries.contains(libraryName);
    }

    public static void closeQuietly (Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
