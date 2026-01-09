package entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import config.Storage;
import managers.AnimationManager;
import managers.AnimationManager.State;

public class Enemy {
    public Rectangle bounds;
    private final Texture texture;
    private Body body;
    private final Player player;
    private final float detectionRadius = 150f;
    private final float speed = 60f;
    private boolean markForRemoval = false, isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;

    // Stats system
    private EnemyStats stats;
    private Texture healthBarTexture;

    // Damage cooldown (prevent rapid damage)
    private float damageCooldown = 0f;
    private final float DAMAGE_COOLDOWN_TIME = 0.5f;

    public Enemy(Rectangle bounds, Texture texture, Body body, Player player,
                 AnimationManager animationManager, int level) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.texture = texture;
        this.body = body;
        this.player = player;

        // Initialize stats
        this.stats = EnemyStats.Factory.createBasicEnemy(level);
        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

        getAnimationManager().setState(AnimationManager.State.IDLE, "Mushie");
    }

    public void update(float delta) {
        if (markForRemoval) {
            dispose();
            return;
        }

        // Update damage cooldown
        if (damageCooldown > 0) {
            damageCooldown -= delta;
        }

        if (!Player.gameStarted) {
            body.setLinearVelocity(0, 0);
            return;
        }

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        if (isPlayerInRadius()) {
            moveTowardsPlayer();
            isMoving = true;
        } else {
            if (getAnimationManager().getState("Mushie") != State.IDLE) {
                getAnimationManager().setState(State.IDLE, "Mushie");
                isMoving = false;
            }
        }

        if (isMoving && getAnimationManager().getState("Mushie") != State.RUNNING)
            getAnimationManager().setState(State.RUNNING, "Mushie");
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            // Render enemy sprite
            TextureRegion currentFrame = new TextureRegion(getAnimationManager().getMushieCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);

            // Render health bar
            renderHealthBar(batch);
        }
    }

    /**
     * Render health bar above enemy
     */
    private void renderHealthBar(SpriteBatch batch) {
        if (stats.getHealthPercentage() >= 1.0f) {
            return; // Don't show health bar if at full health
        }

        float barWidth = bounds.width;
        float barHeight = 3f;
        float barX = bounds.x;
        float barY = bounds.y + bounds.height + 2f;

        // Background (red)
        batch.setColor(0.8f, 0.1f, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        // Foreground (green) - current health
        float healthWidth = barWidth * stats.getHealthPercentage();
        batch.setColor(0.2f, 0.8f, 0.2f, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        // Reset color
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Take damage from player
     */
    public void takeDamage(int damage) {
        stats.takeDamage(damage);

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    /**
     * Deal damage to player (with cooldown)
     */
    public void damagePlayer() {
        if (damageCooldown <= 0) {
            player.getStats().takeDamage(stats.getDamage());
            damageCooldown = DAMAGE_COOLDOWN_TIME;
        }
    }

    public void dispose() {
        if (body != null && markForRemoval) {
            body.getWorld().destroyBody(body);
            body = null;
        }
    }

    public void removeEnemies() {
        if (body != null) {
            body.getWorld().destroyBody(body);
            body = null;
        }
    }

    public boolean emptyBody(Body body) {
        if (texture == null)
            return false;
        else
            return true;
    }

    public void markForRemoval() {
        markForRemoval = true;
    }

    public boolean isMarkedForRemoval() {
        return markForRemoval;
    }

    public Body getBody() {
        return body;
    }

    public EnemyStats getStats() {
        return stats;
    }

    private boolean isPlayerInRadius() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        float distance = playerPosition.dst(enemyPosition);

        return distance <= detectionRadius;
    }

    private void moveTowardsPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        Vector2 direction = new Vector2(playerPosition.x - enemyPosition.x,
                playerPosition.y - enemyPosition.y).nor();

        body.setLinearVelocity(direction.scl(speed));
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }
}