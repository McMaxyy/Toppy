package entities;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import config.Storage;
import managers.AnimationManager;
import managers.AnimationManager.State;
import managers.Dungeon;
import managers.SoundManager;

public class DungeonEnemy {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final Dungeon dungeon;
    private final float detectionRadius = 100f;
    private boolean hasDetected = false;
    private final float speed = 70f;
    private boolean isStunned = false;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private List<Projectile> projectiles = new ArrayList<>();

    private EnemyType enemyType;

    private float animationTime = 0f;
    private State currentState = State.IDLE;

    private List<Vector2> currentPath;
    private int currentPathIndex = 0;
    private float pathUpdateTimer = 0f;
    private final float PATH_UPDATE_INTERVAL = 0.3f;

    private Vector2 lastPosition = new Vector2();
    private float stuckTimer = 0f;
    private final float STUCK_THRESHOLD = 0.5f;
    private final float STUCK_DISTANCE = 0.3f;
    private int pathfindingAttempts = 0;
    private final int MAX_PATHFINDING_ATTEMPTS = 5;

    private final Vector2[] velocityHistory = new Vector2[5];
    private int velocityHistoryIndex = 0;
    private Vector2 averageVelocity = new Vector2();

    private EnemyStats stats;
    private Texture healthBarTexture;
    private Texture whitePixelTexture;

    private float attackCooldown = 0f;
    private boolean isAttacking = false;
    private boolean hasDealtDamage = false;

    private float healthRegenTimer = 0f;
    private final float HEALTH_REGEN_INTERVAL = 4f;
    private final float COMBAT_TIMER = 3f;
    private float combatTimer = 0f;
    private boolean wasInCombat = false;

    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.2f;

    // Projectile textures
    private static Texture fireballTexture;
    private static Texture poisonBallTexture;
    private static final Color MUSHIE_PROJECTILE_COLOR = new Color(0.2f, 0.8f, 0.2f, 1f);
    private static final Color MAGE_PROJECTILE_COLOR = new Color(1.0f, 0.5f, 0.0f, 1f);

    public DungeonEnemy(Rectangle bounds, Body body, Player player,
                        AnimationManager animationManager, Dungeon dungeon, int level) {
        this(bounds, body, player, animationManager, dungeon,
                EnemyStats.Factory.createSkeletonEnemy(level), EnemyType.SKELETON);
    }

    public DungeonEnemy(Rectangle bounds, Body body, Player player,
                        AnimationManager animationManager, Dungeon dungeon, EnemyStats stats) {
        this(bounds, body, player, animationManager, dungeon, stats,
                determineEnemyType(stats.getEnemyName()));
    }

    public DungeonEnemy(Rectangle bounds, Body body, Player player,
                        AnimationManager animationManager, Dungeon dungeon,
                        EnemyStats stats, EnemyType enemyType) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.dungeon = dungeon;
        this.stats = stats;
        this.enemyType = enemyType;

