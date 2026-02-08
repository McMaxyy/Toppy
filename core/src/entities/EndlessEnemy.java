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
import ui.ScreenShake;

public class EndlessEnemy {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float detectionRadius = 1000f;
    private final float speed = 70f;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private boolean isStunned = false;
    private boolean isKnockedBack = false;
    private float knockbackTimer = 0f;
    private Vector2 knockbackVelocity = new Vector2();
    private static final float KNOCKBACK_DURATION = 0.15f;
    private static final float KNOCKBACK_FORCE = 150f;

    private EnemyType enemyType;

    private float animationTime = 0f;
    private State currentState = State.IDLE;

    private EnemyStats stats;
    private Texture healthBarTexture;

    // Attack system
    private float attackCooldown = 0f;
    private boolean isAttacking = false;
    private float attackTimer = 0f;

    private Projectile projectile;
    private Texture projectileTexture;

    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;

    public EndlessEnemy(Rectangle bounds, Body body, Player player,
                        AnimationManager animationManager, EnemyStats stats, EnemyType enemyType) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;
        this.enemyType = enemyType;

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

        boolean isRanged = (enemyType == EnemyType.MUSHIE ||
                enemyType == EnemyType.SKELETON_MAGE ||
                enemyType == EnemyType.GHOST);

        if (isRanged) {
            try {
                if (enemyType == EnemyType.MUSHIE) {
                    this.projectileTexture = Storage.assetManager.get("icons/effects/PoisonBall.png", Texture.class);
                } else if (enemyType == EnemyType.SKELETON_MAGE) {
                    this.projectileTexture = Storage.assetManager.get("icons/effects/Fireball.png", Texture.class);
                }
            } catch (Exception e) {
                this.projectileTexture = null;
            }

            Color projectileColor;
            if (enemyType == EnemyType.MUSHIE) {
                projectileColor = new Color(0.5f, 0.8f, 0.3f, 1f);
            } else if (enemyType == EnemyType.SKELETON_MAGE) {
                projectileColor = new Color(1f, 0.5f, 0.1f, 1f);
            } else if (enemyType == EnemyType.GHOST) {
                projectileColor = new Color(0.5f, 0.2f, 0.8f, 1f);
            } else {
                projectileColor = Color.WHITE;
            }

            this.projectile = new Projectile(
                    body.getWorld(),
                    new Vector2(body.getPosition()),
                    new Vector2(1, 0),
                    stats.getProjectileSpeed(),
                    stats.getAttackRange(),
                    stats.getDamage(),
                    projectileColor,
                    this,
                    projectileTexture
            );
            this.projectile.setActive(false);
        }

        this.currentState = State.IDLE;
        this.animationTime = 0f;
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

