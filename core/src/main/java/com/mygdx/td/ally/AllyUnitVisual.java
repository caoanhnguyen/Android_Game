package com.mygdx.td.ally;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Disposable;

/**
 * AllyUnitVisual – vẽ idle, preattack (1 frame) và attack (strip).
 * Giả định strip SIDE hướng nhìn gốc sang PHẢI.
 */
public class AllyUnitVisual implements Disposable {

    private final Animation<TextureRegion> idleSide, idleDown, idleUp;
    private final Animation<TextureRegion> attackSide, attackDown, attackUp;
    private final TextureRegion preattackSide, preattackDown, preattackUp;

    private final Texture texIdleSide, texIdleDown, texIdleUp;
    private final Texture texAttackSide, texAttackDown, texAttackUp;
    private final Texture texPreattackSide, texPreattackDown, texPreattackUp;

    private final int frameW, frameH;
    private final float tileSize;

    public AllyUnitVisual(String assetFolder, int frameW, int frameH, float tileSize) {
        this.frameW = frameW;
        this.frameH = frameH;
        this.tileSize = tileSize;

        texIdleSide = load(assetFolder + "/S_Idle.png");
        texIdleDown = load(assetFolder + "/D_Idle.png");
        texIdleUp   = load(assetFolder + "/U_Idle.png");

        texPreattackSide = load(assetFolder + "/S_Preattack.png");
        texPreattackDown = load(assetFolder + "/D_Preattack.png");
        texPreattackUp   = load(assetFolder + "/U_Preattack.png");

        texAttackSide = load(assetFolder + "/S_Attack.png");
        texAttackDown = load(assetFolder + "/D_Attack.png");
        texAttackUp   = load(assetFolder + "/U_Attack.png");

        idleSide = buildAnim(texIdleSide, frameW, frameH, 4, 0.13f, Animation.PlayMode.LOOP);
        idleDown = buildAnim(texIdleDown, frameW, frameH, 4, 0.13f, Animation.PlayMode.LOOP);
        idleUp   = buildAnim(texIdleUp,   frameW, frameH, 4, 0.13f, Animation.PlayMode.LOOP);

        attackSide = buildAnim(texAttackSide, frameW, frameH, 4, 0.12f, Animation.PlayMode.NORMAL);
        attackDown = buildAnim(texAttackDown, frameW, frameH, 4, 0.12f, Animation.PlayMode.NORMAL);
        attackUp   = buildAnim(texAttackUp,   frameW, frameH, 4, 0.12f, Animation.PlayMode.NORMAL);

        preattackSide = buildSingle(texPreattackSide, frameW, frameH);
        preattackDown = buildSingle(texPreattackDown, frameW, frameH);
        preattackUp   = buildSingle(texPreattackUp,   frameW, frameH);
    }

    private Texture load(String path) {
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.error("AllyUnitVisual", "Missing: " + path);
            return null;
        }
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    private Animation<TextureRegion> buildAnim(Texture tex, int w, int h, int frames, float frameSec, Animation.PlayMode mode) {
        if (tex == null) return null;
        TextureRegion[] arr = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            arr[i] = new TextureRegion(tex, i * w, 0, w, h);
        }
        Animation<TextureRegion> a = new Animation<>(frameSec, arr);
        a.setPlayMode(mode);
        return a;
    }

    private TextureRegion buildSingle(Texture tex, int w, int h) {
        if (tex == null) return null;
        return new TextureRegion(tex, 0, 0, w, h);
    }

    private Animation<TextureRegion> pickIdle(AllyUnit.Facing f) {
        switch (f) {
            case UP: return idleUp != null ? idleUp : idleSide;
            case DOWN: return idleDown != null ? idleDown : idleSide;
            case SIDE:
            default: return idleSide;
        }
    }

    private Animation<TextureRegion> pickAttack(AllyUnit.Facing f) {
        switch (f) {
            case UP: return attackUp != null ? attackUp : attackSide;
            case DOWN: return attackDown != null ? attackDown : attackSide;
            case SIDE:
            default: return attackSide;
        }
    }

    private TextureRegion pickPreattack(AllyUnit.Facing f) {
        switch (f) {
            case UP: return preattackUp != null ? preattackUp : preattackSide;
            case DOWN: return preattackDown != null ? preattackDown : preattackSide;
            case SIDE:
            default: return preattackSide;
        }
    }

    public void draw(Batch batch, AllyUnit unit, float stateTime) {
        if (unit.state == AllyUnit.State.DEAD) return;

        TextureRegion frame = null;
        switch (unit.state) {
            case PREATTACK:
                frame = pickPreattack(unit.facing);
                break;
            case ATTACK:
                Animation<TextureRegion> at = pickAttack(unit.facing);
                if (at != null) frame = at.getKeyFrame(stateTime, false);
                break;
            case IDLE:
            default:
                Animation<TextureRegion> id = pickIdle(unit.facing);
                if (id != null) frame = id.getKeyFrame(stateTime, true);
                break;
        }
        if (frame == null) return;

        float scale = tileSize / (float) frameH;
        float w = frame.getRegionWidth() * scale;
        float h = frame.getRegionHeight() * scale;

        float drawX = unit.pos.x - w / 2f;
        float drawY = unit.pos.y - h / 2f;

        boolean flipX = false;
        if (unit.facing == AllyUnit.Facing.SIDE) {
            // Nếu strip gốc nhìn sang PHẢI: cần flip khi facingRight == false
            if (unit.facingRight) {
                flipX = true;
            }
        }

        if (flipX) {
            batch.draw(frame, drawX + w, drawY, -w, h);
        } else {
            batch.draw(frame, drawX, drawY, w, h);
        }
    }

    @Override
    public void dispose() {
        if (texIdleSide != null) texIdleSide.dispose();
        if (texIdleDown != null) texIdleDown.dispose();
        if (texIdleUp != null) texIdleUp.dispose();
        if (texPreattackSide != null) texPreattackSide.dispose();
        if (texPreattackDown != null) texPreattackDown.dispose();
        if (texPreattackUp != null) texPreattackUp.dispose();
        if (texAttackSide != null) texAttackSide.dispose();
        if (texAttackDown != null) texAttackDown.dispose();
        if (texAttackUp != null) texAttackUp.dispose();
    }
}
