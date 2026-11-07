package com.yunluhezi.maxcrit.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.yunluhezi.maxcrit.battle.damage.DamageCalculator;
import com.yunluhezi.maxcrit.battle.entity.BattleMonster;
import com.yunluhezi.maxcrit.battle.entity.BattlePlayer;
import com.yunluhezi.maxcrit.battle.entity.Bullet;
import com.yunluhezi.maxcrit.battle.entity.ExpOrb;
import com.yunluhezi.maxcrit.battle.entity.Summon;
import com.yunluhezi.maxcrit.battle.pool.Pools;
import com.yunluhezi.maxcrit.battle.skill.SkillSystem;
import com.yunluhezi.maxcrit.battle.wave.WaveManager;
import com.yunluhezi.maxcrit.config.ConfigHub;
import com.yunluhezi.maxcrit.config.MonsterDef;
import com.yunluhezi.maxcrit.config.SkillDef;
import com.yunluhezi.maxcrit.save.MetaProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗世界：一局游戏实例的全部运行时状态与逻辑。
 * 屏幕、渲染与 UI 都不在这里，纯粹是「世界模拟」。
 */
public class BattleWorld {

    // 实体
    public BattlePlayer player;
    public final Array<BattleMonster> monsters = new Array<>();
    public final Array<Bullet> bullets = new Array<>();
    public final Array<ExpOrb> expOrbs = new Array<>();
    public final Array<Summon> summons = new Array<>();
    public final Array<FloatingText> texts = new Array<>();

    // 系统
    public ConfigHub config;
    public WaveManager wave;
    public SkillSystem skills;
    public final Pools pools = new Pools();

    // 局内状态
    public boolean running = true;
    public boolean paused = false;
    public boolean gameOver = false;
    public boolean victory = false;
    public float elapsed;
    public long totalDamage;
    public long totalCrits;
    public long overCritCount;
    /** 累计获得的回溯印记（本局）。 */
    public int echoEarned;
    /** 暴击突破晶数量。 */
    public int crystals;
    /** 主武器攻击距离。 */
    public float weaponRange = 520f;
    /** 玩家死亡回调。 */
    public Runnable onGameOver;

    // 派生属性（由技能实时修正）
    public float styleMul = 1f;
    public float setBonusMul = 1f;
    public float runeMul = 1f;
    public float echoMul = 1f;
    public float buffMul = 1f;
    public float extraCritRate = 0f;
    public float extraCritDamage = 0f;
    public float extraMaxHp;
    public int summonCap = 0;
    public float summonDamageMul = 1f;
    public boolean shockwaveBonus;
    public float shockwaveExtra = 0f;
    public float damageReductionExtra = 0f;
    public float pickupRadiusMul = 1f;
    public float expGainMul = 1f;
    public float cdReduction = 0f;
    public int extraRevives = 0;
    public float echoTimer = 0f;
    public int echoPerMin = 0;
    public boolean immortality;
    public float immortalTimer = 0f;
    public float moveSpeedMul = 1f;
    public float attackSpeedMul = 1f;
    public float lifeSteal = 0f;

    public BattleWorld(ConfigHub config, MetaProgress.Stats base) {
        this.config = config;
        player = new BattlePlayer();
        player.applyStats(base);
        player.world = this;
        player.pos.set(0, 0);
        player.curHp = player.maxHp;
        // 基础属性快照：技能加成一律从这些值重算，保证多次叠加不重复累乘
        baseAttackSpeed = player.stats.attackSpeed;
        baseMoveSpeed = player.stats.moveSpeed;
        baseDamageReduction = player.stats.damageReduction;
        baseMaxHp = player.maxHp;
    }

    public void start(StageConfig stage) {
        wave = new WaveManager(config, stage.waveCount, stage.monsterHpScale);
        skills = new SkillSystem(config);
    }

    /** 关卡参数（避免 core 依赖 StageDef 之外的额外结构）。 */
    public static class StageConfig {
        public int waveCount = 8;
        public float monsterHpScale = 1f;
    }

    // ================= 主循环 =================

