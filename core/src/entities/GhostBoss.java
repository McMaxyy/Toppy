package entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
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
import ui.ScreenShake;

public class GhostBoss {
    public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float detectionRadius = 400f;
    private final float speed = 60f;
    private boolean markForRemoval = false;
    private boolean isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private boolean isStunned = false;

    private EnemyType enemyType = EnemyType.GHOST_BOSS;

    private float animationTime = 0f;
    private State currentState = State.IDLE;

    private EnemyStats stats;
    private Texture healthBarTexture;
    private Texture shadowBallTexture;

    private float attackCooldown = 0f;
    private boolean isAttacking = false;

    private static float PROJECTILE_COOLDOWN;
    private static float PROJECTILE_SPEED;
    private static float PROJECTILE_RANGE;
    private static final Color SHADOW_BALL_COLOR = new Color(0.5f, 0.2f, 0.8f, 1f);

    // Summon Ghostlings attack
    private enum SummonPhase {
        NONE,
        TELEPORTING,
        SPAWNING,
        SHOOTING_WHILE_WAITING
    }
    private SummonPhase summonPhase = SummonPhase.NONE;
    private static final float SUMMON_COOLDOWN = 15.0f;
    private float summonCooldown = 0f;
    private float summonTimer = 0f;

    private static final float SPAWN_INTERVAL = 0.5f;
    private static final int GHOSTS_PER_SPAWN = 3;
    private static final float SPAWN_DURATION = 3.0f;
    private float spawnTimer = 0f;
    private float totalSpawnTime = 0f;

    private List<Ghost> spawnedGhostlings = new ArrayList<>();
    private Vector2 cornerPosition = new Vector2();
    private float shootWhileWaitingTimer = 0f;
    private static final float SHOOT_WHILE_WAITING_INTERVAL = 0.8f;

    // Duplication attack (at 50% HP)
    private enum DuplicationPhase {
        NONE,
        DUPLICATING,
        SHOOTING
    }
    private DuplicationPhase duplicationPhase = DuplicationPhase.NONE;
    private boolean hasDuplicated = false;
    private static final float DUPLICATION_SHOOT_DURATION = 5.0f;
    private float duplicationTimer = 0f;
    private List<GhostBossDuplicate> duplicates = new ArrayList<>();
    private boolean isInvulnerableDuringDuplication = false;

    // Room corners (will be set by BossRoom)
    private Vector2[] roomCorners = new Vector2[4];
    private int roomWidth;
    private int roomHeight;
    private int tileSize;

    private Projectile[] projectilePool = new Projectile[20];
    private int nextProjectileIndex = 0;
    private World world;

    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;

    public static class GhostBossDuplicate {
        public Vector2 position;
        public boolean isFlipped;
        private float shootTimer = 0f;
        private static final float SHOOT_INTERVAL = 0.5f;

        public GhostBossDuplicate(Vector2 position) {
            this.position = new Vector2(position);
            this.isFlipped = false;
        }

        public boolean shouldShoot(float delta) {
            shootTimer += delta;
            if (shootTimer >= SHOOT_INTERVAL) {
                shootTimer = 0f;
                return true;
            }
            return false;
        }
    }

    public GhostBoss(Rectangle bounds, Body body, Player player,
                     AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;
        this.world = body.getWorld();

        PROJECTILE_COOLDOWN = stats.getAttackCooldown();
        PROJECTILE_SPEED = stats.getProjectileSpeed();
        PROJECTILE_RANGE = stats.getAttackRange();

        this.healthBarTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        try {
            this.shadowBallTexture = Storage.assetManager.get("icons/effects/ShadowBall.png", Texture.class);
        } catch (Exception e) {
            this.shadowBallTexture = null;
        }

        // Create pooled projectiles
        for (int i = 0; i < projectilePool.length; i++) {
            projectilePool[i] = new Projectile(
                    world,
                    new Vector2(body.getPosition()),
                    new Vector2(1, 0),
                    PROJECTILE_SPEED,
                    PROJECTILE_RANGE,
                    stats.getDamage(),
                    SHADOW_BALL_COLOR,
                    this,
                    shadowBallTexture
            );
            projectilePool[i].setActive(false);
        }

        this.currentState = State.IDLE;
        this.animationTime = 0f;
    }

