package entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import config.Storage;
import managers.CollisionFilter;

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
    private Texture projectileTexture; // Custom projectile texture
    private Object owner; // The entity that fired this projectile
    private float rotation; // Rotation angle in degrees

    // Constructor for colored projectiles (legacy)
    public Projectile(World world, Vector2 startPos, Vector2 direction, float speed,
                      float maxDistance, int damage, Color color, Object owner) {
        this(world, startPos, direction, speed, maxDistance, damage, color, owner, null);
    }

    // Constructor with custom texture
    public Projectile(World world, Vector2 startPos, Vector2 direction, float speed,
                      float maxDistance, int damage, Color color, Object owner, Texture projectileTexture) {
        this.startPosition = new Vector2(startPos);
        this.maxDistance = maxDistance;
        this.damage = damage;
        this.color = color;
        this.owner = owner;
        this.texture = Storage.assetManager.get("tiles/hpBar.png", Texture.class);
        this.projectileTexture = projectileTexture;

        this.rotation = MathUtils.atan2(direction.y, direction.x) * MathUtils.radiansToDegrees;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(startPos);
        bodyDef.bullet = true;

        body = world.createBody(bodyDef);

        CircleShape shape = new CircleShape();
        shape.setRadius(size / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.1f;
        fixtureDef.isSensor = true;
        fixtureDef.filter.categoryBits = CollisionFilter.PROJECTILE;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.OBSTACLE | CollisionFilter.ABILITY | CollisionFilter.WALL;

        body.createFixture(fixtureDef);
        body.setUserData(this);
        shape.dispose();

        this.velocity = new Vector2(direction).nor().scl(speed);
        body.setLinearVelocity(velocity);
    }

    public void update(float delta) {
        if (markForRemoval) return;

        float distance = body.getPosition().dst(startPosition);
        if (distance >= maxDistance) {
            markForRemoval();
        }
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            Vector2 pos = body.getPosition();

            if (projectileTexture != null) {
                float width = projectileTexture.getWidth();
                float height = projectileTexture.getHeight();
                float scale = size * 2 / Math.max(width, height);
                float drawWidth = width * scale;
                float drawHeight = height * scale;

                batch.setColor(1, 1, 1, 1);
                batch.draw(projectileTexture,
                        pos.x - drawWidth / 2, pos.y - drawHeight / 2,
                        drawWidth / 2, drawHeight / 2,
                        drawWidth, drawHeight,
                        1f, 1f,
                        rotation,
                        0, 0,
                        (int) width, (int) height,
                        false, false);
            } else {
                batch.setColor(color.r, color.g, color.b, 0.3f);
                batch.draw(texture, pos.x - size, pos.y - size, size * 2, size * 2);

                batch.setColor(color.r, color.g, color.b, 0.8f);
                batch.draw(texture, pos.x - size / 2, pos.y - size / 2, size, size);

                batch.setColor(1f, 1f, 1f, 0.9f);
                batch.draw(texture, pos.x - size / 4, pos.y - size / 4, size / 2, size / 2);

                batch.setColor(1, 1, 1, 1);
            }
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