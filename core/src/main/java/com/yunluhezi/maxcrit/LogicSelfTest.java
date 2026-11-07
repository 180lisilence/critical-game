package com.yunluhezi.maxcrit;

import com.yunluhezi.maxcrit.battle.damage.DamageCalculator;
import com.yunluhezi.maxcrit.battle.wave.WaveManager;
import com.yunluhezi.maxcrit.config.ConfigHub;
import com.yunluhezi.maxcrit.config.MonsterDef;
import com.yunluhezi.maxcrit.config.SkillDef;
import com.yunluhezi.maxcrit.save.MetaProgress;
import com.yunluhezi.maxcrit.save.SaveManager;

import java.util.List;

/**
 * 无头逻辑自测：不需要图形环境，直接验证数值系统。
 * 运行： java -cp <core classes>:<gdx jar> com.yunluhezi.maxcrit.LogicSelfTest
 */
public class LogicSelfTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        testDamageBasic();
        testOverCrit();
        testCritResist();
        testWaveSequence();
        testMetaProgress();
        testSkillRolling();
        testCrcSave();
        testWaveSpawnsAll();
        testSaveRoundTrip();
        testSkillNoExponential();

        System.out.println();
        System.out.println("========================================");
        System.out.println("  通过: " + pass + "   失败: " + fail);
        System.out.println("========================================");
        if (fail > 0) System.exit(1);
    }

    private static void check(String name, boolean cond, String detail) {
        if (cond) {
            pass++;
            System.out.println("  [PASS] " + name + "  " + detail);
        } else {
            fail++;
            System.out.println("  [FAIL] " + name + "  " + detail);
        }
    }

    // ---------- 伤害乘区 ----------
    private static void testDamageBasic() {
        System.out.println("[伤害乘区]");
        // 固定暴击率 0% → 必定不暴击
        float sum = 0;
        int n = 200;
        for (int i = 0; i < n; i++) {
            DamageCalculator.Result r = DamageCalculator.compute(
                    100f, 0f, 50f, 1f, 1f, 1f, 1f, 1f, 0f, true);
            sum += r.damage;
            if (r.crit) { check("0% 暴击率不触发暴击", false, "出现了暴击"); return; }
        }
        float avg = sum / n;
        check("0% 暴击率", true, "平均伤害 " + avg + "（应为 100）");
        check("基础伤害无暴击时等于 100", Math.abs(avg - 100f) < 0.01f, "avg=" + avg);

        // 100% 暴击率 → 必定暴击，伤害 = 100 * (1+0.5) = 150
        float avg2 = 0;
        for (int i = 0; i < n; i++) {
            DamageCalculator.Result r = DamageCalculator.compute(
                    100f, 100f, 50f, 1f, 1f, 1f, 1f, 1f, 0f, true);
            avg2 += r.damage;
            if (!r.crit) { check("100% 暴击率必定暴击", false, "出现了非暴击"); return; }
        }
        avg2 /= n;
        check("100% 暴击率", true, "平均伤害 " + avg2 + "（应为 150）");
        check("暴击伤害 = 基础 × (1+爆伤)", Math.abs(avg2 - 150f) < 0.01f, "avg=" + avg2);

        // 暴击回溯
        DamageCalculator.Result rr = DamageCalculator.compute(
                100f, 100f, 50f, 1f, 1f, 1f, 1f, 1f, 0f, true);
        check("暴击回溯回血", rr.healBack > 0f, "healBack=" + rr.healBack);
        check("暴击回溯减CD", rr.cdRefund > 0f, "cdRefund=" + rr.cdRefund);
    }

    private static void testOverCrit() {
        System.out.println("[满级暴击]");
        // 暴击率 90% + 爆伤 200% → 达到满级阈值
        int over = 0;
        for (int i = 0; i < 300; i++) {
            DamageCalculator.Result r = DamageCalculator.compute(
                    100f, 90f, 200f, 1f, 1f, 1f, 1f, 1f, 0f, true);
            if (r.overCrit) over++;
        }
        check("满级暴击能触发", over > 200, "300 次中触发 " + over + " 次");

        // 未达阈值（暴击率 90% 但爆伤只有 50%）
        int over2 = 0;
        for (int i = 0; i < 300; i++) {
            DamageCalculator.Result r = DamageCalculator.compute(
                    100f, 90f, 50f, 1f, 1f, 1f, 1f, 1f, 0f, true);
            if (r.overCrit) over2++;
        }
        check("爆伤不足时不触发满级暴击", over2 == 0, "触发 " + over2 + " 次（应为 0）");

        // 满级暴击伤害倍率校验
        DamageCalculator.Result r3 = DamageCalculator.compute(
                100f, 100f, 200f, 1f, 1f, 1f, 1f, 1f, 0f, true);
        // 100 * (1+2.0) * 2.5 = 750
        check("满级暴击伤害 = 基础×爆伤×2.5", Math.abs(r3.damage - 750f) < 0.01f, "damage=" + r3.damage);

        // 冲击波伤害
        float sw = DamageCalculator.overCritShockwave(100f, 200f);
        check("冲击波伤害 = 基础×爆伤×0.6", Math.abs(sw - 180f) < 0.01f, "sw=" + sw);
    }

    private static void testCritResist() {
        System.out.println("[暴击抗性]");
        // 目标抗性 50%，玩家暴击率 60% → 有效 10%
        int crit = 0;
        for (int i = 0; i < 2000; i++) {
            DamageCalculator.Result r = DamageCalculator.compute(
                    10f, 60f, 50f, 1f, 1f, 1f, 1f, 1f, 50f, true);
            if (r.crit) crit++;
        }
        float rate = crit / 2000f;
        check("抗性 50% 削掉一半暴击率", rate > 0.05f && rate < 0.16f,
                String.format("实测暴击率 %.1f%%（期望 ~10%%）", rate * 100));

        // 抗性超过暴击率 → 有效 0，永不暴击
        int crit2 = 0;
        for (int i = 0; i < 500; i++) {
            DamageCalculator.Result r = DamageCalculator.compute(
                    10f, 30f, 50f, 1f, 1f, 1f, 1f, 1f, 80f, true);
            if (r.crit) crit2++;
        }
        check("抗性压过暴击率则不暴击", crit2 == 0, "暴击 " + crit2 + " 次（应为 0）");
    }

    // ---------- 波次 ----------
    private static void testWaveSequence() {
        System.out.println("[波次系统]");
        ConfigHub cfg = new ConfigHub();
        cfg.loadAll();
        check("配置表加载", cfg.skills.size() > 0 && cfg.monsters.size() > 0,
                "技能 " + cfg.skills.size() + " 个，怪物 " + cfg.monsters.size() + " 种");

        WaveManager wm = new WaveManager(cfg, 8, 1f);
        check("总波数", wm.totalWaves() == 8, "totalWaves=" + wm.totalWaves());

        // 推进到第一波
        wm.update(0.1f, 0);
        check("首波自动开始", wm.currentWave == 0, "currentWave=" + wm.currentWave);

        // 依次清场推进，最终应 finished
        int guard = 0;
        while (!wm.finished && guard++ < 5000) {
            wm.update(0.05f, 0);
            // 必须走完整刷怪流程：shouldSpawn 判定 + markSpawned 记账
            if (wm.shouldSpawn()) wm.markSpawned();
        }
        check("波次能推进到结束", wm.finished, "guard=" + guard);

        // 最后一波应为 BOSS
        WaveManager wm2 = new WaveManager(cfg, 8, 1f);
        wm2.currentWave = 7;
        check("最后一波是 BOSS", wm2.current().boss, "monster=" + wm2.current().monsterId);

        // 难度膨胀
        WaveManager wm3 = new WaveManager(cfg, 8, 1f);
        wm3.currentWave = 0;
        float hp0 = wm3.current().hpScale;
        wm3.currentWave = 5;
        float hp5 = wm3.current().hpScale;
        check("波次血量随进度膨胀", hp5 > hp0, "波0 hpScale=" + hp0 + " → 波5 hpScale=" + hp5);
    }

    // ---------- 养成换算 ----------
    private static void testMetaProgress() {
        System.out.println("[养成换算]");
        SaveManager.SaveData d = new SaveManager.SaveData();
        List<com.yunluhezi.maxcrit.config.EquipDef> eqs =
                com.yunluhezi.maxcrit.config.EquipDef.builtin();
        List<com.yunluhezi.maxcrit.config.TalentDef> tals =
                com.yunluhezi.maxcrit.config.TalentDef.builtin();

        MetaProgress.Stats base = MetaProgress.compute(d, eqs, tals);
        check("初始面板", base.critRate == 5f && base.critDamage == 50f,
                "暴击率=" + base.critRate + " 爆伤=" + base.critDamage);

        // 加印记
        d.echoMarks = 100;
        MetaProgress.Stats s2 = MetaProgress.compute(d, eqs, tals);
        check("回溯印记加暴击率", s2.critRate > base.critRate,
                "100 印记后暴击率=" + s2.critRate + "（应 > 5）");
        check("回溯印记加爆伤", s2.critDamage > base.critDamage,
                "100 印记后爆伤=" + s2.critDamage + "（应 > 50）");

        // 穿装备（用干净存档，避免前面的印记把暴击率顶到 100 上限）
        SaveManager.SaveData dEq = new SaveManager.SaveData();
        dEq.equipped.put("weapon", "eq_wpn_02");
        dEq.equipped.put("ring", "eq_rng_01");
        MetaProgress.Stats s3 = MetaProgress.compute(dEq, eqs, tals);
        check("装备加属性", s3.critRate > 5f && s3.critDamage > 50f,
                "暴击率=" + s3.critRate + " 爆伤=" + s3.critDamage);

        // 套装 2 件生效
        int crack = 0;
        for (String slot : MetaProgress.SLOTS) {
            String id = dEq.equipped.get(slot);
            if (id == null) continue;
            for (com.yunluhezi.maxcrit.config.EquipDef e : eqs) {
                if (e.id.equals(id) && "set_crack".equals(e.setId)) crack++;
            }
        }
        check("裂隙套 2 件已激活", crack >= 2, "裂隙套件数=" + crack);

        // 天赋（同样用干净存档）
        SaveManager.SaveData dTal = new SaveManager.SaveData();
        dTal.talents.add("tl_k1");
        dTal.talents.add("tl_k2");
        MetaProgress.Stats s4 = MetaProgress.compute(dTal, eqs, tals);
        check("天赋加暴击/爆伤", s4.critRate > 5f && s4.critDamage > 50f,
                "暴击率=" + s4.critRate + " 爆伤=" + s4.critDamage);

        // 上限校验：暴击率不超过 100、爆伤不超过 500
        SaveManager.SaveData dCap = new SaveManager.SaveData();
        dCap.echoMarks = 999999;
        MetaProgress.Stats s5 = MetaProgress.compute(dCap, eqs, tals);
        check("暴击率上限 100", s5.critRate <= 100f, "critRate=" + s5.critRate);
        check("爆伤上限 500", s5.critDamage <= 500f, "critDamage=" + s5.critDamage);
    }

    // ---------- 技能抽取 ----------
    private static void testSkillRolling() {
        System.out.println("[技能三选一]");
        ConfigHub cfg = new ConfigHub();
        cfg.loadAll();
        com.yunluhezi.maxcrit.battle.skill.SkillSystem ss =
                new com.yunluhezi.maxcrit.battle.skill.SkillSystem(cfg);

        List<SkillDef> three = ss.rollThree(1);
        check("能抽到 3 个技能", three.size() == 3, "抽到 " + three.size() + " 个");
        boolean distinct = true;
        for (int i = 0; i < three.size(); i++)
            for (int j = i + 1; j < three.size(); j++)
                if (three.get(i).id.equals(three.get(j).id)) distinct = false;
        check("候选不重复", distinct, three.toString());

        // 满级后不再出现
        for (SkillDef d : cfg.skills) {
            for (int i = 0; i < d.maxStack; i++) ss.acquire(d);
        }
        List<SkillDef> none = ss.rollThree(10);
        check("全部满级后不再抽到", none.isEmpty(), "剩余候选 " + none.size());

        // 稀有度：配置中存在多档稀有度（实际最高 4 档）
        SkillDef rare = null, common = null;
        int maxRarity = 0;
        for (SkillDef d : cfg.skills) {
            if (d.rarity == 4 && rare == null) rare = d;
            if (d.rarity == 1 && common == null) common = d;
            if (d.rarity > maxRarity) maxRarity = d.rarity;
        }
        check("配置中存在多档稀有度", rare != null && common != null && maxRarity >= 4,
                "稀有度范围 1~" + maxRarity);
    }

    // ---------- 存档 CRC ----------
    private static void testCrcSave() {
        System.out.println("[存档校验]");
        SaveManager.SaveData d = new SaveManager.SaveData();
        d.echoMarks = 777;
        d.maxTowerFloor = 42;
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        String payload = json.toJson(d);
        java.util.zip.CRC32 c = new java.util.zip.CRC32();
        c.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String crc = Long.toHexString(c.getValue());
        check("CRC 生成", crc != null && crc.length() > 0, "crc=" + crc);

        // 篡改后 CRC 必须不同
        String tampered = payload.replace("777", "999");
        java.util.zip.CRC32 c2 = new java.util.zip.CRC32();
        c2.update(tampered.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        check("篡改被检出", !crc.equals(Long.toHexString(c2.getValue())),
                "原=" + crc + " 篡改后=" + Long.toHexString(c2.getValue()));
    }

    // ---------- 回归 1：波次必须刷满全部怪物数 ----------
    private static void testWaveSpawnsAll() {
        System.out.println("[回归] 波次刷怪数");
        ConfigHub cfg = new ConfigHub();
        cfg.loadAll();

        // 1) 整局：累计刷出的怪数必须等于各波 monsterCount 之和
        WaveManager wm = new WaveManager(cfg, 8, 1f);
        int guard = 0;
        int totalSpawned = 0;
        int expected = 0;
        int lastWave = -1;
        while (!wm.finished && guard++ < 5000) {
            WaveManager.Wave cur = wm.current();
            if (cur != null && cur.index != lastWave) {
                lastWave = cur.index;
                expected += cur.monsterCount;
            }
            wm.update(0.05f, 0);
            if (wm.shouldSpawn()) {
                wm.markSpawned();
                totalSpawned++;
            }
        }
        check("整局刷出怪物数 = 各波总数", totalSpawned == expected && totalSpawned > 0,
                "实际刷出 " + totalSpawned + " 只，期望 " + expected + " 只");

        // 2) 单波：第一波必须刷满 monsterCount 只
        WaveManager wm2 = new WaveManager(cfg, 8, 1f);
        wm2.update(0.1f, 0);
        WaveManager.Wave w1 = wm2.current();
        int want = w1.monsterCount;
        int cnt = 0;
        for (int i = 0; i < 5000 && cnt < want; i++) {
            wm2.update(0.05f, 0);
            if (wm2.shouldSpawn()) { wm2.markSpawned(); cnt++; }
        }
        check("单波刷满 monsterCount", cnt == want,
                "第 1 波刷出 " + cnt + " / " + want + " 只");

        // 3) 连续两波都能刷满（验证计时器重置正确，不是只在第一波生效）
        WaveManager wm3 = new WaveManager(cfg, 8, 1f);
        wm3.update(0.1f, 0);
        int c1 = 0;
        for (int i = 0; i < 5000 && c1 < wm3.current().monsterCount; i++) {
            wm3.update(0.05f, 0);
            if (wm3.shouldSpawn()) { wm3.markSpawned(); c1++; }
        }
        // 第一波刷满后，需要场上清空才会切波；切波后继续刷第二波
        int c2 = 0;
        for (int i = 0; i < 5000; i++) {
            wm3.update(0.05f, 0);   // aliveMonsters=0 → 本波刷满即切波
            if (wm3.currentWave > 0) break;
        }
        int want2 = wm3.current() != null ? wm3.current().monsterCount : 0;
        for (int i = 0; i < 5000 && c2 < want2; i++) {
            wm3.update(0.05f, 0);
            if (wm3.shouldSpawn()) { wm3.markSpawned(); c2++; }
        }
        check("第二波也能继续刷怪", c2 == want2 && want2 > 0,
                "第 2 波刷出 " + c2 + " / " + want2 + " 只");
    }

    // ---------- 回归 2：存档 CRC 往返一致（不能读档即清档） ----------
    private static void testSaveRoundTrip() {
        System.out.println("[回归] 存档读写往返");
        com.yunluhezi.maxcrit.core.PlatformBridge bridge =
                new com.yunluhezi.maxcrit.core.PlatformBridge(null);
        bridge.setSavePath("selftest_save.json");
        // 清掉上次运行残留，保证断言从空档开始
        try {
            com.badlogic.gdx.Gdx.files.local("selftest_save.json").delete();
        } catch (Throwable ignored) {
            // 无头环境下若不可写则忽略，下面断言仍会校验往返一致性
        }

        SaveManager sm = new SaveManager(bridge);
        sm.load();
        sm.data().echoMarks = 1234;
        sm.data().maxTowerFloor = 17;
        sm.data().biggestCrit = 9999.5f;
        sm.data().unlockedEquips.add("eq_wpn_01");
        sm.data().unlockedEquips.add("eq_rng_01");
        sm.data().equipped.put("weapon", "eq_wpn_02");
        sm.data().runes.put("eq_wpn_02", new com.badlogic.gdx.utils.Array<>(
                new String[]{"rune_atk"}));
        sm.data().talents.add("tl_k1");
        sm.data().clearedStages.add("st_void");
        sm.save();

        // 重新构造一个 SaveManager 模拟「重启游戏后读档」
        SaveManager sm2 = new SaveManager(bridge);
        sm2.load();
        SaveManager.SaveData d2 = sm2.data();
        check("读档后印记保留", d2.echoMarks == 1234, "echoMarks=" + d2.echoMarks);
        check("读档后爬塔层数保留", d2.maxTowerFloor == 17, "floor=" + d2.maxTowerFloor);
        check("读档后最大暴击保留", Math.abs(d2.biggestCrit - 9999.5f) < 0.01f,
                "crit=" + d2.biggestCrit);
        check("读档后装备列表保留", d2.unlockedEquips.size == 2,
                "unlocked=" + d2.unlockedEquips);
        check("读档后穿戴保留",
                "eq_wpn_02".equals(d2.equipped.get("weapon")),
                "equipped=" + d2.equipped);
        check("读档后符文保留", d2.runes.containsKey("eq_wpn_02"),
                "runes=" + d2.runes.keySet());
        check("读档后天赋保留", d2.talents.contains("tl_k1", false),
                "talents=" + d2.talents);
        check("读档后关卡保留", d2.clearedStages.contains("st_void", false),
                "stages=" + d2.clearedStages);
        check("读档后模式未丢失", d2.unlockedModes.size >= 2,
                "modes=" + d2.unlockedModes);

        // 二次往返，确认不是「一次性巧合」
        sm2.save();
        SaveManager sm3 = new SaveManager(bridge);
        sm3.load();
        check("二次往返仍一致", sm3.data().echoMarks == 1234,
                "echoMarks=" + sm3.data().echoMarks);
    }

    // ---------- 回归 3：技能多次叠加不得平方/指数爆炸 ----------
    private static void testSkillNoExponential() {
        System.out.println("[回归] 技能叠加线性性");
        ConfigHub cfg = new ConfigHub();
        cfg.loadAll();
        SaveManager.SaveData sd = new SaveManager.SaveData();
        MetaProgress.Stats base = MetaProgress.compute(sd,
                com.yunluhezi.maxcrit.config.EquipDef.builtin(),
                com.yunluhezi.maxcrit.config.TalentDef.builtin());

        com.yunluhezi.maxcrit.battle.BattleWorld w =
                new com.yunluhezi.maxcrit.battle.BattleWorld(cfg, base);
        w.start(new com.yunluhezi.maxcrit.battle.BattleWorld.StageConfig());

        SkillDef bul = null, swift = null, greed = null;
        for (SkillDef d : cfg.skills) {
            if (d.id.equals("sk_bul_1")) bul = d;
            if (d.id.equals("sk_swift_1")) swift = d;
            if (d.id.equals("sk_gen_greed")) greed = d;
        }
        if (bul == null || swift == null || greed == null) {
            check("技能定义齐全", false, "缺少 sk_bul_1/sk_swift_1/sk_gen_greed");
            return;
        }

        // 最大生命：3 层 sk_bul_1 应为 base * 1.25 * 3（线性），而非 base * 1.25^3
        float hp0 = w.player.maxHp;
        float perLayer = hp0 * bul.value / 100f;
        for (int i = 0; i < 3; i++) w.applySkill(bul);
        float expectedHp = hp0 + perLayer * 3;
        check("最大生命线性叠加（非平方）",
                Math.abs(w.player.maxHp - expectedHp) < 0.01f,
                "实际 " + w.player.maxHp + "，期望 " + expectedHp
                        + "（平方会得到 " + (hp0 * 1.25f * 1.25f * 1.25f) + "）");

        // 攻速：从 base 重算，3 层 = base * 1.15^3
        float as0 = w.player.stats.attackSpeed;
        for (int i = 0; i < 3; i++) w.applySkill(swift);
        float expectedAs = as0 * (1f + swift.value / 100f) * (1f + swift.value / 100f)
                * (1f + swift.value / 100f);
        check("攻速按倍率重算（幂等可预期）",
                Math.abs(w.player.stats.attackSpeed - expectedAs) < 0.001f,
                "实际 " + w.player.stats.attackSpeed + "，期望 " + expectedAs);

        // 经验倍率：3 层贪婪 = 1.25^3，且 addExp 不再二次相乘
        for (int i = 0; i < 3; i++) w.applySkill(greed);
        float expectedMul = 1.25f * 1.25f * 1.25f;
        check("经验倍率只累乘一次",
                Math.abs(w.player.stats.expGain - expectedMul) < 0.001f,
                "expGain=" + w.player.stats.expGain + " 期望 " + expectedMul);

        // 经验倍率只在掉落处生效一次：addExp 收到什么就加什么（用极大升级门槛避免升级扣经验干扰）
        float expBefore = w.player.exp;
        w.player.expToNext = 1_000_000f;
        w.player.addExp(100f);
        check("addExp 不再平方经验",
                Math.abs((w.player.exp - expBefore) - 100f) < 0.01f,
                "实际获得 " + (w.player.exp - expBefore) + "，期望 100（平方会得到 "
                        + (100f * expectedMul) + "）");
    }
}
