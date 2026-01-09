package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.math.Vector2;

/**
 * Loot table system for defining what items enemies drop
 */
public class LootTable {
    private List<LootEntry> entries;
    private int guaranteedCoins;
    private Random random;

    public LootTable() {
        this.entries = new ArrayList<>();
        this.guaranteedCoins = 0;
        this.random = new Random();
    }

    /**
     * Single loot entry with item ID and drop chance
     */
    public static class LootEntry {
        public String itemId;
        public float dropChance; // 0.0 to 1.0
        public int minQuantity;
        public int maxQuantity;

        public LootEntry(String itemId, float dropChance) {
            this(itemId, dropChance, 1, 1);
        }

        public LootEntry(String itemId, float dropChance, int minQuantity, int maxQuantity) {
            this.itemId = itemId;
            this.dropChance = dropChance;
            this.minQuantity = minQuantity;
            this.maxQuantity = maxQuantity;
        }
    }

    /**
     * Add an item to the loot table
     * @param itemId The item ID from ItemRegistry
     * @param dropChance Chance to drop (0.0 = never, 1.0 = always)
     */
    public LootTable addDrop(String itemId, float dropChance) {
        entries.add(new LootEntry(itemId, dropChance));
        return this; // For chaining
    }

    /**
     * Add an item with quantity range
     * @param itemId The item ID from ItemRegistry
     * @param dropChance Chance to drop (0.0 = never, 1.0 = always)
     * @param minQuantity Minimum number to drop
     * @param maxQuantity Maximum number to drop
     */
    public LootTable addDrop(String itemId, float dropChance, int minQuantity, int maxQuantity) {
        entries.add(new LootEntry(itemId, dropChance, minQuantity, maxQuantity));
        return this; // For chaining
    }

    /**
     * Set guaranteed coins (always drop)
     */
    public LootTable setGuaranteedCoins(int amount) {
        this.guaranteedCoins = amount;
        return this;
    }

    /**
     * Roll the loot table and spawn items at position
     * @param itemSpawner The item spawner to use
     * @param position Where to spawn the items
     */
    public void spawnLoot(ItemSpawner itemSpawner, Vector2 position) {
        // Spawn guaranteed coins
        if (guaranteedCoins > 0) {
            String coinType = guaranteedCoins >= 5 ? "coin_pile" : "coin";
            for (int i = 0; i < Math.min(guaranteedCoins, 5); i++) {
                itemSpawner.spawnItemWithOffset(coinType, position, 15f);
            }
        }

        // Roll each loot entry
        for (LootEntry entry : entries) {
            if (random.nextFloat() <= entry.dropChance) {
                // Determine quantity
                int quantity = entry.minQuantity;
                if (entry.maxQuantity > entry.minQuantity) {
                    quantity += random.nextInt(entry.maxQuantity - entry.minQuantity + 1);
                }

                // Spawn the items
                for (int i = 0; i < quantity; i++) {
                    itemSpawner.spawnItemWithOffset(entry.itemId, position, 20f);
                }
            }
        }
    }

    /**
     * Get all entries (for debugging/display)
     */
    public List<LootEntry> getEntries() {
        return entries;
    }
}