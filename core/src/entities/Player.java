package entities;

import abilities.AbilityVisual;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;

import config.GameScreen;
import config.Storage;
import game.GameProj;
import items.Item;
import managers.*;
import abilities.AbilityManager;

public class Player implements PlayerStats.SpeedChangeListener {
    private Body body;
    private Array<Body> spearBodies = new Array<>();
    private Array<Vector2> spearVelocities = new Array<>();
    private Array<Vector2> spearStartPositions = new Array<>();
    private float speed, maxDistance = 80f;
    private AnimationManager animationManager;
    private int direction;
    private static String movementAbility = "";
    private GameProj gameP;
    private Box2DWorld world;
    private Array<Boolean> spearMarkedForRemoval = new Array<>();
    private float spearCooldown = 0f;
    private Texture whitePixel;
    private boolean playerDeath;
    private GameScreen gameScreen;
    public static boolean gameStarted = false;
    private boolean isFlipped = false;
    private boolean isInvisible = false;
    private float dashSpeed = 3000f;
    private float dashDuration = 0.5f;
    private float dashCooldown = 1f;
    private float dashTimer = 0f;
    private float dashCooldownTimer = 0f;
    private boolean isDashing = false;
    private Vector2 dashDirection = new Vector2();
    private boolean invulnerable;
    private short originalMaskBits;
    private short notOriginalMaskBits;
    private short vaultMaskBits;
    private boolean dyingAnimationStarted = false;
    private Inventory inventory;
    private ItemSpawner itemSpawner;
    private PlayerStats stats;
    private Texture healthBarBgTexture;
    private AbilityManager abilityManager;
    private boolean isPaused = false;
    private boolean isCharging = false;
    private float chargeTimer = 0f;
    private Vector2 chargeVelocity = new Vector2();
    private java.util.Set<Object> chargeHitEnemies;
    private BuffManager buffManager;

    private PlayerClass playerClass;
    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;
    private boolean playerBuffs[] = new boolean[4];
    private boolean isVaulting = false;
    private float vaultTimer = 0f;
    private Vector2 vaultVelocity = new Vector2();

    private boolean isSprinting = false;
    private boolean isHolyBlessingActive = false;
    private boolean isBlazingFuryActive = false;
    private boolean isHolySwordActive = false;
    private float holySwordConeMultiplier = 1.0f;
    private boolean isLifeLeechActive = false;
    private int lifeLeechHealAmount = 0;

    private class Trail {
        Vector2 position;
        float lifetime;

        public Trail(Vector2 position, float lifetime) {
            this.position = new Vector2(position);
            this.lifetime = lifetime;
        }
    }

    private Array<Trail> trails = new Array<>();
    private float trailSpawnInterval = 0.05f;
    private float trailTimer = 0f;
    private float trailLifetime = 0.5f;

    public Player(Box2DWorld world, AnimationManager animationManager, int size, GameProj gameP, GameScreen gameScreen) {
        this(world, animationManager, size, gameP, gameScreen, PlayerClass.MERCENARY);
    }

