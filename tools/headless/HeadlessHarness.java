import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 无头测试夹具：为 LogicSelfTest 提供最小可用的 Gdx.app / Gdx.files。
 *
 * 目的：让配置表加载、存档读写、随机数等真实逻辑可以在没有图形环境的情况下跑起来。
 * internal(assets/xxx) 映射到工程 assets 目录；local(存档) 映射到临时目录。
 */
public class HeadlessHarness {

    public static void main(String[] args) throws Exception {
        String root = args.length > 0 ? args[0] : "E:/重生之满级暴击";

        Gdx.app = (Application) Proxy.newProxyInstance(
                HeadlessHarness.class.getClassLoader(),
                new Class<?>[]{Application.class},
                new InvocationHandler() {
                    @Override public Object invoke(Object proxy, Method m, Object[] a) {
                        String n = m.getName();
                        if (n.equals("log") && a != null && a.length >= 2) {
                            System.out.println("[" + a[0] + "] " + a[1]);
                            return null;
                        }
                        if (n.equals("error") && a != null && a.length >= 2) {
                            System.err.println("[" + a[0] + "] " + a[1]);
                            return null;
                        }
                        if (n.equals("getType")) return Application.ApplicationType.HeadlessDesktop;
                        if (n.equals("getApplicationLogger")) return null;
                        if (n.equals("setApplicationLogger")) return null;
                        // 其余方法（graphics/audio/input/net）在本测试中不应被调用
                        throw new UnsupportedOperationException("Gdx.app." + n + "() 在无头测试中不可用");
                    }
                });

        final File assetsDir = new File(root, "assets");
        final File localDir = new File(System.getProperty("java.io.tmpdir"), "maxcrit_test");
        if (!localDir.exists()) localDir.mkdirs();

        Gdx.files = new com.badlogic.gdx.Files() {
            @Override public FileHandle getFileHandle(String path, FileType type) {
                return new FileHandle(new File(path));
            }
            @Override public FileHandle classpath(String path) {
                return new FileHandle(new File(path));
            }
            @Override public FileHandle internal(String path) {
                return new FileHandle(new File(assetsDir, path));
            }
            @Override public FileHandle external(String path) {
                return new FileHandle(new File(path));
            }
            @Override public FileHandle absolute(String path) {
                return new FileHandle(new File(path));
            }
            @Override public FileHandle local(String path) {
                return new FileHandle(new File(localDir, path));
            }
            @Override public String getExternalStoragePath() { return localDir.getAbsolutePath(); }
            @Override public boolean isExternalStorageAvailable() { return true; }
            @Override public String getLocalStoragePath() { return localDir.getAbsolutePath(); }
            @Override public boolean isLocalStorageAvailable() { return true; }
        };

        com.yunluhezi.maxcrit.LogicSelfTest.main(new String[0]);
    }
}
