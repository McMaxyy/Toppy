package entities;

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

public class Ghost {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float detectionRadius = 300f;
    private final float speed = 85f;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private boolean isStunned = false;

    private EnemyType enemyType = EnemyType.GHOST;

    private float animationTime = 0f;
    private State currentState = State.IDLE;

    private EnemyStats stats;
    private Texture healthBarTexture;

    // Explosion mechanics
    private static float EXPLOSION_RANGE;
    private boolean isExploding = false;
    private float explosionTimer = 0f;
    private boolean hasDealtDamage = false;

    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.2f;

    private GhostBoss ownerBoss;

    public Ghost(Rectangle bounds, Body body, Player player,
                 AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;
        EXPLOSION_RANGE = stats.getAoeRadius();

        this.healthBarTexture = Storage.assetManager.get("tiles/hpBar.png", Texture.class);

        this.currentState = State.IDLE;
        this.animationTime = 0f;
    }

    public void setOwnerBoss(GhostBoss boss) {
        this.ownerBoss = boss;
    }

    public GhostBoss getOwnerBoss() {
        return ownerBoss;
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

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        if (isExploding) {
            updateExplosion(delta);
        } else {
            updateMovement(delta);
        }

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;
    }

    private void updateMovement(float delta) {
        float distanceToPlayer = getDistanceToPlayer();

        if (distanceToPlayer <= 30f) {
            startExplosion();
        } else if (isPlayerInRadius()) {
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
    }

    private void startExplosion() {
        isExploding = true;
        explosionTimer = 0f;
        hasDealtDamage = false;
        body.setLinearVelocity(0, 0);

        setState(State.ATTACKING);
    }

    private void updateExplosion(float delta) {
        explosionTimer += delta;
        body.setLinearVelocity(0, 0);

        boolean animationFinished = isCurrentAnimationFinished();

        if (animationFinished && !hasDealtDamage) {
            if (isPlayerInExplosionRange()) {
                damagePlayer();
            }
            hasDealtDamage = true;
            markForRemoval();
        }
    }

    private boolean isPlayerInExplosionRange() {
        return getDistanceToPlayer() <= EXPLOSION_RANGE;
    }

    private float getDistanceToPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 ghostPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        return playerPosition.dst(ghostPosition);
    }

    private boolean isPlayerInRadius() {
        return getDistanceToPlayer() <= detectionRadius;
    }

    private void moveTowardsPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 ghostPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        Vector2 direction = new Vector2(playerPosition.x - ghostPosition.x,
                playerPosition.y - ghostPosition.y).nor();

        body.setLinearVelocity(direction.scl(speed));
    }

    public void render(SpriteBatch batch) {
        if (markForRemoval) return;

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

            // Add ghostly transparency effect
//            float alpha = isExploding ? Math.max(0.3f, 1f - (explosionTimer / 1.0f)) : 0.85f;
            if (!isJustHit) {
                batch.setColor(1f, 1f, 1f, 1f);
            }

            batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        renderHealthBar(batch);
    }

    private void renderHealthBar(SpriteBatch batch) {
        if (stats.getHealthPercentage() >= 1.0f) {
            return;
        }

        float barWidth = bounds.width;
        float barHeight = 3f;
        float barX = bounds.x;
        float barY = bounds.y + bounds.height + 2f;

        // Background
        batch.setColor(0.8f, 0.1f, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        // Health
        float healthWidth = barWidth * stats.getHealthPercentage();
        batch.setColor(0.6f, 0.2f, 0.8f, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void takeDamage(int damage) {
        if (damage > 0) {
            stats.takeDamage(damage);
            isJustHit = true;
            hitFlashTimer = HIT_FLASH_DURATION;
        }

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    private void damagePlayer() {
        if (!player.isInvulnerable()) {
            player.getStats().takeDamage(stats.getDamage());
            player.onTakeDamage();
        }
    }

    public void damagePlayerDirect() {
        damagePlayer();
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
        // Clean up any resources
    }

    public void markForRemoval() {
        markForRemoval = true;
        // Notify owner boss if this ghost was spawned by one
        if (ownerBoss != null) {
            ownerBoss.onGhostlingDeath(this);
        }
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

    public boolean isExploding() {
        return isExploding;
    }

    public boolean isAttacking() {
        return isExploding;
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