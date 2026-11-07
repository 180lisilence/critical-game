package com.yunluhezi.maxcrit.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置表中心：技能 / 怪物 / 装备 / 天赋 / 关卡。
 * 全部 JSON 驱动，禁止硬编码数值；缺失时回落到程序内置默认值，保证游戏始终可启动。
 */
public class ConfigHub {

    public final List<SkillDef> skills = new ArrayList<>();
    public final List<MonsterDef> monsters = new ArrayList<>();
    public final List<EquipDef> equips = new ArrayList<>();
    public final List<TalentDef> talents = new ArrayList<>();
    public final List<StageDef> stages = new ArrayList<>();

    public void loadAll() {
        skills.clear();
        monsters.clear();
        equips.clear();
        talents.clear();
        stages.clear();

        loadArray("config/skill.json", "skills", SkillDef.class, skills);
        loadArray("config/monster.json", "monsters", MonsterDef.class, monsters);
        loadArray("config/equip.json", "equips", EquipDef.class, equips);
        loadArray("config/talent.json", "talents", TalentDef.class, talents);

        if (skills.isEmpty()) skills.addAll(ConfigHub.defaultSkills());
        if (monsters.isEmpty()) monsters.addAll(ConfigHub.defaultMonsters());
        if (equips.isEmpty()) equips.addAll(EquipDef.builtin());
        if (talents.isEmpty()) talents.addAll(TalentDef.builtin());
        stages.addAll(ConfigHub.defaultStages());
    }

    private <T> void loadArray(String path, String key, Class<T> clazz, List<T> out) {
        try {
            if (!Gdx.files.internal(path).exists()) return;
            JsonValue root = new JsonReader().parse(Gdx.files.internal(path));
            JsonValue arr = root.get(key);
            if (arr == null || arr.isArray() == false) return;
            for (JsonValue v : arr) {
                T item = new com.badlogic.gdx.utils.Json().fromJson(clazz, v.toString());
                if (item != null) out.add(item);
            }
        } catch (Exception e) {
            Gdx.app.error("ConfigHub", "load " + path + " failed: " + e.getMessage());
        }
    }

    // ===================== 内置默认配置 =====================

    public static List<SkillDef> defaultSkills() {
        List<SkillDef> l = new ArrayList<>();
        // 暴击瞬杀流
        s(l, "sk_crit_1", "裂隙贯穿", "crit", "主武器伤害 +20%", 1, 5, 12f, "skill_crit", 20f);
        s(l, "sk_crit_2", "超额暴击", "crit", "暴击率 +8%，爆伤 +15%", 2, 5, 9f, "skill_crit", 8f);
        s(l, "sk_crit_3", "满级冲击", "crit", "满级暴击时冲击波伤害 +40%", 3, 3, 5f, "skill_crit", 40f);
        // 极速连打流
        s(l, "sk_swift_1", "疾风连射", "swift", "攻速 +15%", 1, 5, 12f, "skill_swift", 15f);
        s(l, "sk_swift_2", "多重弹道", "swift", "子弹数量 +1", 2, 4, 8f, "skill_swift", 1f);
        s(l, "sk_swift_3", "极速", "swift", "暴击判定 +10%（对高攻速加成）", 3, 3, 5f, "skill_swift", 10f);
        // 虚空召唤流
        s(l, "sk_void_1", "虚空召唤", "void", "召唤 1 个自动攻击的虚空之影", 3, 3, 6f, "skill_void", 1f);
        s(l, "sk_void_2", "深渊凝视", "void", "召唤物伤害 +50%", 2, 4, 7f, "skill_void", 50f);
        s(l, "sk_void_3", "虚空裂隙", "void", "召唤物数量上限 +1", 4, 2, 4f, "skill_void", 1f);
        // 壁垒生存流
        s(l, "sk_bul_1", "能量护盾", "bulwark", "最大生命 +25%", 1, 5, 12f, "skill_bulwark", 25f);
        s(l, "sk_bul_2", "荆棘外壳", "bulwark", "受到伤害 -15%", 2, 4, 8f, "skill_bulwark", 15f);
        s(l, "sk_bul_3", "不朽", "bulwark", "每 30 秒免疫一次致命伤", 4, 2, 4f, "skill_bulwark", 1f);
        // 重生回溯流
        s(l, "sk_reb_1", "回溯印记", "rebirth", "每 60 秒获得 1 个印记（提升暴击率上限）", 3, 3, 6f, "skill_rebirth", 1f);
        s(l, "sk_reb_2", "死亡凝视", "rebirth", "死亡时原地复活并回复 50% 生命", 4, 1, 3f, "skill_rebirth", 50f);
        s(l, "sk_reb_3", "时之沙漏", "rebirth", "技能冷却缩减 20%", 2, 4, 7f, "skill_rebirth", 20f);
        // 通用
        s(l, "sk_gen_magnet", "磁力核心", "gen", "拾取半径 +80%", 1, 5, 10f, "icon_rune_grid", 80f);
        s(l, "sk_gen_greed", "贪婪", "gen", "经验获取 +25%", 1, 5, 10f, "icon_rune_grid", 25f);
        s(l, "sk_gen_god", "神之左手", "gen", "全部伤害 +10%", 3, 4, 8f, "icon_rune_grid", 10f);
        return l;
    }