        updateProjectile(delta);

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        if (isAttacking) {
            updateAttack(delta);
        } else if (isPlayerInRadius()) {
            float distanceToPlayer = getDistanceToPlayer();

            if (attackCooldown <= 0 && distanceToPlayer <= stats.getAttackRange()) {
                startAttack();
            } else if (distanceToPlayer > stats.getAttackRange() * 0.8f) {
                moveTowardsPlayer();
                isMoving = true;

                if (currentState != State.RUNNING) {
                    setState(State.RUNNING);
                }
            } else {
                body.setLinearVelocity(0, 0);
                isMoving = false;

                if (currentState != State.IDLE) {
                    setState(State.IDLE);
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

    private void startAttack() {
        isAttacking = true;
        attackTimer = 0f;
        setState(State.ATTACKING);
    }

    private void updateAttack(float delta) {
        attackTimer += delta;
        body.setLinearVelocity(0, 0);

        if (attackTimer >= stats.getAttackSpeed()) {
            performAttack();
            endAttack();
        }
    }

    private void performAttack() {
        switch (stats.getAttackType()) {
            case RANGED:
                fireProjectile();
                break;
            case MELEE:
                meleeDamagePlayer();
                break;
            case CONAL:
                conalDamagePlayer();
                break;
            case AOE:
                aoeDamagePlayer();
                break;
            default:
                break;
        }
    }

    private void fireProjectile() {
        if (projectile == null) return;

        Vector2 enemyPos = new Vector2(body.getPosition().x, body.getPosition().y);
        Vector2 playerPos = player.getPosition();
        Vector2 direction = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();

        projectile.reset(enemyPos, direction);
    }

    private void meleeDamagePlayer() {
        float dist = getDistanceToPlayer();
        if (dist <= stats.getAttackRange()) {
            damagePlayer();
        }
    }

    private void conalDamagePlayer() {
        Vector2 enemyPos = new Vector2(body.getPosition().x, body.getPosition().y);
        Vector2 playerPos = player.getPosition();
        Vector2 toPlayer = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y);
        float dist = toPlayer.len();

        if (dist <= stats.getAttackRange()) {
            Vector2 facing = new Vector2(isFlipped ? -1 : 1, 0);
            float dot = toPlayer.nor().dot(facing);
            float angleThreshold = (float) Math.cos(Math.toRadians(stats.getAttackConeAngle() / 2f));

            if (dot > angleThreshold) {
                damagePlayer();
            }
        }
    }

    private void aoeDamagePlayer() {
        float dist = getDistanceToPlayer();
        if (dist <= stats.getAoeRadius() / 3) {
            damagePlayer();
        }
    }

    public void damagePlayer() {
        if (!player.isInvulnerable()) {
            player.getStats().takeDamage(stats.getDamage());
            player.onTakeDamage();
        }
    }

    private void endAttack() {
        isAttacking = false;
        attackCooldown = stats.getAttackCooldown();
        setState(State.IDLE);
    }

    private void updateProjectile(float delta) {
        if (projectile == null || !projectile.isActive()) return;

        projectile.update(delta);

        if (!projectile.isMarkedForRemoval() && projectile.getPosition() != null) {
            float dist = projectile.getPosition().dst(player.getPosition());
            if (dist < 10f) {
                if (!player.isInvulnerable()) {
                    player.getStats().takeDamage(projectile.getDamage());
                    player.onTakeDamage();
                }
                projectile.setActive(false);
            }
        }

        if (projectile.isMarkedForRemoval()) {
            projectile.setActive(false);
        }
    }

    public void render(SpriteBatch batch) {
        if (markForRemoval) return;

        if (projectile != null && projectile.isActive()) {
            projectile.render(batch);
        }

        TextureRegion currentFrame = getCurrentFrame();
        if (currentFrame != null) {
            TextureRegion frame = new TextureRegion(currentFrame);
            if (isFlipped && !frame.isFlipX()) {
                frame.flip(true, false);
            } else if (!isFlipped && frame.isFlipX()) {
                frame.flip(true, false);
            }

            if (isJustHit) {
                batch.setColor(1f, 0.5f, 0.5f, 1f);
            }

            batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        renderHealthBar(batch);
    }

    private void renderHealthBar(SpriteBatch batch) {
        float barWidth = bounds.width;
        float barHeight = 3f;
        float barX = bounds.x;
        float barY = bounds.y + bounds.height + 2f;

        batch.setColor(0.2f, 0.2f, 0.2f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        float healthPercent = stats.getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        batch.setColor(0.2f, 0.8f, 0.2f, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private float getDistanceToPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        return playerPosition.dst(enemyPosition);
    }

    private boolean isPlayerInRadius() {
        return getDistanceToPlayer() <= detectionRadius;
    }

    private void moveTowardsPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        Vector2 direction = new Vector2(playerPosition.x - enemyPosition.x,
                playerPosition.y - enemyPosition.y).nor();

        body.setLinearVelocity(direction.scl(speed));
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
            ScreenShake.rumble(3f, 0.3f);
        }

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
        if (projectile != null) {
            projectile = null;
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

    public Projectile getProjectile() {
        return projectile;
    }

    public EnemyType getEnemyType() {
        return enemyType;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public void setStunned(boolean stunned) {
        this.isStunned = stunned;
        if (stunned && body != null) {
            body.setLinearVelocity(0, 0);
        }
    }

    public boolean isStunned() {
        return isStunned;
    }
}