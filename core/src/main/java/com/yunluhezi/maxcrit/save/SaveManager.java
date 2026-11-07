package com.yunluhezi.maxcrit.save;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.yunluhezi.maxcrit.core.PlatformBridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * 玩家存档：回溯印记 / 神装 / 符文 / 天赋点 / 最高爬塔层数 / 历史最大暴击。
 * 格式： {"crc":"xxxx","data":{...}}，CRC32 校验，防手动篡改。
 *
 * 【重要】payload 的序列化必须是**字节稳定**的：
 * 早期实现直接用 Json.toJson(data) 写入、又用 JsonValue.toString() 读回计算 CRC，
 * 但 gdx 的 Json 会给 Map/Array 附加 {class:java.util.LinkedHashMap} 之类的调试元信息，
 * 读回后的字符串与写入时不同 → CRC 永远不匹配 → 玩家每次读档都被判为篡改而清空进度。
 * 现在改为：写入与校验都用同一个自定义 writer 产生完全一致的字符串。
 */
public class SaveManager {

    public static class SaveData {
        /** 回溯印记数量（核心货币）。 */
        public int echoMarks = 0;
        /** 已解锁神装 id 列表。 */
        public Array<String> unlockedEquips = new Array<>();
        /** 当前穿戴的装备：槽位 -> 装备 id。 */
        public Map<String, String> equipped = new LinkedHashMap<>();
        /** 符文：装备 id -> 符文 id 列表。 */
        public Map<String, Array<String>> runes = new LinkedHashMap<>();
        /** 已解锁天赋 id 列表。 */
        public Array<String> talents = new Array<>();
        /** 剩余天赋点。 */
        public int talentPoints = 3;
        /** 已通关的关卡 id 列表。 */
        public Array<String> clearedStages = new Array<>();
        /** 无尽模式最高层数。 */
        public int maxTowerFloor = 0;
        /** 历史单次最大暴击数值。 */
        public float biggestCrit = 0f;
        /** 历史最高对局伤害。 */
        public float biggestDamage = 0f;
        /** 已解锁的模式。 */
        public Array<String> unlockedModes = new Array<>();
        /** 设置：音效音量。 */
        public float sfxVolume = 1f;
        /** 设置：音乐音量。 */
        public float bgmVolume = 0.6f;
        /** 上次选中的关卡。 */
        public String lastStage = "st_void";
    }

    private final PlatformBridge platform;
    private final Json json = new Json();
    private SaveData data = new SaveData();

    public SaveManager(PlatformBridge platform) {
        this.platform = platform;
    }

    public SaveData data() {
        return data;
    }

    public void load() {
        String raw = platform.readRaw();
        if (raw == null || raw.isEmpty()) {
            data = new SaveData();
            applyDefaults();
            save();
            return;
        }
        try {
            JsonValue root = new JsonReader().parse(raw);
            String crc = root.getString("crc", "");
            JsonValue dv = root.get("data");
            if (dv == null) {
                platform.log("save: data 段缺失，重置");
                data = new SaveData();
                applyDefaults();
                save();
                return;
            }
            // 关键：payload 必须用与写入时完全相同的规则重建，才能保证 CRC 一致
            String payload = encodeData(dv);
            if (!crc.isEmpty() && !crc.equals(crc32(payload))) {
                platform.log("save crc mismatch, resetting（存档被判定为篡改）");
                data = new SaveData();
                applyDefaults();
                return;
            }
            SaveData loaded = decodeData(payload);
            if (loaded != null) {
                data = loaded;
                applyDefaults();
            } else {
                data = new SaveData();
                applyDefaults();
            }
        } catch (Exception e) {
            platform.log("save load failed: " + e.getMessage());
            data = new SaveData();
            applyDefaults();
        }
    }

    public void save() {
        try {
            String payload = encodeData(data);
            String crc = crc32(payload);
            String out = "{\"crc\":\"" + crc + "\",\"data\":" + payload + "}";
            platform.writeRaw(out);
        } catch (Exception e) {
            platform.log("save write failed: " + e.getMessage());
        }
    }

    /** 首次进入时解锁基础模式。 */
    private void applyDefaults() {
        if (data.unlockedModes.size == 0) {
            data.unlockedModes.addAll("mode_rift", "mode_tower");
        }
        if (data.talentPoints == 0 && data.talents.size == 0) {
            // 保留 0 点（玩家可能已花光），仅在从未初始化时给 3 点
            data.talentPoints = 3;
        }
    }

    // ================= 编解码（字节稳定） =================

