package game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
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

    // Custom cursor
    private Texture cursorTexture;
    private boolean useCustomCursor = true;

    // For cursor rendering
    private OrthographicCamera hudCamera;
    private Viewport hudViewport;

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
    private boolean cursorConfined = true;
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private float cursorX = 0;
    private float cursorY = 0;
    private boolean useVirtualCursor = true;


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
        Storage storage = Storage.getInstance();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, gameViewport.getWorldWidth(), gameViewport.getWorldHeight());
        viewport = new FitViewport(camera.viewportWidth, camera.viewportHeight, camera);

        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), hudCamera);
        hudCamera.setToOrtho(false, hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
        hudCamera.update();

        setupCustomCursor();

        batch = new SpriteBatch();
        stage = new Stage(viewport, batch);

        skin = storage.skin;
        font = storage.font;

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
            System.out.println("Cursor texture loaded: " + cursorTexture.getWidth() + "x" + cursorTexture.getHeight());
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

    public void updateCursorConfinement() {
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
        mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();

        classSelectionTable = new Table();
        classSelectionTable.setFillParent(true);
        classSelectionTable.center();
        classSelectionTable.setVisible(false);

        try {
            Texture bgTexture = Storage.assetManager.get("tiles/stoneFloor4.png", Texture.class);
            if (bgTexture != null) {
                backgroundImage = new Image(bgTexture);
                backgroundImage.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
                backgroundImage.setColor(new Color(0.2f, 0.2f, 0.2f, 0.7f));
                stage.addActor(backgroundImage);
                backgroundImage.toBack();
            }
        } catch (Exception e) {
            System.err.println("Background texture not available: " + e.getMessage());
        }

        try {
            Texture titleTex = Storage.assetManager.get("title.png", Texture.class);
            if (titleTex != null) {
                titleImage = new Image(titleTex);
                mainTable.add(titleImage).size(300, 100).padBottom(100).row();
            }
        } catch (Exception e) {
            System.err.println("Title texture not available: " + e.getMessage());
        }

        // Main menu buttons
        playButton = new TextButton("PLAY", skin);
        playButton.getLabel().setFontScale(1.5f); // Reduced from 2f
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showClassSelection();
            }
        });

        exitButton = new TextButton("EXIT", skin);
        exitButton.getLabel().setFontScale(1.5f); // Reduced from 2f
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
        selectClassLabel.setFontScale(1.2f); // Reduced from 1.5f
        classSelectionTable.add(selectClassLabel).padBottom(30).colspan(2).row();

        // Mercenary button
        mercenaryButton = new TextButton("MERCENARY", skin);
        mercenaryButton.getLabel().setFontScale(1.2f); // Reduced from 1.5f
        mercenaryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectClass(PlayerClass.MERCENARY);
            }
        });

        // Paladin button
        paladinButton = new TextButton("PALADIN", skin);
        paladinButton.getLabel().setFontScale(1.2f); // Reduced from 1.5f
        paladinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectClass(PlayerClass.PALADIN);
            }
        });

        Table classButtonsTable = new Table();
        classButtonsTable.add(mercenaryButton).size(200, 80).pad(10); // Reduced size
        classButtonsTable.add(paladinButton).size(200, 80).pad(10); // Reduced size
        classSelectionTable.add(classButtonsTable).colspan(2).row();

        // Class description label
        classDescriptionLabel = new Label(PlayerClass.MERCENARY.getDescription(), skin);
        classDescriptionLabel.setWrap(true);
        classSelectionTable.add(classDescriptionLabel).width(400).padTop(20).colspan(2).row();

        // Start game button
        TextButton startGameButton = new TextButton("START GAME", skin);
        startGameButton.getLabel().setFontScale(1.2f); // Reduced from 1.5f
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startGame();
            }
        });
        classSelectionTable.add(startGameButton).size(250, 70).padTop(20).colspan(2).row();

        // Back button
        backButton = new TextButton("BACK", skin);
        backButton.getLabel().setFontScale(1.0f); // Reduced from 1.2f
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showMainMenu();
            }
        });
        classSelectionTable.add(backButton).size(150, 50).padTop(10).colspan(2).row();

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

        // Reset cursor state for gameplay (will be handled by GameProj)
        Gdx.input.setCursorCatched(true);
        gameScreen.switchToNewState(GameScreen.HOME);
    }

    public void render(float delta) {
        if (isDisposed) return;

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        updateCursorConfinement();

        // Render custom cursor on top of everything
        if (useCustomCursor && cursorTexture != null) {
            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();

            // Get mouse coordinates - already in screen space
            float cursorX = Gdx.input.getX();
            float cursorY = Gdx.graphics.getHeight() - Gdx.input.getY();

            // Debug: print cursor position
            // System.out.println("Cursor: " + cursorX + ", " + cursorY);

            // Draw custom cursor - match the size from GameProj
            float cursorSize = 32;
            float offsetX = cursorSize / 4f; // Match GameProj offset
            float offsetY = cursorSize / 3f; // Match GameProj offset

            batch.draw(cursorTexture,
                    cursorX - offsetX,
                    cursorY - offsetY,
                    cursorSize, cursorSize);
            batch.end();
        }
    }

    public void resize(int width, int height) {
        if (isDisposed) return;

        viewport.update(width, height, true);
        camera.update();
        hudViewport.update(width, height, true);
        hudCamera.update();

        if (mainTable != null) {
            mainTable.invalidateHierarchy();
        }
        if (classSelectionTable != null) {
            classSelectionTable.invalidateHierarchy();
        }

        // Update background image size if it exists
        if (backgroundImage != null) {
            backgroundImage.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
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
            // Dispose cursor texture if it was created as fallback
            if (cursorTexture != null) {
                // Only dispose if it's not from asset manager
                try {
                    // Check if this is the fallback texture by seeing if asset manager has it
                    Texture assetManagerTex = Storage.assetManager.get("mouse.png", Texture.class);
                    if (assetManagerTex != cursorTexture) {
                        cursorTexture.dispose();
                    }
                } catch (Exception e) {
                    // If not in asset manager, dispose it
                    cursorTexture.dispose();
                }
                cursorTexture = null;
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

        try {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        } catch (Exception e) {
        }
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