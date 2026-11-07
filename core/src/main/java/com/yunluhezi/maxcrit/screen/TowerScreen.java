package com.yunluhezi.maxcrit.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.yunluhezi.maxcrit.battle.BattleWorld;
import com.yunluhezi.maxcrit.battle.FloatingText;
import com.yunluhezi.maxcrit.battle.entity.BattleMonster;
import com.yunluhezi.maxcrit.battle.entity.Bullet;
import com.yunluhezi.maxcrit.battle.entity.ExpOrb;
import com.yunluhezi.maxcrit.battle.entity.Summon;
import com.yunluhezi.maxcrit.core.MaxCritGame;
import com.yunluhezi.maxcrit.save.MetaProgress;
import com.yunluhezi.maxcrit.ui.Hud;
import com.yunluhezi.maxcrit.ui.SkillChoiceDialog;
import com.yunluhezi.maxcrit.ui.VirtualJoystick;
import com.yunluhezi.maxcrit.util.Colors;
import com.yunluhezi.maxcrit.util.Ui;

import java.util.List;

/**
 * 无尽暴击爬塔：无上限层数，记录最高层数与历史单次最大暴击。
 * 与 GameScreen 共享战斗逻辑，差别在于：每波结束即进入下一层（层数+1），
 * 难度按层数线性膨胀，无通关概念，只有死亡结束。
 */
public class TowerScreen extends BaseScreen {

    private final SpriteBatch batch;
    private final BattleWorld world;
    private final VirtualJoystick joystick;
    private final SkillChoiceDialog skillDialog = new SkillChoiceDialog();
    private final Hud hud = new Hud();

    private int floor = 1;
    private float camX, camY;
    private boolean showingResult = false;
    private final Rectangle btnBack = new Rectangle(570, 150, 140, 64);
    /** 本局最大单次暴击伤害。 */
    private float biggestHit = 0f;
    public TowerScreen(MaxCritGame game) {
        super(game);
        this.batch = new SpriteBatch();

        MetaProgress.Stats stats = MetaProgress.compute(
                game.saveManager.data(),
                game.assets.config.equips,
                game.assets.config.talents);

        world = new BattleWorld(game.assets.config, stats);
        world.start(new BattleWorld.StageConfig());
        world.onLevelUpHook = this::openSkillChoice;
        world.onGameOver = this::onGameOver;

        joystick = new VirtualJoystick(150f, 180f);
        Gdx.input.setInputProcessor(this);
    }

    private void openSkillChoice() {
        List<com.yunluhezi.maxcrit.config.SkillDef> three = world.skills.rollThree(world.player.level);
        if (three.isEmpty()) return;
        world.paused = true;
        skillDialog.show(three);
    }

    private void onGameOver() {
        showingResult = true;
        // 记录最高层数与历史最大暴击
        com.yunluhezi.maxcrit.save.SaveManager.SaveData d = game.saveManager.data();
        if (floor > d.maxTowerFloor) d.maxTowerFloor = floor;
        if (biggestHit > d.biggestCrit) d.biggestCrit = biggestHit;
        d.echoMarks += 5 + floor / 2;
        game.saveManager.save();
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 20f);
        if (!showingResult) {
            world.update(dt);
            // 每波结束 → 下一层，难度线性膨胀
            if (world.wave != null && world.wave.finished && world.monsters.size == 0) {
                floor++;
                world.wave = new com.yunluhezi.maxcrit.battle.wave.WaveManager(
                        world.config, 6, 1f + floor * 0.22f);
                world.wave.currentWave = -1;
            }
        }
        skillDialog.update(dt);

        camX += (world.player.pos.x - camX) * Math.min(1f, dt * 8f);
        camY += (world.player.pos.y - camY) * Math.min(1f, dt * 8f);
        vp.getCamera().position.set(camX, camY, 0);
        vp.getCamera().update();

