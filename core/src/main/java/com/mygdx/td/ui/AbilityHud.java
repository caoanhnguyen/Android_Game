package com.mygdx.td.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.td.TDGame;
import com.mygdx.td.abilities.AbilityManager;
import com.mygdx.td.abilities.AbilityType;
import com.mygdx.td.world.World;

/**
 * AbilityHud – Panel abilities trong khung banner (góc phải dưới).
 * Tương tác: bấm để CHỌN, bấm lại lần nữa để HỦY chọn.
 * Khi đã chọn, GameScreen lắng nghe chạm trên map để dùng.
 * Bổ sung:
 *  - Icon & panel to hơn (icon 64x64).
 *  - Hiển thị giá bên dưới icon.
 *  - Hiệu ứng chọn scale từ TÂM (1.18x) + highlight.
 */
public class AbilityHud {

    // Kích thước / layout
    private static final float PANEL_PAD = 10f;
    private static final float ICON_SIZE = 64f;
    private static final float ITEM_PAD = 8f;
    private static final float PRICE_TOP_PAD = 6f;
    private static final float SELECT_SCALE = 1.18f;
    private static final float SCALE_DUR = 0.09f;

    private final TDGame game;
    private final World world;
    private final AbilityManager am;
    @SuppressWarnings("unused")
    private final Viewport worldViewport;
    private final Stage hudStage;

    private final Table root;
    private NinePatchDrawable bannerDrawable;

    private final BitmapFont costFont;

    private AbilityItem lightningItem;
    private AbilityItem barrelItem;

    private AbilityType selected = null;

    public AbilityHud(TDGame game, World world, AbilityManager am, Viewport worldViewport, Stage hudStage) {
        this.game = game;
        this.world = world;
        this.am = am;
        this.worldViewport = worldViewport;
        this.hudStage = hudStage;

        // Banner frame đồng bộ UI
        try {
            Texture bannerTex = new Texture(Gdx.files.internal("ui/banner_11.9.png"));
            bannerTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            NinePatch patch = new NinePatch(bannerTex, 16, 16, 16, 16);
            bannerDrawable = new NinePatchDrawable(patch);
        } catch (Throwable t) {
            bannerDrawable = null;
        }

        // Font đơn giản cho giá (nếu muốn đồng bộ font pixel, truyền từ UIHud)
        costFont = new BitmapFont();
        costFont.setUseIntegerPositions(false);

        root = new Table();
        root.setFillParent(true);
        root.bottom().right().padRight(16f).padBottom(16f);

        Table panel = new Table();
        if (bannerDrawable != null) panel.setBackground(bannerDrawable);
        panel.pad(PANEL_PAD);

        lightningItem = new AbilityItem(
            am.getLightningIcon(),
            AbilityType.LIGHTNING,
            () -> onItemClicked(AbilityType.LIGHTNING)
        );
        barrelItem = new AbilityItem(
            am.getBarrelIcon(),
            AbilityType.BARREL,
            () -> onItemClicked(AbilityType.BARREL)
        );

        panel.add(lightningItem.root).pad(ITEM_PAD);
        panel.add(barrelItem.root).pad(ITEM_PAD);

        Stack framed = new Stack(panel);
        root.add(framed).align(Align.bottomRight);
        hudStage.addActor(root);

        refreshVisual();
    }

    private void onItemClicked(AbilityType type) {
        if (selected == type) {
            selected = null;
            applySelectVisual(lightningItem, false);
            applySelectVisual(barrelItem, false);
        } else {
            if (!isAffordable(type)) {
                blinkErrorFor(type);
                return;
            }
            selected = type;
            applySelectVisual(lightningItem, selected == AbilityType.LIGHTNING);
            applySelectVisual(barrelItem, selected == AbilityType.BARREL);
        }
        refreshVisual();
    }

