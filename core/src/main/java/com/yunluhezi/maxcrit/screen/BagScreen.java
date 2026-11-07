package com.yunluhezi.maxcrit.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.yunluhezi.maxcrit.config.EquipDef;
import com.yunluhezi.maxcrit.core.MaxCritGame;
import com.yunluhezi.maxcrit.util.Ui;
import com.yunluhezi.maxcrit.save.MetaProgress;
import com.yunluhezi.maxcrit.util.Colors;

import java.util.ArrayList;
import java.util.List;

/**
 * 神装背包：6 部位穿戴 + 符文镶嵌 + 套装效果展示。
 */
public class BagScreen extends BaseScreen {

    private final SpriteBatch batch;
    private final java.util.List<EquipDef> table = new java.util.ArrayList<>();
    private final java.util.List<Rectangle> slotBoxes = new java.util.ArrayList<>();
    private final java.util.List<Rectangle> ownedBoxes = new java.util.ArrayList<>();
    private final Rectangle btnBack = new Rectangle(30, 30, 120, 56);
    private final Rectangle btnRune = new Rectangle(1130, 30, 120, 56);
    private String infoText = "";
    private int hovered = -1;

    public BagScreen(MaxCritGame game) {
        super(game);
        this.batch = new SpriteBatch();
        table.addAll(game.assets.config.equips);
        layout();
        Gdx.input.setInputProcessor(this);
    }

    private void layout() {
        slotBoxes.clear();
        ownedBoxes.clear();
        // 右侧 6 个装备槽
        for (int i = 0; i < 6; i++) {
            slotBoxes.add(new Rectangle(950f, 520f - i * 78f, 120f, 66f));
        }
        // 左侧已拥有装备列表
        int row = 0;
        for (EquipDef d : table) {
            if (row >= 8) break;
            ownedBoxes.add(new Rectangle(60f, 520f - row * 66f, 320f, 56f));
            row++;
        }
    }

    @Override
    public void render(float delta) {
        batch.setProjectionMatrix(uiVp.getCamera().combined);
        batch.begin();

        TextureRegion bg = game.assets.region("ui_screen_036");
        if (bg != null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(bg, 0, 0, 1280, 720);
        } else {
            batch.setColor(0.04f, 0.02f, 0.08f, 1f);
            Ui.rect(batch, 0, 0, 1280, 720);
        }

        game.assets.font.getData().setScale(2.4f);
        game.assets.font.setColor(0.95f, 0.85f, 1f, 1f);
        game.assets.font.draw(batch, "神 装 背 包", 0, 660, 1280, Align.center, false);
        game.assets.font.getData().setScale(1f);

        game.assets.font.setColor(1f, 0.85f, 0.45f, 1f);
        game.assets.font.draw(batch, "回溯印记: " + game.saveManager.data().echoMarks, 40f, 700f);

        // ---- 已拥有装备列表 ----
        game.assets.font.setColor(0.8f, 0.85f, 1f, 1f);
        game.assets.font.draw(batch, "已解锁神装", 60f, 600f);

        int i = 0;
        for (EquipDef d : table) {
            if (i >= ownedBoxes.size()) break;
            Rectangle r = ownedBoxes.get(i);
            boolean unlocked = game.saveManager.data().unlockedEquips.contains(d.id, false);
            boolean worn = isWorn(d.slot);
            batch.setColor(Colors.pick(unlocked,
                    Colors.pick(worn, Colors.of(0.2f, 0.3f, 0.16f, 0.92f), Colors.of(0.14f, 0.09f, 0.26f, 0.92f)),
                    Colors.of(0.08f, 0.07f, 0.11f, 0.8f)));
            Ui.rect(batch, r.x, r.y, r.width, r.height);
            batch.setColor(0.7f, 0.5f, 1f, 0.7f);
            Ui.rect(batch, r.x, r.y, r.width, 2f);

            game.assets.font.setColor(Colors.pick(unlocked, Colors.of(1f, 0.9f, 0.65f, 1f), Colors.of(0.4f, 0.4f, 0.45f, 1f)));
            game.assets.font.draw(batch, (unlocked ? "" : "[未解锁] ") + d.name + "  " + d.qualityName(),
                    r.x + 10f, r.y + 32f, r.width - 20f, Align.left, false);
            game.assets.font.setColor(Colors.pick(unlocked, Colors.of(0.8f, 0.85f, 0.95f, 1f), Colors.of(0.35f, 0.35f, 0.4f, 1f)));
            game.assets.font.draw(batch, d.desc, r.x + 10f, r.y + 12f, r.width - 20f, Align.left, false);
            i++;
        }

        // ---- 装备槽 ----
        game.assets.font.setColor(0.8f, 0.85f, 1f, 1f);
        game.assets.font.draw(batch, "装备部位", 950f, 600f);
        String[] slotNames = {"武器", "头冠", "披风", "护符", "战靴", "戒印"};
        for (int s = 0; s < 6; s++) {
            Rectangle r = slotBoxes.get(s);
            String eqId = game.saveManager.data().equipped.get(MetaProgress.SLOTS[s]);
            EquipDef eq = find(eqId);
            batch.setColor(Colors.pick(eq != null, Colors.of(0.18f, 0.11f, 0.32f, 0.85f), Colors.of(0.08f, 0.07f, 0.11f, 0.85f)));
            Ui.rect(batch, r.x, r.y, r.width, r.height);
            batch.setColor(Colors.pick(eq != null, Colors.of(1f, 0.8f, 0.4f, 1f), Colors.of(0.4f, 0.35f, 0.5f, 0.7f)));
            Ui.rect(batch, r.x, r.y, r.width, 2f);
            game.assets.font.setColor(0.9f, 0.9f, 1f, 1f);
            game.assets.font.draw(batch, slotNames[s], r.x, r.y + r.height - 12f, r.width, Align.center, false);
            game.assets.font.setColor(Colors.pick(eq != null, Colors.of(1f, 0.9f, 0.6f, 1f), Colors.of(0.4f, 0.4f, 0.45f, 1f)));
            game.assets.font.draw(batch, eq != null ? eq.name : "空", r.x, r.y + 18f, r.width, Align.center, false);
        }

        // ---- 套装效果 ----
        game.assets.font.setColor(0.75f, 0.6f, 1f, 1f);
        game.assets.font.draw(batch, "套装效果 (2/4/6 件激活)", 950f, 60f);
        int[] counts = setCounts();
        game.assets.font.setColor(0.85f, 0.9f, 1f, 1f);
        game.assets.font.draw(batch, "裂隙套: " + counts[0] + " 件  (+爆伤)", 950f, 35f);
        game.assets.font.draw(batch, "虚空套: " + counts[1] + " 件  (+移速)", 950f, 12f);

        // ---- 信息栏 ----
        if (!infoText.isEmpty()) {
            game.assets.font.setColor(1f, 0.9f, 0.6f, 1f);
            game.assets.font.draw(batch, infoText, 60f, 20f, 800f, Align.left, false);
        }

        // ---- 按钮 ----
        batch.setColor(0.16f, 0.09f, 0.30f, 0.9f);
        Ui.rect(batch, btnBack.x, btnBack.y, btnBack.width, btnBack.height);
        Ui.rect(batch, btnRune.x, btnRune.y, btnRune.width, btnRune.height);
        game.assets.font.setColor(1f, 0.92f, 0.75f, 1f);
        game.assets.font.draw(batch, "返 回", btnBack.x, btnBack.y + 20f, btnBack.width, Align.center, false);
        game.assets.font.draw(batch, "符 文", btnRune.x, btnRune.y + 20f, btnRune.width, Align.center, false);

        batch.end();
    }

