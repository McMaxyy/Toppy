package items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Base class for all items in the game.
 * Each item has a type, texture, stats, and can be picked up or dropped.
 */
public abstract class Item {
    protected String name;
    protected String description;
    protected ItemType type;
    protected Texture texture;
    protected Texture iconTexture;
    protected Rectangle bounds;
    protected Body body;
    protected boolean pickedUp = false;

    // Item stats (can be 0 if not applicable)
    protected int damage;
    protected int defense;
    protected int healthRestore;
    protected int value; // Coin value when sold

    public enum ItemType {
        WEAPON,
        ARMOR,
        CONSUMABLE,
        COIN
    }

    public Item(String name, String description, ItemType type,
                Texture texture, Texture iconTexture, Vector2 position) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.texture = texture;
        this.iconTexture = iconTexture;
        this.bounds = new Rectangle(position.x, position.y, 16, 16);
        this.damage = 0;
        this.defense = 0;
        this.healthRestore = 0;
        this.value = 0;
    }

    /**
     * Called when the item is used (from inventory)
     */
    public abstract void use(entities.Player player);

    /**
     * Called when the item is equipped (for weapons/armor)
     */
    public abstract void equip(entities.Player player);

    /**
     * Called when the item is unequipped
     */
    public abstract void unequip(entities.Player player);

    /**
     * Update item in world (bobbing animation, etc.)
     */
    public void update(float delta) {
        if (!pickedUp && bounds != null) {
            // Add bobbing effect for items on ground
            float bobOffset = (float) Math.sin(System.currentTimeMillis() / 200.0) * 2f;
            bounds.y += bobOffset * delta * 10;
        }
    }

    /**
     * Render item in world
     */
    public void render(SpriteBatch batch) {
        if (!pickedUp && texture != null && bounds != null) {
            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /**
     * Render item icon in inventory
     */
    public void renderIcon(SpriteBatch batch, float x, float y, float size) {
        if (iconTexture != null) {
            batch.draw(iconTexture, x, y, size, size);
        }
    }

    /**
     * Check if player is close enough to pick up
     */
    public boolean isPlayerNear(Vector2 playerPos, float pickupRadius) {
        if (pickedUp) return false;

        Vector2 itemCenter = new Vector2(
                bounds.x + bounds.width / 2f,
                bounds.y + bounds.height / 2f
        );

        return playerPos.dst(itemCenter) < pickupRadius;
    }

    /**
     * Create a copy of this item (for stacking, etc.)
     */
    public abstract Item copy();

    /**
     * Check if this item can stack with another
     */
    public boolean canStackWith(Item other) {
        return this.getClass().equals(other.getClass()) &&
                this.name.equals(other.name) &&
                this.type != ItemType.WEAPON &&
                this.type != ItemType.ARMOR;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ItemType getType() { return type; }
    public Rectangle getBounds() { return bounds; }
    public Body getBody() { return body; }
    public void setBody(Body body) { this.body = body; }
    public boolean isPickedUp() { return pickedUp; }
    public void setPickedUp(boolean pickedUp) { this.pickedUp = pickedUp; }
    public int getDamage() { return damage; }
    public int getDefense() { return defense; }
    public int getHealthRestore() { return healthRestore; }
    public int getValue() { return value; }
    public Texture getTexture() { return texture; }
    public Texture getIconTexture() { return iconTexture; }
}