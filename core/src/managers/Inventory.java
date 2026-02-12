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

    private Equipment equipment;
    private EquipmentSlot selectedEquipmentSlot = null;
    private boolean selectingEquipmentSlot = false;

    private boolean isDragging = false;
    private int dragSourceSlot = -1;
    private Item draggedItem = null;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;
    private static final float HEALTH_POTION_COOLDOWN = 5f;
    private float healthPotionCooldownTimer = 0f;

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Texture slotTexture;
    private final Texture coinIconTexture;
    private final Texture characterSprite;
    private final Texture trashCan;


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

    private final int SORT_BUTTON_WIDTH = 80;
    private final int SORT_BUTTON_HEIGHT = 25;
    private final Color SORT_BUTTON_COLOR = new Color(0.3f, 0.4f, 0.5f, 0.9f);
    private final Color SORT_BUTTON_HOVER_COLOR = new Color(0.4f, 0.5f, 0.6f, 0.9f);

    private final int TRASH_SIZE = 100;
    private final Color TRASH_COLOR = new Color(0.5f, 0.5f, 0.5f, 0.8f);
    private final Color TRASH_HOVER_COLOR = new Color(0.6f, 0.3f, 0.3f, 0.9f);

    private final int STAT_BUTTON_SIZE = 24;
    private final int RESET_BUTTON_WIDTH = 100;
    private final int RESET_BUTTON_HEIGHT = 30;
    private final Color BUTTON_COLOR = new Color(0.3f, 0.3f, 0.4f, 0.9f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.4f, 0.4f, 0.5f, 0.9f);
    private final Color RESET_BUTTON_COLOR = new Color(0.4f, 0.2f, 0.2f, 0.9f);

    private Map<Integer, Integer> itemCounts;

    private float cachedPanelX, cachedPanelY, cachedPanelWidth, cachedPanelHeight;
    private float cachedInvStartX, cachedInvStartY;
    private float cachedEquipmentX, cachedEquipmentY, cachedEquipmentWidth, cachedEquipmentHeight;
    private float cachedTrashX, cachedTrashY;

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
        this.trashCan = Storage.assetManager.get("ui/Trash.png", Texture.class);
    }

    public void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        if (!inventoryOpen) {
            selectingEquipmentSlot = false;
            selectedEquipmentSlot = null;
            cancelDrag();
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
            if (!isFull())
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

    public void removeItemCompletely(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            items[slot] = null;
            itemCounts.remove(slot);
        }
    }

    public void removeItemByReference(Item item) {
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

    // Sort inventory by item type: WEAPON -> OFFHAND -> ARMOR -> CONSUMABLE
    public void sortInventory() {
        List<ItemWithCount> allItems = new ArrayList<>();

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (items[i] != null) {
                allItems.add(new ItemWithCount(items[i], itemCounts.getOrDefault(i, 1)));
            }
        }

        allItems.sort((a, b) -> {
            int priorityA = getTypePriority(a.item);
            int priorityB = getTypePriority(b.item);
            return Integer.compare(priorityA, priorityB);
        });

        for (int i = 0; i < MAX_SLOTS; i++) {
            items[i] = null;
        }
        itemCounts.clear();

        int slotIndex = 0;
        for (ItemWithCount iwc : allItems) {
            if (slotIndex < MAX_SLOTS) {
                items[slotIndex] = iwc.item;
                itemCounts.put(slotIndex, iwc.count);
                slotIndex++;
            }
        }
    }

    private int getTypePriority(Item item) {
        if (item == null) return 999;

        switch (item.getType()) {
            case WEAPON:
                return 0;
            case OFFHAND:
                return 1;
            case ARMOR:
                return 2;
            case CONSUMABLE:
                return 3;
            default:
                return 4;
        }
    }

    private static class ItemWithCount {
        Item item;
        int count;

        ItemWithCount(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    // Drag and drop methods
    private void startDrag(int slot, float mouseX, float mouseY) {
        if (slot >= 0 && slot < MAX_SLOTS && items[slot] != null) {
            isDragging = true;
            dragSourceSlot = slot;
            draggedItem = items[slot];
        }
    }

    private void cancelDrag() {
        isDragging = false;
        dragSourceSlot = -1;
        draggedItem = null;
    }

    private void completeDrag(float mouseX, float mouseY, entities.Player player) {
        if (!isDragging || draggedItem == null) {
            cancelDrag();
            return;
        }

        // Check if dropped on another inventory slot
        int targetSlot = getSlotAtPosition(mouseX, mouseY);
        if (targetSlot >= 0 && targetSlot != dragSourceSlot) {
            swapItems(dragSourceSlot, targetSlot);
            cancelDrag();
            return;
        }

        // Check if dropped on trash area
        if (isInTrashArea(mouseX, mouseY)) {
            removeItemCompletely(dragSourceSlot);
            cancelDrag();
            return;
        }

        // Check if equipment item dropped on character panel
        if (isEquippableItem(draggedItem) && isInEquipmentPanel(mouseX, mouseY)) {
            equipItemFromInventory(dragSourceSlot, player);
            cancelDrag();
            return;
        }

        // Check if consumable dragged outside inventory bounds
        if (draggedItem.getType() == Item.ItemType.CONSUMABLE && !isInInventoryBounds(mouseX, mouseY)) {
            if (player.getAbilityManager() != null) {
                player.getAbilityManager().startDraggingConsumable(draggedItem);
            }
            cancelDrag();
            return;
        }

        cancelDrag();
    }

    private void swapItems(int slot1, int slot2) {
        Item temp = items[slot1];
        int count1 = itemCounts.getOrDefault(slot1, 1);
        int count2 = itemCounts.getOrDefault(slot2, 1);

        items[slot1] = items[slot2];
        items[slot2] = temp;

        itemCounts.remove(slot1);
        itemCounts.remove(slot2);

        if (items[slot1] != null) {
            itemCounts.put(slot1, count2);
        }
        if (items[slot2] != null) {
            itemCounts.put(slot2, count1);
        }
    }

    private int getSlotAtPosition(float mouseX, float mouseY) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            float x = cachedInvStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float y = cachedInvStartY - row * (SLOT_SIZE + SLOT_PADDING);

            if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInTrashArea(float mouseX, float mouseY) {
        return mouseX >= cachedTrashX && mouseX <= cachedTrashX + TRASH_SIZE &&
                mouseY >= cachedTrashY && mouseY <= cachedTrashY + TRASH_SIZE;
    }

    private boolean isInEquipmentPanel(float mouseX, float mouseY) {
        return mouseX >= cachedEquipmentX && mouseX <= cachedEquipmentX + cachedEquipmentWidth &&
                mouseY >= cachedEquipmentY && mouseY <= cachedEquipmentY + cachedEquipmentHeight;
    }

    private boolean isInInventoryBounds(float mouseX, float mouseY) {
        return mouseX >= cachedPanelX && mouseX <= cachedPanelX + cachedPanelWidth &&
                mouseY >= cachedPanelY && mouseY <= cachedPanelY + cachedPanelHeight;
    }

    private boolean isEquippableItem(Item item) {
        return item != null && (item.getType() == Item.ItemType.WEAPON ||
                item.getType() == Item.ItemType.ARMOR ||
                item.getType() == Item.ItemType.OFFHAND);
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
                removeItemCompletely(inventorySlot);
                if (previousItem != null) {
                    addItem(previousItem);
                }
                SoundManager.getInstance().playPickupSound();
            } else if (item.getName().endsWith("Sword") &&
                    player.getPlayerClass().equals(PlayerClass.PALADIN)) {
                Item previousItem = equipment.equipItem(item, player);
                removeItemCompletely(inventorySlot);
                if (previousItem != null) {
                    addItem(previousItem);
                }
                SoundManager.getInstance().playPickupSound();
            }
        } else {
            Item previousItem = equipment.equipItem(item, player);
            removeItemCompletely(inventorySlot);
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
            return;
        }

        if (items[selectedSlot] == null) return;

        Item item = items[selectedSlot];

        if (item.getType() == Item.ItemType.WEAPON ||
                item.getType() == Item.ItemType.ARMOR ||
                item.getType() == Item.ItemType.OFFHAND) {
            equipItemFromInventory(selectedSlot, player);
            return;
        }

        if (item.getType() == Item.ItemType.CONSUMABLE) {
            boolean isHealthPotion = item.getName() != null && item.getName().contains("Health Potion");
            if (isHealthPotion && healthPotionCooldownTimer > 0f) {
                System.out.println("Health potion on cooldown: " + String.format("%.1f", healthPotionCooldownTimer) + "s");
                return;
            }

            item.use(player);

            if (item.getName() != null && item.getName().contains("Potion")) {
                SoundManager.getInstance().playPotionSound();
            }

            if (isHealthPotion) {
                healthPotionCooldownTimer = HEALTH_POTION_COOLDOWN;
            }

            removeItem(selectedSlot);
            return;
        }

        item.use(player);
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
        if (healthPotionCooldownTimer > 0f) {
            healthPotionCooldownTimer -= delta;
            if (healthPotionCooldownTimer < 0f) healthPotionCooldownTimer = 0f;
        }

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

        cachedPanelX = panelX;
        cachedPanelY = panelY;
        cachedPanelWidth = panelWidth;
        cachedPanelHeight = panelHeight;
        cachedInvStartX = panelX + 280;
        cachedInvStartY = panelY + panelHeight - UI_PADDING - 50 - SLOT_SIZE;
        cachedEquipmentX = panelX + UI_PADDING;
        cachedEquipmentY = panelY + UI_PADDING + 40;
        cachedEquipmentWidth = 250;
        cachedEquipmentHeight = panelHeight - 100;
        cachedTrashX = panelX + panelWidth - UI_PADDING - TRASH_SIZE;
        cachedTrashY = panelY + UI_PADDING;

        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        if (isDragging) {
            if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                completeDrag(mouseX, mouseY, player);
            }
        } else {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                int clickedSlot = getSlotAtPosition(mouseX, mouseY);
                if (clickedSlot >= 0 && items[clickedSlot] != null) {
                    startDrag(clickedSlot, mouseX, mouseY);
                }

                float sortButtonX = cachedInvStartX;
                float sortButtonY = cachedInvStartY + SLOT_SIZE + 10;
                if (isPointInRect(mouseX, mouseY, sortButtonX, sortButtonY, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT)) {
                    sortInventory();
                }
            }
        }

        if (!isDragging) {
            for (int i = 0; i < MAX_SLOTS; i++) {
                int row = i / ITEMS_PER_ROW;
                int col = i % ITEMS_PER_ROW;
                float x = cachedInvStartX + col * (SLOT_SIZE + SLOT_PADDING);
                float y = cachedInvStartY - row * (SLOT_SIZE + SLOT_PADDING);

                if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                    selectedSlot = i;
                    selectingEquipmentSlot = false;

                    if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
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

        float buttonX = equipmentX + 180;

        float vitY = statsStartY - 25;
        if (isPointInRect(mx, my, buttonX, vitY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateHealthPoint();
            return;
        }

        float apY = statsStartY - 55;
        if (isPointInRect(mx, my, buttonX, apY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateAttackPoint();
            return;
        }

        float dpY = statsStartY - 85;
        if (isPointInRect(mx, my, buttonX, dpY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateDefensePoint();
            return;
        }

        float dexY = statsStartY - 115;
        if (isPointInRect(mx, my, buttonX, dexY, STAT_BUTTON_SIZE, STAT_BUTTON_SIZE)) {
            stats.allocateDexPoint();
            return;
        }

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
                if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) useSelectedItem(player, gameP);
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
                if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) useSelectedItem(player, gameP);
            }
        }
    }

    public boolean tryUseConsumable(Item consumable, entities.Player player) {
        if (consumable == null || player == null) return false;
        if (consumable.getType() != Item.ItemType.CONSUMABLE) return false;

        int count = getItemCountForConsumable(consumable);
        if (count <= 0) return false;

        boolean isHealthPotion = consumable.getName() != null && consumable.getName().contains("Health Potion");
        if (isHealthPotion && healthPotionCooldownTimer > 0f) {
            return false;
        }

        consumable.use(player);

        if (consumable.getName() != null && consumable.getName().contains("Potion")) {
            SoundManager.getInstance().playPotionSound();
        }

        if (isHealthPotion) {
            healthPotionCooldownTimer = HEALTH_POTION_COOLDOWN;
        }

        removeItemByReference(consumable);
        return true;
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
        renderTrashArea(batch, panelX, panelY, panelWidth, panelHeight);
        renderSortButton(batch, panelX, panelY, panelWidth, panelHeight);

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

        // Render dragged item
        if (isDragging && draggedItem != null) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();
            draggedItem.renderIcon(batch, mouseX - SLOT_SIZE / 2f, mouseY - SLOT_SIZE / 2f, SLOT_SIZE);
        }

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
        renderTrashArea(batch, panelX, panelY, panelWidth, panelHeight);
        renderSortButton(batch, panelX, panelY, panelWidth, panelHeight);

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

        renderStatsPanel(batch, panelX, panelY, panelHeight, player);

        if (isDragging && draggedItem != null) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();
            draggedItem.renderIcon(batch, mouseX - SLOT_SIZE / 2f, mouseY - SLOT_SIZE / 2f, SLOT_SIZE);
        }

        batch.end();
    }

    private void renderSortButton(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        float invStartX = panelX + 280;
        float invStartY = panelY + panelHeight - UI_PADDING - 50 - SLOT_SIZE;

        float sortButtonX = invStartX;
        float sortButtonY = invStartY + SLOT_SIZE + 10;

        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();
        boolean isHovered = isPointInRect(mouseX, mouseY, sortButtonX, sortButtonY, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (isHovered) {
            shapeRenderer.setColor(SORT_BUTTON_HOVER_COLOR);
        } else {
            shapeRenderer.setColor(SORT_BUTTON_COLOR);
        }
        shapeRenderer.rect(sortButtonX, sortButtonY, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(sortButtonX, sortButtonY, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(0.6f);
        font.draw(batch, "Sort", sortButtonX + 18, sortButtonY + 20);
        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderTrashArea(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        float trashX = panelX + panelWidth - UI_PADDING - TRASH_SIZE;
        float trashY = panelY + UI_PADDING;

        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();
        boolean isHovered = isDragging && isPointInRect(mouseX, mouseY, trashX, trashY, TRASH_SIZE, TRASH_SIZE);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (isHovered) {
            shapeRenderer.setColor(TRASH_HOVER_COLOR);
        } else {
            shapeRenderer.setColor(TRASH_COLOR);
        }
        shapeRenderer.rect(trashX, trashY, TRASH_SIZE, TRASH_SIZE);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(new Color(0.6f, 0.3f, 0.3f, 1f));
        shapeRenderer.rect(trashX, trashY, TRASH_SIZE, TRASH_SIZE);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        batch.begin();
        batch.draw(trashCan, trashX - 8, trashY + TRASH_SIZE / 2f - 60, 128, 128);
        batch.end();
    }

    private void renderEquipmentPanel(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float equipmentPanelWidth = 250;
        float equipmentX = panelX + UI_PADDING;
        float equipmentY = panelY + UI_PADDING + 40;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.9f);
        shapeRenderer.rect(equipmentX, equipmentY, equipmentPanelWidth, panelHeight - 100);
        shapeRenderer.end();

        float charSize = 100;
        float charX = equipmentX + (equipmentPanelWidth - charSize) / 2f;
        float charY = equipmentY + (panelHeight) / 2f + 20;

        batch.begin();
        batch.draw(characterSprite, charX, charY, charSize, charSize);
        batch.end();

        float leftSlotStartX = equipmentX + 10;
        float leftSlotStartY = equipmentY + panelHeight - 150;

        EquipmentSlot[] leftSlots = {
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST,
                EquipmentSlot.GLOVES,
                EquipmentSlot.BOOTS
        };

        float rightSlotStartX = equipmentX + equipmentPanelWidth - EQUIPMENT_SLOT_SIZE - 10;
        float rightSlotStartY = equipmentY + panelHeight - 150;

        EquipmentSlot[] rightSlots = {
                EquipmentSlot.WEAPON,
                EquipmentSlot.OFFHAND
        };

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

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
                return new Color(1f, 0.85f, 0f, 1f);
            case ItemRegistry.PROTECTOR:
                return new Color(1f, 1f, 1f, 1f);
            case ItemRegistry.BARBARIAN:
                return new Color(0.6f, 0.4f, 0.2f, 1f);
            case ItemRegistry.BERSERKER:
                return new Color(0.9f, 0.2f, 0.2f, 1f);
            case ItemRegistry.DECEPTOR:
                return new Color(0.7f, 0.3f, 0.9f, 1f);
            default:
                return SLOT_BORDER_COLOR;
        }
    }

    private void renderInventorySlots(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        float inventoryStartX = panelX + 280;
        float inventoryStartY = panelY + panelHeight - UI_PADDING - 50 - SLOT_SIZE;

        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;

            float slotX = inventoryStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = inventoryStartY - row * (SLOT_SIZE + SLOT_PADDING);

            boolean isHovered = isPointInRect(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            boolean isDragTarget = isDragging && isHovered && i != dragSourceSlot;
            boolean isDragSource = isDragging && i == dragSourceSlot;

            if (isDragTarget) {
                shapeRenderer.setColor(new Color(0.4f, 0.6f, 0.4f, 0.9f));
            } else if (isDragSource) {
                shapeRenderer.setColor(new Color(0.2f, 0.2f, 0.3f, 0.6f));
            } else if (!selectingEquipmentSlot && i == selectedSlot) {
                shapeRenderer.setColor(SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(SLOT_COLOR);
            }

            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

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
            // Don't render item in source slot if dragging
            if (isDragging && i == dragSourceSlot) continue;

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

            font.setColor(Color.WHITE);
        }
    }

    public float getHealthPotionCooldownRemaining() {
        return healthPotionCooldownTimer;
    }

    public float getHealthPotionCooldownTotal() {
        return HEALTH_POTION_COOLDOWN;
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

    public boolean isDragging() {
        return isDragging;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }
}