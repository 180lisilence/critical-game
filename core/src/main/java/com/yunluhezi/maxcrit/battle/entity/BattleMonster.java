package com.yunluhezi.maxcrit.battle.entity;

import com.badlogic.gdx.math.Vector2;
import com.yunluhezi.maxcrit.config.MonsterDef;

/** 怪物实体。 */
public class BattleMonster extends GameEntity {

    public MonsterDef def;
    /** 追踪目标。 */
    public final Vector2 target = new Vector2();
    /** 攻击冷却。 */
    public float attackCd = 0f;
    /** 轻微游走偏移，让队列不重叠。 */
    public float wanderPhase;
    /** 精英/BOSS 标记。 */
    public boolean elite;
    public boolean boss;
    /** 屏幕外休眠后重新激活的冷却。 */
    public float activateDelay;
    /** 受击硬直。 */
    public float hitStun;

    public void setup(MonsterDef d, float hpScale, float speedScale) {
        this.def = d;
        this.maxHp = d.hp * hpScale;
        this.hp = maxHp;
        this.speed = d.speed * speedScale;
        this.radius = d.radius;
        this.elite = d.elite;
        this.boss = d.boss;
        this.wanderPhase = (float) (Math.random() * Math.PI * 2);
        // 【修复】对象池复用时必须重置全部运行时状态。
        // 原实现没有重置 alive/dormant/deathTime/hitStun/attackCd，
        // 导致复用出来的怪物带着上一只的死亡计时（进 cleanup 被立刻回收）
        // 或硬直/攻击冷却残留（表现为新怪站着不动或一出生就接触伤害）。
        this.alive = true;
        this.dormant = false;
        this.deathTime = 0f;
        this.hitStun = 0f;
        this.flashTime = 0f;
        this.attackCd = 0f;
        this.activateDelay = 0f;
        this.target.set(0f, 0f);
        this.pos.set(0f, 0f);
    }

    public void update(float dt, float px, float py) {
        updateCommon(dt);
        if (!alive) return;
        if (dormant) {
            // 屏幕外只保留位置，不做 AI
            return;
        }
        if (hitStun > 0f) {
            hitStun -= dt;
            return;
        }
        float dx = px - pos.x;
        float dy = py - pos.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0.001f) {
            dx /= len;
            dy /= len;
        }
        // 轻微左右摆动，避免完全重叠
        float wob = (float) Math.sin(wanderPhase + pos.x * 0.01f) * 0.28f;
        float mx = dx - dy * wob;
        float my = dy + dx * wob;
        pos.x += mx * speed * dt;
        pos.y += my * speed * dt;
    }
}
