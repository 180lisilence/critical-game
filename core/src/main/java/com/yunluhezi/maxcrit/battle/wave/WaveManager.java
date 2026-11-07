package com.yunluhezi.maxcrit.battle.wave;

import com.badlogic.gdx.utils.Array;
import com.yunluhezi.maxcrit.config.ConfigHub;
import com.yunluhezi.maxcrit.config.MonsterDef;

/**
 * 波次管理器：小怪波 → 精英波 → BOSS波，并随时间膨胀难度。
 */
public class WaveManager {

    public static class Wave {
        public int index;
        public String label;
        public int monsterCount;
        public String monsterId;
        public boolean elite;
        public boolean boss;
        public float spawnInterval = 0.35f;
        public float hpScale = 1f;
        public float speedScale = 1f;
    }

    private final ConfigHub config;
    private final Array<Wave> waves = new Array<>();

    public int currentWave = -1;
    public float waveTimer;
    public int spawnedThisWave;
    public float elapsed;
    /** 是否已进入狂暴（超时）难度。 */
    public boolean berserk;
    public boolean finished;
    /**
     * 刷怪就绪标志（计时已到，允许生成一只）。
     *
     * 【P0 根因】早期实现把「计时到点」和「计时重置」都放在 update() 里：
     *   waveTimer -= dt; if (waveTimer <= 0f) waveTimer = spawnInterval;
     * 由于 update() 在 shouldSpawn() **之前**被调用（见 BattleWorld.update），
     * waveTimer 刚被置为 0 就立刻被重置成正数 → shouldSpawn() 永远返回 false
     * → 每波只能刷出第 1 只怪，整局几乎无法游玩。
     * 现在改为：update() 只负责把计时器减到 0 并置位 spawnReady，
     * 计时器的重置交给 markSpawned()，两条职责彻底分开。
     */
    private boolean spawnReady;

    private int totalWaves;

    public WaveManager(ConfigHub config, int stageWaveCount, float baseHpScale) {
        this.config = config;
        this.totalWaves = Math.max(3, stageWaveCount);
        buildWaves(baseHpScale);
    }

    private void buildWaves(float baseHpScale) {
        waves.clear();
        String[] normals = {"mon_stone", "mon_demon"};
        for (int i = 0; i < totalWaves; i++) {
            Wave w = new Wave();
            w.index = i;
            w.hpScale = baseHpScale * (1f + i * 0.12f);
            w.speedScale = 1f + i * 0.03f;
            w.monsterId = normals[i % normals.length];
            if (i == totalWaves - 1) {
                w.label = "BOSS 波";
                w.boss = true;
                w.elite = true;
                w.monsterId = "mon_boss";
                w.monsterCount = 1;
                w.spawnInterval = 1.2f;
                w.hpScale = baseHpScale * (1.6f + i * 0.15f);
            } else if (i == totalWaves - 2) {
                w.label = "精英波";
                w.elite = true;
                w.monsterId = "mon_elite";
                w.monsterCount = 6;
                w.spawnInterval = 0.5f;
            } else {
                w.label = "第 " + (i + 1) + " 波";
                w.monsterCount = 5 + i * 2;
                w.spawnInterval = 0.3f;
            }
            waves.add(w);
        }
    }

    public Wave current() {
        if (currentWave < 0 || currentWave >= waves.size) return null;
        return waves.get(currentWave);
    }

    /**
     * 推进波次；返回本次是否推进到了新一波。
     *
     * 【刷怪时序】本方法只负责「波次切换」与「刷怪计时」，
     * 实际刷怪由 {@link #shouldSpawn()} 判定、调用方在生成实体后调用 {@link #markSpawned()}。
     * 早期版本把 spawnedThisWave++ 写在本方法里、而 shouldSpawn 又独立判断，
     * 两者竞争导致每波只能刷出 1 只怪（整局几乎无法游玩）。此处已拆分为单一职责。
     */
    public boolean update(float dt, int aliveMonsters) {
        elapsed += dt;
        // 超时进入狂暴
        if (elapsed > 300f) berserk = true;

        if (currentWave < 0) {
            currentWave = 0;
            waveTimer = 0f;
            spawnedThisWave = 0;
            spawnReady = true;
            return true;
        }

        Wave w = current();
        if (w == null) { finished = true; return false; }

        if (spawnedThisWave < w.monsterCount) {
            // 刷怪计时：只负责减到 0 并置位标志，**不重置计时器**
            if (!spawnReady) {
                waveTimer -= dt;
                if (waveTimer <= 0f) spawnReady = true;
            }
        } else {
            // 本波已刷完且场上清空 → 下一波
            if (aliveMonsters <= 0) {
                currentWave++;
                spawnedThisWave = 0;
                waveTimer = 0.8f;
                spawnReady = false;
                if (currentWave >= waves.size) {
                    finished = true;
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    /** 当前波是否有待刷出的怪（计时已到）。 */
    public boolean shouldSpawn() {
        Wave w = current();
        return w != null && spawnReady && spawnedThisWave < w.monsterCount;
    }

    /** 调用方实际生成了一只怪后调用，记账并重置下一次计时。 */
    public void markSpawned() {
        spawnedThisWave++;
        spawnReady = false;
        Wave w = current();
        if (w != null) waveTimer = w.spawnInterval * (berserk ? 0.6f : 1f);
    }

    public float timeScale() {
        return berserk ? 1.35f : 1f;
    }

    public int totalWaves() { return totalWaves; }

    public String progressText() {
        Wave w = current();
        if (w == null) return "";
        return w.label + "  (" + (currentWave + 1) + "/" + totalWaves + ")";
    }
}
