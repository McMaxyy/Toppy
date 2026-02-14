package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import config.Storage;
import entities.*;

public class BossHealthUI {
    private final Viewport hudViewport;
    private Texture healthBarTexture;
    private BitmapFont font;

    // Boss references
    private Herman herman;
    private Herman hermanDuplicate;
    private BossKitty bossKitty;
    private Cyclops cyclops;
    private GhostBoss ghostBoss;

    // Bar dimensions
    private static final float BAR_WIDTH_RATIO = 1f / 3f;
    private static final float BAR_HEIGHT = 25f;
    private static final float TOP_MARGIN = 30f;
    private static final float BAR_SPACING = 20f;

    public BossHealthUI(Viewport hudViewport) {
        this.hudViewport = hudViewport;

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        this.font = Storage.assetManager.get("fonts/CascadiaBold.fnt", BitmapFont.class);
    }

    public void setHerman(Herman herman) {
        this.herman = herman;
    }

    public void setHermanDuplicate(Herman hermanDuplicate) {
        this.hermanDuplicate = hermanDuplicate;
    }

    public void setBossKitty(BossKitty bossKitty) {
        this.bossKitty = bossKitty;
    }

    public void setCyclops(Cyclops cyclops) {
        this.cyclops = cyclops;
    }

    public void setGhostBoss(GhostBoss ghostBoss) {
        this.ghostBoss = ghostBoss;
    }

    public void clearAll() {
        this.herman = null;
        this.hermanDuplicate = null;
        this.bossKitty = null;
        this.cyclops = null;
        this.ghostBoss = null;
    }

    public void render(SpriteBatch batch) {
        float screenWidth = hudViewport.getWorldWidth();
        float screenHeight = hudViewport.getWorldHeight();

        int barIndex = 0;

        // Render Herman
        if (herman != null && !herman.isMarkedForRemoval() && herman.isActivated()) {
            renderHermanBar(batch, screenWidth, screenHeight, barIndex);
            barIndex++;
        }

        // Render Herman Duplicate
        if (hermanDuplicate != null && !hermanDuplicate.isMarkedForRemoval() && hermanDuplicate.isActivated()) {
            renderHermanDuplicateBar(batch, screenWidth, screenHeight, barIndex);
            barIndex++;
        }

        // Render BossKitty
        if (bossKitty != null && !bossKitty.isMarkedForRemoval()) {
            renderBossKittyBar(batch, screenWidth, screenHeight, barIndex);
            barIndex++;
        }

        // Render Cyclops
        if (cyclops != null && !cyclops.isMarkedForRemoval()) {
            renderCyclopsBar(batch, screenWidth, screenHeight, barIndex);
            barIndex++;
        }

        // Render GhostBoss
        if (ghostBoss != null && !ghostBoss.isMarkedForRemoval()) {
            renderGhostBossBar(batch, screenWidth, screenHeight, barIndex);
            barIndex++;
        }
    }

