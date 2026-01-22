package managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import config.Storage;
import entities.Player;
import entities.PlayerStats;
import items.Item;
import managers.Equipment.EquipmentSlot;

/**
 * Character panel with equipment and stats
 */
public class CharacterPanel {
    private boolean characterPanelOpen = false;
    private Equipment equipment;
    private EquipmentSlot selectedEquipmentSlot = null;

    // UI components
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Texture characterSprite;
    private final Texture slotTexture;

    // UI settings
    private final int SLOT_SIZE = 50;
    private final int SLOT_PADDING = 10;
    private final int UI_PADDING = 20;
    private final Color SELECTED_COLOR = new Color(1f, 1f, 0.5f, 1f);
    private final Color SLOT_COLOR = new Color(0.3f, 0.3f, 0.4f, 0.9f);
    private final Color SLOT_BORDER_COLOR = new Color(0.6f, 0.6f, 0.7f, 1f);
    private final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.15f, 0.95f);
    private final Color BUTTON_COLOR = new Color(0.3f, 0.5f, 0.3f, 1f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.4f, 0.6f, 0.4f, 1f);
    private final Color BUTTON_DISABLED_COLOR = new Color(0.2f, 0.2f, 0.2f, 0.5f);

    // Button hover states
    private String hoveredButton = null;
    private final int STAT_BUTTON_SIZE = 25;

    public CharacterPanel(Equipment equipment) {
        this.equipment = equipment;
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.characterSprite = Storage.assetManager.get("character/Sprite-0002.png", Texture.class);
        this.slotTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
    }

    public void toggleCharacterPanel() {
        characterPanelOpen = !characterPanelOpen;
        if (!characterPanelOpen) {
            selectedEquipmentSlot = null;
        }
    }

    public boolean isOpen() {
        return characterPanelOpen;
    }

    public void update(float delta, Player player) {
        if (!characterPanelOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                toggleCharacterPanel();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            toggleCharacterPanel();
            return;
        }

        // Handle mouse input for equipment slots and stat buttons
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int panelWidth = 600;
        int panelHeight = 400;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        // Convert mouse coordinates
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        // Reset hover state
        hoveredButton = null;

        // Check equipment slots
        checkEquipmentMouseHover(mouseX, mouseY, panelX, panelY, panelHeight, player);

        // Check stat buttons
        checkStatButtonsMouseHover(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight, player);
    }

    private void checkEquipmentMouseHover(float mx, float my, float px, float py, float ph, Player player) {
        float equipmentX = px + UI_PADDING;
        float equipmentY = py + UI_PADDING + 40;

        EquipmentSlot[] slots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
                EquipmentSlot.BOOTS,
                EquipmentSlot.WEAPON,
                EquipmentSlot.OFFHAND
        };

        float startX = equipmentX + 10;
        float startY = equipmentY + ph - 180;

        for (int i = 0; i < slots.length; i++) {
            float slotY = startY - i * (SLOT_SIZE + 8);
            if (mx >= startX && mx <= startX + SLOT_SIZE && my >= slotY && my <= slotY + SLOT_SIZE) {
                selectedEquipmentSlot = slots[i];
                // Left click to unequip
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    Item unequippedItem = equipment.unequipItem(slots[i], player);
                    if (unequippedItem != null) {
                        // Add to inventory
                        player.getInventory().addItem(unequippedItem);
                    }
                }
            }
        }
    }

    private void checkStatButtonsMouseHover(float mx, float my, float px, float py, float pw, float ph, Player player) {
        if (player == null) return;

        float statsX = px + pw - 220;
        float statsY = py + ph - 100;

        int availablePoints = player.getStats().getAvailableStatPoints();

        // Only show plus buttons when points are available
        if (availablePoints > 0) {
            // HP plus button
            float hpY = statsY - 30;
            float plusX = statsX + 180;

            if (mx >= plusX && mx <= plusX + STAT_BUTTON_SIZE && my >= hpY && my <= hpY + STAT_BUTTON_SIZE) {
                hoveredButton = "hp_plus";
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    player.getStats().allocateHealthPoint();
                }
            }

            // AP plus button
            float apY = statsY - 60;
            if (mx >= plusX && mx <= plusX + STAT_BUTTON_SIZE && my >= apY && my <= apY + STAT_BUTTON_SIZE) {
                hoveredButton = "ap_plus";
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    player.getStats().allocateAttackPoint();
                }
            }

            // DP plus button
            float dpY = statsY - 90;
            if (mx >= plusX && mx <= plusX + STAT_BUTTON_SIZE && my >= dpY && my <= dpY + STAT_BUTTON_SIZE) {
                hoveredButton = "dp_plus";
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    player.getStats().allocateDefensePoint();
                }
            }
        }

        // Reset button (only if points allocated)
        float resetX = statsX + 30;
        float resetY = statsY - 130;
        float resetW = 100;
        float resetH = 25;

        boolean hasAllocatedPoints = player.getStats().getAllocatedHealthPoints() > 0 ||
                player.getStats().getAllocatedAttackPoints() > 0 ||
                player.getStats().getAllocatedDefensePoints() > 0;

        if (hasAllocatedPoints) {
            if (mx >= resetX && mx <= resetX + resetW && my >= resetY && my <= resetY + resetH) {
                hoveredButton = "reset";
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    player.getStats().resetStatPoints();
                }
            }
        }
    }

    public void render(SpriteBatch batch, boolean batchIsActive, Player player) {
        if (!characterPanelOpen || player == null) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Calculate panel dimensions
        int panelWidth = 600;
        int panelHeight = 400;

        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        if (batchIsActive) {
            batch.end();
        }

        // Draw background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Draw border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        // Render equipment panel (left side)
        renderEquipmentPanel(batch, panelX, panelY, panelHeight);

        // Render character sprite (middle)
        renderCharacterSprite(batch, panelX, panelY, panelWidth, panelHeight);

        // Render stats panel (right side)
        renderStatsPanel(batch, panelX, panelY, panelWidth, panelHeight, player);

        // Begin batch for text
        batch.begin();

        // Title
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        font.draw(batch, "Character", panelX + UI_PADDING,
                panelY + panelHeight - UI_PADDING);

        font.getData().setScale(1.0f);

        batch.end();
    }

    private void renderEquipmentPanel(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float equipmentX = panelX + UI_PADDING;
        float equipmentY = panelY + UI_PADDING + 40;

        // Draw equipment panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.9f);
        shapeRenderer.rect(equipmentX, equipmentY, 140, panelHeight - 100);
        shapeRenderer.end();

        // Draw equipment slots
        EquipmentSlot[] slots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
                EquipmentSlot.BOOTS,
                EquipmentSlot.WEAPON,
                EquipmentSlot.OFFHAND
        };

        String[] slotLabels = {
                "Head", "Body", "Hand", "Feet", "Wpn", "Off"
        };

        float startX = equipmentX + 10;
        float startY = equipmentY + panelHeight - 180;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < slots.length; i++) {
            float slotY = startY - i * (SLOT_SIZE + 8);

            // Highlight selected slot
            if (selectedEquipmentSlot == slots[i]) {
                shapeRenderer.setColor(SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(SLOT_COLOR);
            }

            shapeRenderer.rect(startX, slotY, SLOT_SIZE, SLOT_SIZE);

            // Border
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(selectedEquipmentSlot == slots[i] ? 3 : 2);
            shapeRenderer.setColor(SLOT_BORDER_COLOR);
            shapeRenderer.rect(startX, slotY, SLOT_SIZE, SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        shapeRenderer.end();

        // Draw equipped items
        batch.begin();

        for (int i = 0; i < slots.length; i++) {
            Item equippedItem = equipment.getEquippedItem(slots[i]);
            if (equippedItem != null) {
                float slotY = startY - i * (SLOT_SIZE + 8);
                equippedItem.renderIcon(batch, startX + 2, slotY + 2, SLOT_SIZE - 4);
            }
        }

        // Draw slot labels
        font.getData().setScale(0.6f);
        font.setColor(Color.LIGHT_GRAY);

        for (int i = 0; i < slots.length; i++) {
            float slotY = startY - i * (SLOT_SIZE + 8);
            font.draw(batch, slotLabels[i], startX + SLOT_SIZE + 5, slotY + SLOT_SIZE / 2 + 5);
        }

        font.getData().setScale(1.0f);

        batch.end();
    }

    private void renderCharacterSprite(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        float charX = panelX + 180;
        float charY = panelY + UI_PADDING + 50;
        float charSize = 120;

        batch.begin();
        batch.draw(characterSprite, charX, charY, charSize, charSize);
        batch.end();
    }

    private void renderStatsPanel(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight, Player player) {
        float statsX = panelX + panelWidth - 220;
        float statsY = panelY + panelHeight - 60;

        // Draw stats panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.9f);
        shapeRenderer.rect(statsX, statsY - 180, 200, 200);
        shapeRenderer.end();

        // Draw border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(statsX, statsY - 180, 200, 200);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        // Get player stats
        PlayerStats stats = player.getStats();
        int availablePoints = stats.getAvailableStatPoints();

        // Draw stat buttons
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Only show plus buttons when points are available
        if (availablePoints > 0) {
            // HP plus button
            float hpY = statsY - 30;
            drawStatButton(statsX + 180, hpY, true, "hp_plus");

            // AP plus button
            float apY = statsY - 60;
            drawStatButton(statsX + 180, apY, true, "ap_plus");

            // DP plus button
            float dpY = statsY - 90;
            drawStatButton(statsX + 180, dpY, true, "dp_plus");
        }

        // Reset button (only if points allocated)
        float resetY = statsY - 130;
        boolean hasAllocatedPoints = stats.getAllocatedHealthPoints() > 0 ||
                stats.getAllocatedAttackPoints() > 0 ||
                stats.getAllocatedDefensePoints() > 0;

        if (hasAllocatedPoints) {
            if ("reset".equals(hoveredButton)) {
                shapeRenderer.setColor(0.6f, 0.4f, 0.4f, 1f);
            } else {
                shapeRenderer.setColor(new Color(0.5f, 0.3f, 0.3f, 1f));
            }
            shapeRenderer.rect(statsX + 30, resetY, 100, 25);
        }

        shapeRenderer.end();

        // Draw button borders
        if (availablePoints > 0) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(SLOT_BORDER_COLOR);

            float hpY = statsY - 30;
            shapeRenderer.rect(statsX + 180, hpY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);

            float apY = statsY - 60;
            shapeRenderer.rect(statsX + 180, apY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);

            float dpY = statsY - 90;
            shapeRenderer.rect(statsX + 180, dpY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);

            shapeRenderer.end();
        }

        if (hasAllocatedPoints) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(SLOT_BORDER_COLOR);
            shapeRenderer.rect(statsX + 30, resetY, 100, 25);
            shapeRenderer.end();
        }

        // Draw stats text
        batch.begin();

        font.setColor(Color.WHITE);
        font.getData().setScale(0.8f);

        // Available points
        if (availablePoints > 0) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Points: " + availablePoints, statsX + 10, statsY - 5);
            font.setColor(Color.WHITE);
        }

        // Level and experience
        font.setColor(Color.GOLD);
        font.draw(batch, "Level " + stats.getLevel(), statsX + 10, statsY - 160);
        font.setColor(Color.LIGHT_GRAY);
        font.getData().setScale(0.6f);
        font.draw(batch, "EXP: " + stats.getExperience() + "/" + stats.getExpToNextLevel(),
                statsX + 10, statsY - 175);

        font.getData().setScale(0.7f);
        font.setColor(Color.WHITE);

        // HP (current/max)
        float hpY = statsY - 30;
        font.draw(batch, "Health:", statsX + 10, hpY + 18);
        font.draw(batch, stats.getCurrentHealth() + "/" + stats.getMaxHealth(), statsX + 70, hpY + 18);
        if (availablePoints > 0) {
            font.draw(batch, "+", statsX + 185, hpY + 18);
        }

        // AP (attack power - includes weapon damage)
        float apY = statsY - 60;
        font.draw(batch, "Attack:", statsX + 10, apY + 18);
        font.draw(batch, String.valueOf(stats.getTotalDamage()), statsX + 70, apY + 18);
        if (availablePoints > 0) {
            font.draw(batch, "+", statsX + 185, apY + 18);
        }

        // DP (defense power - includes armor defense)
        float dpY = statsY - 90;
        font.draw(batch, "Defense:", statsX + 10, dpY + 18);
        font.draw(batch, String.valueOf(stats.getTotalDefense()), statsX + 70, dpY + 18);
        if (availablePoints > 0) {
            font.draw(batch, "+", statsX + 185, dpY + 18);
        }

        // Reset button text
        if (hasAllocatedPoints) {
            font.setColor(Color.WHITE);
            font.draw(batch, "RESET", statsX + 60, resetY + 18);
        }

        font.getData().setScale(1.0f);

        batch.end();
    }

    private void drawStatButton(float x, float y, boolean enabled, String buttonId) {
        if (enabled) {
            if (buttonId.equals(hoveredButton)) {
                shapeRenderer.setColor(BUTTON_HOVER_COLOR);
            } else {
                shapeRenderer.setColor(BUTTON_COLOR);
            }
        } else {
            shapeRenderer.setColor(BUTTON_DISABLED_COLOR);
        }
        shapeRenderer.rect(x, y, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);
    }

    public EquipmentSlot getSelectedEquipmentSlot() {
        return selectedEquipmentSlot;
    }

    public void setSelectedEquipmentSlot(EquipmentSlot slot) {
        this.selectedEquipmentSlot = slot;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    public Equipment getEquipment() {
        return equipment;
    }
}