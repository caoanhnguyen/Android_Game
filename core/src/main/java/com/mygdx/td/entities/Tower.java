package com.mygdx.td.entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Tower core logic.
 * Bổ sung:
 *  - skipPlaceAnimation: nếu = true (khi restore từ save) thì:
 *        + Gọi upgrade() sẽ KHÔNG vào trạng thái UPGRADING.
 *        + Tránh phải chờ tới lúc "Play" mới thấy đúng cấp.
 *  - forceFinishPlacementAndUpgrades(): ép tower về trạng thái IDLE ngay lập tức.
 *  - upgradeNoAnimation(): nâng cấp không phát animation (có thể dùng trong restore thay cho upgrade()).
 *
 * Quy trình restore đề xuất:
 *   Tower t = new Tower(x,y,rect, baseType);
 *   t.skipPlaceAnimation = true;
 *   for (int i = 0; i < savedLevel - 1; i++) t.upgradeNoAnimation();
 *   t.forceFinishPlacementAndUpgrades();
 *   // thêm vào world, spawn ally, tạo TowerVisual và gọi visual.syncInstant(t);
 */
public class Tower {

    public enum State { IDLE, PREATTACK, ATTACK, RECOVER, UPGRADING }

    public final Vector2 pos = new Vector2();
    public Rectangle placeRect = null;

    public TowerType type;  // Loại/cấp hiện tại

    private float range;
    private int damage;

    // Attack timing
    public float preattackSec = 0.18f;
    public float fireOffsetInAttackSec = 0.05f;
    public float attackSec = 0.20f;
    public float recoverSec = 0.12f;

    private float cooldown = 0f;
    private State state = State.IDLE;
    private float stateTime = 0f;
    private final Vector2 aimDir = new Vector2(1, 0);

    /**
     * Được set = true khi tower được tạo lại từ save (restore).
     * Khi true:
     *  - upgrade() sẽ không đưa state sang UPGRADING (bỏ animation nâng cấp).
     *  - GameScreen khi tạo TowerVisual sẽ gọi syncInstant thay vì triggerPlace().
     * Sau khi đã sync visual, nên reset lại về false để các upgrade trong gameplay vẫn có animation.
     */
    public boolean skipPlaceAnimation = false;

    public Tower(float x, float y, Rectangle r, TowerType type) {
        this.pos.set(x, y);
        this.placeRect = r;
        setType(type);
    }

    private void setType(TowerType type) {
        this.type = type;
        this.range = type.range;
        this.damage = (int) type.damage;
    }

    public void upgrade() {
        TowerType next = type.nextLevel();
        if (next != null) {
            setType(next);
            // setType đã đồng bộ range & damage; các dòng dưới trở nên thừa nhưng giữ cho rõ ràng:
            setDamage((int) type.damage);
            setRange(type.range);
        }
        // Nếu đang restore (skipPlaceAnimation = true) thì không chạy animation UPGRADING
        if (skipPlaceAnimation) {
            state = State.IDLE;
            stateTime = 0f;
        } else {
            state = State.UPGRADING;
            // Thời lượng animation nâng cấp thực tế TowerVisual kiểm soát;
            // Ở logic state chỉ cần 0.5s để quay về IDLE.
            stateTime = 0f;
        }
    }

    /**
     * Nâng cấp không tạo animation, luôn ở IDLE.
     * Dùng khi apply nhiều cấp trong quá trình restore.
     */
    public void upgradeNoAnimation() {
        TowerType next = type.nextLevel();
        if (next != null) {
            setType(next);
            setDamage((int) type.damage);
            setRange(type.range);
        }
        state = State.IDLE;
        stateTime = 0f;
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
                // Nếu vì lý do nào đó vẫn vào UPGRADING khi skipPlaceAnimation true => ép về IDLE ngay.
                if (skipPlaceAnimation || stateTime >= 0.5f) { state = State.IDLE; stateTime = 0f; }
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

    /**
     * Ép huỷ mọi animation đặt / nâng cấp hiện tại (dùng sau restore).
     * Đảm bảo tower ở trạng thái IDLE sẵn sàng bắn & visual có thể sync ngay.
     */
    public void forceFinishPlacementAndUpgrades() {
        state = State.IDLE;
        stateTime = 0f;
        // Cho cooldown về 0 để vừa vào game bắn được (tùy bạn muốn giữ hay không)
        cooldown = 0f;
    }
}
