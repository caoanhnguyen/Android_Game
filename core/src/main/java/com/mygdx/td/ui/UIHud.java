package com.mygdx.td.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygdx.td.Assets;
import com.mygdx.td.TDGame;
import com.mygdx.td.entities.Enemy;
import com.mygdx.td.entities.Tower;
import com.mygdx.td.world.World;

public class UIHud {
    private final TDGame game;
    private final World world;
    private final Stage stage;
    private final Skin skin;

    private final Label waveLabel;
    private final Label goldLabel;
    private final Label livesLabel;
    private final Label statusLabel;
    private final TextButton startButton;

    // Panels đơn giản (tower/enemy)
    private final Table towerPanel;
    private final Label towerInfo;
    private Tower selectedTower;

    private final Table enemyPanel;
    private final Label enemyInfo;
    private Enemy selectedEnemy;

    public Runnable onStartWave;

    public UIHud(TDGame game, World world) {
        this.game = game;
        this.world = world;
        this.stage = new Stage(new ScreenViewport());
        this.skin = buildBasicSkin(game.assets);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        waveLabel  = new Label("Wave:0", skin);
        goldLabel  = new Label("Gold:0", skin);
        livesLabel = new Label("Lives:0", skin);
        statusLabel= new Label("WAIT", skin);
        startButton= new TextButton("START", skin);

        startButton.addListener(e -> {
            if (startButton.isPressed() && onStartWave != null) onStartWave.run();
            return false;
        });

        root.top().left();
        root.add(waveLabel).pad(4);
        root.add(goldLabel).pad(4);
        root.add(livesLabel).pad(4);
        root.add(statusLabel).pad(4);
        root.add(startButton).pad(4);

        // Tower panel
        towerPanel = new Table(skin);
        towerPanel.setBackground(new TextureRegionDrawable(game.assets.getWhiteRegion()).tint(Color.valueOf("1e2c3aAA")));
        towerInfo = new Label("No tower", skin);
        towerPanel.add(new Label("TOWER", skin)).padBottom(4);
        towerPanel.row();
        towerPanel.add(towerInfo);
        towerPanel.pack();
        towerPanel.setVisible(false);

        // Enemy panel
        enemyPanel = new Table(skin);
        enemyPanel.setBackground(new TextureRegionDrawable(game.assets.getWhiteRegion()).tint(Color.valueOf("3a1e2cAA")));
        enemyInfo = new Label("No enemy", skin);
        enemyPanel.add(new Label("ENEMY", skin)).padBottom(4);
        enemyPanel.row();
        enemyPanel.add(enemyInfo);
        enemyPanel.pack();
        enemyPanel.setVisible(false);

        Table bottomRight = new Table();
        bottomRight.setFillParent(true);
        bottomRight.bottom().right();
        bottomRight.add(towerPanel).pad(6);
        bottomRight.row();
        bottomRight.add(enemyPanel).pad(6);
        stage.addActor(bottomRight);
    }

    private Skin buildBasicSkin(Assets assets) {
        Skin s = new Skin();
        s.add("default-font", assets.fontSmall);
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = assets.fontSmall;
        ls.fontColor = Color.WHITE;
        s.add("default", ls);

        TextButton.TextButtonStyle bs = new TextButton.TextButtonStyle();
        bs.font = assets.fontSmall;
        bs.up   = new TextureRegionDrawable(assets.getWhiteRegion()).tint(Color.valueOf("244c6eFF"));
        bs.over = new TextureRegionDrawable(assets.getWhiteRegion()).tint(Color.valueOf("2d6894FF"));
        bs.down = new TextureRegionDrawable(assets.getWhiteRegion()).tint(Color.valueOf("163042FF"));
        s.add("default", bs);

        return s;
    }

    public Stage getStage() { return stage; }

    public void updateHudValues(boolean inWave) {
        waveLabel.setText("Wave:" + world.waveManager.getCurrentWave());
        goldLabel.setText("Gold:" + world.gold);
        livesLabel.setText("Lives:" + world.lives);
        statusLabel.setText(inWave ? "RUN" : "WAIT");
        startButton.setDisabled(inWave || world.gameOver);
    }

    public void selectTower(Tower t) {
        selectedTower = t;
        if (t == null) {
            towerPanel.setVisible(false);
        } else {
            towerInfo.setText(String.format("Pos(%.0f,%.0f)", t.pos.x, t.pos.y));
            towerPanel.setVisible(true);
            // bỏ enemy panel nếu mở
            selectedEnemy = null;
            enemyPanel.setVisible(false);
        }
    }

    public void selectEnemy(Enemy e) {
        selectedEnemy = e;
        if (e == null) {
            enemyPanel.setVisible(false);
        } else {
            enemyInfo.setText(String.format("HP %.0f/%.0f", e.hp, e.maxHp));
            enemyPanel.setVisible(true);
            selectedTower = null;
            towerPanel.setVisible(false);
        }
    }

    public void act(float dt) {
        if (selectedEnemy != null && !selectedEnemy.dead) {
            enemyInfo.setText(String.format("HP %.0f/%.0f", selectedEnemy.hp, selectedEnemy.maxHp));
        }
        stage.act(dt);
    }

    public void draw() { stage.draw(); }

    public void dispose() { stage.dispose(); }
}
