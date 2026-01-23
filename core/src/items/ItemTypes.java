package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import config.Storage;

public abstract class ItemTypes {

    public static class ArmorItem extends Item {

        public ArmorItem(String name, String description, Texture texture,
                         Texture iconTexture, Vector2 position, int defense, int damage,
                         int vitality, int dex, int value) {
            super(name, description, ItemType.ARMOR, texture, iconTexture, position);
            this.defense = defense;
            this.damage = damage;
            this.bonusVitality = vitality;
            this.bonusDex = dex;
            this.value = value;
        }

        // Convenience constructor for backward compatibility
        public ArmorItem(String name, String description, Texture texture,
                         Texture iconTexture, Vector2 position, int defense, int value) {
            this(name, description, texture, iconTexture, position, defense, 0, 0, 0, value);
        }

        @Override
        public void use(entities.Player player) {
            equip(player);
        }

        @Override
        public void equip(entities.Player player) {
            if (defense > 0) player.getStats().addGearDefense(defense);
            if (damage > 0) player.getStats().addGearDamage(damage);
            if (bonusVitality > 0) player.getStats().addGearVitality(bonusVitality);
            if (bonusDex > 0) player.getStats().addGearDex(bonusDex);
        }

        @Override
        public void unequip(entities.Player player) {
            if (defense > 0) player.getStats().removeGearDefense(defense);
            if (damage > 0) player.getStats().removeGearDamage(damage);
            if (bonusVitality > 0) player.getStats().removeGearVitality(bonusVitality);
            if (bonusDex > 0) player.getStats().removeGearDex(bonusDex);
        }

        @Override
        public Item copy() {
            ArmorItem copy = new ArmorItem(name, description, texture, iconTexture,
                    new Vector2(bounds.x, bounds.y), defense, damage, bonusVitality, bonusDex, value);
            copy.setGearType(gearType);
            return copy;
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
        }

        @Override
        public void equip(entities.Player player) {
        }

        @Override
        public void unequip(entities.Player player) {
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
        }

        @Override
        public void unequip(entities.Player player) {
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
                          Texture iconTexture, Vector2 position, int damage, int defense,
                          int vitality, int dex, int value) {
            super(name, description, ItemType.WEAPON, texture, iconTexture, position);
            this.damage = damage;
            this.defense = defense;
            this.bonusVitality = vitality;
            this.bonusDex = dex;
            this.value = value;
            this.attackSpeed = 100;
        }

        // Convenience constructor for backward compatibility
        public WeaponItem(String name, String description, Texture texture,
                          Texture iconTexture, Vector2 position, int damage, int value) {
            this(name, description, texture, iconTexture, position, damage, 0, 0, 0, value);
        }

        @Override
        public void use(entities.Player player) {
            equip(player);
        }

        @Override
        public void equip(entities.Player player) {
            if (damage > 0) player.getStats().addGearDamage(damage);
            if (defense > 0) player.getStats().addGearDefense(defense);
            if (bonusVitality > 0) player.getStats().addGearVitality(bonusVitality);
            if (bonusDex > 0) player.getStats().addGearDex(bonusDex);
        }

        @Override
        public void unequip(entities.Player player) {
            if (damage > 0) player.getStats().removeGearDamage(damage);
            if (defense > 0) player.getStats().removeGearDefense(defense);
            if (bonusVitality > 0) player.getStats().removeGearVitality(bonusVitality);
            if (bonusDex > 0) player.getStats().removeGearDex(bonusDex);
        }

        @Override
        public Item copy() {
            WeaponItem copy = new WeaponItem(name, description, texture, iconTexture,
                    new Vector2(bounds.x, bounds.y), damage, defense, bonusVitality, bonusDex, value);
            copy.setGearType(gearType);
            return copy;
        }

        public int getAttackSpeed() { return attackSpeed; }
        public void setAttackSpeed(int attackSpeed) { this.attackSpeed = attackSpeed; }
    }
}