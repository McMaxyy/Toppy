package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.Viewport;

import config.Storage;
import entities.Player;
import entities.PlayerClass;
import managers.BuffManager;

public class PlayerStatusUI {

    // Textures
    private Texture mercenaryCircleTexture;
    private Texture paladinCircleTexture;
    private Texture mercenaryPortraitTexture;
    private Texture paladinPortraitTexture;
    private Texture whitePixel;

    // Layout configuration
    private float circleSize = 128f;
    private float padding = 10f;
    private float healthBarWidth = 250f;
    private float healthBarHeight = 48f;
    private float healthBarSkew = 10f;
    private float buffIconSize = 24f;
    private float buffIconSpacing = 4f;

    // Positioning
    private float posX;
    private float posY;

    // References
    private Player player;
    private Viewport hudViewport;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    // Colors
    private final Color healthFillColor = new Color(0.7f, 0.1f, 0.1f, 1f);
    private final Color healthBackgroundColor = new Color(0.25f, 0.25f, 0.25f, 1f);
    private final Color healthTextColor = Color.WHITE;
    private final Color healthTextShadowColor = new Color(0f, 0f, 0f, 0.7f);

    public PlayerStatusUI(Player player, Viewport hudViewport) {
        this.player = player;
        this.hudViewport = hudViewport;
        this.glyphLayout = new GlyphLayout();

        loadTextures();
        updatePosition();
    }

    private void loadTextures() {
        // Load circle frames
        try {
            mercenaryCircleTexture = Storage.assetManager.get("ui/MercenaryCircle.png", Texture.class);
        } catch (Exception e) {
            System.err.println("Failed to load MercenaryCircle.png: " + e.getMessage());
        }

        try {
            paladinCircleTexture = Storage.assetManager.get("ui/PaladinCircle.png", Texture.class);
        } catch (Exception e) {
            System.err.println("Failed to load PaladinCircle.png: " + e.getMessage());
        }

        // Load character portraits
        try {
            mercenaryPortraitTexture = Storage.assetManager.get("character/Mercenary/Mercenary.png", Texture.class);
        } catch (Exception e) {
            System.err.println("Failed to load mercenary portrait: " + e.getMessage());
        }

        try {
            paladinPortraitTexture = Storage.assetManager.get("character/Paladin/Gobbo.png", Texture.class);
        } catch (Exception e) {
            System.err.println("Failed to load paladin portrait: " + e.getMessage());
        }

        whitePixel = Storage.assetManager.get("white_pixel.png", Texture.class);

        font = Storage.assetManager.get("fonts/CascadiaBold.fnt", BitmapFont.class);
    }

    public void updatePosition() {
        posX = padding;
        posY = hudViewport.getWorldHeight() - padding - circleSize;
    }
    public void render(SpriteBatch batch) {
        if (player == null) return;

        // Calculate positions
        float circleX = posX;
        float circleY = posY;

        // Health bar positioned to the right of the circle, vertically centered
        float healthBarX = circleX + circleSize + padding;
        float healthBarY = circleY + (circleSize - healthBarHeight) / 2f + 8f;

        float buffAreaX = healthBarX;
        float buffAreaY = healthBarY - buffIconSize - padding;

        renderCircleFrame(batch, circleX, circleY);

        renderPortrait(batch, circleX, circleY);

        renderHealthBar(batch, healthBarX, healthBarY);

        renderBuffIcons(batch, buffAreaX, buffAreaY);
    }

    private void renderPortrait(SpriteBatch batch, float x, float y) {
        Texture portraitTexture = getPortraitTexture();
        if (portraitTexture == null) return;

        float portraitSize = circleSize * 0.7f;
        float portraitOffsetX = (circleSize - portraitSize) / 2f;
        float portraitOffsetY = (circleSize - portraitSize) / 2f;

        batch.setColor(Color.WHITE);
        batch.draw(portraitTexture,
                x + portraitOffsetX,
                y + portraitOffsetY + 5,
                portraitSize,
                portraitSize);
    }

    private void renderCircleFrame(SpriteBatch batch, float x, float y) {
        Texture circleTexture = getCircleTexture();
        if (circleTexture == null) return;

        batch.setColor(Color.WHITE);
        batch.draw(circleTexture, x, y, circleSize, circleSize);
    }

