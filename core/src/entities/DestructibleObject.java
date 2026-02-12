package entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;

public class DestructibleObject {

    private final Rectangle bounds;
    private final Texture texture;
    private Body body;
    private final EnemyStats stats;

    private boolean markedForRemoval = false;

    public DestructibleObject(Rectangle bounds, Texture texture, Body body, EnemyStats stats) {
        this.bounds = bounds;
        this.texture = texture;
        this.body = body;
        this.stats = stats;
        if (this.body != null) {
            this.body.setUserData(this);
        }
    }

    public void render(SpriteBatch batch) {
        if (markedForRemoval) return;
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void takeDamage(int amount) {
        if (markedForRemoval) return;

        stats.setCurrentHealth(stats.getCurrentHealth() - amount);
        if (stats.getCurrentHealth() <= 0) {
            stats.setCurrentHealth(0);
            markedForRemoval = true;
        }
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Body getBody() {
        return body;
    }

    public void clearBody() {
        body = null;
    }

    public EnemyStats getStats() {
        return stats;
    }

    public void dispose() { }
}
