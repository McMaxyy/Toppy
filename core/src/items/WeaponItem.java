package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

/**
 * Weapon items that can be equipped to deal damage
 */
public class WeaponItem extends Item {
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

