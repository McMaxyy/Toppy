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
import managers.SoundManager;

public class Cyclops {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float detectionRadius = 350f;
    private final float speed = 60f;
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
    private float MELEE_RANGE;
    private final float MELEE_ATTACK_DURATION = 1.0f;
    private float MELEE_COOLDOWN;
    private float meleeWindupTimer = 0f;
    private final float MELEE_WINDUP_TIME = 0.5f;
    private boolean showMeleeIndicator = false;
    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;

    // Ground Pound attack (replaces charge attack)
    private enum GroundPoundPhase {
        NONE,
        CHARGING,
        FIRST_PULSE,
        SECOND_PULSE,
    }
    private GroundPoundPhase groundPoundPhase = GroundPoundPhase.NONE;
    private final float GROUND_POUND_COOLDOWN = 5.0f;
    private float groundPoundCooldown = 0f;
    private float groundPoundTimer = 0f;
    private final float GROUND_POUND_CHARGE_TIME = 1.2f;
    private final float FIRST_PULSE_RADIUS = 75f;
    private float SECOND_PULSE_RADIUS;
    private final float PULSE_DURATION = 0.3f;
    private float currentPulseRadius = 0f;
    private boolean firstPulseDamageDealt = false;
    private boolean secondPulseDamageDealt = false;
    private final float GROUND_POUND_DISTANCE_THRESHOLD = 75f;

    private float specialAbilityCooldown = 0f;
    private float specialAbilityTimer = 0f;
    private boolean isUsingSpecialAbility = false;
    private final float SPECIAL_ABILITY_COOLDOWN = 10f;

    public Cyclops(Rectangle bounds, Body body, Player player,
                   AnimationManager animationManager, int level) {
        this(bounds, body, player, animationManager,
                EnemyStats.Factory.createCyclops(level));
    }

    public Cyclops(Rectangle bounds, Body body, Player player,
                   AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;

        MELEE_RANGE = stats.getAttackRange();
        MELEE_COOLDOWN = stats.getAttackCooldown();
        SECOND_PULSE_RADIUS = stats.getAoeRadius();

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        this.whitePixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);

