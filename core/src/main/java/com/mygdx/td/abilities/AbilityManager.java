package com.mygdx.td.abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.td.TDGame;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.world.World;

public class AbilityManager implements Disposable {

    // Paths
    private static final String ICON_LIGHTNING_PATH = "abilities/icons/lightning_icon.png";
    private static final String ICON_BARREL_PATH    = "abilities/icons/barrel_icon.png";
    private static final String LIGHTNING_STRIP_PATH = "abilities/lightning/lightning_strip.png"; // 6 frame, 64x160
    private static final String BARREL_PLACE_PATH       = "abilities/barrel/barrel_place.png";       // 4 x 48x48
    private static final String BARREL_FUSE_PATH        = "abilities/barrel/barrel_fuse.png";        // 4 x 48x48
    private static final String BARREL_PRE_EXPLODE_PATH = "abilities/barrel/barrel_pre_explode.png"; // 4 x 48x48
    private static final String BARREL_EXPLOSION_PATH   = "abilities/barrel/barrel_explosion.png";   // 8 x 48x48

    // Frame sizes
    private static final int LIGHTNING_FW = 64, LIGHTNING_FH = 160, LIGHTNING_FRAMES = 6;
    private static final int B48 = 48;

    // Tuning
    public int COST_LIGHTNING = 100;
    public int COST_BARREL    = 70;

    private static final float LIGHTNING_FRAME_SEC = 0.06f;

    private static final float BARREL_PLACE_TIME = 0.25f;
    private static final float BARREL_PRE_EXPLODE_TIME = 0.30f;
    private static final float BARREL_FUSE_TIME = 1.20f;
    private static final float EXPLOSION_RADIUS = 120f;
    private static final float EXPLOSION_DAMAGE = 100f;
    private static final float SLOW_PERCENT = 0.25f; // 25%
    private static final float SLOW_DURATION = 2.5f;

    private static final float PICK_RADIUS = 64f; // pick enemy cho lightning

    private final World world;

    // Animations / textures
    public final Texture iconLightning;
    public final Texture iconBarrel;

    private final Animation<TextureRegion> lightningAnim;

    private final Animation<TextureRegion> barrelPlaceAnim;
    private final Animation<TextureRegion> barrelFuseAnim;
    private final Animation<TextureRegion> barrelPreExplodeAnim;
    private final Animation<TextureRegion> barrelExplosionAnim;

    // Runtime state
    public static class LightningEffect {
        public float x, y;
        public float stateTime;
        public boolean done;
    }
    public static class ExplosionEffect {
        public float x, y;
        public float stateTime;
        public boolean done;
    }

    private final Array<ActiveBarrel> barrels = new Array<>();
    private final Array<LightningEffect> lightningFX = new Array<>();
    private final Array<ExplosionEffect> explosionFX = new Array<>();

    public AbilityManager(World world) {
        this.world = world;

        // Load icons
        iconLightning = safeTex(ICON_LIGHTNING_PATH);
        iconBarrel    = safeTex(ICON_BARREL_PATH);

        // Load animations
        lightningAnim = buildStrip(LIGHTNING_STRIP_PATH, LIGHTNING_FW, LIGHTNING_FH, LIGHTNING_FRAMES, LIGHTNING_FRAME_SEC, Animation.PlayMode.NORMAL);

        barrelPlaceAnim      = buildStrip(BARREL_PLACE_PATH, B48, B48, 4, 0.06f, Animation.PlayMode.NORMAL);
        barrelFuseAnim       = buildStrip(BARREL_FUSE_PATH, B48, B48, 4, 0.10f, Animation.PlayMode.LOOP);
        barrelPreExplodeAnim = buildStrip(BARREL_PRE_EXPLODE_PATH, B48, B48, 4, 0.08f, Animation.PlayMode.NORMAL);
        barrelExplosionAnim  = buildStrip(BARREL_EXPLOSION_PATH, B48, B48, 8, 0.06f, Animation.PlayMode.NORMAL);
    }

    private Texture safeTex(String path) {
        try {
            Texture t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return t;
        } catch (Throwable t) {
            return null;
        }
    }

