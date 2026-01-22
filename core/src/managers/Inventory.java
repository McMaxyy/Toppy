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

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import config.Storage;
import game.GameProj;
import items.Item;
import managers.Equipment.EquipmentSlot;

/**
 * Enhanced inventory system with equipment screen
 */
public class Inventory {
    private static final int MAX_SLOTS = 28;
    private static final int ITEMS_PER_ROW = 7;

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
        this.coinIconTexture = Storage.assetManager.get("icons/items/coin.png", Texture.class);
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

    public void useSelectedItem(entities.Player player, GameProj gameP) {
        Vector3 mousePosition3D = gameP.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);

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

    public void update(float delta, entities.Player player, GameProj gameP) {
        if (!inventoryOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
                toggleInventory();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.B) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            toggleInventory();
            return;
        }

        // --- MOUSE TRACKING LOGIC ---
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int panelWidth = 700;
        int panelHeight = 500;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        // libGDX mouse Y is top-down, we need bottom-up to match ShapeRenderer
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        // 1. Check Inventory Slots
        float invStartX = panelX + 280;
        float invStartY = panelY + panelHeight - UI_PADDING - 50 - SLOT_SIZE;

        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            float x = invStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float y = invStartY - row * (SLOT_SIZE + SLOT_PADDING);

            if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                selectedSlot = i;
                selectingEquipmentSlot = false; // Mouse over inventory takes priority

                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    useSelectedItem(player, gameP);
                }
            }
        }

        checkEquipmentMouseHover(mouseX, mouseY, panelX, panelY, panelHeight, player, gameP);

        // Keep your TAB/Arrow key logic below if you want hybrid controls,
        // otherwise, you can remove the Arrow Key blocks entirely.
    }

    private void checkEquipmentMouseHover(float mx, float my, float px, float py, float ph, entities.Player player, GameProj gameP) {
        float equipmentX = px + UI_PADDING;
        float equipmentY = py + UI_PADDING + 40;
        float eqWidth = 250;

        // Define the positions exactly as you do in render
        EquipmentSlot[] leftSlots = {EquipmentSlot.HELMET, EquipmentSlot.CHEST, EquipmentSlot.GLOVES, EquipmentSlot.BOOTS};
        float leftX = equipmentX + 10;
        float leftStartY = equipmentY + ph - 140;

        for (int i = 0; i < leftSlots.length; i++) {
            float slotY = leftStartY - i * (EQUIPMENT_SLOT_SIZE + 8);
            if (mx >= leftX && mx <= leftX + EQUIPMENT_SLOT_SIZE && my >= slotY && my <= slotY + EQUIPMENT_SLOT_SIZE) {
                selectedEquipmentSlot = leftSlots[i];
                selectingEquipmentSlot = true;
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) useSelectedItem(player, gameP);
            }
        }

        EquipmentSlot[] rightSlots = {EquipmentSlot.WEAPON, EquipmentSlot.OFFHAND};
        float rightX = equipmentX + eqWidth - EQUIPMENT_SLOT_SIZE - 10;
        float rightStartY = equipmentY + ph - 140;

        for (int i = 0; i < rightSlots.length; i++) {
            float slotY = rightStartY - i * (EQUIPMENT_SLOT_SIZE + 8);
            if (mx >= rightX && mx <= rightX + EQUIPMENT_SLOT_SIZE && my >= slotY && my <= slotY + EQUIPMENT_SLOT_SIZE) {
                selectedEquipmentSlot = rightSlots[i];
                selectingEquipmentSlot = true;
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) useSelectedItem(player, gameP);
            }
        }
    }

    private EquipmentSlot getNextEquipmentSlot(EquipmentSlot current, boolean forward) {
        EquipmentSlot[] allSlots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
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
        font.getData().setScale(1f);
        font.draw(batch, "Inventory", panelX + UI_PADDING,
                panelY + panelHeight - UI_PADDING / 2);

        // Coin count
        batch.draw(coinIconTexture, panelX + panelWidth - UI_PADDING - 120,
                panelY + panelHeight - UI_PADDING - 30, 40, 40);
        font.getData().setScale(1.0f);
        font.draw(batch, "" + coins, panelX + panelWidth - UI_PADDING - 70,
                panelY + panelHeight - UI_PADDING);

        // Draw equipped items in equipment slots
        renderEquippedItems(batch, panelX, panelY, panelHeight);

        // Draw items in inventory slots
        renderInventoryItems(batch, panelX, panelY, panelWidth, panelHeight);

        // Draw selected item info
        renderItemInfo(batch, panelX, panelY, panelWidth);

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
        float charY = equipmentY + (panelHeight) / 2f;

        batch.begin();
        batch.draw(characterSprite, charX, charY, charSize, charSize);
        batch.end();

        // Draw armor slots (left side of character)
        float leftSlotStartX = equipmentX + 10;
        float leftSlotStartY = equipmentY + panelHeight - 150;

        EquipmentSlot[] leftSlots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
                EquipmentSlot.BOOTS
        };

        // Draw weapon slots (right side of character)
        float rightSlotStartX = equipmentX + equipmentPanelWidth - EQUIPMENT_SLOT_SIZE - 10;
        float rightSlotStartY = equipmentY + panelHeight - 150;

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
    }

    private void renderEquippedItems(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float equipmentX = panelX + UI_PADDING;
        float equipmentY = panelY + UI_PADDING + 40;
        float equipmentPanelWidth = 250;

        // Left slots (armor)
        float leftSlotStartX = equipmentX + 10;
        float leftSlotStartY = equipmentY + panelHeight - 150;

        EquipmentSlot[] leftSlots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
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
        float rightSlotStartY = equipmentY + panelHeight - 150;

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

            if (displayItem.getDamage() > 0) {
                font.draw(batch, "Damage: " + displayItem.getDamage(), infoX, infoY - 35);
            }
            if (displayItem.getDefense() > 0) {
                font.draw(batch, "Defense: " + displayItem.getDefense(), infoX, infoY - 35);
            }
            if (displayItem.getHealthRestore() > 0) {
                font.draw(batch, "Heals: " + displayItem.getHealthRestore(), infoX, infoY - 35);
            }
        }
    }

    private String getShortSlotName(EquipmentSlot slot) {
        switch (slot) {
            case HELMET: return "";
            case CHEST: return "";
            case GLOVES: return "";
            case BOOTS: return "";
            case WEAPON: return "";
            case OFFHAND: return "";
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

    public boolean isInventoryOpen () {
        return inventoryOpen;
    }
}