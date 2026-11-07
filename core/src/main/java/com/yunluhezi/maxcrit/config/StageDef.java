package com.yunluhezi.maxcrit.config;

/** 关卡配置。 */
public class StageDef {
    public String id;
    public String name;
    public int order = 1;
    /** 关卡内波次数。 */
    public int waveCount = 8;
    /** 怪物血量缩放。 */
    public float monsterHpScale = 1f;
    /** 通关掉落的神装数量。 */
    public int rewardEquipCount = 1;
}
