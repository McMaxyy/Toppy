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
import managers.SoundManager;

import java.util.ArrayList;
import java.util.List;

public class Enemy {
    public Rectangle bounds;
    private final Texture texture;
    private Body body;
    private final Player player;
    private final float detectionRadius = 150f;
    private final float speed = 70f;
    private boolean isStunned = false;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private boolean isKnockedBack = false;
    private float knockbackTimer = 0f;
    private Vector2 knockbackVelocity = new Vector2();
    private static final float KNOCKBACK_DURATION = 0.15f;
    private static final float KNOCKBACK_FORCE = 150f;

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

    private List<Projectile> projectiles = new ArrayList<>();
    private static final Color MUSHIE_PROJECTILE_COLOR = new Color(0.2f, 0.8f, 0.2f, 1f);

    // Projectile textures
    private static Texture poisonBallTexture;

    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;
    private Vector2 attackDirection = new Vector2();

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

        // Load projectile texture
        loadProjectileTextures();

        this.currentState = State.IDLE;
        this.animationTime = 0f;
    }

    private static void loadProjectileTextures() {
        if (poisonBallTexture == null) {
            try {
                poisonBallTexture = Storage.assetManager.get("icons/effects/PoisonBall.png", Texture.class);
            } catch (Exception e) {
                poisonBallTexture = null;
            }
        }
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

    private void setState(State newState) {
        if (currentState != newState) {
            currentState = newState;
            animationTime = 0f;
        }
    }

    private TextureRegion getCurrentFrame() {
        Animation<TextureRegion> animation = animationManager.getAnimationForState(enemyType, currentState);
        if (animation != null) {
            // Loop for IDLE and RUNNING, don't loop for ATTACKING
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

        if (isKnockedBack) {
            knockbackTimer -= delta;

            float knockbackProgress = knockbackTimer / KNOCKBACK_DURATION;
            body.setLinearVelocity(
                    knockbackVelocity.x * knockbackProgress,
                    knockbackVelocity.y * knockbackProgress
            );

            if (knockbackTimer <= 0) {
                isKnockedBack = false;
                body.setLinearVelocity(0, 0);
            }

            animationTime += delta;
            bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                    body.getPosition().y - bounds.height / 2f);

            isFlipped = body.getPosition().x > player.getBody().getPosition().x;
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

        if (isAttacking) {
            updateAttack(delta);
        } else {
            updateMovement(delta);
        }

        updateProjectiles(delta);

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

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

        if (stats.getAttackType() == AttackType.MELEE || stats.getAttackType() == AttackType.CONAL) {
            updateAttackDirection();
        }

        boolean animationFinished = isCurrentAnimationFinished();

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

        setState(State.ATTACKING);
    }

    private void updateAttackDirection() {
        Vector2 playerPos = player.getPosition();
        Vector2 enemyPos = new Vector2(body.getPosition().x, body.getPosition().y);
        attackDirection.set(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();
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
                this,
                poisonBallTexture
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

    private void endAttack() {
        isAttacking = false;
        attackCooldown = stats.getAttackCooldown();
        hasDealtDamage = false;

        setState(State.IDLE);
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
                    batch.setColor(1f, 0.5f, 0.5f, 1f); // Red/white tint when hit
                }
                batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
                if (isJustHit) {
                    batch.setColor(1f, 1f, 1f, 1f); // Reset color
                }
            }

            // Render health bar
            renderHealthBar(batch);

            // Render projectiles
            for (Projectile projectile : projectiles) {
                projectile.render(batch);
            }
        }
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

    private void applyKnockback() {
        if (body == null || player == null || player.getPosition() == null) return;

        Vector2 enemyPos = body.getPosition();
        Vector2 playerPos = player.getPosition();

        Vector2 knockbackDir = new Vector2(
                enemyPos.x - playerPos.x,
                enemyPos.y - playerPos.y
        );

        if (knockbackDir.len() > 0) {
            knockbackDir.nor();
            knockbackVelocity.set(knockbackDir.x * KNOCKBACK_FORCE, knockbackDir.y * KNOCKBACK_FORCE);
            isKnockedBack = true;
            knockbackTimer = KNOCKBACK_DURATION;
        }
    }

    public void takeDamage(int damage) {
        if (damage > 0) {
            stats.takeDamage(damage);
            isJustHit = true;
            hitFlashTimer = HIT_FLASH_DURATION;

            applyKnockback();
        }

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    public void damagePlayer() {
        if (!player.isInvulnerable() && !player.isInvisible()) {
            player.getStats().takeDamage(stats.getDamage());
            player.onTakeDamage();
        }
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
        // Dispose projectiles
        for (Projectile projectile : projectiles) {
            if (body != null && body.getWorld() != null) {
                projectile.dispose(body.getWorld());
            }
        }
        projectiles.clear();
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

    public void setStunned(boolean stunned) {
        this.isStunned = stunned;
        if (stunned && body != null) {
            body.setLinearVelocity(0, 0);
        }
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }
    public boolean isStunned() { return isStunned; }
}