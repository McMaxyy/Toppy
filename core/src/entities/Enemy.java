package entities;

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

import java.util.ArrayList;
import java.util.List;

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

    // Enemy type system
    private EnemyType enemyType;

    // Per-instance animation tracking
    private float animationTime = 0f;
    private State currentState = State.IDLE;

    // Stats system
    private EnemyStats stats;
    private Texture healthBarTexture;
    private Texture whitePixelTexture;

    // Attack system
    private float attackCooldown = 0f;
    private boolean isAttacking = false;
    private boolean hasDealtDamage = false;

    // Projectile management (for ranged enemies)
    private List<Projectile> projectiles = new ArrayList<>();
    private static final Color MUSHIE_PROJECTILE_COLOR = new Color(0.2f, 0.8f, 0.2f, 1f);

    // Attack indicator
    private boolean showAttackIndicator = false;
    private Vector2 attackDirection = new Vector2();

    public Enemy(Rectangle bounds, Texture texture, Body body, Player player,
                 AnimationManager animationManager, int level) {
        this(bounds, texture, body, player, animationManager,
                EnemyStats.Factory.createBasicEnemy(level), EnemyType.MUSHIE);
    }

    public Enemy(Rectangle bounds, Texture texture, Body body, Player player,
                 AnimationManager animationManager, EnemyStats stats) {
        this(bounds, texture, body, player, animationManager, stats,
                determineEnemyType(stats.getEnemyName()));
    }

    public Enemy(Rectangle bounds, Texture texture, Body body, Player player,
                 AnimationManager animationManager, EnemyStats stats, EnemyType enemyType) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.texture = texture;
        this.body = body;
        this.player = player;
        this.stats = stats;
        this.enemyType = enemyType;

        this.healthBarTexture = Storage.assetManager.get("tiles/hpBar.png", Texture.class);
        this.whitePixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);

        // Set initial state
        this.currentState = State.IDLE;
        this.animationTime = 0f;
    }

    private static EnemyType determineEnemyType(String name) {
        if (name == null) return EnemyType.MUSHIE;
        switch (name.toLowerCase()) {
            case "wolfie":
                return EnemyType.WOLFIE;
            case "skeleton":
                return EnemyType.SKELETON;
            case "boss kitty":
            case "bosskitty":
                return EnemyType.BOSS_KITTY;
            default:
                return EnemyType.MUSHIE;
        }
    }

    /**
     * Set animation state for this enemy instance
     */
    private void setState(State newState) {
        if (currentState != newState) {
            currentState = newState;
            animationTime = 0f;
        }
    }

    /**
     * Get current animation frame for this enemy instance
     */
    private TextureRegion getCurrentFrame() {
        Animation<TextureRegion> animation = animationManager.getAnimationForState(enemyType, currentState);
        if (animation != null) {
            // Loop for IDLE and RUNNING, don't loop for ATTACKING
            boolean loop = (currentState == State.IDLE || currentState == State.RUNNING);
            return animation.getKeyFrame(animationTime, loop);
        }
        return null;
    }

    /**
     * Check if current animation is finished (for non-looping animations)
     */
    private boolean isCurrentAnimationFinished() {
        Animation<TextureRegion> animation = animationManager.getAnimationForState(enemyType, currentState);
        if (animation != null) {
            return animation.isAnimationFinished(animationTime);
        }
        return true;
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

        // Update per-instance animation time
        animationTime += delta;

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

        // Update projectiles
        updateProjectiles(delta);

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

                if (currentState != State.RUNNING) {
                    setState(State.RUNNING);
                }
            }
        } else {
            body.setLinearVelocity(0, 0);
            isMoving = false;

            if (currentState != State.IDLE) {
                setState(State.IDLE);
            }
        }
    }

    private void updateAttack(float delta) {
        body.setLinearVelocity(0, 0);

        // Show attack indicator during wind-up for melee enemies
        if (stats.getAttackType() == AttackType.MELEE || stats.getAttackType() == AttackType.CONAL) {
            showAttackIndicator = true;
            updateAttackDirection();
        }

        // Check if attack animation is finished
        boolean animationFinished = isCurrentAnimationFinished();

        if (animationFinished && !hasDealtDamage) {
            executeAttack();
            hasDealtDamage = true;
        }

        // End attack after animation completes
        if (animationFinished) {
            endAttack();
        }
    }

    private void executeAttack() {
        switch (stats.getAttackType()) {
            case RANGED:
                createProjectile();
                break;
            case MELEE:
            case CONAL:
                if (canDamagePlayer()) {
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
                if (canDamagePlayer()) {
                    damagePlayer();
                }
                break;
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
                return true;
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

        updateAttackDirection();

        // Set attack animation state - resets animation time
        setState(State.ATTACKING);

        handleAttackStart();
    }

    private void updateAttackDirection() {
        Vector2 playerPos = player.getPosition();
        Vector2 enemyPos = new Vector2(body.getPosition().x, body.getPosition().y);
        attackDirection.set(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();
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

    private void createProjectile() {
        Vector2 startPos = new Vector2(body.getPosition());
        Vector2 playerPos = player.getPosition();
        Vector2 direction = new Vector2(playerPos.x - startPos.x, playerPos.y - startPos.y).nor();

        Projectile projectile = new Projectile(
                body.getWorld(),
                startPos,
                direction,
                stats.getProjectileSpeed(),
                stats.getAttackRange() * 2f,
                stats.getDamage(),
                MUSHIE_PROJECTILE_COLOR,
                this
        );
        projectiles.add(projectile);

        System.out.println(stats.getEnemyName() + " fired a projectile!");
    }

    private void updateProjectiles(float delta) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update(delta);

            if (!projectile.isMarkedForRemoval() && projectile.getPosition() != null) {
                float dist = projectile.getPosition().dst(player.getPosition());
                if (dist < 10f) {
                    player.getStats().takeDamage(projectile.getDamage());
                    System.out.println(stats.getEnemyName() + " projectile hit player for " +
                            projectile.getDamage() + " damage!");
                    projectile.markForRemoval();
                }
            }

            if (projectile.isMarkedForRemoval()) {
                projectile.dispose(body.getWorld());
                projectiles.remove(i);
            }
        }
    }

    private void endAttack() {
        isAttacking = false;
        attackCooldown = stats.getAttackCooldown();
        hasDealtDamage = false;
        showAttackIndicator = false;

        // Return to idle state
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

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            // Render attack indicator BEFORE the enemy sprite
            if (showAttackIndicator && isAttacking) {
                renderAttackIndicator(batch);
            }

            // Render enemy sprite
            TextureRegion currentFrame = getCurrentFrame();
            if (currentFrame != null) {
                TextureRegion frame = new TextureRegion(currentFrame);
                if (isFlipped && !frame.isFlipX()) {
                    frame.flip(true, false);
                } else if (!isFlipped && frame.isFlipX()) {
                    frame.flip(true, false);
                }
                batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
            }

            // Render health bar
            renderHealthBar(batch);

            // Render projectiles
            for (Projectile projectile : projectiles) {
                projectile.render(batch);
            }
        }
    }

    private void renderAttackIndicator(SpriteBatch batch) {
        Vector2 enemyPos = new Vector2(body.getPosition().x, body.getPosition().y);

        switch (stats.getAttackType()) {
            case MELEE:
                renderMeleeIndicator(batch, enemyPos);
                break;
            case CONAL:
                renderConalIndicator(batch, enemyPos);
                break;
            case AOE:
                renderAoeIndicator(batch, enemyPos);
                break;
        }
    }

    private void renderMeleeIndicator(SpriteBatch batch, Vector2 enemyPos) {
        float range = stats.getAttackRange();

        batch.setColor(1f, 1f, 1f, 0.3f);

        int segments = 16;
        float angleStep = 360f / segments;

        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float nextAngle = (i + 1) * angleStep;
            float rad = (float) Math.toRadians(angle);
            float nextRad = (float) Math.toRadians(nextAngle);

            float x1 = enemyPos.x + (float) Math.cos(rad) * range;
            float y1 = enemyPos.y + (float) Math.sin(rad) * range;
            float x2 = enemyPos.x + (float) Math.cos(nextRad) * range;
            float y2 = enemyPos.y + (float) Math.sin(nextRad) * range;

            drawLine(batch, x1, y1, x2, y2, 2f);
        }

        batch.setColor(1f, 1f, 1f, 0.15f);
        batch.draw(whitePixelTexture, enemyPos.x - range, enemyPos.y - range, range * 2, range * 2);

        batch.setColor(1, 1, 1, 1);
    }

    private void renderConalIndicator(SpriteBatch batch, Vector2 enemyPos) {
        float range = stats.getAttackRange();
        float coneAngle = stats.getAttackConeAngle();

        batch.setColor(1f, 1f, 1f, 0.3f);

        float facingAngle = isFlipped ? 180f : 0f;

        int segments = 12;
        float halfCone = coneAngle / 2f;
        float angleStep = coneAngle / segments;

        for (int i = 0; i < segments; i++) {
            float angle1 = facingAngle - halfCone + (i * angleStep);
            float angle2 = facingAngle - halfCone + ((i + 1) * angleStep);

            float rad1 = (float) Math.toRadians(angle1);
            float rad2 = (float) Math.toRadians(angle2);

            float x1 = enemyPos.x + (float) Math.cos(rad1) * range;
            float y1 = enemyPos.y + (float) Math.sin(rad1) * range;
            float x2 = enemyPos.x + (float) Math.cos(rad2) * range;
            float y2 = enemyPos.y + (float) Math.sin(rad2) * range;

            drawLine(batch, enemyPos.x, enemyPos.y, x1, y1, 2f);

            batch.setColor(1f, 1f, 1f, 0.2f);
            drawLine(batch, x1, y1, x2, y2, 2f);
            batch.setColor(1f, 1f, 1f, 0.3f);
        }

        float finalAngle = facingAngle + halfCone;
        float finalRad = (float) Math.toRadians(finalAngle);
        float finalX = enemyPos.x + (float) Math.cos(finalRad) * range;
        float finalY = enemyPos.y + (float) Math.sin(finalRad) * range;
        drawLine(batch, enemyPos.x, enemyPos.y, finalX, finalY, 2f);

        batch.setColor(1, 1, 1, 1);
    }

    private void renderAoeIndicator(SpriteBatch batch, Vector2 enemyPos) {
        float radius = stats.getAoeRadius();

        batch.setColor(1f, 1f, 1f, 0.3f);

        int segments = 24;
        float angleStep = 360f / segments;

        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float nextAngle = (i + 1) * angleStep;
            float rad = (float) Math.toRadians(angle);
            float nextRad = (float) Math.toRadians(nextAngle);

            float x1 = enemyPos.x + (float) Math.cos(rad) * radius;
            float y1 = enemyPos.y + (float) Math.sin(rad) * radius;
            float x2 = enemyPos.x + (float) Math.cos(nextRad) * radius;
            float y2 = enemyPos.y + (float) Math.sin(nextRad) * radius;

            drawLine(batch, x1, y1, x2, y2, 2f);
        }

        batch.setColor(1f, 1f, 1f, 0.15f);
        batch.draw(whitePixelTexture, enemyPos.x - radius, enemyPos.y - radius, radius * 2, radius * 2);

        batch.setColor(1, 1, 1, 1);
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

        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void takeDamage(int damage) {
        stats.takeDamage(damage);

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    public void damagePlayer() {
        player.getStats().takeDamage(stats.getDamage());
        System.out.println(stats.getEnemyName() + " hit player for " + stats.getDamage() + " damage!");
    }

    public void dispose() {
        for (Projectile projectile : projectiles) {
            if (body != null && body.getWorld() != null) {
                projectile.dispose(body.getWorld());
            }
        }
        projectiles.clear();

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

    public EnemyType getEnemyType() {
        return enemyType;
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

    public List<Projectile> getProjectiles() {
        return projectiles;
    }
}