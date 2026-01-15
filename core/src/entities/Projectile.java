package entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import config.Storage;
import managers.CollisionFilter;

/**
 * Projectile entity for ranged enemy attacks
 */
public class Projectile {
    private Body body;
    private Vector2 velocity;
    private Vector2 startPosition;
    private float maxDistance;
    private int damage;
    private boolean markForRemoval = false;
    private float size = 6f;
    private Color color;
    private Texture texture;
    private Object owner; // The entity that fired this projectile

    public Projectile(World world, Vector2 startPos, Vector2 direction, float speed,
                      float maxDistance, int damage, Color color, Object owner) {
        this.startPosition = new Vector2(startPos);
        this.maxDistance = maxDistance;
        this.damage = damage;
        this.color = color;
        this.owner = owner;
        this.texture = Storage.assetManager.get("tiles/hpBar.png", Texture.class);

        // Create physics body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(startPos);
        bodyDef.bullet = true; // Enable CCD for fast-moving projectile

        body = world.createBody(bodyDef);

        CircleShape shape = new CircleShape();
        shape.setRadius(size / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.1f;
        fixtureDef.isSensor = true;
        fixtureDef.filter.categoryBits = CollisionFilter.PROJECTILE;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.OBSTACLE | CollisionFilter.ABILITY;

        body.createFixture(fixtureDef);
        body.setUserData(this);
        shape.dispose();

        // Set velocity
        this.velocity = new Vector2(direction).nor().scl(speed);
        body.setLinearVelocity(velocity);
    }

    public void update(float delta) {
        if (markForRemoval) return;

        // Check if projectile has traveled too far
        float distance = body.getPosition().dst(startPosition);
        if (distance >= maxDistance) {
            markForRemoval();
        }
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            Vector2 pos = body.getPosition();

            // Draw a glowing circle effect
            // Outer glow
            batch.setColor(color.r, color.g, color.b, 0.3f);
            batch.draw(texture, pos.x - size, pos.y - size, size * 2, size * 2);

            // Inner circle
            batch.setColor(color.r, color.g, color.b, 0.8f);
            batch.draw(texture, pos.x - size / 2, pos.y - size / 2, size, size);

            // Core (brighter)
            batch.setColor(1f, 1f, 1f, 0.9f);
            batch.draw(texture, pos.x - size / 4, pos.y - size / 4, size / 2, size / 2);

            // Reset color
            batch.setColor(1, 1, 1, 1);
        }
    }

    public void markForRemoval() {
        markForRemoval = true;
    }

    public boolean isMarkedForRemoval() {
        return markForRemoval;
    }

    public void dispose(World world) {
        if (body != null) {
            world.destroyBody(body);
            body = null;
        }
    }

    public Body getBody() {
        return body;
    }

    public int getDamage() {
        return damage;
    }

    public Object getOwner() {
        return owner;
    }

    public Vector2 getPosition() {
        return body != null ? body.getPosition() : null;
    }
}