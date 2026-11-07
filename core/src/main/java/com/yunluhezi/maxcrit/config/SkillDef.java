package com.yunluhezi.maxcrit.config;

import com.badlogic.gdx.utils.Json;

/** 技能配置（对应 assets/config/skill.json）。 */
public class SkillDef {
    public String id;
    public String name;
    /** 五大流派：crit / swift / void / bulwark / rebirth */
    public String style;
    public String desc;
    /** 稀有度 1~5，越高越稀有，抽取权重越低。 */
    public int rarity = 1;
    /** 最大叠加层数。 */
    public int maxStack = 5;
    /** 权重。 */
    public float weight = 10f;
    /** 需要的最小等级。 */
    public int minLevel = 1;
    /** 进阶形态的技能 id，为空表示不可进阶。 */
    public String upgradeTo;
    /** 技能效果参数（按类型解释）。 */
    public float value = 0f;
    /** 图标资源名（assets/textures/icons/ 下）。 */
    public String icon;

    public String toJson() {
        return new Json().toJson(this);
    }
}
