package ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import config.Storage;
import entities.Player;
import items.Item;
import managers.Inventory;
import managers.ItemRegistry;

public class MerchantShop {
    private boolean isOpen = false;
    private Player player;
    private Inventory inventory;

    // Shop inventory
    private Item[] shopItems;
    private static final int GEAR_SLOTS = 7;
    private static final int ITEM_SLOTS = 3;
    private static final int TOTAL_SHOP_SLOTS = GEAR_SLOTS + ITEM_SLOTS;

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Texture coinIconTexture;

    private final int SLOT_SIZE = 50;
    private final int SLOT_PADDING = 8;
    private final int UI_PADDING = 20;

    private final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.15f, 0.95f);
    private final Color SLOT_COLOR = new Color(0.3f, 0.3f, 0.4f, 0.9f);
    private final Color SLOT_BORDER_COLOR = new Color(0.6f, 0.6f, 0.7f, 1f);
    private final Color SELECTED_COLOR = new Color(1f, 1f, 0.5f, 1f);
    private final Color BUY_TAB_COLOR = new Color(0.2f, 0.4f, 0.3f, 0.9f);
    private final Color SELL_TAB_COLOR = new Color(0.4f, 0.2f, 0.3f, 0.9f);
    private final Color HOVER_COLOR = new Color(0.4f, 0.4f, 0.5f, 0.9f);
    private final Color INSUFFICIENT_FUNDS_COLOR = new Color(0.5f, 0.2f, 0.2f, 0.9f);

    private int selectedShopSlot = -1;
    private int selectedInventorySlot = -1;
    private boolean inBuySection = true;

    private ItemRegistry itemRegistry;
    private Random random;

    // Gear item IDs for randomization
    private static final String[] GEAR_ITEM_IDS = {
            // Swords
            "valkyries_iron_sword", "protectors_iron_sword", "barbarians_iron_sword",
            "berserkers_iron_sword", "deceptors_iron_sword",
            // Spears
            "valkyries_iron_spear", "protectors_iron_spear", "barbarians_iron_spear",
            "berserkers_iron_spear", "deceptors_iron_spear",
            // Helmets
            "valkyries_iron_helmet", "protectors_iron_helmet", "barbarians_iron_helmet",
            "berserkers_iron_helmet", "deceptors_iron_helmet",
            // Armor
            "valkyries_iron_armor", "protectors_iron_armor", "barbarians_iron_armor",
            "berserkers_iron_armor", "deceptors_iron_armor",
            // Gloves
            "valkyries_iron_gloves", "protectors_iron_gloves", "barbarians_iron_gloves",
            "berserkers_iron_gloves", "deceptors_iron_gloves",
            // Boots
            "valkyries_iron_boots", "protectors_iron_boots", "barbarians_iron_boots",
            "berserkers_iron_boots", "deceptors_iron_boots",
            // Shields
            "valkyries_iron_shield", "protectors_iron_shield", "barbarians_iron_shield",
            "berserkers_iron_shield", "deceptors_iron_shield"
    };

    private static final String[] CONSUMABLE_ITEM_IDS = {
            "small_health_potion", "health_potion", "large_health_potion"
    };

    public MerchantShop() {
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.coinIconTexture = Storage.assetManager.get("icons/items/Coin.png", Texture.class);
        this.itemRegistry = ItemRegistry.getInstance();
        this.random = new Random();
        this.shopItems = new Item[TOTAL_SHOP_SLOTS];

        randomizeShopInventory();
    }

    public void setPlayer(Player player) {
        this.player = player;
        this.inventory = player.getInventory();
    }

    public void open() {
        isOpen = true;
        selectedShopSlot = -1;
        selectedInventorySlot = -1;
        inBuySection = true;
    }

    public void close() {
        isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void toggle() {
        if (isOpen) {
            close();
        } else {
            open();
        }
    }

    public void randomizeShopInventory() {
        for (int i = 0; i < TOTAL_SHOP_SLOTS; i++) {
            shopItems[i] = null;
        }

        List<String> usedGearIds = new ArrayList<>();
        for (int i = 0; i < GEAR_SLOTS; i++) {
            String gearId;
            do {
                gearId = GEAR_ITEM_IDS[random.nextInt(GEAR_ITEM_IDS.length)];
            } while (usedGearIds.contains(gearId));

            usedGearIds.add(gearId);
            shopItems[i] = itemRegistry.createItem(gearId, new com.badlogic.gdx.math.Vector2(0, 0));
        }

        for (int i = GEAR_SLOTS; i < TOTAL_SHOP_SLOTS; i++) {
            String consumableId = CONSUMABLE_ITEM_IDS[random.nextInt(CONSUMABLE_ITEM_IDS.length)];
            shopItems[i] = itemRegistry.createItem(consumableId, new com.badlogic.gdx.math.Vector2(0, 0));
        }
    }

    public void update(float delta) {
        if (!isOpen || player == null) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            close();
            return;
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int panelWidth = 750;
        int panelHeight = 500;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        // Check tab clicks
        float tabWidth = 100;
        float tabHeight = 30;
        float buyTabX = panelX + UI_PADDING;
        float sellTabX = panelX + UI_PADDING + tabWidth + 10;
        float tabY = panelY + panelHeight - UI_PADDING - tabHeight;

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (isPointInRect(mouseX, mouseY, buyTabX, tabY, tabWidth, tabHeight)) {
                inBuySection = true;
                selectedShopSlot = -1;
                selectedInventorySlot = -1;
            } else if (isPointInRect(mouseX, mouseY, sellTabX, tabY, tabWidth, tabHeight)) {
                inBuySection = false;
                selectedShopSlot = -1;
                selectedInventorySlot = -1;
            }
        }

        if (inBuySection) {
            handleBuySection(mouseX, mouseY, panelX, panelY, panelHeight);
        } else {
            handleSellSection(mouseX, mouseY, panelX, panelY, panelHeight);
        }
    }

    private void handleBuySection(float mouseX, float mouseY, float panelX, float panelY, float panelHeight) {
        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - 80;

        // Check shop slot hover/click
        for (int i = 0; i < TOTAL_SHOP_SLOTS; i++) {
            int row = i / 5;
            int col = i % 5;
            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);

            if (isPointInRect(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                selectedShopSlot = i;

                // Left click to buy
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && shopItems[i] != null) {
                    buyItem(i);
                }
            }
        }
    }

    private void handleSellSection(float mouseX, float mouseY, float panelX, float panelY, float panelHeight) {
        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - 80;

        int itemsPerRow = 7;
        int maxSlots = 28;

        // Check inventory slot hover/click
        for (int i = 0; i < maxSlots; i++) {
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);

            if (isPointInRect(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                selectedInventorySlot = i;

                // Right click to sell
                if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                    sellItem(i);
                }
            }
        }
    }

    private void buyItem(int shopSlot) {
        if (shopSlot < 0 || shopSlot >= TOTAL_SHOP_SLOTS) return;

        Item item = shopItems[shopSlot];
        if (item == null) return;

        int buyPrice = item.getBuyValue();

        if (inventory.getCoins() < buyPrice) {
            System.out.println("Not enough coins to buy " + item.getName());
            return;
        }

        if (inventory.isFull()) {
            System.out.println("Inventory is full!");
            return;
        }

        inventory.removeCoins(buyPrice);
        Item purchasedItem = item.copy();
        inventory.addItem(purchasedItem);

        shopItems[shopSlot] = null;
    }

    private void sellItem(int inventorySlot) {
        Item item = inventory.getItem(inventorySlot);
        if (item == null) return;

        if (item.getType() == Item.ItemType.COIN) return;

        int sellPrice = item.getSellValue();

        inventory.addCoins(sellPrice);
        inventory.removeItem(inventorySlot);
    }

    private boolean isPointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    public void render(SpriteBatch batch, boolean batchIsActive) {
        if (!isOpen) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int panelWidth = 750;
        int panelHeight = 500;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

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

        float tabWidth = 100;
        float tabHeight = 30;
        float buyTabX = panelX + UI_PADDING;
        float sellTabX = panelX + UI_PADDING + tabWidth + 10;
        float tabY = panelY + panelHeight - UI_PADDING - tabHeight;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (inBuySection) {
            shapeRenderer.setColor(BUY_TAB_COLOR);
        } else {
            shapeRenderer.setColor(SLOT_COLOR);
        }
        shapeRenderer.rect(buyTabX, tabY, tabWidth, tabHeight);

        if (!inBuySection) {
            shapeRenderer.setColor(SELL_TAB_COLOR);
        } else {
            shapeRenderer.setColor(SLOT_COLOR);
        }
        shapeRenderer.rect(sellTabX, tabY, tabWidth, tabHeight);

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(buyTabX, tabY, tabWidth, tabHeight);
        shapeRenderer.rect(sellTabX, tabY, tabWidth, tabHeight);
        shapeRenderer.end();

        if (inBuySection) {
            renderBuySection(batch, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
        } else {
            renderSellSection(batch, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
        }

        batch.begin();

        // Title
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        font.draw(batch, "Merchant Shop", panelX + panelWidth / 2f - 80, panelY + panelHeight - UI_PADDING / 2f);

        // Tab labels
        font.getData().setScale(0.7f);
        font.draw(batch, "Buy", buyTabX + 35, tabY + 22);
        font.draw(batch, "Sell", sellTabX + 35, tabY + 22);

        // Coin count
        batch.draw(coinIconTexture, panelX + panelWidth - UI_PADDING - 120,
                panelY + panelHeight - UI_PADDING - 30, 30, 30);
        font.getData().setScale(0.9f);
        font.draw(batch, "" + inventory.getCoins(), panelX + panelWidth - UI_PADDING - 85,
                panelY + panelHeight - UI_PADDING - 5);

        // Render items
        if (inBuySection) {
            renderBuyItems(batch, panelX, panelY, panelHeight);
        } else {
            renderSellItems(batch, panelX, panelY, panelHeight);
        }

        // Render item info
        renderItemInfo(batch, panelX, panelY, panelWidth);

        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);
        batch.end();
    }

    private void renderBuySection(SpriteBatch batch, float panelX, float panelY, float panelWidth,
                                  float panelHeight, float mouseX, float mouseY) {
        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - 80;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < TOTAL_SHOP_SLOTS; i++) {
            int row = i / 5;
            int col = i % 5;
            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);

            boolean isHovered = isPointInRect(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            boolean canAfford = shopItems[i] != null && inventory.getCoins() >= shopItems[i].getBuyValue();

            if (isHovered && shopItems[i] != null) {
                if (canAfford) {
                    shapeRenderer.setColor(HOVER_COLOR);
                } else {
                    shapeRenderer.setColor(INSUFFICIENT_FUNDS_COLOR);
                }
            } else if (i == selectedShopSlot) {
                shapeRenderer.setColor(SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(SLOT_COLOR);
            }

            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            // Border color based on gear type
            Color borderColor = SLOT_BORDER_COLOR;
            if (shopItems[i] != null && shopItems[i].getGearType() != null) {
                borderColor = getGearTypeColor(shopItems[i].getGearType());
            }

            Gdx.gl.glLineWidth(isHovered ? 3 : 2);
            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        shapeRenderer.end();
    }

    private void renderSellSection(SpriteBatch batch, float panelX, float panelY, float panelWidth,
                                   float panelHeight, float mouseX, float mouseY) {
        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - 80;

        int itemsPerRow = 7;
        int maxSlots = 28;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < maxSlots; i++) {
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;
            float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
            float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);

            boolean isHovered = isPointInRect(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            Item item = inventory.getItem(i);

            if (isHovered && item != null) {
                shapeRenderer.setColor(HOVER_COLOR);
            } else if (i == selectedInventorySlot) {
                shapeRenderer.setColor(SELECTED_COLOR);
            } else {
                shapeRenderer.setColor(SLOT_COLOR);
            }

            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            // Border color based on gear type
            Color borderColor = SLOT_BORDER_COLOR;
            if (item != null && item.getGearType() != null) {
                borderColor = getGearTypeColor(item.getGearType());
            }

            Gdx.gl.glLineWidth(isHovered ? 3 : 2);
            shapeRenderer.setColor(borderColor);
            shapeRenderer.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            Gdx.gl.glLineWidth(1);
        }

        shapeRenderer.end();
    }

    private void renderBuyItems(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - 80;

        for (int i = 0; i < TOTAL_SHOP_SLOTS; i++) {
            if (shopItems[i] != null) {
                int row = i / 5;
                int col = i % 5;
                float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
                float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);

                shopItems[i].renderIcon(batch, slotX + 5, slotY + 5, SLOT_SIZE - 10);

                // Draw price
                font.getData().setScale(0.5f);
                font.setColor(Color.GOLD);
                font.draw(batch, "" + shopItems[i].getBuyValue(), slotX + 2, slotY + 14);
            }
        }
    }

    private void renderSellItems(SpriteBatch batch, float panelX, float panelY, float panelHeight) {
        float slotsStartX = panelX + UI_PADDING;
        float slotsStartY = panelY + panelHeight - UI_PADDING - 80;

        int itemsPerRow = 7;
        int maxSlots = 28;

        for (int i = 0; i < maxSlots; i++) {
            Item item = inventory.getItem(i);
            if (item != null) {
                int row = i / itemsPerRow;
                int col = i % itemsPerRow;
                float slotX = slotsStartX + col * (SLOT_SIZE + SLOT_PADDING);
                float slotY = slotsStartY - row * (SLOT_SIZE + SLOT_PADDING);

                item.renderIcon(batch, slotX + 5, slotY + 5, SLOT_SIZE - 10);

                // Draw sell price
                font.getData().setScale(0.5f);
                font.setColor(Color.GREEN);
                font.draw(batch, "" + item.getSellValue(), slotX + 2, slotY + 14);

                // Draw stack count
                int count = inventory.getItemCount(i);
                if (count > 1) {
                    font.setColor(Color.YELLOW);
                    font.draw(batch, "x" + count, slotX + SLOT_SIZE - 20, slotY + 14);
                }
            }
        }
    }

    private void renderItemInfo(SpriteBatch batch, float panelX, float panelY, float panelWidth) {
        Item displayItem = null;
        boolean isBuying = false;

        if (inBuySection && selectedShopSlot >= 0 && selectedShopSlot < TOTAL_SHOP_SLOTS) {
            displayItem = shopItems[selectedShopSlot];
            isBuying = true;
        } else if (!inBuySection && selectedInventorySlot >= 0) {
            displayItem = inventory.getItem(selectedInventorySlot);
            isBuying = false;
        }

        if (displayItem == null) return;

        float infoX = panelX + 350;
        float infoY = panelY + 180;

        font.setColor(Color.WHITE);
        font.getData().setScale(0.8f);
        font.draw(batch, displayItem.getName(), infoX, infoY);

        font.getData().setScale(0.6f);
        int lineOffset = 0;
        float lineSpacing = 20f;

        // Price info
        if (isBuying) {
            font.setColor(Color.GOLD);
            font.draw(batch, "Buy Price: " + displayItem.getBuyValue(), infoX, infoY - 25 - (lineOffset * lineSpacing));
        } else {
            font.setColor(Color.GREEN);
            font.draw(batch, "Sell Price: " + displayItem.getSellValue(), infoX, infoY - 25 - (lineOffset * lineSpacing));
        }
        lineOffset++;

        // Stats
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

        // Instructions
        font.setColor(Color.GRAY);
        font.getData().setScale(0.5f);
        if (isBuying) {
            font.draw(batch, "Left-click to buy", infoX, infoY - 25 - (lineOffset * lineSpacing) - 20);
        } else {
            font.draw(batch, "Right-click to sell", infoX, infoY - 25 - (lineOffset * lineSpacing) - 20);
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

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}