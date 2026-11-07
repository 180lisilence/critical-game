package com.yunluhezi.maxcrit.util;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * UI 绘制工具：白纹理（用于画纯色矩形/分隔条）+ 屏幕坐标转世界坐标。
 *
 * 说明：LibGDX 的 SpriteBatch 没有 getTexture()，无法像某些引擎那样直接
 * 拿当前纹理画矩形；这里自建一张 1x1 白色纹理，所有纯色块都用它绘制，
 * 既能控制颜色，又保持单次 batch 合批。
 */
public final class Ui {

    private Ui() {}

    private static Texture white;
    private static Texture whiteCircle;
    private static Texture softCircle;

    /** 1x1 纯白纹理，用于画任意纯色矩形。 */
    public static Texture white() {
        if (white == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 1f, 1f, 1f);
            pm.fill();
            white = new Texture(pm);
            pm.dispose();
            white.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        return white;
    }

    /** 圆形贴图（用 drawCircle 代替，此处保留给渐变需求）。 */
    public static Texture softCircle() {
        if (softCircle == null) {
            int size = 64;
            Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = (x + 0.5f) / size * 2f - 1f;
                    float dy = (y + 0.5f) / size * 2f - 1f;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    float a = Math.max(0f, 1f - d);
                    pm.setColor(1f, 1f, 1f, a * a);
                    pm.drawPixel(x, y);
                }
            }
            softCircle = new Texture(pm);
            pm.dispose();
            softCircle.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        return softCircle;
    }

    /** 画一个纯色矩形。 */
    public static void rect(Batch batch, float x, float y, float w, float h) {
        batch.draw(white(), x, y, w, h);
    }

    /**
     * 画一个实心圆。
     * LibGDX 的 Batch 没有 drawCircle（那是 ShapeRenderer 的 API），
     * 这里用程序生成的柔边圆形贴图绘制，视觉接近且保持合批。
     */
    public static void circle(Batch batch, float x, float y, float radius) {
        batch.draw(softCircle(), x - radius, y - radius, radius * 2f, radius * 2f);
    }

    /** 画一个带边框的纯色矩形（4 条边）。 */
    public static void frame(Batch batch, float x, float y, float w, float h, float thickness) {
        rect(batch, x, y, w, thickness);
        rect(batch, x, y + h - thickness, w, thickness);
        rect(batch, x, y, thickness, h);
        rect(batch, x + w - thickness, y, thickness, h);
    }

    /**
     * 屏幕像素坐标 → 世界坐标。
     * Viewport 没有 unprojectX/Y，这里基于相机自行换算。
     */
    public static Vector2 toWorld(Viewport viewport, float screenX, float screenY) {
        Vector3 v3 = new Vector3(screenX, screenY, 0f);
        viewport.getCamera().unproject(v3, viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        return new Vector2(v3.x, v3.y);
    }

    public static float toWorldX(Viewport viewport, float screenX) {
        return toWorld(viewport, screenX, 0f).x;
    }

    public static float toWorldY(Viewport viewport, float screenY) {
        return toWorld(viewport, 0f, screenY).y;
    }
}
