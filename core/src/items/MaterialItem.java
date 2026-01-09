package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

/**
 * Material items for crafting
 */
public class MaterialItem extends Item {

    public MaterialItem(String name, String description, Texture texture,
                        Texture iconTexture, Vector2 position, int value) {
        super(name, description, ItemType.MATERIAL, texture, iconTexture, position);
        this.value = value;
    }

    @Override
    public void use(entities.Player player) {
        // Materials cannot be used directly
    }

    @Override
    public void equip(entities.Player player) {
        // Materials cannot be equipped
    }

    @Override
    public void unequip(entities.Player player) {
        // Materials cannot be unequipped
    }

    @Override
    public Item copy() {
        return new MaterialItem(name, description, texture, iconTexture,
                new Vector2(bounds.x, bounds.y), value);
    }
}
