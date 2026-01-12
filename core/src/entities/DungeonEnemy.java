package entities;

import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import config.Storage;
import managers.AnimationManager;
import managers.AnimationManager.State;
import managers.Dungeon;

public class DungeonEnemy {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final Dungeon dungeon;
    private final float detectionRadius = 200f; // Increased from 150f
    private final float speed = 70f; // Increased from 40f for faster movement
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;

    // Pathfinding with improvements
    private List<Vector2> currentPath;
    private int currentPathIndex = 0;
    private float pathUpdateTimer = 0f;
    private final float PATH_UPDATE_INTERVAL = 0.3f; // More frequent updates

    // Improved stuck detection
    private Vector2 lastPosition = new Vector2();
    private float stuckTimer = 0f;
    private final float STUCK_THRESHOLD = 0.5f; // Reduced threshold
    private final float STUCK_DISTANCE = 0.3f; // More sensitive
    private int pathfindingAttempts = 0;
    private final int MAX_PATHFINDING_ATTEMPTS = 5; // Fewer attempts before giving up

    // Velocity smoothing for better stuck detection
    private final Vector2[] velocityHistory = new Vector2[5];
    private int velocityHistoryIndex = 0;
    private Vector2 averageVelocity = new Vector2();

    // Stats system
    private EnemyStats stats;
    private Texture healthBarTexture;

    // Attack system - using stats
    private float attackCooldown = 0f;
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private boolean hasDealtDamage = false;

    // Health regeneration for tougher enemies
    private float healthRegenTimer = 0f;
    private final float HEALTH_REGEN_INTERVAL = 4f; // Heal every 4 seconds when not in combat
    private final float COMBAT_TIMER = 3f; // Time after losing aggro before starting regen
    private float combatTimer = 0f;
    private boolean wasInCombat = false;

    public DungeonEnemy(Rectangle bounds, Body body, Player player,
                        AnimationManager animationManager, Dungeon dungeon, int level) {
        this(bounds, body, player, animationManager, dungeon,
                EnemyStats.Factory.createDungeonEnemy(level));
    }

    /**
     * Constructor with custom stats
     */
    public DungeonEnemy(Rectangle bounds, Body body, Player player,
                        AnimationManager animationManager, Dungeon dungeon, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.dungeon = dungeon;
        this.stats = stats;

        this.lastPosition = new Vector2(body.getPosition());
        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

        // Initialize velocity history
        for (int i = 0; i < velocityHistory.length; i++) {
            velocityHistory[i] = new Vector2();
        }

        animationManager.setState(AnimationManager.State.IDLE, "Mushie");
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

        // Update health regeneration
        updateHealthRegen(delta);

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        // Update attack state
        if (isAttacking) {
            updateAttack(delta);
        } else {
            updateMovement(delta);
        }

        // Update flip based on player position
        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        // Update stuck detection
        updateStuckDetection(delta);
    }

    private void updateHealthRegen(float delta) {
        boolean isInCombat = isPlayerInRadius();

        if (isInCombat) {
            combatTimer = COMBAT_TIMER;
            wasInCombat = true;
            healthRegenTimer = 0f;
        } else if (wasInCombat) {
            combatTimer -= delta;
            if (combatTimer <= 0) {
                wasInCombat = false;
                healthRegenTimer += delta;
                if (healthRegenTimer >= HEALTH_REGEN_INTERVAL && stats.getCurrentHealth() < stats.getMaxHealth()) {
                    healthRegenTimer = 0f;
                    // Heal 10% of max health
                    int healAmount = Math.max(1, (int)(stats.getMaxHealth() * 0.1f));
                    stats.heal(healAmount);
                }
            }
        }
    }

    private void updateStuckDetection(float delta) {
        Vector2 currentPos = body.getPosition();
        Vector2 velocity = body.getLinearVelocity();

        // Update velocity history
        velocityHistory[velocityHistoryIndex].set(velocity);
        velocityHistoryIndex = (velocityHistoryIndex + 1) % velocityHistory.length;

        // Calculate average velocity
        averageVelocity.set(0, 0);
        for (Vector2 v : velocityHistory) {
            averageVelocity.add(v);
        }
        averageVelocity.scl(1f / velocityHistory.length);

        float distanceMoved = currentPos.dst(lastPosition);
        boolean tryingToMove = averageVelocity.len() > 15f; // Higher threshold for trying to move

        if (tryingToMove && distanceMoved < STUCK_DISTANCE) {
            stuckTimer += delta;
        } else {
            stuckTimer = Math.max(0, stuckTimer - delta * 2f); // Faster recovery
            pathfindingAttempts = 0;
        }

        lastPosition.set(currentPos);
    }