    public void update(float dt) {
        if (!running || paused) return;
        elapsed += dt;

        // 玩家
        player.update(dt);
        if (player.dead) {
            endGame(false);
            return;
        }

        // 技能相关计时
        updateTimers(dt);

        // 波次
        wave.update(dt, monsters.size);
        if (wave.shouldSpawn()) {
            wave.markSpawned();
            spawnMonster();
        }
        if (wave.finished && monsters.size == 0) {
            endGame(true);
            return;
        }

        // 实体更新
        for (BattleMonster m : monsters) {
            m.update(dt, player.pos.x, player.pos.y);
        }
        for (Bullet b : bullets) {
            b.update(dt);
        }
        for (ExpOrb o : expOrbs) {
            o.update(dt);
        }
        for (Summon s : summons) {
            s.update(dt, player.pos.x, player.pos.y);
        }
        for (FloatingText t : texts) {
            t.update(dt);
        }

        // 碰撞
        collideBullets(dt);
        collideMonsters(dt);
        collideExpOrbs(dt);
        updateSummons(dt);

        // 清理
        cleanup();
    }

    private void updateTimers(float dt) {
        if (echoPerMin > 0) {
            echoTimer += dt;
            if (echoTimer >= 60f) {
                echoTimer -= 60f;
                echoEarned += echoPerMin;
            }
        }
        if (immortality) {
            immortalTimer -= dt;
            if (immortalTimer <= 0f) {
                immortality = false;
                player.invincibleTime = 1.5f;
            }
        }
    }

    // ================= 刷怪 =================

    private void spawnMonster() {
        WaveManager.Wave w = wave.current();
        if (w == null) return;
        MonsterDef def = config.monster(w.monsterId);
        if (def == null) def = config.monsters.get(0);

        BattleMonster m = pools.obtainMonster();
        float angle = (float) (Math.random() * Math.PI * 2);
        float dist = 620f + (float) (Math.random() * 160f);
        m.setup(def, w.hpScale * wave.timeScale(), w.speedScale);
        m.pos.set(player.pos.x + (float) Math.cos(angle) * dist,
                player.pos.y + (float) Math.sin(angle) * dist);
        m.wanderPhase = (float) (Math.random() * Math.PI * 2);
        m.alive = true;
        m.dormant = false;
        monsters.add(m);
    }

    // ================= 玩家开火 =================

    public void playerFire() {
        BattleMonster target = findNearestMonster(weaponRange);
        if (target == null) return;

        float baseAttack = player.stats.attack;
        DamageCalculator.Result r = DamageCalculator.compute(
                baseAttack,
                player.stats.critRate + extraCritRate,
                player.stats.critDamage + extraCritDamage,
                styleMul, setBonusMul, runeMul, echoMul, buffMul,
                target.def.critResist, true);

        int count = Math.max(1, player.stats.bulletCount);
        float spread = count > 1 ? 0.16f : 0f;

        for (int i = 0; i < count; i++) {
            Bullet b = pools.obtainBullet();
            float ang = (float) Math.atan2(target.pos.y - player.pos.y, target.pos.x - player.pos.x);
            ang += (i - (count - 1) / 2f) * spread;
            b.fire(player.pos.x, player.pos.y,
                    player.pos.x + (float) Math.cos(ang) * 100f,
                    player.pos.y + (float) Math.sin(ang) * 100f,
                    r.damage, 640f);
            b.wasCrit = r.crit;
            b.overCrit = r.overCrit;
            bullets.add(b);
        }

        // 暴击回溯
        if (r.crit) {
            totalCrits++;
            if (r.healBack > 0) player.healBack(r.healBack);
            if (r.cdRefund > 0) skillCdRefund += r.cdRefund * (1f - cdReduction / 100f);
        }
        if (r.overCrit) {
            overCritCount++;
            doShockwave(target.pos.x, target.pos.y, baseAttack);
        }

        totalDamage += (long) r.damage;
    }

    /** 满级暴击的范围冲击波。 */
    private void doShockwave(float x, float y, float baseAttack) {
        float dmg = DamageCalculator.overCritShockwave(baseAttack, player.stats.critDamage + extraCritDamage);
        dmg *= 1f + shockwaveExtra / 100f;
        float radius = 150f;
        for (BattleMonster m : monsters) {
            if (!m.alive) continue;
            float dx = m.pos.x - x, dy = m.pos.y - y;
            if (dx * dx + dy * dy <= radius * radius) {
                m.takeDamage(dmg);
                if (!m.alive) onMonsterDeath(m);
            }
        }
        texts.add(new FloatingText(x, y + 30f, "满级暴击!", new Color(0.75f, 0.45f, 1f, 1f), 1.6f));
    }

    /** 主动技能 CD 减免累计（当前版本以"充能"呈现）。 */
    public float skillCdRefund = 0f;