        getAnimationManager().setState(State.IDLE, "Cyclops");
    }

    public void update(float delta) {
        if (markForRemoval) {
            return;
        }

        if (isStunned) {
            body.setLinearVelocity(0, 0);
            if (getAnimationManager().getState("Cyclops") != State.IDLE) {
                getAnimationManager().setState(State.IDLE, "Cyclops");
            }
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

        // Update cooldowns
        if (specialAbilityCooldown > 0) {
            specialAbilityCooldown -= delta;
        }
        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }
        if (groundPoundCooldown > 0) {
            groundPoundCooldown -= delta;
        }

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        // Check for special ability usage (when below 50% health)
        if (!isAttacking && !isUsingSpecialAbility && groundPoundPhase == GroundPoundPhase.NONE &&
                specialAbilityCooldown <= 0 && stats.getCurrentHealth() < stats.getMaxHealth() * 0.5f) {
            startSpecialAbility();
        }

        // Update states
        if (groundPoundPhase != GroundPoundPhase.NONE) {
            updateGroundPound(delta);
        } else if (isAttacking) {
            updateMeleeAttack(delta);
        } else if (isUsingSpecialAbility) {
            updateSpecialAbility(delta);
        } else {
            updateMovement(delta);
        }
    }

    private void updateMovement(float delta) {
        if (isPlayerInRadius()) {
            float distanceToPlayer = getDistanceToPlayer();

            if (distanceToPlayer > GROUND_POUND_DISTANCE_THRESHOLD &&
                    distanceToPlayer <= detectionRadius * 0.6f &&
                    groundPoundCooldown <= 0 && attackCooldown <= 0) {
                startGroundPound();
            } else if (distanceToPlayer <= MELEE_RANGE && attackCooldown <= 0) {
                startMeleeAttack();
            } else {
                moveTowardsPlayer();
                isMoving = true;

                if (getAnimationManager().getState("Cyclops") != State.RUNNING) {
                    getAnimationManager().setState(State.RUNNING, "Cyclops");
                }
            }
        } else {
            body.setLinearVelocity(0, 0);
            isMoving = false;

            if (getAnimationManager().getState("Cyclops") != State.IDLE) {
                getAnimationManager().setState(State.IDLE, "Cyclops");
            }
        }
    }

    private void startGroundPound() {
        Vector2 targetPosition = new Vector2(player.getPosition().x + 10, player.getPosition().y + 10);
        body.setTransform(targetPosition, 1f);
        groundPoundPhase = GroundPoundPhase.CHARGING;
        groundPoundTimer = 0f;
        firstPulseDamageDealt = false;
        secondPulseDamageDealt = false;
        currentPulseRadius = 0f;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.ATTACKING, "Cyclops");
    }

    private void updateGroundPound(float delta) {
        groundPoundTimer += delta;
        body.setLinearVelocity(0, 0);

        switch (groundPoundPhase) {
            case CHARGING:
                if (groundPoundTimer >= GROUND_POUND_CHARGE_TIME) {
                    groundPoundPhase = GroundPoundPhase.FIRST_PULSE;
                    groundPoundTimer = 0f;
                    currentPulseRadius = FIRST_PULSE_RADIUS;
                }
                break;

            case FIRST_PULSE:
                if (!firstPulseDamageDealt && isPlayerInPulseRange(FIRST_PULSE_RADIUS)) {
                    damagePlayer(stats.getDamage());
                    firstPulseDamageDealt = true;
                }

                if (groundPoundTimer >= PULSE_DURATION) {
                    groundPoundPhase = GroundPoundPhase.SECOND_PULSE;
                    groundPoundTimer = 0f;
                    currentPulseRadius = SECOND_PULSE_RADIUS;
                }
                break;

            case SECOND_PULSE:
                if (!secondPulseDamageDealt && isPlayerInPulseRange(SECOND_PULSE_RADIUS)) {
                    damagePlayer((int)(stats.getDamage() * 1.5f));
                    secondPulseDamageDealt = true;
                }

                if (groundPoundTimer >= PULSE_DURATION) {
                    endGroundPound();
                }
                break;

            default:
                break;
        }
    }

    private boolean isPlayerInPulseRange(float radius) {
        Vector2 playerPosition = player.getPosition();
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);
        float distance = playerPosition.dst(bossPosition);
        return distance <= radius;
    }

    private void endGroundPound() {
        groundPoundPhase = GroundPoundPhase.NONE;
        groundPoundTimer = 0f;
        groundPoundCooldown = GROUND_POUND_COOLDOWN;
        attackCooldown = 1.5f;
        currentPulseRadius = 0f;
        firstPulseDamageDealt = false;
        secondPulseDamageDealt = false;

        getAnimationManager().setState(State.RUNNING, "Cyclops");
    }

    private void startMeleeAttack() {
        isAttacking = true;
        attackTimer = 0f;
        meleeWindupTimer = 0f;
        hasDealtDamage = false;
        showMeleeIndicator = true;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.ATTACKING, "Cyclops");
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

        getAnimationManager().setState(State.RUNNING, "Cyclops");
    }

    private void startSpecialAbility() {
        Vector2 targetPosition = new Vector2(player.getPosition().x + 10, player.getPosition().y + 10);
        body.setTransform(targetPosition, 1f);
        isUsingSpecialAbility = true;
        specialAbilityTimer = 0f;
        hasDealtDamage = false;
        body.setLinearVelocity(0, 0);

        getAnimationManager().setState(State.ATTACKING, "Cyclops");
    }

    private void updateSpecialAbility(float delta) {
        specialAbilityTimer += delta;
        Vector2 bodyVel = new Vector2(0, 0);
        if (body.getLinearVelocity() != bodyVel)
            body.setLinearVelocity(0, 0);

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

        getAnimationManager().setState(State.RUNNING, "Cyclops");
    }

    public void render(SpriteBatch batch) {
        if (markForRemoval) return;

        // Render ground pound indicators
        if (groundPoundPhase != GroundPoundPhase.NONE) {
            renderGroundPoundIndicator(batch);
        }

        if (showMeleeIndicator) {
            renderMeleeIndicator(batch);
        }

        if (isUsingSpecialAbility) {
            renderSpecialAbilityIndicator(batch);
        }

        if (isJustHit) {
            batch.setColor(1f, 0.5f, 0.5f, 1f);
        }
        TextureRegion currentFrame = new TextureRegion(getAnimationManager().getCyclopsCurrentFrame());
        currentFrame.flip(isFlipped, false);
        batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        if (isJustHit) {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        renderBossHealthBar(batch);
    }

    private void renderGroundPoundIndicator(SpriteBatch batch) {
        Vector2 bossPos = new Vector2(body.getPosition().x, body.getPosition().y);
        int segments = 32;
        float angleStep = 360f / segments;

        if (groundPoundPhase == GroundPoundPhase.CHARGING) {
            // Show charging indicator - growing circle
            float chargeProgress = Math.min(1f, groundPoundTimer / GROUND_POUND_CHARGE_TIME);
            float indicatorRadius = FIRST_PULSE_RADIUS * chargeProgress;

            // Outer ring (shows max first pulse range)
            batch.setColor(1f, 0.5f, 0f, 0.3f);
            drawCircle(batch, bossPos, FIRST_PULSE_RADIUS, segments, angleStep, 2f);

            // Second pulse preview
            batch.setColor(1f, 0.2f, 0f, 0.15f);
            drawCircle(batch, bossPos, SECOND_PULSE_RADIUS, segments, angleStep, 1f);

            // Charging fill
            batch.setColor(1f, 0.6f, 0.1f, 0.4f * chargeProgress);
            for (int i = 0; i < segments * chargeProgress; i++) {
                float angle = i * angleStep;
                float rad = (float) Math.toRadians(angle);
                float x = bossPos.x + (float) Math.cos(rad) * indicatorRadius;
                float y = bossPos.y + (float) Math.sin(rad) * indicatorRadius;
                drawLine(batch, bossPos.x, bossPos.y, x, y, 2f);
            }

        } else if (groundPoundPhase == GroundPoundPhase.FIRST_PULSE) {
            // First pulse - expanding orange circle
            float pulseProgress = groundPoundTimer / PULSE_DURATION;
            float alpha = 0.7f * (1f - pulseProgress);

            batch.setColor(1f, 0.5f, 0f, alpha);
            drawCircle(batch, bossPos, FIRST_PULSE_RADIUS, segments, angleStep, 4f);

            // Fill
            batch.setColor(1f, 0.6f, 0.2f, alpha * 0.5f);
            for (int i = 0; i < segments; i++) {
                float angle = i * angleStep;
                float rad = (float) Math.toRadians(angle);
                float x = bossPos.x + (float) Math.cos(rad) * FIRST_PULSE_RADIUS;
                float y = bossPos.y + (float) Math.sin(rad) * FIRST_PULSE_RADIUS;
                drawLine(batch, bossPos.x, bossPos.y, x, y, 2f);
            }

        } else if (groundPoundPhase == GroundPoundPhase.SECOND_PULSE) {
            // Second pulse - larger red circle
            float pulseProgress = groundPoundTimer / PULSE_DURATION;
            float alpha = 0.8f * (1f - pulseProgress);

            batch.setColor(1f, 0.2f, 0f, alpha);
            drawCircle(batch, bossPos, SECOND_PULSE_RADIUS, segments, angleStep, 5f);

            // Fill
            batch.setColor(1f, 0.3f, 0.1f, alpha * 0.4f);
            for (int i = 0; i < segments; i++) {
                float angle = i * angleStep;
                float rad = (float) Math.toRadians(angle);
                float x = bossPos.x + (float) Math.cos(rad) * SECOND_PULSE_RADIUS;
                float y = bossPos.y + (float) Math.sin(rad) * SECOND_PULSE_RADIUS;
                drawLine(batch, bossPos.x, bossPos.y, x, y, 2f);
            }
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawCircle(SpriteBatch batch, Vector2 center, float radius, int segments, float angleStep, float thickness) {
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            float rad1 = (float) Math.toRadians(angle1);
            float rad2 = (float) Math.toRadians(angle2);

            float x1 = center.x + (float) Math.cos(rad1) * radius;
            float y1 = center.y + (float) Math.sin(rad1) * radius;
            float x2 = center.x + (float) Math.cos(rad2) * radius;
            float y2 = center.y + (float) Math.sin(rad2) * radius;

            drawLine(batch, x1, y1, x2, y2, thickness);
        }
    }

    private void renderMeleeIndicator(SpriteBatch batch) {
        Vector2 bossPos = new Vector2(body.getPosition().x, body.getPosition().y);

        float fillProgress = Math.min(1f, meleeWindupTimer / MELEE_WINDUP_TIME);

        int segments = 32;
        float angleStep = 360f / segments;

        // Draw filled portion (danger zone filling up)
        batch.setColor(0.8f, 0.4f, 0.1f, 0.3f * fillProgress);
        for (int i = 0; i < segments * fillProgress; i++) {
            float angle = i * angleStep;
            float rad = (float) Math.toRadians(angle);
            float x = bossPos.x + (float) Math.cos(rad) * MELEE_RANGE;
            float y = bossPos.y + (float) Math.sin(rad) * MELEE_RANGE;
            drawLine(batch, bossPos.x, bossPos.y, x, y, 2f);
        }

        // Draw circle outline
        batch.setColor(0.9f, 0.5f, 0.2f, 0.6f);
        drawCircle(batch, bossPos, MELEE_RANGE, segments, angleStep, 2f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderSpecialAbilityIndicator(SpriteBatch batch) {
        Vector2 bossPos = new Vector2(body.getPosition().x, body.getPosition().y);
        float specialRadius = stats.getAoeRadius();

        float pulse = (float) Math.sin(specialAbilityTimer * 8f) * 0.3f + 0.5f;

        int segments = 32;
        float angleStep = 360f / segments;

        batch.setColor(0.8f, 0.2f, 0.8f, pulse);
        drawCircle(batch, bossPos, specialRadius, segments, angleStep, 3f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderBossHealthBar(SpriteBatch batch) {
        float barWidth = bounds.width * 1.5f;
        float barHeight = 5f;
        float barX = bounds.x - (barWidth - bounds.width) / 2f;
        float barY = bounds.y + bounds.height + 5f;

        // Background (dark purple for Cyclops)
        batch.setColor(0.3f, 0.0f, 0.3f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        // Foreground (gradient based on health)
        float healthPercent = stats.getHealthPercentage();
        float healthWidth = barWidth * healthPercent;

        // Purple to orange gradient as health decreases
        float red = 1.0f - (healthPercent * 0.2f);
        float green = healthPercent * 0.5f;
        float blue = healthPercent * 0.8f;
        batch.setColor(red, green, blue, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        // Ground pound cooldown indicator (orange bar below health)
        if (groundPoundCooldown > 0) {
            float cooldownPercent = groundPoundCooldown / GROUND_POUND_COOLDOWN;
            float cooldownWidth = barWidth * (1f - cooldownPercent);
            batch.setColor(1f, 0.5f, 0f, 0.7f);
            batch.draw(healthBarTexture, barX, barY - 3f, cooldownWidth, barHeight * 0.4f);
        }

        // Special ability cooldown indicator
        if (specialAbilityCooldown > 0) {
            float cooldownPercent = specialAbilityCooldown / SPECIAL_ABILITY_COOLDOWN;
            float cooldownWidth = barWidth * (1f - cooldownPercent);
            batch.setColor(0.8f, 0.2f, 0.8f, 0.7f);
            batch.draw(healthBarTexture, barX, barY - 6f, cooldownWidth, barHeight * 0.4f);
        }

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
        // Clean up any resources
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
        return isAttacking || groundPoundPhase != GroundPoundPhase.NONE;
    }

    public boolean isUsingGroundPound() {
        return groundPoundPhase != GroundPoundPhase.NONE;
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