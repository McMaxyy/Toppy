package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import config.Storage;

import game.GameProj;

/**
 * Settings/Pause menu with Resume and Exit options
 */
public class Settings {
    private boolean isOpen = false;

    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture pixelTexture;
    private GameProj gameProj; // Reference for safe exit

    // UI Layout
    private final float MENU_WIDTH = 300f;
    private final float MENU_HEIGHT = 200f;
    private final float BUTTON_WIDTH = 200f;
    private final float BUTTON_HEIGHT = 50f;
    private final float BUTTON_SPACING = 20f;

    private float menuX, menuY;
    private float resumeButtonX, resumeButtonY;
    private float exitButtonX, exitButtonY;

    // Colors
    private final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.95f);
    private final Color BUTTON_COLOR = new Color(0.3f, 0.3f, 0.35f, 1f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.4f, 0.4f, 0.45f, 1f);
    private final Color TEXT_COLOR = Color.WHITE;

    private int hoveredButton = -1; // -1 = none, 0 = resume, 1 = exit

    public Settings() {
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.pixelTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

        calculateLayout();
    }

    /**
     * Set the GameProj reference for safe exit
     */
    public void setGameProj(GameProj gameProj) {
        this.gameProj = gameProj;
    }

    /**
     * Calculate button positions based on screen size
     */
    private void calculateLayout() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Center the menu
        menuX = (screenWidth - MENU_WIDTH) / 2f;
        menuY = (screenHeight - MENU_HEIGHT) / 2f;

        // Position buttons
        float buttonStartY = menuY + MENU_HEIGHT - 80f;

        resumeButtonX = menuX + (MENU_WIDTH - BUTTON_WIDTH) / 2f;
        resumeButtonY = buttonStartY;

        exitButtonX = menuX + (MENU_WIDTH - BUTTON_WIDTH) / 2f;
        exitButtonY = buttonStartY - BUTTON_HEIGHT - BUTTON_SPACING;
    }

    /**
     * Toggle settings menu open/closed
     */
    public void toggle() {
        isOpen = !isOpen;

        if (isOpen) {
            calculateLayout(); // Recalculate in case screen size changed
        }
    }

    /**
     * Open settings menu
     */
    public void open() {
        isOpen = true;
        calculateLayout();
    }

    /**
     * Close settings menu (resume game)
     */
    public void close() {
        isOpen = false;
    }

    /**
     * Update settings menu (handle input)
     */
    public void update(float delta) {
        if (!isOpen) return;

        // Update hover state
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Flip Y coordinate

        hoveredButton = -1;

        // Check Resume button hover
        if (mouseX >= resumeButtonX && mouseX <= resumeButtonX + BUTTON_WIDTH &&
                mouseY >= resumeButtonY && mouseY <= resumeButtonY + BUTTON_HEIGHT) {
            hoveredButton = 0;

            if (Gdx.input.justTouched()) {
                close();
            }
        }

        // Check Exit button hover
        if (mouseX >= exitButtonX && mouseX <= exitButtonX + BUTTON_WIDTH &&
                mouseY >= exitButtonY && mouseY <= exitButtonY + BUTTON_HEIGHT) {
            hoveredButton = 1;

            if (Gdx.input.justTouched()) {
                if (gameProj != null) {
                    gameProj.safeExit();
                } else {
                    Gdx.app.exit();
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Render settings menu
     */
    public void render(SpriteBatch batch, boolean batchIsActive) {
        if (!isOpen) return;

        // End batch if active
        if (batchIsActive) {
            batch.end();
        }

//        // Draw semi-transparent overlay
//        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
//        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        shapeRenderer.end();

//        // Draw menu background
//        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//        shapeRenderer.setColor(BACKGROUND_COLOR);
//        shapeRenderer.rect(menuX, menuY, MENU_WIDTH, MENU_HEIGHT);
//        shapeRenderer.end();

        // Draw menu border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(menuX, menuY, MENU_WIDTH, MENU_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        // Draw Resume button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Color resumeColor = hoveredButton == 0 ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        shapeRenderer.setColor(resumeColor);
        shapeRenderer.rect(resumeButtonX, resumeButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        shapeRenderer.end();

        // Draw Resume button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(hoveredButton == 0 ? Color.WHITE : Color.GRAY);
        shapeRenderer.rect(resumeButtonX, resumeButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        // Draw Exit button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Color exitColor = hoveredButton == 1 ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        shapeRenderer.setColor(exitColor);
        shapeRenderer.rect(exitButtonX, exitButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        shapeRenderer.end();

        // Draw Exit button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(hoveredButton == 1 ? Color.WHITE : Color.GRAY);
        shapeRenderer.rect(exitButtonX, exitButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        // Draw text
        batch.begin();

        // Title
        font.setColor(TEXT_COLOR);
        font.getData().setScale(1.5f);
        String title = "PAUSED";
        font.draw(batch, title, menuX, menuY + MENU_HEIGHT * 2);

        // Resume button text
        font.getData().setScale(1.0f);
        String resumeText = "Resume";
        float resumeTextWidth = font.getRegion().getRegionWidth() * resumeText.length() * 0.3f;
        font.draw(batch, resumeText,
                resumeButtonX + (BUTTON_WIDTH - BUTTON_WIDTH / 2) / 2f,
                resumeButtonY + BUTTON_HEIGHT / 2f + 8f);

        // Exit button text
        String exitText = "Exit";
        font.draw(batch, exitText,
                exitButtonX + (BUTTON_WIDTH - BUTTON_WIDTH / 2) / 2f,
                exitButtonY + BUTTON_HEIGHT / 2f + 8f);

        // Reset font scale
        font.getData().setScale(1.0f);

        batch.end();

        // Restart batch if it was active
        if (batchIsActive) {
            batch.begin();
        }
    }

    /**
     * Check if settings menu is open
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        shapeRenderer.dispose();
    }
}