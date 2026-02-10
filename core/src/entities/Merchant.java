package entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import managers.AnimationManager;
import managers.CollisionFilter;

public class Merchant {
    private Rectangle bounds;
    private Body body;
    private float stateTime = 0f;
    private boolean isActive = true;
    private final AnimationManager animationManager;

    private static final float INTERACTION_RADIUS = 30f;
    private static final int SIZE = 32;

    public Merchant(float x, float y, World world, AnimationManager animationManager) {
        this.bounds = new Rectangle(x, y, SIZE, SIZE);
        this.body = createBody(world, x, y);
        this.animationManager = animationManager;

        getAnimationManager().setState(AnimationManager.State.IDLE, "Merchant");
    }

    private Body createBody(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + SIZE / 2f, y + SIZE / 2f);

        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(SIZE / 3f, SIZE / 3f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.3f;
        fixtureDef.isSensor = true;
        fixtureDef.filter.categoryBits = CollisionFilter.OBSTACLE;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER;

        body.createFixture(fixtureDef);
        body.setUserData(this);

        shape.dispose();
        return body;
    }

    public void update(float delta) {
        if (isActive) {
            stateTime += delta;
        }
    }

    public void render(SpriteBatch batch) {
        if (!isActive) return;

        TextureRegion currentFrame = new TextureRegion(getAnimationManager().getMerchantCurrentFrame());
        batch.draw(currentFrame, bounds.x, bounds.y, SIZE, SIZE);
    }

    public boolean isPlayerNear(Vector2 playerPos) {
        Vector2 merchantCenter = new Vector2(
                bounds.x + bounds.width / 2f,
                bounds.y + bounds.height / 2f
        );
        return playerPos.dst(merchantCenter) < INTERACTION_RADIUS;
    }

    public void setPosition(float x, float y, World world) {
        bounds.setPosition(x, y);
        if (body != null) {
            body.setTransform(x + SIZE / 2f, y + SIZE / 2f, 0);
        }
    }

    public void disable() {
        isActive = false;
        if (body != null) {
            body.setActive(false);
        }
    }

    public void enable() {
        isActive = true;
        if (body != null) {
            body.setActive(true);
        }
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Body getBody() {
        return body;
    }

    public Vector2 getPosition() {
        return new Vector2(bounds.x, bounds.y);
    }

    public boolean isActive() {
        return isActive;
    }

    public void dispose(World world) {
        if (body != null && world != null) {
            world.destroyBody(body);
            body = null;
        }
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }
}