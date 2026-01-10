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
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;

    // Stats system
    private EnemyStats stats;
    private Texture healthBarTexture;

    // Attack system
    private float attackCooldown = 0f;
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private boolean hasDealtDamage = false; // Track if damage was dealt this attack

    public Enemy(Rectangle bounds, Texture texture, Body body, Player player,
                 AnimationManager animationManager, int level) {
        this(bounds, texture, body, player, animationManager,
                EnemyStats.Factory.createBasicEnemy(level));
    }

    public Enemy(Rectangle bounds, Texture texture, Body body, Player player,
                 AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.texture = texture;
        this.body = body;
        this.player = player;
        this.stats = stats;

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        getAnimationManager().setState(AnimationManager.State.IDLE, "Mushie");
    }

    public void update(float delta) {
        if (markForRemoval) {
            dispose();
            return;
        }

        if (!Player.gameStarted) {
            body.setLinearVelocity(0, 0);
            return;
        }

        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }

        // Update attack state
        if (isAttacking) {
            updateAttack(delta);
        } else {
            updateMovement(delta);
        }

        // Update position
        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        // Update flip based on player position
        isFlipped = body.getPosition().x > player.getBody().getPosition().x;
    }

    private void updateMovement(float delta) {
        if (isPlayerInRadius()) {
            if (isPlayerInAttackRange() && attackCooldown <= 0) {
                startAttack();
            } else {
                moveTowardsPlayer();
                isMoving = true;

                if (getAnimationManager().getState("Mushie") != State.RUNNING) {
                    getAnimationManager().setState(State.RUNNING, "Mushie");
                }
            }
        } else {
            body.setLinearVelocity(0, 0);
            isMoving = false;

            if (getAnimationManager().getState("Mushie") != State.IDLE) {
                getAnimationManager().setState(State.IDLE, "Mushie");
            }
        }
    }

    private void updateAttack(float delta) {
        attackTimer += delta;

        body.setLinearVelocity(0, 0);

        // Check if attack animation is complete
        if (attackTimer >= stats.getAttackSpeed()) {
            // Attack wind-up complete - check if can deal damage based on attack type
            if (!hasDealtDamage && canDamagePlayer()) {
                damagePlayer();
                hasDealtDamage = true;
            }

            endAttack();
        }
    }

    /**
     * Check if enemy can damage player based on attack type
     */
    private boolean canDamagePlayer() {
        switch (stats.getAttackType()) {
            case CONAL:
                return isPlayerInAttackCone();
            case MELEE:
            case DOT:
                return isPlayerInAttackRange();
            case AOE:
                return isPlayerInAoeRange();
            case RANGED:
                return isPlayerInAttackRange();
            case CHARGE:
                return wasPlayerHitDuringCharge();
            default:
                return isPlayerInAttackRange();
        }
    }

    private void startAttack() {
        isAttacking = true;
        attackTimer = 0f;
        hasDealtDamage = false;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.DYING, "Mushie");
        handleAttackStart();
    }

    /**
     * Handle specific logic for different attack types when starting
     */
    private void handleAttackStart() {
        switch (stats.getAttackType()) {
            case CHARGE:
                // Start charging towards player
                startChargeAttack();
                break;
            case RANGED:
                // Create projectile
                createProjectile();
                break;
            // Add other attack type specific logic here
        }
    }

    private void startChargeAttack() {
        // Implement charge attack logic
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        Vector2 direction = new Vector2(playerPosition.x - enemyPosition.x,
                playerPosition.y - enemyPosition.y).nor();

        // Set charge velocity
        body.setLinearVelocity(direction.scl(stats.getChargeSpeed()));
    }

    private void createProjectile() {
        // Implement projectile creation
        // This would create a projectile entity that moves toward the player
        System.out.println("Projectile created!");
    }

    private void endAttack() {
        isAttacking = false;
        attackTimer = 0f;
        attackCooldown = stats.getAttackCooldown();
        hasDealtDamage = false;

        // Return to idle/running state
        getAnimationManager().setState(State.IDLE, "Mushie");

        // Handle attack type specific cleanup
        handleAttackEnd();

        System.out.println(stats.getEnemyName() + " attack ended!");
    }

    /**
     * Handle specific cleanup for different attack types
     */
    private void handleAttackEnd() {
        switch (stats.getAttackType()) {
            case CHARGE:
                // Stop charging
                body.setLinearVelocity(0, 0);
                break;
            // Add other attack type specific cleanup here
        }
    }

    /**
     * Check if player is within attack range
     */
    private boolean isPlayerInAttackRange() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        float distance = playerPosition.dst(enemyPosition);
        return distance <= stats.getAttackRange();
    }

    /**
     * Check if player is within the attack cone (for CONAL attacks)
     */
    private boolean isPlayerInAttackCone() {
        if (!isPlayerInAttackRange()) {
            return false;
        }

        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        // Direction from enemy to player
        Vector2 toPlayer = new Vector2(
                playerPosition.x - enemyPosition.x,
                playerPosition.y - enemyPosition.y
        ).nor();

        // Enemy's facing direction (based on flip)
        Vector2 facingDirection = new Vector2(isFlipped ? -1 : 1, 0);

        // Calculate angle between facing direction and direction to player
        float dotProduct = toPlayer.dot(facingDirection);
        float angleToPlayer = (float) Math.toDegrees(Math.acos(dotProduct));

        // Check if player is within the cone angle
        return angleToPlayer <= stats.getAttackConeAngle();
    }

    /**
     * Check if player is within AOE range (for AOE attacks)
     */
    private boolean isPlayerInAoeRange() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        float distance = playerPosition.dst(enemyPosition);
        return distance <= stats.getAoeRadius();
    }

    /**
     * Check if player was hit during charge attack
     */
    private boolean wasPlayerHitDuringCharge() {
        // Simple implementation - check if player is very close after charge
        return isPlayerInAttackRange();
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            // Render enemy sprite
            TextureRegion currentFrame = new TextureRegion(getAnimationManager().getMushieCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);

            // Render health bar
            renderHealthBar(batch);

            // Render attack indicators based on attack type
            renderAttackIndicator(batch);
        }
    }

    /**
     * Render attack indicator based on attack type
     */
    private void renderAttackIndicator(SpriteBatch batch) {
        if (!isAttacking || attackTimer < 0.1f) {
            return; // Don't show indicator at the very start of attack
        }

        // This is a simple implementation - you can expand this with proper graphics
        batch.setColor(1f, 0f, 0f, 0.3f); // Red with transparency

        Vector2 enemyPos = new Vector2(body.getPosition().x, body.getPosition().y);

        switch (stats.getAttackType()) {
            case CONAL:
                // Draw cone shape (simplified as triangle)
                // You would need to implement proper cone rendering here
                break;
            case AOE:
                // Draw circle for AOE
                // You would need to implement proper circle rendering here
                break;
        }

        batch.setColor(1f, 1f, 1f, 1f); // Reset color
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
     * Deal damage to player
     */
    public void damagePlayer() {
        player.getStats().takeDamage(stats.getDamage());
        System.out.println(stats.getEnemyName() + " hit player for " + stats.getDamage() + " damage!");
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

    public boolean isAttacking() {
        return isAttacking;
    }

    public AttackType getAttackType() {
        return stats.getAttackType();
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