        batch.setProjectionMatrix(vp.getCamera().combined);
        batch.begin();
        drawWorld();
        // 【修复】HUD/摇杆/弹窗必须用固定屏幕投影，
        // 原来与 drawWorld 共用跟随相机的世界投影，导致 UI 随玩家飘出屏幕。
        batch.setProjectionMatrix(uiVp.getCamera().combined);
        batch.begin();
        if (!showingResult) {
            hud.render(batch, game.assets.font, world);
            joystick.render(batch, game.assets.font);
        }
        skillDialog.render(batch, game.assets.font, game.assets);
        if (showingResult) drawResult();
        batch.end();
    }


    private void drawWorld() {
        Ui.rect(batch, camX - 700, camY - 400, 1400, 800);
        TextureRegion scene = game.assets.region("scene_obelisk");
        if (scene != null) {
            batch.setColor(0.6f, 0.45f, 0.85f, 0.2f);
            batch.draw(scene, camX - 700, camY - 400, 1400, 800);
        }
        // 网格
        batch.setColor(0.4f, 0.25f, 0.7f, 0.1f);
        float step = 100f;
        float gx = (float) (Math.floor((camX - 700) / step) * step);
        for (float x = gx; x < camX + 700; x += step) {
            Ui.rect(batch, x, camY - 400, 1.5f, 800);
        }
        float gy = (float) (Math.floor((camY - 400) / step) * step);
        for (float y = gy; y < camY + 400; y += step) {
            Ui.rect(batch, camX - 700, y, 1400, 1.5f);
        }

        for (ExpOrb o : world.expOrbs) {
            batch.setColor(0.45f, 0.9f, 1f, 0.95f);
            Ui.circle(batch, o.pos.x, o.pos.y, o.radius);
        }
        for (Summon s : world.summons) {
            batch.setColor(0.55f, 0.35f, 0.95f, 0.85f);
            Ui.circle(batch, s.pos.x, s.pos.y, s.radius);
        }
        for (BattleMonster m : world.monsters) {
            TextureRegion r = game.assets.region(m.def.texture);
            if (r != null) {
                float size = m.radius * 2.6f;
                batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(r, m.pos.x - size / 2f, m.pos.y - size / 2f, size, size);
            }
            float bw = m.radius * 2f;
            Ui.rect(batch, m.pos.x - bw / 2f, m.pos.y + m.radius + 6f, bw, 5f);
            float hr = m.maxHp > 0 ? Math.max(0, m.hp / m.maxHp) : 0;
            batch.setColor(0.85f, 0.25f, 0.3f, 0.95f);
            Ui.rect(batch, m.pos.x - bw / 2f, m.pos.y + m.radius + 6f, bw * hr, 5f);
        }
        for (Bullet b : world.bullets) {
            if (b.overCrit) batch.setColor(0.85f, 0.5f, 1f, 1f);
            else if (b.wasCrit) batch.setColor(1f, 0.85f, 0.3f, 1f);
            else batch.setColor(0.85f, 0.9f, 1f, 0.9f);
            Ui.circle(batch, b.pos.x, b.pos.y, b.radius);
        }
        TextureRegion hero = game.assets.region("hero_necro");
        if (hero != null) {
            float size = world.player.radius * 2.8f;
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(hero, world.player.pos.x - size / 2f, world.player.pos.y - size / 2f, size, size);
        }
        for (FloatingText t : world.texts) {
            float a = Math.min(1f, t.life / t.maxLife * 1.6f);
            game.assets.font.getData().setScale(t.scale);
            game.assets.font.setColor(t.color.r, t.color.g, t.color.b, a);
            game.assets.font.draw(batch, t.text, t.x - 60f, t.y, 120f, Align.center, false);
        }
        game.assets.font.getData().setScale(1f);

        // 层数
        game.assets.font.getData().setScale(2.4f);
        game.assets.font.setColor(1f, 0.85f, 0.4f, 1f);
        game.assets.font.draw(batch, "第 " + floor + " 层", 0, 690f, 1280, Align.center, false);
        game.assets.font.getData().setScale(1f);
        game.assets.font.setColor(0.9f, 0.5f, 0.55f, 1f);
        game.assets.font.draw(batch, "最高记录: " + game.saveManager.data().maxTowerFloor, 20f, 660f);
    }

    private void drawResult() {
        batch.setColor(0.02f, 0.01f, 0.05f, 0.88f);
        Ui.rect(batch, 0, 0, 1280, 720);

        game.assets.font.getData().setScale(3.2f);
        game.assets.font.setColor(0.95f, 0.45f, 0.5f, 1f);
        game.assets.font.draw(batch, "爬 塔 结 束", 0, 500, 1280, Align.center, false);
        game.assets.font.getData().setScale(1.4f);

        String[] rows = {
                "到达层数: " + floor,
                "历史最高: " + game.saveManager.data().maxTowerFloor,
                "存活时间: " + (int) world.elapsed + " 秒",
                "累计伤害: " + world.totalDamage,
                "暴击次数: " + world.totalCrits,
                "满级暴击: " + world.overCritCount,
        };
        float y = 400f;
        for (String r : rows) {
            game.assets.font.setColor(0.9f, 0.9f, 1f, 1f);
            game.assets.font.draw(batch, r, 0, y, 1280, Align.center, false);
            y -= 38f;
        }
        game.assets.font.getData().setScale(1f);

        Ui.rect(batch, btnBack.x, btnBack.y, btnBack.width, btnBack.height);
        game.assets.font.setColor(1f, 0.92f, 0.75f, 1f);
        game.assets.font.draw(batch, "返 回", btnBack.x, btnBack.y + 24f, btnBack.width, Align.center, false);
    }

    /** 每帧由 Game.update 调用。 */
    public void syncInput() {
        world.player.moveInput.set(joystick.dir);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        if (skillDialog.visible) {
            skillDialog.touchDown(x, y);
            return true;
        }
        if (showingResult) {
            if (btnBack.contains(x, y)) game.setScreen(new MenuScreen(game));
            return true;
        }
        joystick.touchDown(pointer, x, y);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (skillDialog.visible) return true;
        joystick.touchDrag(pointer, Ui.toWorldX(uiVp, screenX), Ui.toWorldY(uiVp, screenY));
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        if (skillDialog.visible) {
            int idx = skillDialog.touchUp(x, y);
            if (idx >= 0) {
                com.yunluhezi.maxcrit.config.SkillDef d = skillDialog.selected();
                if (d != null) world.applySkill(d);
                world.paused = false;
            }
            return true;
        }
        if (showingResult) return true;
        joystick.touchUp(pointer);
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
