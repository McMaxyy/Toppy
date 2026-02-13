package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import config.GameScreen;
import config.SaveData;
import config.SaveManager;
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
    private final Color TAB_ACTIVE_COLOR = new Color(0.25f, 0.35f, 0.45f, 1f);
    private final Color TAB_INACTIVE_COLOR = new Color(0.15f, 0.15f, 0.2f, 1f);
    private final Color TAB_BORDER_COLOR = new Color(0.4f, 0.4f, 0.5f, 1f);
    private final Color SLIDER_BG_COLOR = new Color(0.15f, 0.15f, 0.2f, 1f);
    private final Color SLIDER_FILL_COLOR = new Color(0.4f, 0.6f, 0.8f, 1f);
    private final Color CHECKBOX_COLOR = new Color(0.2f, 0.2f, 0.25f, 1f);
    private final Color CHECKBOX_CHECKED_COLOR = new Color(0.3f, 0.6f, 0.3f, 1f);
    private final Color KEYBIND_BOX_COLOR = new Color(0.2f, 0.2f, 0.28f, 1f);
    private final Color KEYBIND_LISTENING_COLOR = new Color(0.4f, 0.3f, 0.2f, 1f);
    private final Color RESET_BUTTON_COLOR = new Color(0.5f, 0.25f, 0.25f, 1f);

    private enum MenuState { MAIN_PAUSE, SETTINGS_AUDIO, SETTINGS_GAME, SETTINGS_CONFIG }
    private MenuState currentMenuState = MenuState.MAIN_PAUSE;

    private int hoveredButton = -1;
    private int hoveredTab = -1;
    private boolean isDraggingMusicSlider = false;
    private boolean isDraggingSfxSlider = false;

    private float musicVolume = 0.7f;
    private float sfxVolume = 0.3f;
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;
    private boolean screenShakeEnabled = true;
    private boolean legendEnabled = true;

    // Keybinding
    private boolean isListeningForKey = false;
    private String listeningAction = null;
    private int[] capturedKeys = new int[2];
    private int capturedKeyCount = 0;
    private float listeningTimer = 0f;
    private float listeningFlashTimer = 0f;
    private static final float KEY_COMBO_TIMEOUT = 2.0f;

    private GlyphLayout glyphLayout;

    public Settings() {
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.pixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);
        this.glyphLayout = new GlyphLayout();
        syncWithSaveData();
        calculateLayout();
    }

    private void syncWithSaveData() {
        SoundManager sm = SoundManager.getInstance();
        this.musicVolume = sm.getMusicVolume();
        this.sfxVolume = sm.getSfxVolume();
        this.musicEnabled = sm.isMusicEnabled();
        this.sfxEnabled = sm.isSfxEnabled();
        this.screenShakeEnabled = SaveManager.isScreenShakeEnabled();
        this.legendEnabled = SaveManager.isLegendEnabled();
    }

    public void setGameProj(GameProj gameProj) { this.gameProj = gameProj; }
    public void setGameScreen(GameScreen gameScreen) { this.gameScreen = gameScreen; }

    private void calculateLayout() {
        panelWidth = Gdx.graphics.getWidth() * PANEL_WIDTH_PERCENT;
        panelHeight = Gdx.graphics.getHeight();
    }

    public void toggle() {
        isOpen = !isOpen;
        if (isOpen) {
            calculateLayout();
            currentMenuState = MenuState.MAIN_PAUSE;
            syncWithSaveData();
            cancelKeyListening();
        }
    }

    public void open() {
        isOpen = true;
        calculateLayout();
        currentMenuState = MenuState.MAIN_PAUSE;
        syncWithSaveData();
        cancelKeyListening();
    }

    public void close() {
        isOpen = false;
        currentMenuState = MenuState.MAIN_PAUSE;
        cancelKeyListening();
    }

    private void playClickSound() { SoundManager.getInstance().playButtonSound(); }

    public void update(float delta) {
        if (!isOpen) return;

        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        hoveredButton = -1;
        hoveredTab = -1;

        if (isListeningForKey) {
            updateKeyListening(delta);
            return;
        }

        switch (currentMenuState) {
            case MAIN_PAUSE: updateMainPauseMenu(mouseX, mouseY); break;
            case SETTINGS_AUDIO:
            case SETTINGS_GAME:
            case SETTINGS_CONFIG: updateSettingsMenu(mouseX, mouseY); break;
        }

        if (!Gdx.input.isTouched()) {
            isDraggingMusicSlider = false;
            isDraggingSfxSlider = false;
        }
    }

    private void updateKeyListening(float delta) {
        listeningTimer += delta;
        listeningFlashTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            playClickSound();
            cancelKeyListening();
            return;
        }

        for (int key = 0; key < 256; key++) {
            if (Gdx.input.isKeyJustPressed(key) && key != Input.Keys.ESCAPE) {
                if (capturedKeyCount == 0) {
                    capturedKeys[0] = key;
                    capturedKeyCount = 1;
                    listeningTimer = 0f;
                } else if (capturedKeyCount == 1 && key != capturedKeys[0]) {
                    if (Gdx.input.isKeyPressed(capturedKeys[0])) {
                        capturedKeys[1] = key;
                        capturedKeyCount = 2;
                        finishKeyListening();
                    }
                }
                break;
            }
        }

        if (capturedKeyCount == 1 && listeningTimer >= KEY_COMBO_TIMEOUT) {
            finishKeyListening();
        }

        if (capturedKeyCount == 1 && !Gdx.input.isKeyPressed(capturedKeys[0])) {
            finishKeyListening();
        }
    }

    private void startKeyListening(String action) {
        isListeningForKey = true;
        listeningAction = action;
        capturedKeyCount = 0;
        listeningTimer = 0f;
        listeningFlashTimer = 0f;
        playClickSound();
    }

    private void cancelKeyListening() {
        isListeningForKey = false;
        listeningAction = null;
        capturedKeyCount = 0;
    }

    private void finishKeyListening() {
        if (listeningAction != null && capturedKeyCount > 0) {
            int[] newKeys = new int[capturedKeyCount];
            System.arraycopy(capturedKeys, 0, newKeys, 0, capturedKeyCount);
            SaveManager.setKeybinding(listeningAction, newKeys);
            playClickSound();
        }
        cancelKeyListening();
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
                    playClickSound();
                    handleMainMenuClick(i);
                }
            }
        }
    }

    private void handleMainMenuClick(int buttonIndex) {
        switch (buttonIndex) {
            case 0: close(); break;
            case 1: currentMenuState = MenuState.SETTINGS_AUDIO; break;
            case 2:
                gameProj.transferToSafeStash();
                this.gameScreen = gameProj.getGameScreen();
                gameScreen.switchToNewState(GameScreen.START);
                close();
                break;
            case 3:
                if (gameProj != null) {
                    gameProj.transferToSafeStash();
                    gameProj.safeExit();
                }
                else Gdx.app.exit();
                break;
        }
    }

    private void updateSettingsMenu(int mouseX, int mouseY) {
        float tabWidth = (panelWidth - PADDING * 2 - 20) / 3;
        float tabY = panelHeight - 120f;

        if (isPointInRect(mouseX, mouseY, PADDING, tabY, tabWidth, TAB_HEIGHT)) {
            hoveredTab = 0;
            if (Gdx.input.justTouched()) { playClickSound(); currentMenuState = MenuState.SETTINGS_AUDIO; }
        }
        if (isPointInRect(mouseX, mouseY, PADDING + tabWidth + 10, tabY, tabWidth, TAB_HEIGHT)) {
            hoveredTab = 1;
            if (Gdx.input.justTouched()) { playClickSound(); currentMenuState = MenuState.SETTINGS_GAME; }
        }
        if (isPointInRect(mouseX, mouseY, PADDING + (tabWidth + 10) * 2, tabY, tabWidth, TAB_HEIGHT)) {
            hoveredTab = 2;
            if (Gdx.input.justTouched()) { playClickSound(); currentMenuState = MenuState.SETTINGS_CONFIG; }
        }

        float backButtonY = PADDING + 20;
        if (isPointInRect(mouseX, mouseY, PADDING, backButtonY, panelWidth - PADDING * 2, BUTTON_HEIGHT)) {
            hoveredButton = 100;
            if (Gdx.input.justTouched()) { playClickSound(); currentMenuState = MenuState.MAIN_PAUSE; }
        }

        switch (currentMenuState) {
            case SETTINGS_AUDIO: updateAudioSettings(mouseX, mouseY); break;
            case SETTINGS_GAME: updateGameSettings(mouseX, mouseY); break;
            case SETTINGS_CONFIG: updateConfigSettings(mouseX, mouseY); break;
        }
    }

    private void updateAudioSettings(int mouseX, int mouseY) {
        float settingsStartY = panelHeight - 180f;
        float sliderWidth = panelWidth - PADDING * 2 - 170;
        float sliderX = PADDING + 120;
        float rowSpacing = 60f;
        float checkboxX = panelWidth - PADDING - 30;

        if (isDraggingMusicSlider || (Gdx.input.isTouched() && isPointInRect(mouseX, mouseY, sliderX - 10, settingsStartY - 5, sliderWidth + 20, 30))) {
            isDraggingMusicSlider = true;
            musicVolume = Math.max(0f, Math.min(1f, (mouseX - sliderX) / sliderWidth));
            SoundManager.getInstance().setMusicVolume(musicVolume);
        }

        if (Gdx.input.justTouched() && isPointInRect(mouseX, mouseY, checkboxX, settingsStartY - rowSpacing, 25, 25)) {
            playClickSound();
            SoundManager.getInstance().toggleMusic();
            musicEnabled = SoundManager.getInstance().isMusicEnabled();
        }

        if (isDraggingSfxSlider || (Gdx.input.isTouched() && isPointInRect(mouseX, mouseY, sliderX - 10, settingsStartY - rowSpacing * 2 - 5, sliderWidth + 20, 30))) {
            isDraggingSfxSlider = true;
            sfxVolume = Math.max(0f, Math.min(1f, (mouseX - sliderX) / sliderWidth));
            SoundManager.getInstance().setSfxVolume(sfxVolume);
        }

        if (Gdx.input.justTouched() && isPointInRect(mouseX, mouseY, checkboxX, settingsStartY - rowSpacing * 3, 25, 25)) {
            playClickSound();
            SoundManager.getInstance().toggleSfx();
            sfxEnabled = SoundManager.getInstance().isSfxEnabled();
        }
    }

    private void updateGameSettings(int mouseX, int mouseY) {
        float checkboxX = panelWidth - PADDING - 30;
        float settingsStartY = panelHeight - 180f;
        float rowSpacing = 60f;

        if (Gdx.input.justTouched() && isPointInRect(mouseX, mouseY, checkboxX, settingsStartY, 25, 25)) {
            playClickSound();
            screenShakeEnabled = !screenShakeEnabled;
            SaveManager.setScreenShakeEnabled(screenShakeEnabled);
        }
        if (Gdx.input.justTouched() && isPointInRect(mouseX, mouseY, checkboxX, settingsStartY - rowSpacing, 25, 25)) {
            playClickSound();
            legendEnabled = !legendEnabled;
            SaveManager.setLegendEnabled(legendEnabled);
        }
    }

    private void updateConfigSettings(int mouseX, int mouseY) {
        float settingsStartY = panelHeight - 180f;
        float rowSpacing = 40f;
        float keybindBoxWidth = 120f;
        float keybindBoxHeight = 30f;
        float keybindBoxX = panelWidth - PADDING - keybindBoxWidth - 10;
        String[] actions = SaveData.getAllActions();

        for (int i = 0; i < actions.length; i++) {
            float rowY = settingsStartY - i * rowSpacing;
            if (Gdx.input.justTouched() && isPointInRect(mouseX, mouseY, keybindBoxX, rowY, keybindBoxWidth, keybindBoxHeight)) {
                startKeyListening(actions[i]);
            }
        }

        float resetButtonWidth = panelWidth - PADDING * 2 - 10;
        float resetButtonY = settingsStartY - actions.length * rowSpacing - 40;
        if (isPointInRect(mouseX, mouseY, PADDING, resetButtonY, resetButtonWidth, BUTTON_HEIGHT)) {
            hoveredButton = 101;
            if (Gdx.input.justTouched()) {
                playClickSound();
                SaveManager.resetKeybindingsToDefault();
            }
        }
    }

    private boolean isPointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    public void render(SpriteBatch batch, boolean batchIsActive) {
        if (!isOpen) return;
        if (batchIsActive) batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(0, 0, panelWidth, panelHeight);
        shapeRenderer.end();

        switch (currentMenuState) {
            case MAIN_PAUSE: renderMainPauseMenu(batch); break;
            default: renderSettingsMenu(batch); break;
        }

        if (batchIsActive) batch.begin();
    }

    private void renderMainPauseMenu(SpriteBatch batch) {
        float buttonWidth = panelWidth - PADDING * 2;
        float buttonX = PADDING;
        float buttonStartY = panelHeight - 350f;
        String[] buttons = {"Resume", "Settings", "Main Menu", "Exit to Desktop"};

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < buttons.length; i++) {
            float buttonY = buttonStartY - i * (BUTTON_HEIGHT + BUTTON_SPACING);
            shapeRenderer.setColor(hoveredButton == i ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
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
            font.draw(batch, buttons[i], buttonX + (buttonWidth - glyphLayout.width) / 2, buttonY + BUTTON_HEIGHT / 2 + glyphLayout.height / 2);
        }
        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderSettingsMenu(SpriteBatch batch) {
        float tabWidth = (panelWidth - PADDING * 2 - 20) / 3;
        float tabY = panelHeight - 120f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(currentMenuState == MenuState.SETTINGS_AUDIO ? TAB_ACTIVE_COLOR : (hoveredTab == 0 ? BUTTON_HOVER_COLOR : TAB_INACTIVE_COLOR));
        shapeRenderer.rect(PADDING, tabY, tabWidth, TAB_HEIGHT);
        shapeRenderer.setColor(currentMenuState == MenuState.SETTINGS_GAME ? TAB_ACTIVE_COLOR : (hoveredTab == 1 ? BUTTON_HOVER_COLOR : TAB_INACTIVE_COLOR));
        shapeRenderer.rect(PADDING + tabWidth + 10, tabY, tabWidth, TAB_HEIGHT);
        shapeRenderer.setColor(currentMenuState == MenuState.SETTINGS_CONFIG ? TAB_ACTIVE_COLOR : (hoveredTab == 2 ? BUTTON_HOVER_COLOR : TAB_INACTIVE_COLOR));
        shapeRenderer.rect(PADDING + (tabWidth + 10) * 2, tabY, tabWidth, TAB_HEIGHT);

        shapeRenderer.setColor(hoveredButton == 100 ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        shapeRenderer.rect(PADDING, PADDING + 20, panelWidth - PADDING * 2, BUTTON_HEIGHT);
        shapeRenderer.end();

        switch (currentMenuState) {
            case SETTINGS_AUDIO: renderAudioSettings(batch); break;
            case SETTINGS_GAME: renderGameSettings(batch); break;
            case SETTINGS_CONFIG: renderConfigSettings(batch); break;
        }

        batch.begin();
        font.setColor(TEXT_COLOR);
        font.getData().setScale(1.2f);
        font.draw(batch, "SETTINGS", PADDING, panelHeight - 40);

        font.getData().setScale(0.65f);
        glyphLayout.setText(font, "Audio");
        font.draw(batch, "Audio", PADDING + (tabWidth - glyphLayout.width) / 2, tabY + TAB_HEIGHT / 2 + glyphLayout.height / 2);
        glyphLayout.setText(font, "Game");
        font.draw(batch, "Game", PADDING + tabWidth + 10 + (tabWidth - glyphLayout.width) / 2, tabY + TAB_HEIGHT / 2 + glyphLayout.height / 2);
        glyphLayout.setText(font, "Controls");
        font.draw(batch, "Controls", PADDING + (tabWidth + 10) * 2 + (tabWidth - glyphLayout.width) / 2, tabY + TAB_HEIGHT / 2 + glyphLayout.height / 2);

        font.getData().setScale(0.8f);
        glyphLayout.setText(font, "Back");
        font.draw(batch, "Back", PADDING + (panelWidth - PADDING * 2 - glyphLayout.width) / 2, PADDING + 20 + BUTTON_HEIGHT / 2 + glyphLayout.height / 2);
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
        float checkboxX = panelWidth - PADDING - 30;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(SLIDER_BG_COLOR);
        shapeRenderer.rect(sliderX, settingsStartY, sliderWidth, sliderHeight);
        shapeRenderer.setColor(SLIDER_FILL_COLOR);
        shapeRenderer.rect(sliderX, settingsStartY, sliderWidth * musicVolume, sliderHeight);

        shapeRenderer.setColor(musicEnabled ? CHECKBOX_CHECKED_COLOR : CHECKBOX_COLOR);
        shapeRenderer.rect(checkboxX, settingsStartY - rowSpacing, checkboxSize, checkboxSize);

        shapeRenderer.setColor(SLIDER_BG_COLOR);
        shapeRenderer.rect(sliderX, settingsStartY - rowSpacing * 2, sliderWidth, sliderHeight);
        shapeRenderer.setColor(SLIDER_FILL_COLOR);
        shapeRenderer.rect(sliderX, settingsStartY - rowSpacing * 2, sliderWidth * sfxVolume, sliderHeight);

        shapeRenderer.setColor(sfxEnabled ? CHECKBOX_CHECKED_COLOR : CHECKBOX_COLOR);
        shapeRenderer.rect(checkboxX, settingsStartY - rowSpacing * 3, checkboxSize, checkboxSize);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(TAB_BORDER_COLOR);
        shapeRenderer.rect(checkboxX, settingsStartY - rowSpacing, checkboxSize, checkboxSize);
        shapeRenderer.rect(checkboxX, settingsStartY - rowSpacing * 3, checkboxSize, checkboxSize);
        shapeRenderer.end();

        batch.begin();
        font.setColor(TEXT_COLOR);
        font.getData().setScale(0.65f);
        font.draw(batch, "MUSIC:", PADDING, settingsStartY + sliderHeight - 2);
        font.draw(batch, Math.round(musicVolume * 100) + "%", sliderX + sliderWidth + 10, settingsStartY + sliderHeight - 2);
        font.draw(batch, "MUSIC ON/OFF:", PADDING, settingsStartY - rowSpacing + checkboxSize - 5);
        font.draw(batch, "EFFECTS:", PADDING, settingsStartY - rowSpacing * 2 + sliderHeight - 2);
        font.draw(batch, Math.round(sfxVolume * 100) + "%", sliderX + sliderWidth + 10, settingsStartY - rowSpacing * 2 + sliderHeight - 2);
        font.draw(batch, "EFFECTS ON/OFF:", PADDING, settingsStartY - rowSpacing * 3 + checkboxSize - 5);
        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderGameSettings(SpriteBatch batch) {
        float settingsStartY = panelHeight - 180f;
        float rowSpacing = 60f;
        float checkboxSize = 25f;
        float checkboxX = panelWidth - PADDING - 30;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(screenShakeEnabled ? CHECKBOX_CHECKED_COLOR : CHECKBOX_COLOR);
        shapeRenderer.rect(checkboxX, settingsStartY, checkboxSize, checkboxSize);
        shapeRenderer.setColor(legendEnabled ? CHECKBOX_CHECKED_COLOR : CHECKBOX_COLOR);
        shapeRenderer.rect(checkboxX, settingsStartY - rowSpacing, checkboxSize, checkboxSize);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(TAB_BORDER_COLOR);
        shapeRenderer.rect(checkboxX, settingsStartY, checkboxSize, checkboxSize);
        shapeRenderer.rect(checkboxX, settingsStartY - rowSpacing, checkboxSize, checkboxSize);
        shapeRenderer.end();

        batch.begin();
        font.setColor(TEXT_COLOR);
        font.getData().setScale(0.65f);
        font.draw(batch, "SCREEN SHAKE:", PADDING, settingsStartY + checkboxSize - 5);
        font.draw(batch, "SHOW LEGEND:", PADDING, settingsStartY - rowSpacing + checkboxSize - 5);
        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderConfigSettings(SpriteBatch batch) {
        float settingsStartY = panelHeight - 180f;
        float rowSpacing = 40f;
        float keybindBoxWidth = 150f;
        float keybindBoxHeight = 30f;
        float keybindBoxX = panelWidth - PADDING - keybindBoxWidth - 10;
        String[] actions = SaveData.getAllActions();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < actions.length; i++) {
            float rowY = settingsStartY - i * rowSpacing;
            boolean isListening = isListeningForKey && actions[i].equals(listeningAction);
            if (isListening) {
                float flash = (float) Math.sin(listeningFlashTimer * 8) * 0.5f + 0.5f;
                shapeRenderer.setColor(KEYBIND_LISTENING_COLOR.r + flash * 0.2f, KEYBIND_LISTENING_COLOR.g + flash * 0.1f, KEYBIND_LISTENING_COLOR.b, 1f);
            } else {
                shapeRenderer.setColor(KEYBIND_BOX_COLOR);
            }
            shapeRenderer.rect(keybindBoxX, rowY, keybindBoxWidth, keybindBoxHeight);
        }

        float resetButtonWidth = panelWidth - PADDING * 2 - 10;
        float resetButtonY = settingsStartY - actions.length * rowSpacing - 40;
        shapeRenderer.setColor(hoveredButton == 101 ? new Color(0.6f, 0.3f, 0.3f, 1f) : RESET_BUTTON_COLOR);
        shapeRenderer.rect(PADDING, resetButtonY, resetButtonWidth, BUTTON_HEIGHT);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(TAB_BORDER_COLOR);
        for (int i = 0; i < actions.length; i++) {
            float rowY = settingsStartY - i * rowSpacing;
            shapeRenderer.rect(keybindBoxX, rowY, keybindBoxWidth, keybindBoxHeight);
        }
        shapeRenderer.end();

        batch.begin();
        font.setColor(TEXT_COLOR);
        font.getData().setScale(0.6f);
        for (int i = 0; i < actions.length; i++) {
            float rowY = settingsStartY - i * rowSpacing;
            String displayName = SaveData.getActionDisplayName(actions[i]);
            font.draw(batch, displayName + ":", PADDING, rowY + keybindBoxHeight - 8);

            boolean isListening = isListeningForKey && actions[i].equals(listeningAction);
            String keyText;
            if (isListening) {
                if (capturedKeyCount == 0) {
                    keyText = "Press key...";
                } else {
                    keyText = Input.Keys.toString(capturedKeys[0]) + " + ...";
                }
            } else {
                keyText = SaveManager.getKeybindingDisplayString(actions[i]);
            }
            glyphLayout.setText(font, keyText);
            font.draw(batch, keyText, keybindBoxX + (keybindBoxWidth - glyphLayout.width) / 2, rowY + keybindBoxHeight - 8);
        }

        float resetButtonY2 = settingsStartY - actions.length * rowSpacing - 40;
        font.getData().setScale(0.7f);
        glyphLayout.setText(font, "Reset to Default");
        font.draw(batch, "Reset to Default", resetButtonWidth / 2 - glyphLayout.width / 2, resetButtonY2 + BUTTON_HEIGHT / 2 + glyphLayout.height / 2);
        font.getData().setScale(1.0f);
        batch.end();
    }

    public boolean isOpen() { return isOpen; }

    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
