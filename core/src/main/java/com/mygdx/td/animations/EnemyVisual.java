package com.mygdx.td.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.td.entities.Enemy;

import java.util.ArrayList;
import java.util.List;

public class EnemyVisual implements Disposable {

    public enum Facing { DOWN, UP, SIDE }

    private final float tileSize;
    private final boolean baseFacesRight;

    private final Animation<TextureRegion> walkSide;
    private final Animation<TextureRegion> walkDown;
    private final Animation<TextureRegion> walkUp;

    private final Animation<TextureRegion> deathGeneric;
    private final Animation<TextureRegion> deathSide;
    private final Animation<TextureRegion> deathDown;
    private final Animation<TextureRegion> deathUp;

    private final Texture stripWalkSide, stripWalkDown, stripWalkUp;
    private final Texture stripDeathGeneric, stripDeathSide, stripDeathDown, stripDeathUp;

    private final int frameW, frameH;
    private final int spacingX, marginX, marginY;

    private float stateTime = 0f;
    private boolean startedDeath = false;
    private Facing deathFacing = Facing.SIDE;
    private float removeDelay = 0.2f;

    private EnemyVisual(Animation<TextureRegion> walkSide,
                        Animation<TextureRegion> walkDown,
                        Animation<TextureRegion> walkUp,
                        Animation<TextureRegion> deathGeneric,
                        Animation<TextureRegion> deathSide,
                        Animation<TextureRegion> deathDown,
                        Animation<TextureRegion> deathUp,
                        Texture stripWalkSide,
                        Texture stripWalkDown,
                        Texture stripWalkUp,
                        Texture stripDeathGeneric,
                        Texture stripDeathSide,
                        Texture stripDeathDown,
                        Texture stripDeathUp,
                        int frameW, int frameH, int spacingX, int marginX, int marginY,
                        float tileSize, boolean baseFacesRight) {
        this.walkSide = walkSide;
        this.walkDown = walkDown;
        this.walkUp = walkUp;
        this.deathGeneric = deathGeneric;
        this.deathSide = deathSide;
        this.deathDown = deathDown;
        this.deathUp = deathUp;

        this.stripWalkSide = stripWalkSide;
        this.stripWalkDown = stripWalkDown;
        this.stripWalkUp = stripWalkUp;
        this.stripDeathGeneric = stripDeathGeneric;
        this.stripDeathSide = stripDeathSide;
        this.stripDeathDown = stripDeathDown;
        this.stripDeathUp = stripDeathUp;

        this.frameW = frameW;
        this.frameH = frameH;
        this.spacingX = spacingX;
        this.marginX = marginX;
        this.marginY = marginY;

        this.tileSize = tileSize;
        this.baseFacesRight = baseFacesRight;

        this.stateTime = MathUtils.random(0f, 1f);
    }

