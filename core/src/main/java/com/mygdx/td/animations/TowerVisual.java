package com.mygdx.td.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;

/**
 * TowerVisual: vẽ idle animation + hiệu ứng upgrade đúng file B_UpgradeX.png của loại trụ mới.
 */
public class TowerVisual {
    private static final int FRAME_W = 70;
    private static final int FRAME_H = 130;
    private static final int DRAW_W  = 70;
    private static final int DRAW_H  = 130;
    private static final int ANCHOR_BOTTOM_TO_POSY = 32;
    private static final float IDLE_FRAME_SEC    = 0.20f;
    private static final float UPGRADE_FRAME_SEC = 0.10f;

    private TowerType currentType;

    private Texture idleTex;
    private Texture upgradeTex;
    private Animation<TextureRegion> idleAnim;
    private Animation<TextureRegion> upgradeAnim;
    private boolean isUpgrading = false;
    private float animTime = 0f;

    public TowerVisual(TowerType type) {
        loadForType(type);
    }

    private void disposeIfNeeded() {
        if (idleTex != null) { idleTex.dispose(); idleTex = null; }
        if (upgradeTex != null) { upgradeTex.dispose(); upgradeTex = null; }
    }

    public void loadForType(TowerType type) {
        disposeIfNeeded();
        currentType = type;

        idleTex = new Texture(Gdx.files.internal("towers/" + type.assetFolder + "/" + type.idleFile));
        idleTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Dùng upgrade file của loại trụ mới!
        upgradeTex = new Texture(Gdx.files.internal("towers/" + type.assetFolder + "/" + type.upgradeFile));
        upgradeTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int idleFrames    = Math.max(1, idleTex.getWidth()    / FRAME_W);
        int upgradeFrames = Math.max(1, upgradeTex.getWidth() / FRAME_W);

        idleAnim    = buildStripAnimation(idleTex,    FRAME_W, FRAME_H, idleFrames,    IDLE_FRAME_SEC,    Animation.PlayMode.LOOP);
        upgradeAnim = buildStripAnimation(upgradeTex, FRAME_W, FRAME_H, upgradeFrames, UPGRADE_FRAME_SEC, Animation.PlayMode.NORMAL);

        isUpgrading = false;
        animTime = 0f;
    }

    private Animation<TextureRegion> buildStripAnimation(Texture sheet, int frameW, int frameH, int frames, float frameDur, Animation.PlayMode mode) {
        TextureRegion[] regions = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            regions[i] = new TextureRegion(sheet, i * frameW, 0, frameW, frameH);
        }
        Animation<TextureRegion> anim = new Animation<>(frameDur, regions);
        anim.setPlayMode(mode);
        return anim;
    }

    public void triggerPlace() {
        isUpgrading = true;
        animTime = 0f;
    }

    public void triggerUpgrade() {
        isUpgrading = true;
        animTime = 0f;
    }

    public void changeType(TowerType type) {
        loadForType(type);
        triggerUpgrade();
    }

    public void update(Tower t, float dt) {
        if (currentType != t.type) {
            changeType(t.type);
        }
        if (t.getState() == Tower.State.UPGRADING && !isUpgrading) {
            triggerUpgrade();
        }
        animTime += dt;
        if (isUpgrading && upgradeAnim.isAnimationFinished(animTime)) {
            isUpgrading = false;
            animTime = 0f;
        }
    }

    public void draw(Batch batch, Tower t) {
        TextureRegion frame = isUpgrading ? upgradeAnim.getKeyFrame(animTime) : idleAnim.getKeyFrame(animTime, true);
        float drawX = t.pos.x - DRAW_W / 2f;
        float drawY = t.pos.y - ANCHOR_BOTTOM_TO_POSY;
        batch.draw(frame, drawX, drawY, DRAW_W, DRAW_H);
    }

    public void dispose() {
        disposeIfNeeded();
    }
}
