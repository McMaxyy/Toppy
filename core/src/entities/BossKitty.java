package entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import config.Storage;
import managers.AnimationManager;
import managers.AnimationManager.State;

public class BossKitty {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float detectionRadius = 300f;
    private final float speed = 80f;
    private boolean markForRemoval = false, isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;

    // Stats system
    private EnemyStats stats;
    private Texture healthBarTexture;

    // Damage cooldown
    private float damageCooldown = 0f;
    private final float DAMAGE_COOLDOWN_TIME = 1.0f; // Boss attacks slower

    public BossKitty(Rectangle bounds, Body body, Player player,
                     AnimationManager animationManager, int level) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;

        // Initialize boss stats
        this.stats = EnemyStats.Factory.createBoss(level);
        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
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

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        if (isPlayerInRadius()) {
            moveTowardsPlayer();
            isMoving = true;
        } else {
            if (getAnimationManager().getState("BossKitty") != State.IDLE) {
                getAnimationManager().setState(State.IDLE, "BossKitty");
                isMoving = false;
            }
        }

        if (isMoving && getAnimationManager().getState("BossKitty") != State.RUNNING)
            getAnimationManager().setState(State.RUNNING, "BossKitty");
    }

    private void dispose() {
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

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            // Render boss sprite
            TextureRegion currentFrame = new TextureRegion(getAnimationManager().getBossKittyCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);

            // Render boss health bar (larger and always visible)
            renderBossHealthBar(batch);
        }
    }

    /**
     * Render larger health bar for boss
     */
    private void renderBossHealthBar(SpriteBatch batch) {
        float barWidth = bounds.width * 1.5f; // Wider than normal enemies
        float barHeight = 5f; // Taller bar
        float barX = bounds.x - (barWidth - bounds.width) / 2f; // Center it
        float barY = bounds.y + bounds.height + 5f;

        // Background (dark red)
        batch.setColor(0.5f, 0.0f, 0.0f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        // Foreground (gradient based on health)
        float healthPercent = stats.getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        // Color changes from green to yellow to red as health decreases
        float red = 1.0f - (healthPercent * 0.5f);
        float green = healthPercent;
        batch.setColor(red, green, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        // Border
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
     * Deal damage to player
     */
    public void damagePlayer() {
        if (damageCooldown <= 0) {
            player.getStats().takeDamage(stats.getDamage());
            damageCooldown = DAMAGE_COOLDOWN_TIME;
        }
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