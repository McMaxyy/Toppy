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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import config.GameScreen;
import config.Storage;
import entities.PlayerClass;
import managers.SoundManager;
import config.SaveManager;

public class StartScreen extends Game {
    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;
    private SpriteBatch batch;
    private Skin skin;
    private BitmapFont font;

    // Custom cursor
    private Texture cursorTexture;
    private boolean useCustomCursor = true;

    private OrthographicCamera hudCamera;
    private Viewport hudViewport;

    private Game game;
    private GameScreen gameScreen;

    private Table mainTable;
    private Table classSelectionTable;
    private Table settingsTable;
    private Table gameModeTable;
    private TextButton endlessButton;
    private TextButton normalButton;
    private TextButton gameModeBackButton;
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
    private TextButton displayTabButton;
    private TextButton audioTabButton;
    private Table displaySettingsTable;
    private Table audioSettingsTable;
    private enum SettingsTab { DISPLAY, AUDIO }
    private SettingsTab currentSettingsTab = SettingsTab.DISPLAY;
    private Slider musicSlider;
    private Slider sfxSlider;
    private CheckBox musicEnabledCheckbox;
    private CheckBox sfxEnabledCheckbox;
    private Label musicPercentLabel;
    private Label sfxPercentLabel;
    private PlayerClass selectedClass = PlayerClass.MERCENARY;

    // Screen states
    private enum ScreenState {
        MAIN_MENU,
        GAME_MODE_SELECTION,
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

        isFullscreen = SaveManager.isFullscreen();
        selectedWidth = SaveManager.getWindowWidth();
        selectedHeight = SaveManager.getWindowHeight();

        createUI();

        Gdx.input.setInputProcessor(stage);
    }

