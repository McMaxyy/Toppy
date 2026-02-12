package managers;

import config.GameScreen;
import items.Item;

import java.util.HashMap;
import java.util.Map;

public class LootTableRegistry {
    private static LootTableRegistry instance;
    private Map<String, LootTable> lootTables;

    private LootTableRegistry() {
        this.lootTables = new HashMap<>();
        registerDefaultLootTables();
    }

    public static LootTableRegistry getInstance() {
        if (instance == null) {
            instance = new LootTableRegistry();
        }
        return instance;
    }

    private void registerDefaultLootTables() {
        if (GameScreen.getGameMode() == 0) {
            LootTable basicEnemy = new LootTable()
                    .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.02f)
                    .addDrop(Item.ItemType.CONSUMABLE, "small_health_potion", 0.15f)
                    .addDrop(Item.ItemType.WEAPON,"iron_sword", 0.02f)
                    .addDrop(Item.ItemType.WEAPON,"iron_spear", 0.02f)
                    .addDrop(Item.ItemType.ARMOR, "iron_boots", 0.02f);
            registerLootTable("basic_enemy", basicEnemy);

            LootTable meleeEnemy = new LootTable()
                    .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.01f)
                    .addDrop(Item.ItemType.CONSUMABLE, "health_potion", 0.05f)
                    .addDrop(Item.ItemType.WEAPON,"iron_sword", 0.02f)
                    .addDrop(Item.ItemType.WEAPON,"iron_spear", 0.02f)
                    .addDrop(Item.ItemType.ARMOR, "iron_gloves", 0.02f);

            registerLootTable("melee_enemy", meleeEnemy);

            LootTable rangedEnemy = new LootTable()
                    .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.01f)
                    .addDrop(Item.ItemType.CONSUMABLE, "small_health_potion", 0.1f)
                    .addDrop(Item.ItemType.WEAPON,"iron_sword", 0.02f)
                    .addDrop(Item.ItemType.WEAPON,"iron_spear", 0.02f)
                    .addDrop(Item.ItemType.ARMOR, "iron_helmet", 0.02f);
            registerLootTable("ranged_enemy", rangedEnemy);

            LootTable dungeonEnemy = new LootTable()
                    .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.02f)
                    .addDrop(Item.ItemType.CONSUMABLE, "health_potion", 0.05f)
                    .addDrop(Item.ItemType.WEAPON,"iron_sword", 0.02f)
                    .addDrop(Item.ItemType.WEAPON,"iron_spear", 0.02f)
                    .addDrop(Item.ItemType.ARMOR, "iron_armor", 0.02f);
            registerLootTable("dungeon_enemy", dungeonEnemy);
        } else {
            LootTable basicEnemy = new LootTable()
                    .setGuaranteedCoins(1)
                    .addDrop("coin", 0.1f, 1, 1)
                    .addDrop("attack_potion", 0.01f, 1, 1)
                    .addDrop("lucky_clover", 0.005f, 1, 1)
                    .addDrop(Item.ItemType.CONSUMABLE, "small_health_potion", 0.15f);
            registerLootTable("basic_enemy", basicEnemy);

            LootTable meleeEnemy = new LootTable()
                    .setGuaranteedCoins(1)
                    .addDrop("coin", 0.1f, 1, 1)
                    .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.01f)
                    .addDrop("lucky_clover", 0.005f, 1, 1)
                    .addDrop(Item.ItemType.CONSUMABLE, "health_potion", 0.05f);
            registerLootTable("melee_enemy", meleeEnemy);

            LootTable rangedEnemy = new LootTable()
                    .setGuaranteedCoins(1) // Always drops 1 coin
                    .addDrop("coin", 0.1f, 1, 1)
                    .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.01f)
                    .addDrop("lucky_clover", 0.005f, 1, 1)
                    .addDrop(Item.ItemType.CONSUMABLE, "small_health_potion", 0.1f);
            registerLootTable("ranged_enemy", rangedEnemy);

            LootTable dungeonEnemy = new LootTable()
                    .setGuaranteedCoins(2)
                    .addDrop(Item.ItemType.COIN,"coin_pile", 0.05f)
                    .addDrop("coin", 0.15f, 1, 2)
                    .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.02f)
                    .addDrop("lucky_clover", 0.001f, 1, 1)
                    .addDrop(Item.ItemType.CONSUMABLE, "health_potion", 0.05f);
            registerLootTable("dungeon_enemy", dungeonEnemy);
        }

        LootTable boss = new LootTable()
                .setGuaranteedCoins(5)
                .addDrop("coin_pile", 1.0f, 2, 3)
                .addDrop("large_health_potion", 0.5f, 1, 2)
                .addDrop(Item.ItemType.WEAPON,"iron_sword", 0.005f)
                .addDrop(Item.ItemType.ARMOR, "iron_armor", 0.005f)
                .addDrop("health_potion", 1.0f, 1, 3);
        registerLootTable("boss", boss);

        LootTable megaBoss = new LootTable()
                .setGuaranteedCoins(5)
                .addDrop("coin_pile", 1.0f, 2, 5)
                .addDrop("large_health_potion", 1f, 2, 3)
                .addDrop(Item.ItemType.WEAPON,"iron_sword", 0.2f)
                .addDrop(Item.ItemType.ARMOR, "iron_armor", 0.2f)
                .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.5f)
                .addDrop("health_potion", 1.0f, 2, 3);
        registerLootTable("mega_boss", megaBoss);

        LootTable lootGoblin = new LootTable()
                .setGuaranteedCoins(1)
                .addDrop("coin_pile", 1.0f, 1, 3)
                .addDrop("coin", 1.0f, 1, 5)
                .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.01f);

        registerLootTable("loot_goblin", lootGoblin);

        LootTable eliteEnemy = new LootTable()
                .setGuaranteedCoins(3);
        registerLootTable("elite_enemy", eliteEnemy);

        LootTable destructible = new LootTable()
                .addDrop("coin", 0.5f, 1, 1);
        registerLootTable("destructible", destructible);
    }

    public void registerLootTable(String enemyType, LootTable lootTable) {
        lootTables.put(enemyType, lootTable);
    }

    public LootTable getLootTable(String enemyType) {
        return lootTables.get(enemyType);
    }

    public boolean hasLootTable(String enemyType) {
        return lootTables.containsKey(enemyType);
    }

    public void spawnLoot(String enemyType, ItemSpawner itemSpawner,
                          com.badlogic.gdx.math.Vector2 position) {
        LootTable table = getLootTable(enemyType);
        if (table != null) {
            table.spawnLoot(itemSpawner, position);
        } else {
            System.err.println("No loot table found for enemy type: " + enemyType);
        }
    }
}