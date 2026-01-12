package managers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import entities.Player;
import items.Item;

/**
 * Manages item spawning and world items
 */
public class ItemSpawner {
    private List<WorldItem> worldItems;
    private final World world;
    private final ItemRegistry itemRegistry;
    private final Random random;
    private final float PICKUP_RADIUS = 25f;

    // Queue for items to spawn (to avoid Box2D world lock issues)
    private List<PendingItem> pendingItems;

    /**
     * Wrapper class for items in the world with physics
     */
    public static class WorldItem {
        public Item item;
        public Body body;
        public float lifetime;

        public WorldItem(Item item, Body body) {
            this.item = item;
            this.body = body;
            this.lifetime = 0f;
        }

        public void update(float delta) {
            if (item != null && body != null) {
                // Update item position from physics body
                item.getBounds().setPosition(
                        body.getPosition().x - item.getBounds().width / 2f,
                        body.getPosition().y - item.getBounds().height / 2f
                );
                item.update(delta);
                lifetime += delta;
            }
        }

        public void render(SpriteBatch batch) {
            if (item != null) {
                item.render(batch);
            }
        }
    }

    /**
     * Class for queuing items to spawn (to avoid Box2D lock issues)
     */
    private static class PendingItem {
        String itemId;
        Vector2 position;
        Vector2 velocity;

        PendingItem(String itemId, Vector2 position) {
            this.itemId = itemId;
            this.position = position;
            this.velocity = null;
        }

        PendingItem(String itemId, Vector2 position, Vector2 velocity) {
            this.itemId = itemId;
            this.position = position;
            this.velocity = velocity;
        }
    }

    public ItemSpawner(World world) {
        this.world = world;
        this.worldItems = new ArrayList<>();
        this.pendingItems = new ArrayList<>();
        this.itemRegistry = ItemRegistry.getInstance();
        this.random = new Random();
    }

    /**
     * Spawn an item at a position
     * Items are queued and spawned during update to avoid Box2D lock issues
     */
    public WorldItem spawnItem(String itemId, Vector2 position) {
        // Queue the item to be spawned in update()
        pendingItems.add(new PendingItem(itemId, new Vector2(position)));
        return null; // Will be created in update()
    }

    /**
     * Actually spawn an item (called during update when world is not locked)
     */
    private WorldItem spawnItemNow(String itemId, Vector2 position, Vector2 velocity) {
        Item item = itemRegistry.createItem(itemId, position);
        if (item == null) {
            return null;
        }

        Body body = createItemBody(position, item.getBounds().width, item.getBounds().height);
        item.setBody(body);

        // Apply velocity if provided
        if (velocity != null) {
            body.setLinearVelocity(velocity);
        }

        WorldItem worldItem = new WorldItem(item, body);
        worldItems.add(worldItem);

        return worldItem;
    }

    /**
     * Spawn a random item at a position
     */
    public WorldItem spawnRandomItem(Vector2 position) {
        String[] allItems = itemRegistry.getAllItemIds();
        if (allItems.length == 0) {
            return null;
        }

        String randomItemId = allItems[random.nextInt(allItems.length)];
        return spawnItem(randomItemId, position);
    }

    /**
     * Spawn item with random offset from position
     */
    public WorldItem spawnItemWithOffset(String itemId, Vector2 position, float maxOffset) {
        float offsetX = (random.nextFloat() - 0.5f) * maxOffset * 2;
        float offsetY = (random.nextFloat() - 0.5f) * maxOffset * 2;
        Vector2 spawnPos = new Vector2(position.x + offsetX, position.y + offsetY);

        return spawnItem(itemId, spawnPos);
    }

    /**
     * Drop an item from player inventory
     */
    public WorldItem dropItem(Item item, Vector2 playerPosition) {
        if (item == null) {
            return null;
        }

        // Calculate drop position
        float dropDistance = 30f;
        Vector2 dropPosition = new Vector2(
                playerPosition.x + dropDistance,
                playerPosition.y
        );

        pendingItems.add(new PendingItem(item.getName().toLowerCase().replace(" ", "_"),
                dropPosition));

        return null; // Will be created in update()
    }