    private BattleMonster findNearestMonster(float range) {
        BattleMonster best = null;
        float bestD = range * range;
        for (BattleMonster m : monsters) {
            if (!m.alive || m.dormant) continue;
            float dx = m.pos.x - player.pos.x, dy = m.pos.y - player.pos.y;
            float d = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = m; }
        }
        return best;
    }

    // ================= 碰撞 =================

    private void collideBullets(float dt) {
        for (Bullet b : bullets) {
            if (!b.alive) continue;
            for (BattleMonster m : monsters) {
                if (!m.alive || m.dormant) continue;
                float dx = m.pos.x - b.pos.x, dy = m.pos.y - b.pos.y;
                float rr = m.radius + b.radius;
                if (dx * dx + dy * dy <= rr * rr) {
                    m.takeDamage(b.damage);
                    if (!m.alive) onMonsterDeath(m);
                    b.alive = false;
                    break;
                }
            }
        }
    }

    private void collideMonsters(float dt) {
        for (BattleMonster m : monsters) {
            if (!m.alive || m.dormant) continue;
            float dx = m.pos.x - player.pos.x, dy = m.pos.y - player.pos.y;
            float rr = m.radius + player.radius;
            float d2 = dx * dx + dy * dy;
            if (d2 <= rr * rr) {
                m.attackCd -= dt;
                if (m.attackCd <= 0f) {
                    m.attackCd = 0.8f;
                    player.receiveDamage(m.def.damage);
                    float dmg = m.def.damage;
                    texts.add(new FloatingText(player.pos.x, player.pos.y + 30f,
                            "-" + (int) dmg, new Color(1f, 0.4f, 0.4f, 1f), 1.1f));
                }
            }
        }
    }

    private void collideExpOrbs(float dt) {
        float pr = player.stats.pickupRadius * pickupRadiusMul;
        for (ExpOrb o : expOrbs) {
            if (!o.alive) continue;
            float dx = player.pos.x - o.pos.x, dy = player.pos.y - o.pos.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < pr) {
                o.attracted = true;
                float nx = dx / Math.max(0.001f, d);
                float ny = dy / Math.max(0.001f, d);
                float pull = 700f;
                o.pos.x += nx * pull * dt;
                o.pos.y += ny * pull * dt;
            }
            if (d < player.radius + o.radius) {
                player.addExp(o.value);
                o.alive = false;
            }
        }
    }

    private void updateSummons(float dt) {
        for (Summon s : summons) {
            if (!s.alive) continue;
            s.attackCd -= dt;
            if (s.attackCd > 0f) continue;
            BattleMonster t = findNearestMonsterFrom(s.pos.x, s.pos.y, 420f);
            if (t == null) continue;
            s.attackCd = 1f / s.fireRate;
            float dmg = s.damage * summonDamageMul;
            DamageCalculator.Result r = DamageCalculator.compute(
                    dmg, player.stats.critRate + extraCritRate,
                    player.stats.critDamage + extraCritDamage,
                    1f, 1f, 1f, 1f, 1f, t.def.critResist, false);
            t.takeDamage(r.damage);
            if (!t.alive) onMonsterDeath(t);
        }
    }

    private BattleMonster findNearestMonsterFrom(float x, float y, float range) {
        BattleMonster best = null;
        float bestD = range * range;
        for (BattleMonster m : monsters) {
            if (!m.alive || m.dormant) continue;
            float dx = m.pos.x - x, dy = m.pos.y - y;
            float d = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = m; }
        }
        return best;
    }

    private void onMonsterDeath(BattleMonster m) {
        // 掉落经验球（经验倍率在此统一生效）
        int orbCount = m.boss ? 12 : (m.elite ? 5 : 1);
        float perOrb = m.def.exp / Math.max(1, orbCount) * expGainMul;
        for (int i = 0; i < orbCount; i++) {
            ExpOrb o = pools.obtainExpOrb();
            o.drop(m.pos.x, m.pos.y, perOrb);
            expOrbs.add(o);
        }
        // BOSS 掉落暴击突破晶
        if (m.boss) {
            crystals++;
            player.weaponBreakLevel++;
            texts.add(new FloatingText(m.pos.x, m.pos.y + 40f, "暴击突破晶!",
                    new Color(1f, 0.85f, 0.35f, 1f), 1.5f));
        }
    }

    // ================= 事件 =================

    public void onPlayerLevelUp() {
        // 触发技能三选一（由 Screen 层弹出）
        if (onLevelUpHook != null) onLevelUpHook.run();
    }

    public Runnable onLevelUpHook;

    public void onPlayerRevive() {
        texts.add(new FloatingText(player.pos.x, player.pos.y + 40f, "重生!",
                new Color(0.5f, 1f, 0.7f, 1f), 1.5f));
    }

    private void endGame(boolean win) {
        if (gameOver) return;
        gameOver = true;
        running = false;
        victory = win;
        if (onGameOver != null) onGameOver.run();
    }

    public void cleanup() {
        for (int i = monsters.size - 1; i >= 0; i--) {
            BattleMonster m = monsters.get(i);
            if (!m.alive && m.deathTime <= 0f) {
                pools.monster.free(m);
                monsters.removeIndex(i);
            }
        }
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (!b.alive) {
                pools.bullet.free(b);
                bullets.removeIndex(i);
            }
        }
        for (int i = expOrbs.size - 1; i >= 0; i--) {
            ExpOrb o = expOrbs.get(i);
            if (!o.alive) {
                pools.expOrb.free(o);
                expOrbs.removeIndex(i);
            }
        }
        for (int i = summons.size - 1; i >= 0; i--) {
            Summon s = summons.get(i);
            if (!s.alive) {
                pools.summon.free(s);
                summons.removeIndex(i);
            }
        }
        for (int i = texts.size - 1; i >= 0; i--) {
            if (texts.get(i).isDead()) texts.removeIndex(i);
        }
    }

    // ================= 技能应用 =================

    /**
     * 应用一层技能。
     *
     * 【修复】原实现存在多处「累计值被重复叠加」的数值 bug：
     *  - sk_bul_1: extraMaxHp += maxHp*v/100 后又 maxHp += extraMaxHp，
     *    第二次取技能时 extraMaxHp 里已含上次结果 → 生命按平方级暴涨。
     *  - sk_bul_2 / sk_gen_greed: 同样把累计倍率再次乘进 stats。
     *  - sk_swift_1: attackSpeedMul 每次乘进 stats.attackSpeed，攻速指数增长。
     * 现改为：所有派生属性都从「基础值 + 当前层数」重新计算，保证可叠加且幂等。
     */
    public void applySkill(SkillDef d) {
        if (d == null) return;
        skills.acquire(d);
        int lv = skills.levelOf(d.id);
        float v = d.value;

        switch (d.id) {
            case "sk_crit_1": styleMul *= (1f + v / 100f); break;
            case "sk_crit_2": extraCritRate += v; extraCritDamage += v; break;
            case "sk_crit_3": shockwaveBonus = true; shockwaveExtra += v; break;
            case "sk_swift_1": attackSpeedMul *= (1f + v / 100f); break;
            case "sk_swift_2": player.stats.bulletCount += (int) v; break;
            case "sk_swift_3": extraCritRate += v * 0.5f; break;
            case "sk_void_1": summonCap += (int) v; spawnSummon(); break;
            case "sk_void_2": summonDamageMul *= (1f + v / 100f); break;
            case "sk_void_3": summonCap += (int) v; spawnSummon(); break;
            // 【修复】最大生命按「层数」线性叠加，且不再叠加到 extraMaxHp 上
            case "sk_bul_1": {
                float add = baseMaxHp * v / 100f;
                player.maxHp += add;
                player.curHp += add;
                break;
            }
            case "sk_bul_2": damageReductionExtra += v; break;
            case "sk_bul_3": extraRevives++; player.revives++; break;
            case "sk_reb_1": echoPerMin += (int) v; break;
            case "sk_reb_2": player.revives++; break;
            case "sk_reb_3": cdReduction += v; break;
            case "sk_gen_magnet": pickupRadiusMul *= (1f + v / 100f); break;
            case "sk_gen_greed": expGainMul *= (1f + v / 100f); break;
            case "sk_gen_god": buffMul *= (1f + v / 100f); break;
            default: break;
        }

        // 同步派生属性：用「基础值 × 倍率」重算，避免重复累乘
        player.stats.attackSpeed = baseAttackSpeed * attackSpeedMul;
        player.stats.moveSpeed = baseMoveSpeed * moveSpeedMul;
        player.stats.damageReduction = baseDamageReduction + damageReductionExtra;
        // 【修复】expGain 之前被直接赋值 (= expGainMul)，而 addExp 里又乘了一次 stats.expGain，
        // 导致经验收益被平方。现在统一：addExp 用裸值，乘法只在此处做一次。
        player.stats.expGain = expGainMul;
    }

    /** 入局时的基础属性快照（技能加成从这些值重新计算）。 */
    private float baseAttackSpeed = 1f;
    private float baseMoveSpeed = 220f;
    private float baseDamageReduction = 0f;
    private float baseMaxHp = 100f;

    private void spawnSummon() {
        if (summons.size >= summonCap) return;
        Summon s = pools.obtainSummon();
        s.setup(60f, 8f, 0.85f);
        s.pos.set(player.pos.x + 40f, player.pos.y);
        summons.add(s);
    }
}
