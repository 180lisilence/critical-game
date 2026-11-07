package com.yunluhezi.maxcrit.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.yunluhezi.maxcrit.config.TalentDef;
import com.yunluhezi.maxcrit.core.MaxCritGame;
import com.yunluhezi.maxcrit.util.Ui;
import com.yunluhezi.maxcrit.util.Colors;

import java.util.ArrayList;
import java.util.List;

/** 觉醒天赋树：瞬杀 / 永续 / 神佑 三系，消耗天赋点解锁被动。 */
public class TalentScreen extends BaseScreen {

    private final SpriteBatch batch;
    private final List<TalentDef> defs = new ArrayList<>();
    private final List<Rectangle> boxes = new ArrayList<>();
    private final Rectangle btnBack = new Rectangle(30, 30, 120, 56);
    private int hovered = -1;

    public TalentScreen(MaxCritGame game) {
        super(game);
        this.batch = new SpriteBatch();
        defs.addAll(game.assets.config.talents);
        layout();
        Gdx.input.setInputProcessor(this);
    }

    private void layout() {
        boxes.clear();
        // 三系分列
        String[] branches = {"kill", "endless", "bless"};
        for (int b = 0; b < branches.length; b++) {
            float cx = 240f + b * 400f;
            int tier = 0;
            for (TalentDef t : defs) {
                if (!t.branch.equals(branches[b])) continue;
                float y = 520f - tier * 170f;
                boxes.add(new Rectangle(cx - 110f, y - 60f, 220f, 120f));
                tier++;
            }
        }
    }

    @Override
    public void render(float delta) {
        batch.setProjectionMatrix(uiVp.getCamera().combined);
        batch.begin();

        // 背景
        com.badlogic.gdx.graphics.g2d.TextureRegion bg = game.assets.region("ui_screen_038");
        if (bg != null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(bg, 0, 0, 1280, 720);
        } else {
            batch.setColor(0.04f, 0.02f, 0.08f, 1f);
            Ui.rect(batch, 0, 0, 1280, 720);
        }

        game.assets.font.getData().setScale(2.4f);
        game.assets.font.setColor(0.95f, 0.85f, 1f, 1f);
        game.assets.font.draw(batch, "觉 醒 天 赋", 0, 660, 1280, Align.center, false);
        game.assets.font.getData().setScale(1f);

        // 剩余天赋点
        game.assets.font.setColor(1f, 0.85f, 0.45f, 1f);
        game.assets.font.draw(batch, "可用天赋点: " + game.saveManager.data().talentPoints, 40f, 700f);

        // 分支标题
        String[] bn = {"瞬 杀", "永 续", "神 佑"};
        for (int b = 0; b < 3; b++) {
            game.assets.font.setColor(0.8f, 0.6f, 1f, 1f);
            game.assets.font.draw(batch, bn[b], 0, 620f, 240f + b * 400f, Align.center, false);
        }

        int idx = 0;
        for (int b = 0; b < 3; b++) {
            for (TalentDef t : defs) {
                if (t.tier != b + 1) continue;
                if (!t.branch.equals(new String[]{"kill", "endless", "bless"}[b])) continue;
                Rectangle r = boxes.get(idx++);
                boolean owned = game.saveManager.data().talents.contains(t.id, false);
                boolean canBuy = game.saveManager.data().talentPoints >= t.cost
                        && parentOk(t) && !owned;

                batch.setColor(Colors.pick(owned,
                        Colors.of(0.2f, 0.35f, 0.18f, 0.92f),
                        Colors.pick(canBuy, Colors.of(0.18f, 0.11f, 0.32f, 0.92f),
                                Colors.of(0.10f, 0.08f, 0.14f, 0.85f))));
                Ui.rect(batch, r.x, r.y, r.width, r.height);
                batch.setColor(Colors.pick(canBuy, Colors.of(1f, 0.85f, 0.4f, 1f), Colors.of(0.5f, 0.4f, 0.7f, 0.8f)));
                Ui.rect(batch, r.x, r.y, r.width, 3f);
                Ui.rect(batch, r.x, r.y + r.height - 3f, r.width, 3f);
                Ui.rect(batch, r.x, r.y, 3f, r.height);
                Ui.rect(batch, r.x + r.width - 3f, r.y, 3f, r.height);

                game.assets.font.setColor(Colors.pick(owned, Colors.of(0.6f, 1f, 0.6f, 1f), Colors.of(1f, 0.92f, 0.75f, 1f)));
                game.assets.font.draw(batch, t.name, r.x, r.y + r.height - 34f, r.width, Align.center, false);
                game.assets.font.setColor(0.85f, 0.85f, 0.95f, 1f);
                game.assets.font.draw(batch, t.desc, r.x + 10f, r.y + 30f, r.width - 20f, Align.center, true);
                game.assets.font.setColor(0.7f, 0.6f, 1f, 1f);
                game.assets.font.draw(batch, (owned ? "已激活" : "天赋点 " + t.cost),
                        r.x, r.y + 8f, r.width, Align.center, false);
            }
        }

        // 返回
        batch.setColor(0.16f, 0.09f, 0.30f, 0.9f);
        Ui.rect(batch, btnBack.x, btnBack.y, btnBack.width, btnBack.height);
        game.assets.font.setColor(1f, 0.92f, 0.75f, 1f);
        game.assets.font.draw(batch, "返 回", btnBack.x, btnBack.y + 20f, btnBack.width, Align.center, false);

        batch.end();
    }

    private boolean parentOk(TalentDef t) {
        if (t.parent == null) return true;
        return game.saveManager.data().talents.contains(t.parent, false);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        if (btnBack.contains(x, y)) {
            game.saveManager.save();
            game.setScreen(new MenuScreen(game));
            return true;
        }
        int i = 0;
        for (int b = 0; b < 3; b++) {
            for (TalentDef t : defs) {
                if (t.tier != b + 1) continue;
                if (!t.branch.equals(new String[]{"kill", "endless", "bless"}[b])) continue;
                Rectangle r = boxes.get(i++);
                if (r.contains(x, y)) {
                    if (!game.saveManager.data().talents.contains(t.id, false)
                            && game.saveManager.data().talentPoints >= t.cost && parentOk(t)) {
                        game.saveManager.data().talentPoints -= t.cost;
                        game.saveManager.data().talents.add(t.id);
                        game.saveManager.save();
                    }
                    return true;
                }
            }
        }
        return true;
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