    /**
     * 把 SaveData 编码为**稳定的** JSON 字符串。
     * 全部使用手写 writer，避免 gdx Json 注入 class 元信息导致 CRC 不稳定。
     */
    private static String encodeData(SaveData d) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        sb.append("\"echoMarks\":").append(d.echoMarks);
        sb.append(",\"unlockedEquips\":").append(strArray(d.unlockedEquips));
        sb.append(",\"equipped\":").append(strMap(d.equipped));
        sb.append(",\"runes\":").append(runeMap(d.runes));
        sb.append(",\"talents\":").append(strArray(d.talents));
        sb.append(",\"talentPoints\":").append(d.talentPoints);
        sb.append(",\"clearedStages\":").append(strArray(d.clearedStages));
        sb.append(",\"maxTowerFloor\":").append(d.maxTowerFloor);
        sb.append(",\"biggestCrit\":").append(d.biggestCrit);
        sb.append(",\"biggestDamage\":").append(d.biggestDamage);
        sb.append(",\"unlockedModes\":").append(strArray(d.unlockedModes));
        sb.append(",\"sfxVolume\":").append(d.sfxVolume);
        sb.append(",\"bgmVolume\":").append(d.bgmVolume);
        sb.append(",\"lastStage\":\"").append(escape(d.lastStage)).append('"');
        sb.append('}');
        return sb.toString();
    }

    /** 从已解析的 JsonValue 重建稳定的 payload 字符串（与 encodeData 字段顺序完全一致）。 */
    private static String encodeData(JsonValue v) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        sb.append("\"echoMarks\":").append(v.getInt("echoMarks", 0));
        sb.append(",\"unlockedEquips\":").append(strArray(v.get("unlockedEquips")));
        sb.append(",\"equipped\":").append(strMap(v.get("equipped")));
        sb.append(",\"runes\":").append(runeMap(v.get("runes")));
        sb.append(",\"talents\":").append(strArray(v.get("talents")));
        sb.append(",\"talentPoints\":").append(v.getInt("talentPoints", 3));
        sb.append(",\"clearedStages\":").append(strArray(v.get("clearedStages")));
        sb.append(",\"maxTowerFloor\":").append(v.getInt("maxTowerFloor", 0));
        sb.append(",\"biggestCrit\":").append(v.getFloat("biggestCrit", 0f));
        sb.append(",\"biggestDamage\":").append(v.getFloat("biggestDamage", 0f));
        sb.append(",\"unlockedModes\":").append(strArray(v.get("unlockedModes")));
        sb.append(",\"sfxVolume\":").append(v.getFloat("sfxVolume", 1f));
        sb.append(",\"bgmVolume\":").append(v.getFloat("bgmVolume", 0.6f));
        sb.append(",\"lastStage\":\"").append(escape(v.getString("lastStage", "st_void"))).append('"');
        sb.append('}');
        return sb.toString();
    }

    /** 把稳定的 payload 字符串解析回 SaveData。 */
    private SaveData decodeData(String payload) {
        try {
            JsonValue v = new JsonReader().parse(payload);
            SaveData d = new SaveData();
            d.echoMarks = v.getInt("echoMarks", 0);
            d.unlockedEquips = toArray(v.get("unlockedEquips"));
            d.equipped = toMap(v.get("equipped"));
            d.runes = toRuneMap(v.get("runes"));
            d.talents = toArray(v.get("talents"));
            d.talentPoints = v.getInt("talentPoints", 3);
            d.clearedStages = toArray(v.get("clearedStages"));
            d.maxTowerFloor = v.getInt("maxTowerFloor", 0);
            d.biggestCrit = v.getFloat("biggestCrit", 0f);
            d.biggestDamage = v.getFloat("biggestDamage", 0f);
            d.unlockedModes = toArray(v.get("unlockedModes"));
            d.sfxVolume = v.getFloat("sfxVolume", 1f);
            d.bgmVolume = v.getFloat("bgmVolume", 0.6f);
            d.lastStage = v.getString("lastStage", "st_void");
            return d;
        } catch (Exception e) {
            platform.log("save decode failed: " + e.getMessage());
            return null;
        }
    }

    // ---- 稳定序列化小工具 ----

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static String strArray(Array<String> arr) {
        StringBuilder sb = new StringBuilder("[");
        if (arr != null) {
            for (int i = 0; i < arr.size; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(escape(arr.get(i))).append('"');
            }
        }
        return sb.append(']').toString();
    }

    private static String strArray(JsonValue v) {
        StringBuilder sb = new StringBuilder("[");
        if (v != null) {
            for (JsonValue e : v) {
                if (sb.length() > 1) sb.append(',');
                sb.append('"').append(escape(e.isString() ? e.asString() : e.toString())).append('"');
            }
        }
        return sb.append(']').toString();
    }

    private static String strMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        if (map != null) {
            boolean first = true;
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(e.getKey())).append("\":\"")
                        .append(escape(e.getValue())).append('"');
            }
        }
        return sb.append('}').toString();
    }

    private static String strMap(JsonValue v) {
        StringBuilder sb = new StringBuilder("{");
        if (v != null) {
            for (JsonValue e : v) {
                if (sb.length() > 1) sb.append(',');
                sb.append('"').append(escape(e.name)).append("\":\"")
                        .append(escape(e.isString() ? e.asString() : e.toString())).append('"');
            }
        }
        return sb.append('}').toString();
    }

    private static String runeMap(Map<String, Array<String>> map) {
        StringBuilder sb = new StringBuilder("{");
        if (map != null) {
            boolean first = true;
            for (Map.Entry<String, Array<String>> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(e.getKey())).append("\":").append(strArray(e.getValue()));
            }
        }
        return sb.append('}').toString();
    }

    private static String runeMap(JsonValue v) {
        StringBuilder sb = new StringBuilder("{");
        if (v != null) {
            for (JsonValue e : v) {
                if (sb.length() > 1) sb.append(',');
                sb.append('"').append(escape(e.name)).append("\":").append(strArray(e));
            }
        }
        return sb.append('}').toString();
    }

    private static Array<String> toArray(JsonValue v) {
        Array<String> out = new Array<>();
        if (v != null) {
            for (JsonValue e : v) {
                out.add(e.isString() ? e.asString() : e.toString());
            }
        }
        return out;
    }

    private static Map<String, String> toMap(JsonValue v) {
        Map<String, String> out = new LinkedHashMap<>();
        if (v != null) {
            for (JsonValue e : v) {
                out.put(e.name, e.isString() ? e.asString() : e.toString());
            }
        }
        return out;
    }

    private static Map<String, Array<String>> toRuneMap(JsonValue v) {
        Map<String, Array<String>> out = new LinkedHashMap<>();
        if (v != null) {
            for (JsonValue e : v) {
                out.put(e.name, toArray(e));
            }
        }
        return out;
    }

    static String crc32(String s) {
        CRC32 c = new CRC32();
        c.update(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Long.toHexString(c.getValue());
    }
}
