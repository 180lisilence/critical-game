package com.yunluhezi.maxcrit.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * 所有 Screen 的公共基类：统一输入处理与视口。
 *
 * 【坐标系约定】本工程有两套坐标，必须严格区分：
 *  - 世界坐标：随相机移动（战斗场景用）。相机 position 被设成玩家位置，
 *              所以不能拿它做 UI 命中判定。
 *  - UI 坐标：固定 1280x720 设计分辨率（HUD / 摇杆 / 弹窗 / 按钮用）。
 *              由本类的 uiVp 统一提供：绘制用 uiVp 的投影矩阵，
 *              点击用 uiVp 反投影，两者必然一致。
 */
public abstract class BaseScreen extends InputAdapter implements Screen {

    protected com.yunluhezi.maxcrit.core.MaxCritGame game;
    protected FitViewport vp;
    /** UI 层专用视口：固定 1280x720，不跟随世界相机。 */
    protected FitViewport uiVp;

    public BaseScreen(com.yunluhezi.maxcrit.core.MaxCritGame game) {
        this.game = game;
        this.vp = new FitViewport(1280, 720);
        this.uiVp = new FitViewport(1280, 720);
        if (Gdx.graphics != null) {
            uiVp.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        }
    }

    public InputMultiplexer multiplexer() {
        return new InputMultiplexer(this);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }
}
