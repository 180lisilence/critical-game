import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.zip.CRC32;

/**
 * 存档 CRC 往返验证：验证 SaveManager 的「写入 → 读出 → 校验」链路是否真的一致。
 * 这是 P0 级验证：如果 toJson 与 JsonValue.toString() 输出不一致，玩家每次读档都会被判为篡改而清空。
 */
public class CrcRoundTrip {

    public static void main(String[] args) {
        Gdx.app = (com.badlogic.gdx.Application) java.lang.reflect.Proxy.newProxyInstance(
                CrcRoundTrip.class.getClassLoader(),
                new Class<?>[]{com.badlogic.gdx.Application.class},
                (proxy, m, a) -> {
                    if (m.getName().equals("log") || m.getName().equals("error")) {
                        System.out.println("[" + (a != null ? a[0] : "") + "] " + (a != null && a.length > 1 ? a[1] : ""));
                        return null;
                    }
                    if (m.getName().equals("getType")) return com.badlogic.gdx.Application.ApplicationType.HeadlessDesktop;
                    return null;
                });

        com.yunluhezi.maxcrit.save.SaveManager.SaveData d =
                new com.yunluhezi.maxcrit.save.SaveManager.SaveData();
        d.echoMarks = 777;
        d.maxTowerFloor = 42;
        d.biggestCrit = 1234.5f;
        d.talents.add("tl_k1");
        d.equipped.put("weapon", "eq_wpn_02");
        d.runes.put("eq_wpn_02", new com.badlogic.gdx.utils.Array<>(new String[]{"r_01", "r_02"}));

        Json json = new Json();
        String payload = json.toJson(d);
        String crc = crc32(payload);
        String out = "{\"crc\":\"" + crc + "\",\"data\":" + payload + "}";

        System.out.println("--- 写入内容 ---");
        System.out.println(out);

        // ===== 模拟 SaveManager.load() 的读回逻辑 =====
        JsonValue root = new JsonReader().parse(out);
        String readCrc = root.getString("crc", "");
        JsonValue dv = root.get("data");
        String readPayload = dv.toString();

        System.out.println("--- 读回 payload ---");
        System.out.println(readPayload);
        System.out.println("--- 写入 payload ---");
        System.out.println(payload);
        System.out.println();
        System.out.println("写入 crc = " + crc);
        System.out.println("读回 crc = " + crc32(readPayload));
        boolean match = readCrc.equals(crc32(readPayload));
        System.out.println(">>> CRC 是否一致: " + (match ? "一致（正常）" : "不一致（读档必被清空！）"));
        if (!match) {
            System.out.println(">>> payload 是否逐字符相同: " + payload.equals(readPayload));
        }
    }

    private static String crc32(String s) {
        CRC32 c = new CRC32();
        c.update(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Long.toHexString(c.getValue());
    }
}
