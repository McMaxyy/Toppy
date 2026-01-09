package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

/**
 * Consumable items that can be used for effects
 */
public class ConsumableItem extends Item {

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
