package com.yunluhezi.maxcrit.battle.entity;

import com.badlogic.gdx.math.Vector2;

/** 子弹（主武器自动发射）。 */
public class Bullet extends GameEntity {
    public final Vector2 dir = new Vector2();
    public float speed = 620f;
    public float damage = 10f;
    public float life = 1.6f;
    /** 是否为暴击弹（视觉区分）。 */
    public boolean wasCrit;
    public boolean overCrit;

    public void fire(float x, float y, float tx, float ty, float dmg, float spd) {
        pos.set(x, y);
        dir.set(tx - x, ty - y);
        if (dir.len2() < 0.0001f) dir.set(1, 0);
        dir.nor();
        damage = dmg;
        speed = spd;
        life = 1.6f;
        radius = 7f;
        alive = true;
        dormant = false;
        flashTime = 0f;
        deathTime = 0f;
        // 【修复】对象池复用时重置暴击标记，否则普通弹会继承上一发的满级暴击紫色
        wasCrit = false;
        overCrit = false;
    }

    public void update(float dt) {
        if (!alive) return;
        pos.x += dir.x * speed * dt;
        pos.y += dir.y * speed * dt;
        life -= dt;
        if (life <= 0f) alive = false;
    }
}
