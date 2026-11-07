package com.yunluhezi.maxcrit.battle.pool;

import com.badlogic.gdx.utils.Pool;
import com.yunluhezi.maxcrit.battle.entity.Bullet;
import com.yunluhezi.maxcrit.battle.entity.ExpOrb;
import com.yunluhezi.maxcrit.battle.entity.Summon;
import com.yunluhezi.maxcrit.battle.entity.BattleMonster;

/** 统一对象池工厂：避免战斗中频繁 new 造成 GC 抖动。 */
public class Pools {

    public final Pool<Bullet> bullet = new Pool<Bullet>() {
        @Override protected Bullet newObject() { return new Bullet(); }
    };
    public final Pool<ExpOrb> expOrb = new Pool<ExpOrb>() {
        @Override protected ExpOrb newObject() { return new ExpOrb(); }
    };
    public final Pool<BattleMonster> monster = new Pool<BattleMonster>() {
        @Override protected BattleMonster newObject() { return new BattleMonster(); }
    };
    public final Pool<Summon> summon = new Pool<Summon>() {
        @Override protected Summon newObject() { return new Summon(); }
    };

    public Bullet obtainBullet() { return bullet.obtain(); }
    public ExpOrb obtainExpOrb() { return expOrb.obtain(); }
    public BattleMonster obtainMonster() { return monster.obtain(); }
    public Summon obtainSummon() { return summon.obtain(); }
}
