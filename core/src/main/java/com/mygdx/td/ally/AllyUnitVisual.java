package com.mygdx.td.ally;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Disposable;

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

        preattackSide = buildRegion(texPreattackSide, frameW, frameH);
        preattackDown = buildRegion(texPreattackDown, frameW, frameH);
        preattackUp   = buildRegion(texPreattackUp,   frameW, frameH);
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

    private TextureRegion buildRegion(Texture tex, int w, int h) {
        if (tex == null) return null;
        return new TextureRegion(tex, 0, 0, w, h);
    }

    public void draw(Batch batch, AllyUnit unit, float stateTime) {
        Animation<TextureRegion> anim = null;
        TextureRegion region = null;
        switch (unit.state) {
            case ATTACK:
                anim = facingAnim(unit.facing, attackSide, attackDown, attackUp);
                break;
            case PREATTACK:
                region = facingRegion(unit.facing, preattackSide, preattackDown, preattackUp);
                break;
            case DEAD:
                return;
            case IDLE:
            default:
                anim = facingAnim(unit.facing, idleSide, idleDown, idleUp);
        }
        TextureRegion frame;
        if (region != null) frame = region;
        else if (anim != null) frame = anim.getKeyFrame(stateTime, unit.state == AllyUnit.State.IDLE);
        else return;

        float scale = tileSize / (float) frameH;
        float w = frameW * scale;
        float h = frameH * scale;
        float x = unit.pos.x - w / 2f;
        float y = unit.pos.y - (tileSize * 0.5f);

        batch.draw(frame, x, y, w, h);
    }

    private Animation<TextureRegion> facingAnim(AllyUnit.Facing facing, Animation<TextureRegion> side, Animation<TextureRegion> down, Animation<TextureRegion> up) {
        switch (facing) {
            case UP: return up != null ? up : side;
            case DOWN: return down != null ? down : side;
            case SIDE: default: return side;
        }
    }
    private TextureRegion facingRegion(AllyUnit.Facing facing, TextureRegion side, TextureRegion down, TextureRegion up) {
        switch (facing) {
            case UP: return up != null ? up : side;
            case DOWN: return down != null ? down : side;
            case SIDE: default: return side;
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
