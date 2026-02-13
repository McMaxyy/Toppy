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

    public static final String VALKYRIE = "valkyrie";
    public static final String PROTECTOR = "protector";
    public static final String BARBARIAN = "barbarian";
    public static final String BERSERKER = "berserker";
    public static final String DECEPTOR = "deceptor";
    public static final String SPECIAL = "special";

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
        public int buyValue;
        public int sellValue;

        public boolean attackPotion;
        public boolean defensePotion;
        public boolean dexPotion;
        public boolean luckyClover;
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
            this.buyValue = 1;
            this.sellValue = 1;
            this.gearType = null;
            this.attackPotion = false;
            this.defensePotion = false;
            this.dexPotion = false;
            this.luckyClover = false;
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

        public ItemDefinition setAttackPotion(boolean attackPotion) {
            this.attackPotion = attackPotion;
            return this;
        }

        public ItemDefinition setDefensePotion(boolean defensePotion) {
            this.defensePotion = defensePotion;
            return this;
        }

        public ItemDefinition setDexPotion(boolean dexPotion) {
            this.dexPotion = dexPotion;
            return this;
        }

        public ItemDefinition setLuckyClover(boolean luckyClover) {
            this.luckyClover = luckyClover;
            return this;
        }

        public ItemDefinition setBuyValue(int buyValue) {
            this.buyValue = buyValue;
            this.sellValue = (int)(buyValue * 0.4f);
            return this;
        }

        public ItemDefinition setSellValue(int sellValue) {
            this.sellValue = sellValue;
            return this;
        }

        public ItemDefinition setValues(int buyValue, int sellValue) {
            this.buyValue = buyValue;
            this.sellValue = sellValue;
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
        ).setBonusVitality(4).setDamage(10).setValues(120, 50).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_sword",
                "Protector's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setBonusVitality(6).setDefense(8).setValues(120, 50).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_sword",
                "Barbarian's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setDamage(10).setDefense(4).setValues(120, 50).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_sword",
                "Berserker's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setDamage(10).setBonusDex(4).setValues(120, 50).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_sword",
                "Deceptor's Iron Sword",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSword.png",
                "icons/gear/ironSword.png"
        ).setBonusDex(4).setDefense(10).setValues(120, 50).setGearType(DECEPTOR));

        // ===== IRON SPEARS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_spear",
                "Valkyrie's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setBonusVitality(4).setDamage(10).setValues(120, 50).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_spear",
                "Protector's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setBonusVitality(6).setDefense(8).setValues(120, 50).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_spear",
                "Barbarian's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setDamage(10).setDefense(4).setValues(120, 50).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_spear",
                "Berserker's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setDamage(10).setBonusDex(4).setValues(120, 50).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_spear",
                "Deceptor's Iron Spear",
                "",
                Item.ItemType.WEAPON,
                "icons/gear/ironSpear.png",
                "icons/gear/ironSpear.png"
        ).setBonusDex(4).setDefense(6).setValues(120, 50).setGearType(DECEPTOR));

        // ===== IRON HELMETS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_helmet",
                "Valkyrie's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setBonusVitality(3).setDamage(6).setValues(180, 75).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_helmet",
                "Protector's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setBonusVitality(3).setDefense(6).setValues(180, 75).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_helmet",
                "Barbarian's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setDamage(6).setDefense(3).setValues(180, 75).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_helmet",
                "Berserker's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setDamage(6).setBonusDex(3).setValues(180, 75).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_helmet",
                "Deceptor's Iron Helmet",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironHelmet.png",
                "icons/gear/ironHelmet.png"
        ).setBonusDex(3).setDefense(6).setValues(180, 75).setGearType(DECEPTOR));

        // ===== IRON ARMOR (CHEST) =====
        registerItem(new ItemDefinition(
                "valkyries_iron_armor",
                "Valkyrie's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setBonusVitality(4).setDamage(8).setValues(250, 100).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_armor",
                "Protector's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setBonusVitality(4).setDefense(8).setValues(250, 100).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_armor",
                "Barbarian's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setDamage(8).setDefense(4).setValues(250, 100).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_armor",
                "Berserker's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setDamage(8).setBonusDex(4).setValues(250, 100).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_armor",
                "Deceptor's Iron Armor",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironChest.png",
                "icons/gear/ironChest.png"
        ).setBonusDex(4).setDefense(8).setValues(250, 100).setGearType(DECEPTOR));

        // ===== IRON GLOVES =====
        registerItem(new ItemDefinition(
                "valkyries_iron_gloves",
                "Valkyrie's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setBonusVitality(6).setDamage(3).setValues(120, 50).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_gloves",
                "Protector's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setBonusVitality(6).setDefense(3).setValues(120, 50).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_gloves",
                "Barbarian's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setDamage(6).setDefense(3).setValues(120, 50).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_gloves",
                "Berserker's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setDamage(6).setBonusDex(3).setValues(120, 50).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_gloves",
                "Deceptor's Iron Gloves",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironGloves.png",
                "icons/gear/ironGloves.png"
        ).setBonusDex(3).setDefense(6).setValues(120, 50).setGearType(DECEPTOR));

        // ===== IRON BOOTS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_boots",
                "Valkyrie's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setBonusVitality(6).setDamage(3).setValues(120, 50).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_boots",
                "Protector's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setBonusVitality(6).setDefense(3).setValues(120, 50).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_boots",
                "Barbarian's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setDamage(6).setDefense(3).setValues(120, 50).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_boots",
                "Berserker's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setDamage(6).setBonusDex(3).setValues(120, 50).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_boots",
                "Deceptor's Iron Boots",
                "",
                Item.ItemType.ARMOR,
                "icons/gear/ironBoots.png",
                "icons/gear/ironBoots.png"
        ).setBonusDex(3).setDefense(6).setValues(120, 50).setGearType(DECEPTOR));

        // ===== IRON SHIELDS =====
        registerItem(new ItemDefinition(
                "valkyries_iron_shield",
                "Valkyrie's Iron Shield",
                "",
                Item.ItemType.OFFHAND,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setBonusVitality(3).setDamage(6).setValues(150, 100).setGearType(VALKYRIE));

        registerItem(new ItemDefinition(
                "protectors_iron_shield",
                "Protector's Iron Shield",
                "",
                Item.ItemType.OFFHAND,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setBonusVitality(3).setDefense(6).setValues(150, 100).setGearType(PROTECTOR));

        registerItem(new ItemDefinition(
                "barbarians_iron_shield",
                "Barbarian's Iron Shield",
                "",
                Item.ItemType.OFFHAND,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setDamage(6).setDefense(3).setValues(150, 100).setGearType(BARBARIAN));

        registerItem(new ItemDefinition(
                "berserkers_iron_shield",
                "Berserker's Iron Shield",
                "",
                Item.ItemType.OFFHAND,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setDamage(6).setBonusDex(3).setValues(150, 100).setGearType(BERSERKER));

        registerItem(new ItemDefinition(
                "deceptors_iron_shield",
                "Deceptor's Iron Shield",
                "",
                Item.ItemType.OFFHAND,
                "icons/gear/ironShield.png",
                "icons/gear/ironShield.png"
        ).setBonusDex(3).setDefense(6).setValues(150, 100).setGearType(DECEPTOR));

        // ===== SR ITEMS =====
        registerItem(new ItemDefinition(
                "speed_shoe",
                "SR Rubber Shoes",
                "Additional cooldown reduction",
                Item.ItemType.ARMOR,
                "icons/gear/Crocs.png",
                "icons/gear/Crocs.png"
        ).setBonusDex(5).setDefense(5).setBonusVitality(5).setDamage(5).setValues(1000, 500).setGearType(SPECIAL));

        registerItem(new ItemDefinition(
                "bullet_vest",
                "SR Bullet Vest",
                "Chance to avoid damage",
                Item.ItemType.ARMOR,
                "icons/gear/BulletVest.png",
                "icons/gear/BulletVest.png"
        ).setBonusDex(7).setDefense(7).setBonusVitality(7).setDamage(7).setValues(1300, 650).setGearType(SPECIAL));

        registerItem(new ItemDefinition(
                "pirate_hat",
                "SR Pirate's Hat",
                "A chance to steal life from enemies",
                Item.ItemType.ARMOR,
                "icons/gear/PirateHat.png",
                "icons/gear/PirateHat.png"
        ).setBonusDex(5).setDefense(5).setBonusVitality(5).setDamage(5).setValues(1000, 500).setGearType(SPECIAL));

        registerItem(new ItemDefinition(
                "boxing_gloves",
                "SR Boxing Gloves",
                "Chance to deal double damage",
                Item.ItemType.ARMOR,
                "icons/gear/BoxingGlove.png",
                "icons/gear/BoxingGlove.png"
        ).setBonusDex(5).setDefense(5).setBonusVitality(5).setDamage(5).setValues(1000, 500).setGearType(SPECIAL));

        // ===== CONSUMABLES =====
        registerItem(new ItemDefinition(
                "health_potion",
                "Health Potion",
                "Restores health",
                Item.ItemType.CONSUMABLE,
                "icons/items/HealthPotion.png",
                "icons/items/HealthPotion.png"
        ).setHealthRestore(75).setValues(40, 15));

        registerItem(new ItemDefinition(
                "small_health_potion",
                "Small Health Potion",
                "Restores small health",
                Item.ItemType.CONSUMABLE,
                "icons/items/SmolHealthPotion.png",
                "icons/items/SmolHealthPotion.png"
        ).setHealthRestore(35).setValues(20, 8));

        registerItem(new ItemDefinition(
                "large_health_potion",
                "Large Health Potion",
                "Restores big health",
                Item.ItemType.CONSUMABLE,
                "icons/items/BigHealthPotion.png",
                "icons/items/BigHealthPotion.png"
        ).setHealthRestore(200).setValues(80, 30));

        registerItem(new ItemDefinition(
                "attack_potion",
                "Attack Potion",
                "Big strong",
                Item.ItemType.CONSUMABLE,
                "icons/items/AttackPotion.png",
                "icons/items/AttackPotion.png"
        ).setAttackPotion(true).setValues(150, 75));

        registerItem(new ItemDefinition(
                "dex_potion",
                "Dex Potion",
                "Big fast",
                Item.ItemType.CONSUMABLE,
                "icons/items/DexPotion.png",
                "icons/items/DexPotion.png"
        ).setDexPotion(true).setValues(150, 75));

        registerItem(new ItemDefinition(
                "defense_potion",
                "Defense Potion",
                "Big tank",
                Item.ItemType.CONSUMABLE,
                "icons/items/DefensePotion.png",
                "icons/items/DefensePotion.png"
        ).setDefensePotion(true).setValues(150, 75));

        registerItem(new ItemDefinition(
                "lucky_clover",
                "Lucky Clover",
                "Good luck",
                Item.ItemType.CONSUMABLE,
                "icons/items/Clover.png",
                "icons/items/Clover.png"
        ).setLuckyClover(true).setValues(300, 200));

        // ===== COINS =====
        registerItem(new ItemDefinition(
                "coin",
                "Coin",
                "Currency",
                Item.ItemType.COIN,
                "icons/items/Coin.png",
                "icons/items/Coin.png"
        ).setValues(1, 1));

        registerItem(new ItemDefinition(
                "coin_pile",
                "Coin Pile",
                "A pile of coins",
                Item.ItemType.COIN,
                "icons/items/PileOfCoins.png",
                "icons/items/PileOfCoins.png"
        ).setValues(5, 5));
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
                        position, def.damage, def.defense, def.bonusVitality, def.bonusDex, def.buyValue, def.sellValue);
                weapon.setGearType(def.gearType);
                return weapon;

            case OFFHAND:
                ItemTypes.OffhandItem offhand = new ItemTypes.OffhandItem(def.name, def.description, texture, iconTexture,
                        position, def.damage, def.defense, def.bonusVitality, def.bonusDex, def.buyValue, def.sellValue);
                offhand.setGearType(def.gearType);
                return offhand;

            case ARMOR:
                ItemTypes.ArmorItem armor = new ItemTypes.ArmorItem(def.name, def.description, texture, iconTexture,
                        position, def.defense, def.damage, def.bonusVitality, def.bonusDex, def.buyValue, def.sellValue);
                armor.setGearType(def.gearType);
                return armor;

            case CONSUMABLE:
                return new ItemTypes.ConsumableItem(def.name, def.description, texture, iconTexture,
                        position, def.healthRestore, def.buyValue, def.sellValue);

            case COIN:
                return new ItemTypes.CoinItem(texture, iconTexture, position, def.buyValue);

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