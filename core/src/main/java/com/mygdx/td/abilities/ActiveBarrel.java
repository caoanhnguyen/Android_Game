package com.mygdx.td.abilities;

public class ActiveBarrel {
    public enum State { PLACE, FUSE, PRE_EXPLODE, DONE }

    public float x, y;
    public float fuseRemaining;    // đếm fuse tổng
    public float placeTimer;       // hiển thị short place anim
    public State state = State.PLACE;
    public boolean exploded = false;
    public float stateTime = 0f;   // cho animation hiển thị
    public ActiveBarrel(float x, float y, float fuseTime, float placeTime) {
        this.x = x; this.y = y;
        this.fuseRemaining = fuseTime;
        this.placeTimer = placeTime;
    }
}
