package com.yunluhezi.maxcrit.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.yunluhezi.maxcrit.battle.BattleWorld;
import com.yunluhezi.maxcrit.battle.FloatingText;
import com.yunluhezi.maxcrit.battle.entity.BattleMonster;
import com.yunluhezi.maxcrit.battle.entity.Bullet;
import com.yunluhezi.maxcrit.battle.entity.ExpOrb;
import com.yunluhezi.maxcrit.battle.entity.Summon;
import com.yunluhezi.maxcrit.config.SkillDef;
import com.yunluhezi.maxcrit.core.MaxCritGame;
import com.yunluhezi.maxcrit.util.Ui;
import com.yunluhezi.maxcrit.save.MetaProgress;
import com.yunluhezi.maxcrit.ui.Hud;
import com.yunluhezi.maxcrit.ui.SkillChoiceDialog;
import com.yunluhezi.maxcrit.ui.VirtualJoystick;
import com.yunluhezi.maxcrit.util.Colors;

import java.util.List;

/**
 * 战斗屏幕：摇杆走位 + 自动锁敌攻击 + 经验升级三选一 + 结算。
 */
public class GameScreen extends BaseScreen {

    private final SpriteBatch batch;
    private final FitViewport vp;
    private final BattleWorld world;
    private final VirtualJoystick joystick;
    private final SkillChoiceDialog skillDialog = new SkillChoiceDialog();
    private final Hud hud = new Hud();

    private final String mode;
    private boolean showingResult = false;
    private boolean resultWin = false;
    private final Rectangle btnBack = new Rectangle(570, 150, 140, 64);

    /** 摄像机跟随玩家。 */
    private float camX, camY;

    public GameScreen(MaxCritGame game, String mode) {
        super(game);
        this.mode = mode;
        this.batch = new SpriteBatch();
        this.vp = new FitViewport(1280, 720);

        // 计算入局属性
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
        List<SkillDef> three = world.skills.rollThree(world.player.level);
        if (three.isEmpty()) return;
        world.paused = true;
        skillDialog.show(three);
    }

    private void onGameOver() {
        showingResult = true;
        resultWin = world.victory;
        // 结算：发放回溯印记
        int gain = world.victory ? 20 + world.player.level * 3 : 8 + world.player.level;
        gain += world.echoEarned;
        game.saveManager.data().echoMarks += gain;
        if (world.totalDamage > game.saveManager.data().biggestDamage) {
            game.saveManager.data().biggestDamage = world.totalDamage;
        }
        game.saveManager.data().biggestCrit = Math.max(game.saveManager.data().biggestCrit,
                (float) world.player.stats.attack);
        game.saveManager.save();
    }

    public void onPauseGame() {
        // 退到后台时自动保存
        game.saveManager.save();
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 20f); // 防止切后台后大跳
        if (!showingResult) {
            world.update(dt);
        }
        skillDialog.update(dt);

        // 摄像机跟随
        camX += (world.player.pos.x - camX) * Math.min(1f, dt * 8f);
        camY += (world.player.pos.y - camY) * Math.min(1f, dt * 8f);
        vp.getCamera().position.set(camX, camY, 0);
        vp.getCamera().update();

        batch.setProjectionMatrix(vp.getCamera().combined);
        batch.begin();
        drawWorld();
        batch.end();

        // HUD 用独立投影（固定屏幕坐标）
        // 【修复】原来这里仍用 vp.getCamera().combined（跟随相机的世界投影），
        // 导致血条/经验条/摇杆随玩家一起移动并飘出屏幕。必须切回 UI 视口投影。
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
        // 背景（星空）
        batch.setColor(0.04f, 0.02f, 0.09f, 1f);
        Ui.rect(batch, camX - 700, camY - 400, 1400, 800);

        // 场景装饰（用成品场景图平铺）
        TextureRegion scene = game.assets.region("scene_nebula");
        if (scene != null) {
            batch.setColor(0.55f, 0.45f, 0.8f, 0.22f);
            batch.draw(scene, camX - 700, camY - 400, 1400, 800);
        }

        // 地面网格（帮助感知移动）
        batch.setColor(0.35f, 0.2f, 0.6f, 0.12f);
        float gridStep = 100f;
        float gx = (float) (Math.floor((camX - 700) / gridStep) * gridStep);
        for (float x = gx; x < camX + 700; x += gridStep) {
            Ui.rect(batch, x, camY - 400, 1.5f, 800);
        }
        float gy = (float) (Math.floor((camY - 400) / gridStep) * gridStep);
        for (float y = gy; y < camY + 400; y += gridStep) {
            Ui.rect(batch, camX - 700, y, 1400, 1.5f);
        }

        // 经验球
        for (ExpOrb o : world.expOrbs) {
            batch.setColor(0.45f, 0.9f, 1f, 0.95f);
            Ui.circle(batch, o.pos.x, o.pos.y, o.radius);
            batch.setColor(1f, 1f, 1f, 0.9f);
            Ui.circle(batch, o.pos.x, o.pos.y, o.radius * 0.4f);
        }

        // 召唤物
        for (Summon s : world.summons) {
            batch.setColor(0.55f, 0.35f, 0.95f, 0.85f);
            Ui.circle(batch, s.pos.x, s.pos.y, s.radius);
            batch.setColor(0.85f, 0.7f, 1f, 0.9f);
            Ui.circle(batch, s.pos.x, s.pos.y, s.radius * 0.45f);
        }

