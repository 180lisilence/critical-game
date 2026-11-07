package com.yunluhezi.maxcrit.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/** 伤害跳字（暴击金色 / 满级暴击紫金）。 */
public class FloatingText {
    public float x, y;
    public String text;
    public float life = 0.9f;
    public float maxLife = 0.9f;
    public Color color;
    public float scale = 1f;
    public float vy = -60f;

    public FloatingText(float x, float y, String text, Color color, float scale) {
        this.x = x; this.y = y; this.text = text; this.color = color; this.scale = scale;
        this.life = maxLife;
    }

    public void update(float dt) {
        life -= dt;
        y += vy * dt;
        vy *= 0.94f;
    }

    public boolean isDead() { return life <= 0f; }
}
