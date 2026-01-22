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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import config.GameScreen;
import config.Storage;
import entities.PlayerClass;

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
    private Table classSelectionTable;
    private TextButton playButton;
    private TextButton exitButton;
    private TextButton mercenaryButton;
    private TextButton paladinButton;
    private TextButton backButton;
    private Label classDescriptionLabel;
    private Image titleImage;
    private Image backgroundImage;

    // Selected player class
    private PlayerClass selectedClass = PlayerClass.MERCENARY;

    // Screen states
    private enum ScreenState {
        MAIN_MENU,
        CLASS_SELECTION
    }
    private ScreenState currentState = ScreenState.MAIN_MENU;

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

        // Create class selection table
        classSelectionTable = new Table();
        classSelectionTable.setFillParent(true);
        classSelectionTable.center();
        classSelectionTable.setVisible(false);

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

        // Main menu buttons
        playButton = new TextButton("PLAY", skin);
        playButton.getLabel().setFontScale(2f);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showClassSelection();
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

        // Class selection UI
        Label selectClassLabel = new Label("SELECT YOUR CLASS", skin);
        selectClassLabel.setFontScale(1.5f);
        classSelectionTable.add(selectClassLabel).padBottom(50).colspan(2).row();

        // Mercenary button
        mercenaryButton = new TextButton("MERCENARY", skin);
        mercenaryButton.getLabel().setFontScale(1.5f);
        mercenaryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectClass(PlayerClass.MERCENARY);
            }
        });

        // Paladin button
        paladinButton = new TextButton("PALADIN", skin);
        paladinButton.getLabel().setFontScale(1.5f);
        paladinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectClass(PlayerClass.PALADIN);
            }
        });

        Table classButtonsTable = new Table();
        classButtonsTable.add(mercenaryButton).size(200, 100).pad(20);
        classButtonsTable.add(paladinButton).size(200, 100).pad(20);
        classSelectionTable.add(classButtonsTable).colspan(2).row();

        // Class description label
        classDescriptionLabel = new Label(PlayerClass.MERCENARY.getDescription(), skin);
        classDescriptionLabel.setWrap(true);
        classSelectionTable.add(classDescriptionLabel).width(400).padTop(30).colspan(2).row();

        // Start game button
        TextButton startGameButton = new TextButton("START GAME", skin);
        startGameButton.getLabel().setFontScale(1.5f);
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startGame();
            }
        });
        classSelectionTable.add(startGameButton).size(250, 70).padTop(40).colspan(2).row();

        // Back button
        backButton = new TextButton("BACK", skin);
        backButton.getLabel().setFontScale(1.2f);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showMainMenu();
            }
        });
        classSelectionTable.add(backButton).size(150, 50).padTop(20).colspan(2).row();

        stage.addActor(mainTable);
        stage.addActor(classSelectionTable);

        // Highlight default selection
        updateClassSelection();
    }

    private void showClassSelection() {
        currentState = ScreenState.CLASS_SELECTION;
        mainTable.setVisible(false);
        classSelectionTable.setVisible(true);
    }

    private void showMainMenu() {
        currentState = ScreenState.MAIN_MENU;
        mainTable.setVisible(true);
        classSelectionTable.setVisible(false);
    }

    private void selectClass(PlayerClass playerClass) {
        selectedClass = playerClass;
        classDescriptionLabel.setText(playerClass.getDescription());
        updateClassSelection();
    }

    private void updateClassSelection() {
        // Update button colors to show selection
        if (selectedClass == PlayerClass.MERCENARY) {
            mercenaryButton.setColor(Color.GOLD);
            paladinButton.setColor(Color.WHITE);
        } else {
            mercenaryButton.setColor(Color.WHITE);
            paladinButton.setColor(Color.GOLD);
        }
    }

    private void startGame() {
        // Store the selected class in Storage for GameProj to access
        Storage.setSelectedPlayerClass(selectedClass);
        gameScreen.switchToNewState(GameScreen.HOME);
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
        if (classSelectionTable != null) {
            classSelectionTable.invalidateHierarchy();
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
        classSelectionTable = null;
        playButton = null;
        exitButton = null;
        mercenaryButton = null;
        paladinButton = null;
        backButton = null;
        classDescriptionLabel = null;
        titleImage = null;
        backgroundImage = null;
    }

    public Stage getStage() {
        return stage;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public PlayerClass getSelectedClass() {
        return selectedClass;
    }
}