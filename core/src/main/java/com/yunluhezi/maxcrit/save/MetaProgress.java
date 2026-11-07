package com.yunluhezi.maxcrit.save;

import com.yunluhezi.maxcrit.config.EquipDef;
import com.yunluhezi.maxcrit.config.TalentDef;

import java.util.ArrayList;
import java.util.List;

/**
 * 局外养成 → 局内面板的换算。
 * 这是「每一局都更强」的核心：把回溯印记 / 神装 / 符文 / 天赋折算成战斗属性。
 */
public class MetaProgress {

    /** 局内战斗属性（进入对局时的基础面板）。 */
    public static class Stats {
        public float maxHp = 100f;
        public float attack = 10f;
        public float moveSpeed = 220f;
        public float attackSpeed = 1f;      // 次/秒
        public float critRate = 5f;         // %
        public float critDamage = 50f;      // % 爆伤加成
        public float damageReduction = 0f;  // %
        public float pickupRadius = 90f;
        public float expGain = 1f;
        public int bulletCount = 1;
        public int reviveCount = 0;
        public float cdReduction = 0f;      // %
        public float allDamageMul = 1f;
        public int echoGainPerMin = 0;
    }

    public static final String[] SLOTS = {"weapon", "helmet", "cape", "amulet", "boots", "ring"};

    /**
     * 计算最终入局属性。
     */
    public static Stats compute(SaveManager.SaveData sd, List<EquipDef> equipTable, List<TalentDef> talentTable) {
        Stats s = new Stats();

        // --- 回溯印记：每枚永久 +1.5% 暴击率、+4% 爆伤 ---
        float echoMul = 1f + sd.echoMarks * 0.015f;
        s.critRate = 5f + sd.echoMarks * 1.5f;
        s.critDamage = 50f + sd.echoMarks * 4f;

        // --- 神装 ---
        int setCounts[] = new int[8];
        for (String slot : SLOTS) {
            String eqId = sd.equipped.get(slot);
            if (eqId == null) continue;
            EquipDef def = find(equipTable, eqId);
            if (def == null) continue;
            s.critRate += def.critRate;
            s.critDamage += def.critDamage;
            s.attack += def.attack;
            s.maxHp += def.hp;
            s.moveSpeed += def.moveSpeed;
            if (def.setId != null && !def.setId.isEmpty()) {
                int idx = setIndex(def.setId);
                if (idx >= 0 && idx < setCounts.length) setCounts[idx]++;
            }
        }

        // --- 套装效果：2/4/6 件 ---
        // 裂隙套：暴击倍率；虚空套：召唤/移速；破晓套：生存
        s.critDamage += setEffect(setCounts, 0) * 12f;   // 裂隙：每层 +12% 爆伤
        s.moveSpeed += setEffect(setCounts, 1) * 6f;     // 虚空：每层 +6 移速
        s.maxHp += setEffect(setCounts, 2) * 15f;        // 破晓：每层 +15 生命
        s.damageReduction += setEffect(setCounts, 2) * 3f;

        // --- 符文（每个符文：+1.5% 暴击率、+5% 爆伤） ---
        int runeCount = 0;
        for (java.util.Map.Entry<String, com.badlogic.gdx.utils.Array<String>> en : sd.runes.entrySet()) {
            runeCount += en.getValue().size;
        }
        s.critRate += runeCount * 1.5f;
        s.critDamage += runeCount * 5f;

        // --- 天赋 ---
        for (String tid : sd.talents) {
            TalentDef t = findTalent(talentTable, tid);
            if (t == null) continue;
            switch (t.id) {
                case "tl_k1": s.critRate += t.value; break;
                case "tl_k2": s.critDamage += t.value; break;
                case "tl_e1": s.moveSpeed += t.value; break;
                case "tl_e2": s.maxHp += t.value * 3f; break;
                case "tl_e3": s.attackSpeed *= (1f + t.value / 100f); break;
                case "tl_b1": s.reviveCount += (int) t.value; break;
                case "tl_b2": s.damageReduction += t.value; break;
                case "tl_b3": s.maxHp *= 1f + t.value / 100f * 0.3f; break;
                default: break;
            }
        }

        s.critRate = Math.min(100f, s.critRate);
        s.critDamage = Math.min(500f, s.critDamage);
        s.damageReduction = Math.min(70f, s.damageReduction);
        s.allDamageMul *= echoMul > 1f ? 1f : 1f; // 回溯印记只加暴击属性，不额外乘区
        return s;
    }

    private static int setIndex(String setId) {
        switch (setId) {
            case "set_crack": return 0;
            case "set_void": return 1;
            case "set_dawn": return 2;
            default: return -1;
        }
    }

    /** 套装件数对应的层数效果：2件=1层，4件=2层，6件=3层。 */
    private static float setEffect(int[] counts, int idx) {
        int c = counts[idx];
        if (c >= 6) return 3;
        if (c >= 4) return 2;
        if (c >= 2) return 1;
        return 0;
    }

    private static EquipDef find(List<EquipDef> table, String id) {
        for (EquipDef d : table) if (d.id.equals(id)) return d;
        return null;
    }

    private static TalentDef findTalent(List<TalentDef> table, String id) {
        for (TalentDef d : table) if (d.id.equals(id)) return d;
        return null;
    }
}
