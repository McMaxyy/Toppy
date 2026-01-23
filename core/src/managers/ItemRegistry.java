package managers;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

import config.Storage;
import items.*;

public class ItemRegistry {
    private static ItemRegistry instance;
    private Map<String, ItemDefinition> itemDefinitions;

    // Gear type prefixes for colored borders
    public static final String VALKYRIE = "valkyrie";
    public static final String PROTECTOR = "protector";
    public static final String BARBARIAN = "barbarian";
    public static final String BERSERKER = "berserker";
    public static final String DECEPTOR = "deceptor";

    public static class ItemDefinition {
        public String id;
        public String name;
        public String description;
        public Item.ItemType type;
        public String texturePath;
        public String iconPath;
        public int damage;
        public int defense;
        public int bonusVitality;
        public int bonusDex;
        public int healthRestore;
        public int value;
        public String gearType;

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
            this.bonusVitality = 0;
            this.bonusDex = 0;
            this.healthRestore = 0;
            this.value = 1;
            this.gearType = null;
        }

        public ItemDefinition setDamage(int damage) {
            this.damage = damage;
            return this;
        }

        public ItemDefinition setDefense(int defense) {
            this.defense = defense;
            return this;
        }

        public ItemDefinition setBonusVitality(int vitality) {
            this.bonusVitality = vitality;
            return this;
        }

