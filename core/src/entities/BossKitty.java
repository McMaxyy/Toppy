package entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;

import config.Storage;
import managers.AnimationManager;
import managers.AnimationManager.State;
import managers.SoundManager;

public class BossKitty {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float detectionRadius = 300f;
    private final float speed = 70f;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private boolean isStunned = false;

    private EnemyStats stats;
    private Texture healthBarTexture;
    private Texture whitePixelTexture;

    private float attackCooldown = 0f;
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private boolean hasDealtDamage = false;

    private enum BossAttackMode {
        CHARGE,
        MELEE
    }
    private BossAttackMode currentAttackMode = BossAttackMode.MELEE;

    private final float CHARGE_DISTANCE_THRESHOLD = 100f;
    private float CHARGE_SPEED;
    private final float CHARGE_DURATION = 1f;
    private final float CHARGE_COOLDOWN = 4.0f;
    private float chargeCooldown = 0f;
    private boolean isCharging = false;
    private Vector2 chargeDirection = new Vector2();
    private Vector2 chargeStartPosition = new Vector2();

    private Array<TrailPoint> chargeTrailPoints = new Array<>();
    private float trailSpawnTimer = 0f;
    private final float TRAIL_SPAWN_INTERVAL = 0.02f;
    private final float TRAIL_POINT_LIFETIME = 0.4f;
    private final float TRAIL_ALPHA = 0.6f;

    private float MELEE_RANGE;
    private final float MELEE_ATTACK_DURATION = 0.8f;
    private float MELEE_COOLDOWN;
    private float meleeWindupTimer = 0f;
    private final float MELEE_WINDUP_TIME = 0.4f;
    private boolean showMeleeIndicator = false;

    private float specialAbilityCooldown = 0f;
    private float specialAbilityTimer = 0f;
    private boolean isUsingSpecialAbility = false;
    private final float SPECIAL_ABILITY_COOLDOWN = 10f;
    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;

    private static class TrailPoint {
        Vector2 position;
        float lifetime;
        float maxLifetime;

        TrailPoint(Vector2 pos, float lifetime) {
            this.position = new Vector2(pos);
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
        }
    }

    public BossKitty(Rectangle bounds, Body body, Player player,
                     AnimationManager animationManager, int level) {
        this(bounds, body, player, animationManager,
                EnemyStats.Factory.createBoss(level));
    }

    public BossKitty(Rectangle bounds, Body body, Player player,
                     AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;

        MELEE_RANGE = stats.getAttackRange();
        MELEE_COOLDOWN = stats.getAttackCooldown();
        CHARGE_SPEED = stats.getChargeSpeed();

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        this.whitePixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);

