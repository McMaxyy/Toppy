package entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import config.Storage;
import managers.CollisionFilter;

public class Portal {
    private Rectangle bounds;
    private Body body;
    private Texture texture;
    private float animationTimer = 0f;
    private boolean isCleared = false;

    public Portal(float x, float y, float size, World world, boolean isCleared) {
        this.bounds = new Rectangle(x, y, size, size);
        this.texture = Storage.assetManager.get("tiles/Portal.png", Texture.class); // Placeholder texture

        createBody(world, x, y, size);
    }

    private void createBody(World world, float x, float y, float size) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + size / 2f, y + size / 2f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(size / 2f, size / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true; // Portal is a trigger
        fixtureDef.filter.categoryBits = CollisionFilter.OBSTACLE;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER;

        body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        body.setUserData(this); // Store reference for collision detection

        shape.dispose();
    }

    public void update(float delta) {
        animationTimer += delta;
    }

    public void render(SpriteBatch batch) {
        // Pulsing effect
        float scale = 1f + (float) Math.sin(animationTimer * 3f) * 0.1f;
        float size = bounds.width * scale;
        float offset = (bounds.width - size) / 2f;

//        batch.setColor(0.5f, 0.2f, 1f, 0.8f);
        if (!isCleared) {
            batch.draw(texture,
                    bounds.x + offset,
                    bounds.y + offset,
                    size,
                    size);
        }
        else {
            batch.setColor(0.5f, 0.2f, 1f, 0.8f);
            batch.draw(texture,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.width);
        }
        batch.setColor(1, 1, 1, 1);
    }

    public boolean isPlayerNear(Vector2 playerPos, float radius) {
        Vector2 portalCenter = new Vector2(bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f);
        return playerPos.dst(portalCenter) < radius;
    }

    public Body getBody() {
        return body;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void dispose(World world) {
        if (body != null) {
            world.destroyBody(body);
            body = null;
        }
    }

    public boolean getIsCleared() {
        return isCleared;
    }

    public void setIsCleared(boolean clear) {
        isCleared = clear;
    }
}
