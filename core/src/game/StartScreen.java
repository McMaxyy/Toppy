package game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
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
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import config.GameScreen;
import config.Storage;
import entities.PlayerClass;

public class StartScreen extends Game{
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
    private Table settingsTable;
    private TextButton playButton;
    private TextButton exitButton;
    private TextButton settingsButton;
    private TextButton mercenaryButton;
    private TextButton paladinButton;
    private TextButton backButton;
    private TextButton startGameButton;
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
        CLASS_SELECTION,
        SETTINGS
    }
    private ScreenState currentState = ScreenState.MAIN_MENU;

    // Settings
    private CheckBox fullscreenCheckbox;
    private CheckBox windowedCheckbox;
    private CheckBox size1280x720Checkbox;
    private CheckBox size1600x900Checkbox;
    private CheckBox size1920x1080Checkbox;

    private boolean isFullscreen = true;
    private int selectedWidth = 1920;
    private int selectedHeight = 1080;

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
        BitmapFont font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        font.setColor(Color.WHITE);

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
        mainTable.left();

        classSelectionTable = new Table();
        classSelectionTable.setFillParent(true);
        classSelectionTable.center();
        classSelectionTable.setVisible(false);

        settingsTable = new Table();
        settingsTable.setFillParent(true);
        settingsTable.center();
        settingsTable.setVisible(false);

        try {
            Texture bgTexture = Storage.assetManager.get("MainMenu.png", Texture.class);
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
                mainTable.add(titleImage).size(600, 150).padBottom(100).row();
            }
        } catch (Exception e) {
            System.err.println("Title texture not available: " + e.getMessage());
        }

        playButton = new TextButton("PLAY", skin);
        playButton.getLabel().setFontScale(1.5f);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showClassSelection();
            }
        });

        settingsButton = new TextButton("SETTINGS", skin);
        settingsButton.getLabel().setFontScale(1.5f);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showSettings();
            }
        });

        exitButton = new TextButton("EXIT", skin);
        exitButton.getLabel().setFontScale(1.5f);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        mainTable.add(playButton).size(300, 80).padBottom(20).row();
        mainTable.add(settingsButton).size(300, 80).padBottom(20).row();
        mainTable.add(exitButton).size(300, 80).row();

        createClassSelectionUI();

        createSettingsUI();

        stage.addActor(mainTable);
        stage.addActor(classSelectionTable);
        stage.addActor(settingsTable);

        updateClassSelection();
    }

    private void createClassSelectionUI() {
        Label selectClassLabel = new Label("SELECT YOUR CLASS", skin);
        selectClassLabel.setFontScale(1.2f);
        classSelectionTable.add(selectClassLabel).padBottom(30).colspan(2).row();

        // Mercenary button
        mercenaryButton = new TextButton("MERCENARY", skin);
        mercenaryButton.getLabel().setFontScale(1.2f);
        mercenaryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectClass(PlayerClass.MERCENARY);
            }
        });

        // Paladin button
        paladinButton = new TextButton("PALADIN", skin);
        paladinButton.getLabel().setFontScale(1.2f);
        paladinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectClass(PlayerClass.PALADIN);
            }
        });

        Table classButtonsTable = new Table();
        classButtonsTable.add(mercenaryButton).size(200, 80).pad(10);
        classButtonsTable.add(paladinButton).size(200, 80).pad(10);
        classSelectionTable.add(classButtonsTable).colspan(2).row();

        // Class description label
        classDescriptionLabel = new Label(PlayerClass.MERCENARY.getDescription(), skin);
        classDescriptionLabel.setWrap(true);
        classSelectionTable.add(classDescriptionLabel).width(400).padTop(20).colspan(2).row();

        // Start game button
        startGameButton = new TextButton("START GAME", skin);
        startGameButton.getLabel().setFontScale(1.2f);
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startGame();
            }
        });
        classSelectionTable.add(startGameButton).size(250, 70).padTop(20).colspan(2).row();

        backButton = new TextButton("BACK", skin);
        backButton.getLabel().setFontScale(1.0f);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showMainMenu();
            }
        });
        classSelectionTable.add(backButton).size(150, 50).padTop(10).colspan(2).row();
    }

    private void createSettingsUI() {
        Label settingsLabel = new Label("SETTINGS", skin);
        settingsLabel.setFontScale(1.5f);
        settingsTable.add(settingsLabel).padBottom(30).colspan(2).row();

        Label windowModeLabel = new Label("WINDOW MODE:", skin);
        windowModeLabel.setFontScale(1.0f);
        settingsTable.add(windowModeLabel).left().padBottom(15).colspan(2).row();

        fullscreenCheckbox = new CheckBox(" Fullscreen", skin);
        fullscreenCheckbox.setChecked(true);
        fullscreenCheckbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (fullscreenCheckbox.isChecked()) {
                    windowedCheckbox.setChecked(false);
                    isFullscreen = true;
                } else {
                    if (!windowedCheckbox.isChecked()) {
                        fullscreenCheckbox.setChecked(true);
                    }
                }
            }
        });

        windowedCheckbox = new CheckBox(" Windowed", skin);
        windowedCheckbox.setChecked(false);
        windowedCheckbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (windowedCheckbox.isChecked()) {
                    fullscreenCheckbox.setChecked(false);
                    isFullscreen = false;
                } else {
                    if (!fullscreenCheckbox.isChecked()) {
                        windowedCheckbox.setChecked(true);
                    }
                }
            }
        });

        Table modeTable = new Table();
        modeTable.add(fullscreenCheckbox).padRight(20);
        modeTable.add(windowedCheckbox);
        settingsTable.add(modeTable).padBottom(30).colspan(2).row();

        Label resolutionLabel = new Label("RESOLUTION:", skin);
        resolutionLabel.setFontScale(1.0f);
        settingsTable.add(resolutionLabel).left().padBottom(15).colspan(2).row();

        size1280x720Checkbox = new CheckBox(" 1280x720", skin);
        size1280x720Checkbox.setChecked(false);
        size1280x720Checkbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (size1280x720Checkbox.isChecked()) {
                    size1600x900Checkbox.setChecked(false);
                    size1920x1080Checkbox.setChecked(false);
                    selectedWidth = 1280;
                    selectedHeight = 720;
                } else {
                    if (!size1600x900Checkbox.isChecked() && !size1920x1080Checkbox.isChecked()) {
                        size1280x720Checkbox.setChecked(true);
                    }
                }
            }
        });

        size1600x900Checkbox = new CheckBox(" 1600x900", skin);
        size1600x900Checkbox.setChecked(false);
        size1600x900Checkbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (size1600x900Checkbox.isChecked()) {
                    size1280x720Checkbox.setChecked(false);
                    size1920x1080Checkbox.setChecked(false);
                    selectedWidth = 1600;
                    selectedHeight = 900;
                } else {
                    // Ensure at least one is checked
                    if (!size1280x720Checkbox.isChecked() && !size1920x1080Checkbox.isChecked()) {
                        size1600x900Checkbox.setChecked(true);
                    }
                }
            }
        });

        size1920x1080Checkbox = new CheckBox(" 1920x1080", skin);
        size1920x1080Checkbox.setChecked(true);
        size1920x1080Checkbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (size1920x1080Checkbox.isChecked()) {
                    size1280x720Checkbox.setChecked(false);
                    size1600x900Checkbox.setChecked(false);
                    selectedWidth = 1920;
                    selectedHeight = 1080;
                } else {
                    // Ensure at least one is checked
                    if (!size1280x720Checkbox.isChecked() && !size1600x900Checkbox.isChecked()) {
                        size1920x1080Checkbox.setChecked(true);
                    }
                }
            }
        });

        Table resolutionTable = new Table();
        resolutionTable.add(size1280x720Checkbox).padRight(10);
        resolutionTable.add(size1600x900Checkbox).padRight(10);
        resolutionTable.add(size1920x1080Checkbox);
        settingsTable.add(resolutionTable).padBottom(40).colspan(2).row();

        TextButton applyButton = new TextButton("APPLY", skin);
        applyButton.getLabel().setFontScale(1.0f);
        applyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                applySettings();
            }
        });

        TextButton settingsBackButton = new TextButton("BACK", skin);
        settingsBackButton.getLabel().setFontScale(1.0f);
        settingsBackButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showMainMenu();
            }
        });

        Table settingsButtonsTable = new Table();
        settingsButtonsTable.add(applyButton).size(150, 50).padRight(20);
        settingsButtonsTable.add(settingsBackButton).size(150, 50);
        settingsTable.add(settingsButtonsTable).colspan(2).row();
    }

    private void applySettings() {
        showMainMenu();
    }

    private void showClassSelection() {
        currentState = ScreenState.CLASS_SELECTION;
        mainTable.setVisible(false);
        classSelectionTable.setVisible(true);
        settingsTable.setVisible(false);
    }

    private void showSettings() {
        currentState = ScreenState.SETTINGS;
        mainTable.setVisible(false);
        classSelectionTable.setVisible(false);
        settingsTable.setVisible(true);
    }

    private void showMainMenu() {
        currentState = ScreenState.MAIN_MENU;
        mainTable.setVisible(true);
        classSelectionTable.setVisible(false);
        settingsTable.setVisible(false);
    }

    private void selectClass(PlayerClass playerClass) {
        selectedClass = playerClass;
        classDescriptionLabel.setText(playerClass.getDescription());
        updateClassSelection();
    }

    private void updateClassSelection() {
        if (selectedClass == PlayerClass.MERCENARY) {
            mercenaryButton.setColor(Color.GOLD);
            paladinButton.setColor(Color.WHITE);
        } else {
            mercenaryButton.setColor(Color.WHITE);
            paladinButton.setColor(Color.GOLD);
        }
    }

    private void startGame() {
        Storage.setSelectedPlayerClass(selectedClass);

        Gdx.input.setCursorCatched(false);
        setScreen(new GameScreen(this, selectedWidth, selectedHeight, isFullscreen));
        gameScreen.switchToNewState(GameScreen.HOME);
    }

    public void render(float delta) {
        if (isDisposed) return;

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
    public void create() {

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
        if (settingsTable != null) {
            settingsTable.invalidateHierarchy();
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
            if (stage != null) {
                stage.clear();
                stage.dispose();
                stage = null;
            }
            if (batch != null) {
                batch.dispose();
                batch = null;
            }
            if (cursorTexture != null) {
                try {
                    Texture assetManagerTex = Storage.assetManager.get("mouse.png", Texture.class);
                    if (assetManagerTex != cursorTexture) {
                        cursorTexture.dispose();
                    }
                } catch (Exception e) {
                    cursorTexture.dispose();
                }
                cursorTexture = null;
            }
        } catch (Exception e) {
            System.err.println("Error disposing StartScreen: " + e.getMessage());
        }

        mainTable = null;
        classSelectionTable = null;
        settingsTable = null;
        playButton = null;
        settingsButton = null;
        exitButton = null;
        mercenaryButton = null;
        paladinButton = null;
        backButton = null;
        startGameButton = null;
        classDescriptionLabel = null;
        titleImage = null;
        backgroundImage = null;
        fullscreenCheckbox = null;
        windowedCheckbox = null;
        size1280x720Checkbox = null;
        size1600x900Checkbox = null;
        size1920x1080Checkbox = null;

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