package com.mygdx.td.ally;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.Enemy;

/**
 * AllyUnit – lính đứng trên trụ.
 * offsetX: lệch ngang để hỗ trợ 2 unit trên tower cấp cao.
 */
public class AllyUnit {

    public enum State { IDLE, PREATTACK, ATTACK, DEAD }
    public enum Facing { SIDE, UP, DOWN }

    public State state = State.IDLE;
    public Facing facing = Facing.SIDE;

    public float stateTime = 0f;
    public float auxTimer   = 0f;

    public final Vector2 pos = new Vector2();
    public final Vector2 dir = new Vector2(1, 0);

    public boolean facingRight = true;

    public float hp = 100f;
    public float maxHp = 100f;

    public Tower owner;
    public Enemy targetRef;

    public float offsetX = 0f;   // NEW: lệch ngang giữa nhiều ally cùng tower

    public boolean isDead() { return state == State.DEAD; }

    public void setState(State newState) {
        if (state != newState) {
            state = newState;
            stateTime = 0f;
            auxTimer  = 0f;
        }
    }

    public void damage(float dmg) {
        if (isDead()) return;
        hp -= dmg;
        if (hp <= 0f) {
            hp = 0f;
            setState(State.DEAD);
        }
    }
}
