package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import config.Storage;

/**
 * Coin items for currency
 */
public class CoinItem extends Item {
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
