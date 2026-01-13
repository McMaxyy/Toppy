package managers;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all loot tables
 * Manages loot tables for different enemy types
 */
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

    /**
     * Register all default loot tables for different enemy types
     */
    private void registerDefaultLootTables() {
        // ===== BASIC ENEMY (Mushie) - Level 1 =====
        LootTable basicEnemy = new LootTable()
                .setGuaranteedCoins(1) // Always drops 1 coin
                .addDrop("coin", 0.3f, 1, 2) // 30% chance for 1-2 extra coins
                .addDrop("small_health_potion", 0.15f) // 15% chance for small health potion// 25% chance for wood
                .addDrop("health_potion", 0.05f); // 5% chance for regular health potion

        registerLootTable("basic_enemy", basicEnemy);

        LootTable rangedEnemy = new LootTable()
                .setGuaranteedCoins(1) // Always drops 1 coin
                .addDrop("coin", 0.3f, 1, 2) // 30% chance for 1-2 extra coins
                .addDrop("small_health_potion", 0.15f) // 15% chance for small health potion) // 25% chance for wood
                .addDrop("health_potion", 0.05f); // 5% chance for regular health potion

        registerLootTable("ranged_enemy", rangedEnemy);

        LootTable fastEnemy = new LootTable()
                .setGuaranteedCoins(1) // Always drops 1 coin
                .addDrop("coin", 0.3f, 1, 2) // 30% chance for 1-2 extra coins
                .addDrop("small_health_potion", 0.15f) // 15% chance for small health potion) // 25% chance for wood
                .addDrop("health_potion", 0.05f); // 5% chance for regular health potion

        registerLootTable("fast_enemy", fastEnemy);

        // ===== DUNGEON ENEMY (Dungeon Mushie) - Level 2 =====
        LootTable dungeonEnemy = new LootTable()
                .setGuaranteedCoins(2) // Always drops 2 coins
                .addDrop("coin_pile", 0.5f) // 50% chance for coin pile (5 coins)
                .addDrop("iron_gloves", 0.1f) // 30% chance for health potion
                .addDrop("iron_boots", 0.1f) // 10% chance for large health potion
                .addDrop("iron_helmet", 0.1f) // 8% chance for wooden sword
                .addDrop("iron_armor", 0.1f); // 8% chance for leather armor

        registerLootTable("dungeon_enemy", dungeonEnemy);

        // ===== BOSS (Boss Kitty) - Level 3+ =====
        LootTable boss = new LootTable()
                .setGuaranteedCoins(5) // Always drops 5 coins
                .addDrop("coin_pile", 1.0f, 3, 5) // Always drops 3-5 coin piles
                .addDrop("large_health_potion", 0.8f, 1, 2) // 80% chance for 1-2 large potions
                .addDrop("iron_spear", 0.5f) // 50% chance for iron sword
                .addDrop("iron_armor", 0.5f) // 50% chance for iron armor
                .addDrop("health_potion", 1.0f, 2, 3); // Always drops 2-3 health potions

        registerLootTable("boss", boss);

        // ===== CUSTOM ENEMY TYPES =====
        // You can add more enemy types here

        // Forest Enemy Example
        LootTable forestEnemy = new LootTable()
                .setGuaranteedCoins(1)
                .addDrop("wood", 0.5f, 1, 3) // 50% chance for 1-3 wood
                .addDrop("small_health_potion", 0.2f);

        registerLootTable("forest_enemy", forestEnemy);

        // Elite Enemy Example (stronger than basic, weaker than boss)
        LootTable eliteEnemy = new LootTable()
                .setGuaranteedCoins(3)
                .addDrop("coin_pile", 0.7f)
                .addDrop("health_potion", 0.4f)
                .addDrop("iron_ore", 0.3f)
                .addDrop("iron_spear", 0.15f)
                .addDrop("iron_armor", 0.15f);

        registerLootTable("elite_enemy", eliteEnemy);
    }

    /**
     * Register a custom loot table
     */
    public void registerLootTable(String enemyType, LootTable lootTable) {
        lootTables.put(enemyType, lootTable);
    }

    /**
     * Get a loot table by enemy type
     */
    public LootTable getLootTable(String enemyType) {
        return lootTables.get(enemyType);
    }

    /**
     * Check if a loot table exists
     */
    public boolean hasLootTable(String enemyType) {
        return lootTables.containsKey(enemyType);
    }

    /**
     * Quick method to spawn loot for an enemy type
     */
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