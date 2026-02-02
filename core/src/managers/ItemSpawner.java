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

public class ItemSpawner {
    private List<WorldItem> worldItems;
    private final World world;
    private final ItemRegistry itemRegistry;
    private final Random random;
    private final float PICKUP_RADIUS = 25f;

    // Queue for items to spawn (to avoid Box2D world lock issues)
    private List<PendingItem> pendingItems;

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

    public WorldItem spawnItem(String itemId, Vector2 position) {
        pendingItems.add(new PendingItem(itemId, new Vector2(position)));
        return null;
    }

    private WorldItem spawnItemNow(String itemId, Vector2 position, Vector2 velocity) {
        Item item = itemRegistry.createItem(itemId, position);
        if (item == null) {
            return null;
        }

        Body body = createItemBody(position, item.getBounds().width, item.getBounds().height);
        item.setBody(body);

        if (velocity != null) {
            body.setLinearVelocity(velocity);
        }

        WorldItem worldItem = new WorldItem(item, body);
        worldItems.add(worldItem);

        return worldItem;
    }

    public WorldItem spawnItemWithOffset(String itemId, Vector2 position, float maxOffset) {
        float offsetX = (random.nextFloat() - 0.5f) * maxOffset * 2;
        float offsetY = (random.nextFloat() - 0.5f) * maxOffset * 2;
        Vector2 spawnPos = new Vector2(position.x + offsetX, position.y + offsetY);

        return spawnItem(itemId, spawnPos);
    }

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

    public List<Item> checkPickups(Player player, Inventory inventory) {
        List<Item> pickedUpItems = new ArrayList<>();
        Vector2 playerPos = player.getPosition();

        // Get coin multiplier from player's stats (Lucky Clover buff)
        float coinMultiplier = player.getStats().getCoinMultiplier();

        Iterator<WorldItem> iterator = worldItems.iterator();
        while (iterator.hasNext()) {
            WorldItem worldItem = iterator.next();

            if (worldItem.item.isPlayerNear(playerPos, PICKUP_RADIUS)) {
                boolean added = false;

                if (worldItem.item.getType() == Item.ItemType.COIN) {
                    added = addCoinWithMultiplier(inventory, worldItem.item, coinMultiplier);
                } else {
                    added = inventory.addItem(worldItem.item);
                }

                if (added) {
                    worldItem.item.setPickedUp(true);
                    pickedUpItems.add(worldItem.item);

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

    private boolean addCoinWithMultiplier(Inventory inventory, Item coinItem, float multiplier) {
        int baseValue = coinItem.getBuyValue();
        int multipliedValue = (int)(baseValue * multiplier);

        // Add the multiplied coin value directly
        inventory.addCoins(multipliedValue);
        return true;
    }

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
        fixtureDef.isSensor = true;

        fixtureDef.filter.categoryBits = CollisionFilter.OBSTACLE;
        fixtureDef.filter.maskBits = 0;

        body.createFixture(fixtureDef);
        shape.dispose();

        PolygonShape shape2 = new PolygonShape();
        shape2.setAsBox(width / 5f, height / 5f);

        FixtureDef fixtureDef2 = new FixtureDef();
        fixtureDef2.shape = shape2;
        fixtureDef2.density = 0.5f;
        fixtureDef2.friction = 0.5f;

        fixtureDef2.filter.categoryBits = CollisionFilter.ITEM;
        fixtureDef2.filter.maskBits = CollisionFilter.ITEM | CollisionFilter.WALL;

        body.createFixture(fixtureDef2);
        shape2.dispose();

        return body;
    }

    public void update(float delta) {
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

    public void render(SpriteBatch batch) {
        for (WorldItem worldItem : worldItems) {
            worldItem.render(batch);
        }
    }

    public List<WorldItem> getWorldItems() {
        return worldItems;
    }

    public void clear() {
        for (WorldItem worldItem : worldItems) {
            if (worldItem.body != null && !world.isLocked()) {
                world.destroyBody(worldItem.body);
            }
        }
        worldItems.clear();
        pendingItems.clear();
    }
}