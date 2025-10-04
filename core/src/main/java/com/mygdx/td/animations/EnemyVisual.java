package com.mygdx.td.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.td.entities.Enemy;

/**
 * EnemyVisual
 *
 * Hỗ trợ 2 cách tạo:
 *  1) create(): dành cho bộ asset chuẩn có đúng 6 frame, tên cố định:
 *     S_Run.png / D_Run.png / U_Run.png / S_Death.png / D_Death.png / U_Death.png
 *  2) fromStripsFixed(): HÀM TƯƠNG THÍCH NGƯỢC (giữ nguyên signature cũ để EnemyManager hoặc code cũ không phải sửa).
 *     Bạn truyền vào tên file cụ thể (có thể trùng hoặc khác quy ước). Các tham số spacingX, marginX, marginY hiện KHÔNG dùng
 *     (strip chuẩn không có khoảng trống), nhưng vẫn giữ để không vỡ interface.
 *
 * Quy ước sheet:
 *  - Run sheet: width = frameW * frameCount, height = frameH
 *  - Death sheet: tương tự
 *  - frameCount: nếu không xác định (framesWalk < 0 && framesDeath < 0) --> mặc định 6
 */
public class EnemyVisual implements Disposable {

    public enum Facing { DOWN, UP, SIDE }

    private final Animation<TextureRegion> walkSide;
    private final Animation<TextureRegion> walkDown;
    private final Animation<TextureRegion> walkUp;

    private final Animation<TextureRegion> deathSide;
    private final Animation<TextureRegion> deathDown;
    private final Animation<TextureRegion> deathUp;

    // Raw textures để dispose
    private final Texture texWalkSide, texWalkDown, texWalkUp;
    private final Texture texDeathSide, texDeathDown, texDeathUp;

    private final int frameW;
    private final int frameH;
    private final float tileSize;
    private final boolean baseFacesRight;

    private float stateTime = 0f;
    private boolean startedDeath = false;
    private Facing deathFacing = Facing.SIDE;
    private float afterDeathDelay = 0.15f;

    private EnemyVisual(Animation<TextureRegion> walkSide,
                        Animation<TextureRegion> walkDown,
                        Animation<TextureRegion> walkUp,
                        Animation<TextureRegion> deathSide,
                        Animation<TextureRegion> deathDown,
                        Animation<TextureRegion> deathUp,
                        Texture texWalkSide,
                        Texture texWalkDown,
                        Texture texWalkUp,
                        Texture texDeathSide,
                        Texture texDeathDown,
                        Texture texDeathUp,
                        int frameW,
                        int frameH,
                        float tileSize,
                        boolean baseFacesRight) {
        this.walkSide = walkSide;
        this.walkDown = walkDown;
        this.walkUp = walkUp;
        this.deathSide = deathSide;
        this.deathDown = deathDown;
        this.deathUp = deathUp;
        this.texWalkSide = texWalkSide;
        this.texWalkDown = texWalkDown;
        this.texWalkUp = texWalkUp;
        this.texDeathSide = texDeathSide;
        this.texDeathDown = texDeathDown;
        this.texDeathUp = texDeathUp;
        this.frameW = frameW;
        this.frameH = frameH;
        this.tileSize = tileSize;
        this.baseFacesRight = baseFacesRight;
    }

