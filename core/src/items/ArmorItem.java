package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

/**
 * Armor items that can be equipped for defense
 */
public class ArmorItem extends Item {

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