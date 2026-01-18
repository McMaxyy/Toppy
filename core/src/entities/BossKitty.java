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
    private boolean hasDealtDamage = false;

    // Boss special abilities
    private float specialAbilityCooldown = 0f;
    private float specialAbilityTimer = 0f;
    private boolean isUsingSpecialAbility = false;
    private final float SPECIAL_ABILITY_COOLDOWN = 10f; // Every 10 seconds

    // Default constructor with level
    public BossKitty(Rectangle bounds, Body body, Player player,
                     AnimationManager animationManager, int level) {
        this(bounds, body, player, animationManager,
                EnemyStats.Factory.createBoss(level));
    }

    // Constructor with custom stats
    public BossKitty(Rectangle bounds, Body body, Player player,
                     AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

        // Initialize with idle state
        getAnimationManager().setState(State.IDLE, "BossKitty");
    }

    public void update(float delta) {
        if (markForRemoval) {
            return;
        }

        // Update special ability cooldown
        if (specialAbilityCooldown > 0) {
            specialAbilityCooldown -= delta;
        }

        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        // Check for special ability usage
        if (!isAttacking && !isUsingSpecialAbility &&
                specialAbilityCooldown <= 0 && stats.getCurrentHealth() < stats.getMaxHealth() * 0.5f) {
            // Use special ability when below 50% health
            startSpecialAbility();
        }

        // Update attack state
        if (isAttacking) {
            updateAttack(delta);
        } else if (isUsingSpecialAbility) {
            updateSpecialAbility(delta);
        } else {
            updateMovement(delta);
        }
    }

    private void updateMovement(float delta) {
        if (isPlayerInRadius()) {
            // Check if player is in attack range
            if (isPlayerInAttackRange() && attackCooldown <= 0) {
                startAttack();
            } else {
                // Move towards player
                moveTowardsPlayer();
                isMoving = true;

                if (getAnimationManager().getState("BossKitty") != State.RUNNING) {
                    getAnimationManager().setState(State.RUNNING, "BossKitty");
                }
            }
        } else {
            // Out of detection radius - idle
            body.setLinearVelocity(0, 0);
            isMoving = false;

            if (getAnimationManager().getState("BossKitty") != State.IDLE) {
                getAnimationManager().setState(State.IDLE, "BossKitty");
            }
        }
    }

    private void updateAttack(float delta) {
        attackTimer += delta;

        // Stop movement during attack
        body.setLinearVelocity(0, 0);

        // Check if attack animation is complete
        if (attackTimer >= stats.getAttackSpeed()) {
            // Attack wind-up complete - check if can deal damage
            if (!hasDealtDamage && canDamagePlayer()) {
                damagePlayer();
                hasDealtDamage = true;
            }

            // End attack
            endAttack();
        }
    }

    /**
     * Check if boss can damage player based on attack type
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

        // Play attack animation
        getAnimationManager().setState(State.DYING, "BossKitty");

        // Handle different attack types
        handleAttackStart();

        System.out.println("Boss started " + stats.getAttackType() + " attack!");
    }

    /**
     * Handle specific logic for different attack types when starting
     */
    private void handleAttackStart() {
        switch (stats.getAttackType()) {
            case CHARGE:
                startChargeAttack();
                break;
            case RANGED:
                createBossProjectile();
                break;
            case AOE:
                createAoeEffect();
                break;
        }
    }

    private void startChargeAttack() {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        Vector2 direction = new Vector2(playerPosition.x - bossPosition.x,
                playerPosition.y - bossPosition.y).nor();

        body.setLinearVelocity(direction.scl(stats.getChargeSpeed()));
    }

    private void createBossProjectile() {
        System.out.println("Boss created projectile!");
    }

    private void createAoeEffect() {
        System.out.println("Boss created AOE effect!");
    }

    private void endAttack() {
        isAttacking = false;
        attackTimer = 0f;
        attackCooldown = stats.getAttackCooldown();
        hasDealtDamage = false;

        // Return to running state (boss is always aggressive)
        getAnimationManager().setState(State.RUNNING, "BossKitty");

        // Handle attack type specific cleanup
        handleAttackEnd();

        System.out.println("Boss attack ended!");
    }

    /**
     * Handle specific cleanup for different attack types
     */
    private void handleAttackEnd() {
        switch (stats.getAttackType()) {
            case CHARGE:
                body.setLinearVelocity(0, 0);
                break;
        }
    }

    /**
     * Special ability - boss specific attack
     */
    private void startSpecialAbility() {
        isUsingSpecialAbility = true;
        specialAbilityTimer = 0f;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.DYING, "BossKitty");
    }

    private void updateSpecialAbility(float delta) {
        specialAbilityTimer += delta;

        if (specialAbilityTimer >= 2.0f) {
            endSpecialAbility();
        } else if (specialAbilityTimer >= 1.5f) {
            if (!hasDealtDamage) {
                player.getStats().takeDamage(stats.getDamage() * 2);
                hasDealtDamage = true;
                System.out.println("Boss special ability hit player for " + (stats.getDamage() * 2) + " damage!");
            }
        }
    }

    private void endSpecialAbility() {
        isUsingSpecialAbility = false;
        specialAbilityTimer = 0f;
        specialAbilityCooldown = SPECIAL_ABILITY_COOLDOWN;
        hasDealtDamage = false;

        getAnimationManager().setState(State.RUNNING, "BossKitty");
    }

    /**
     * Check if player is within attack range
     */
    private boolean isPlayerInAttackRange() {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        float distance = playerPosition.dst(bossPosition);
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
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        Vector2 toPlayer = new Vector2(
                playerPosition.x - bossPosition.x,
                playerPosition.y - bossPosition.y
        ).nor();

        Vector2 facingDirection = new Vector2(isFlipped ? -1 : 1, 0);

        float dotProduct = toPlayer.dot(facingDirection);
        float angleToPlayer = (float) Math.toDegrees(Math.acos(dotProduct));

        return angleToPlayer <= stats.getAttackConeAngle();
    }

    /**
     * Check if player is within AOE range (for AOE attacks)
     */
    private boolean isPlayerInAoeRange() {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        float distance = playerPosition.dst(bossPosition);
        return distance <= stats.getAoeRadius();
    }

    /**
     * Check if player was hit during charge attack
     */
    private boolean wasPlayerHitDuringCharge() {
        return isPlayerInAttackRange();
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            // Render boss sprite
            TextureRegion currentFrame = new TextureRegion(getAnimationManager().getBossKittyCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);

            // Render boss health bar (larger and always visible)
            renderBossHealthBar(batch);

            // Render special ability indicator
            renderSpecialAbilityIndicator(batch);
        }
    }

    /**
     * Render special ability indicator
     */
    private void renderSpecialAbilityIndicator(SpriteBatch batch) {
        if (isUsingSpecialAbility) {
            // Draw a glow effect around the boss
            float glowSize = bounds.width * 1.5f;
            float glowX = bounds.x - (glowSize - bounds.width) / 2f;
            float glowY = bounds.y - (glowSize - bounds.height) / 2f;

            // Red glow for special ability
            batch.setColor(1f, 0f, 0f, 0.5f);
            // You would need a circle texture here
            // batch.draw(someGlowTexture, glowX, glowY, glowSize, glowSize);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * Render larger health bar for boss
     */
    private void renderBossHealthBar(SpriteBatch batch) {
        float barWidth = bounds.width * 1.5f;
        float barHeight = 5f;
        float barX = bounds.x - (barWidth - bounds.width) / 2f;
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

        // Special ability cooldown indicator
        if (specialAbilityCooldown > 0) {
            float cooldownPercent = specialAbilityCooldown / SPECIAL_ABILITY_COOLDOWN;
            float cooldownWidth = barWidth * (1f - cooldownPercent);
            batch.setColor(0.5f, 0.5f, 1f, 0.7f);
            batch.draw(healthBarTexture, barX, barY, cooldownWidth, barHeight * 0.3f);
        }

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
        System.out.println("Boss hit player for " + stats.getDamage() + " damage!");
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {

    }

    public void removeEnemies() {
        if (body != null) {
            body.getWorld().destroyBody(body);
            body = null;
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

    public boolean isAttacking() {
        return isAttacking;
    }

    public boolean isUsingSpecialAbility() {
        return isUsingSpecialAbility;
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