    private boolean isAffordable(AbilityType t) {
        if (t == AbilityType.LIGHTNING) return world.gold >= am.COST_LIGHTNING;
        if (t == AbilityType.BARREL)    return world.gold >= am.COST_BARREL;
        return false;
    }

    private void blinkErrorFor(AbilityType t) {
        AbilityItem item = (t == AbilityType.LIGHTNING) ? lightningItem : barrelItem;
        if (item == null) return;
        item.root.clearActions();
        item.root.addAction(Actions.sequence(
            Actions.color(new Color(1,1,1,0.45f), 0.10f),
            Actions.color(Color.WHITE, 0.10f)
        ));
    }

    private void applySelectVisual(AbilityItem item, boolean isSelected) {
        if (item == null) return;
        item.setSelected(isSelected);
    }

    private void refreshVisual() {
        lightningItem.setCost(am.COST_LIGHTNING);
        barrelItem.setCost(am.COST_BARREL);

        boolean canLightning = isAffordable(AbilityType.LIGHTNING) || selected == AbilityType.LIGHTNING;
        boolean canBarrel    = isAffordable(AbilityType.BARREL)    || selected == AbilityType.BARREL;
        lightningItem.setAffordable(canLightning);
        barrelItem.setAffordable(canBarrel);
    }

    public void update() {
        refreshVisual();
    }

    public boolean hasSelection() { return selected != null; }
    public AbilityType getSelected() { return selected; }
    public void clearSelection() {
        selected = null;
        applySelectVisual(lightningItem, false);
        applySelectVisual(barrelItem, false);
        refreshVisual();
    }

    public void dispose() {
        root.remove();
        costFont.dispose();
    }

    // ========= Inner helpers =========
    /** Table tự cập nhật origin về tâm khi size đổi để scale từ giữa. */
    private static class CenterOriginTable extends Table {
        @Override protected void sizeChanged() {
            setOrigin(getWidth() * 0.5f, getHeight() * 0.5f);
        }
    }

    private class AbilityItem {
        final CenterOriginTable root; // container có transform để scale từ TÂM
        final Image icon;
        final Label price;
        final AbilityType type;

        AbilityItem(Texture iconTex, AbilityType type, Runnable onClick) {
            this.type = type;

            // Icon
            Texture fallback = null;
            if (iconTex == null) {
                fallback = new Texture(Gdx.files.internal("libgdx.png"));
                fallback.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
            icon = new Image(new TextureRegion(new TextureRegion(iconTex != null ? iconTex : fallback)));
            icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);

            // Nhãn giá
            Label.LabelStyle st = new Label.LabelStyle(costFont, Color.valueOf("ffe96b"));
            price = new Label("0", st);
            price.setFontScale(0.85f);
            price.setAlignment(Align.center);

            // Layout theo cột (icon trên – giá dưới)
            Table col = new Table();
            col.add(icon).size(ICON_SIZE, ICON_SIZE).row();
            col.add(price).padTop(PRICE_TOP_PAD).center();

            root = new CenterOriginTable();
            root.setTransform(true); // cho phép scale/rotate
            root.add(col).center();

            // Click chọn/hủy
            root.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (onClick != null) onClick.run();
                }
            });
        }

        void setCost(int cost) { price.setText(String.valueOf(cost)); }

        void setAffordable(boolean canUse) {
            float a = canUse ? 1f : 0.35f;
            icon.setColor(1,1,1,a);
            price.setColor(price.getColor().r, price.getColor().g, price.getColor().b, a);
        }

        void setSelected(boolean sel) {
            root.clearActions();
            float targetScale = sel ? SELECT_SCALE : 1f;
            Color targetColor = sel ? new Color(1f, 0.98f, 0.65f, 1f) : Color.WHITE;
            root.addAction(Actions.parallel(
                Actions.scaleTo(targetScale, targetScale, SCALE_DUR),
                Actions.color(targetColor, SCALE_DUR)
            ));
        }
    }
}
