package managers;

import java.util.HashMap;
import java.util.Map;

import items.ItemTypes;
import items.Item;

/**
 * Manages equipped items for the player
 */
public class Equipment {

    public enum EquipmentSlot {
        HELMET,
        CHEST,
        GLOVES,
        BOOTS,
        WEAPON,
        OFFHAND
    }

    private Map<EquipmentSlot, Item> equippedItems;

    public Equipment() {
        this.equippedItems = new HashMap<>();

        // Initialize all slots as empty
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equippedItems.put(slot, null);
        }
    }

    public Item equipItem(Item item, entities.Player player) {
        if (item == null) return null;

        EquipmentSlot slot = getSlotForItem(item);
        if (slot == null) {
            return null;
        }

        Item previousItem = equippedItems.get(slot);

        if (previousItem != null) {
            previousItem.unequip(player);
        }

        equippedItems.put(slot, item);
        item.equip(player);

        return previousItem;
    }

    public Item unequipItem(EquipmentSlot slot, entities.Player player) {
        Item item = equippedItems.get(slot);
        if (item != null) {
            item.unequip(player);
            equippedItems.put(slot, null);
        }
        return item;
    }

    /**
     * Get the item in a specific slot
     */
    public Item getEquippedItem(EquipmentSlot slot) {
        return equippedItems.get(slot);
    }

    /**
     * Check if a slot is empty
     */
    public boolean isSlotEmpty(EquipmentSlot slot) {
        return equippedItems.get(slot) == null;
    }

    /**
     * Determine which slot an item belongs to based on its type and name
     */
    private EquipmentSlot getSlotForItem(Item item) {
        if (item instanceof ItemTypes.WeaponItem) {
            return EquipmentSlot.WEAPON;
        }

        if (item instanceof ItemTypes.ArmorItem) {
            String name = item.getName().toLowerCase();

            // Check for shield/offhand items
            if (name.contains("shield") || name.contains("buckler")) {
                return EquipmentSlot.OFFHAND;
            }

            // Check by name for armor pieces
            if (name.contains("helmet") || name.contains("hat") || name.contains("hood")) {
                return EquipmentSlot.HELMET;
            } else if (name.contains("chest") || name.contains("plate") || name.contains("tunic") ||
                    name.contains("robe") || name.contains("armor")) {
                return EquipmentSlot.CHEST;
            } else if (name.contains("glove") || name.contains("gauntlet") || name.contains("hand")) {
                return EquipmentSlot.GLOVES;
            } else if (name.contains("boot") || name.contains("shoe") || name.contains("feet")) {
                return EquipmentSlot.BOOTS;
            }

            // Default armor to chest if can't determine
            return EquipmentSlot.CHEST;
        }

        // Can't equip this item type
        return null;
    }

    /**
     * Get all equipped items
     */
    public Map<EquipmentSlot, Item> getAllEquippedItems() {
        return equippedItems;
    }

    /**
     * Calculate total defense from all equipped armor
     */
    public int getTotalDefense() {
        int totalDefense = 0;
        for (Item item : equippedItems.values()) {
            if (item != null) {
                totalDefense += item.getDefense();
            }
        }
        return totalDefense;
    }

    /**
     * Get total damage from equipped weapon
     */
    public int getTotalDamage() {
        Item weapon = equippedItems.get(EquipmentSlot.WEAPON);
        return weapon != null ? weapon.getDamage() : 0;
    }
}