package com.yunluhezi.maxcrit.battle.damage;

import com.badlogic.gdx.math.MathUtils;

/**
 * 【自研满级暴击乘区系统】
 *
 * 伤害公式（严格按乘区顺序结算，每一乘区独立可调、便于数值平衡）：
 *
 *   最终伤害 = 基础攻击
 *             × 流派倍率        (skillStyleMul)
 *             × 神装套装系数    (setBonusMul)
 *             × 符文爆伤增幅    (runeMul)
 *             × 回溯印记永久加成 (echoMul)
 *             × 天赋/局内被动    (buffMul)
 *             × 暴击判定系数    (critMul)
 *             × 满级超额暴击系数 (overCritMul)
 *
 * 暴击判定：roll(0~100) < 暴击率 → 触发
 * 满级超额暴击：当暴击率与爆伤同时达到阈值（OVER_CRIT_CRIT_RATE / OVER_CRIT_DAMAGE_BONUS），
 *              暴击命中时额外触发【满级暴击】，附带范围冲击波。
 */
public class DamageCalculator {

    // ===== 满级阈值（可由配置表覆盖） =====
    /** 满级暴击的暴击率阈值（%）。 */
    public static final float OVER_CRIT_CRIT_RATE = 85f;
    /** 满级暴击的爆伤加成阈值（%）。 */
    public static final float OVER_CRIT_DAMAGE_BONUS = 150f;
    /** 满级暴击的额外伤害倍率。 */
    public static final float OVER_CRIT_MUL = 2.5f;

    /** 一次伤害结算的完整结果，供表现层（跳字/特效）使用。 */
    public static class Result {
        public float damage;
        public boolean crit;
        public boolean overCrit;
        /** 暴击回溯：本次命中回复的血量。 */
        public float healBack;
        /** 暴击回溯：本次命中缩减的技能 CD（秒）。 */
        public float cdRefund;
        /** 最终暴击率（含所有加成），用于 UI 展示。 */
        public float critRate;
        public float critDamageBonus;
    }

    /**
     * 完整伤害结算。
     *
     * @param baseAttack       基础攻击
     * @param critRate         暴击率（%），0~100
     * @param critDamageBonus  爆伤加成（%），150 表示 2.5 倍
     * @param styleMul         流派倍率
     * @param setBonusMul      神装套装系数
     * @param runeMul          符文爆伤增幅
     * @param echoMul          回溯印记永久加成
     * @param buffMul          天赋/局内被动倍率
     * @param targetCritResist 目标暴击抗性（%），会等效削减暴击率
     * @param allowOverCrit    是否允许触发满级暴击
     */
    public static Result compute(float baseAttack,
                                  float critRate,
                                  float critDamageBonus,
                                  float styleMul,
                                  float setBonusMul,
                                  float runeMul,
                                  float echoMul,
                                  float buffMul,
                                  float targetCritResist,
                                  boolean allowOverCrit) {
        Result r = new Result();

        // 暴击抗性：目标减免暴击率，最低 0
        float effectiveCrit = MathUtils.clamp(critRate - targetCritResist, 0f, 100f);
        r.critRate = effectiveCrit;
        r.critDamageBonus = critDamageBonus;

        // 满级暴击判定：暴击率与爆伤同时达阈值
        boolean overThreshold = effectiveCrit >= OVER_CRIT_CRIT_RATE
                && critDamageBonus >= OVER_CRIT_DAMAGE_BONUS;

        float mul = baseAttack * styleMul * setBonusMul * runeMul * echoMul * buffMul;

        // 暴击判定
        r.crit = MathUtils.random() * 100f < effectiveCrit;
        if (r.crit) {
            mul *= (1f + critDamageBonus / 100f);
            // 暴击回溯：回复血量 + 缩减主动技能 CD
            r.healBack = baseAttack * 0.02f;
            r.cdRefund = 0.15f;
        }

        // 满级超额暴击
        r.overCrit = r.crit && overThreshold && allowOverCrit;
        if (r.overCrit) {
            mul *= OVER_CRIT_MUL;
            r.healBack *= 2f;
        }

        r.damage = Math.max(1f, mul);
        return r;
    }

    /**
     * 满级暴击的冲击波伤害（独立于主伤害的一次 AOE 结算）。
     */
    public static float overCritShockwave(float baseAttack, float critDamageBonus) {
        return baseAttack * (1f + critDamageBonus / 100f) * 0.6f;
    }
}
