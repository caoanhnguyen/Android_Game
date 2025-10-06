package com.mygdx.td.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.entities.TowerType;

/**
 * TowerVisual:
 *  - Hiển thị idle animation theo cấp hiện tại.
 *  - Hiệu ứng upgrade / place dùng cùng strip upgrade.
 *  - Hỗ trợ syncInstant(...) để đồng bộ ngay lập tức khi restore (bỏ animation).
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

    private void loadForType(TowerType type) {
        disposeIfNeeded();
        currentType = type;

        idleTex = new Texture(Gdx.files.internal("towers/" + type.assetFolder + "/" + type.idleFile));
        idleTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

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

    /** Animation khi đặt trụ lần đầu (dùng chung với upgrade strip). */
    public void triggerPlace() {
        isUpgrading = true;
        animTime = 0f;
    }

    /** Animation khi nâng cấp. */
    public void triggerUpgrade() {
        isUpgrading = true;
        animTime = 0f;
    }

    /**
     * Đổi loại/cấp (khi tower.type thay đổi). Luôn kích hoạt upgrade animation.
     */
    private void changeType(TowerType type) {
        loadForType(type);
        triggerUpgrade();
    }

    /**
     * Đồng bộ tức thì – dùng sau khi restore:
     *  - Bỏ mọi animation placing/upgrade.
     *  - Đảm bảo idleAnim hiển thị đúng cấp ngay lập tức.
     */
    public void syncInstant(Tower tower) {
        if (currentType != tower.type) {
            loadForType(tower.type);
        }
        isUpgrading = false;
        animTime = 0f;
    }

    /**
     * Cập nhật animation.
     * @param t  tower logic
     * @param dt delta time (khi pause có thể truyền 0 để giữ khung hiện tại)
     */
    public void update(Tower t, float dt) {
        // Nếu cấp thay đổi (upgrade logic đã xong) → đổi texture
        if (currentType != t.type) {
            // Nếu skipPlaceAnimation đang bật nghĩa là restore, không muốn animation
            if (t.skipPlaceAnimation) {
                loadForType(t.type);
                isUpgrading = false;
                animTime = 0f;
            } else {
                changeType(t.type);
            }
        }

        // Nếu tower đang trong trạng thái UPGRADING nhưng skipPlaceAnimation = true → bỏ animation
        if (t.skipPlaceAnimation && isUpgrading) {
            isUpgrading = false;
            animTime = 0f;
        } else if (t.getState() == Tower.State.UPGRADING && !isUpgrading && !t.skipPlaceAnimation) {
            // Tower logic set state = UPGRADING (khi upgrade thường), phát upgrade anim nếu chưa chạy
            triggerUpgrade();
        }

        animTime += dt;
        if (isUpgrading && upgradeAnim.isAnimationFinished(animTime)) {
            isUpgrading = false;
            animTime = 0f;
        }
    }

    public void draw(Batch batch, Tower t) {
        TextureRegion frame = (isUpgrading ? upgradeAnim.getKeyFrame(animTime)
            : idleAnim.getKeyFrame(animTime, true));
        float drawX = t.pos.x - DRAW_W / 2f;
        float drawY = t.pos.y - ANCHOR_BOTTOM_TO_POSY;
        batch.draw(frame, drawX, drawY, DRAW_W, DRAW_H);
    }

    public void dispose() {
        disposeIfNeeded();
    }
}
