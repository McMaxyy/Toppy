package ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import config.SaveManager;
import config.Storage;
import items.Item;
import managers.Inventory;
import managers.ItemRegistry;

public class SafeStashPopup {
    private static final int STASH_SLOTS = 12;
    private static final int ITEMS_PER_ROW = 4;

    private final int SLOT_SIZE = 50;
    private final int SLOT_PADDING = 10;
    private final int UI_PADDING = 20;
    private final int TITLE_HEIGHT = 30;
    private final int CLOSE_BUTTON_SIZE = 22;

    private final Color SLOT_COLOR = new Color(0.3f, 0.3f, 0.4f, 0.9f);
    private final Color SLOT_BORDER_COLOR = new Color(0.6f, 0.6f, 0.7f, 1f);
    private final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.15f, 0.95f);
    private final Color CLOSE_BUTTON_COLOR = new Color(0.4f, 0.2f, 0.2f, 0.9f);
    private final Color CLOSE_BUTTON_HOVER_COLOR = new Color(0.6f, 0.3f, 0.3f, 0.95f);
    private final Color DESCRIPTION_COLOR = new Color(1f, 0.4f, 0.7f, 1f);

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout;
    private final Item[] stashItems;
    private boolean open = true;

    public SafeStashPopup(String[] stashItemIds) {
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.glyphLayout = new GlyphLayout();
        this.stashItems = new Item[STASH_SLOTS];
        loadItems(stashItemIds);
    }

    public boolean isOpen() {
        return open;
    }

    public void update(float delta, Inventory inventory) {
        if (!open) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            open = false;
            return;
        }

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float panelWidth = UI_PADDING * 2 + ITEMS_PER_ROW * SLOT_SIZE + (ITEMS_PER_ROW - 1) * SLOT_PADDING;
        int rows = (int) Math.ceil(STASH_SLOTS / (float) ITEMS_PER_ROW);
        float panelHeight = UI_PADDING * 2 + TITLE_HEIGHT + rows * SLOT_SIZE + (rows - 1) * SLOT_PADDING;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;
        float closeX = panelX + panelWidth - UI_PADDING - CLOSE_BUTTON_SIZE;
        float closeY = panelY + panelHeight - UI_PADDING - CLOSE_BUTTON_SIZE + 2f;

        int screenHeightPx = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeightPx - Gdx.input.getY();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) &&
                isPointInRect(mouseX, mouseY, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)) {
            open = false;
            return;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            int clickedSlot = getSlotAtPosition(mouseX, mouseY);
            if (clickedSlot >= 0 && stashItems[clickedSlot] != null && inventory != null) {
                boolean added = inventory.addItem(stashItems[clickedSlot]);
                if (added) {
                    stashItems[clickedSlot] = null;
                    syncSaveData();
                }
            }
        }
    }

    public void render(SpriteBatch batch, OrthographicCamera hudCamera) {
        if (!open) return;

        float screenWidth = hudCamera.viewportWidth;
        float screenHeight = hudCamera.viewportHeight;
        float panelWidth = UI_PADDING * 2 + ITEMS_PER_ROW * SLOT_SIZE + (ITEMS_PER_ROW - 1) * SLOT_PADDING;
        int rows = (int) Math.ceil(STASH_SLOTS / (float) ITEMS_PER_ROW);
        float panelHeight = UI_PADDING * 2 + TITLE_HEIGHT + rows * SLOT_SIZE + (rows - 1) * SLOT_PADDING;

        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        float closeX = panelX + panelWidth - UI_PADDING - CLOSE_BUTTON_SIZE;
        float closeY = panelY + panelHeight - UI_PADDING - CLOSE_BUTTON_SIZE + 2f;

        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - TITLE_HEIGHT - SLOT_SIZE;

        int screenHeightPx = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeightPx - Gdx.input.getY();

        Item hoveredItem = null;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < STASH_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;

            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);

            boolean isHovered = isPointInRect(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            shapeRenderer.setColor(isHovered ? new Color(0.4f, 0.6f, 0.4f, 0.9f) : SLOT_COLOR);
            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            if (isHovered) {
                hoveredItem = stashItems[i];
            }
        }
        shapeRenderer.end();

        boolean closeHovered = isPointInRect(mouseX, mouseY, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(closeHovered ? CLOSE_BUTTON_HOVER_COLOR : CLOSE_BUTTON_COLOR);
        shapeRenderer.rect(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < STASH_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);
            Color borderColor = SLOT_BORDER_COLOR;
            if (stashItems[i] != null && stashItems[i].getGearType() != null) {
                borderColor = getGearTypeColor(stashItems[i].getGearType());
            }
            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
        }
        shapeRenderer.end();

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(0.8f);
        font.draw(batch, "Storage", panelX + UI_PADDING, panelY + panelHeight - UI_PADDING / 2f);
        font.getData().setScale(0.7f);
        font.draw(batch, "X", closeX + 6f, closeY + 18f);

        for (int i = 0; i < STASH_SLOTS; i++) {
            if (stashItems[i] == null) continue;
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);
            stashItems[i].renderIcon(batch, slotX + 5, slotY + 5, SLOT_SIZE - 10);
        }

        if (hoveredItem != null) {
            renderItemInfo(batch, hoveredItem, panelX, panelY, panelWidth, panelHeight, hudCamera);
        }
        font.getData().setScale(1.0f);
        batch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    private int getSlotAtPosition(float mouseX, float mouseY) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float panelWidth = UI_PADDING * 2 + ITEMS_PER_ROW * SLOT_SIZE + (ITEMS_PER_ROW - 1) * SLOT_PADDING;
        int rows = (int) Math.ceil(STASH_SLOTS / (float) ITEMS_PER_ROW);
        float panelHeight = UI_PADDING * 2 + TITLE_HEIGHT + rows * SLOT_SIZE + (rows - 1) * SLOT_PADDING;

        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;
        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - TITLE_HEIGHT - SLOT_SIZE;

        for (int i = 0; i < STASH_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);
            if (isPointInRect(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    private void loadItems(String[] stashItemIds) {
        ItemRegistry registry = ItemRegistry.getInstance();
        if (stashItemIds == null) return;

        for (int i = 0; i < STASH_SLOTS && i < stashItemIds.length; i++) {
            if (stashItemIds[i] == null) continue;
            stashItems[i] = registry.createItem(stashItemIds[i], new Vector2(0f, 0f));
        }
    }

    private void syncSaveData() {
        String[] slots = new String[STASH_SLOTS];
        for (int i = 0; i < STASH_SLOTS; i++) {
            if (stashItems[i] != null) {
                slots[i] = stashItems[i].getItemId();
            }
        }
        SaveManager.setSafeStashSlots(slots);
    }

    private void renderItemInfo(SpriteBatch batch, Item item, float panelX, float panelY, float panelWidth,
                                float panelHeight, OrthographicCamera hudCamera) {
        float screenWidth = hudCamera.viewportWidth;
        float infoX = panelX + panelWidth + UI_PADDING;
        if (infoX + 220f > screenWidth) {
            infoX = panelX + UI_PADDING;
        }
        float infoY = panelY + panelHeight - UI_PADDING - 20f;
        float padding = 10f;
        float lineSpacing = 20f;

        float maxWidth = 0f;
        float totalHeight = 0f;

        font.getData().setScale(0.8f);
        glyphLayout.setText(font, item.getName());
        maxWidth = Math.max(maxWidth, glyphLayout.width);
        totalHeight += glyphLayout.height;

        font.getData().setScale(0.6f);

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            glyphLayout.setText(font, item.getDescription());
            maxWidth = Math.max(maxWidth, glyphLayout.width);
            totalHeight += lineSpacing;
        }

        if (item.getDamage() > 0) {
            glyphLayout.setText(font, "Damage: " + item.getDamage());
            maxWidth = Math.max(maxWidth, glyphLayout.width);
            totalHeight += lineSpacing;
        }

        if (item.getDefense() > 0) {
            glyphLayout.setText(font, "Defense: " + item.getDefense());
            maxWidth = Math.max(maxWidth, glyphLayout.width);
            totalHeight += lineSpacing;
        }

        if (item.getBonusVitality() > 0) {
            glyphLayout.setText(font, "Vitality: " + item.getBonusVitality());
            maxWidth = Math.max(maxWidth, glyphLayout.width);
            totalHeight += lineSpacing;
        } else if (item.getHealthRestore() > 0) {
            glyphLayout.setText(font, "Health: " + item.getHealthRestore());
            maxWidth = Math.max(maxWidth, glyphLayout.width);
            totalHeight += lineSpacing;
        }

        if (item.getBonusDex() > 0) {
            glyphLayout.setText(font, "Dexterity: " + item.getBonusDex());
            maxWidth = Math.max(maxWidth, glyphLayout.width);
            totalHeight += lineSpacing;
        }

        float rectX = infoX - padding;
        float rectY = infoY - totalHeight - padding;
        float rectWidth = maxWidth + padding * 2f;
        float rectHeight = totalHeight + padding * 2f;

        batch.end();
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(rectX, rectY, rectWidth, rectHeight);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(rectX, rectY, rectWidth, rectHeight);
        shapeRenderer.end();
        batch.begin();

        font.setColor(Color.WHITE);
        font.getData().setScale(0.8f);
        font.draw(batch, item.getName(), infoX, infoY);

        font.getData().setScale(0.6f);

        int lineOffset = 0;

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            font.setColor(DESCRIPTION_COLOR);
            font.draw(batch, item.getDescription(), infoX, infoY - 25 - (lineOffset * lineSpacing));
            lineOffset++;
        }

        if (item.getDamage() > 0) {
            font.setColor(Color.ORANGE);
            font.draw(batch, "Damage: " + item.getDamage(), infoX, infoY - 25 - (lineOffset * lineSpacing));
            lineOffset++;
        }

        if (item.getDefense() > 0) {
            font.setColor(Color.CYAN);
            font.draw(batch, "Defense: " + item.getDefense(), infoX, infoY - 25 - (lineOffset * lineSpacing));
            lineOffset++;
        }

        if (item.getBonusVitality() > 0) {
            font.setColor(Color.RED);
            font.draw(batch, "Vitality: " + item.getBonusVitality(), infoX, infoY - 25 - (lineOffset * lineSpacing));
            lineOffset++;
        } else if (item.getHealthRestore() > 0) {
            font.setColor(Color.RED);
            font.draw(batch, "Health: " + item.getHealthRestore(), infoX, infoY - 25 - (lineOffset * lineSpacing));
            lineOffset++;
        }

        if (item.getBonusDex() > 0) {
            font.setColor(Color.GREEN);
            font.draw(batch, "Dexterity: " + item.getBonusDex(), infoX, infoY - 25 - (lineOffset * lineSpacing));
            lineOffset++;
        }

        font.setColor(Color.WHITE);
    }

    private Color getGearTypeColor(String gearType) {
        if (gearType == null) return SLOT_BORDER_COLOR;

        switch (gearType) {
            case ItemRegistry.VALKYRIE:
                return new Color(1f, 0.85f, 0f, 1f);
            case ItemRegistry.PROTECTOR:
                return new Color(1f, 1f, 1f, 1f);
            case ItemRegistry.BARBARIAN:
                return new Color(0.6f, 0.4f, 0.2f, 1f);
            case ItemRegistry.BERSERKER:
                return new Color(0.9f, 0.2f, 0.2f, 1f);
            case ItemRegistry.DECEPTOR:
                return new Color(0.7f, 0.3f, 0.9f, 1f);
            case ItemRegistry.SPECIAL:
                return new Color(1f, 0.4f, 0.7f, 1f);
            default:
                return SLOT_BORDER_COLOR;
        }
    }
}