        public ItemDefinition setBonusDex(int dex) {
            this.bonusDex = dex;
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

        public ItemDefinition setGearType(String gearType) {
            this.gearType = gearType;
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

    private void registerDefaultItems() {
        // ===== IRON SWORDS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_sword",
                "Valkyrie's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setBonusVitality(1).setDamage(6).setValue(10).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_sword",
                "Protector's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setBonusVitality(1).setDefense(6).setValue(10).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_sword",
                "Barbarian's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setDamage(10).setDefense(6).setValue(10).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_sword",
                "Berserker's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setDamage(10).setBonusDex(1).setValue(10).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_sword",
                "Deceptor's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setBonusDex(1).setDefense(6).setValue(10).setGearType(DECEPTOR));

        // ===== IRON SPEARS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_spear",
                "Valkyrie's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setBonusVitality(2).setDamage(10).setValue(50).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_spear",
                "Protector's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setBonusVitality(2).setDefense(10).setValue(50).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_spear",
                "Barbarian's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setDamage(15).setDefense(10).setValue(50).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_spear",
                "Berserker's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setDamage(15).setBonusDex(1).setValue(50).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_spear",
                "Deceptor's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setBonusDex(2).setDefense(10).setValue(50).setGearType(DECEPTOR));

        // ===== IRON HELMETS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_helmet",
                "Valkyrie's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setBonusVitality(1).setDamage(5).setValue(75).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_helmet",
                "Protector's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setBonusVitality(1).setDefense(5).setValue(75).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_helmet",
                "Barbarian's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setDamage(8).setDefense(5).setValue(75).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_helmet",
                "Berserker's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setDamage(8).setBonusDex(1).setValue(75).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_helmet",
                "Deceptor's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setBonusDex(1).setDefense(5).setValue(75).setGearType(DECEPTOR));

        // ===== IRON ARMOR (CHEST) =====
        registerItem(new ItemDefinition(
                "valkyries_iron_armor",
                "Valkyrie's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setBonusVitality(1).setDamage(8).setValue(100).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_armor",
                "Protector's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setBonusVitality(1).setDefense(8).setValue(100).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_armor",
                "Barbarian's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setDamage(12).setDefense(8).setValue(100).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_armor",
                "Berserker's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setDamage(12).setBonusDex(1).setValue(100).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_armor",
                "Deceptor's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setBonusDex(1).setDefense(8).setValue(100).setGearType(DECEPTOR));

        // ===== IRON GLOVES =====
        registerItem(new ItemDefinition(
                "valkyries_iron_gloves",
                "Valkyrie's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setBonusVitality(1).setDamage(3).setValue(50).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_gloves",
                "Protector's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setBonusVitality(1).setDefense(3).setValue(50).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_gloves",
                "Barbarian's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setDamage(5).setDefense(3).setValue(50).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_gloves",
                "Berserker's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setDamage(5).setBonusDex(1).setValue(50).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_gloves",
                "Deceptor's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setBonusDex(1).setDefense(3).setValue(50).setGearType(DECEPTOR));

        // ===== IRON BOOTS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_boots",
                "Valkyrie's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setBonusVitality(1).setDamage(3).setValue(50).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_boots",
                "Protector's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setBonusVitality(1).setDefense(3).setValue(50).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_boots",
                "Barbarian's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setDamage(5).setDefense(3).setValue(50).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_boots",
                "Berserker's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setDamage(5).setBonusDex(1).setValue(50).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_boots",
                "Deceptor's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setBonusDex(1).setDefense(3).setValue(50).setGearType(DECEPTOR));

        // ===== IRON SHIELDS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_shield",
                "Valkyrie's Iron Shield",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setBonusVitality(1).setDamage(6).setValue(100).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_shield",
                "Protector's Iron Shield",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setBonusVitality(1).setDefense(6).setValue(100).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_shield",
                "Barbarian's Iron Shield",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setDamage(10).setDefense(6).setValue(100).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_shield",
                "Berserker's Iron Shield",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setDamage(10).setBonusDex(1).setValue(100).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_shield",
                "Deceptor's Iron Shield",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setBonusDex(1).setDefense(6).setValue(100).setGearType(DECEPTOR));

        // ===== CONSUMABLES =====
        registerItem(new ItemDefinition(
                "health_potion",
                "Health Potion",
                "Restores 50 health",
                Item.ItemType.CONSUMABLE,
                "icons/items/HealthPotion.png",
                "icons/items/HealthPotion.png"
        ).setHealthRestore(50).setValue(15));

        registerItem(new ItemDefinition(
                "small_health_potion",
                "Small Health Potion",
                "Restores 25 health",
                Item.ItemType.CONSUMABLE,
                "icons/items/HealthPotion.png",
                "icons/items/HealthPotion.png"
        ).setHealthRestore(25).setValue(8));

        registerItem(new ItemDefinition(
                "large_health_potion",
                "Large Health Potion",
                "Restores 100 health",
                Item.ItemType.CONSUMABLE,
                "icons/items/HealthPotion.png",
                "icons/items/HealthPotion.png"
        ).setHealthRestore(100).setValue(30));

        // ===== COINS =====
        registerItem(new ItemDefinition(
                "coin",
                "Coin",
                "Currency",
                Item.ItemType.COIN,
                "icons/items/coin.png",
                "icons/items/coin.png"
        ).setValue(1));

        registerItem(new ItemDefinition(
                "coin_pile",
                "Coin Pile",
                "A pile of coins",
                Item.ItemType.COIN,
                "icons/items/coin.png",
                "icons/items/coin.png"
        ).setValue(5));
    }

    public void registerItem(ItemDefinition definition) {
        itemDefinitions.put(definition.id, definition);
    }

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
                ItemTypes.WeaponItem weapon = new ItemTypes.WeaponItem(def.name, def.description, texture, iconTexture,
                        position, def.damage, def.defense, def.bonusVitality, def.bonusDex, def.value);
                weapon.setGearType(def.gearType);
                return weapon;

            case ARMOR:
                ItemTypes.ArmorItem armor = new ItemTypes.ArmorItem(def.name, def.description, texture, iconTexture,
                        position, def.defense, def.damage, def.bonusVitality, def.bonusDex, def.value);
                armor.setGearType(def.gearType);
                return armor;

            case CONSUMABLE:
                return new ItemTypes.ConsumableItem(def.name, def.description, texture, iconTexture,
                        position, def.healthRestore, def.value);

            case COIN:
                return new ItemTypes.CoinItem(texture, iconTexture, position, def.value);

            default:
                return null;
        }
    }

    public String[] getAllItemIds() {
        return itemDefinitions.keySet().toArray(new String[0]);
    }

    public ItemDefinition getDefinition(String itemId) {
        return itemDefinitions.get(itemId);
    }

    public boolean hasItem(String itemId) {
        return itemDefinitions.containsKey(itemId);
    }
}