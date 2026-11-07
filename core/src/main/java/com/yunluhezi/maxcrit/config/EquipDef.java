package com.yunluhezi.maxcrit.config;

import java.util.ArrayList;
import java.util.List;

/** 装备配置（对应 assets/config/equip.json）。 */
public class EquipDef {
    public String id;
    public String name;
    /** 部位：weapon/helmet/cape/amulet/boots/ring */
    public String slot;
    /** 品质：0凡品 1精良 2稀有 3史诗 4传世 5神域 6神话 */
    public int quality = 0;
    public String desc;
    /** 装备图标名。 */
    public String icon;
    /** 固有属性：加暴击率(%)。 */
    public float critRate;
    /** 加爆伤(%)。 */
    public float critDamage;
    /** 加攻击。 */
    public float attack;
    /** 加最大生命。 */
    public float hp;
    /** 加移速。 */
    public float moveSpeed;
    /** 套装 id，为空表示无套装。 */
    public String setId;

    public static String[] QUALITY_NAME = {
            "凡品", "精良", "稀有", "史诗", "传世", "神域", "神话"
    };

    public String qualityName() {
        int q = Math.max(0, Math.min(QUALITY_NAME.length - 1, quality));
        return QUALITY_NAME[q];
    }

    /** 全部神装条目（程序内置，无需外部表也能跑）。 */
    public static List<EquipDef> builtin() {
        List<EquipDef> list = new ArrayList<>();
        add(list, "eq_wpn_01", "裂隙之刃", "weapon", 2, 12f, 40f, 8f, 0f, 0f, "set_crack", "equip_weapon");
        add(list, "eq_wpn_02", "星陨之枪", "weapon", 3, 18f, 65f, 12f, 0f, 0f, "set_crack", "equip_weapon");
        add(list, "eq_hlm_01", "虚空头冠", "helmet", 2, 8f, 25f, 0f, 20f, 0f, "set_void", "equip_helmet");
        add(list, "eq_cape_01", "暗夜披风", "cape", 3, 10f, 30f, 0f, 30f, 10f, "set_void", "equip_cape");
        add(list, "eq_amn_01", "破晓护符", "amulet", 4, 25f, 80f, 0f, 50f, 0f, "set_dawn", "equip_amulet");
        add(list, "eq_bts_01", "疾行战靴", "boots", 2, 6f, 15f, 0f, 0f, 25f, "set_dawn", "equip_boots");
        add(list, "eq_rng_01", "暴击戒印", "ring", 5, 35f, 120f, 0f, 0f, 0f, "set_crack", "equip_ring");
        return list;
    }

    private static void add(List<EquipDef> l, String id, String name, String slot, int q,
                            float cr, float cd, float atk, float hp, float ms, String set, String icon) {
        EquipDef d = new EquipDef();
        d.id = id; d.name = name; d.slot = slot; d.quality = q;
        d.critRate = cr; d.critDamage = cd; d.attack = atk; d.hp = hp; d.moveSpeed = ms;
        d.setId = set; d.icon = icon;
        d.desc = "暴击率+" + (int) cr + "%  爆伤+" + (int) cd + "%"
                + (atk > 0 ? "  攻击+" + (int) atk : "")
                + (hp > 0 ? "  生命+" + (int) hp : "")
                + (ms > 0 ? "  移速+" + (int) ms : "");
        l.add(d);
    }
}
