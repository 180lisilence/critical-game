package com.yunluhezi.maxcrit.config;

import java.util.ArrayList;
import java.util.List;

/** 天赋树配置：瞬杀 / 永续 / 神佑 三系。 */
public class TalentDef {
    public String id;
    public String name;
    /** 系：kill / endless / bless */
    public String branch;
    public String desc;
    public int tier = 1;
    public int cost = 1;
    /** 需要的父节点。 */
    public String parent;
    /** 效果参数。 */
    public float value;

    public static List<TalentDef> builtin() {
        List<TalentDef> l = new ArrayList<>();
        add(l, "tl_k1", "锋锐", "kill", "暴击率 +5%", 1, 1, null, 5f);
        add(l, "tl_k2", "碎星", "kill", "爆伤 +20%", 2, 1, "tl_k1", 20f);
        add(l, "tl_k3", "瞬杀", "kill", "对残血目标额外 +25% 伤害", 3, 2, "tl_k2", 25f);
        add(l, "tl_e1", "迅捷", "endless", "移速 +8%", 1, 1, null, 8f);
        add(l, "tl_e2", "永续", "endless", "最大生命 +15%", 2, 1, "tl_e1", 15f);
        add(l, "tl_e3", "不息", "endless", "攻速 +10%", 3, 2, "tl_e2", 10f);
        add(l, "tl_b1", "护盾", "bless", "开局获得 1 次护盾", 1, 1, null, 1f);
        add(l, "tl_b2", "神佑", "bless", "受到的伤害 -12%", 2, 1, "tl_b1", 12f);
        add(l, "tl_b3", "回溯", "bless", "每局开始时回复 30% 生命", 3, 2, "tl_b2", 30f);
        return l;
    }

    private static void add(List<TalentDef> l, String id, String name, String branch, String desc,
                            int tier, int cost, String parent, float value) {
        TalentDef d = new TalentDef();
        d.id = id; d.name = name; d.branch = branch; d.desc = desc;
        d.tier = tier; d.cost = cost; d.parent = parent; d.value = value;
        l.add(d);
    }
}