    public Player(Box2DWorld world, AnimationManager animationManager, int size, GameProj gameP, GameScreen gameScreen, PlayerClass playerClass) {
        this.animationManager = animationManager;
        this.gameP = gameP;
        this.world = world;
        this.gameScreen = gameScreen;
        this.inventory = new Inventory();
        this.stats = new PlayerStats();
        this.buffManager = new BuffManager(this);
        this.healthBarBgTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        this.playerClass = playerClass;
        whitePixel = Storage.assetManager.get("white_pixel.png", Texture.class);

        // Set initial speed from stats and register for speed changes
        this.speed = stats.getBaseSpeed();
        stats.setSpeedChangeListener(this);

        animationManager.setPlayerClass(playerClass);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(0, 0);

        body = world.getWorld().createBody(bodyDef);
        body.setFixedRotation(true);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(size / 10f, size / 6f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.3f;

        fixtureDef.filter.categoryBits = CollisionFilter.PLAYER;
        fixtureDef.filter.maskBits = CollisionFilter.OBSTACLE | CollisionFilter.ENEMY | CollisionFilter.WALL;
        originalMaskBits = fixtureDef.filter.maskBits;

        body.createFixture(fixtureDef);

        fixtureDef.filter.maskBits = CollisionFilter.ENEMY;
        notOriginalMaskBits = fixtureDef.filter.maskBits;

        fixtureDef.filter.maskBits = CollisionFilter.OBSTACLE | CollisionFilter.WALL;
        vaultMaskBits = fixtureDef.filter.maskBits;

        shape.dispose();
    }

    @Override
    public void onSpeedChanged(float newSpeed) {
        this.speed = newSpeed;
    }

    public void initializeAbilityManager(GameProj gameProj) {
        this.abilityManager = new AbilityManager(this, gameProj, playerClass);
    }

    public void setItemSpawner(ItemSpawner itemSpawner) {
        this.itemSpawner = itemSpawner;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void update(float delta) {
        if (spearCooldown > 0) {
            spearCooldown -= delta;
        }

        stats.update(delta);

        if (buffManager != null) {
            buffManager.update(delta);
        }

        if (isJustHit) {
            hitFlashTimer -= delta;
            if (hitFlashTimer <= 0) {
                isJustHit = false;
            }
        }

        if (abilityManager != null) {
            abilityManager.update(delta);
            if (!inventory.isOpen() && !isPaused) {
                abilityManager.handleInput();
            }
        }

        setInvisible(isPlayerInSmokeBomb());

        if (!isPaused) {
            input(delta);
        }

        updateAnimationState();
        getAnimationManager().update(delta);

        for (int i = spearBodies.size - 1; i >= 0; i--) {
            Body spearBody = spearBodies.get(i);
            Vector2 startPosition = spearStartPositions.get(i);

            if (spearMarkedForRemoval.get(i) || spearBody == null ||
                    spearBody.getPosition().dst(startPosition) >= maxDistance) {
                removeSpear(spearBody, i);
            }
        }

        if (stats.isDead() && !playerDeath) {
            playerDie();
        }

        if(playerDeath && !world.getWorld().isLocked())
            die();

        for (int i = trails.size - 1; i >= 0; i--) {
            Trail trail = trails.get(i);
            trail.lifetime -= delta;
            if (trail.lifetime <= 0) {
                trails.removeIndex(i);
            }
        }

        inventory.update(delta, this, gameP);
    }

    private boolean isPlayerInSmokeBomb() {
        if (abilityManager == null) return false;

        for (AbilityVisual visual : abilityManager.getActiveVisuals()) {
            if (visual instanceof AbilityVisual.SmokeBombZone) {
                AbilityVisual.SmokeBombZone smoke = (AbilityVisual.SmokeBombZone) visual;
                if (smoke.isActive() && smoke.contains(getPosition())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateMaskBits() {
        if (body != null && body.getFixtureList().size > 0) {
            Fixture fixture = body.getFixtureList().first();
            Filter filter = fixture.getFilterData();

            if (invulnerable && !gameP.isInDungeon() && !gameP.isInBossRoom() && !gameP.isInEndlessRoom()) {
                switch (movementAbility) {
                    case "Charge":
                        filter.maskBits = notOriginalMaskBits;
                        break;
                    case "ShadowStep":
                    case "Vault":
                        filter.maskBits = 0;
                        break;
                    case "":
                    default:
                        filter.maskBits = originalMaskBits;
                        break;
                }
            } else if (invulnerable && (gameP.isInDungeon() || gameP.isInBossRoom() || gameP.isInEndlessRoom())) {
                switch (movementAbility) {
                    case "ShadowStep":
                    case "Vault":
                        filter.maskBits = vaultMaskBits;
                        break;
                    case "Charge":
                    case "":
                    default:
                        filter.maskBits = originalMaskBits;
                        break;
                }
            }
            else {
                filter.maskBits = originalMaskBits;
            }


            fixture.setFilterData(filter);
        }
    }

    public Vector2 getPosition() {
        if(body != null)
            return body.getPosition();
        else
            return null;
    }

    public void updateBounds() {}

    public void input(float delta) {
        if (isPaused) {
            body.setLinearVelocity(0, 0);
            return;
        }

        float moveX = 0;
        float moveY = 0;

        if (isVaulting) {
            vaultTimer -= delta;

            body.setLinearVelocity(vaultVelocity.x, vaultVelocity.y);

            trailTimer += delta;
            if (trailTimer >= trailSpawnInterval) {
                trails.add(new Trail(body.getPosition(), trailLifetime));
                trailTimer = 0f;
            }

            if (vaultTimer <= 0) {
                endVault();
            }

            return;
        }

        if (isCharging) {
            chargeTimer -= delta;

            body.setLinearVelocity(chargeVelocity.x, chargeVelocity.y);

            trailTimer += delta;
            if (trailTimer >= trailSpawnInterval) {
                trails.add(new Trail(body.getPosition(), trailLifetime));
                trailTimer = 0f;
            }

            if (chargeTimer <= 0) {
                endCharge();
            }

            return;
        }

        if (!isDashing && !playerDeath) {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;

            move(moveX, moveY, delta);
        } else {
            if (isDashing) {
                body.setLinearVelocity(dashDirection.x * dashSpeed * 25, dashDirection.y * dashSpeed * 25);
                dashTimer -= delta;

                trailTimer += delta;
                if (trailTimer >= trailSpawnInterval) {
                    trails.add(new Trail(body.getPosition(), trailLifetime));
                    trailTimer = 0f;
                }

                if (dashTimer <= 0) {
                    isDashing = false;
                    body.setLinearVelocity(0, 0);
                    setInvulnerable(false);
                }
            }
        }


        if (dashCooldownTimer > 0) {
            dashCooldownTimer -= delta;
        }

        updateBounds();
        gameP.generateChunksAroundPlayer();
    }

    private void updateAnimationState() {
        Vector3 mousePosition3D = gameP.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);
        Vector2 playerPosition = body.getPosition();

        isFlipped = mousePosition.x < playerPosition.x;

        float angle = (float) Math.toDegrees(Math.atan2(mousePosition.y - playerPosition.y, mousePosition.x - playerPosition.x));
        if (angle < 0) angle += 360;

        if (angle >= 45 && angle < 135) {
            direction = 1;
        } else if (angle >= 135 && angle < 225) {
            direction = 2;
        } else if (angle >= 225 && angle < 315) {
            direction = 0;
        } else {
            direction = 3;
        }
    }

    public void move(float dx, float dy, float delta) {
        float magnitude = (float) Math.sqrt(dx * dx + dy * dy);

        if (!playerDeath) {
            if (magnitude > 0) {
                dx /= magnitude;
                dy /= magnitude;

                if (Math.abs(dx) > Math.abs(dy)) {
                    direction = dx > 0 ? 3 : 2;
                } else {
                    direction = dy > 0 ? 1 : 0;
                }

                getAnimationManager().setState(AnimationManager.State.RUNNING, "Player");

                if (!gameP.isInDungeon() && !gameP.isInBossRoom() && !gameP.isInEndlessRoom()) {
                    SoundManager.getInstance().startGrassRunning();
                } else
                    SoundManager.getInstance().startStoneRunning();

                if(!gameStarted)
                    gameStarted = true;
            } else {
                getAnimationManager().setState(AnimationManager.State.IDLE, "Player");
                SoundManager.getInstance().stopGrassRunning();
                SoundManager.getInstance().stopStoneRunning();
            }
        }

        float velocityX = dx * speed * delta;
        float velocityY = dy * speed * delta;

        body.setLinearVelocity(velocityX, velocityY);
    }

    private void createSpear() {
        Vector3 mousePosition3D = gameP.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);
        Vector2 playerPosition = body.getPosition();

        float angle = (float) Math.atan2(mousePosition.y - playerPosition.y, mousePosition.x - playerPosition.x);
        float spearSpeed = 150f;

        Vector2 velocity = new Vector2((float) Math.cos(angle) * spearSpeed, (float) Math.sin(angle) * spearSpeed);

        BodyDef spearBodyDef = new BodyDef();
        spearBodyDef.type = BodyDef.BodyType.DynamicBody;
        spearBodyDef.position.set(playerPosition.x + velocity.x / spearSpeed, playerPosition.y + velocity.y / spearSpeed);
        spearBodyDef.angle = angle;
        spearBodyDef.bullet = true;

        Body spearBody = world.getWorld().createBody(spearBodyDef);
        spearBody.setFixedRotation(true);

        PolygonShape spearShape = new PolygonShape();
        spearShape.setAsBox(2f, 2f, new Vector2(0, 0), angle);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = spearShape;
        fixtureDef.density = 1f;
        fixtureDef.isSensor = false;
        fixtureDef.filter.categoryBits = CollisionFilter.SPEAR;
        fixtureDef.filter.maskBits = CollisionFilter.OBSTACLE | CollisionFilter.ENEMY;

        spearBody.createFixture(fixtureDef);
        spearShape.dispose();

        spearBody.setLinearVelocity(velocity);
        spearBody.setUserData(this);

        spearBodies.add(spearBody);
        spearVelocities.add(velocity);
        spearStartPositions.add(new Vector2(spearBody.getPosition()));

        spearMarkedForRemoval.add(false);
    }

    public void markSpearForRemoval(Body spearBody) {
        for (int i = 0; i < spearBodies.size; i++) {
            if (spearBodies.get(i) == spearBody) {
                spearMarkedForRemoval.set(i, true);
                break;
            }
        }
    }

    public void die() {
        gameStarted = false;
        body.setLinearVelocity(0, 0);

        if (!dyingAnimationStarted) {
            getAnimationManager().setState(AnimationManager.State.DYING, "Player");
            dyingAnimationStarted = true;
            return;
        }

        if (getAnimationManager().isAnimationFinished()) {
            playerDeath = false;
            dyingAnimationStarted = false;
            gameScreen.switchToNewState(GameScreen.POSTGAME);
        }
    }

    public void playerDie() {
        playerDeath = true;
        dyingAnimationStarted = false;
    }

    public void onTakeDamage() {
        isJustHit = true;
        hitFlashTimer = HIT_FLASH_DURATION;
    }

    public void cleanupSpears() {
        if (world != null && !world.getWorld().isLocked()) {
            for (int i = spearBodies.size - 1; i >= 0; i--) {
                Body spearBody = spearBodies.get(i);
                if (spearBody != null) {
                    try {
                        world.getWorld().destroyBody(spearBody);
                    } catch (Exception e) {
                        // Already destroyed
                    }
                }
            }
        }
        spearBodies.clear();
        spearVelocities.clear();
        spearStartPositions.clear();
        spearMarkedForRemoval.clear();
    }

    public void removeSpear(Body spearBody, int i) {
        if (spearBody != null && !world.getWorld().isLocked()) {
            try {
                world.getWorld().destroyBody(spearBody);
            } catch (Exception e) {
                // Body already destroyed or world locked
            }
        }

        spearBodies.removeIndex(i);
        spearVelocities.removeIndex(i);
        spearStartPositions.removeIndex(i);
        spearMarkedForRemoval.removeIndex(i);
    }

    public void addAbilityVisual(AbilityVisual visual) {
        if (abilityManager != null) {
            abilityManager.addAbilityVisual(visual);
        }
    }

    public void startCharge(Vector2 velocity, float duration) {
        isCharging = true;
        chargeTimer = duration;
        chargeVelocity = velocity;
        setInvulnerable(true);

        if (chargeHitEnemies == null) {
            chargeHitEnemies = new java.util.HashSet<>();
        } else {
            chargeHitEnemies.clear();
        }
    }

    public void endCharge() {
        isCharging = false;
        chargeTimer = 0f;
        body.setLinearVelocity(0, 0);
        setInvulnerable(false);
    }

    public boolean isCharging() {
        return isCharging;
    }

    public java.util.Set<Object> getChargeHitEnemies() {
        return chargeHitEnemies;
    }

    public void startVault(Vector2 velocity, float duration) {
        isVaulting = true;
        vaultTimer = duration;
        vaultVelocity = velocity;
    }

    public void endVault() {
        isVaulting = false;
        vaultTimer = 0f;
        body.setLinearVelocity(0, 0);
    }

    public boolean isVaulting() {
        return isVaulting;
    }

    public void setSprinting(boolean sprinting) {
        this.isSprinting = sprinting;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public void setHolyBlessingActive(boolean active) {
        this.isHolyBlessingActive = active;
    }

    public boolean isHolyBlessingActive() {
        return isHolyBlessingActive;
    }

    public void setBlazingFuryActive(boolean active) {
        this.isBlazingFuryActive = active;
    }

    public boolean isBlazingFuryActive() {
        return isBlazingFuryActive;
    }

    public void setHolySwordActive(boolean active, float coneMultiplier) {
        this.isHolySwordActive = active;
        this.holySwordConeMultiplier = coneMultiplier;
    }

    public boolean isHolySwordActive() {
        return isHolySwordActive;
    }

    public float getHolySwordConeMultiplier() {
        return holySwordConeMultiplier;
    }

    public void setLifeLeechActive(boolean active, int healAmount) {
        this.isLifeLeechActive = active;
        this.lifeLeechHealAmount = healAmount;
    }

    public boolean isLifeLeechActive() {
        return isLifeLeechActive;
    }

    public int getLifeLeechHealAmount() {
        return lifeLeechHealAmount;
    }

    public void onBasicAttackHit() {
        if (isLifeLeechActive && lifeLeechHealAmount > 0) {
            stats.heal(lifeLeechHealAmount);
        }
    }

    public void setPlayerBuff(String buff) {
        switch (buff) {
            case "Attack Potion":
                if (!playerBuffs[0]) {
                    playerBuffs[0] = true;
                    stats.addAndRemoveBuffs(buff, true);
                }
                break;
            case "Defense Potion":
                if (!playerBuffs[1]) {
                    playerBuffs[1] = true;
                    stats.addAndRemoveBuffs(buff, true);
                }
                break;
            case "Dex Potion":
                if (!playerBuffs[2]) {
                    playerBuffs[2] = true;
                    stats.addAndRemoveBuffs(buff, true);
                }
                break;
            case "Lucky Clover":
                if (!playerBuffs[3]) {
                    playerBuffs[3] = true;
                    stats.addAndRemoveBuffs(buff, true);
                }
                break;
        }
        buffManager.activateBuff(buff);
    }

    public void removePlayerBuff(String buff) {
        switch (buff) {
            case "Attack Potion":
                if (playerBuffs[0]) {
                    playerBuffs[0] = false;
                    stats.addAndRemoveBuffs(buff, false);
                }
                break;
            case "Defense Potion":
                if (playerBuffs[1]) {
                    playerBuffs[1] = false;
                    stats.addAndRemoveBuffs(buff, false);
                }
                break;
            case "Dex Potion":
                if (playerBuffs[2]) {
                    playerBuffs[2] = false;
                    stats.addAndRemoveBuffs(buff, false);
                }
                break;
            case "Lucky Clover":
                if (playerBuffs[3]) {
                    playerBuffs[3] = false;
                    stats.addAndRemoveBuffs(buff, false);
                }
                break;
        }
    }

    public boolean isPlayerFlipped() {
        return isFlipped;
    }

    public void render(SpriteBatch batch, int TILE_SIZE) {
        Vector2 position = body.getPosition();

        getAnimationManager().update(Gdx.graphics.getDeltaTime());

        TextureRegion currentFrame = getAnimationManager().getCurrentFrame();
        TextureRegion frame = new TextureRegion(currentFrame);

        if (isFlipped) {
            if (!frame.isFlipX()) {
                frame.flip(true, false);
            }
        } else {
            if (frame.isFlipX()) {
                frame.flip(true, false);
            }
        }

        if (isJustHit) {
            float flashIntensity = hitFlashTimer / HIT_FLASH_DURATION;
            batch.setColor(1f, 1f - flashIntensity * 0.5f, 1f - flashIntensity * 0.5f, 1f);
        }

        if (isJustHit) {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        // Render projectile spears (these are kept for future projectile functionality)
        if (playerClass == PlayerClass.MERCENARY) {
            for (int i = 0; i < spearBodies.size; i++) {
                Body spearBody = spearBodies.get(i);

                if (spearBody != null) {
                    Texture spearTexture = Storage.assetManager.get("icons/gear/ironSpear.png");
                    TextureRegion spearRegion = new TextureRegion(spearTexture);

                    float rotationAngle = (float) Math.toDegrees(spearBody.getAngle());
                    float posX = spearBody.getPosition().x - spearTexture.getWidth() / 8f;
                    float posY = spearBody.getPosition().y - spearTexture.getHeight() / 8f;

                    batch.draw(spearRegion,
                            posX, posY,
                            spearTexture.getWidth() / 8f, spearTexture.getHeight() / 8f,
                            spearTexture.getWidth() / 6f, spearTexture.getHeight() / 6f,
                            1, 1,
                            rotationAngle - 45);
                }
            }
        }

        if (isJustHit) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(frame,
                    position.x - TILE_SIZE / 4f,
                    position.y - TILE_SIZE / 4f,
                    TILE_SIZE / 2f, TILE_SIZE / 2f);
            batch.setColor(1f, 0.5f, 0.5f, 0.8f);
        }
        else {
            batch.draw(frame,
                    position.x - TILE_SIZE / 4f,
                    position.y - TILE_SIZE / 4f,
                    TILE_SIZE / 2f, TILE_SIZE / 2f);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        if (abilityManager != null) {
            abilityManager.renderAbilityEffects(batch);
        }

        for (Trail trail : trails) {
            float alpha = trail.lifetime / trailLifetime;
            batch.setColor(1, 1, 1, alpha);
            batch.draw(getAnimationManager().getCurrentFrame(),
                    trail.position.x - TILE_SIZE / 4f,
                    trail.position.y - TILE_SIZE / 4f,
                    TILE_SIZE / 2f, TILE_SIZE / 2f);
        }
        batch.setColor(1, 1, 1, 1);

    }

    public void renderSkillBar(SpriteBatch batch) {
        if (abilityManager != null) {
            abilityManager.renderSkillBar(batch);
        }
    }

    public void renderAbilityEffects(SpriteBatch batch) {
        if (abilityManager != null) {
            abilityManager.renderAbilityEffects(batch);
        }
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public Body getBody() {
        return body;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
        updateMaskBits();
    }

    public void addSpearBodies(Body spearBody, Vector2 velocity) {
        spearBodies.add(spearBody);
        spearVelocities.add(velocity);
        spearStartPositions.add(new Vector2(spearBody.getPosition()));
        spearMarkedForRemoval.add(false);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
    public int getLevel() {
        return stats.getLevel();
    }
    public BuffManager getBuffManager() {
        return buffManager;
    }
    public void setMovementAbility(String movementAbility) {
        this.movementAbility = movementAbility;
    }

    public boolean isInvisible() { return isInvisible; }
    public void setInvisible(boolean invisible) {
        this.isInvisible = invisible;
    }
}