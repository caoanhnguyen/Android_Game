package com.mygdx.td.entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Tower {

    public enum State { IDLE, PREATTACK, ATTACK, RECOVER, UPGRADING }

    public final Vector2 pos = new Vector2();
    public Rectangle placeRect = null;

    public TowerType type;  // Loại/cấp hiện tại

    private float range;
    private int damage;

    public float preattackSec = 0.18f;
    public float fireOffsetInAttackSec = 0.05f;
    public float attackSec = 0.20f;
    public float recoverSec = 0.12f;

    private float cooldown = 0f;
    private State state = State.IDLE;
    private float stateTime = 0f;
    private final Vector2 aimDir = new Vector2(1, 0);

    public Tower(float x, float y, Rectangle r, TowerType type) {
        this.pos.set(x, y);
        this.placeRect = r;
        setType(type);
    }

    public void setType(TowerType type) {
        this.type = type;
        this.range = type.range;
        this.damage = (int)type.damage;
    }

    public void upgrade() {
        TowerType next = type.nextLevel();
        if (next != null){
            setType(next);
            setDamage((int) type.damage);
            setRange(type.range);
        }
        this.state = State.UPGRADING;
        this.stateTime = 2f;
    }

    public void update(float dt) {
        if (cooldown > 0f) cooldown -= dt;
        stateTime += dt;
        switch (state) {
            case PREATTACK:
                if (stateTime >= preattackSec) { state = State.ATTACK; stateTime = 0f; }
                break;
            case ATTACK:
                if (stateTime >= attackSec) { state = State.RECOVER; stateTime = 0f; }
                break;
            case RECOVER:
                if (stateTime >= recoverSec) { state = State.IDLE; stateTime = 0f; }
                break;
            case UPGRADING:
                if (stateTime >= 0.5f) { state = State.IDLE; stateTime = 0f; }
                break;
            default: break;
        }
    }

    public float beginAttackTowards(float targetX, float targetY) {
        aimDir.set(targetX - pos.x, targetY - pos.y);
        if (aimDir.isZero()) aimDir.set(1, 0); else aimDir.nor();
        state = State.PREATTACK;
        stateTime = 0f;
        cooldown = getAttackCycleSec();
        return preattackSec + fireOffsetInAttackSec;
    }

    public boolean canFire() { return cooldown <= 0f && state == State.IDLE; }
    public void resetFireCooldown() { cooldown = getAttackCycleSec(); }
    public float getAttackCycleSec() { return preattackSec + attackSec + recoverSec; }
    public Vector2 getAimDir() { return aimDir; }
    public State getState() { return state; }
    public float getStateTime() { return stateTime; }
    public float getRange() { return range; }
    public int getDamage() { return damage; }
    public void setRange(float r) { this.range = r; }
    public void setDamage(int d) { this.damage = d; }
    public int getUpgradeLevel() { return type.upgradeLevel; }
}
