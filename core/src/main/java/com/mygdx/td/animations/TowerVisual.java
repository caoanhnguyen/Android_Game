package com.mygdx.td.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.td.entities.Tower;

import java.util.ArrayList;
import java.util.List;

public class TowerVisual implements Disposable {

    public enum Facing { SIDE, UP, DOWN }

    public static class Config {
        public String baseFolder;

        // Base strips + cut config
        public String baseIdleFile;    public int baseIdleFrames;    public float baseIdleFPSec;
        public String baseUpgrade1File;public int baseUpgrade1Frames;public float baseUpgrade1FPSec;
        public String baseUpgrade2File;public int baseUpgrade2Frames;public float baseUpgrade2FPSec;
        public int baseFrameW = 70, baseFrameH = 130;
        public int baseSpacingX = 0, baseMarginX = 0, baseMarginY = 0;

        // Unit strips + cut config (có thể khác size base)
        public String unitIdleSide, unitIdleUp, unitIdleDown;
        public int unitIdleFrames = 4; public float unitIdleFPSec = 0.14f;

        public String unitPreSide, unitPreUp, unitPreDown;
        public int unitPreFrames = 4; public float unitPreFPSec = 0.08f;

        public String unitAtkSide, unitAtkUp, unitAtkDown;
        public int unitAtkFrames = 4; public float unitAtkFPSec = 0.08f;

        public int unitFrameW = 0, unitFrameH = 0;
        public int unitSpacingX = 0, unitMarginX = 0, unitMarginY = 0;

        // Tắt/bật vẽ unit (bạn đang cần tắt)
        public boolean enableUnit = false;
        public boolean unitFacesRight = false;

        // Kích thước vẽ tower (match đúng strip 70x130 bạn đang dùng)
        public int drawW = 70;
        public int drawH = 130;

        // Căn đáy: đáy sprite đặt tại (pos.y - anchorBottomToPosY) để khít ô 64x64
        public int anchorBottomToPosY = 32;
    }

    private final boolean unitFacesRight;
    private final boolean enableUnit;
    private final int drawW, drawH, anchorBottomToPosY;

    // Base
    private final Animation<TextureRegion> baseIdle;
    private final Animation<TextureRegion> baseUpg1;
    private final Animation<TextureRegion> baseUpg2;
    private final Texture tBaseIdle, tBaseUpg1, tBaseUpg2;

    // Unit (có thể null hoàn toàn nếu enableUnit=false)
    private final Animation<TextureRegion> uIdleSide, uIdleUp, uIdleDown;
    private final Animation<TextureRegion> uPreSide, uPreUp, uPreDown;
    private final Animation<TextureRegion> uAtkSide, uAtkUp, uAtkDown;
    private final Texture tIdleSide, tIdleUp, tIdleDown;
    private final Texture tPreSide, tPreUp, tPreDown;
    private final Texture tAtkSide, tAtkUp, tAtkDown;

    private float baseUpgState = 0f;
    private int baseUpgStage = 0; // 0: none, 1: upg1, 2: upg2
    private boolean baseUpgradeTriggered = false;