        // 怪物
        for (BattleMonster m : world.monsters) {
            TextureRegion r = game.assets.region(m.def.texture);
            if (r != null) {
                float size = m.radius * 2.6f;
                if (m.flashTime > 0f) batch.setColor(1f, 0.75f, 0.75f, 1f);
                else batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(r, m.pos.x - size / 2f, m.pos.y - size / 2f, size, size);
            } else {
                batch.setColor(0.7f, 0.3f, 0.4f, 1f);
                Ui.circle(batch, m.pos.x, m.pos.y, m.radius);
            }
            // 血条
            float bw = m.radius * 2f;
            batch.setColor(0.1f, 0f, 0.05f, 0.85f);
            Ui.rect(batch, m.pos.x - bw / 2f, m.pos.y + m.radius + 6f, bw, 5f);
            float hr = m.maxHp > 0 ? Math.max(0, m.hp / m.maxHp) : 0;
            batch.setColor(Colors.pick(m.boss, Colors.of(1f, 0.5f, 0.2f, 0.95f), Colors.of(0.85f, 0.25f, 0.3f, 0.95f)));
            Ui.rect(batch, m.pos.x - bw / 2f, m.pos.y + m.radius + 6f, bw * hr, 5f);
        }

        // 子弹
        for (Bullet b : world.bullets) {
            if (b.overCrit) batch.setColor(0.85f, 0.5f, 1f, 1f);
            else if (b.wasCrit) batch.setColor(1f, 0.85f, 0.3f, 1f);
            else batch.setColor(0.85f, 0.9f, 1f, 0.9f);
            Ui.circle(batch, b.pos.x, b.pos.y, b.radius);
        }

        // 玩家
        TextureRegion hero = game.assets.region("hero_necro");
        if (hero != null) {
            float size = world.player.radius * 2.8f;
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(hero, world.player.pos.x - size / 2f, world.player.pos.y - size / 2f, size, size);
        } else {
            batch.setColor(0.9f, 0.8f, 1f, 1f);
            Ui.circle(batch, world.player.pos.x, world.player.pos.y, world.player.radius);
        }
        // 无敌闪烁
        if (world.player.invincibleTime > 0f) {
            batch.setColor(1f, 1f, 1f, 0.35f);
            Ui.circle(batch, world.player.pos.x, world.player.pos.y, world.player.radius * 1.6f);
        }

        // 伤害跳字
        for (FloatingText t : world.texts) {
            float a = Math.min(1f, t.life / t.maxLife * 1.6f);
            game.assets.font.getData().setScale(t.scale);
            game.assets.font.setColor(t.color.r, t.color.g, t.color.b, a);
            game.assets.font.draw(batch, t.text, t.x - 60f, t.y, 120f, Align.center, false);
        }
        game.assets.font.getData().setScale(1f);
        batch.setColor(Color.WHITE);
    }

    private void drawResult() {
        batch.setColor(0.02f, 0.01f, 0.05f, 0.85f);
        Ui.rect(batch, 0, 0, 1280, 720);

        game.assets.font.getData().setScale(3.2f);
        game.assets.font.setColor(Colors.pick(resultWin, Colors.of(1f, 0.9f, 0.5f, 1f), Colors.of(0.9f, 0.4f, 0.4f, 1f)));
        game.assets.font.draw(batch, resultWin ? "通 关 胜 利" : "重 生 归 零",
                0, 500, 1280, Align.center, false);
        game.assets.font.getData().setScale(1.4f);

        String[] rows = {
                "存活时间: " + (int) world.elapsed + " 秒",
                "等级: " + world.player.level,
                "累计伤害: " + world.totalDamage,
                "暴击次数: " + world.totalCrits,
                "满级暴击: " + world.overCritCount,
                "获得回溯印记: " + (resultWin ? 20 + world.player.level * 3 : 8 + world.player.level) + world.echoEarned,
        };
        float y = 400f;
        for (String r : rows) {
            game.assets.font.setColor(0.9f, 0.9f, 1f, 1f);
            game.assets.font.draw(batch, r, 0, y, 1280, Align.center, false);
            y -= 38f;
        }
        game.assets.font.getData().setScale(1f);

        // 返回按钮
        batch.setColor(0.16f, 0.09f, 0.30f, 0.9f);
        Ui.rect(batch, btnBack.x, btnBack.y, btnBack.width, btnBack.height);
        game.assets.font.setColor(1f, 0.92f, 0.75f, 1f);
        game.assets.font.draw(batch, "返 回", btnBack.x, btnBack.y + 24f, btnBack.width, Align.center, false);
    }

    // ================= 输入 =================

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        if (skillDialog.visible) {
            skillDialog.touchDown(x, y);
            return true;
        }
        if (showingResult) {
            if (btnBack.contains(x, y)) {
                game.setScreen(new MenuScreen(game));
            }
            return true;
        }
        joystick.touchDown(pointer, x, y);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        if (skillDialog.visible) return true;
        joystick.touchDrag(pointer, x, y);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float x = Ui.toWorldX(uiVp, screenX);
        float y = Ui.toWorldY(uiVp, screenY);
        if (skillDialog.visible) {
            int idx = skillDialog.touchUp(x, y);
            if (idx >= 0) {
                SkillDef d = skillDialog.selected();
                if (d != null) {
                    world.applySkill(d);
                }
                world.paused = false;
            }
            return true;
        }
        if (showingResult) return true;
        joystick.touchUp(pointer);
        return true;
    }

    /** 每帧由 Game.update 调用，把摇杆输入同步到玩家。 */
    public void syncInput() {
        // 摇杆 → 玩家输入
        world.player.moveInput.set(joystick.dir);
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
