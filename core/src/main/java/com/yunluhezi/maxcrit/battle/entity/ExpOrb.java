package com.yunluhezi.maxcrit.battle.entity;

import com.badlogic.gdx.math.Vector2;

/** 经验球。 */
public class ExpOrb extends GameEntity {
    public float value = 3f;
    public float life = 20f;
    /** 是否已被吸引。 */
    public boolean attracted;
    /** 掉落时的初速度。 */
    public final Vector2 scatter = new Vector2();

    public void drop(float x, float y, float v) {
        pos.set(x, y);
        value = v;
        life = 20f;
        radius = 9f;
        alive = true;
        attracted = false;
        deathTime = 0f;
        flashTime = 0f;
        dormant = false;
        float a = (float) (Math.random() * Math.PI * 2);
        float sp = 60f + (float) (Math.random() * 90f);
        scatter.set((float) Math.cos(a) * sp, (float) Math.sin(a) * sp);
    }

    public void update(float dt) {
        if (!alive) return;
        life -= dt;
        if (life <= 0f) { alive = false; return; }
        if (!attracted) {
            pos.x += scatter.x * dt;
            pos.y += scatter.y * dt;
            scatter.x *= 0.92f;
            scatter.y *= 0.92f;
        }
    }
}
