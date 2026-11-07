package com.yunluhezi.maxcrit.ui;

import com.yunluhezi.maxcrit.util.Ui;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.yunluhezi.maxcrit.battle.BattleWorld;
import com.yunluhezi.maxcrit.config.SkillDef;

import java.util.List;

/**
 * 技能三选一弹窗：升级时暂停游戏，弹出候选技能供玩家选择。
 */
public class SkillChoiceDialog {

    public boolean visible = false;
    private final List<SkillDef> candidates = new java.util.ArrayList<>();
    private final Rectangle[] hitboxes = new Rectangle[3];
    private int hovered = -1;
    private final float[] alpha = new float[3];

    private static final float CARD_W = 280f;
    private static final float CARD_H = 380f;
    private static final float GAP = 30f;

    public void show(List<SkillDef> three) {
        candidates.clear();
        if (three != null) candidates.addAll(three);
        visible = candidates.size() > 0;
        for (int i = 0; i < 3; i++) alpha[i] = 0f;
    }

    public void hide() {
        visible = false;
    }

    /** 返回被选中的技能序号，未选择返回 -1。 */
    public int touchDown(float x, float y) {
        if (!visible) return -1;
        for (int i = 0; i < candidates.size(); i++) {
            if (hitboxes[i] != null && hitboxes[i].contains(x, y)) {
                hovered = i;
                return i;
            }
        }
        return -1;
    }

    public int touchUp(float x, float y) {
        if (!visible) return -1;
        int hit = -1;
        for (int i = 0; i < candidates.size(); i++) {
            if (hitboxes[i] != null && hitboxes[i].contains(x, y)) {
                hit = i;
                break;
            }
        }
        if (hit >= 0) {
            visible = false;
            return hit;
        }
        return -1;
    }

    public SkillDef selected() {
        return hovered >= 0 && hovered < candidates.size() ? candidates.get(hovered) : null;
    }

    public void update(float dt) {
        for (int i = 0; i < alpha.length; i++) {
            alpha[i] = Math.min(1f, alpha[i] + dt * 4f);
        }
    }

    public void render(SpriteBatch batch, BitmapFont font, com.yunluhezi.maxcrit.asset.GameAssets assets) {
        if (!visible) return;

        float w = 1280f, h = 720f;
        // 遮罩
        batch.setColor(0.02f, 0.01f, 0.06f, 0.82f);
        Ui.rect(batch, 0, 0, w, h);

        // 标题
        font.getData().setScale(2.2f);
        font.setColor(0.95f, 0.85f, 1f, 1f);
        font.draw(batch, "选择你的力量", w / 2f, h - 110f, w, Align.center, false);
        font.getData().setScale(1f);

        int n = candidates.size();
        float totalW = n * CARD_W + (n - 1) * GAP;
        float startX = (w - totalW) / 2f;
        float cardY = (h - CARD_H) / 2f - 20f;

        for (int i = 0; i < n; i++) {
            SkillDef d = candidates.get(i);
            float x = startX + i * (CARD_W + GAP);
            float a = alpha[i];
            boolean hov = (i == hovered);

            // 卡片底
            batch.setColor(0.12f, 0.07f, 0.22f, 0.95f * a);
            Ui.rect(batch, x, cardY, CARD_W, CARD_H);
            // 边框
            batch.setColor(0.7f, 0.45f, 1f, (hov ? 1f : 0.65f) * a);
            Ui.rect(batch, x, cardY, CARD_W, 5f);
            Ui.rect(batch, x, cardY + CARD_H - 5f, CARD_W, 5f);
            Ui.rect(batch, x, cardY, 5f, CARD_H);
            Ui.rect(batch, x + CARD_W - 5f, cardY, 5f, CARD_H);

            // 图标
            com.badlogic.gdx.graphics.g2d.TextureRegion r = assets.region(d.icon);
            if (r != null) {
                batch.setColor(1f, 1f, 1f, a);
                float is = 130f;
                float ix = x + (CARD_W - is) / 2f;
                float iy = cardY + CARD_H - is - 30f;
                batch.draw(r, ix, iy, is, is);
            }

            // 名称
            font.setColor(1f, 0.9f, 0.6f, a);
            font.getData().setScale(1.5f);
            font.draw(batch, d.name, x + 10f, cardY + CARD_H - 200f, CARD_W - 20f, Align.center, false);

            // 稀有度
            font.getData().setScale(1.0f);
            font.setColor(0.75f, 0.5f, 1f, a);
            font.draw(batch, "★".repeat(Math.max(1, d.rarity)), x + 10f, cardY + CARD_H - 230f,
                    CARD_W - 20f, Align.center, false);

            // 描述
            font.setColor(0.9f, 0.9f, 0.95f, a);
            font.draw(batch, d.desc, x + 18f, cardY + CARD_H - 280f, CARD_W - 36f, Align.center, true);

            // 流派
            font.setColor(0.6f, 0.85f, 1f, a);
            font.draw(batch, styleName(d.style), x + 10f, cardY + 40f, CARD_W - 20f, Align.center, false);

            hitboxes[i] = new Rectangle(x, cardY, CARD_W, CARD_H);
        }
        batch.setColor(Color.WHITE);
    }

    public static String styleName(String style) {
        if (style == null) return "";
        switch (style) {
            case "crit": return "暴击瞬杀";
            case "swift": return "极速连打";
            case "void": return "虚空召唤";
            case "bulwark": return "壁垒生存";
            case "rebirth": return "重生回溯";
            default: return "通用";
        }
    }
}
