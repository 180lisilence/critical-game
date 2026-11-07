package com.yunluhezi.maxcrit.battle.entity;

import com.badlogic.gdx.math.Vector2;

/** 召唤物（虚空之影）：自动攻击的随从。 */
public class Summon extends GameEntity {
    public float damage = 6f;
    public float attackCd = 0f;
    public float fireRate = 0.85f;
    public final Vector2 target = new Vector2();
    public float life = 999f;

    public void setup(float hp, float dmg, float rate) {
        this.maxHp = hp; this.hp = hp;
        this.damage = dmg;
        this.fireRate = rate;
        this.radius = 16f;
        this.alive = true;
        // 【修复】对象池复用时重置寿命与攻击冷却：
        // 原实现没重置 life，复用出来的召唤物寿命已被上一只耗尽 → 一出场就消失。
        this.life = 999f;
        this.attackCd = 0f;
        this.deathTime = 0f;
        this.flashTime = 0f;
        this.dormant = false;
        this.pos.set(0f, 0f);
        this.target.set(0f, 0f);
    }

    public void update(float dt, float px, float py) {
        updateCommon(dt);
        if (!alive) return;
        life -= dt;
        if (life <= 0f) { alive = false; return; }
        // 跟随玩家，保持距离
        float dx = px - pos.x, dy = py - pos.y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        float want = 90f;
        if (d > want) {
            dx /= Math.max(0.001f, d);
            dy /= Math.max(0.001f, d);
            pos.x += dx * 240f * dt;
            pos.y += dy * 240f * dt;
        } else if (d < want * 0.55f) {
            dx /= Math.max(0.001f, d);
            dy /= Math.max(0.001f, d);
            pos.x -= dx * 160f * dt;
            pos.y -= dy * 160f * dt;
        }
    }
}
