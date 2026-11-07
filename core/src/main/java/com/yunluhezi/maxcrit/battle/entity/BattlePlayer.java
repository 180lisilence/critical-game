package com.yunluhezi.maxcrit.battle.entity;

import com.badlogic.gdx.math.Vector2;
import com.yunluhezi.maxcrit.battle.BattleWorld;
import com.yunluhezi.maxcrit.battle.damage.DamageCalculator;
import com.yunluhezi.maxcrit.save.MetaProgress;

/** 玩家实体（球形觉醒者）。 */
public class BattlePlayer extends GameEntity {

    /** 摇杆输入方向（已归一化，屏幕坐标系）。 */
    public final Vector2 moveInput = new Vector2();
    /** 局内属性。 */
    public MetaProgress.Stats stats = new MetaProgress.Stats();
    /** 当前生命。 */
    public float curHp;
    /** 护盾值。 */
    public float shield = 0f;
    /** 攻速计时器。 */
    public float attackTimer = 0f;
    /** 等级。 */
    public int level = 1;
    /** 本局经验。 */
    public float exp = 0f;
    /** 升到下一级所需经验。 */
    public float expToNext = 10f;
    /** 复活次数。 */
    public int revives;
    /** 复活是否已用完（按等级重置）。 */
    public int revivesUsed;
    /** 是否无敌（复活后短暂）。 */
    public float invincibleTime = 0f;
    /** 主武器突破等级（暴击突破晶）。 */
    public int weaponBreakLevel = 0;
    /** 死亡时触发。 */
    public boolean dead = false;
    /** 引用战斗场景。 */
    public BattleWorld world;

    public BattlePlayer() {
        radius = 24f;
    }

    public void applyStats(MetaProgress.Stats s) {
        this.stats = s;
        this.maxHp = s.maxHp;
        this.curHp = s.maxHp;
        this.revives = s.reviveCount;
        radius = 24f;
    }

    public void update(float dt) {
        updateCommon(dt);
        if (invincibleTime > 0f) invincibleTime -= dt;
        if (!alive) return;

        float speed = stats.moveSpeed;
        pos.x += moveInput.x * speed * dt;
        pos.y += moveInput.y * speed * dt;

        attackTimer -= dt;
        if (attackTimer <= 0f) {
            attackTimer = 1f / Math.max(0.1f, stats.attackSpeed);
            if (world != null) world.playerFire();
        }
    }

    public void addExp(float amount) {
        // 【修复】原来这里乘了 stats.expGain，而 applySkill 里又把 expGainMul 直接赋给
        // stats.expGain —— 经验收益被平方（1.25^5 ≈ 3 倍于预期）。现在 addExp 只加裸值，
        // 经验倍率统一由 BattleWorld.applySkill 写入 stats.expGain，此处不再乘。
        exp += amount;
        while (exp >= expToNext) {
            exp -= expToNext;
            level++;
            expToNext = 10f + level * 6f;
            if (world != null) world.onPlayerLevelUp();
        }
    }

    /** 受到伤害，返回实际扣血量。 */
    public float receiveDamage(float dmg) {
        if (invincibleTime > 0f || dead) return 0f;
        float real = dmg * (1f - stats.damageReduction / 100f);
        if (shield > 0f) {
            float absorbed = Math.min(shield, real);
            shield -= absorbed;
            real -= absorbed;
        }
        curHp -= real;
        if (curHp <= 0f) {
            curHp = 0f;
            if (revivesUsed < revives) {
                revivesUsed++;
                curHp = maxHp * 0.5f;
                invincibleTime = 2f;
                if (world != null) world.onPlayerRevive();
            } else {
                dead = true;
                alive = false;
                deathTime = 1.2f;
            }
        }
        return real;
    }

    /** 暴击回溯：回复生命。 */
    public void healBack(float amount) {
        curHp = Math.min(maxHp, curHp + amount);
    }
}