    private Animation<TextureRegion> buildStrip(String path, int fw, int fh, int frames, float frameSec, Animation.PlayMode mode) {
        Texture tex = safeTex(path);
        if (tex == null) return null;
        TextureRegion[][] split = TextureRegion.split(tex, fw, fh);
        Array<TextureRegion> arr = new Array<>();
        for (int i = 0; i < frames; i++) arr.add(split[0][i]);
        Animation<TextureRegion> anim = new Animation<>(frameSec, arr);
        anim.setPlayMode(mode);
        return anim;
    }

    public void reset() {
        barrels.clear();
        lightningFX.clear();
        explosionFX.clear();
    }

    // ============ Public API (pay-per-use) ============
    public boolean useLightningAt(float wx, float wy) {
        if (world.gold < COST_LIGHTNING) return false;
        Enemy e = findNearestEnemy(wx, wy, PICK_RADIUS);
        if (e == null) return false;
        world.gold -= COST_LIGHTNING;

        float damage = 220f; // đơn giản; có thể scale theo wave nếu muốn
        e.damage(damage);

        LightningEffect fx = new LightningEffect();
        fx.x = e.getPos().x;
        fx.y = e.getPos().y + 40f;
        fx.stateTime = 0f;
        lightningFX.add(fx);

        playLightningSfx();
        return true;
    }

    public boolean placeBarrel(float wx, float wy) {
        if (world.gold < COST_BARREL) return false;

        int waiting = 0;
        for (ActiveBarrel b: barrels) if (!b.exploded) waiting++;
        if (waiting >= 3) return false;

        world.gold -= COST_BARREL;
        ActiveBarrel b = new ActiveBarrel(wx, wy, BARREL_FUSE_TIME, BARREL_PLACE_TIME);
        barrels.add(b);
        return true;
    }

    // ============ Update / Draw ============
    public void update(float dt) {
        // Update lightning effects
        for (int i = lightningFX.size - 1; i >= 0; i--) {
            LightningEffect fx = lightningFX.get(i);
            fx.stateTime += dt;
            if (lightningAnim != null && fx.stateTime > lightningAnim.getAnimationDuration()) {
                fx.done = true;
                lightningFX.removeIndex(i);
            }
        }

        // Update barrels
        for (int i = barrels.size - 1; i >= 0; i--) {
            ActiveBarrel b = barrels.get(i);
            b.stateTime += dt;

            switch (b.state) {
                case PLACE:
                    b.placeTimer -= dt;
                    b.fuseRemaining -= dt;
                    if (b.placeTimer <= 0f) {
                        b.state = ActiveBarrel.State.FUSE;
                        b.stateTime = 0f;
                    }
                    break;
                case FUSE:
                    b.fuseRemaining -= dt;
                    if (b.fuseRemaining <= BARREL_PRE_EXPLODE_TIME) {
                        b.state = ActiveBarrel.State.PRE_EXPLODE;
                        b.stateTime = 0f;
                    }
                    break;
                case PRE_EXPLODE:
                    b.fuseRemaining -= dt;
                    if (b.fuseRemaining <= 0f) {
                        explodeBarrel(b);
                        b.state = ActiveBarrel.State.DONE;
                        b.stateTime = 0f;
                        b.exploded = true;
                    }
                    break;
                case DONE:
                    barrels.removeIndex(i);
                    break;
            }
        }

        // Update explosion effects
        for (int i = explosionFX.size - 1; i >= 0; i--) {
            ExplosionEffect ex = explosionFX.get(i);
            ex.stateTime += dt;
            if (barrelExplosionAnim != null && ex.stateTime > barrelExplosionAnim.getAnimationDuration()) {
                ex.done = true;
                explosionFX.removeIndex(i);
            }
        }
    }

