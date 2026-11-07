package com.yunluhezi.maxcrit.config;

import com.badlogic.gdx.utils.Array;

/** 怪物配置（对应 assets/config/monster.json）。 */
public class MonsterDef {
    public String id;
    public String name;
    public float hp = 30f;
    public float speed = 60f;
    /** 接触伤害。 */
    public float damage = 8f;
    /** 暴击抗性（%），会削减玩家暴击率。 */
    public float critResist = 0f;
    /** 经验球掉落量。 */
    public float exp = 3f;
    /** 掉落半径。 */
    public float radius = 22f;
    /** 是否为精英/BOSS。 */
    public boolean elite = false;
    public boolean boss = false;
    /** 外观贴图名（assets/textures/icons/ 下）。 */
    public String texture = "enemy_stone";
    /** 移动方式：chase / strafe / charger */
    public String ai = "chase";

    public Array<SkillDef> skills;
}