    /**
     * Spawn loot when enemy dies
     */
    public void spawnEnemyLoot(Vector2 position, int enemyLevel) {
        // Coins drop chance: 60%
        if (random.nextFloat() < 0.6f) {
            int coinAmount = 1 + random.nextInt(3 + enemyLevel);
            String coinType = coinAmount > 3 ? "coin_pile" : "coin";
            spawnItemWithOffset(coinType, position, 15f);
        }

        // Health potion drop chance: 20%
        if (random.nextFloat() < 0.2f) {
            spawnItemWithOffset("small_health_potion", position, 15f);
        }

        // Rare item drop chance: 5%
        if (random.nextFloat() < 0.05f) {
            String[] rareItems = {"iron_sword", "iron_armor", "large_health_potion"};
            String rareItem = rareItems[random.nextInt(rareItems.length)];
            spawnItemWithOffset(rareItem, position, 15f);
        }
    }

    /**
     * Spawn boss loot (guaranteed good items)
     */
    public void spawnBossLoot(Vector2 position) {
        // Guaranteed coins
        for (int i = 0; i < 5; i++) {
            spawnItemWithOffset("coin_pile", position, 30f);
        }

        // Guaranteed rare item
        String[] bossLoot = {"iron_sword", "iron_armor", "large_health_potion"};
        for (String item : bossLoot) {
            spawnItemWithOffset(item, position, 30f);
        }
    }

    /**
     * Check for item pickups near player
     */
    public List<Item> checkPickups(Player player, Inventory inventory) {
        List<Item> pickedUpItems = new ArrayList<>();
        Vector2 playerPos = player.getPosition();

        Iterator<WorldItem> iterator = worldItems.iterator();
        while (iterator.hasNext()) {
            WorldItem worldItem = iterator.next();

            if (worldItem.item.isPlayerNear(playerPos, PICKUP_RADIUS)) {
                // Try to add to inventory
                if (inventory.addItem(worldItem.item)) {
                    worldItem.item.setPickedUp(true);
                    pickedUpItems.add(worldItem.item);

                    // Destroy physics body
                    if (worldItem.body != null && !world.isLocked()) {
                        world.destroyBody(worldItem.body);
                        worldItem.body = null;
                    }

                    iterator.remove();
                }
            }
        }

        return pickedUpItems;
    }

    /**
     * Create physics body for item
     */
    private Body createItemBody(Vector2 position, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(position.x + width / 2f, position.y + height / 2f);

        Body body = world.createBody(bodyDef);
        body.setFixedRotation(true);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 3f, height / 3f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.5f;
        fixtureDef.isSensor = true; // Items don't block movement

        fixtureDef.filter.categoryBits = CollisionFilter.OBSTACLE;
        fixtureDef.filter.maskBits = 0; // Don't collide with anything

        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    /**
     * Update all world items
     */
    public void update(float delta) {
        // Process pending items (spawn them now that world is not locked)
        if (!pendingItems.isEmpty() && !world.isLocked()) {
            for (PendingItem pending : pendingItems) {
                spawnItemNow(pending.itemId, pending.position, pending.velocity);
            }
            pendingItems.clear();
        }

        // Update existing items
        for (WorldItem worldItem : worldItems) {
            worldItem.update(delta);
        }
    }

    /**
     * Render all world items
     */
    public void render(SpriteBatch batch) {
        for (WorldItem worldItem : worldItems) {
            worldItem.render(batch);
        }
    }

    /**
     * Get all world items
     */
    public List<WorldItem> getWorldItems() {
        return worldItems;
    }

    /**
     * Clear all world items
     */
    public void clear() {
        for (WorldItem worldItem : worldItems) {
            if (worldItem.body != null && !world.isLocked()) {
                world.destroyBody(worldItem.body);
            }
        }
        worldItems.clear();
        pendingItems.clear(); // Also clear pending items
    }

    /**
     * Remove items that have been in the world too long (optional cleanup)
     */
    public void cleanupOldItems(float maxLifetime) {
        Iterator<WorldItem> iterator = worldItems.iterator();
        while (iterator.hasNext()) {
            WorldItem worldItem = iterator.next();

            if (worldItem.lifetime > maxLifetime) {
                if (worldItem.body != null && !world.isLocked()) {
                    world.destroyBody(worldItem.body);
                }
                iterator.remove();
            }
        }
    }
}