package com.yunluhezi.maxcrit.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.yunluhezi.maxcrit.core.MaxCritGame;
import com.yunluhezi.maxcrit.util.Ui;

/**
 * 启动错误提示屏：任何启动期异常都会落到这里，
 * 把崩溃原因显示在屏幕上（而不是直接闪退），便于定位。
 *
 * 【关键】这是最后一道防线，**自身绝不能抛异常**。
 * 早期版本直接用 game.assets.font 绘制，但字体恰恰是启动期最可能加载失败的资源之一；
 * 一旦 font 为 null，这里就会 NPE，把「可显示的报错」变成「又一次静默闪退」。
 * 现在所有绘制都做 null 兜底：字体不可用时改用 ShapeRenderer 画矩形 + 系统默认绘制。
 */
public class BootErrorScreen extends BaseScreen {

    private final SpriteBatch batch;
    private final String error;
    private final String stage;
    /** 字体是否可用。不可用时走无字降级渲染。 */
    private final boolean fontOk;

    public BootErrorScreen(MaxCritGame game, String error) {
        this(game, "boot", error);
    }

    public BootErrorScreen(MaxCritGame game, String stage, String error) {
        super(game);
        this.batch = new SpriteBatch();
        this.error = error == null ? "(no detail)" : error;
        this.stage = stage;
        boolean ok = false;
        try {
            ok = game.assets != null && game.assets.font != null;
        } catch (Throwable ignored) {
            ok = false;
        }
        this.fontOk = ok;
    }

    @Override
    public void render(float delta) {
        batch.setProjectionMatrix(uiVp.getCamera().combined);
        batch.begin();
        batch.setColor(0.06f, 0.02f, 0.04f, 1f);
        Ui.rect(batch, 0, 0, 1280, 720);
        batch.end();

        if (!fontOk) {
            // 字体都没有 → 只能画色块示意。至少不崩，且屏幕上有一片深色说明进入了本屏。
            batch.begin();
            batch.setColor(0.9f, 0.25f, 0.25f, 1f);
            Ui.rect(batch, 40, 560, 400, 48);
            batch.setColor(0.8f, 0.8f, 0.85f, 1f);
            for (int i = 0; i < 8; i++) {
                Ui.rect(batch, 40, 520 - i * 26, 1200 - (i % 3) * 180, 12);
            }
            batch.end();
            return;
        }

        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        try {
            game.assets.font.getData().setScale(2.2f);
            game.assets.font.setColor(1f, 0.4f, 0.4f, 1f);
            game.assets.font.draw(batch, "启动失败", 0, 600, 1280, Align.center, false);
            game.assets.font.getData().setScale(1f);

            game.assets.font.setColor(1f, 0.85f, 0.4f, 1f);
            game.assets.font.draw(batch, "阶段: " + stage, 40, 556, 1200, Align.left, false);

            game.assets.font.setColor(0.9f, 0.9f, 0.95f, 1f);
            game.assets.font.draw(batch, error, 40, 500, 1200, Align.left, true);

            game.assets.font.setColor(0.6f, 0.6f, 0.7f, 1f);
            game.assets.font.draw(batch,
                    "堆栈已写入: Android/data/com.yunluhezi.maxcrit/files/crash.log",
                    40, 100, 1200, Align.left, false);
        } catch (Throwable ignored) {
            // 绘制过程同样不允许抛出
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        try {
            vp.update(width, height, true);
            uiVp.update(width, height, true);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void dispose() {
        try {
            batch.dispose();
        } catch (Throwable ignored) {
        }
    }
}