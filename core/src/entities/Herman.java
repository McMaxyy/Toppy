package entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.List;

import config.Storage;
import managers.AnimationManager;
import managers.AnimationManager.State;
import managers.CollisionFilter;

public class Herman {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float activationRadius = 200f;
    private final float arenaRadius = 300f;
    private boolean markForRemoval = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private boolean isStunned = false;
    private boolean isActivated = false;
    private boolean arenaCreated = false;

    private EnemyType enemyType = EnemyType.HERMAN;

    private float animationTime = 0f;
    private State currentState = State.IDLE;

    private EnemyStats stats;
    private Texture healthBarTexture;
    private Texture whitePixelTexture;
    private Texture acornTexture;
    private Texture groundAttackTexture;

    // Attack system
    private float attackCooldown = 0f;
    private boolean isAttacking = false;
    private static float NORMAL_ATTACK_COOLDOWN;
    private static float PROJECTILE_SPEED;
    private static float PROJECTILE_RANGE;
    private static final Color ACORN_COLOR = new Color(0.6f, 0.4f, 0.2f, 1f);

    // Special attack system
    private enum SpecialAttackPhase {
        NONE,
        CASTING,
        ACTIVE
    }
    private SpecialAttackPhase specialPhase = SpecialAttackPhase.NONE;
    private static final float SPECIAL_ATTACK_COOLDOWN = 12.0f;
    private float specialCooldown = 5.0f;
    private float specialTimer = 0f;
    private static final float SPECIAL_DURATION = 5.0f;
    private static final float GROUND_ATTACK_SPAWN_INTERVAL = 0.5f;
    private static final float GROUND_ATTACK_DELAY = 1.0f;
    private float groundAttackSpawnTimer = 0f;

    private List<GroundAttackMarker> groundAttackMarkers = new ArrayList<>();
    private boolean hasDuplicated = false;
    private boolean isDuplicate = false;
    private Herman originalBoss = null;

    private DuplicateSpawnCallback duplicateSpawnCallback;

    private List<Projectile> projectiles = new ArrayList<>();
    private World world;

    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;

    private ArenaCreationCallback arenaCallback;

    private static class GroundAttackMarker {
        Vector2 position;
        float timer;
        float delay;
        boolean hasDamaged;
        float size;

        public GroundAttackMarker(Vector2 position, float delay) {
            this.position = new Vector2(position);
            this.timer = 0f;
            this.delay = delay;
            this.hasDamaged = false;
            this.size = 40f; // Attack radius
        }

        public boolean isExpired() {
            return timer > delay + 0.3f;
        }

        public boolean shouldDealDamage() {
            return timer >= delay && !hasDamaged;
        }
    }

    public interface ArenaCreationCallback {
        void onCreateArena(Vector2 bossPosition, float clearRadius, float barrierRadius);
    }

    public interface DuplicateSpawnCallback {
        void onSpawnDuplicate(Vector2 position, int health);
    }

    public Herman(Rectangle bounds, Body body, Player player,
                  AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;
        this.world = body.getWorld();

        NORMAL_ATTACK_COOLDOWN = stats.getAttackCooldown();
        PROJECTILE_RANGE = stats.getAttackRange();
        PROJECTILE_SPEED = stats.getProjectileSpeed();

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        this.whitePixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);

        try {
            this.acornTexture = Storage.assetManager.get("icons/effects/Acorn.png", Texture.class);
        } catch (Exception e) {
            this.acornTexture = null;
        }

        try {
            this.groundAttackTexture = Storage.assetManager.get("icons/effects/GroundAttack.png", Texture.class);
        } catch (Exception e) {
            this.groundAttackTexture = null;
        }

        this.currentState = State.IDLE;
        this.animationTime = 0f;

