package com.mygdx.td.abilities;

public enum AbilityType {
    LIGHTNING,
    BARREL;

    public boolean isTargetEnemy() {
        return this == LIGHTNING;
    }

    public boolean isTargetGround() {
        return this == BARREL;
    }
}