    private TowerVisual(Config c,
                        Animation<TextureRegion> baseIdle, Animation<TextureRegion> baseUpg1, Animation<TextureRegion> baseUpg2,
                        Animation<TextureRegion> uIdleSide, Animation<TextureRegion> uIdleUp, Animation<TextureRegion> uIdleDown,
                        Animation<TextureRegion> uPreSide, Animation<TextureRegion> uPreUp, Animation<TextureRegion> uPreDown,
                        Animation<TextureRegion> uAtkSide, Animation<TextureRegion> uAtkUp, Animation<TextureRegion> uAtkDown,
                        Texture tBaseIdle, Texture tBaseUpg1, Texture tBaseUpg2,
                        Texture tIdleSide, Texture tIdleUp, Texture tIdleDown,
                        Texture tPreSide, Texture tPreUp, Texture tPreDown,
                        Texture tAtkSide, Texture tAtkUp, Texture tAtkDown) {
        this.enableUnit = c.enableUnit;
        this.unitFacesRight = c.unitFacesRight;
        this.drawW = c.drawW; this.drawH = c.drawH;
        this.anchorBottomToPosY = c.anchorBottomToPosY;

        this.baseIdle = baseIdle; this.baseUpg1 = baseUpg1; this.baseUpg2 = baseUpg2;
        this.uIdleSide = uIdleSide; this.uIdleUp = uIdleUp; this.uIdleDown = uIdleDown;
        this.uPreSide = uPreSide; this.uPreUp = uPreUp; this.uPreDown = uPreDown;
        this.uAtkSide = uAtkSide; this.uAtkUp = uAtkUp; this.uAtkDown = uAtkDown;

        this.tBaseIdle = tBaseIdle; this.tBaseUpg1 = tBaseUpg1; this.tBaseUpg2 = tBaseUpg2;
        this.tIdleSide = tIdleSide; this.tIdleUp = tIdleUp; this.tIdleDown = tIdleDown;
        this.tPreSide = tPreSide; this.tPreUp = tPreUp; this.tPreDown = tPreDown;
        this.tAtkSide = tAtkSide; this.tAtkUp = tAtkUp; this.tAtkDown = tAtkDown;
    }

