package com.yunluhezi.maxcrit.battle.entity;

import com.badlogic.gdx.math.Vector2;

/** 战斗实体基类：圆形碰撞体 + 位置 + 生命。 */
public abstract class GameEntity {
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    public float radius = 20f;
    /** 移动速度（像素/秒）。 */
    public float speed = 0f;
    public float hp = 10f;
    public float maxHp = 10f;
    public boolean alive = true;
    /** 屏幕外休眠标记（性能优化）。 */
    public boolean dormant = false;
    /** 受击闪白计时。 */
    public float flashTime = 0f;
    /** 死亡动画计时。 */
    public float deathTime = 0f;

    public void takeDamage(float dmg) {
        hp -= dmg;
        flashTime = 0.12f;
        if (hp <= 0f) {
            hp = 0f;
            alive = false;
            deathTime = 0.35f;
        }
    }

    public void updateCommon(float dt) {
        if (flashTime > 0f) flashTime -= dt;
        if (!alive && deathTime > 0f) deathTime -= dt;
    }
}
