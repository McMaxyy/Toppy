package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import config.Storage;
import entities.PlayerClass;
import entities.PlayerStats;
import game.GameProj;
import items.Item;
import managers.Equipment.EquipmentSlot;

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

    // Stats panel settings
    private final int STAT_BUTTON_SIZE = 24;
    private final int RESET_BUTTON_WIDTH = 100;
    private final int RESET_BUTTON_HEIGHT = 30;
    private final Color BUTTON_COLOR = new Color(0.3f, 0.3f, 0.4f, 0.9f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.4f, 0.4f, 0.5f, 0.9f);
    private final Color RESET_BUTTON_COLOR = new Color(0.4f, 0.2f, 0.2f, 0.9f);

    private Map<Integer, Integer> itemCounts;

    public Inventory() {
        this.items = new Item[MAX_SLOTS];
        this.coins = 0;
        this.itemCounts = new HashMap<>();
        this.equipment = new Equipment();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.slotTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        this.coinIconTexture = Storage.assetManager.get("icons/items/Coin.png", Texture.class);
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
        return addItem(item, 1.0f);
    }

    public boolean addItem(Item item, float coinMultiplier) {
        if (item.getType() == Item.ItemType.COIN) {
            int coinValue = (int)(item.getBuyValue() * coinMultiplier);
            coins += coinValue;
            return true;
        } else {
            SoundManager.getInstance().playPickupSound();
        }

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] != null && items[i].canStackWith(item)) {
                int currentCount = itemCounts.getOrDefault(i, 1);
                itemCounts.put(i, currentCount + 1);
                return true;
            }
        }

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

    public void removeItemByReference(Item item) {
        // Find and remove one instance of this item
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] != null && items[i].canStackWith(item)) {
                int count = itemCounts.getOrDefault(i, 1);
                if (count > 1) {
                    itemCounts.put(i, count - 1);
                } else {
                    items[i] = null;
                    itemCounts.remove(i);
                }
                return;
            }
        }
    }

    public int getItemCountForConsumable(Item consumable) {
        if (consumable == null) return 0;

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] != null && items[i].canStackWith(consumable)) {
                return itemCounts.getOrDefault(i, 0);
            }
        }
        return 0;
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

    public void equipItemFromInventory(int inventorySlot, entities.Player player) {
        Item item = items[inventorySlot];
        if (item == null) return;

        if (item.getType() != Item.ItemType.WEAPON &&
                item.getType() != Item.ItemType.ARMOR &&
                item.getType() != Item.ItemType.OFFHAND) {
            return;
        }

        if (item.getType() == Item.ItemType.WEAPON) {
            if (item.getName().endsWith("Spear") &&
                    player.getPlayerClass().equals(PlayerClass.MERCENARY)) {
                Item previousItem = equipment.equipItem(item, player);

                removeItem(inventorySlot);

                if (previousItem != null) {
                    addItem(previousItem);
                }

                SoundManager.getInstance().playPickupSound();
            } else if (item.getName().endsWith("Sword") &&
                    player.getPlayerClass().equals(PlayerClass.PALADIN)) {
                Item previousItem = equipment.equipItem(item, player);

                removeItem(inventorySlot);

                if (previousItem != null) {
                    addItem(previousItem);
                }

                SoundManager.getInstance().playPickupSound();
            }
        } else {
            Item previousItem = equipment.equipItem(item, player);

            removeItem(inventorySlot);

            if (previousItem != null) {
                addItem(previousItem);
            }

            SoundManager.getInstance().playPickupSound();
        }

    }

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
        if (selectingEquipmentSlot) {
            if (selectedEquipmentSlot != null) {
                unequipItemToInventory(selectedEquipmentSlot, player);
            }
        } else {
            if (items[selectedSlot] != null) {
                Item item = items[selectedSlot];

                if (item.getType() == Item.ItemType.WEAPON || item.getType() == Item.ItemType.ARMOR || item.getType() == Item.ItemType.OFFHAND) {
                    equipItemFromInventory(selectedSlot, player);
                } else {
                    item.use(player);

                    if (item.getName().contains("Potion"))
                        SoundManager.getInstance().playPotionSound();

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
        SoundManager.getInstance().playPickupCoinSound();
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
            if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
                toggleInventory();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.I) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            toggleInventory();
            return;
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int panelWidth = 700;
        int panelHeight = 500;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        float invStartX = panelX + 280;
        float invStartY = panelY + panelHeight - UI_PADDING - 50 - SLOT_SIZE;

        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            float x = invStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float y = invStartY - row * (SLOT_SIZE + SLOT_PADDING);

            if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                selectedSlot = i;
                selectingEquipmentSlot = false;

                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    Item item = items[selectedSlot];

                    // Check if it's a consumable - start dragging it
                    if (item != null && item.getType() == Item.ItemType.CONSUMABLE) {
                        player.getAbilityManager().startDraggingConsumable(item);
                    } else {
                        useSelectedItem(player, gameP);
                    }
                }
            }
        }

        checkEquipmentMouseHover(mouseX, mouseY, panelX, panelY, panelHeight, player, gameP);

        checkStatButtonClicks(mouseX, mouseY, panelX, panelY, panelHeight, player);
    }

    private void checkStatButtonClicks(float mx, float my, float px, float py, float ph, entities.Player player) {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;

        PlayerStats stats = player.getStats();
        float equipmentX = px + UI_PADDING;
        float statsStartY = py + UI_PADDING + 180;

        // Button X position (right side of stats text area)
        float buttonX = equipmentX + 180;

        // Check VIT + button
        float vitY = statsStartY - 25;
        if (isPointInRect(mx, my, buttonX, vitY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateHealthPoint();
            return;
        }

        // Check AP + button
        float apY = statsStartY - 55;
        if (isPointInRect(mx, my, buttonX, apY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateAttackPoint();
            return;
        }

        // Check DP + button
        float dpY = statsStartY - 85;
        if (isPointInRect(mx, my, buttonX, dpY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateDefensePoint();
            return;
        }

        // Check DEX + button
        float dexY = statsStartY - 115;
        if (isPointInRect(mx, my, buttonX, dexY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateDexPoint();
            return;
        }

        // Check Reset button
        float resetX = equipmentX + 10;
        float resetY = py + UI_PADDING;
        if (isPointInRect(mx, my, resetX, resetY, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT)) {
            stats.resetStatPoints();
        }
    }

    private boolean isPointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    private void checkEquipmentMouseHover(float mx, float my, float px, float py, float ph, entities.Player player, GameProj gameP) {
        float equipmentX = px + UI_PADDING;
        float equipmentY = py + UI_PADDING + 40;
        float eqWidth = 250;

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

    public void render(SpriteBatch batch, boolean batchIsActive) {
        if (!inventoryOpen) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        int panelWidth = 700;
        int panelHeight = 500;

        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        if (batchIsActive) {
            batch.end();
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        renderEquipmentPanel(batch, panelX, panelY, panelHeight);

        renderInventorySlots(batch, panelX, panelY, panelWidth, panelHeight);

        batch.begin();

        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        font.draw(batch, "Inventory", panelX + UI_PADDING,
                panelY + panelHeight - UI_PADDING / 2);

        batch.draw(coinIconTexture, panelX + panelWidth - UI_PADDING - 120,
                panelY + panelHeight - UI_PADDING - 30, 40, 40);
        font.getData().setScale(1.0f);
        font.draw(batch, "" + coins, panelX + panelWidth - UI_PADDING - 70,
                panelY + panelHeight - UI_PADDING);

        renderEquippedItems(batch, panelX, panelY, panelHeight);

        renderInventoryItems(batch, panelX, panelY, panelWidth, panelHeight);

        renderItemInfo(batch, panelX, panelY, panelWidth);

        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);

        batch.end();
    }
    public void render(SpriteBatch batch, boolean batchIsActive, entities.Player player) {
        if (!inventoryOpen) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        int panelWidth = 700;
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

        // Render stats panel (this handles its own batch begin/end)
        renderStatsPanel(batch, panelX, panelY, panelHeight, player);

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
        float charSize = 100;
        float charX = equipmentX + (equipmentPanelWidth - charSize) / 2f;
        float charY = equipmentY + (panelHeight) / 2f + 20;

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

            if (selectingEquipmentSlot && selectedEquipmentSlot == slot) {
                shapeRenderer.setColor(EQUIPMENT_SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(EQUIPMENT_SLOT_COLOR);
            }

            shapeRenderer.rect(leftSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);

            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            // Get border color from equipped item
            Color borderColor = SLOT_BORDER_COLOR;
            Item equippedItem = equipment.getEquippedItem(slot);
            if (equippedItem != null && equippedItem.getGearType() != null) {
                borderColor = getGearTypeColor(equippedItem.getGearType());
            }

            Gdx.gl.glLineWidth(selectingEquipmentSlot && selectedEquipmentSlot == slot ? 4 : 3);
            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(leftSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        // Draw right slots (weapons)
        for (int i = 0; i < rightSlots.length; i++) {
            EquipmentSlot slot = rightSlots[i];
            float slotY = rightSlotStartY - i * (EQUIPMENT_SLOT_SIZE + 8);

            if (selectingEquipmentSlot && selectedEquipmentSlot == slot) {
                shapeRenderer.setColor(EQUIPMENT_SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(EQUIPMENT_SLOT_COLOR);
            }

            shapeRenderer.rect(rightSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);

            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            // Get border color from equipped item
            Color borderColor = SLOT_BORDER_COLOR;
            Item equippedItem = equipment.getEquippedItem(slot);
            if (equippedItem != null && equippedItem.getGearType() != null) {
                borderColor = getGearTypeColor(equippedItem.getGearType());
            }

            Gdx.gl.glLineWidth(selectingEquipmentSlot && selectedEquipmentSlot == slot ? 4 : 3);
            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(rightSlotStartX, slotY, EQUIPMENT_SLOT_SIZE, EQUIPMENT_SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        shapeRenderer.end();
    }

    public void renderStatsPanel(SpriteBatch batch, float panelX, float panelY, float panelHeight, entities.Player player) {
        PlayerStats stats = player.getStats();
        float equipmentX = panelX + UI_PADDING;
        float statsStartY = panelY + UI_PADDING + 180;

        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        boolean hasPoints = stats.getAvailableStatPoints() > 0;
        float buttonX = equipmentX + 180;

        batch.end();

        float vitY = statsStartY - 25;
        float apY = statsStartY - 55;
        float dpY = statsStartY - 85;
        float dexY = statsStartY - 115;
        float resetX = equipmentX + 20;
        float resetY = panelY + UI_PADDING;

        if (stats.getAvailableStatPoints() > 0) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            if (hasPoints && isPointInRect(mouseX, mouseY, buttonX, vitY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
                shapeRenderer.setColor(BUTTON_HOVER_COLOR);
            } else {
                shapeRenderer.setColor(hasPoints ? BUTTON_COLOR : new Color(0.2f, 0.2f, 0.2f, 0.5f));
            }
            shapeRenderer.rect(buttonX, vitY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);

            if (hasPoints && isPointInRect(mouseX, mouseY, buttonX, apY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
                shapeRenderer.setColor(BUTTON_HOVER_COLOR);
            } else {
                shapeRenderer.setColor(hasPoints ? BUTTON_COLOR : new Color(0.2f, 0.2f, 0.2f, 0.5f));
            }
            shapeRenderer.rect(buttonX, apY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);

            if (hasPoints && isPointInRect(mouseX, mouseY, buttonX, dpY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
                shapeRenderer.setColor(BUTTON_HOVER_COLOR);
            } else {
                shapeRenderer.setColor(hasPoints ? BUTTON_COLOR : new Color(0.2f, 0.2f, 0.2f, 0.5f));
            }
            shapeRenderer.rect(buttonX, dpY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);

            if (hasPoints && isPointInRect(mouseX, mouseY, buttonX, dexY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
                shapeRenderer.setColor(BUTTON_HOVER_COLOR);
            } else {
                shapeRenderer.setColor(hasPoints ? BUTTON_COLOR : new Color(0.2f, 0.2f, 0.2f, 0.5f));
            }
            shapeRenderer.rect(buttonX, dexY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);

            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(SLOT_BORDER_COLOR);
            shapeRenderer.rect(buttonX, vitY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);
            shapeRenderer.rect(buttonX, apY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);
            shapeRenderer.rect(buttonX, dpY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);
            shapeRenderer.rect(buttonX, dexY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE);
            shapeRenderer.end();
        }

        if (stats.getAvailableStatPoints() == 0 && stats.getLevel() > 1) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            if (isPointInRect(mouseX, mouseY, resetX, resetY, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT)) {
                shapeRenderer.setColor(new Color(0.5f, 0.3f, 0.3f, 0.9f));
            } else {
                shapeRenderer.setColor(RESET_BUTTON_COLOR);
            }
            shapeRenderer.rect(resetX, resetY, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.rect(resetX, resetY, RESET_BUTTON_WIDTH, RESET_BUTTON_HEIGHT);
            shapeRenderer.end();

        }

        batch.begin();

        font.setColor(Color.WHITE);
        font.getData().setScale(0.6f);

        font.draw(batch, "Level " + stats.getLevel(), equipmentX + 85, statsStartY + 250);

        // Available points
        font.draw(batch, "Available points: " + stats.getAvailableStatPoints(), equipmentX + 10, statsStartY + 30);

        int totalVit = stats.getDisplayVit();
        int totalAP = stats.getDisplayAP();
        int totalDP = stats.getDisplayDP();
        int totalDex = stats.getDisplayDex();

        font.setColor(Color.RED);
        font.draw(batch, "VIT: " + totalVit, equipmentX + 10, statsStartY - 10);

        font.setColor(Color.ORANGE);
        font.draw(batch, "ATT: " + totalAP, equipmentX + 10, statsStartY - 40);

        font.setColor(Color.CYAN);
        font.draw(batch, "DEF: " + totalDP, equipmentX + 10, statsStartY - 70);

        font.setColor(Color.GREEN);
        font.draw(batch, "DEX: " + totalDex, equipmentX + 10, statsStartY - 100);

        if (stats.getAvailableStatPoints() > 0) {
            font.setColor(hasPoints ? Color.WHITE : Color.GRAY);
            font.getData().setScale(1.0f);
            font.draw(batch, "+", buttonX + 6, vitY + 20);
            font.draw(batch, "+", buttonX + 6, apY + 20);
            font.draw(batch, "+", buttonX + 6, dpY + 20);
            font.draw(batch, "+", buttonX + 6, dexY + 20);
        }

        if (stats.getAvailableStatPoints() == 0 && stats.getLevel() > 1) {
            font.setColor(Color.WHITE);
            font.getData().setScale(0.7f);
            font.draw(batch, "Reset", resetX + 15, resetY + 22);
        }

        font.getData().setScale(1.0f);
    }

    private void renderEquippedItems(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float equipmentX = panelX + UI_PADDING;
        float equipmentY = panelY + UI_PADDING + 40;
        float equipmentPanelWidth = 250;

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

    private Color getGearTypeColor(String gearType) {
        if (gearType == null) return SLOT_BORDER_COLOR;

        switch (gearType) {
            case ItemRegistry.VALKYRIE:
                return new Color(1f, 0.85f, 0f, 1f); // Yellow
            case ItemRegistry.PROTECTOR:
                return new Color(1f, 1f, 1f, 1f); // White
            case ItemRegistry.BARBARIAN:
                return new Color(0.6f, 0.4f, 0.2f, 1f); // Brown
            case ItemRegistry.BERSERKER:
                return new Color(0.9f, 0.2f, 0.2f, 1f); // Red
            case ItemRegistry.DECEPTOR:
                return new Color(0.7f, 0.3f, 0.9f, 1f); // Purple
            default:
                return SLOT_BORDER_COLOR;
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

            // Get border color based on item's gear type
            Color borderColor = SLOT_BORDER_COLOR;
            if (items[i] != null && items[i].getGearType() != null) {
                borderColor = getGearTypeColor(items[i].getGearType());
            }

            Gdx.gl.glLineWidth(!selectingEquipmentSlot && i == selectedSlot ? 4 : 3);
            shapeRenderer.setColor(borderColor);
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
                    font.getData().setScale(0.6f);
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
            font.setColor(Color.WHITE);
            font.getData().setScale(0.8f);

            float infoY = panelY + UI_PADDING + 150;
            float infoX = panelX + 280;
            font.draw(batch, displayItem.getName(), infoX, infoY);

            font.getData().setScale(0.6f);

            int lineOffset = 0;
            float lineSpacing = 20f;

            if (displayItem.getDamage() > 0) {
                font.setColor(Color.ORANGE);
                font.draw(batch, "Damage: " + displayItem.getDamage(), infoX, infoY - 25 - (lineOffset * lineSpacing));
                lineOffset++;
            }

            if (displayItem.getDefense() > 0) {
                font.setColor(Color.CYAN);
                font.draw(batch, "Defense: " + displayItem.getDefense(), infoX, infoY - 25 - (lineOffset * lineSpacing));
                lineOffset++;
            }

            if (displayItem.getBonusVitality() > 0) {
                font.setColor(Color.RED);
                font.draw(batch, "Vitality: " + displayItem.getBonusVitality(), infoX, infoY - 25 - (lineOffset * lineSpacing));
                lineOffset++;
            } else if (displayItem.getHealthRestore() > 0) {
                font.setColor(Color.RED);
                font.draw(batch, "Health: " + displayItem.getHealthRestore(), infoX, infoY - 25 - (lineOffset * lineSpacing));
                lineOffset++;
            }

            if (displayItem.getBonusDex() > 0) {
                font.setColor(Color.GREEN);
                font.draw(batch, "Dexterity: " + displayItem.getBonusDex(), infoX, infoY - 25 - (lineOffset * lineSpacing));
                lineOffset++;
            }

            // Reset font color
            font.setColor(Color.WHITE);
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