    public void setRoomDimensions(int width, int height, int tileSize) {
        this.roomWidth = width;
        this.roomHeight = height;
        this.tileSize = tileSize;

        float margin = tileSize * 3;
        roomCorners[0] = new Vector2(margin, margin);
        roomCorners[1] = new Vector2(width * tileSize - margin, margin);
        roomCorners[2] = new Vector2(margin, height * tileSize - margin);
        roomCorners[3] = new Vector2(width * tileSize - margin, height * tileSize - margin);
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
        if (summonCooldown > 0) {
            summonCooldown -= delta;
        }

        updateProjectiles(delta);

        updateGhostlings(delta);

        isFlipped = body.getPosition().x > player.getBody().getPosition().x;

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        if (!hasDuplicated && duplicationPhase == DuplicationPhase.NONE &&
                stats.getCurrentHealth() <= stats.getMaxHealth() * 0.5f) {
            startDuplication();
        }

        if (duplicationPhase != DuplicationPhase.NONE) {
            updateDuplication(delta);
        } else if (summonPhase != SummonPhase.NONE) {
            updateSummon(delta);
        } else {
            updateMovementAndAttack(delta);
        }
    }

    private void updateProjectiles(float delta) {
        for (Projectile projectile : projectilePool) {
            if (!projectile.isActive()) continue;

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
    }

    private void updateGhostlings(float delta) {
        for (int i = spawnedGhostlings.size() - 1; i >= 0; i--) {
            Ghost ghost = spawnedGhostlings.get(i);
            if (ghost.isMarkedForRemoval()) {
                if (ghost.getBody() != null) {
                    world.destroyBody(ghost.getBody());
                    ghost.clearBody();
                }
                spawnedGhostlings.remove(i);
            } else {
                ghost.update(delta);
            }
        }
    }

    private void updateMovementAndAttack(float delta) {
        if (isPlayerInRadius()) {
            float distanceToPlayer = getDistanceToPlayer();

            if (summonCooldown <= 0 && spawnedGhostlings.isEmpty()) {
                startSummon();
            }
            else if (attackCooldown <= 0) {
                fireProjectile();
                attackCooldown = PROJECTILE_COOLDOWN;
            }
            else if (distanceToPlayer > 80f) {
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

    private void fireProjectile() {
        fireProjectileFrom(new Vector2(body.getPosition().x, body.getPosition().y));
        setState(State.ATTACKING);
    }

    private void fireProjectileFrom(Vector2 position) {
        Projectile projectile = getNextAvailableProjectile();
        if (projectile != null) {
            Vector2 playerPos = player.getPosition();
            Vector2 direction = new Vector2(playerPos.x - position.x, playerPos.y - position.y).nor();
            projectile.reset(position, direction);
        }
    }

    private Projectile getNextAvailableProjectile() {
        int startIndex = nextProjectileIndex;
        do {
            if (!projectilePool[nextProjectileIndex].isActive()) {
                Projectile result = projectilePool[nextProjectileIndex];
                nextProjectileIndex = (nextProjectileIndex + 1) % projectilePool.length;
                return result;
            }
            nextProjectileIndex = (nextProjectileIndex + 1) % projectilePool.length;
        } while (nextProjectileIndex != startIndex);

        Projectile result = projectilePool[nextProjectileIndex];
        nextProjectileIndex = (nextProjectileIndex + 1) % projectilePool.length;
        return result;
    }

    // =========================================================================
    // SUMMON GHOSTLINGS ATTACK
    // =========================================================================
    private void startSummon() {
        summonPhase = SummonPhase.TELEPORTING;
        summonTimer = 0f;
        spawnTimer = 0f;
        totalSpawnTime = 0f;

        int furthestCorner = getFurthestCornerFromPlayer();
        cornerPosition.set(roomCorners[furthestCorner]);

        body.setTransform(cornerPosition, 0f);
        body.setLinearVelocity(0, 0);

        setState(State.ATTACKING);
    }

    private int getFurthestCornerFromPlayer() {
        Vector2 playerPos = player.getPosition();
        int furthest = 0;
        float maxDist = 0f;

        for (int i = 0; i < 4; i++) {
            float dist = playerPos.dst(roomCorners[i]);
            if (dist > maxDist) {
                maxDist = dist;
                furthest = i;
            }
        }
        return furthest;
    }

    private void updateSummon(float delta) {
        summonTimer += delta;
        body.setLinearVelocity(0, 0);

        switch (summonPhase) {
            case TELEPORTING:
                if (summonTimer >= 0.5f) {
                    summonPhase = SummonPhase.SPAWNING;
                    summonTimer = 0f;
                    spawnTimer = SPAWN_INTERVAL; // Spawn immediately on first tick
                    totalSpawnTime = 0f;
                }
                break;

            case SPAWNING:
                body.setLinearVelocity(0, 0);
                totalSpawnTime += delta;
                spawnTimer += delta;

                if (spawnTimer >= SPAWN_INTERVAL) {
                    spawnGhostlingBatch();
                    spawnTimer = 0f;
                }

                if (totalSpawnTime >= SPAWN_DURATION) {
                    summonPhase = SummonPhase.SHOOTING_WHILE_WAITING;
                    summonTimer = 0f;
                    shootWhileWaitingTimer = 0f;
                }
                break;

            case SHOOTING_WHILE_WAITING:
                body.setLinearVelocity(0, 0);
                shootWhileWaitingTimer += delta;
                if (shootWhileWaitingTimer >= SHOOT_WHILE_WAITING_INTERVAL) {
                    fireProjectile();
                    shootWhileWaitingTimer = 0f;
                }

                if (spawnedGhostlings.isEmpty()) {
                    endSummon();
                }
                break;

            default:
                break;
        }
    }

    private void spawnGhostlingBatch() {
        Vector2 bossPos = new Vector2(body.getPosition().x, body.getPosition().y);
        Vector2 playerPos = player.getPosition();

        Vector2 toPlayer = new Vector2(playerPos.x - bossPos.x, playerPos.y - bossPos.y).nor();

        for (int i = 0; i < GHOSTS_PER_SPAWN; i++) {
            float spawnDistance = 30f + MathUtils.random(20f);
            float spreadAngle = MathUtils.random(-45f, 45f);

            float rad = MathUtils.degreesToRadians * spreadAngle;
            float cos = MathUtils.cos(rad);
            float sin = MathUtils.sin(rad);
            float spawnDirX = toPlayer.x * cos - toPlayer.y * sin;
            float spawnDirY = toPlayer.x * sin + toPlayer.y * cos;

            float spawnX = bossPos.x + spawnDirX * spawnDistance;
            float spawnY = bossPos.y + spawnDirY * spawnDistance;

            float margin = tileSize * 2.5f;
            spawnX = MathUtils.clamp(spawnX, margin, roomWidth * tileSize - margin);
            spawnY = MathUtils.clamp(spawnY, margin, roomHeight * tileSize - margin);

            spawnGhostling(spawnX, spawnY);
        }
    }

    private void spawnGhostling(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(4f, 4f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR |
                CollisionFilter.OBSTACLE | CollisionFilter.ABILITY | CollisionFilter.WALL;

        Body ghostBody = world.createBody(bodyDef);
        ghostBody.createFixture(fixtureDef);
        ghostBody.setFixedRotation(true);
        shape.dispose();

        PolygonShape enemyCollisionShape = new PolygonShape();
        enemyCollisionShape.setAsBox(2f, 2f);

        FixtureDef enemyFixtureDef = new FixtureDef();
        enemyFixtureDef.shape = enemyCollisionShape;
        enemyFixtureDef.density = 0.5f;
        enemyFixtureDef.friction = 0.1f;
        enemyFixtureDef.filter.categoryBits = CollisionFilter.ENEMY_ENEMY;
        enemyFixtureDef.filter.maskBits = CollisionFilter.ENEMY_ENEMY;
        ghostBody.createFixture(enemyFixtureDef);
        enemyCollisionShape.dispose();

        Rectangle ghostBounds = new Rectangle(x - 12, y - 12, 16, 16);
        EnemyStats ghostStats = EnemyStats.Factory.createGhostling(stats.getLevel());
        Ghost ghost = new Ghost(ghostBounds, ghostBody, player, animationManager, ghostStats);
        ghost.setOwnerBoss(this);
        ghostBody.setUserData(ghost);

        spawnedGhostlings.add(ghost);
    }

    private void endSummon() {
        summonPhase = SummonPhase.NONE;
        summonTimer = 0f;
        summonCooldown = SUMMON_COOLDOWN;

        setState(State.RUNNING);
    }

    public void onGhostlingDeath(Ghost ghost) {
    }

    // =========================================================================
    // DUPLICATION ATTACK (at 50% HP)
    // =========================================================================
    private void startDuplication() {
        hasDuplicated = true;
        duplicationPhase = DuplicationPhase.DUPLICATING;
        duplicationTimer = 0f;
        isInvulnerableDuringDuplication = true;

        summonPhase = SummonPhase.NONE;

        body.setLinearVelocity(0, 0);
        setState(State.ATTACKING);

        duplicates.clear();
        for (int i = 0; i < 3; i++) {
            duplicates.add(new GhostBossDuplicate(new Vector2()));
        }
    }

    private void updateDuplication(float delta) {
        duplicationTimer += delta;
        body.setLinearVelocity(0, 0);

        switch (duplicationPhase) {
            case DUPLICATING:
                if (duplicationTimer >= 0.3f) {
                    teleportToCorners();
                    duplicationPhase = DuplicationPhase.SHOOTING;
                    duplicationTimer = 0f;
                }
                break;

            case SHOOTING:
                if (attackCooldown <= 0) {
                    fireProjectile();
                    attackCooldown = 0.3f;
                }

                for (GhostBossDuplicate duplicate : duplicates) {
                    duplicate.isFlipped = duplicate.position.x > player.getPosition().x;
                    if (duplicate.shouldShoot(delta)) {
                        fireProjectileFrom(duplicate.position);
                    }
                }

                if (duplicationTimer >= DUPLICATION_SHOOT_DURATION) {
                    endDuplication();
                }
                break;

            default:
                break;
        }
    }

    private void teleportToCorners() {
        body.setTransform(roomCorners[0], 0f);

        for (int i = 0; i < duplicates.size() && i < 3; i++) {
            duplicates.get(i).position.set(roomCorners[i + 1]);
        }
    }

    private void endDuplication() {
        duplicationPhase = DuplicationPhase.NONE;
        duplicationTimer = 0f;
        isInvulnerableDuringDuplication = false;
        duplicates.clear();

        setState(State.RUNNING);
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    public void render(SpriteBatch batch) {
        if (markForRemoval) return;

        for (Ghost ghost : spawnedGhostlings) {
            ghost.render(batch);
        }

        for (Projectile proj : projectilePool) {
            if (proj.isActive()) {
                proj.render(batch);
            }
        }

        for (GhostBossDuplicate duplicate : duplicates) {
            renderDuplicate(batch, duplicate);
        }

        TextureRegion currentFrame = getCurrentFrame();
        if (currentFrame != null) {
            TextureRegion frame = new TextureRegion(currentFrame);
            if (isFlipped && !frame.isFlipX()) {
                frame.flip(true, false);
            } else if (!isFlipped && frame.isFlipX()) {
                frame.flip(true, false);
            }

            if (isJustHit && !isInvulnerableDuringDuplication) {
                batch.setColor(1f, 0.5f, 0.5f, 1f);
            } else if (isInvulnerableDuringDuplication) {
                float pulse = (float) Math.sin(duplicationTimer * 10f) * 0.2f + 0.8f;
                batch.setColor(0.8f, 0.6f, 1f, pulse);
            }

            batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    private void renderDuplicate(SpriteBatch batch, GhostBossDuplicate duplicate) {
        batch.setColor(0.7f, 0.5f, 0.9f, 0.7f);

        TextureRegion currentFrame = getCurrentFrame();
        if (currentFrame != null) {
            TextureRegion frame = new TextureRegion(currentFrame);
            if (duplicate.isFlipped && !frame.isFlipX()) {
                frame.flip(true, false);
            } else if (!duplicate.isFlipped && frame.isFlipX()) {
                frame.flip(true, false);
            }

            batch.draw(frame,
                    duplicate.position.x - bounds.width / 2f,
                    duplicate.position.y - bounds.height / 2f,
                    bounds.width, bounds.height);
        }

        batch.setColor(1f, 1f, 1f, 1f);
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
        Vector2 bossPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        Vector2 direction = new Vector2(playerPosition.x - bossPosition.x,
                playerPosition.y - bossPosition.y).nor();

        body.setLinearVelocity(direction.scl(speed));
    }

    public void takeDamage(int damage) {
        if (isInvulnerableDuringDuplication) {
            return; // Boss is invulnerable during duplication
        }

        if (damage > 0) {
            stats.takeDamage(damage);
            isJustHit = true;
            hitFlashTimer = HIT_FLASH_DURATION;

            ScreenShake.rumble(0.5f, 0.3f);
        }

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
        for (int i = 0; i < projectilePool.length; i++) {
            projectilePool[i] = null;
        }

        for (Ghost ghost : spawnedGhostlings) {
            if (ghost.getBody() != null) {
                world.destroyBody(ghost.getBody());
                ghost.clearBody();
            }
            ghost.dispose();
        }
        spawnedGhostlings.clear();

        duplicates.clear();
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
        return isAttacking || summonPhase != SummonPhase.NONE || duplicationPhase != DuplicationPhase.NONE;
    }

    public boolean isSummoning() {
        return summonPhase != SummonPhase.NONE;
    }

    public boolean isDuplicating() {
        return duplicationPhase != DuplicationPhase.NONE;
    }

    public List<Ghost> getSpawnedGhostlings() {
        return spawnedGhostlings;
    }

    public Projectile[] getProjectilePool() {
        return projectilePool;
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

    public boolean isInvulnerable() {
        return isInvulnerableDuringDuplication;
    }
}