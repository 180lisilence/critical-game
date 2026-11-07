package com.yunluhezi.maxcrit.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.yunluhezi.maxcrit.core.MaxCritGame;
import com.yunluhezi.maxcrit.util.Ui;
import com.yunluhezi.maxcrit.save.SaveManager;
import com.yunluhezi.maxcrit.save.SaveManager.SaveData;

/**
 * 主菜单：以成品 UI 稿为底板，叠加动态数据层（回溯印记、最高层数等）。
 */
public class MenuScreen extends BaseScreen {

    private final SpriteBatch batch;
    private final SaveManager save;

    private Rectangle btnStart, btnTalent, btnBag, btnTower;
    private int hovered = -1;

    public MenuScreen(MaxCritGame game) {
        super(game);
        this.batch = new SpriteBatch();
        this.save = game.saveManager;
        layout();
    }

    private void layout() {
        btnStart = new Rectangle(520, 250, 240, 78);
        btnTalent = new Rectangle(520, 160, 240, 68);
        btnBag = new Rectangle(300, 160, 200, 68);
        btnTower = new Rectangle(780, 160, 200, 68);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        batch.setProjectionMatrix(uiVp.getCamera().combined);
        batch.begin();

        // 背景：优先用成品 UI 稿做底板
        com.badlogic.gdx.graphics.g2d.TextureRegion bg = game.assets.region("ui_screen_021");
        if (bg != null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(bg, 0, 0, 1280, 720);
        } else {
            batch.setColor(0.05f, 0.02f, 0.10f, 1f);
            Ui.rect(batch, 0, 0, 1280, 720);
        }

        // 标题
        com.badlogic.gdx.graphics.g2d.TextureRegion logo = game.assets.region("logo_banner");
        if (logo != null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(logo, 340, 470, 600, 220);
        } else {
            game.assets.font.getData().setScale(4f);
            game.assets.font.setColor(0.95f, 0.8f, 1f, 1f);
            game.assets.font.draw(batch, "重生之满级暴击", 0, 570, 1280, Align.center, false);
            game.assets.font.getData().setScale(1f);
        }

        // 按钮（半透明底板 + 文字，真实可点）
        drawButton(batch, btnStart, "开 始 游 戏", 0);
        drawButton(batch, btnTalent, "觉醒天赋", 1);
        drawButton(batch, btnBag, "神装背包", 2);
        drawButton(batch, btnTower, "无尽爬塔", 3);

        // 动态数据层
        SaveData d = save.data();
        game.assets.font.setColor(1f, 0.85f, 0.45f, 1f);
        game.assets.font.draw(batch, "回溯印记: " + d.echoMarks, 30f, 690f);
        game.assets.font.setColor(0.75f, 0.85f, 1f, 1f);
        game.assets.font.draw(batch, "最高层数: " + d.maxTowerFloor, 30f, 655f);
        game.assets.font.draw(batch, "历史最大暴击: " + (int) d.biggestCrit, 30f, 620f);

        game.assets.font.setColor(0.6f, 0.6f, 0.75f, 1f);
        game.assets.font.getData().setScale(0.85f);
        game.assets.font.draw(batch, "云麓盒子 · LibGDX", 0, 20f, 1280, Align.center, false);
        game.assets.font.getData().setScale(1f);

        batch.end();
    }

    private void drawButton(SpriteBatch batch, Rectangle r, String text, int id) {
        boolean hov = (id == hovered);
        batch.setColor(0.16f, 0.09f, 0.30f, hov ? 0.95f : 0.75f);
        Ui.rect(batch, r.x, r.y, r.width, r.height);
        batch.setColor(0.75f, 0.5f, 1f, hov ? 1f : 0.7f);
        Ui.rect(batch, r.x, r.y, r.width, 3f);
        Ui.rect(batch, r.x, r.y + r.height - 3f, r.width, 3f);
        game.assets.font.setColor(1f, 0.92f, 0.75f, 1f);
        game.assets.font.getData().setScale(1.6f);
        game.assets.font.draw(batch, text, r.x, r.y + r.height / 2f - 8f, r.width, Align.center, false);
        game.assets.font.getData().setScale(1f);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        if (btnStart.contains(x, y)) { startGame(); return true; }
        if (btnTalent.contains(x, y)) { game.setScreen(new TalentScreen(game)); return true; }
        if (btnBag.contains(x, y)) { game.setScreen(new BagScreen(game)); return true; }
        if (btnTower.contains(x, y)) { game.setScreen(new TowerScreen(game)); return true; }
        return false;
    }

    public boolean touchMoved(int screenX, int screenY) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        return true;
    }

    private void startGame() {
        game.setScreen(new GameScreen(game, "mode_rift"));
    }

    @Override
    public void resize(int width, int height) {
        vp.update(width, height, true);
        uiVp.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
