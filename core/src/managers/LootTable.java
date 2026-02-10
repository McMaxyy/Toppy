package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.math.Vector2;
import items.Item;

public class LootTable {
    private List<LootEntry> entries;
    private int guaranteedCoins;
    private Random random;

    public LootTable() {
        this.entries = new ArrayList<>();
        this.guaranteedCoins = 0;
        this.random = new Random();
    }

    public static class LootEntry {
        public String itemId;
        public Item.ItemType itemType;
        public float dropChance;
        public int minQuantity;
        public int maxQuantity;

        public LootEntry(String itemId, float dropChance) {
            this(null, itemId, dropChance, 1, 1);
        }

        public LootEntry(String itemId, float dropChance, int minQuantity, int maxQuantity) {
            this(null, itemId, dropChance, minQuantity, maxQuantity);
        }

        public LootEntry(Item.ItemType itemType, String itemId, float dropChance) {
            this(itemType, itemId, dropChance, 1, 1);
        }

        public LootEntry(Item.ItemType itemType, String itemId, float dropChance, int minQuantity, int maxQuantity) {
            this.itemType = itemType;
            this.itemId = itemId;
            this.dropChance = dropChance;
            this.minQuantity = minQuantity;
            this.maxQuantity = maxQuantity;
        }
    }

    public LootTable addDrop(Item.ItemType itemType, String itemId, float dropChance) {
        entries.add(new LootEntry(itemType, itemId, dropChance, 1, 1));
        return this;
    }

    public LootTable addDrop(String itemId, float dropChance, int minQuantity, int maxQuantity) {
        entries.add(new LootEntry(null, itemId, dropChance, minQuantity, maxQuantity));
        return this;
    }

    public LootTable setGuaranteedCoins(int amount) {
        this.guaranteedCoins = amount;
        return this;
    }

    private String randomizeItemId(Item.ItemType itemType, String baseItemId) {
        if (itemType == null) {
            return baseItemId;
        }

        String[] prefixes = {"valkyries_", "protectors_", "barbarians_", "berserkers_", "deceptors_"};
        String randomPrefix = prefixes[random.nextInt(5)];

        switch (itemType) {
            case WEAPON:
                if (baseItemId.equals("iron_sword")) {
                    return randomPrefix + "iron_sword";
                } else if (baseItemId.equals("iron_spear")) {
                    return randomPrefix + "iron_spear";
                }
                break;

            case ARMOR:
                if (baseItemId.equals("iron_helmet")) {
                    return randomPrefix + "iron_helmet";
                } else if (baseItemId.equals("iron_armor")) {
                    return randomPrefix + "iron_armor";
                } else if (baseItemId.equals("iron_gloves")) {
                    return randomPrefix + "iron_gloves";
                } else if (baseItemId.equals("iron_boots")) {
                    return randomPrefix + "iron_boots";
                }
                break;

            case OFFHAND:
                if (baseItemId.equals("iron_shield")) {
                    return randomPrefix + "iron_shield";
                }
                break;
            case CONSUMABLE:
                if (baseItemId.equals("buff_potion")) {
                    int buff = random.nextInt(3);
                    if (buff == 0)
                        return "attack_potion";
                    else if (buff == 1)
                        return "dex_potion";
                    else
                        return "defense_potion";
                }
                break;
            default:
                break;
        }

        return baseItemId;
    }

    public void spawnLoot(ItemSpawner itemSpawner, Vector2 position) {
        if (guaranteedCoins > 0) {
            String coinType = guaranteedCoins >= 5 ? "coin_pile" : "coin";
            for (int i = 0; i < Math.min(guaranteedCoins, 5); i++) {
                itemSpawner.spawnItemWithOffset(coinType, position, 15f);
            }
        }

        for (LootEntry entry : entries) {
            if (random.nextFloat() <= entry.dropChance) {
                int quantity = entry.minQuantity;
                if (entry.maxQuantity > entry.minQuantity) {
                    quantity += random.nextInt(entry.maxQuantity - entry.minQuantity + 1);
                }

                String finalItemId = randomizeItemId(entry.itemType, entry.itemId);

                for (int i = 0; i < quantity; i++) {
                    itemSpawner.spawnItemWithOffset(finalItemId, position, 20f);
                }
            }
        }
    }

    public List<LootEntry> getEntries() {
        return entries;
    }
}