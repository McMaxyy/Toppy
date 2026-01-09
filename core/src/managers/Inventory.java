package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import config.Storage;
import items.Item;
import managers.Equipment.EquipmentSlot;

/**
 * Enhanced inventory system with equipment screen
 */
public class Inventory {
    private static final int MAX_SLOTS = 10;
    private static final int ITEMS_PER_ROW = 5;

    private Item[] items;
    private int coins;
    private boolean inventoryOpen = false;
    private int selectedSlot = 0;

    // Equipment system
    private Equipment equipment;
    private EquipmentSlot selectedEquipmentSlot = null;
    private boolean selectingEquipmentSlot = false;

    // UI components
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Texture slotTexture;
    private final Texture coinIconTexture;
    private final Texture characterSprite;

    // UI settings
    private final int SLOT_SIZE = 50;
    private final int SLOT_PADDING = 10;
    private final int UI_PADDING = 20;
    private final int EQUIPMENT_SLOT_SIZE = 45;
    private final Color SELECTED_COLOR = new Color(1f, 1f, 0.5f, 1f);
    private final Color EQUIPMENT_SELECTED_COLOR = new Color(0.5f, 1f, 0.5f, 1f);
    private final Color SLOT_COLOR = new Color(0.3f, 0.3f, 0.4f, 0.9f);
    private final Color EQUIPMENT_SLOT_COLOR = new Color(0.25f, 0.25f, 0.35f, 0.9f);
    private final Color SLOT_BORDER_COLOR = new Color(0.6f, 0.6f, 0.7f, 1f);
    private final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.15f, 0.95f);

    // Item counts for stacking
    private Map<Integer, Integer> itemCounts;

    public Inventory() {
        this.items = new Item[MAX_SLOTS];
        this.coins = 0;
        this.itemCounts = new HashMap<>();
        this.equipment = new Equipment();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.slotTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        this.coinIconTexture = Storage.assetManager.get("tiles/coin.png", Texture.class);
        this.characterSprite = Storage.assetManager.get("character/Sprite-0002.png", Texture.class);
    }

    public void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        if (!inventoryOpen) {
            selectingEquipmentSlot = false;
            selectedEquipmentSlot = null;
        }
    }

    public boolean isOpen() {
        return inventoryOpen;
    }

    public boolean addItem(Item item) {
        if (item.getType() == Item.ItemType.COIN) {
            coins += item.getValue();
            return true;
        }

        // Check if item can stack with existing items
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] != null && items[i].canStackWith(item)) {
                int currentCount = itemCounts.getOrDefault(i, 1);
                itemCounts.put(i, currentCount + 1);
                return true;
            }
        }

        // Find first empty slot
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] == null) {
                items[i] = item;
                itemCounts.put(i, 1);
                return true;
            }
        }

        return false;
    }

    public void removeItem(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS && items[slot] != null) {
            int count = itemCounts.getOrDefault(slot, 1);
            if (count > 1) {
                itemCounts.put(slot, count - 1);
            } else {
                items[slot] = null;
                itemCounts.remove(slot);
            }
        }
    }

    public Item getItem(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            return items[slot];
        }
        return null;
    }

    public int getItemCount(int slot) {
        return itemCounts.getOrDefault(slot, 0);
    }

    /**
     * Equip item from inventory to equipment slot
     */
    public void equipItemFromInventory(int inventorySlot, entities.Player player) {
        Item item = items[inventorySlot];
        if (item == null) return;

        // Only weapons and armor can be equipped
        if (item.getType() != Item.ItemType.WEAPON && item.getType() != Item.ItemType.ARMOR) {
            System.out.println("Cannot equip this item type");
            return;
        }

        // Equip the item (this returns the previously equipped item if any)
        Item previousItem = equipment.equipItem(item, player);

        // Remove from inventory
        removeItem(inventorySlot);

        // Add the previously equipped item back to inventory if there was one
        if (previousItem != null) {
            addItem(previousItem);
        }
    }

    /**
     * Unequip item from equipment slot to inventory
     */
    public void unequipItemToInventory(EquipmentSlot slot, entities.Player player) {
        if (isFull()) {
            System.out.println("Inventory is full! Cannot unequip item.");
            return;
        }

        Item item = equipment.unequipItem(slot, player);
        if (item != null) {
            addItem(item);
        }
    }

    public void useSelectedItem(entities.Player player) {
        if (selectingEquipmentSlot) {
            // Unequip from equipment slot
            if (selectedEquipmentSlot != null) {
                unequipItemToInventory(selectedEquipmentSlot, player);
            }
        } else {
            // Use/equip from inventory
            if (items[selectedSlot] != null) {
                Item item = items[selectedSlot];

                // If it's equipment, equip it
                if (item.getType() == Item.ItemType.WEAPON || item.getType() == Item.ItemType.ARMOR) {
                    equipItemFromInventory(selectedSlot, player);
                } else {
                    // It's a consumable, use it
                    item.use(player);
                    if (item.getType() == Item.ItemType.CONSUMABLE) {
                        removeItem(selectedSlot);
                    }
                }
            }
        }
    }

    public Item dropItem(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS && items[slot] != null) {
            Item droppedItem = items[slot].copy();
            removeItem(slot);
            return droppedItem;
        }
        return null;
    }

    public void addCoins(int amount) {
        coins += amount;
    }

    public boolean removeCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    public int getCoins() {
        return coins;
    }

    public void update(float delta) {
        if (!inventoryOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
                toggleInventory();
            }
            return;
        }

        // Close inventory
        if (Gdx.input.isKeyJustPressed(Input.Keys.B) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            toggleInventory();
            return;
        }

        // Switch between inventory and equipment with TAB
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            selectingEquipmentSlot = !selectingEquipmentSlot;
            if (selectingEquipmentSlot) {
                selectedEquipmentSlot = EquipmentSlot.HELMET; // Start at first equipment slot
            }
        }

        if (selectingEquipmentSlot) {
            // Navigate equipment slots
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                selectedEquipmentSlot = getNextEquipmentSlot(selectedEquipmentSlot, true);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                selectedEquipmentSlot = getNextEquipmentSlot(selectedEquipmentSlot, false);
            }
        } else {
            // Navigate inventory
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                selectedSlot = (selectedSlot + 1) % MAX_SLOTS;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                selectedSlot = (selectedSlot - 1 + MAX_SLOTS) % MAX_SLOTS;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                selectedSlot = (selectedSlot + ITEMS_PER_ROW) % MAX_SLOTS;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                selectedSlot = (selectedSlot - ITEMS_PER_ROW + MAX_SLOTS) % MAX_SLOTS;
            }
        }

        // Use/equip with E or Enter
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            // Handled by player
        }

        // Drop with Q
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            // Handled by player
        }
    }

    private EquipmentSlot getNextEquipmentSlot(EquipmentSlot current, boolean forward) {
        EquipmentSlot[] allSlots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.SHOULDERS,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
                EquipmentSlot.LEGS,
                EquipmentSlot.BOOTS,
                EquipmentSlot.WEAPON,
                EquipmentSlot.OFFHAND
        };

        for (int i = 0; i < allSlots.length; i++) {
            if (allSlots[i] == current) {
                if (forward) {
                    return allSlots[(i + 1) % allSlots.length];
                } else {
                    return allSlots[(i - 1 + allSlots.length) % allSlots.length];
                }
            }
        }

        return EquipmentSlot.HELMET;
    }

    public void render(SpriteBatch batch, boolean batchIsActive) {
        if (!inventoryOpen) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Calculate panel dimensions
        int panelWidth = 700; // Wider to accommodate equipment
        int panelHeight = 500;

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

        // Render equipment slots and character
        renderEquipmentPanel(batch, panelX, panelY, panelHeight);

        // Render inventory slots
        renderInventorySlots(batch, panelX, panelY, panelWidth, panelHeight);

        // Draw UI text and items
        batch.begin();

        // Title
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        font.draw(batch, "Character & Inventory", panelX + UI_PADDING,
                panelY + panelHeight - UI_PADDING);

        // Coin count
        batch.draw(coinIconTexture, panelX + panelWidth - UI_PADDING - 100,
                panelY + panelHeight - UI_PADDING - 30, 20, 20);
        font.getData().setScale(1.0f);
        font.draw(batch, "x " + coins, panelX + panelWidth - UI_PADDING - 75,
                panelY + panelHeight - UI_PADDING - 15);

        // Draw equipped items in equipment slots
        renderEquippedItems(batch, panelX, panelY, panelHeight);

        // Draw items in inventory slots
        renderInventoryItems(batch, panelX, panelY, panelWidth, panelHeight);

        // Draw selected item info
        renderItemInfo(batch, panelX, panelY, panelWidth);

        // Instructions
        font.setColor(Color.GRAY);
        font.getData().setScale(0.6f);
        float instructY = panelY + UI_PADDING;
        String instructions = selectingEquipmentSlot ?
                "Up/Down: Navigate Equipment | E: Unequip | TAB: Switch to Inventory" :
                "Arrows: Navigate | E: Use/Equip | Q: Drop | TAB: Switch to Equipment | B/ESC: Close";
        font.draw(batch, instructions, panelX + UI_PADDING, instructY);

        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);

        batch.end();
    }

    private void renderEquipmentPanel(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float equipmentPanelWidth = 250;
        float equipmentX = panelX + UI_PADDING;
        float equipmentY = panelY + UI_PADDING + 40;

        // Draw equipment slots background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.9f);
        shapeRenderer.rect(equipmentX, equipmentY, equipmentPanelWidth, panelHeight - 100);
        shapeRenderer.end();

        // Draw character sprite in center
        float charSize = 120;
        float charX = equipmentX + (equipmentPanelWidth - charSize) / 2f;
        float charY = equipmentY + (panelHeight - 100 - charSize) / 2f;

        batch.begin();
        batch.draw(characterSprite, charX, charY, charSize, charSize);
        batch.end();

        // Draw armor slots (left side of character)
        float leftSlotStartX = equipmentX + 10;
        float leftSlotStartY = equipmentY + panelHeight - 140;

        EquipmentSlot[] leftSlots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.SHOULDERS,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
                EquipmentSlot.LEGS,
                EquipmentSlot.BOOTS
        };

        // Draw weapon slots (right side of character)
        float rightSlotStartX = equipmentX + equipmentPanelWidth - EQUIPMENT_SLOT_SIZE - 10;
        float rightSlotStartY = equipmentY + panelHeight - 140;

        EquipmentSlot[] rightSlots = {
                EquipmentSlot.WEAPON,
                EquipmentSlot.OFFHAND
        };

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw left slots (armor)
        for (int i = 0; i < leftSlots.length; i++) {
            EquipmentSlot slot = leftSlots[i];
            float slotY = leftSlotStartY - i * (EQUIPMENT_SLOT_SIZE + 8);

            // Highlight selected equipment slot
            if (selectingEquipmentSlot && selectedEquipmentSlot == slot) {
                shapeRenderer.setColor(EQUIPMENT_SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(EQUIPMENT_SLOT_COLOR);
            }

            shapeRenderer.rect(leftSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);

            // Border
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(selectingEquipmentSlot && selectedEquipmentSlot == slot ? 3 : 2);
            shapeRenderer.setColor(SLOT_BORDER_COLOR);
            shapeRenderer.rect(leftSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        // Draw right slots (weapons)
        for (int i = 0; i < rightSlots.length; i++) {
            EquipmentSlot slot = rightSlots[i];
            float slotY = rightSlotStartY - i * (EQUIPMENT_SLOT_SIZE + 8);

            // Highlight selected equipment slot
            if (selectingEquipmentSlot && selectedEquipmentSlot == slot) {
                shapeRenderer.setColor(EQUIPMENT_SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(EQUIPMENT_SLOT_COLOR);
            }

            shapeRenderer.rect(rightSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);

            // Border
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(selectingEquipmentSlot && selectedEquipmentSlot == slot ? 3 : 2);
            shapeRenderer.setColor(SLOT_BORDER_COLOR);
            shapeRenderer.rect(rightSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        shapeRenderer.end();

        // Draw slot labels
        batch.begin();
        font.getData().setScale(0.6f);
        font.setColor(Color.LIGHT_GRAY);

        // Left slot labels
        for (int i = 0; i < leftSlots.length; i++) {
            float slotY = leftSlotStartY - i * (EQUIPMENT_SLOT_SIZE + 8);
            String slotName = getShortSlotName(leftSlots[i]);
            font.draw(batch, slotName, leftSlotStartX + EQUIPMENT_SLOT_SIZE + 5, slotY + EQUIPMENT_SLOT_SIZE / 2 + 5);
        }

        // Right slot labels (align to the left of the slots)
        for (int i = 0; i < rightSlots.length; i++) {
            float slotY = rightSlotStartY - i * (EQUIPMENT_SLOT_SIZE + 8);
            String slotName = getShortSlotName(rightSlots[i]);
            float textWidth = font.getSpaceXadvance() * slotName.length() * 0.6f;
            font.draw(batch, slotName, rightSlotStartX - textWidth - 5, slotY + EQUIPMENT_SLOT_SIZE / 2 + 5);
        }

        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderEquippedItems(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float equipmentX = panelX + UI_PADDING;
        float equipmentY = panelY + UI_PADDING + 40;
        float equipmentPanelWidth = 250;

        // Left slots (armor)
        float leftSlotStartX = equipmentX + 10;
        float leftSlotStartY = equipmentY + panelHeight - 140;

        EquipmentSlot[] leftSlots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.SHOULDERS,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
                EquipmentSlot.LEGS,
                EquipmentSlot.BOOTS
        };

        for (int i = 0; i < leftSlots.length; i++) {
            Item equippedItem = equipment.getEquippedItem(leftSlots[i]);
            if (equippedItem != null) {
                float slotY = leftSlotStartY - i * (EQUIPMENT_SLOT_SIZE + 8);
                equippedItem.renderIcon(batch, leftSlotStartX + 2, slotY + 2, EQUIPMENT_SLOT_SIZE - 4);
            }
        }

        // Right slots (weapons)
        float rightSlotStartX = equipmentX + equipmentPanelWidth - EQUIPMENT_SLOT_SIZE - 10;
        float rightSlotStartY = equipmentY + panelHeight - 140;

        EquipmentSlot[] rightSlots = {
                EquipmentSlot.WEAPON,
                EquipmentSlot.OFFHAND
        };

        for (int i = 0; i < rightSlots.length; i++) {
            Item equippedItem = equipment.getEquippedItem(rightSlots[i]);
            if (equippedItem != null) {
                float slotY = rightSlotStartY - i * (EQUIPMENT_SLOT_SIZE + 8);
                equippedItem.renderIcon(batch, rightSlotStartX + 2, slotY + 2, EQUIPMENT_SLOT_SIZE - 4);
            }
        }
    }

    private void renderInventorySlots(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        float inventoryStartX = panelX + 280;
        float inventoryStartY = panelY + panelHeight - UI_PADDING - 50 - SLOT_SIZE;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;

            float slotX = inventoryStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = inventoryStartY - row * (SLOT_SIZE + SLOT_PADDING);

            if (!selectingEquipmentSlot && i == selectedSlot) {
                shapeRenderer.setColor(SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(SLOT_COLOR);
            }

            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(!selectingEquipmentSlot && i == selectedSlot ? 3 : 2);
            shapeRenderer.setColor(SLOT_BORDER_COLOR);
            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        shapeRenderer.end();
    }

    private void renderInventoryItems(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        float inventoryStartX = panelX + 280;
        float inventoryStartY = panelY + panelHeight - UI_PADDING - 50 - SLOT_SIZE;

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] != null) {
                int row = i / ITEMS_PER_ROW;
                int col = i % ITEMS_PER_ROW;

                float slotX = inventoryStartX + col * (SLOT_SIZE + SLOT_PADDING);
                float slotY = inventoryStartY - row * (SLOT_SIZE + SLOT_PADDING);

                items[i].renderIcon(batch, slotX + 5, slotY + 5, SLOT_SIZE - 10);

                int count = itemCounts.getOrDefault(i, 1);
                if (count > 1) {
                    font.getData().setScale(0.8f);
                    font.setColor(Color.YELLOW);
                    font.draw(batch, "x" + count, slotX + SLOT_SIZE - 20, slotY + 15);
                }
            }
        }
    }

    private void renderItemInfo(SpriteBatch batch, float panelX, float panelY, float panelWidth) {
        Item displayItem = null;

        if (selectingEquipmentSlot && selectedEquipmentSlot != null) {
            displayItem = equipment.getEquippedItem(selectedEquipmentSlot);
        } else if (items[selectedSlot] != null) {
            displayItem = items[selectedSlot];
        }

        if (displayItem != null) {
            font.setColor(Color.CYAN);
            font.getData().setScale(0.9f);

            float infoY = panelY + UI_PADDING + 70;
            float infoX = panelX + 280;
            font.draw(batch, displayItem.getName(), infoX, infoY);

            font.setColor(Color.LIGHT_GRAY);
            font.getData().setScale(0.7f);
            font.draw(batch, displayItem.getDescription(), infoX, infoY - 20);

            if (displayItem.getDamage() > 0) {
                font.draw(batch, "Damage: " + displayItem.getDamage(), infoX, infoY - 35);
            }
            if (displayItem.getDefense() > 0) {
                font.draw(batch, "Defense: " + displayItem.getDefense(), infoX + 100, infoY - 35);
            }
            if (displayItem.getHealthRestore() > 0) {
                font.draw(batch, "Heals: " + displayItem.getHealthRestore(), infoX, infoY - 35);
            }
        }
    }

    private String getShortSlotName(EquipmentSlot slot) {
        switch (slot) {
            case HELMET: return "Head";
            case SHOULDERS: return "Shoulders";
            case CHEST: return "Chest";
            case GLOVES: return "Hands";
            case LEGS: return "Legs";
            case BOOTS: return "Feet";
            case WEAPON: return "Main-hand";
            case OFFHAND: return "Off-hand";
            default: return slot.toString();
        }
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public boolean isFull() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] == null) {
                return false;
            }
        }
        return true;
    }

    public List<Item> getAllItems() {
        List<Item> allItems = new ArrayList<>();
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] != null) {
                allItems.add(items[i]);
            }
        }
        return allItems;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public boolean isSelectingEquipmentSlot() {
        return selectingEquipmentSlot;
    }

    public EquipmentSlot getSelectedEquipmentSlot() {
        return selectedEquipmentSlot;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}