package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import config.Storage;

public abstract class ItemTypes {

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
        }

        @Override
        public void unequip(entities.Player player) {
            // Remove armor stats from player
            player.getStats().setArmorDefense(0);
        }

        @Override
        public Item copy() {
            return new ArmorItem(name, description, texture, iconTexture,
                    new Vector2(bounds.x, bounds.y), defense, value);
        }
    }

    public static class CoinItem extends Item {
        private int amount;

        public CoinItem(Texture texture, Texture iconTexture, Vector2 position, int amount) {
            super("Coin", "Currency", ItemType.COIN, texture, iconTexture, position);
            this.amount = amount;
            this.value = amount;
            this.texture = Storage.assetManager.get("icons/items/coin.png", Texture.class);
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

    public static class WeaponItem extends Item {
        private int attackSpeed;

        public WeaponItem(String name, String description, Texture texture,
                          Texture iconTexture, Vector2 position, int damage, int value) {
            super(name, description, ItemType.WEAPON, texture, iconTexture, position);
            this.damage = damage;
            this.value = value;
            this.attackSpeed = 100;
        }

        @Override
        public void use(entities.Player player) {
            equip(player);
        }

        @Override
        public void equip(entities.Player player) {
            player.getStats().setWeaponDamage(damage);
        }

        @Override
        public void unequip(entities.Player player) {
            player.getStats().setWeaponDamage(0);
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