    private void updateMovement(float delta) {
        // Detect if stuck
        updateStuckDetection(delta);

        // Update pathfinding timer
        pathUpdateTimer += delta;

        if (isPlayerInRadius()) {
            // Check if player is in attack range
            if (isPlayerInAttackRange() && attackCooldown <= 0) {
                startAttack();
                return;
            }

            // Recalculate path periodically or if stuck
            boolean forceRecalc = pathUpdateTimer >= PATH_UPDATE_INTERVAL ||
                    currentPath == null ||
                    currentPath.isEmpty() ||
                    stuckTimer >= STUCK_THRESHOLD ||
                    isFarFromPath();

            if (forceRecalc) {
                if (stuckTimer >= STUCK_THRESHOLD) {
                    pathfindingAttempts++;

                    if (pathfindingAttempts >= MAX_PATHFINDING_ATTEMPTS) {
                        // Try direct movement as fallback
                        attemptDirectMovement();
                        pathUpdateTimer = 0f;
                        stuckTimer = 0f;
                        return;
                    }
                }

                calculatePathToPlayer();
                pathUpdateTimer = 0f;

                if (stuckTimer >= STUCK_THRESHOLD) {
                    stuckTimer = 0f;
                }
            }

            // Follow path with improved navigation
            if (currentPath != null && !currentPath.isEmpty()) {
                followPath();
                isMoving = true;
            }
        } else {
            body.setLinearVelocity(0, 0);
            currentPath = null;
            currentPathIndex = 0;
            stuckTimer = 0f;
            pathfindingAttempts = 0;
            if (animationManager.getState("Mushie") != State.IDLE) {
                animationManager.setState(State.IDLE, "Mushie");
                isMoving = false;
            }
        }

        if (isMoving && animationManager.getState("Mushie") != State.RUNNING) {
            animationManager.setState(State.RUNNING, "Mushie");
        }
    }

    private boolean isFarFromPath() {
        if (currentPath == null || currentPath.isEmpty() || currentPathIndex >= currentPath.size()) {
            return true;
        }

        Vector2 currentPos = body.getPosition();
        Vector2 targetNode = currentPath.get(currentPathIndex);

        return currentPos.dst(targetNode) > 50f; // If too far from current path node
    }

    private void updateAttack(float delta) {
        attackTimer += delta;

        // Stop movement during attack
        body.setLinearVelocity(0, 0);

        // Check if attack animation is complete
        if (attackTimer >= stats.getAttackSpeed()) {
            // Attack wind-up complete - check if can deal damage based on attack type
            if (!hasDealtDamage && canDamagePlayer()) {
                damagePlayer();
                hasDealtDamage = true;
            }

            // End attack
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

        // Play attack animation
        animationManager.setState(State.DYING, "Mushie");

        // Handle different attack types
        handleAttackStart();
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
                createProjectile();
                break;
            // Add other attack type specific logic here
        }
    }

    private void startChargeAttack() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        Vector2 direction = new Vector2(playerPosition.x - enemyPosition.x,
                playerPosition.y - enemyPosition.y).nor();