        // Herman is stationary
        if (body != null) {
            body.setLinearVelocity(0, 0);
            body.setType(BodyDef.BodyType.StaticBody);
        }
    }

    // Constructor for duplicate
    public Herman(Rectangle bounds, Body body, Player player,
                  AnimationManager animationManager, EnemyStats stats,
                  boolean isDuplicate, Herman original) {
        this(bounds, body, player, animationManager, stats);
        this.isDuplicate = isDuplicate;
        this.originalBoss = original;
        this.isActivated = true; // Duplicate is immediately active
        this.hasDuplicated = true; // Prevent duplicate from duplicating
    }

    public void setArenaCallback(ArenaCreationCallback callback) {
        this.arenaCallback = callback;
    }

    public void setDuplicateSpawnCallback(DuplicateSpawnCallback callback) {
        this.duplicateSpawnCallback = callback;
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
            // Loop IDLE and SPECIAL_ATTACK, don't loop ATTACKING
            boolean loop = (currentState == State.IDLE || currentState == State.SPECIAL_ATTACK);
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

        if (body != null) {
            body.setLinearVelocity(0, 0);
        }

        if (player.isInvisible()) {
            body.setLinearVelocity(0, 0);
            if (currentState != State.IDLE) {
                setState(State.IDLE);
            }
            animationTime += delta;
            bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                    body.getPosition().y - bounds.height / 2f);
            return;
        }

        if (isStunned) {
            if (currentState != State.IDLE) {
                setState(State.IDLE);
            }
            animationTime += delta;
            updateBounds();
            return;
        }

        animationTime += delta;

        if (isJustHit) {
            hitFlashTimer -= delta;
            if (hitFlashTimer <= 0) {
                isJustHit = false;
            }
        }

        if (!isActivated) {
            if (getDistanceToPlayer() <= activationRadius) {
                activate();
            } else {
                updateBounds();
                return;
            }
        }

        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }
        if (specialCooldown > 0) {
            specialCooldown -= delta;
        }

        updateProjectiles(delta);

        updateGroundAttackMarkers(delta);

        if (!hasDuplicated && !isDuplicate &&
                stats.getCurrentHealth() <= stats.getMaxHealth() * 0.5f) {
            performDuplication();
        }

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        updateBounds();

        if (specialPhase != SpecialAttackPhase.NONE) {
            updateSpecialAttack(delta);
        } else if (isAttacking) {
            updateNormalAttack(delta);
        } else {
            updateIdleState(delta);
        }
    }

    private void activate() {
        isActivated = true;

        if (!arenaCreated && arenaCallback != null) {
            arenaCallback.onCreateArena(
                    new Vector2(body.getPosition()),
                    activationRadius,
                    arenaRadius
            );
            arenaCreated = true;
        }
    }

    private void updateBounds() {
        if (body != null) {
            bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                    body.getPosition().y - bounds.height / 2f);
        }
    }

    private void updateIdleState(float delta) {
        if (specialCooldown <= 0) {
            startSpecialAttack();
        } else if (attackCooldown <= 0) {
            startNormalAttack();
        } else {
            if (currentState != State.IDLE) {
                setState(State.IDLE);
            }
        }
    }

    // =========================================================================
    // NORMAL ATTACK: Fire 3 acorns at player
    // =========================================================================
    private void startNormalAttack() {
        isAttacking = true;
        setState(State.ATTACKING);
    }

    private void updateNormalAttack(float delta) {
        if (isCurrentAnimationFinished()) {
            fireAcornBurst();
            endNormalAttack();
        }
    }

    private void fireAcornBurst() {
        Vector2 bossPos = new Vector2(body.getPosition());
        Vector2 playerPos = player.getPosition();
        Vector2 baseDirection = new Vector2(playerPos.x - bossPos.x, playerPos.y - bossPos.y).nor();

        float[] angles = {-15f, 0f, 15f};
        for (float angleOffset : angles) {
            Vector2 direction = rotateVector(baseDirection, angleOffset);
            Projectile projectile = new Projectile(
                    world,
                    bossPos,
                    direction,
                    PROJECTILE_SPEED,
                    PROJECTILE_RANGE,
                    stats.getDamage(),
                    ACORN_COLOR,
                    this,
                    acornTexture
            );
            projectiles.add(projectile);
        }
    }

    private Vector2 rotateVector(Vector2 vec, float degrees) {
        float rad = (float) Math.toRadians(degrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new Vector2(
                vec.x * cos - vec.y * sin,
                vec.x * sin + vec.y * cos
        );
    }

    private void endNormalAttack() {
        isAttacking = false;
        attackCooldown = NORMAL_ATTACK_COOLDOWN;
        setState(State.IDLE);
    }

    // =========================================================================
    // SPECIAL ATTACK: Ground markers that damage after 1 second
    // =========================================================================
    private void startSpecialAttack() {
        specialPhase = SpecialAttackPhase.CASTING;
        specialTimer = 0f;
        groundAttackSpawnTimer = 0f;
        groundAttackMarkers.clear();
        setState(State.SPECIAL_ATTACK);
    }

    private void updateSpecialAttack(float delta) {
        specialTimer += delta;

        switch (specialPhase) {
            case CASTING:
                if (specialTimer >= 0.5f) {
                    specialPhase = SpecialAttackPhase.ACTIVE;
                    specialTimer = 0f;
                    setState(State.SPECIAL_ATTACK);
                }
                break;

            case ACTIVE:
                groundAttackSpawnTimer += delta;
                if (groundAttackSpawnTimer >= GROUND_ATTACK_SPAWN_INTERVAL) {
                    spawnGroundAttack();
                    groundAttackSpawnTimer = 0f;
                }

                if (specialTimer >= SPECIAL_DURATION) {
                    endSpecialAttack();
                }
                break;

            default:
                break;
        }
    }

    private void spawnGroundAttack() {
        Vector2 targetPos = calculateGroundAttackPosition();
        groundAttackMarkers.add(new GroundAttackMarker(targetPos, GROUND_ATTACK_DELAY));
    }

    private Vector2 calculateGroundAttackPosition() {
        Vector2 playerPos = player.getPosition();
        Vector2 playerVelocity = player.getBody().getLinearVelocity();

        float velocityMagnitude = playerVelocity.len();
        float movementThreshold = 10f;

        if (velocityMagnitude > movementThreshold) {
            float predictionTime = 0.3f;
            Vector2 predictedPos = new Vector2(
                    playerPos.x + playerVelocity.x * predictionTime,
                    playerPos.y + playerVelocity.y * predictionTime
            );
            return predictedPos;
        } else {
            return new Vector2(playerPos);
        }
    }

    private void updateGroundAttackMarkers(float delta) {
        for (int i = groundAttackMarkers.size() - 1; i >= 0; i--) {
            GroundAttackMarker marker = groundAttackMarkers.get(i);
            marker.timer += delta;

            if (marker.shouldDealDamage()) {
                if (isPlayerInMarker(marker)) {
                    damagePlayer(stats.getDamage());
                }
                marker.hasDamaged = true;
            }

            if (marker.isExpired()) {
                groundAttackMarkers.remove(i);
            }
        }
    }

    private boolean isPlayerInMarker(GroundAttackMarker marker) {
        float dist = player.getPosition().dst(marker.position);
        return dist <= marker.size;
    }

    private void endSpecialAttack() {
        specialPhase = SpecialAttackPhase.NONE;
        specialTimer = 0f;
        specialCooldown = SPECIAL_ATTACK_COOLDOWN;
        setState(State.IDLE);
    }

    // =========================================================================
    // DUPLICATION AT 50% HP
    // =========================================================================
    private void performDuplication() {
        hasDuplicated = true;

        Vector2 bossPos = new Vector2(body.getPosition());
        float offsetX = 100f;
        float offsetY = 100f;
        Vector2 duplicatePos = new Vector2(bossPos.x + offsetX, bossPos.y - offsetY);

        int halfHealth = stats.getMaxHealth() / 2;

        stats.setCurrentHealth(halfHealth);

        if (duplicateSpawnCallback != null) {
            duplicateSpawnCallback.onSpawnDuplicate(duplicatePos, halfHealth);
        }
    }

    private void updateProjectiles(float delta) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update(delta);

            if (!projectile.isMarkedForRemoval() && projectile.getPosition() != null) {
                float dist = projectile.getPosition().dst(player.getPosition());
                if (dist < 12f) {
                    if (!player.isInvulnerable()) {
                        player.getStats().takeDamage(projectile.getDamage());
                        player.onTakeDamage();
                    }
                    projectile.markForRemoval();
                }
            }

            if (projectile.isMarkedForRemoval()) {
                projectile.dispose(world);
                projectiles.remove(i);
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (markForRemoval) return;

        renderGroundAttackMarkers(batch);

        for (Projectile proj : projectiles) {
            proj.render(batch);
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
            } else if (!isActivated) {
                batch.setColor(0.7f, 0.7f, 0.7f, 1f);
            } else if (isDuplicate) {
                batch.setColor(0.8f, 0.9f, 0.8f, 1f);
            }

            batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        if (isActivated) {
            renderBossHealthBar(batch);
        }

        if (!isActivated && getDistanceToPlayer() <= arenaRadius) {
            renderActivationIndicator(batch);
        }
    }

    private void renderGroundAttackMarkers(SpriteBatch batch) {
        for (GroundAttackMarker marker : groundAttackMarkers) {
            float progress = marker.timer / marker.delay;
            float alpha;
            float scale;

            if (marker.timer < marker.delay) {
                alpha = 0.3f + progress * 0.4f;
                scale = 0.5f + progress * 0.5f;

                float red = 1f;
                float green = 1f - progress * 0.7f;
                batch.setColor(red, green, 0.2f, alpha);
            } else {
                float postDamageTime = marker.timer - marker.delay;
                alpha = 0.8f * (1f - postDamageTime / 0.3f);
                scale = 1.2f;
                batch.setColor(1f, 0.2f, 0.1f, Math.max(0, alpha));
            }

            float size = marker.size * 2 * scale;
            if (groundAttackTexture != null) {
                batch.draw(groundAttackTexture,
                        marker.position.x - size / 2,
                        marker.position.y - size / 2,
                        size, size);
            } else {
                drawCircleFilled(batch, marker.position, marker.size * scale, 16);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawCircleFilled(SpriteBatch batch, Vector2 center, float radius, int segments) {
        float angleStep = 360f / segments;
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float rad = (float) Math.toRadians(angle);
            float x = center.x + (float) Math.cos(rad) * radius;
            float y = center.y + (float) Math.sin(rad) * radius;
            drawLine(batch, center.x, center.y, x, y, 2f);
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

    private void renderActivationIndicator(SpriteBatch batch) {
        Vector2 bossPos = new Vector2(body.getPosition());
        float pulse = (float) Math.sin(animationTime * 3f) * 0.2f + 0.5f;

        batch.setColor(0.8f, 0.5f, 0.2f, pulse * 0.3f);
        drawCircle(batch, bossPos, activationRadius, 32);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawCircle(SpriteBatch batch, Vector2 center, float radius, int segments) {
        float angleStep = 360f / segments;
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            float rad1 = (float) Math.toRadians(angle1);
            float rad2 = (float) Math.toRadians(angle2);

            float x1 = center.x + (float) Math.cos(rad1) * radius;
            float y1 = center.y + (float) Math.sin(rad1) * radius;
            float x2 = center.x + (float) Math.cos(rad2) * radius;
            float y2 = center.y + (float) Math.sin(rad2) * radius;

            drawLine(batch, x1, y1, x2, y2, 2f);
        }
    }

    private void renderBossHealthBar(SpriteBatch batch) {
        float barWidth = bounds.width * 2f;
        float barHeight = 6f;
        float barX = bounds.x - (barWidth - bounds.width) / 2f;
        float barY = bounds.y + bounds.height + 8f;

        batch.setColor(0.3f, 0.2f, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        float healthPercent = stats.getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        float red = 0.6f - healthPercent * 0.3f;
        float green = 0.4f + healthPercent * 0.4f;
        float blue = 0.2f;
        batch.setColor(red, green, blue, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        if (specialCooldown > 0) {
            float cooldownPercent = specialCooldown / SPECIAL_ATTACK_COOLDOWN;
            float cooldownWidth = barWidth * (1f - cooldownPercent);
            batch.setColor(0.8f, 0.4f, 0.1f, 0.7f);
            batch.draw(healthBarTexture, barX, barY - 4f, cooldownWidth, barHeight * 0.5f);
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private float getDistanceToPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition());
        return playerPosition.dst(bossPosition);
    }

    private void damagePlayer(int damage) {
        if (!player.isInvulnerable()) {
            player.getStats().takeDamage(damage);
            player.onTakeDamage();
        }
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

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
        // Dispose projectiles
        for (Projectile proj : projectiles) {
            proj.dispose(world);
        }
        projectiles.clear();

        groundAttackMarkers.clear();
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

    public boolean isActivated() {
        return isActivated;
    }

    public boolean isAttacking() {
        return isAttacking || specialPhase != SpecialAttackPhase.NONE;
    }

    public boolean isUsingSpecialAttack() {
        return specialPhase != SpecialAttackPhase.NONE;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public boolean hasDuplicated() {
        return hasDuplicated;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public EnemyType getEnemyType() {
        return enemyType;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public void setStunned(boolean stunned) {
        this.isStunned = stunned;
    }

    public boolean isStunned() {
        return isStunned;
    }

    public float getArenaRadius() {
        return arenaRadius;
    }

    public float getActivationRadius() {
        return activationRadius;
    }
}