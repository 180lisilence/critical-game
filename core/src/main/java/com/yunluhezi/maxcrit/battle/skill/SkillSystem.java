package com.yunluhezi.maxcrit.battle.skill;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.yunluhezi.maxcrit.config.ConfigHub;
import com.yunluhezi.maxcrit.config.SkillDef;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能系统：升级三选一的抽取与叠加。
 * 稀有技能权重更低；已满级技能不再出现；低级技能可进阶为高级形态。
 */
public class SkillSystem {

    /** 玩家已持有的技能：id -> 层数。 */
    public final ObjectMap<String, Integer> owned = new ObjectMap<>();

    private final ConfigHub config;

    public SkillSystem(ConfigHub config) {
        this.config = config;
    }

    /**
     * 抽取 3 个候选技能（不重复）。
     */
    public List<SkillDef> rollThree(int playerLevel) {
        List<SkillDef> pool = new ArrayList<>();
        for (SkillDef d : config.skills) {
            if (d.minLevel > playerLevel) continue;
            int have = owned.get(d.id, 0);
            if (have >= d.maxStack) {
                // 满级但可进阶 → 用进阶形态替换
                if (d.upgradeTo != null) {
                    SkillDef up = config.skill(d.upgradeTo);
                    if (up != null && owned.get(up.id, 0) < up.maxStack) pool.add(up);
                }
                continue;
            }
            pool.add(d);
        }
        if (pool.isEmpty()) return pool;

        List<SkillDef> result = new ArrayList<>();
        int count = Math.min(3, pool.size());
        for (int i = 0; i < count; i++) {
            // 按权重抽取（稀有度越高权重越低）
            float total = 0f;
            for (SkillDef d : pool) {
                if (result.contains(d)) continue;
                total += effectiveWeight(d);
            }
            if (total <= 0f) break;
            float r = (float) Math.random() * total;
            float acc = 0f;
            SkillDef chosen = null;
            for (SkillDef d : pool) {
                if (result.contains(d)) continue;
                acc += effectiveWeight(d);
                if (r <= acc) { chosen = d; break; }
            }
            if (chosen == null) {
                for (SkillDef d : pool) {
                    if (!result.contains(d)) { chosen = d; break; }
                }
            }
            if (chosen != null) result.add(chosen);
        }
        return result;
    }

    private float effectiveWeight(SkillDef d) {
        int have = owned.get(d.id, 0);
        // 已持有的技能权重减半，鼓励构筑多样性
        float w = d.weight * (have > 0 ? 0.5f : 1f);
        // 稀有度修正
        w *= (6 - d.rarity) / 5f;
        return Math.max(0.1f, w);
    }

    public void acquire(SkillDef d) {
        int have = owned.get(d.id, 0);
        owned.put(d.id, have + 1);
    }

    public int levelOf(String id) {
        return owned.get(id, 0);
    }
}