    private void renderHealthBar(SpriteBatch batch, float x, float y) {
        float healthPercent = player.getStats().getHealthPercentage();
        int currentHealth = player.getStats().getCurrentHealth();
        int maxHealth = player.getStats().getMaxHealth();

        // Parallelogram dimensions
        float innerPadding = 3f;
        float innerWidth = healthBarWidth - (innerPadding * 2);
        float innerHeight = healthBarHeight - (innerPadding * 2);
        float innerSkew = healthBarSkew * (innerWidth / healthBarWidth);

        // Draw background (grey - missing health) as parallelogram
        batch.setColor(healthBackgroundColor);
        drawParallelogram(batch, x + innerPadding, y + innerPadding, innerWidth, innerHeight, innerSkew);

        // Draw health fill (red) as parallelogram
        if (healthPercent > 0) {
            batch.setColor(healthFillColor);
            float fillWidth = innerWidth * healthPercent;
            float fillSkew = innerSkew * healthPercent;
            drawParallelogram(batch, x + innerPadding, y + innerPadding, fillWidth, innerHeight, fillSkew);
        }

        // Draw health text with shadow for better readability
        String healthText = currentHealth + "/" + maxHealth;
        font.getData().setScale(0.45f);
        glyphLayout.setText(font, healthText);

        float textX = x + (healthBarWidth - glyphLayout.width) / 2f + (healthBarSkew / 2f);
        float textY = y + (healthBarHeight + glyphLayout.height) / 2f;

        // Draw text shadow
        font.setColor(healthTextShadowColor);
        font.draw(batch, healthText, textX + 1, textY - 1);

        // Draw text
        font.setColor(healthTextColor);
        font.draw(batch, healthText, textX, textY);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void drawParallelogram(SpriteBatch batch, float x, float y, float width, float height, float skew) {
        if (whitePixel == null || width <= 0 || height <= 0) return;

        // Draw parallelogram as horizontal line segments with progressive offset
        int segments = Math.max(1, (int) height);
        float segmentHeight = height / segments;

        for (int i = 0; i < segments; i++) {
            float progress = (float) i / (segments - 1 > 0 ? segments - 1 : 1);
            float offsetX = skew * progress;
            float segmentY = y + i * segmentHeight;

            batch.draw(whitePixel, x + offsetX, segmentY, width, segmentHeight + 0.5f);
        }
    }

    private void renderBuffIcons(SpriteBatch batch, float x, float y) {
        BuffManager buffManager = player.getBuffManager();
        if (buffManager == null) {
            batch.setColor(Color.WHITE);
            return;
        }

        buffManager.renderAt(batch, x, y, buffIconSize, buffIconSpacing);

        batch.setColor(Color.WHITE);
    }

    private Texture getCircleTexture() {
        if (player.getPlayerClass() == PlayerClass.PALADIN) {
            return paladinCircleTexture;
        }
        return mercenaryCircleTexture;
    }

    private Texture getPortraitTexture() {
        if (player.getPlayerClass() == PlayerClass.PALADIN) {
            return paladinPortraitTexture;
        }
        return mercenaryPortraitTexture;
    }

    // Setters for customization

    public void setCircleSize(float size) {
        this.circleSize = size;
        updatePosition();
    }

    public void setPadding(float padding) {
        this.padding = padding;
        updatePosition();
    }

    public void setHealthBarWidth(float width) {
        this.healthBarWidth = width;
    }

    public void setHealthBarHeight(float height) {
        this.healthBarHeight = height;
    }

    public void setHealthBarSkew(float skew) {
        this.healthBarSkew = skew;
    }

    public void setBuffIconSize(float size) {
        this.buffIconSize = size;
    }

    public void setBuffIconSpacing(float spacing) {
        this.buffIconSpacing = spacing;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    // Getters

    public float getCircleSize() {
        return circleSize;
    }

    public float getTotalWidth() {
        return circleSize + padding + healthBarWidth;
    }

    public float getTotalHeight() {
        return circleSize;
    }

    public void resize() {
        updatePosition();
    }

    public void dispose() {
        // Textures are managed by AssetManager, so we don't dispose them here
    }
}