    private void renderHermanBar(SpriteBatch batch, float screenWidth, float screenHeight, int barIndex) {
        float barWidth = screenWidth * BAR_WIDTH_RATIO;
        float barX = (screenWidth - barWidth) / 2f;
        float barY = screenHeight - BAR_HEIGHT - TOP_MARGIN - (barIndex * (BAR_HEIGHT + BAR_SPACING));

        // Background
        batch.setColor(0.3f, 0.2f, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, BAR_HEIGHT);

        // Health bar
        float healthPercent = herman.getStats().getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        float red = 0.6f - healthPercent * 0.3f;
        float green = 0.4f + healthPercent * 0.4f;
        float blue = 0.2f;
        batch.setColor(red, green, blue, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, BAR_HEIGHT);

        // Special cooldown indicator (if using special attack)
        if (herman.isUsingSpecialAttack()) {
            batch.setColor(0.8f, 0.4f, 0.1f, 0.7f);
            batch.draw(healthBarTexture, barX, barY - 8f, barWidth * 0.3f, BAR_HEIGHT * 0.5f);
        }

        // Boss name label
        font.setColor(Color.WHITE);
        font.getData().setScale(0.5f);
        font.draw(batch, "Herman", barX + 5f, barY + BAR_HEIGHT + 15f);
        font.getData().setScale(1f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderHermanDuplicateBar(SpriteBatch batch, float screenWidth, float screenHeight, int barIndex) {
        float barWidth = screenWidth * BAR_WIDTH_RATIO;
        float barX = (screenWidth - barWidth) / 2f;
        float barY = screenHeight - BAR_HEIGHT - TOP_MARGIN - (barIndex * (BAR_HEIGHT + BAR_SPACING));

        // Background - slightly darker
        batch.setColor(0.25f, 0.17f, 0.08f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, BAR_HEIGHT);

        // Health bar - slightly different color
        float healthPercent = hermanDuplicate.getStats().getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        float red = 0.5f - healthPercent * 0.25f;
        float green = 0.35f + healthPercent * 0.35f;
        float blue = 0.15f;
        batch.setColor(red, green, blue, 0.9f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, BAR_HEIGHT);

        // Boss name label
        font.setColor(new Color(0.9f, 0.9f, 0.7f, 1f));
        font.getData().setScale(0.5f);
        font.draw(batch, "Herman (Clone)", barX + 5f, barY + BAR_HEIGHT + 15f);
        font.getData().setScale(1f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderBossKittyBar(SpriteBatch batch, float screenWidth, float screenHeight, int barIndex) {
        float barWidth = screenWidth * BAR_WIDTH_RATIO;
        float barX = (screenWidth - barWidth) / 2f;
        float barY = screenHeight - BAR_HEIGHT - TOP_MARGIN - (barIndex * (BAR_HEIGHT + BAR_SPACING));

        // Background
        batch.setColor(0.2f, 0.1f, 0.2f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, BAR_HEIGHT);

        // Health bar - purple/pink gradient
        float healthPercent = bossKitty.getStats().getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        float red = 0.8f;
        float green = 0.3f + healthPercent * 0.2f;
        float blue = 0.6f;
        batch.setColor(red, green, blue, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, BAR_HEIGHT);

        // Boss name label
        font.setColor(new Color(1f, 0.7f, 0.9f, 1f));
        font.getData().setScale(0.5f);
        font.draw(batch, "Freydis", barX + 5f, barY + BAR_HEIGHT + 15f);
        font.getData().setScale(1f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderCyclopsBar(SpriteBatch batch, float screenWidth, float screenHeight, int barIndex) {
        float barWidth = screenWidth * BAR_WIDTH_RATIO;
        float barX = (screenWidth - barWidth) / 2f;
        float barY = screenHeight - BAR_HEIGHT - TOP_MARGIN - (barIndex * (BAR_HEIGHT + BAR_SPACING));

        // Background
        batch.setColor(0.2f, 0.15f, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, BAR_HEIGHT);

        // Health bar - orange/red gradient
        float healthPercent = cyclops.getStats().getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        float red = 0.9f;
        float green = 0.3f + healthPercent * 0.3f;
        float blue = 0.1f;
        batch.setColor(red, green, blue, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, BAR_HEIGHT);

        // Boss name label
        font.setColor(new Color(1f, 0.8f, 0.5f, 1f));
        font.getData().setScale(0.5f);
        font.draw(batch, "Cyclops", barX + 5f, barY + BAR_HEIGHT + 15f);
        font.getData().setScale(1f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderGhostBossBar(SpriteBatch batch, float screenWidth, float screenHeight, int barIndex) {
        float barWidth = screenWidth * BAR_WIDTH_RATIO;
        float barX = (screenWidth - barWidth) / 2f;
        float barY = screenHeight - BAR_HEIGHT - TOP_MARGIN - (barIndex * (BAR_HEIGHT + BAR_SPACING));

        // Background - dark purple
        batch.setColor(0.2f, 0.0f, 0.3f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, BAR_HEIGHT);

        // Health bar - purple gradient
        float healthPercent = ghostBoss.getStats().getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        float red = 0.6f + (1f - healthPercent) * 0.4f;
        float green = healthPercent * 0.3f;
        float blue = 0.8f;
        batch.setColor(red, green, blue, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, BAR_HEIGHT);

        if (ghostBoss.isSummoning()) {
            batch.setColor(0.5f, 0.2f, 0.8f, 0.7f);
            batch.draw(healthBarTexture, barX, barY - 8f, barWidth * 0.3f, BAR_HEIGHT * 0.5f);
        }

        font.setColor(new Color(0.9f, 0.7f, 1f, 1f));
        font.getData().setScale(0.5f);
        font.draw(batch, "Vengeful Spirit", barX + 5f, barY + BAR_HEIGHT + 15f);
        font.getData().setScale(1f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
    }
}