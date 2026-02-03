package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import config.GameScreen;
import config.Storage;
import game.GameProj;
import managers.SoundManager;

public class Settings {
    private boolean isOpen = false;

    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture pixelTexture;
    private GameProj gameProj;
    private GameScreen gameScreen;

    // UI Layout
    private final float PANEL_WIDTH_PERCENT = 0.33f;
    private float panelWidth;
    private float panelHeight;
    private final float BUTTON_HEIGHT = 45f;
    private final float BUTTON_SPACING = 12f;
    private final float PADDING = 25f;
    private final float TAB_HEIGHT = 35f;

    // Colors
    private final Color BACKGROUND_COLOR = new Color(0.08f, 0.08f, 0.12f, 0.88f);
    private final Color BUTTON_COLOR = new Color(0.18f, 0.18f, 0.24f, 1f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.28f, 0.28f, 0.38f, 1f);
    private final Color TEXT_COLOR = Color.WHITE;
    private final Color TEXT_DIM_COLOR = new Color(0.6f, 0.6f, 0.6f, 1f);
    private final Color TAB_ACTIVE_COLOR = new Color(0.25f, 0.35f, 0.45f, 1f);
    private final Color TAB_INACTIVE_COLOR = new Color(0.15f, 0.15f, 0.2f, 1f);
    private final Color TAB_BORDER_COLOR = new Color(0.4f, 0.4f, 0.5f, 1f);
    private final Color SLIDER_BG_COLOR = new Color(0.15f, 0.15f, 0.2f, 1f);
    private final Color SLIDER_FILL_COLOR = new Color(0.4f, 0.6f, 0.8f, 1f);
    private final Color CHECKBOX_COLOR = new Color(0.2f, 0.2f, 0.25f, 1f);
    private final Color CHECKBOX_CHECKED_COLOR = new Color(0.3f, 0.6f, 0.3f, 1f);
    private final Color DISABLED_COLOR = new Color(0.25f, 0.25f, 0.25f, 0.6f);
    private final Color DISABLED_TEXT_COLOR = new Color(0.4f, 0.4f, 0.4f, 1f);

    // Menu states
    private enum MenuState {
        MAIN_PAUSE,
        SETTINGS_AUDIO,
    }
    private MenuState currentMenuState = MenuState.MAIN_PAUSE;

    // Button hover tracking
    private int hoveredButton = -1;
    private int hoveredTab = -1;

    // Slider dragging
    private boolean isDraggingMusicSlider = false;
    private boolean isDraggingSfxSlider = false;

    // Audio settings
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.3f;
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;

    // Graphics settings
    public enum WindowMode {
        BORDERLESS,
        WINDOWED
    }
    private WindowMode currentWindowMode = WindowMode.BORDERLESS;

    public enum WindowSize {
        SIZE_1280x720(1280, 720, "1280x720"),
        SIZE_1600x900(1600, 900, "1600x900");

        public final int width;
        public final int height;
        public final String label;

        WindowSize(int width, int height, String label) {
            this.width = width;
            this.height = height;
            this.label = label;
        }
    }
    private WindowSize currentWindowSize = WindowSize.SIZE_1280x720;

    private GlyphLayout glyphLayout;

    public Settings() {
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.pixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);
        this.glyphLayout = new GlyphLayout();

        // Sync with SoundManager
        SoundManager sm = SoundManager.getInstance();
        this.musicVolume = sm.getMusicVolume();
        this.sfxVolume = sm.getSfxVolume();
        this.musicEnabled = sm.isMusicEnabled();
        this.sfxEnabled = sm.isSfxEnabled();