    private ClickListener createButtonListener(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                SoundManager.getInstance().playButtonSound();
                if (action != null) {
                    action.run();
                }
            }
        };
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
            pixmap.setColor(0, 0, 0, 0);
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

        gameModeTable = new Table();
        gameModeTable.setFillParent(true);
        gameModeTable.center();
        gameModeTable.setVisible(false);

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
        playButton.getLabel().setFontScale(1f);
        playButton.addListener(createButtonListener(this::showGameModeSelection));

        settingsButton = new TextButton("SETTINGS", skin);
        settingsButton.getLabel().setFontScale(1f);
        settingsButton.addListener(createButtonListener(this::showSettings));

        exitButton = new TextButton("EXIT", skin);
        exitButton.getLabel().setFontScale(1f);
        exitButton.addListener(createButtonListener(() -> Gdx.app.exit()));

        mainTable.add(playButton).size(300, 100).padBottom(20).row();
        mainTable.add(settingsButton).size(300, 100).padBottom(20).row();
        mainTable.add(exitButton).size(300, 100).row();

        createClassSelectionUI();
        createSettingsUI();

        stage.addActor(mainTable);
        stage.addActor(classSelectionTable);
        stage.addActor(settingsTable);
        stage.addActor(gameModeTable);

        updateClassSelection();
        createGameModeUI();
    }

    private void showGameModeSelection() {
        currentState = ScreenState.GAME_MODE_SELECTION;
        mainTable.setVisible(false);
        gameModeTable.setVisible(true);
        classSelectionTable.setVisible(false);
        settingsTable.setVisible(false);
    }

    private void createGameModeUI() {
        Label selectModeLabel = new Label("SELECT GAME MODE", skin);
        selectModeLabel.setFontScale(1f);
        gameModeTable.add(selectModeLabel).padBottom(50).colspan(2).row();

        endlessButton = new TextButton("ENDLESS", skin);
        endlessButton.getLabel().setFontScale(1f);
        endlessButton.addListener(createButtonListener(() -> {
            GameScreen.setGameMode(0);
            showClassSelection();
        }));

        normalButton = new TextButton("NORMAL", skin);
        normalButton.getLabel().setFontScale(1f);
        normalButton.addListener(createButtonListener(() -> {
            GameScreen.setGameMode(1);
            showClassSelection();
        }));

        Table modeButtonsTable = new Table();
        modeButtonsTable.add(endlessButton).size(300, 100).pad(10);
        modeButtonsTable.add(normalButton).size(300, 100).pad(10);
        gameModeTable.add(modeButtonsTable).colspan(2).row();

        gameModeBackButton = new TextButton("BACK", skin);
        gameModeBackButton.getLabel().setFontScale(1.0f);
        gameModeBackButton.addListener(createButtonListener(this::showMainMenu));
        gameModeTable.add(gameModeBackButton).size(150, 60).padTop(40).colspan(2).row();
    }

    private void createClassSelectionUI() {
        Label selectClassLabel = new Label("SELECT YOUR CLASS", skin);
        selectClassLabel.setFontScale(1f);
        classSelectionTable.add(selectClassLabel).padBottom(30).colspan(2).row();

        mercenaryButton = new TextButton("MERCENARY", skin);
        mercenaryButton.getLabel().setFontScale(1f);
        mercenaryButton.addListener(createButtonListener(() -> selectClass(PlayerClass.MERCENARY)));

        paladinButton = new TextButton("PALADIN", skin);
        paladinButton.getLabel().setFontScale(1f);
        paladinButton.addListener(createButtonListener(() -> selectClass(PlayerClass.PALADIN)));

        Table classButtonsTable = new Table();
        classButtonsTable.add(mercenaryButton).size(300, 100).pad(10);
        classButtonsTable.add(paladinButton).size(300, 100).pad(10);
        classSelectionTable.add(classButtonsTable).colspan(2).row();

        classDescriptionLabel = new Label(PlayerClass.MERCENARY.getDescription(), skin);
        classDescriptionLabel.setWrap(false);
        classSelectionTable.add(classDescriptionLabel).width(400).padTop(20).colspan(2).row();

        startGameButton = new TextButton("START GAME", skin);
        startGameButton.getLabel().setFontScale(1f);
        startGameButton.addListener(createButtonListener(this::startGame));
        classSelectionTable.add(startGameButton).size(300, 100).padTop(20).colspan(2).row();

        backButton = new TextButton("BACK", skin);
        backButton.getLabel().setFontScale(1.0f);
        backButton.addListener(createButtonListener(this::showGameModeSelection));
        classSelectionTable.add(backButton).size(150, 60).padTop(10).colspan(2).row();
    }

    private void createSettingsUI() {
        settingsTable.clear();
        settingsTable.center();

        // Title
        Label settingsTitle = new Label("SETTINGS", skin);
        settingsTitle.setFontScale(1.2f);
        settingsTable.add(settingsTitle).padBottom(25).colspan(2).row();

        // ---------- Tabs row ----------
        Table tabsRow = new Table();

        displayTabButton = new TextButton("DISPLAY", skin);
        audioTabButton   = new TextButton("AUDIO", skin);

        displayTabButton.getLabel().setFontScale(0.8f);
        audioTabButton.getLabel().setFontScale(0.8f);

        displayTabButton.addListener(createButtonListener(() -> switchSettingsTab(SettingsTab.DISPLAY)));
        audioTabButton.addListener(createButtonListener(() -> switchSettingsTab(SettingsTab.AUDIO)));

        tabsRow.add(displayTabButton).size(160, 55).padRight(10);
        tabsRow.add(audioTabButton).size(160, 55);

        settingsTable.add(tabsRow).padBottom(25).colspan(2).row();

        displaySettingsTable = new Table();
        audioSettingsTable = new Table();

        buildDisplaySettings(displaySettingsTable);
        buildAudioSettings(audioSettingsTable);

        settingsTable.add(displaySettingsTable).colspan(2).row();
        settingsTable.add(audioSettingsTable).colspan(2).row();

        TextButton applyButton = new TextButton("APPLY", skin);
        applyButton.getLabel().setFontScale(0.8f);
        applyButton.addListener(createButtonListener(this::applySettings));

        TextButton settingsBackButton = new TextButton("BACK", skin);
        settingsBackButton.getLabel().setFontScale(0.8f);
        settingsBackButton.addListener(createButtonListener(this::showMainMenu));

        Table settingsButtonsTable = new Table();
        settingsButtonsTable.add(applyButton).size(150, 60).padRight(20);
        settingsButtonsTable.add(settingsBackButton).size(150, 60);

        settingsTable.add(settingsButtonsTable).padTop(30).colspan(2).row();

        switchSettingsTab(SettingsTab.DISPLAY);
    }

    private void switchSettingsTab(SettingsTab tab) {
        currentSettingsTab = tab;

        boolean display = tab == SettingsTab.DISPLAY;
        displaySettingsTable.setVisible(display);
        audioSettingsTable.setVisible(!display);

        displayTabButton.setColor(display ? Color.GOLD : Color.WHITE);
        audioTabButton.setColor(!display ? Color.GOLD : Color.WHITE);
    }

    private void buildAudioSettings(Table t) {
        t.clear();

        SoundManager sm = SoundManager.getInstance();

        float initialMusic = sm.getMusicVolume();
        float initialSfx = sm.getSfxVolume();

        Label audioLabel = new Label("AUDIO:", skin);
        audioLabel.setFontScale(1.0f);
        t.add(audioLabel).left().padBottom(20).colspan(2).row();

        // MUSIC
        Label musicLabel = new Label("Music Volume", skin);
        musicLabel.setFontScale(0.9f);

        musicPercentLabel = new Label(Math.round(initialMusic * 100) + "%", skin);
        musicPercentLabel.setFontScale(0.9f);

        musicSlider = new Slider(0f, 1f, 0.01f, false, skin);
        musicSlider.setValue(initialMusic);

        musicEnabledCheckbox = new CheckBox(" Enabled", skin);
        musicEnabledCheckbox.setChecked(sm.isMusicEnabled());

        musicSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                float v = musicSlider.getValue();
                sm.setMusicVolume(v);
                musicPercentLabel.setText(Math.round(v * 100) + "%");
            }
        });
        musicSlider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float v = musicSlider.getValue();
                sm.setMusicVolume(v);
                musicPercentLabel.setText(Math.round(v * 100) + "%");
            }
        });

        musicEnabledCheckbox.addListener(createButtonListener(() -> {
            sm.toggleMusic();
            musicEnabledCheckbox.setChecked(sm.isMusicEnabled());
        }));

        t.add(musicLabel).left().padBottom(8);
        t.add(musicPercentLabel).right().padBottom(8).row();
        t.add(musicSlider).width(420).height(35).left().padBottom(8).colspan(2).row();
        t.add(musicEnabledCheckbox).left().padBottom(25).colspan(2).row();

        // SFX
        Label sfxLabel = new Label("SFX Volume", skin);
        sfxLabel.setFontScale(0.9f);

        sfxPercentLabel = new Label(Math.round(initialSfx * 100) + "%", skin);
        sfxPercentLabel.setFontScale(0.9f);

        sfxSlider = new Slider(0f, 1f, 0.01f, false, skin);
        sfxSlider.setValue(initialSfx);

        sfxEnabledCheckbox = new CheckBox(" Enabled", skin);
        sfxEnabledCheckbox.setChecked(sm.isSfxEnabled());

        sfxSlider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float v = sfxSlider.getValue();
                sm.setSfxVolume(v);
                sfxPercentLabel.setText(Math.round(v * 100) + "%");
            }
        });

        sfxEnabledCheckbox.addListener(createButtonListener(() -> {
            sm.toggleSfx();
            sfxEnabledCheckbox.setChecked(sm.isSfxEnabled());
        }));

        t.add(sfxLabel).left().padBottom(8);
        t.add(sfxPercentLabel).right().padBottom(8).row();
        t.add(sfxSlider).width(420).height(35).left().padBottom(8).colspan(2).row();
        t.add(sfxEnabledCheckbox).left().colspan(2).row();
    }

    private void buildDisplaySettings(Table t) {
        t.clear();

        Label windowModeLabel = new Label("WINDOW MODE:", skin);
        windowModeLabel.setFontScale(1.0f);
        t.add(windowModeLabel).left().padBottom(15).colspan(2).row();

        fullscreenCheckbox = new CheckBox(" Fullscreen", skin);
        fullscreenCheckbox.setChecked(isFullscreen);
        fullscreenCheckbox.addListener(createButtonListener(() -> {
            if (fullscreenCheckbox.isChecked()) {
                windowedCheckbox.setChecked(false);
                isFullscreen = true;
                SaveManager.setFullscreen(true);
            } else if (!windowedCheckbox.isChecked()) {
                fullscreenCheckbox.setChecked(true);
            }
        }));

        windowedCheckbox = new CheckBox(" Windowed", skin);
        windowedCheckbox.setChecked(!isFullscreen);
        windowedCheckbox.addListener(createButtonListener(() -> {
            if (windowedCheckbox.isChecked()) {
                fullscreenCheckbox.setChecked(false);
                isFullscreen = false;
                SaveManager.setFullscreen(false);
            } else if (!fullscreenCheckbox.isChecked()) {
                windowedCheckbox.setChecked(true);
            }
        }));

        Table modeTable = new Table();
        modeTable.add(fullscreenCheckbox).padRight(20);
        modeTable.add(windowedCheckbox);
        t.add(modeTable).padBottom(30).colspan(2).row();

        Label resolutionLabel = new Label("RESOLUTION:", skin);
        resolutionLabel.setFontScale(1.0f);
        t.add(resolutionLabel).left().padBottom(15).colspan(2).row();

        size1280x720Checkbox = new CheckBox(" 1280x720", skin);
        size1600x900Checkbox = new CheckBox(" 1600x900", skin);
        size1920x1080Checkbox = new CheckBox(" 1920x1080", skin);

        size1280x720Checkbox.setChecked(selectedWidth == 1280 && selectedHeight == 720);
        size1600x900Checkbox.setChecked(selectedWidth == 1600 && selectedHeight == 900);
        size1920x1080Checkbox.setChecked(selectedWidth == 1920 && selectedHeight == 1080);

        if (!size1280x720Checkbox.isChecked() && !size1600x900Checkbox.isChecked() && !size1920x1080Checkbox.isChecked()) {
            size1920x1080Checkbox.setChecked(true);
            selectedWidth = 1920;
            selectedHeight = 1080;
        }

        size1280x720Checkbox.addListener(createButtonListener(() -> {
            if (size1280x720Checkbox.isChecked()) {
                size1600x900Checkbox.setChecked(false);
                size1920x1080Checkbox.setChecked(false);
                selectedWidth = 1280;
                selectedHeight = 720;
                SaveManager.setWindowSize(selectedWidth, selectedHeight);
            } else if (!size1600x900Checkbox.isChecked() && !size1920x1080Checkbox.isChecked()) {
                size1280x720Checkbox.setChecked(true);
            }
        }));

        size1600x900Checkbox.addListener(createButtonListener(() -> {
            if (size1600x900Checkbox.isChecked()) {
                size1280x720Checkbox.setChecked(false);
                size1920x1080Checkbox.setChecked(false);
                selectedWidth = 1600;
                selectedHeight = 900;
                SaveManager.setWindowSize(selectedWidth, selectedHeight);
            } else if (!size1280x720Checkbox.isChecked() && !size1920x1080Checkbox.isChecked()) {
                size1600x900Checkbox.setChecked(true);
            }
        }));

        size1920x1080Checkbox.addListener(createButtonListener(() -> {
            if (size1920x1080Checkbox.isChecked()) {
                size1280x720Checkbox.setChecked(false);
                size1600x900Checkbox.setChecked(false);
                selectedWidth = 1920;
                selectedHeight = 1080;
                SaveManager.setWindowSize(selectedWidth, selectedHeight);
            } else if (!size1280x720Checkbox.isChecked() && !size1600x900Checkbox.isChecked()) {
                size1920x1080Checkbox.setChecked(true);
            }
        }));

        Table resolutionTable = new Table();
        resolutionTable.add(size1280x720Checkbox).padRight(10);
        resolutionTable.add(size1600x900Checkbox).padRight(10);
        resolutionTable.add(size1920x1080Checkbox);

        t.add(resolutionTable).padBottom(10).colspan(2).row();
    }


    private void applySettings() {
        game.setScreen(new GameScreen(game, selectedWidth, selectedHeight, isFullscreen));        showMainMenu();
    }

    private void showClassSelection() {
        currentState = ScreenState.CLASS_SELECTION;
        mainTable.setVisible(false);
        gameModeTable.setVisible(false);
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
        gameModeTable.setVisible(false);
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

//        setScreen(new GameScreen(this, selectedWidth, selectedHeight, isFullscreen));
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
        gameModeTable = null;
        endlessButton = null;
        normalButton = null;
        gameModeBackButton = null;

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