    public static TowerVisual fromConfig(Config c) {
        Texture tbI = loadOrNull(c.baseFolder, c.baseIdleFile);
        Texture tbU1 = loadOrNull(c.baseFolder, c.baseUpgrade1File);
        Texture tbU2 = loadOrNull(c.baseFolder, c.baseUpgrade2File);

        Animation<TextureRegion> aBaseIdle = fixed(tbI, c.baseFrameW, c.baseFrameH, c.baseIdleFrames, c.baseIdleFPSec, c.baseSpacingX, c.baseMarginX, c.baseMarginY, Animation.PlayMode.LOOP);
        Animation<TextureRegion> aBaseUpg1 = fixed(tbU1, c.baseFrameW, c.baseFrameH, c.baseUpgrade1Frames, c.baseUpgrade1FPSec, c.baseSpacingX, c.baseMarginX, c.baseMarginY, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aBaseUpg2 = fixed(tbU2, c.baseFrameW, c.baseFrameH, c.baseUpgrade2Frames, c.baseUpgrade2FPSec, c.baseSpacingX, c.baseMarginX, c.baseMarginY, Animation.PlayMode.NORMAL);

        if (aBaseIdle == null) throw new IllegalArgumentException("TowerVisual: base idle strip required.");

        Texture tiS = null, tiU = null, tiD = null;
        Texture tpS = null, tpU = null, tpD = null;
        Texture taS = null, taU = null, taD = null;

        Animation<TextureRegion> aIdleSide = null, aIdleUp = null, aIdleDown = null;
        Animation<TextureRegion> aPreSide = null, aPreUp = null, aPreDown = null;
        Animation<TextureRegion> aAtkSide = null, aAtkUp = null, aAtkDown = null;

        if (c.enableUnit) {
            tiS = loadOrNull(c.baseFolder, c.unitIdleSide);
            tiU = loadOrNull(c.baseFolder, c.unitIdleUp);
            tiD = loadOrNull(c.baseFolder, c.unitIdleDown);

            tpS = loadOrNull(c.baseFolder, c.unitPreSide);
            tpU = loadOrNull(c.baseFolder, c.unitPreUp);
            tpD = loadOrNull(c.baseFolder, c.unitPreDown);

            taS = loadOrNull(c.baseFolder, c.unitAtkSide);
            taU = loadOrNull(c.baseFolder, c.unitAtkUp);
            taD = loadOrNull(c.baseFolder, c.unitAtkDown);

            aIdleSide = fixed(tiS, c.unitFrameW, c.unitFrameH, c.unitIdleFrames, c.unitIdleFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.LOOP);
            aIdleUp   = fixed(tiU, c.unitFrameW, c.unitFrameH, c.unitIdleFrames, c.unitIdleFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.LOOP);
            aIdleDown = fixed(tiD, c.unitFrameW, c.unitFrameH, c.unitIdleFrames, c.unitIdleFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.LOOP);

            aPreSide = fixed(tpS, c.unitFrameW, c.unitFrameH, c.unitPreFrames, c.unitPreFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.NORMAL);
            aPreUp   = fixed(tpU, c.unitFrameW, c.unitFrameH, c.unitPreFrames, c.unitPreFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.NORMAL);
            aPreDown = fixed(tpD, c.unitFrameW, c.unitFrameH, c.unitPreFrames, c.unitPreFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.NORMAL);

            aAtkSide = fixed(taS, c.unitFrameW, c.unitFrameH, c.unitAtkFrames, c.unitAtkFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.NORMAL);
            aAtkUp   = fixed(taU, c.unitFrameW, c.unitFrameH, c.unitAtkFrames, c.unitAtkFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.NORMAL);
            aAtkDown = fixed(taD, c.unitFrameW, c.unitFrameH, c.unitAtkFrames, c.unitAtkFPSec, c.unitSpacingX, c.unitMarginX, c.unitMarginY, Animation.PlayMode.NORMAL);
        }

        return new TowerVisual(
            c,
            aBaseIdle, aBaseUpg1, aBaseUpg2,
            aIdleSide, aIdleUp, aIdleDown,
            aPreSide, aPreUp, aPreDown,
            aAtkSide, aAtkUp, aAtkDown,
            tbI, tbU1, tbU2,
            tiS, tiU, tiD,
            tpS, tpU, tpD,
            taS, taU, taD
        );
    }

    private static Texture loadOrNull(String folder, String file) {
        if (file == null) return null;
        try {
            Texture t = new Texture(Gdx.files.internal(folder + "/" + file));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return t;
        } catch (Exception e) {
            Gdx.app.error("TowerVisual", "Missing texture: " + folder + "/" + file);
            return null;
        }
    }

    private static Animation<TextureRegion> fixed(Texture strip,
                                                  int frameW, int frameH, int frames, float frameSec,
                                                  int spacingX, int marginX, int marginY,
                                                  Animation.PlayMode mode) {
        if (strip == null) return null;
        int texW = strip.getWidth();
        int texH = strip.getHeight();
        List<TextureRegion> list = new ArrayList<>();

        if (frameW <= 0 || frameH <= 0) {
            if (frames <= 0) frames = 1;
            int fw = texW / frames, fh = texH;
            for (int i = 0; i < frames; i++) list.add(new TextureRegion(strip, i * fw, 0, fw, fh));
        } else if (frames > 0) {
            int x = marginX, y = marginY;
            for (int i = 0; i < frames; i++) {
                if (x + frameW > texW || y + frameH > texH) break;
                list.add(new TextureRegion(strip, x, y, frameW, frameH));
                x += frameW + spacingX;
            }
        } else {
            int x = marginX, y = marginY;
            while (x + frameW <= texW && y + frameH <= texH) {
                list.add(new TextureRegion(strip, x, y, frameW, frameH));
                x += frameW + spacingX;
                if (spacingX == 0 && (x + frameW > texW)) break;
            }
        }

        if (list.isEmpty()) return null;
        TextureRegion[] arr = list.toArray(new TextureRegion[0]);
        Animation<TextureRegion> a = new Animation<>(frameSec, arr);
        a.setPlayMode(mode);
        return a;
    }

    public void triggerPlaceUpgrade() {
        baseUpgradeTriggered = true;
        baseUpgStage = (baseUpg1 != null ? 1 : (baseUpg2 != null ? 2 : 0));
        baseUpgState = 0f;
    }

    public void update(Tower t, float dt) {
        if (baseUpgradeTriggered && baseUpgStage > 0) {
            baseUpgState += dt;
            if (baseUpgStage == 1 && baseUpg1 != null && baseUpg1.isAnimationFinished(baseUpgState)) {
                baseUpgStage = (baseUpg2 != null ? 2 : 0);
                baseUpgState = 0f;
                if (baseUpgStage == 0) baseUpgradeTriggered = false;
            } else if (baseUpgStage == 2 && baseUpg2 != null && baseUpg2.isAnimationFinished(baseUpgState)) {
                baseUpgStage = 0; baseUpgState = 0f; baseUpgradeTriggered = false;
            }
        }
    }

    public void draw(Batch batch, Tower t) {
        // Vị trí: căn giữa X, đáy tại pos.y - anchor
        float w = drawW;
        float h = drawH;
        int drawX = Math.round(t.pos.x - w / 2f);
        int drawY = Math.round(t.pos.y - anchorBottomToPosY);

        // 1) Base
        TextureRegion baseFrame = getBaseFrame();
        if (baseFrame != null) batch.draw(baseFrame, drawX, drawY, w, h);

        // 2) Unit (tạm thời tắt)
        if (!enableUnit) return;

        TextureRegion unitFrame = getUnitFrame(t, chooseFacing(t.getAimDir()));
        if (unitFrame != null) {
            boolean flipX = needFlipX(t.getAimDir());
            if (flipX) batch.draw(unitFrame, drawX + w, drawY, -w, h);
            else batch.draw(unitFrame, drawX, drawY, w, h);
        }
    }

    private TextureRegion getBaseFrame() {
        if (baseUpgradeTriggered) {
            if (baseUpgStage == 1 && baseUpg1 != null) return baseUpg1.getKeyFrame(baseUpgState, false);
            if (baseUpgStage == 2 && baseUpg2 != null) return baseUpg2.getKeyFrame(baseUpgState, false);
        }
        return baseIdle != null ? baseIdle.getKeyFrame((Gdx.graphics.getFrameId() % 1000) * 0.016f, true) : null;
    }

    private TextureRegion getUnitFrame(Tower t, Facing facing) {
        float time = t.getStateTime();
        switch (t.getState()) {
            case PREATTACK:
                switch (facing) {
                    case UP:   if (uPreUp != null)   return uPreUp.getKeyFrame(time, false);
                    case DOWN: if (uPreDown != null) return uPreDown.getKeyFrame(time, false);
                    default:   return uPreSide != null ? uPreSide.getKeyFrame(time, false) : fallbackIdle(facing, time);
                }
            case ATTACK:
                switch (facing) {
                    case UP:   if (uAtkUp != null)   return uAtkUp.getKeyFrame(time, false);
                    case DOWN: if (uAtkDown != null) return uAtkDown.getKeyFrame(time, false);
                    default:   return uAtkSide != null ? uAtkSide.getKeyFrame(time, false) : fallbackIdle(facing, time);
                }
            default:
                return fallbackIdle(facing, time);
        }
    }

    private TextureRegion fallbackIdle(Facing facing, float time) {
        switch (facing) {
            case UP:   if (uIdleUp != null)   return uIdleUp.getKeyFrame(time, true);
            case DOWN: if (uIdleDown != null) return uIdleDown.getKeyFrame(time, true);
            default:   return uIdleSide != null ? uIdleSide.getKeyFrame(time, true)
                : (uIdleDown != null ? uIdleDown.getKeyFrame(time, true) : (uIdleUp != null ? uIdleUp.getKeyFrame(time, true) : null));
        }
    }

    private Facing chooseFacing(Vector2 dir) {
        float ax = Math.abs(dir.x), ay = Math.abs(dir.y);
        if (ay > ax) return dir.y >= 0 ? Facing.UP : Facing.DOWN;
        return Facing.SIDE;
    }

    private boolean needFlipX(Vector2 dir) {
        return unitFacesRight ? (dir.x < 0f) : (dir.x > 0f);
    }

    @Override
    public void dispose() {
        if (tBaseIdle != null) tBaseIdle.dispose();
        if (tBaseUpg1 != null) tBaseUpg1.dispose();
        if (tBaseUpg2 != null) tBaseUpg2.dispose();

        if (tIdleSide != null) tIdleSide.dispose();
        if (tIdleUp != null) tIdleUp.dispose();
        if (tIdleDown != null) tIdleDown.dispose();

        if (tPreSide != null) tPreSide.dispose();
        if (tPreUp != null) tPreUp.dispose();
        if (tPreDown != null) tPreDown.dispose();

        if (tAtkSide != null) tAtkSide.dispose();
        if (tAtkUp != null) tAtkUp.dispose();
        if (tAtkDown != null) tAtkDown.dispose();
    }
}