    // =======================================================================
    // 1) API mới: create() – dùng tên file cố định
    // =======================================================================
    public static EnemyVisual create(String baseFolder,
                                     int frameW, int frameH, int frameCount,
                                     float walkFrameSec, float deathFrameSec,
                                     float tileSize,
                                     boolean baseFacesRight) {

        if (baseFolder.endsWith("/")) baseFolder = baseFolder.substring(0, baseFolder.length() - 1);

        Texture sRun = load(baseFolder + "/S_Run.png");
        Texture dRun = load(baseFolder + "/D_Run.png");
        Texture uRun = load(baseFolder + "/U_Run.png");

        Texture sDeath = load(baseFolder + "/S_Death.png");
        Texture dDeath = load(baseFolder + "/D_Death.png");
        Texture uDeath = load(baseFolder + "/U_Death.png");

        Animation<TextureRegion> aSRun = buildStrip(sRun, frameW, frameH, frameCount, walkFrameSec, Animation.PlayMode.LOOP);
        Animation<TextureRegion> aDRun = buildStrip(dRun, frameW, frameH, frameCount, walkFrameSec, Animation.PlayMode.LOOP);
        Animation<TextureRegion> aURun = buildStrip(uRun, frameW, frameH, frameCount, walkFrameSec, Animation.PlayMode.LOOP);

        Animation<TextureRegion> aSDeath = buildStrip(sDeath, frameW, frameH, frameCount, deathFrameSec, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aDDeath = buildStrip(dDeath, frameW, frameH, frameCount, deathFrameSec, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aUDeath = buildStrip(uDeath, frameW, frameH, frameCount, deathFrameSec, Animation.PlayMode.NORMAL);

        if (aSRun == null && aDRun == null && aURun == null) {
            Gdx.app.error("EnemyVisual", "Không có walking anim ở folder: " + baseFolder);
        }

        return new EnemyVisual(
            aSRun, aDRun, aURun,
            aSDeath, aDDeath, aUDeath,
            sRun, dRun, uRun,
            sDeath, dDeath, uDeath,
            frameW, frameH,
            tileSize, baseFacesRight
        );
    }

    // =======================================================================
    // 2) API tương thích cũ: fromStripsFixed(...)
    //     GIỮ NGUYÊN SIGNATURE CHO CODE CŨ (EnemyManager, v.v.)
    // =======================================================================
    public static EnemyVisual fromStripsFixed(String baseFolder,
                                              String walkSideFile,
                                              String walkDownFile,
                                              String walkUpFile,
                                              String deathSideFile,
                                              String deathDownFile,
                                              String deathUpFile,
                                              String deathGenericFile, // không dùng – giữ tham số
                                              int frameW, int frameH,
                                              int framesWalk, float walkFrameSec,
                                              int framesDeath, float deathFrameSec,
                                              int spacingX, int marginX, int marginY, // hiện không dùng
                                              float tileSize,
                                              boolean baseFacesRight) {

        if (baseFolder.endsWith("/")) baseFolder = baseFolder.substring(0, baseFolder.length() - 1);

        // Quyết định frameCount:
        int frameCount;
        if (framesWalk > 0) frameCount = framesWalk;
        else if (framesDeath > 0) frameCount = framesDeath;
        else frameCount = 6; // mặc định 6 như chuẩn sheet của bạn

        Texture sRun = load(filePath(baseFolder, walkSideFile));
        Texture dRun = load(filePath(baseFolder, walkDownFile));
        Texture uRun = load(filePath(baseFolder, walkUpFile));

        Texture sDeath = load(filePath(baseFolder, deathSideFile));
        Texture dDeath = load(filePath(baseFolder, deathDownFile));
        Texture uDeath = load(filePath(baseFolder, deathUpFile));
        // deathGenericFile bỏ qua

        Animation<TextureRegion> aSRun = buildStrip(sRun, frameW, frameH, frameCount, walkFrameSec, Animation.PlayMode.LOOP);
        Animation<TextureRegion> aDRun = buildStrip(dRun, frameW, frameH, frameCount, walkFrameSec, Animation.PlayMode.LOOP);
        Animation<TextureRegion> aURun = buildStrip(uRun, frameW, frameH, frameCount, walkFrameSec, Animation.PlayMode.LOOP);

        Animation<TextureRegion> aSDeath = buildStrip(sDeath, frameW, frameH, frameCount, deathFrameSec, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aDDeath = buildStrip(dDeath, frameW, frameH, frameCount, deathFrameSec, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> aUDeath = buildStrip(uDeath, frameW, frameH, frameCount, deathFrameSec, Animation.PlayMode.NORMAL);

        if (aSRun == null && aDRun == null && aURun == null) {
            Gdx.app.error("EnemyVisual", "[Compat] Không load được walking anim (fromStripsFixed) base=" + baseFolder);
        }

        return new EnemyVisual(
            aSRun, aDRun, aURun,
            aSDeath, aDDeath, aUDeath,
            sRun, dRun, uRun,
            sDeath, dDeath, uDeath,
            frameW, frameH,
            tileSize, baseFacesRight
        );
    }

    // Helper kết hợp base + filename (allow filename đã có path tương đối)
    private static String filePath(String base, String file) {
        if (file == null) return null;
        if (file.startsWith("/")) file = file.substring(1);
        return base + "/" + file;
    }

    // =======================================================================
    // Loading & Animation builders
    // =======================================================================

    private static Texture load(String path) {
        if (path == null) return null;
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.error("EnemyVisual", "Thiếu file: " + path);
            return null;
        }
        try {
            Texture t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return t;
        } catch (Exception e) {
            Gdx.app.error("EnemyVisual", "Lỗi load: " + path + " -> " + e.getMessage());
            return null;
        }
    }

    private static Animation<TextureRegion> buildStrip(Texture tex,
                                                       int frameW, int frameH,
                                                       int frameCount,
                                                       float frameSec,
                                                       Animation.PlayMode mode) {
        if (tex == null) return null;
        if (tex.getWidth() < frameW * frameCount || tex.getHeight() < frameH) {
            Gdx.app.error("EnemyVisual", "Sheet size mismatch: " + tex.getWidth() + "x" + tex.getHeight()
                + " < expected " + (frameW * frameCount) + "x" + frameH);
            return null;
        }
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new TextureRegion(tex, i * frameW, 0, frameW, frameH);
        }
        Animation<TextureRegion> anim = new Animation<>(frameSec, frames);
        anim.setPlayMode(mode);
        return anim;
    }

    // =======================================================================
    // Update / Draw
    // =======================================================================

    public void update(Enemy enemy, float dt) {
        if (enemy.isDead()) {
            if (!startedDeath) {
                startedDeath = true;
                deathFacing = chooseFacing(enemy);
                stateTime = 0f;
            } else {
                stateTime += dt;
                Animation<TextureRegion> d = currentDeathAnim();
                if (d == null || d.isAnimationFinished(stateTime)) {
                    afterDeathDelay -= dt;
                }
            }
        } else {
            stateTime += dt;
        }
    }

    public boolean isReadyToRemove(Enemy enemy) {
        if (!enemy.isDead()) return false;
        Animation<TextureRegion> d = currentDeathAnim();
        if (d == null) return afterDeathDelay <= 0f;
        return d.isAnimationFinished(stateTime) && afterDeathDelay <= 0f;
    }

    public void draw(Batch batch, Enemy enemy) {
        TextureRegion frame = currentFrame(enemy);
        if (frame == null) return;

        boolean flipX = needFlipX(enemy);
        float scale = tileSize / (float) frameH;
        float w = frameW * scale;
        float h = frameH * scale;
        float x = enemy.getPos().x - w / 2f;
        float y = enemy.getPos().y - (tileSize * 0.5f);

        if (flipX) batch.draw(frame, x + w, y, -w, h);
        else batch.draw(frame, x, y, w, h);
    }

    private TextureRegion currentFrame(Enemy e) {
        if (e.isDead()) {
            Animation<TextureRegion> d = currentDeathAnim();
            if (d != null) return d.getKeyFrame(stateTime, false);
        }
        Facing f = chooseFacing(e);
        switch (f) {
            case UP:
                if (walkUp != null) return walkUp.getKeyFrame(stateTime, true);
                if (walkDown != null) return walkDown.getKeyFrame(stateTime, true);
                if (walkSide != null) return walkSide.getKeyFrame(stateTime, true);
                break;
            case DOWN:
                if (walkDown != null) return walkDown.getKeyFrame(stateTime, true);
                if (walkUp != null) return walkUp.getKeyFrame(stateTime, true);
                if (walkSide != null) return walkSide.getKeyFrame(stateTime, true);
                break;
            case SIDE:
            default:
                if (walkSide != null) return walkSide.getKeyFrame(stateTime, true);
                if (walkDown != null) return walkDown.getKeyFrame(stateTime, true);
                if (walkUp != null) return walkUp.getKeyFrame(stateTime, true);
                break;
        }
        return null;
    }

    private Animation<TextureRegion> currentDeathAnim() {
        switch (deathFacing) {
            case UP:
                if (deathUp != null) return deathUp;
                if (deathSide != null) return deathSide;
                return deathDown;
            case DOWN:
                if (deathDown != null) return deathDown;
                if (deathSide != null) return deathSide;
                return deathUp;
            case SIDE:
            default:
                if (deathSide != null) return deathSide;
                if (deathDown != null) return deathDown;
                return deathUp;
        }
    }

    private Facing chooseFacing(Enemy e) {
        float ax = Math.abs(e.getDir().x);
        float ay = Math.abs(e.getDir().y);
        if (ay > ax) return e.getDir().y >= 0f ? Facing.UP : Facing.DOWN;
        return Facing.SIDE;
    }

    private boolean needFlipX(Enemy e) {
        Facing f = e.isDead() ? deathFacing : chooseFacing(e);
        if (f != Facing.SIDE) return false;
        // baseFacesRight = false nghĩa là sheet nhìn trái; nếu enemy di chuyển sang phải thì flip
        return baseFacesRight ? (e.getDir().x < 0f) : (e.getDir().x > 0f);
    }

    @Override
    public void dispose() {
        if (texWalkSide != null) texWalkSide.dispose();
        if (texWalkDown != null) texWalkDown.dispose();
        if (texWalkUp != null) texWalkUp.dispose();
        if (texDeathSide != null) texDeathSide.dispose();
        if (texDeathDown != null) texDeathDown.dispose();
        if (texDeathUp != null) texDeathUp.dispose();
    }
}