    private static void s(List<SkillDef> l, String id, String name, String style, String desc,
                          int rarity, int maxStack, float weight, String icon, float value) {
        SkillDef d = new SkillDef();
        d.id = id; d.name = name; d.style = style; d.desc = desc;
        d.rarity = rarity; d.maxStack = maxStack; d.weight = weight;
        d.icon = icon; d.value = value;
        l.add(d);
    }

    public static List<MonsterDef> defaultMonsters() {
        List<MonsterDef> l = new ArrayList<>();
        m(l, "mon_stone", "石魔", 30, 55, 8, 0, 3, 22, "enemy_stone", "chase");
        m(l, "mon_demon", "暗影恶魔", 45, 80, 11, 5, 4, 24, "enemy_demon", "chase");
        m(l, "mon_golem", "熔核巨像", 120, 40, 16, 10, 8, 32, "enemy_golem", "chase");
        m(l, "mon_elite", "深渊精英", 260, 60, 22, 15, 15, 38, "enemy_demon", "chase");
        m(l, "mon_boss", "裂隙主宰", 1200, 45, 30, 25, 60, 60, "enemy_golem", "chase");
        return l;
    }

    private static void m(List<MonsterDef> l, String id, String name, float hp, float speed,
                          float dmg, float resist, float exp, float r, String tex, String ai) {
        MonsterDef d = new MonsterDef();
        d.id = id; d.name = name; d.hp = hp; d.speed = speed; d.damage = dmg;
        d.critResist = resist; d.exp = exp; d.radius = r; d.texture = tex; d.ai = ai;
        d.boss = id.equals("mon_boss");
        d.elite = id.equals("mon_elite") || d.boss;
        l.add(d);
    }

    public static List<StageDef> defaultStages() {
        List<StageDef> l = new ArrayList<>();
        st(l, "st_void", "虚空初隙", 1, 8, 60f);
        st(l, "st_star", "星陨深渊", 2, 10, 80f);
        st(l, "st_chaos", "混沌边界", 3, 12, 100f);
        st(l, "st_dawn", "破晓终境", 4, 14, 120f);
        return l;
    }

    private static void st(List<StageDef> l, String id, String name, int order, int waveCount, float monsterHpScale) {
        StageDef d = new StageDef();
        d.id = id; d.name = name; d.order = order; d.waveCount = waveCount;
        d.monsterHpScale = monsterHpScale;
        l.add(d);
    }

    public SkillDef skill(String id) {
        for (SkillDef s : skills) if (s.id.equals(id)) return s;
        return null;
    }

    public MonsterDef monster(String id) {
        for (MonsterDef m : monsters) if (m.id.equals(id)) return m;
        return null;
    }
}