        calculateLayout();
    }

    public void setGameProj(GameProj gameProj) {
        this.gameProj = gameProj;
    }

    public void setGameScreen(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    private void calculateLayout() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        panelWidth = screenWidth * PANEL_WIDTH_PERCENT;
        panelHeight = screenHeight;
    }

    public void toggle() {
        isOpen = !isOpen;
        if (isOpen) {
            calculateLayout();
            currentMenuState = MenuState.MAIN_PAUSE;
        }
    }

    public void open() {
        isOpen = true;
        calculateLayout();
        currentMenuState = MenuState.MAIN_PAUSE;
    }

    public void close() {
        isOpen = false;
        currentMenuState = MenuState.MAIN_PAUSE;
    }

    public void update(float delta) {
        if (!isOpen) return;

        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        hoveredButton = -1;
        hoveredTab = -1;

        switch (currentMenuState) {
            case MAIN_PAUSE:
                updateMainPauseMenu(mouseX, mouseY);
                break;
            case SETTINGS_AUDIO:
                updateSettingsMenu(mouseX, mouseY);
                break;
        }

        if (!Gdx.input.isTouched()) {
            isDraggingMusicSlider = false;
            isDraggingSfxSlider = false;
        }
    }

    private void updateMainPauseMenu(int mouseX, int mouseY) {
        float buttonWidth = panelWidth - PADDING * 2;
        float buttonX = PADDING;
        float buttonStartY = panelHeight - 350f;

        String[] buttons = {"Resume", "Settings", "Main Menu", "Exit to Desktop"};

        for (int i = 0; i < buttons.length; i++) {
            float buttonY = buttonStartY - i * (BUTTON_HEIGHT + BUTTON_SPACING);

            if (isPointInRect(mouseX, mouseY, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT)) {
                hoveredButton = i;

                if (Gdx.input.justTouched()) {
                    handleMainMenuClick(i);
                }
            }
        }
    }

    private void handleMainMenuClick(int buttonIndex) {
        switch (buttonIndex) {
            case 0:
                close();
                break;
            case 1:
                currentMenuState = MenuState.SETTINGS_AUDIO;
                break;
            case 2:
                this.gameScreen = gameProj.getGameScreen();
                gameScreen.switchToNewState(GameScreen.START);
                close();
                break;
            case 3:
                if (gameProj != null) {
                    gameProj.safeExit();
                } else {
                    Gdx.app.exit();
                }
                break;
        }
    }

    private void updateSettingsMenu(int mouseX, int mouseY) {
        float tabWidth = (panelWidth - PADDING * 2 - 10) / 2;
        float audioTabX = PADDING;
        float graphicsTabX = PADDING + tabWidth + 10;
        float tabY = panelHeight - 120f;

        if (isPointInRect(mouseX, mouseY, audioTabX, tabY, tabWidth, TAB_HEIGHT)) {
            hoveredTab = 0;
            if (Gdx.input.justTouched()) {
                currentMenuState = MenuState.SETTINGS_AUDIO;
            }
        }

        float backButtonY = PADDING + 20;
        float backButtonWidth = panelWidth - PADDING * 2;
        if (isPointInRect(mouseX, mouseY, PADDING, backButtonY, backButtonWidth, BUTTON_HEIGHT)) {
            hoveredButton = 100;
            if (Gdx.input.justTouched()) {
                currentMenuState = MenuState.MAIN_PAUSE;
            }
        }

        if (currentMenuState == MenuState.SETTINGS_AUDIO) {
            updateAudioSettings(mouseX, mouseY);
        }
    }

    private void updateAudioSettings(int mouseX, int mouseY) {
        float settingsStartY = panelHeight - 180f;
        float sliderWidth = panelWidth - PADDING * 2 - 120;
        float sliderX = PADDING + 120;
        float sliderHeight = 20f;
        float rowSpacing = 60f;

        float musicSliderY = settingsStartY;
        if (isDraggingMusicSlider ||
                (Gdx.input.isTouched() && isPointInRect(mouseX, mouseY, sliderX - 10, musicSliderY - 5, sliderWidth + 20, sliderHeight + 10))) {
            isDraggingMusicSlider = true;
            float newVolume = (mouseX - sliderX) / sliderWidth;
            musicVolume = Math.max(0f, Math.min(1f, newVolume));
            SoundManager.getInstance().setMusicVolume(musicVolume);
        }

        float checkboxX = panelWidth - PADDING - 30;
        float musicCheckY = settingsStartY - rowSpacing;
        if (Gdx.input.justTouched() && isPointInRect(mouseX, mouseY, checkboxX, musicCheckY, 25, 25)) {
            SoundManager.getInstance().toggleMusic();
            musicEnabled = SoundManager.getInstance().isMusicEnabled();
        }

        float sfxSliderY = settingsStartY - rowSpacing * 2;
        if (isDraggingSfxSlider ||
                (Gdx.input.isTouched() && isPointInRect(mouseX, mouseY, sliderX - 10, sfxSliderY - 5, sliderWidth + 20, sliderHeight + 10))) {
            isDraggingSfxSlider = true;
            float newVolume = (mouseX - sliderX) / sliderWidth;
            sfxVolume = Math.max(0f, Math.min(1f, newVolume));
            SoundManager.getInstance().setSfxVolume(sfxVolume);
        }

        float sfxCheckY = settingsStartY - rowSpacing * 3;
        if (Gdx.input.justTouched() && isPointInRect(mouseX, mouseY, checkboxX, sfxCheckY, 25, 25)) {
            SoundManager.getInstance().toggleSfx();
            sfxEnabled = SoundManager.getInstance().isSfxEnabled();
        }
    }

    private boolean isPointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    public void render(SpriteBatch batch, boolean batchIsActive) {
        if (!isOpen) return;

        if (batchIsActive) {
            batch.end();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(0, 0, panelWidth, panelHeight);
        shapeRenderer.end();

        switch (currentMenuState) {
            case MAIN_PAUSE:
                renderMainPauseMenu(batch);
                break;
            case SETTINGS_AUDIO:
                renderSettingsMenu(batch);
                break;
        }

        if (batchIsActive) {
            batch.begin();
        }
    }

    private void renderMainPauseMenu(SpriteBatch batch) {
        float buttonWidth = panelWidth - PADDING * 2;
        float buttonX = PADDING;
        float buttonStartY = panelHeight - 350f;

        String[] buttons = {"Resume", "Settings", "Main Menu", "Exit to Desktop"};

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < buttons.length; i++) {
            float buttonY = buttonStartY - i * (BUTTON_HEIGHT + BUTTON_SPACING);

            if (hoveredButton == i) {
                shapeRenderer.setColor(BUTTON_HOVER_COLOR);
            } else {
                shapeRenderer.setColor(BUTTON_COLOR);
            }
            shapeRenderer.rect(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT);
        }
        shapeRenderer.end();

        batch.begin();
        font.setColor(TEXT_COLOR);
        font.getData().setScale(1.2f);

        font.draw(batch, "PAUSED", PADDING, panelHeight - 40);

        font.getData().setScale(0.8f);
        for (int i = 0; i < buttons.length; i++) {
            float buttonY = buttonStartY - i * (BUTTON_HEIGHT + BUTTON_SPACING);
            glyphLayout.setText(font, buttons[i]);
            float textX = buttonX + (buttonWidth - glyphLayout.width) / 2;
            float textY = buttonY + BUTTON_HEIGHT / 2 + glyphLayout.height / 2;
            font.draw(batch, buttons[i], textX, textY);
        }

        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderSettingsMenu(SpriteBatch batch) {
        float tabWidth = (panelWidth - PADDING * 2 - 10) / 2;
        float audioTabX = PADDING;
        float tabY = panelHeight - 120f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (currentMenuState == MenuState.SETTINGS_AUDIO) {
            shapeRenderer.setColor(TAB_ACTIVE_COLOR);
        } else {
            shapeRenderer.setColor(hoveredTab == 0 ? BUTTON_HOVER_COLOR : TAB_INACTIVE_COLOR);
        }
        shapeRenderer.rect(audioTabX, tabY, tabWidth, TAB_HEIGHT);

        float backButtonY = PADDING + 20;
        float backButtonWidth = panelWidth - PADDING * 2;
        if (hoveredButton == 100) {
            shapeRenderer.setColor(BUTTON_HOVER_COLOR);
        } else {
            shapeRenderer.setColor(BUTTON_COLOR);
        }
        shapeRenderer.rect(PADDING, backButtonY, backButtonWidth, BUTTON_HEIGHT);

        shapeRenderer.end();

        if (currentMenuState == MenuState.SETTINGS_AUDIO)
            renderAudioSettings(batch);

        batch.begin();
        font.setColor(TEXT_COLOR);
        font.getData().setScale(1.2f);
        font.draw(batch, "SETTINGS", PADDING, panelHeight - 40);

        font.getData().setScale(0.7f);
        glyphLayout.setText(font, "Audio");
        font.draw(batch, "Audio", audioTabX + (tabWidth - glyphLayout.width) / 2, tabY + TAB_HEIGHT / 2 + glyphLayout.height / 2);

        font.getData().setScale(0.8f);
        glyphLayout.setText(font, "Back");
        float backButtonWidth2 = panelWidth - PADDING * 2;
        font.draw(batch, "Back", PADDING + (backButtonWidth2 - glyphLayout.width) / 2, PADDING + 20 + BUTTON_HEIGHT / 2 + glyphLayout.height / 2);

        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderAudioSettings(SpriteBatch batch) {
        float settingsStartY = panelHeight - 180f;
        float sliderWidth = panelWidth - PADDING * 2 - 170;
        float sliderX = PADDING + 120;
        float sliderHeight = 20f;
        float rowSpacing = 60f;
        float checkboxSize = 25f;
        float checkboxX = panelWidth - PADDING - 60;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float musicSliderY = settingsStartY;
        shapeRenderer.setColor(SLIDER_BG_COLOR);
        shapeRenderer.rect(sliderX, musicSliderY, sliderWidth, sliderHeight);
        shapeRenderer.setColor(SLIDER_FILL_COLOR);
        shapeRenderer.rect(sliderX, musicSliderY, sliderWidth * musicVolume, sliderHeight);

        float musicCheckY = settingsStartY - rowSpacing;
        if (musicEnabled) {
            shapeRenderer.setColor(CHECKBOX_CHECKED_COLOR);
        } else {
            shapeRenderer.setColor(CHECKBOX_COLOR);
        }
        shapeRenderer.rect(checkboxX, musicCheckY, checkboxSize, checkboxSize);

        float sfxSliderY = settingsStartY - rowSpacing * 2;
        shapeRenderer.setColor(SLIDER_BG_COLOR);
        shapeRenderer.rect(sliderX, sfxSliderY, sliderWidth, sliderHeight);
        shapeRenderer.setColor(SLIDER_FILL_COLOR);
        shapeRenderer.rect(sliderX, sfxSliderY, sliderWidth * sfxVolume, sliderHeight);

        float sfxCheckY = settingsStartY - rowSpacing * 3;
        if (sfxEnabled) {
            shapeRenderer.setColor(CHECKBOX_CHECKED_COLOR);
        } else {
            shapeRenderer.setColor(CHECKBOX_COLOR);
        }
        shapeRenderer.rect(checkboxX, sfxCheckY, checkboxSize, checkboxSize);

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(TAB_BORDER_COLOR);
        shapeRenderer.rect(checkboxX, musicCheckY, checkboxSize, checkboxSize);
        shapeRenderer.rect(checkboxX, sfxCheckY, checkboxSize, checkboxSize);
        shapeRenderer.end();

        batch.begin();
        font.setColor(TEXT_COLOR);
        font.getData().setScale(0.65f);

        font.draw(batch, "MUSIC:", PADDING, musicSliderY + sliderHeight - 2);
        int musicPercent = Math.round(musicVolume * 100);
        font.draw(batch, musicPercent + "%", sliderX + sliderWidth + 10, musicSliderY + sliderHeight - 2);

        font.draw(batch, "MUSIC ON/OFF:", PADDING, musicCheckY + checkboxSize - 5);

        font.draw(batch, "EFFECTS:", PADDING, sfxSliderY + sliderHeight - 2);
        int sfxPercent = Math.round(sfxVolume * 100);
        font.draw(batch, sfxPercent + "%", sliderX + sliderWidth + 10, sfxSliderY + sliderHeight - 2);

        font.draw(batch, "EFFECTS ON/OFF:", PADDING, sfxCheckY + checkboxSize - 5);

        font.getData().setScale(1.0f);
        batch.end();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    public WindowMode getCurrentWindowMode() {
        return currentWindowMode;
    }

    public WindowSize getCurrentWindowSize() {
        return currentWindowSize;
    }
}