    public static EnemyVisual fromStripsFixed(String baseFolder,
                                              String walkSideFile,
                                              String walkDownFile,
                                              String walkUpFile,
                                              String deathSideFile,
                                              String deathDownFile,
                                              String deathUpFile,
                                              String deathGenericFile,
                                              int frameW, int frameH,
                                              int framesWalk, float walkFrameSec,
                                              int framesDeath, float deathFrameSec,
                                              int spacingX, int marginX, int marginY,
                                              float tileSize,
                                              boolean baseFacesRight) {

        Texture sSide = loadOrNull(baseFolder, walkSideFile);
        Texture sDown = loadOrNull(baseFolder, walkDownFile);
        Texture sUp   = loadOrNull(baseFolder, walkUpFile);

        Texture sDeathSide = loadOrNull(baseFolder, deathSideFile);
        Texture sDeathDown = loadOrNull(baseFolder, deathDownFile);
        Texture sDeathUp   = loadOrNull(baseFolder, deathUpFile);
        Texture sDeathGen  = loadOrNull(baseFolder, deathGenericFile);

        Animation<TextureRegion> aSide = fixed(sSide, frameW, frameH, framesWalk, walkFrameSec, spacingX, marginX, marginY, Animation.PlayMode.LOOP);
        Animation<TextureRegion> aDown = fixed(sDown, frameW, frameH, framesWalk, walkFrameSec, spacingX, marginX, marginY, Animation.PlayMode.LOOP);
        Animation<TextureRegion> aUp   = fixed(sUp,   frameW, frameH, framesWalk, walkFrameSec, spacingX, marginX, marginY, Animation.PlayMode.LOOP);

        // Fail-fast: phải có ít nhất 1 anim đi bộ
        if (aSide == null && aDown == null && aUp == null) {
            throw new IllegalArgumentException("EnemyVisual: No walking strips loaded. Check file names and frame sizes.");
        }

        Animation<TextureRegion> aDeathSide = fixed(sDeathSide, frameW, frameH, framesDeath, deathFrameSec, spacingX, marginX, marginY, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aDeathDown = fixed(sDeathDown, frameW, frameH, framesDeath, deathFrameSec, spacingX, marginX, marginY, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aDeathUp   = fixed(sDeathUp,   frameW, frameH, framesDeath, deathFrameSec, spacingX, marginX, marginY, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aDeathGen  = fixed(sDeathGen,  frameW, frameH, framesDeath, deathFrameSec, spacingX, marginX, marginY, Animation.PlayMode.NORMAL);

        int finalFW = frameW, finalFH = frameH;
        if (finalFW <= 0 || finalFH <= 0) {
            Texture any = firstNonNull(sSide, sDown, sUp, sDeathSide, sDeathDown, sDeathUp, sDeathGen);
            if (any == null) throw new IllegalArgumentException("EnemyVisual: no textures provided");
            int guessFrames = (framesWalk > 0 ? framesWalk : (framesDeath > 0 ? framesDeath : 4));
            finalFW = any.getWidth() / Math.max(guessFrames, 1);
            finalFH = any.getHeight();
        }

        return new EnemyVisual(
            aSide, aDown, aUp,
            aDeathGen, aDeathSide, aDeathDown, aDeathUp,
            sSide, sDown, sUp, sDeathGen, sDeathSide, sDeathDown, sDeathUp,
            finalFW, finalFH, spacingX, marginX, marginY,
            tileSize, baseFacesRight
        );
    }

    private static Texture firstNonNull(Texture... ts) { for (Texture t : ts) if (t != null) return t; return null; }

    private static Texture loadOrNull(String base, String file) {
        if (file == null) return null;
        try {
            Texture t = new Texture(Gdx.files.internal(base + "/" + file));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return t;
        } catch (Exception e) {
            Gdx.app.error("EnemyVisual", "Missing texture: " + base + "/" + file);
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
            int fw = texW / frames;
            int fh = texH;
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
        Gdx.app.log("EnemyVisual", "Built anim frames=" + arr.length + " size=" + frameW + "x" + frameH + " from " + strip);
        return a;
    }

    public void update(Enemy enemy, float dt) {
        if (enemy.isDead() && !startedDeath) {
            startedDeath = true;
            stateTime = 0f;
            deathFacing = chooseFacing(enemy);
        } else {
            stateTime += dt;
        }
        if (startedDeath) {
            Animation<TextureRegion> d = currentDeathAnim();
            if (d != null && d.isAnimationFinished(stateTime)) {
                removeDelay -= dt;
            }
        }
    }

    public boolean isReadyToRemove(Enemy enemy) {
        if (!enemy.isDead()) return false;
        Animation<TextureRegion> d = currentDeathAnim();
        if (d == null) return true;
        return d.isAnimationFinished(stateTime) && removeDelay <= 0f;
    }

    public void draw(Batch batch, Enemy enemy) {
        TextureRegion fr = currentFrame(enemy);
        if (fr == null) return;
        boolean flipX = needFlipX(enemy);

        float scale = tileSize > 0f ? (tileSize / (float) frameH) : 1f;
        float w = frameW * scale;
        float h = frameH * scale;

        float x = enemy.getPos().x - w / 2f;
        float y = enemy.getPos().y - (tileSize * 0.5f);

        int drawX = Math.round(x);
        int drawY = Math.round(y);

        if (flipX) batch.draw(fr, drawX + w, drawY, -w, h);
        else batch.draw(fr, drawX, drawY, w, h);
    }

    private TextureRegion currentFrame(Enemy enemy) {
        if (enemy.isDead()) {
            Animation<TextureRegion> d = currentDeathAnim();
            if (d != null) return d.getKeyFrame(stateTime, false);
        }

        Facing facing = chooseFacing(enemy);
        switch (facing) {
            case UP:
                if (walkUp != null)   return walkUp.getKeyFrame(stateTime, true);
                if (walkDown != null) return walkDown.getKeyFrame(stateTime, true);
                break;
            case DOWN:
                if (walkDown != null) return walkDown.getKeyFrame(stateTime, true);
                if (walkUp != null)   return walkUp.getKeyFrame(stateTime, true);
                break;
            case SIDE:
            default:
                if (walkSide != null) return walkSide.getKeyFrame(stateTime, true);
                if (walkDown != null) return walkDown.getKeyFrame(stateTime, true);
                if (walkUp != null)   return walkUp.getKeyFrame(stateTime, true);
        }
        return null;
    }

    private Animation<TextureRegion> currentDeathAnim() {
        switch (deathFacing) {
            case UP:
                if (deathUp != null) return deathUp;
                if (deathGeneric != null) return deathGeneric;
                return deathSide != null ? deathSide : deathDown;
            case DOWN:
                if (deathDown != null) return deathDown;
                if (deathGeneric != null) return deathGeneric;
                return deathSide != null ? deathSide : deathUp;
            case SIDE:
            default:
                if (deathSide != null) return deathSide;
                if (deathGeneric != null) return deathGeneric;
                return deathDown != null ? deathDown : deathUp;
        }
    }

    private Facing chooseFacing(Enemy enemy) {
        float ax = Math.abs(enemy.getDir().x);
        float ay = Math.abs(enemy.getDir().y);
        if (ay > ax) return enemy.getDir().y >= 0f ? Facing.UP : Facing.DOWN;
        return Facing.SIDE;
    }

    private boolean needFlipX(Enemy enemy) {
        Facing f = enemy.isDead() ? deathFacing : chooseFacing(enemy);
        if (f != Facing.SIDE) return false;
        return baseFacesRight ? (enemy.getDir().x < 0f) : (enemy.getDir().x > 0f);
    }

    @Override
    public void dispose() {
        if (stripWalkSide != null) stripWalkSide.dispose();
        if (stripWalkDown != null) stripWalkDown.dispose();
        if (stripWalkUp != null) stripWalkUp.dispose();
        if (stripDeathGeneric != null) stripDeathGeneric.dispose();
        if (stripDeathSide != null) stripDeathSide.dispose();
        if (stripDeathDown != null) stripDeathDown.dispose();
        if (stripDeathUp != null) stripDeathUp.dispose();
    }
}