        body.setLinearVelocity(direction.scl(stats.getChargeSpeed()));
    }

    private void createProjectile() {
        // Implement projectile creation
        // This would create a projectile entity that moves toward the player
    }

    private void endAttack() {
        isAttacking = false;
        attackTimer = 0f;
        attackCooldown = stats.getAttackCooldown();
        hasDealtDamage = false;

        // Return to idle state
        animationManager.setState(State.IDLE, "Mushie");

        // Handle attack type specific cleanup
        handleAttackEnd();
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

        Vector2 toPlayer = new Vector2(
                playerPosition.x - enemyPosition.x,
                playerPosition.y - enemyPosition.y
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
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        float distance = playerPosition.dst(enemyPosition);
        return distance <= stats.getAoeRadius();
    }

    /**
     * Check if player was hit during charge attack
     */
    private boolean wasPlayerHitDuringCharge() {
        return isPlayerInAttackRange();
    }

    private void calculatePathToPlayer() {
        Vector2 enemyPos = body.getPosition();
        Vector2 playerPos = player.getPosition();

        currentPath = dungeon.findPath(enemyPos, playerPos);
        currentPathIndex = 0;

        if (currentPath != null && currentPath.size() > 1) {
            // Start from the closest node, not necessarily the second one
            float closestDist = Float.MAX_VALUE;
            int closestIndex = 0;

            for (int i = 0; i < currentPath.size(); i++) {
                float dist = enemyPos.dst(currentPath.get(i));
                if (dist < closestDist) {
                    closestDist = dist;
                    closestIndex = i;
                }
            }

            currentPathIndex = Math.min(closestIndex + 1, currentPath.size() - 1);
        }
    }

    private void followPath() {
        if (currentPath == null || currentPathIndex >= currentPath.size()) {
            body.setLinearVelocity(0, 0);
            return;
        }

        Vector2 targetNode = currentPath.get(currentPathIndex);
        Vector2 currentPos = body.getPosition();

        float distanceToNode = currentPos.dst(targetNode);

        // Use a larger threshold for reaching nodes to prevent getting stuck
        if (distanceToNode < 20f) {
            currentPathIndex++;
            if (currentPathIndex >= currentPath.size()) {
                body.setLinearVelocity(0, 0);
                return;
            }
            targetNode = currentPath.get(currentPathIndex);
            distanceToNode = currentPos.dst(targetNode);
        }

        Vector2 direction = new Vector2(targetNode.x - currentPos.x,
                targetNode.y - currentPos.y).nor();

        // Adjust speed based on distance for smoother movement
        float adjustedSpeed = speed;
        if (distanceToNode < 30f) {
            adjustedSpeed = speed * (distanceToNode / 30f);
            adjustedSpeed = Math.max(adjustedSpeed, speed * 0.5f);
        }

        body.setLinearVelocity(direction.scl(adjustedSpeed));
    }

    private void attemptDirectMovement() {
        Vector2 targetPosition = new Vector2(player.getPosition().x + 10, player.getPosition().y + 10);
        body.setTransform(targetPosition, 1f);
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            // Render enemy sprite
            TextureRegion currentFrame = new TextureRegion(animationManager.getMushieCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);

            // Render health bar
            renderHealthBar(batch);

            // Render attack indicator for visual feedback
            if (isAttacking && attackTimer > stats.getAttackSpeed() * 0.5f) {
                renderAttackIndicator(batch);
            }
        }
    }

    /**
     * Render attack indicator based on attack type
     */
    private void renderAttackIndicator(SpriteBatch batch) {
        // Simple attack indicator - red flash
        batch.setColor(1f, 0.3f, 0.3f, 0.7f);
        TextureRegion currentFrame = new TextureRegion(animationManager.getMushieCurrentFrame());
        currentFrame.flip(isFlipped, false);
        batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Render health bar above enemy
     */
    private void renderHealthBar(SpriteBatch batch) {
        if (stats.getHealthPercentage() >= 1.0f) {
            return;
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

        // Health regeneration indicator (blue overlay when regenerating)
        if (healthRegenTimer > 0 && !wasInCombat) {
            float regenPercent = healthRegenTimer / HEALTH_REGEN_INTERVAL;
            float regenWidth = barWidth * regenPercent;
            batch.setColor(0.2f, 0.5f, 1f, 0.6f);
            batch.draw(healthBarTexture, barX, barY, regenWidth, barHeight * 0.5f);
        }

        // Reset color
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Take damage from player
     */
    public void takeDamage(int damage) {
        stats.takeDamage(damage);

        // Reset combat timer when taking damage
        combatTimer = COMBAT_TIMER;
        wasInCombat = true;
        healthRegenTimer = 0f;

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    /**
     * Deal damage to player
     */
    public void damagePlayer() {
        player.getStats().takeDamage(stats.getDamage());
    }

    public void dispose() {
        if (body != null && markForRemoval) {
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

    public AttackType getAttackType() {
        return stats.getAttackType();
    }

    private boolean isPlayerInRadius() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(enemyPosition);
        return distance <= detectionRadius;
    }
}