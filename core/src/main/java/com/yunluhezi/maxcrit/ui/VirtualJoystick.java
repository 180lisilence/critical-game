package com.yunluhezi.maxcrit.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.yunluhezi.maxcrit.core.MaxCritGame;
import com.yunluhezi.maxcrit.util.Ui;

/**
 * 虚拟摇杆（Android 触屏 / PC 可用鼠标拖拽）。
 * 屏幕左下角固定基座，右下角也可开一个（双手操作）。
 */
public class VirtualJoystick {

    /** 摇杆输出方向（屏幕坐标系，已归一化，长度 0~1）。 */
    public final Vector2 dir = new Vector2();

    private float baseX, baseY;
    private final float knobRadius = 26f;
    private final float baseRadius = 62f;
    private final float maxDist = 78f;

    private float knobX, knobY;
    private int touchPointer = -1;
    private boolean active;
    /** 当前触控位置（世界/屏幕坐标）。 */
    private float touchX, touchY;

    public VirtualJoystick(float baseX, float baseY) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.knobX = baseX;
        this.knobY = baseY;
    }

    /** 处理触屏输入。 */
    public void touchDown(int pointer, float x, float y) {
        if (touchPointer != -1) return;
        touchPointer = pointer;
        touchX = x;
        touchY = y;
        // 若按下位置在基座附近，则从基座开始；否则在按下处生成浮动基座
        float d = dist(x, y, baseX, baseY);
        if (d > baseRadius * 1.8f) {
            // 浮动摇杆
            baseX = x;
            baseY = y;
        }
        knobX = x;
        knobY = y;
        active = true;
        recompute();
    }

    public void touchDrag(int pointer, float x, float y) {
        if (pointer != touchPointer) return;
        touchX = x;
        touchY = y;
        recompute();
    }

    public void touchUp(int pointer) {
        if (pointer != touchPointer) return;
        touchPointer = -1;
        active = false;
        dir.setZero();
        knobX = baseX;
        knobY = baseY;
    }

    private void recompute() {
        float dx = touchX - baseX;
        float dy = touchY - baseY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > maxDist) {
            dx = dx / d * maxDist;
            dy = dy / d * maxDist;
            d = maxDist;
        }
        knobX = baseX + dx;
        knobY = baseY + dy;
        if (d > 6f) {
            dir.set(dx / maxDist, dy / maxDist);
        } else {
            dir.setZero();
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void render(Batch batch, BitmapFont font) {
        // 基座
        batch.setColor(0.35f, 0.2f, 0.6f, 0.35f);
        Ui.circle(batch, baseX, baseY, baseRadius);
        batch.setColor(0.75f, 0.55f, 1f, 0.7f);
        Ui.circle(batch, baseX, baseY, baseRadius);
        // 摇杆头
        batch.setColor(0.8f, 0.6f, 1f, 0.9f);
        Ui.circle(batch, knobX, knobY, knobRadius);
        batch.setColor(1f, 1f, 1f, 0.85f);
        Ui.circle(batch, knobX, knobY, knobRadius * 0.45f);
        batch.setColor(Color.WHITE);
    }
}