        this.lastPosition = new Vector2(body.getPosition());
        this.healthBarTexture = Storage.assetManager.get("tiles/hpBar.png", Texture.class);
        this.whitePixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);

        // Load projectile textures
        loadProjectileTextures();

        for (int i = 0; i < velocityHistory.length; i++) {
            velocityHistory[i] = new Vector2();
        }

        this.currentState = State.IDLE;
        this.animationTime = 0f;
    }

    private static void loadProjectileTextures() {
        if (fireballTexture == null) {
            try {
                fireballTexture = Storage.assetManager.get("icons/effects/Fireball.png", Texture.class);
            } catch (Exception e) {
                fireballTexture = null;
            }
        }
        if (poisonBallTexture == null) {
            try {
                poisonBallTexture = Storage.assetManager.get("icons/effects/PoisonBall.png", Texture.class);
            } catch (Exception e) {
                poisonBallTexture = null;
            }
        }
    }

    private static EnemyType determineEnemyType(String name) {
        if (name == null) return EnemyType.SKELETON;
        switch (name.toLowerCase()) {
            case "mushie":
                return EnemyType.MUSHIE;
            case "wolfie":
                return EnemyType.WOLFIE;
            case "skeleton rogue":
            case "skeletonrogue":
                return EnemyType.SKELETON_ROGUE;
            case "skeleton mage":
            case "skeletonmage":
                return EnemyType.SKELETON_MAGE;
            case "boss kitty":
            case "bosskitty":
                return EnemyType.BOSS_KITTY;
            default:
                return EnemyType.SKELETON;
        }
    }

    private void setState(State newState) {
        if (currentState != newState) {
            currentState = newState;
            animationTime = 0f;
        }
    }

    private TextureRegion getCurrentFrame() {
        Animation<TextureRegion> animation = animationManager.getAnimationForState(enemyType, currentState);
        if (animation != null) {
            boolean loop = (currentState == State.IDLE || currentState == State.RUNNING);
            return animation.getKeyFrame(animationTime, loop);
        }
        return null;
    }

    private boolean isCurrentAnimationFinished() {
        Animation<TextureRegion> animation = animationManager.getAnimationForState(enemyType, currentState);
        if (animation != null) {
            return animation.isAnimationFinished(animationTime);
        }
        return true;
    }

    public void update(float delta) {
        if (markForRemoval) {
            return;
        }

        if (isStunned) {
            body.setLinearVelocity(0, 0);
            if (currentState != State.IDLE) {
                setState(State.IDLE);
            }
            animationTime += delta;
            bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                    body.getPosition().y - bounds.height / 2f);
            return;
        }

        if (!Player.gameStarted) {
            body.setLinearVelocity(0, 0);
            return;
        }

        animationTime += delta;

        if (isJustHit) {
            hitFlashTimer -= delta;
            if (hitFlashTimer <= 0) {
                isJustHit = false;
            }
        }

        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }

        updateHealthRegen(delta);

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        if (isAttacking) {
            updateAttack(delta);
        } else {
            updateMovement(delta);
        }

        updateProjectiles(delta);

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;
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
                    int healAmount = Math.max(1, (int)(stats.getMaxHealth() * 0.1f));
                    stats.heal(healAmount);
                }
            }
        }
    }

    private void updateStuckDetection(float delta) {
        Vector2 currentPos = body.getPosition();
        Vector2 velocity = body.getLinearVelocity();

        velocityHistory[velocityHistoryIndex].set(velocity);
        velocityHistoryIndex = (velocityHistoryIndex + 1) % velocityHistory.length;

        averageVelocity.set(0, 0);
        for (Vector2 v : velocityHistory) {
            averageVelocity.add(v);
        }
        averageVelocity.scl(1f / velocityHistory.length);

        float distanceMoved = currentPos.dst(lastPosition);
        boolean tryingToMove = averageVelocity.len() > 15f;

        if (tryingToMove && distanceMoved < STUCK_DISTANCE) {
            stuckTimer += delta;
        } else {
            stuckTimer = Math.max(0, stuckTimer - delta * 2f);
            pathfindingAttempts = 0;
        }

        lastPosition.set(currentPos);
    }

    private void updateMovement(float delta) {
        updateStuckDetection(delta);
        pathUpdateTimer += delta;

        if (isPlayerInRadius() || hasDetected) {
            hasDetected = true;

            // For ranged enemies (mage), check line of sight before attacking
            if (stats.getAttackType() == AttackType.RANGED) {
                if (isPlayerInAttackRange() && attackCooldown <= 0 && hasLineOfSightToPlayer()) {
                    startAttack();
                    return;
                }
            } else {
                if (isPlayerInAttackRange() && attackCooldown <= 0) {
                    startAttack();
                    return;
                }
            }

            boolean forceRecalc = pathUpdateTimer >= PATH_UPDATE_INTERVAL ||
                    currentPath == null ||
                    currentPath.isEmpty() ||
                    stuckTimer >= STUCK_THRESHOLD ||
                    isFarFromPath();

            if (forceRecalc) {
                if (stuckTimer >= STUCK_THRESHOLD) {
                    pathfindingAttempts++;
                    if (pathfindingAttempts >= MAX_PATHFINDING_ATTEMPTS) {
                        attemptDirectMovement();
                        pathUpdateTimer = 0f;
                        stuckTimer = 0f;
                        pathfindingAttempts = 0;
                        return;
                    }
                }

                calculatePathToPlayer();
                pathUpdateTimer = 0f;
                if (stuckTimer >= STUCK_THRESHOLD) {
                    stuckTimer = 0f;
                }
            }

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
            if (currentState != State.IDLE) {
                setState(State.IDLE);
                isMoving = false;
            }
        }

        if (isMoving && currentState != State.RUNNING) {
            setState(State.RUNNING);
        }
    }

    private boolean hasLineOfSightToPlayer() {
        Vector2 enemyPos = body.getPosition();
        Vector2 playerPos = player.getPosition();

        // Use Bresenham's line algorithm to check tiles between enemy and player
        int x0 = (int) (enemyPos.x / dungeon.getTileSize());
        int y0 = (int) (enemyPos.y / dungeon.getTileSize());
        int x1 = (int) (playerPos.x / dungeon.getTileSize());
        int y1 = (int) (playerPos.y / dungeon.getTileSize());

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Check if current tile is a wall
            if (!dungeon.isWalkableWorld(x0 * dungeon.getTileSize() + dungeon.getTileSize() / 2f,
                    y0 * dungeon.getTileSize() + dungeon.getTileSize() / 2f)) {
                return false;
            }

            if (x0 == x1 && y0 == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }

        return true;
    }

    private boolean isFarFromPath() {
        if (currentPath == null || currentPath.isEmpty() || currentPathIndex >= currentPath.size()) {
            return true;
        }
        Vector2 currentPos = body.getPosition();
        Vector2 targetNode = currentPath.get(currentPathIndex);
        return currentPos.dst(targetNode) > 50f;
    }

    private void updateAttack(float delta) {
        body.setLinearVelocity(0, 0);

        boolean animationFinished = isCurrentAnimationFinished();

        // For ranged attacks, fire projectile when animation finishes
        if (animationFinished && !hasDealtDamage) {
            executeAttack();
            hasDealtDamage = true;
        }

        if (animationFinished) {
            endAttack();
        }
    }

    private void executeAttack() {
        switch (stats.getAttackType()) {
            case RANGED:
                createProjectile();
                break;
            case CONAL:
                if (isPlayerInAttackCone()) {
                    damagePlayer();
                }
                break;
            case MELEE:
            case DOT:
                if (isPlayerInAttackRange()) {
                    damagePlayer();
                }
                break;
            case AOE:
                if (isPlayerInAoeRange()) {
                    damagePlayer();
                }
                break;
            case CHARGE:
                if (wasPlayerHitDuringCharge()) {
                    damagePlayer();
                }
                break;
            default:
                if (isPlayerInAttackRange()) {
                    damagePlayer();
                }
                break;
        }
    }

    private void createProjectile() {
        Vector2 startPos = new Vector2(body.getPosition());
        Vector2 playerPos = player.getPosition();
        Vector2 direction = new Vector2(playerPos.x - startPos.x, playerPos.y - startPos.y).nor();

        Texture projectileTexture = fireballTexture;
        Color projectileColor = MAGE_PROJECTILE_COLOR;

        Projectile projectile = new Projectile(
                body.getWorld(),
                startPos,
                direction,
                stats.getProjectileSpeed(),
                stats.getAttackRange() * 2f,
                stats.getDamage(),
                projectileColor,
                this,
                projectileTexture
        );
        projectiles.add(projectile);
    }

    private void updateProjectiles(float delta) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update(delta);

            if (!projectile.isMarkedForRemoval() && projectile.getPosition() != null) {
                float dist = projectile.getPosition().dst(player.getPosition());
                if (dist < 10f) {
                    player.getStats().takeDamage(projectile.getDamage());
                    player.onTakeDamage();
                    projectile.markForRemoval();
                }
            }

            if (projectile.isMarkedForRemoval()) {
                projectile.dispose(body.getWorld());
                projectiles.remove(i);
            }
        }
    }

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
        hasDealtDamage = false;
        body.setLinearVelocity(0, 0);
        setState(State.ATTACKING);
        handleAttackStart();
    }

    private void handleAttackStart() {
        switch (stats.getAttackType()) {
            case CHARGE:
                startChargeAttack();
                break;
        }
    }

    private void startChargeAttack() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        Vector2 direction = new Vector2(playerPosition.x - enemyPosition.x,
                playerPosition.y - enemyPosition.y).nor();
        body.setLinearVelocity(direction.scl(stats.getChargeSpeed()));
    }

    private void endAttack() {
        isAttacking = false;
        attackCooldown = stats.getAttackCooldown();
        hasDealtDamage = false;
        setState(State.IDLE);
        handleAttackEnd();
    }

    private void handleAttackEnd() {
        switch (stats.getAttackType()) {
            case CHARGE:
                body.setLinearVelocity(0, 0);
                break;
        }
    }

    private boolean isPlayerInAttackRange() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(enemyPosition);
        return distance <= stats.getAttackRange();
    }

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
        float angleToPlayer = (float) Math.toDegrees(Math.acos(Math.min(1f, Math.max(-1f, dotProduct))));

        return angleToPlayer <= stats.getAttackConeAngle();
    }

    private boolean isPlayerInAoeRange() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(enemyPosition);
        return distance <= stats.getAoeRadius();
    }

    private boolean wasPlayerHitDuringCharge() {
        return isPlayerInAttackRange();
    }

    private void calculatePathToPlayer() {
        Vector2 enemyPos = body.getPosition();
        Vector2 playerPos = player.getPosition();

        currentPath = dungeon.findPath(enemyPos, playerPos);
        currentPathIndex = 0;

        if (currentPath != null && currentPath.size() > 1) {
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

            TextureRegion currentFrame = getCurrentFrame();
            if (currentFrame != null) {
                TextureRegion frame = new TextureRegion(currentFrame);
                if (isFlipped && !frame.isFlipX()) {
                    frame.flip(true, false);
                } else if (!isFlipped && frame.isFlipX()) {
                    frame.flip(true, false);
                }

                // Apply hit flash effect
                if (isJustHit) {
                    batch.setColor(1f, 0.5f, 0.5f, 1f);
                }
                batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
                if (isJustHit) {
                    batch.setColor(1f, 1f, 1f, 1f);
                }
            }

            renderHealthBar(batch);

            // Render projectiles
            for (Projectile projectile : projectiles) {
                projectile.render(batch);
            }
        }
    }

    private void drawLine(SpriteBatch batch, float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        batch.draw(whitePixelTexture, x1, y1 - thickness / 2, 0, thickness / 2,
                dist, thickness, 1, 1, angle,
                0, 0, whitePixelTexture.getWidth(), whitePixelTexture.getHeight(),
                false, false);
    }

    private void renderHealthBar(SpriteBatch batch) {
        if (stats.getHealthPercentage() >= 1.0f) {
            return;
        }

        float barWidth = bounds.width;
        float barHeight = 3f;
        float barX = bounds.x;
        float barY = bounds.y + bounds.height + 2f;

        batch.setColor(0.8f, 0.1f, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        float healthWidth = barWidth * stats.getHealthPercentage();
        batch.setColor(0.2f, 0.8f, 0.2f, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        if (healthRegenTimer > 0 && !wasInCombat) {
            float regenPercent = healthRegenTimer / HEALTH_REGEN_INTERVAL;
            float regenWidth = barWidth * regenPercent;
            batch.setColor(0.2f, 0.5f, 1f, 0.6f);
            batch.draw(healthBarTexture, barX, barY, regenWidth, barHeight * 0.5f);
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void takeDamage(int damage) {
        if (damage > 0) {
            stats.takeDamage(damage);
            isJustHit = true;
            hitFlashTimer = HIT_FLASH_DURATION;
        }

        combatTimer = COMBAT_TIMER;
        wasInCombat = true;
        healthRegenTimer = 0f;

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    public void damagePlayer() {
        if (!player.isInvulnerable()) {
            player.getStats().takeDamage(stats.getDamage());
            player.onTakeDamage();
        }
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
        for (Projectile projectile : projectiles) {
            if (body != null && body.getWorld() != null) {
                projectile.dispose(body.getWorld());
            }
        }
        projectiles.clear();
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

    public EnemyType getEnemyType() {
        return enemyType;
    }

    private boolean isPlayerInRadius() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(enemyPosition);
        return distance <= detectionRadius;
    }

    public void setStunned(boolean stunned) {
        this.isStunned = stunned;
        if (stunned && body != null) {
            body.setLinearVelocity(0, 0);
        }
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }
    public boolean isStunned() { return isStunned; }
}