        getAnimationManager().setState(State.IDLE, "BossKitty");
    }

    public void update(float delta) {
        if (markForRemoval) {
            return;
        }

        if (isStunned) {
            body.setLinearVelocity(0, 0);
//            if (getAnimationManager().getState("BossKitty") != State.IDLE) {
//                getAnimationManager().setState(State.IDLE, "BossKitty");
//            }
            bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                    body.getPosition().y - bounds.height / 2f);
            return;
        }

        if (isJustHit) {
            hitFlashTimer -= delta;
            if (hitFlashTimer <= 0) {
                isJustHit = false;
            }
        }

        if (specialAbilityCooldown > 0) {
            specialAbilityCooldown -= delta;
        }
        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }
        if (chargeCooldown > 0) {
            chargeCooldown -= delta;
        }

        updateTrailPoints(delta);

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        // Check for special ability usage (when below 50% health)
        if (!isAttacking && !isUsingSpecialAbility && !isCharging &&
                specialAbilityCooldown <= 0 && stats.getCurrentHealth() < stats.getMaxHealth() * 0.5f) {
            startSpecialAbility();
        }

        // Update states
        if (isCharging) {
            updateChargeAttack(delta);
        } else if (isAttacking) {
            updateMeleeAttack(delta);
        } else if (isUsingSpecialAbility) {
            updateSpecialAbility(delta);
        } else {
            updateMovement(delta);
        }
    }

    private void updateTrailPoints(float delta) {
        for (int i = chargeTrailPoints.size - 1; i >= 0; i--) {
            TrailPoint point = chargeTrailPoints.get(i);
            point.lifetime -= delta;
            if (point.lifetime <= 0) {
                chargeTrailPoints.removeIndex(i);
            }
        }
    }

    private void updateMovement(float delta) {
        if (isPlayerInRadius()) {
            float distanceToPlayer = getDistanceToPlayer();

            if (distanceToPlayer > CHARGE_DISTANCE_THRESHOLD && chargeCooldown <= 0 && attackCooldown <= 0) {
                startChargeAttack();
            } else if (distanceToPlayer <= MELEE_RANGE && attackCooldown <= 0) {
                startMeleeAttack();
            } else {
                moveTowardsPlayer();
                isMoving = true;

                if (getAnimationManager().getState("BossKitty") != State.RUNNING) {
                    getAnimationManager().setState(State.RUNNING, "BossKitty");
                }
            }
        } else {
            body.setLinearVelocity(0, 0);
            isMoving = false;

            if (getAnimationManager().getState("BossKitty") != State.IDLE) {
                getAnimationManager().setState(State.IDLE, "BossKitty");
            }
        }
    }


    private void startChargeAttack() {
        isCharging = true;
        attackTimer = 0f;
        hasDealtDamage = false;
        chargeTrailPoints.clear();

        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        chargeDirection = new Vector2(playerPosition.x - bossPosition.x,
                playerPosition.y - bossPosition.y).nor();
        chargeStartPosition.set(bossPosition);

        body.setLinearVelocity(chargeDirection.x * CHARGE_SPEED, chargeDirection.y * CHARGE_SPEED);

        getAnimationManager().setState(State.DYING, "BossKitty");

        System.out.println("Boss started CHARGE attack!");
    }

    private void updateChargeAttack(float delta) {
        attackTimer += delta;

        trailSpawnTimer += delta;
        if (trailSpawnTimer >= TRAIL_SPAWN_INTERVAL) {
            Vector2 bossPos = new Vector2(body.getPosition().x, body.getPosition().y);
            chargeTrailPoints.add(new TrailPoint(bossPos, TRAIL_POINT_LIFETIME));
            trailSpawnTimer = 0f;
        }

        if (!hasDealtDamage && isPlayerInChargeHitbox()) {
            damagePlayer(stats.getDamage() * 2);
            hasDealtDamage = true;
        }

        if (attackTimer >= CHARGE_DURATION) {
            endChargeAttack();
        }
    }

    private boolean isPlayerInChargeHitbox() {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(bossPosition);
        return distance <= 30f; // Charge hit radius
    }

    private void endChargeAttack() {
        isCharging = false;
        attackTimer = 0f;
        chargeCooldown = CHARGE_COOLDOWN;
        attackCooldown = 1.0f; // Short cooldown before next attack
        hasDealtDamage = false;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.RUNNING, "BossKitty");
        System.out.println("Boss CHARGE ended!");
    }

    private void startMeleeAttack() {
        isAttacking = true;
        currentAttackMode = BossAttackMode.MELEE;
        attackTimer = 0f;
        meleeWindupTimer = 0f;
        hasDealtDamage = false;
        showMeleeIndicator = true;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.DYING, "BossKitty");
    }

    private void updateMeleeAttack(float delta) {
        attackTimer += delta;
        meleeWindupTimer += delta;

        body.setLinearVelocity(0, 0);

        if (meleeWindupTimer >= MELEE_WINDUP_TIME && !hasDealtDamage) {
            if (isPlayerInMeleeRange()) {
                damagePlayer(stats.getDamage());
                hasDealtDamage = true;
            }
            showMeleeIndicator = false;
        }

        if (attackTimer >= MELEE_ATTACK_DURATION) {
            endMeleeAttack();
        }
    }

    private boolean isPlayerInMeleeRange() {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(bossPosition);
        return distance <= MELEE_RANGE;
    }

    private void endMeleeAttack() {
        isAttacking = false;
        attackTimer = 0f;
        attackCooldown = MELEE_COOLDOWN;
        hasDealtDamage = false;
        showMeleeIndicator = false;

        getAnimationManager().setState(State.RUNNING, "BossKitty");
    }

    private void startSpecialAbility() {
        Vector2 targetPosition = new Vector2(player.getPosition().x + 10, player.getPosition().y + 10);
        body.setTransform(targetPosition, 1f);
        isUsingSpecialAbility = true;
        specialAbilityTimer = 0f;
        hasDealtDamage = false;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.DYING, "BossKitty");
    }

    private void updateSpecialAbility(float delta) {
        body.setLinearVelocity(0, 0);
        specialAbilityTimer += delta;

        if (specialAbilityTimer >= 1.5f) {
            endSpecialAbility();
        } else if (specialAbilityTimer >= 1f) {
            if (!hasDealtDamage) {
                if (getDistanceToPlayer() <= stats.getAoeRadius()) {
                    damagePlayer(stats.getDamage() * 3);
                    hasDealtDamage = true;
                }
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

    public void render(SpriteBatch batch) {
        if (markForRemoval) return;

        renderChargeTrail(batch);

        if (showMeleeIndicator) {
            renderMeleeIndicator(batch);
        }

        if (isUsingSpecialAbility) {
            renderSpecialAbilityIndicator(batch);
        }

        if (isJustHit) {
            batch.setColor(1f, 0.5f, 0.5f, 1f);
        }
        TextureRegion currentFrame = new TextureRegion(getAnimationManager().getBossKittyCurrentFrame());
        currentFrame.flip(isFlipped, false);
        batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        if (isJustHit) {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        renderBossHealthBar(batch);
    }

    private void renderChargeTrail(SpriteBatch batch) {
        if (chargeTrailPoints.size == 0) return;

        TextureRegion frame = getAnimationManager().getBossKittyCurrentFrame();
        float size = bounds.width * 0.8f;

        for (TrailPoint point : chargeTrailPoints) {
            float alpha = TRAIL_ALPHA * (point.lifetime / point.maxLifetime);

            // Orange/red tint for boss charge trail
            batch.setColor(1f, 0.4f, 0.1f, alpha);
            batch.draw(frame,
                    point.position.x - size / 2f,
                    point.position.y - size / 2f,
                    size, size);
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderMeleeIndicator(SpriteBatch batch) {
        Vector2 bossPos = new Vector2(body.getPosition().x, body.getPosition().y);

        float fillProgress = Math.min(1f, meleeWindupTimer / MELEE_WINDUP_TIME);

        int segments = 32;
        float angleStep = 360f / segments;

        batch.setColor(1f, 0.2f, 0.2f, 0.3f * fillProgress);
        for (int i = 0; i < segments * fillProgress; i++) {
            float angle = i * angleStep;
            float rad = (float) Math.toRadians(angle);
            float x = bossPos.x + (float) Math.cos(rad) * MELEE_RANGE;
            float y = bossPos.y + (float) Math.sin(rad) * MELEE_RANGE;
            drawLine(batch, bossPos.x, bossPos.y, x, y, 2f);
        }

        // Draw circle outline
        batch.setColor(1f, 0.3f, 0.3f, 0.6f);
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            float rad1 = (float) Math.toRadians(angle1);
            float rad2 = (float) Math.toRadians(angle2);

            float x1 = bossPos.x + (float) Math.cos(rad1) * MELEE_RANGE;
            float y1 = bossPos.y + (float) Math.sin(rad1) * MELEE_RANGE;
            float x2 = bossPos.x + (float) Math.cos(rad2) * MELEE_RANGE;
            float y2 = bossPos.y + (float) Math.sin(rad2) * MELEE_RANGE;

            drawLine(batch, x1, y1, x2, y2, 2f);
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderSpecialAbilityIndicator(SpriteBatch batch) {
        Vector2 bossPos = new Vector2(body.getPosition().x, body.getPosition().y);
        float specialRadius = 80f;

        // Pulsing red glow
        float pulse = (float) Math.sin(specialAbilityTimer * 8f) * 0.3f + 0.5f;

        // Draw expanding circle
        int segments = 32;
        float angleStep = 360f / segments;

        batch.setColor(1f, 0f, 0f, pulse);
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            float rad1 = (float) Math.toRadians(angle1);
            float rad2 = (float) Math.toRadians(angle2);

            float x1 = bossPos.x + (float) Math.cos(rad1) * specialRadius;
            float y1 = bossPos.y + (float) Math.sin(rad1) * specialRadius;
            float x2 = bossPos.x + (float) Math.cos(rad2) * specialRadius;
            float y2 = bossPos.y + (float) Math.sin(rad2) * specialRadius;

            drawLine(batch, x1, y1, x2, y2, 3f);
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

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

        // Charge cooldown indicator (blue bar below health)
        if (chargeCooldown > 0) {
            float cooldownPercent = chargeCooldown / CHARGE_COOLDOWN;
            float cooldownWidth = barWidth * (1f - cooldownPercent);
            batch.setColor(0.3f, 0.5f, 1f, 0.7f);
            batch.draw(healthBarTexture, barX, barY - 3f, cooldownWidth, barHeight * 0.4f);
        }

        // Special ability cooldown indicator
        if (specialAbilityCooldown > 0) {
            float cooldownPercent = specialAbilityCooldown / SPECIAL_ABILITY_COOLDOWN;
            float cooldownWidth = barWidth * (1f - cooldownPercent);
            batch.setColor(1f, 0.5f, 0f, 0.7f);
            batch.draw(healthBarTexture, barX, barY - 6f, cooldownWidth, barHeight * 0.4f);
        }

        // Reset color
        batch.setColor(1f, 1f, 1f, 1f);
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

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================
    private float getDistanceToPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        return playerPosition.dst(bossPosition);
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

    private void damagePlayer(int damage) {
        if (!player.isInvulnerable()) {
            player.getStats().takeDamage(damage);
            player.onTakeDamage();
        }
    }

    public void damagePlayer() {
        damagePlayer(stats.getDamage());
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
        chargeTrailPoints.clear();
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
        return isAttacking || isCharging;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public boolean isUsingSpecialAbility() {
        return isUsingSpecialAbility;
    }

    public AttackType getAttackType() {
        return stats.getAttackType();
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

    public boolean isStunned() { return isStunned; }
}