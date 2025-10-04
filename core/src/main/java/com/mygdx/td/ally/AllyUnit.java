package com.mygdx.td.ally;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;

/**
 * Đơn vị lính đứng trên trụ hoặc sinh ra từ trụ.
 */
public class AllyUnit {
    public enum State { IDLE, PREATTACK, ATTACK, DEAD }
    public enum Facing { SIDE, UP, DOWN }

    public State state = State.IDLE;
    public Facing facing = Facing.SIDE;

    public float stateTime = 0f;

    public final Vector2 pos = new Vector2();
    public final Vector2 dir = new Vector2(1, 0);

    public float hp = 100, maxHp = 100;
    public float range = 140f;
    public float attackDamage = 30f;
    public float attackCooldown = 1.0f;
    public float attackTimer = 0f;
    public Enemy target = null;
    public Tower owner; // trụ cha (nếu cần)

    public boolean isDead() { return state == State.DEAD; }

    public void setState(State newState) {
        if (state != newState) {
            state = newState;
            stateTime = 0f;
        }
    }
}
