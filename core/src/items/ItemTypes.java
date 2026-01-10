package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import config.Storage;

/**
 * Container class for all item type implementations.
 * All item classes are public static inner classes.
 */
public abstract class ItemTypes {

    /**
     * Armor items that can be equipped for defense
     */
    public static class ArmorItem extends Item {

        public ArmorItem(String name, String description, Texture texture,
                         Texture iconTexture, Vector2 position, int defense, int value) {
            super(name, description, ItemType.ARMOR, texture, iconTexture, position);
            this.defense = defense;
            this.value = value;
        }

        @Override
        public void use(entities.Player player) {
            // Armor is equipped, not used directly
            equip(player);
        }

        @Override
        public void equip(entities.Player player) {
            // Apply armor stats to player through equipment system
            player.getStats().setArmorDefense(defense);
            System.out.println("Equipped armor: " + name + " (+" + defense + " defense)");
        }

        @Override
        public void unequip(entities.Player player) {
            // Remove armor stats from player
            player.getStats().setArmorDefense(0);
            System.out.println("Unequipped armor: " + name);
        }

        @Override
        public Item copy() {
            return new ArmorItem(name, description, texture, iconTexture,
                    new Vector2(bounds.x, bounds.y), defense, value);
        }
    }

    /**
     * Coin items for currency
     */
    public static class CoinItem extends Item {
        private int amount;

        public CoinItem(Texture texture, Texture iconTexture, Vector2 position, int amount) {
            super("Coin", "Currency", ItemType.COIN, texture, iconTexture, position);
            this.amount = amount;
            this.value = amount;
            this.texture = Storage.assetManager.get("tiles/coin.png", Texture.class);
        }

        @Override
        public void use(entities.Player player) {
            // Coins are automatically added to currency count
        }

        @Override
        public void equip(entities.Player player) {
            // Coins cannot be equipped
        }

        @Override
        public void unequip(entities.Player player) {
            // Coins cannot be unequipped
        }

        @Override
        public Item copy() {
            return new CoinItem(texture, iconTexture,
                    new Vector2(bounds.x, bounds.y), amount);
        }

        public int getAmount() {
            return amount;
        }
    }

    /**
     * Consumable items that can be used for effects
     */
    public static class ConsumableItem extends Item {

        public ConsumableItem(String name, String description, Texture texture,
                              Texture iconTexture, Vector2 position,
                              int healthRestore, int value) {
            super(name, description, ItemType.CONSUMABLE, texture, iconTexture, position);
            this.healthRestore = healthRestore;
            this.value = value;
        }

        @Override
        public void use(entities.Player player) {
            player.getStats().heal(healthRestore);
            System.out.println("Used " + name + " (+" + healthRestore + " health)");
        }

        @Override
        public void equip(entities.Player player) {
            // Consumables cannot be equipped
        }

        @Override
        public void unequip(entities.Player player) {
            // Consumables cannot be unequipped
        }

        @Override
        public Item copy() {
            return new ConsumableItem(name, description, texture, iconTexture,
                    new Vector2(bounds.x, bounds.y), healthRestore, value);
        }
    }

    /**
     * Weapon items that can be equipped to deal damage
     */
    public static class WeaponItem extends Item {
        private int attackSpeed;

        public WeaponItem(String name, String description, Texture texture,
                          Texture iconTexture, Vector2 position, int damage, int value) {
            super(name, description, ItemType.WEAPON, texture, iconTexture, position);
            this.damage = damage;
            this.value = value;
            this.attackSpeed = 100; // Default attack speed
        }

        @Override
        public void use(entities.Player player) {
            // Weapons are equipped, not used directly
            equip(player);
        }

        @Override
        public void equip(entities.Player player) {
            player.getStats().setWeaponDamage(damage);
            System.out.println("Equipped weapon: " + name + " (+" + damage + " damage)");
        }

        @Override
        public void unequip(entities.Player player) {
            player.getStats().setWeaponDamage(0);
            System.out.println("Unequipped weapon: " + name);
        }

        @Override
        public Item copy() {
            return new WeaponItem(name, description, texture, iconTexture,
                    new Vector2(bounds.x, bounds.y), damage, value);
        }

        public int getAttackSpeed() { return attackSpeed; }
        public void setAttackSpeed(int attackSpeed) { this.attackSpeed = attackSpeed; }
    }
}