    private int[] setCounts() {
        int[] c = new int[3];
        for (String slot : MetaProgress.SLOTS) {
            EquipDef d = find(game.saveManager.data().equipped.get(slot));
            if (d == null || d.setId == null) continue;
            if (d.setId.equals("set_crack")) c[0]++;
            else if (d.setId.equals("set_void")) c[1]++;
            else if (d.setId.equals("set_dawn")) c[2]++;
        }
        return c;
    }

    private boolean isWorn(String slot) {
        String id = game.saveManager.data().equipped.get(slot);
        return id != null;
    }

    private EquipDef find(String id) {
        if (id == null) return null;
        for (EquipDef d : table) if (d.id.equals(id)) return d;
        return null;
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
        if (btnRune.contains(x, y)) {
            infoText = "符文系统将在下一版本开放（当前为预留接口）";
            return true;
        }
        // 槽位 → 卸下
        for (int s = 0; s < slotBoxes.size(); s++) {
            if (slotBoxes.get(s).contains(x, y)) {
                String cur = game.saveManager.data().equipped.get(MetaProgress.SLOTS[s]);
                if (cur != null) {
                    game.saveManager.data().equipped.remove(MetaProgress.SLOTS[s]);
                    game.saveManager.save();
                    infoText = "已卸下 " + MetaProgress.SLOTS[s];
                }
                return true;
            }
        }
        // 列表 → 穿上
        int i = 0;
        for (EquipDef d : table) {
            if (i >= ownedBoxes.size()) break;
            if (ownedBoxes.get(i).contains(x, y)) {
                if (game.saveManager.data().unlockedEquips.contains(d.id, false)) {
                    game.saveManager.data().equipped.put(d.slot, d.id);
                    game.saveManager.save();
                    infoText = "已装备 " + d.name;
                } else {
                    infoText = "尚未解锁：" + d.name + "（通关关卡可获得）";
                }
                return true;
            }
            i++;
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
