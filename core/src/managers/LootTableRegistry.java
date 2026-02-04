package managers;

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
        LootTable basicEnemy = new LootTable()
                .setGuaranteedCoins(1)
                .addDrop("coin", 0.3f, 1, 1)
                .addDrop("attack_potion", 0.01f, 1, 1)
                .addDrop("lucky_clover", 0.01f, 1, 1)
                .addDrop(Item.ItemType.CONSUMABLE, "small_health_potion", 0.15f);
        registerLootTable("basic_enemy", basicEnemy);

        LootTable meleeEnemy = new LootTable()
                .setGuaranteedCoins(1)
                .addDrop("coin", 0.3f, 1, 1)
                .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.01f)
                .addDrop("lucky_clover", 0.005f, 1, 1)
                .addDrop(Item.ItemType.CONSUMABLE, "health_potion", 0.15f);
        registerLootTable("melee_enemy", meleeEnemy);

        LootTable rangedEnemy = new LootTable()
                .setGuaranteedCoins(1) // Always drops 1 coin
                .addDrop("coin", 0.3f, 1, 1)
                .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.01f)
                .addDrop("lucky_clover", 0.005f, 1, 1)
                .addDrop(Item.ItemType.CONSUMABLE, "small_health_potion", 0.2f);
        registerLootTable("ranged_enemy", rangedEnemy);

        LootTable fastEnemy = new LootTable()
                .setGuaranteedCoins(1) // Always drops 1 coin
                .addDrop("coin", 0.3f, 1, 2)
                .addDrop(Item.ItemType.CONSUMABLE, "small_health_potion", 0.15f)
                .addDrop(Item.ItemType.CONSUMABLE, "health_potion", 0.05f);
        registerLootTable("fast_enemy", fastEnemy);

        LootTable dungeonEnemy = new LootTable()
                .setGuaranteedCoins(2)
                .addDrop(Item.ItemType.COIN,"coin_pile", 0.15f)
                .addDrop("coin", 0.2f, 1, 2)
                .addDrop(Item.ItemType.CONSUMABLE, "buff_potion", 0.02f)
                .addDrop("lucky_clover", 0.01f, 1, 1)
                .addDrop(Item.ItemType.CONSUMABLE, "health_potion", 0.25f);
        registerLootTable("dungeon_enemy", dungeonEnemy);

        LootTable boss = new LootTable()
                .setGuaranteedCoins(5)
                .addDrop("coin_pile", 1.0f, 2, 5)
                .addDrop("large_health_potion", 0.8f, 1, 2)
                .addDrop(Item.ItemType.WEAPON,"iron_sword", 0.1f)
                .addDrop(Item.ItemType.ARMOR, "iron_armor", 0.1f)
                .addDrop("health_potion", 1.0f, 2, 3);
        registerLootTable("boss", boss);


        LootTable eliteEnemy = new LootTable()
                .setGuaranteedCoins(3);
        registerLootTable("elite_enemy", eliteEnemy);
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