    public void draw(Batch batch) {
        // Draw barrels (place/fuse/pre-explode)
        for (ActiveBarrel b : barrels) {
            TextureRegion reg = null;
            switch (b.state) {
                case PLACE:
                    if (barrelPlaceAnim != null) reg = barrelPlaceAnim.getKeyFrame(b.stateTime, false);
                    break;
                case FUSE:
                    if (barrelFuseAnim != null) reg = barrelFuseAnim.getKeyFrame(b.stateTime, true);
                    break;
                case PRE_EXPLODE:
                    if (barrelPreExplodeAnim != null) reg = barrelPreExplodeAnim.getKeyFrame(b.stateTime, false);
                    break;
                default: break;
            }
            if (reg != null) {
                float w = reg.getRegionWidth();
                float h = reg.getRegionHeight();
                batch.draw(reg, b.x - w/2f, b.y - h/2f);
            }
        }

        // Draw explosion effects
        for (ExplosionEffect ex : explosionFX) {
            if (barrelExplosionAnim == null) continue;
            TextureRegion reg = barrelExplosionAnim.getKeyFrame(ex.stateTime, false);
            float w = reg.getRegionWidth(), h = reg.getRegionHeight();
            batch.draw(reg, ex.x - w/2f, ex.y - h/2f);
        }

        // Draw lightning effects
        for (LightningEffect fx : lightningFX) {
            if (lightningAnim == null) continue;
            TextureRegion reg = lightningAnim.getKeyFrame(fx.stateTime, false);
            float w = reg.getRegionWidth(), h = reg.getRegionHeight();
            batch.draw(reg, fx.x - w/2f, fx.y - h/2f);
        }
    }

    // ============ Helpers ============
    private Enemy findNearestEnemy(float x, float y, float radius) {
        float r2 = radius * radius;
        Enemy best = null;
        float bestD2 = Float.MAX_VALUE;
        for (Enemy e : world.enemies) {
            if (e.isDead() || e.hasReachedEnd()) continue;
            float dx = e.getPos().x - x;
            float dy = e.getPos().y - y;
            float d2 = dx*dx + dy*dy;
            if (d2 <= r2 && d2 < bestD2) {
                bestD2 = d2; best = e;
            }
        }
        return best;
    }

    private void explodeBarrel(ActiveBarrel b) {
        // Damage + slow
        float r = EXPLOSION_RADIUS;
        float r2 = r * r;
        for (Enemy e : world.enemies) {
            if (e.isDead() || e.hasReachedEnd()) continue;
            float dx = e.getPos().x - b.x;
            float dy = e.getPos().y - b.y;
            float d2 = dx*dx + dy*dy;
            if (d2 <= r2) {
                e.damage(EXPLOSION_DAMAGE);
                e.applySlow(SLOW_PERCENT, SLOW_DURATION);
            }
        }
        // spawn effect
        ExplosionEffect ex = new ExplosionEffect();
        ex.x = b.x; ex.y = b.y; ex.stateTime = 0f;
        explosionFX.add(ex);

        playBarrelSfx();
    }

    private void playLightningSfx() {
        TDGame g = world.getGame();
        if (g != null && g.assets != null && g.assets.lightning_sound != null) {
            g.playSound(g.assets.lightning_sound);
        }
    }

    private void playBarrelSfx() {
        TDGame g = world.getGame();
        if (g != null && g.assets != null && g.assets.barrel_sound != null) {
            g.playSound(g.assets.barrel_sound);
        }
    }

    // Getters for icons (AbilityHud dùng)
    public Texture getLightningIcon() { return iconLightning; }
    public Texture getBarrelIcon()    { return iconBarrel; }

    @Override
    public void dispose() {
        if (iconLightning != null) iconLightning.dispose();
        if (iconBarrel != null) iconBarrel.dispose();

        disposeTex(lightningAnim);
        disposeTex(barrelPlaceAnim);
        disposeTex(barrelFuseAnim);
        disposeTex(barrelPreExplodeAnim);
        disposeTex(barrelExplosionAnim);
    }

    private void disposeTex(Animation<TextureRegion> anim) {
        if (anim == null) return;
        TextureRegion r0 = anim.getKeyFrames().length > 0 ? anim.getKeyFrames()[0] : null;
        if (r0 != null && r0.getTexture() != null) {
            r0.getTexture().dispose();
        }
    }
}
