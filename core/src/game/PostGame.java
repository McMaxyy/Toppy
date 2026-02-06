package game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import config.GameScreen;
import config.Storage;

public class PostGame implements Screen {
    private final GameScreen gameScreen;
    public final Stage stage;
    private final Skin skin;
    private final SpriteBatch batch;
    private final BitmapFont titleFont;
    private final BitmapFont statsFont;

    private final float totalTimeSurvived;
    private final float endlessTimeSurvived;
    private final int playerLevel;
    private final int endlessEnemiesKilled;

    private Texture backgroundTexture;

    private Texture cursorTexture;
    private boolean useCustomCursor = true;
    private OrthographicCamera hudCamera;
    private FitViewport hudViewport;
    private boolean cursorConfined = true;
    private float cursorX = 0;
    private float cursorY = 0;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public PostGame(GameScreen gameScreen, float totalTimeSurvived, float endlessTimeSurvived,
                    int playerLevel, int endlessEnemiesKilled) {
        this.gameScreen = gameScreen;
        this.totalTimeSurvived = totalTimeSurvived;
        this.endlessTimeSurvived = endlessTimeSurvived;
        this.playerLevel = playerLevel;
        this.endlessEnemiesKilled = endlessEnemiesKilled;

        batch = new SpriteBatch();
        FitViewport viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage = new Stage(viewport, batch);

        // Setup HUD camera for cursor
        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), hudCamera);
        hudCamera.setToOrtho(false, hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
        hudCamera.update();

        Storage storage = Storage.getInstance();
        skin = storage.skin;

        titleFont = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        statsFont = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);

        try {
            backgroundTexture = Storage.assetManager.get("MainMenu.png", Texture.class);
        } catch (Exception e) {
            backgroundTexture = null;
        }

        setupCustomCursor();

        createUI();
        Gdx.input.setInputProcessor(stage);
    }

    private void setupCustomCursor() {
        try {
            cursorTexture = Storage.assetManager.get("mouse.png", Texture.class);
            if (cursorTexture == null) {
                throw new RuntimeException("Cursor texture is null");
            }
            useCustomCursor = true;
        } catch (Exception e) {
            System.err.println("Failed to load cursor texture: " + e.getMessage());
            useCustomCursor = false;

            // Create a fallback cursor
            Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fillCircle(16, 16, 8);
            pm.setColor(Color.RED);
            pm.fillCircle(16, 16, 4);
            cursorTexture = new Texture(pm);
            pm.dispose();
            useCustomCursor = true;
        }

        try {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0, 0, 0, 0); // Transparent
            pixmap.fill();
            Cursor emptyCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
            Gdx.graphics.setCursor(emptyCursor);
            pixmap.dispose();
        } catch (Exception e) {
            System.err.println("Failed to create empty cursor: " + e.getMessage());
        }

        Gdx.input.setCursorCatched(true);
        confineCursorToWindow();
    }

    private void confineCursorToWindow() {
        try {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0, 0, 0, 0);
            pixmap.fill();
            Cursor emptyCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
            Gdx.graphics.setCursor(emptyCursor);
            pixmap.dispose();
        } catch (Exception e) {
            System.err.println("Failed to create empty cursor: " + e.getMessage());
        }

        int centerX = Gdx.graphics.getWidth() / 2;
        int centerY = Gdx.graphics.getHeight() / 2;
        Gdx.input.setCursorPosition(centerX, centerY);

        cursorX = centerX;
        cursorY = centerY;
        lastMouseX = centerX;
        lastMouseY = centerY;

        Gdx.input.setCursorCatched(true);
    }

    private void updateCursorConfinement() {
        if (!cursorConfined) return;

        int deltaX = Gdx.input.getDeltaX();
        int deltaY = Gdx.input.getDeltaY();

        cursorX += deltaX;
        cursorY -= deltaY;

        int windowWidth = Gdx.graphics.getWidth();
        int windowHeight = Gdx.graphics.getHeight();

        int borderMargin = 2;

        cursorX = Math.max(borderMargin, Math.min(cursorX, windowWidth - borderMargin));
        cursorY = Math.max(borderMargin, Math.min(cursorY, windowHeight - borderMargin));

        lastMouseX = (int)cursorX;
        lastMouseY = (int)cursorY;

        if (cursorX <= borderMargin || cursorX >= windowWidth - borderMargin ||
                cursorY <= borderMargin || cursorY >= windowHeight - borderMargin) {

            Gdx.input.setCursorPosition((int)cursorX, windowHeight - (int)cursorY);
        }

        int currentMouseX = Gdx.input.getX();
        int currentMouseY = Gdx.input.getY();
        int maxDelta = 50;

        if (Math.abs(currentMouseX - cursorX) > maxDelta ||
                Math.abs(windowHeight - currentMouseY - cursorY) > maxDelta) {

            cursorX = currentMouseX;
            cursorY = windowHeight - currentMouseY;

            cursorX = Math.max(borderMargin, Math.min(cursorX, windowWidth - borderMargin));
            cursorY = Math.max(borderMargin, Math.min(cursorY, windowHeight - borderMargin));

            Gdx.input.setCursorPosition((int)cursorX, windowHeight - (int)cursorY);
        }
    }

    private void createUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label titleLabel = new Label("Game Over", titleStyle);
        titleLabel.setFontScale(1.5f);
        mainTable.add(titleLabel).padBottom(50).row();

        Table statsTable = new Table();
        statsTable.defaults().padBottom(20).padLeft(20).padRight(20);

        String totalTimeText = formatTime(totalTimeSurvived);
        Label totalTimeLabel = createStatLabel("Total Time Played: " + totalTimeText);
        statsTable.add(totalTimeLabel).row();

        String endlessTimeText = formatTime(endlessTimeSurvived);
        Label endlessTimeLabel = createStatLabel("Endless Dungeon Time: " + endlessTimeText);
        statsTable.add(endlessTimeLabel).row();

        Label levelLabel = createStatLabel("Final Level: " + playerLevel);
        statsTable.add(levelLabel).row();

        Label killsLabel = createStatLabel("Endless Enemies Killed: " + endlessEnemiesKilled);
        statsTable.add(killsLabel).row();

        mainTable.add(statsTable).padBottom(50).row();

        TextButton returnButton = new TextButton("Return to Menu", skin);
        returnButton.getLabel().setFontScale(1.2f);
        returnButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameScreen.switchToNewState(GameScreen.START);
            }
        });

        mainTable.add(returnButton).width(300).height(60).row();
    }

    private Label createStatLabel(String text) {
        Label.LabelStyle statStyle = new Label.LabelStyle(statsFont, Color.WHITE);
        Label label = new Label(text, statStyle);
        label.setFontScale(0.8f);
        return label;
    }

    private String formatTime(float timeInSeconds) {
        int hours = (int) (timeInSeconds / 3600);
        int minutes = (int) ((timeInSeconds % 3600) / 60);
        int seconds = (int) (timeInSeconds % 60);

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        if (backgroundTexture != null) {
            batch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
            batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setColor(1f, 1f, 1f, 1f);
        }

        batch.end();

        stage.act(delta);
        stage.draw();

        updateCursorConfinement();

        if (useCustomCursor && cursorTexture != null) {
            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();

            float cursorX = Gdx.input.getX();
            float cursorY = Gdx.graphics.getHeight() - Gdx.input.getY();

            float cursorSize = 32;
            float offsetX = cursorSize / 4f;
            float offsetY = cursorSize / 3f;

            batch.draw(cursorTexture,
                    cursorX - offsetX,
                    cursorY - offsetY,
                    cursorSize, cursorSize);
            batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        hudViewport.update(width, height, true);
        hudCamera.update();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (batch != null) {
            batch.dispose();
        }

        cursorTexture = null;

        try {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        } catch (Exception e) {
        }
    }
}