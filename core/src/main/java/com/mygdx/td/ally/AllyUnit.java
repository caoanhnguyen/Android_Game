package com.mygdx.td.ally;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.Enemy;

/**
 * AllyUnit – lính đứng trên trụ. Chỉ hiển thị / đồng bộ theo Tower.
 * Các state: IDLE -> PREATTACK -> ATTACK -> (lại IDLE)
 */
public class AllyUnit {

    public enum State { IDLE, PREATTACK, ATTACK, DEAD }
    public enum Facing { SIDE, UP, DOWN }

    public State state = State.IDLE;
    public Facing facing = Facing.SIDE;

    public float stateTime = 0f;        // thời gian trong state hiện tại
    public float auxTimer = 0f;         // timer phụ cho PREATTACK/ATTACK

    public final Vector2 pos = new Vector2();
    public final Vector2 dir = new Vector2(1, 0); // hướng nhìn chuẩn hoá (cập nhật mỗi frame theo tower.aimDir)

    public boolean facingRight = true;  // dùng để flip khi vẽ side (strip gốc giả định nhìn sang phải)
    public float hp = 100f;
    public float maxHp = 100f;

    public Tower owner;
    public Enemy targetRef; // (không dùng gây damage – chỉ để bạn mở rộng sau)

    public boolean isDead() { return state == State.DEAD; }

    public void setState(State newState) {
        if (state != newState) {
            state = newState;
            stateTime = 0f;
            auxTimer = 0f;
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
