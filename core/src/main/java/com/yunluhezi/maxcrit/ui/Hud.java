package com.yunluhezi.maxcrit.ui;

import com.yunluhezi.maxcrit.util.Ui;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.yunluhezi.maxcrit.battle.BattleWorld;

/** 战斗 HUD：血条、经验条、等级、属性面板、波次、计时、暴击统计。 */
public class Hud {

    public void render(SpriteBatch batch, BitmapFont font, BattleWorld w) {
        float sw = 1280f, sh = 720f;

        // ===== 顶部：关卡/波次/计时 =====
        font.setColor(0.85f, 0.8f, 1f, 1f);
        font.draw(batch, w.wave != null ? w.wave.progressText() : "", 20f, sh - 26f);
        String time = String.format("%02d:%02d", (int) (w.elapsed / 60f), (int) (w.elapsed % 60f));
        font.draw(batch, time, sw - 120f, sh - 26f);
        if (w.wave != null && w.wave.berserk) {
            font.setColor(1f, 0.35f, 0.3f, 1f);
            font.draw(batch, "狂 暴", sw - 120f, sh - 48f);
        }

        // ===== 左上：血条 =====
        float hpW = 320f, hpH = 26f;
        float hpX = 20f, hpY = sh - 78f;
        batch.setColor(0.12f, 0.03f, 0.06f, 0.9f);
        Ui.rect(batch, hpX, hpY, hpW, hpH);
        float ratio = w.player.maxHp > 0 ? w.player.curHp / w.player.maxHp : 0;
        ratio = Math.max(0, Math.min(1, ratio));
        batch.setColor(0.85f, 0.15f, 0.25f, 0.95f);
        Ui.rect(batch, hpX, hpY, hpW * ratio, hpH);
        batch.setColor(0.9f, 0.4f, 0.5f, 0.8f);
        Ui.rect(batch, hpX, hpY, hpW * ratio, 4f);
        font.setColor(1f, 0.85f, 0.9f, 1f);
        font.draw(batch, (int) w.player.curHp + " / " + (int) w.player.maxHp, hpX + 6f, hpY + 8f);

        // 护盾条
        if (w.player.shield > 0) {
            batch.setColor(0.3f, 0.7f, 1f, 0.85f);
            Ui.rect(batch, hpX, hpY + hpH + 2f, hpW * 0.3f, 6f);
        }

        // ===== 底部：经验条 =====
        float exW = sw - 40f, exH = 14f, exY = 12f;
        batch.setColor(0.08f, 0.05f, 0.15f, 0.9f);
        Ui.rect(batch, 20f, exY, exW, exH);
        float er = w.player.expToNext > 0 ? w.player.exp / w.player.expToNext : 0;
        er = Math.max(0, Math.min(1, er));
        batch.setColor(0.45f, 0.85f, 1f, 0.95f);
        Ui.rect(batch, 20f, exY, exW * er, exH);

        // ===== 属性面板 =====
        float px = 20f, py = exY + 26f;
        font.setColor(0.8f, 0.85f, 1f, 0.95f);
        StringBuilder sb = new StringBuilder();
        sb.append("Lv.").append(w.player.level).append("   ");
        sb.append("暴击 ").append((int) (w.player.stats.critRate + w.extraCritRate)).append("%  ");
        sb.append("爆伤 ").append((int) (w.player.stats.critDamage + w.extraCritDamage)).append("%");
        font.draw(batch, sb.toString(), px, py + 18f);

        font.setColor(1f, 0.85f, 0.45f, 0.95f);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("暴击数 ").append(w.totalCrits);
        sb2.append("   满级暴击 ").append(w.overCritCount);
        sb2.append("   印记 ").append(w.echoEarned);
        if (w.crystals > 0) sb2.append("   突破晶 ").append(w.crystals);
        font.draw(batch, sb2.toString(), px, py);

        // ===== 右侧：满级暴击进度 =====
        float bx = sw - 260f, by = sh - 190f;
        font.setColor(0.75f, 0.6f, 1f, 1f);
        font.draw(batch, "满级暴击", bx, by + 66f);
        float cr = Math.min(1f, (w.player.stats.critRate + w.extraCritRate) / 85f);
        float cd = Math.min(1f, (w.player.stats.critDamage + w.extraCritDamage) / 150f);
        drawBar(batch, bx, by + 40f, 230f, 12f, cr, 0.9f, 0.3f, 0.9f);
        drawBar(batch, bx, by + 16f, 230f, 12f, cd, 1f, 0.75f, 0.25f);
        font.setColor(1f, 0.95f, 0.7f, 1f);
        font.draw(batch, "暴击率 " + (int) (w.player.stats.critRate + w.extraCritRate) + "/85", bx, by + 82f);
        font.draw(batch, "爆伤 " + (int) (w.player.stats.critDamage + w.extraCritDamage) + "/150", bx, by + 58f);

        batch.setColor(Color.WHITE);
    }

    private void drawBar(SpriteBatch batch, float x, float y, float w, float h,
                         float r, float cr, float cg, float cb) {
        batch.setColor(0.1f, 0.08f, 0.16f, 0.85f);
        Ui.rect(batch, x, y, w, h);
        batch.setColor(cr, cg, cb, 0.95f);
        Ui.rect(batch, x, y, w * Math.max(0, Math.min(1, r)), h);
    }
}
