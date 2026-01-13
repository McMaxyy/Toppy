package managers;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

import config.Storage;
import items.*;

/**
 * Central registry for all item definitions in the game.
 * Makes it easy to create items and add new ones.
 */
public class ItemRegistry {
    private static ItemRegistry instance;
    private Map<String, ItemDefinition> itemDefinitions;

    /**
     * Item definition class for registering new items
     */
    public static class ItemDefinition {
        public String id;
        public String name;
        public String description;
        public Item.ItemType type;
        public String texturePath;
        public String iconPath;
        public int damage;
        public int defense;
        public int healthRestore;
        public int value;

        public ItemDefinition(String id, String name, String description,
                              Item.ItemType type, String texturePath, String iconPath) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.texturePath = texturePath;
            this.iconPath = iconPath;
            this.damage = 0;
            this.defense = 0;
            this.healthRestore = 0;
            this.value = 1;
        }

        public ItemDefinition setDamage(int damage) {
            this.damage = damage;
            return this;
        }

        public ItemDefinition setDefense(int defense) {
            this.defense = defense;
            return this;
        }

        public ItemDefinition setHealthRestore(int healthRestore) {
            this.healthRestore = healthRestore;
            return this;
        }

        public ItemDefinition setValue(int value) {
            this.value = value;
            return this;
        }
    }

    private ItemRegistry() {
        itemDefinitions = new HashMap<>();
        registerDefaultItems();
    }

    public static ItemRegistry getInstance() {
        if (instance == null) {
            instance = new ItemRegistry();
        }
        return instance;
    }

    /**
     * Register all default items
     */
    private void registerDefaultItems() {
        // ===== WEAPONS =====
        registerItem(new ItemDefinition(
                "wooden_sword",
                "Wooden Sword",
                "A basic wooden sword",
                Item.ItemType.WEAPON,
                "character/Spear.png",  // Placeholder
                "character/Spear.png"   // Placeholder
        ).setDamage(5).setValue(10));

        registerItem(new ItemDefinition(
                "iron_spear",
                "Iron Spear",
                "A sturdy iron spear",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",  // Placeholder
                "icons/gear/ironSpear.png"   // Placeholder
        ).setDamage(10).setValue(50));

        registerItem(new ItemDefinition(
                "spear",
                "Spear",
                "A sharp spear for ranged attacks",
                Item.ItemType.WEAPON,
                "character/Spear.png",
                "character/Spear.png"
        ).setDamage(8).setValue(30));

        // ===== CHEST ARMOR =====
        registerItem(new ItemDefinition(
                "leather_armor",
                "Leather Armor",
                "Basic leather chest protection",
                Item.ItemType.ARMOR,
                "tiles/green_tile.png",  // Placeholder
                "tiles/green_tile.png"   // Placeholder
        ).setDefense(3).setValue(20));

        registerItem(new ItemDefinition(
                "iron_armor",
                "Iron Armor",
                "Strong iron chest protection",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",  // Placeholder
                "icons/gear/ironChest.png"   // Placeholder
        ).setDefense(8).setValue(100));

        // ===== HELMETS =====
        registerItem(new ItemDefinition(
                "leather_helmet",
                "Leather Helmet",
                "Basic leather head protection",
                Item.ItemType.ARMOR,
                "tiles/green_tile.png",
                "tiles/green_tile.png"
        ).setDefense(2).setValue(15));

        registerItem(new ItemDefinition(
                "iron_helmet",
                "Iron Helmet",
                "Strong iron head protection",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setDefense(5).setValue(75));

        // ===== GLOVES =====
        registerItem(new ItemDefinition(
                "leather_gloves",
                "Leather Gloves",
                "Basic leather hand protection",
                Item.ItemType.ARMOR,
                "tiles/green_tile.png",  // Placeholder
                "tiles/green_tile.png"   // Placeholder
        ).setDefense(1).setValue(10));

        registerItem(new ItemDefinition(
                "iron_gloves",
                "Iron Gloves",
                "Heavy iron gauntlets",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",  // Placeholder
                "icons/gear/ironGloves.png"   // Placeholder
        ).setDefense(3).setValue(50));

        // ===== BOOTS =====
        registerItem(new ItemDefinition(
                "leather_boots",
                "Leather Boots",
                "Basic leather footwear",
                Item.ItemType.ARMOR,
                "tiles/green_tile.png",  // Placeholder
                "tiles/green_tile.png"   // Placeholder
        ).setDefense(1).setValue(10));

        registerItem(new ItemDefinition(
                "iron_boots",
                "Iron Boots",
                "Heavy iron boots",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",  // Placeholder
                "icons/gear/ironBoots.png"   // Placeholder
        ).setDefense(3).setValue(50));

        // ===== SHIELDS (OFF-HAND) =====
        registerItem(new ItemDefinition(
                "wooden_shield",
                "Wooden Shield",
                "Basic wooden shield for blocking",
                Item.ItemType.ARMOR,
                "tiles/green_tile.png",  // Placeholder
                "tiles/green_tile.png"   // Placeholder
        ).setDefense(3).setValue(20));

        registerItem(new ItemDefinition(
                "iron_shield",
                "Iron Shield",
                "Heavy iron shield with excellent protection",
                Item.ItemType.ARMOR,
                "icons/gear/ironShield.png",  // Placeholder
                "icons/gear/ironShield.png"   // Placeholder
        ).setDefense(8).setValue(100));

        registerItem(new ItemDefinition(
                "buckler",
                "Buckler",
                "Small, light shield for parrying",
                Item.ItemType.ARMOR,
                "tiles/green_tile.png",  // Placeholder
                "tiles/green_tile.png"   // Placeholder
        ).setDefense(2).setValue(15));

        // ===== CONSUMABLES =====
        registerItem(new ItemDefinition(
                "health_potion",
                "Health Potion",
                "Restores 50 health",
                Item.ItemType.CONSUMABLE,
                "tiles/coin.png",  // Placeholder
                "tiles/coin.png"   // Placeholder
        ).setHealthRestore(50).setValue(15));

        registerItem(new ItemDefinition(
                "small_health_potion",
                "Small Health Potion",
                "Restores 25 health",
                Item.ItemType.CONSUMABLE,
                "tiles/coin.png",  // Placeholder
                "tiles/coin.png"   // Placeholder
        ).setHealthRestore(25).setValue(8));

        registerItem(new ItemDefinition(
                "large_health_potion",
                "Large Health Potion",
                "Restores 100 health",
                Item.ItemType.CONSUMABLE,
                "tiles/coin.png",  // Placeholder
                "tiles/coin.png"   // Placeholder
        ).setHealthRestore(100).setValue(30));

        // ===== COINS =====
        registerItem(new ItemDefinition(
                "coin",
                "Coin",
                "Currency",
                Item.ItemType.COIN,
                "tiles/coin.png",
                "tiles/coin.png"
        ).setValue(1));

        registerItem(new ItemDefinition(
                "coin_pile",
                "Coin Pile",
                "A pile of coins",
                Item.ItemType.COIN,
                "tiles/coin.png",
                "tiles/coin.png"
        ).setValue(5));
    }

    /**
     * Register a new item definition
     */
    public void registerItem(ItemDefinition definition) {
        itemDefinitions.put(definition.id, definition);
    }

    /**
     * Create an item instance from a registered definition
     */
    public Item createItem(String itemId, Vector2 position) {
        ItemDefinition def = itemDefinitions.get(itemId);
        if (def == null) {
            System.err.println("Item not found: " + itemId);
            return null;
        }

        Texture texture = Storage.assetManager.get(def.texturePath, Texture.class);
        Texture iconTexture = Storage.assetManager.get(def.iconPath, Texture.class);

        switch (def.type) {
            case WEAPON:
                return new ItemTypes.WeaponItem(def.name, def.description, texture, iconTexture,
                        position, def.damage, def.value);

            case ARMOR:
                return new ItemTypes.ArmorItem(def.name, def.description, texture, iconTexture,
                        position, def.defense, def.value);

            case CONSUMABLE:
                return new ItemTypes.ConsumableItem(def.name, def.description, texture, iconTexture,
                        position, def.healthRestore, def.value);

            case COIN:
                return new ItemTypes.CoinItem(texture, iconTexture, position, def.value);

            default:
                return null;
        }
    }

    /**
     * Get all registered item IDs
     */
    public String[] getAllItemIds() {
        return itemDefinitions.keySet().toArray(new String[0]);
    }

    /**
     * Get item definition by ID
     */
    public ItemDefinition getDefinition(String itemId) {
        return itemDefinitions.get(itemId);
    }

    /**
     * Check if an item is registered
     */
    public boolean hasItem(String itemId) {
        return itemDefinitions.containsKey(itemId);
    }
}