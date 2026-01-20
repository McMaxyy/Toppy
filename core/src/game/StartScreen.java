package game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import config.GameScreen;
import config.Storage;

public class StartScreen {
    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;
    private SpriteBatch batch;
    private Skin skin;
    private BitmapFont font;

    private Game game;
    private GameScreen gameScreen;

    // UI elements
    private Table mainTable;
    private TextButton playButton;
    private TextButton exitButton;
    private Image titleImage;
    private Image backgroundImage;

    // Track if already disposed
    private boolean isDisposed = false;

    public StartScreen(Viewport gameViewport, Game game, GameScreen gameScreen) {
        this.game = game;
        this.gameScreen = gameScreen;

        // Initialize camera and viewport
        camera = new OrthographicCamera();
        camera.setToOrtho(false, gameViewport.getWorldWidth(), gameViewport.getWorldHeight());
        viewport = new FitViewport(camera.viewportWidth, camera.viewportHeight, camera);

        // Initialize rendering
        batch = new SpriteBatch();
        stage = new Stage(viewport, batch);

        // Get skin from Storage
        skin = Storage.getInstance().skin;
        font = Storage.getInstance().font;

        // Create UI
        createUI();

        // Set this stage as input processor
        Gdx.input.setInputProcessor(stage);
    }

    private void createUI() {
        // Create main table that fills the screen
        mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();

        // Try to add a background image if available
        try {
            Texture bgTexture = Storage.assetManager.get("tiles/stoneFloor4.png", Texture.class);
            if (bgTexture != null) {
                backgroundImage = new Image(bgTexture);
                backgroundImage.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
                backgroundImage.setColor(new Color(0.2f, 0.2f, 0.2f, 0.7f)); // Darken it
                stage.addActor(backgroundImage);
                backgroundImage.toBack();
            }
        } catch (Exception e) {
            // Background texture not available, that's fine
        }

        try {
            Texture titleTex = Storage.assetManager.get("title.png", Texture.class);
            if (titleTex != null) {
                titleImage = new Image(titleTex);
                mainTable.add(titleImage).size(300, 100).padBottom(100).row();
            }
        } catch (Exception e) {
            // Title texture not available, skip it
        }

        playButton = new TextButton("PLAY", skin);
        playButton.getLabel().setFontScale(2f);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Let GameScreen handle the disposal - don't dispose here!
                gameScreen.switchToNewState(GameScreen.HOME);
            }
        });

        exitButton = new TextButton("EXIT", skin);
        exitButton.getLabel().setFontScale(2f);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        mainTable.add(playButton).size(300, 80).padBottom(40).row();
        mainTable.add(exitButton).size(300, 80).row();

        stage.addActor(mainTable);

        addDecorations();
    }

    private void addDecorations() {
        // Add some decorative text or images if desired
        // For example, add a version number or game title
    }

    public void render(float delta) {
        if (isDisposed) return;

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (font != null) {
            font.getData().setScale(3f);
            font.setColor(Color.WHITE);
            font.draw(batch, "DUNGEON ADVENTURE",
                    viewport.getWorldWidth() / 2 - 150,
                    viewport.getWorldHeight() - 50);
            font.getData().setScale(1f);
        }
        batch.end();
    }

    public void resize(int width, int height) {
        if (isDisposed) return;

        viewport.update(width, height, true);
        camera.update();
        if (mainTable != null) {
            mainTable.invalidateHierarchy();
        }
    }

    public void dispose() {
        if (isDisposed) return;
        isDisposed = true;

        try {
            // Clear the stage first
            if (stage != null) {
                stage.clear();
                stage.dispose();
                stage = null;
            }
            // Dispose batch after stage (since stage uses the batch)
            if (batch != null) {
                batch.dispose();
                batch = null;
            }
        } catch (Exception e) {
            System.err.println("Error disposing StartScreen: " + e.getMessage());
        }

        // Clear references
        mainTable = null;
        playButton = null;
        exitButton = null;
        titleImage = null;
        backgroundImage = null;
    }

    public Stage getStage() {
        return stage;
    }

    public boolean isDisposed() {
        return isDisposed;
    }
}