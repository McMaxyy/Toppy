package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import config.Storage;
import entities.*;
import game.GameProj;
import managers.Chunk;

import java.util.ArrayList;

/**
 * All Mercenary class abilities in one file.
 */
public abstract class MercenaryAbilities {}

class ChargeAbility extends Ability {
    private static final float CHARGE_SPEED = 3000f; // Match Player dash speed
    private static final float CHARGE_DURATION = 0.5f; // Match Player dash duration
    private static final float STUN_DURATION = 1.5f;

    // Charge state
    private Player chargingPlayer;
    private GameProj currentGameProj;
    private AbilityVisual.ChargeTrail trailVisual;

    public ChargeAbility(Texture iconTexture) {
        super(
                "Charge",
                "Dash forward, dealing damage and stunning enemies on impact",
                1.0f,  // cooldown
                50,    // damage
                0f,    // cast time
                CHARGE_DURATION,    // duration
                30f,   // distance (hit radius)
                AbilityType.CROWD_CONTROL,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        player.setInvulnerable(true);
        Vector2 movementDirection = getPlayerMovementDirection();

        // If no movement keys are pressed, dash towards mouse instead
        if (movementDirection.isZero()) {
            Vector3 mousePosition3D = gameProj.getCamera().unproject(
                    new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
            );
            Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);
            Vector2 playerPosition = player.getPosition();

            movementDirection = new Vector2(
                    mousePosition.x - playerPosition.x,
                    mousePosition.y - playerPosition.y
            ).nor();
        }

        // Calculate velocity EXACTLY like Player dash
        Vector2 chargeVelocity = new Vector2(
                movementDirection.x * CHARGE_SPEED * 25,
                movementDirection.y * CHARGE_SPEED * 25
        );

        // Start charging using Player's charge system
        player.startCharge(chargeVelocity, CHARGE_DURATION);
        chargingPlayer = player;
        currentGameProj = gameProj;

        // Create trail visual
        trailVisual = new AbilityVisual.ChargeTrail(player, CHARGE_DURATION + 0.5f);
        player.addAbilityVisual(trailVisual);

        new Thread(() -> {
            try {
                Thread.sleep((long)(duration * 2 * 1000));

                Gdx.app.postRunnable(() -> {
                    player.setInvulnerable(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (chargingPlayer != null && chargingPlayer.isCharging()) {
            checkForEnemyHits();
        } else if (chargingPlayer != null && !chargingPlayer.isCharging()) {
            chargingPlayer = null;
            currentGameProj = null;
        }
    }

    /**
     * Get the player's current movement direction based on input keys
     * This mimics the Player.input() method
     */
    private Vector2 getPlayerMovementDirection() {
        Vector2 direction = new Vector2(0, 0);

//        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) direction.y += 1;
//        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) direction.y -= 1;
//        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) direction.x -= 1;
//        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) direction.x += 1;

        // Normalize to prevent diagonal movement being faster
        if (!direction.isZero()) {
            direction.nor();
        }

        return direction;
    }

    private void checkForEnemyHits() {
        if (chargingPlayer == null || currentGameProj == null) return;

        Vector2 playerPos = chargingPlayer.getPosition();
        float hitRadius = distance;

        // Get the set of already hit enemies from the player
        java.util.Set<Object> hitEnemies = chargingPlayer.getChargeHitEnemies();

        // Check overworld enemies
        for (Chunk chunk : currentGameProj.getChunks().values()) {
            // Regular enemies
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null && !hitEnemies.contains(enemy)) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < hitRadius) {
                        enemy.takeDamage(damage);
                        StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                        stun.onApply();
                        currentGameProj.addStatusEffect(enemy, stun);
                        hitEnemies.add(enemy);
                    }
                }
            }

            // Boss enemies
            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null && !hitEnemies.contains(boss)) {
                    float dist = playerPos.dst(boss.getBody().getPosition());
                    if (dist < hitRadius) {
                        boss.takeDamage(damage);
                        StunEffect stun = new StunEffect(boss, STUN_DURATION);
                        stun.onApply();
                        currentGameProj.addStatusEffect(boss, stun);
                        hitEnemies.add(boss);
                    }
                }
            }
        }

        // Check dungeon enemies
        if (currentGameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentGameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null && !hitEnemies.contains(enemy)) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < hitRadius) {
                        enemy.takeDamage(damage);
                        StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                        stun.onApply();
                        currentGameProj.addStatusEffect(enemy, stun);
                        hitEnemies.add(enemy);
                    }
                }
            }
        }
    }
    public boolean isCharging() {
        return chargingPlayer != null && chargingPlayer.isCharging();
    }
}

/**
 * Double Swing - Attack twice in rapid succession
 * Visual: Swinging white conal indicator
 */
class DoubleSwingAbility extends Ability {
    private AbilityVisual.ConalAttack firstSwing;
    private AbilityVisual.ConalAttack secondSwing;
    private boolean firstSwingComplete = false;

    public DoubleSwingAbility(Texture iconTexture) {
        super(
                "Double Swing",
                "Strike twice, each hit dealing 90% weapon damage",
                1.0f,  // cooldown
                0,     // damage (based on player weapon)
                0f,    // cast time
                0f,    // duration
                50f,   // distance (attack range)
                AbilityType.DAMAGE,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        int playerDamage = player.getStats().getTotalDamage();
        int swingDamage = (int)(playerDamage * 0.9f);

        // Create first swinging visual
        firstSwing = AbilityVisual.ConalAttack.createSwingingWhite(player, gameProj, 0.3f, distance, 1);
        player.addAbilityVisual(firstSwing);

        // Deal first swing damage immediately
        dealDamageInArea(player, swingDamage, gameProj);

        // Schedule second swing after a delay
        new Thread(() -> {
            try {
                Thread.sleep(300);

                Gdx.app.postRunnable(() -> {
                    if (player != null) {
                        secondSwing = AbilityVisual.ConalAttack.createSwingingWhite(player, gameProj, 0.3f, distance, 2);
                        player.addAbilityVisual(secondSwing);

                        dealDamageInArea(player, swingDamage, gameProj);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void dealDamageInArea(Player player, int damage, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();

        Vector3 mousePos3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
        Vector2 attackDir = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();

        for (Chunk chunk : gameProj.getChunks().values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(damage);
                    }
                }
            }

            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    Vector2 enemyPos = boss.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        boss.takeDamage(damage);
                    }
                }
            }
        }

        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(damage);
                    }
                }
            }
        }
    }
}

/**
 * Bubble - Protective shield that blocks all damage
 * Visual: Bubble texture overlay with physics body
 */
class BubbleAbility extends Ability {

    public BubbleAbility(Texture iconTexture) {
        super(
                "Bubble",
                "Gain a shield that blocks all damage for 2 seconds",
                12.0f, // cooldown
                0,     // damage
                0f,    // cast time
                2f,    // duration
                0f,    // distance (not used)
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        player.setInvulnerable(true);

        // Create bubble visual with protective body
        AbilityVisual.Bubble bubbleVisual = new AbilityVisual.Bubble(
                player, player.getBody().getWorld(), 2.0f
        );
        player.addAbilityVisual(bubbleVisual);

        BubbleShieldEffect shield = new BubbleShieldEffect(player, 2.0f);
        shield.onApply();
        gameProj.addStatusEffect(player, shield);

        new Thread(() -> {
            try {
                Thread.sleep((long)(duration * 1000));

                // Run on main thread to avoid concurrency issues
                Gdx.app.postRunnable(() -> {
                    player.setInvulnerable(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

/**
 * Rend - Apply bleeding to ALL enemies in the cone
 * Visual: Red conal indicator
 */
class RendAbility extends Ability {
    private static final float BLEED_DURATION = 5.0f;
    private static final int BLEED_DAMAGE_PER_TICK = 10;

    public RendAbility(Texture iconTexture) {
        super(
                "Rend",
                "Inflict bleeding wounds on all enemies in a cone, dealing damage over time",
                10.0f, // cooldown
                0,    // initial damage
                0f,    // cast time
                BLEED_DURATION, // duration
                50f,   // distance (attack range)
                AbilityType.DEBUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();
        int enemiesHit = 0;

        // Create RED conal attack indicator with ability's distance
        AbilityVisual.ConalAttack indicator = AbilityVisual.ConalAttack.createRed(player, gameProj, 0.4f, distance);
        player.addAbilityVisual(indicator);

        Vector3 mousePos3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
        Vector2 attackDir = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();

        // Hit ALL enemies in overworld chunks
        for (Chunk chunk : gameProj.getChunks().values()) {
            // Hit regular enemies
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float dist = toEnemy.len();

                    // Check if enemy is within range and in the cone
                    if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(damage);

                        // Apply bleed effect
                        BleedEffect bleed = new BleedEffect(enemy, BLEED_DURATION, BLEED_DAMAGE_PER_TICK);
                        bleed.onApply();
                        gameProj.addStatusEffect(enemy, bleed);

                        enemiesHit++;
                    }
                }
            }

            // Hit boss enemies
            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    Vector2 enemyPos = boss.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float dist = toEnemy.len();

                    if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        boss.takeDamage(damage);

                        // Apply bleed effect to boss
                        BleedEffect bleed = new BleedEffect(boss, BLEED_DURATION, BLEED_DAMAGE_PER_TICK);
                        bleed.onApply();
                        gameProj.addStatusEffect(boss, bleed);

                        enemiesHit++;
                    }
                }
            }
        }

        // Hit ALL enemies in dungeon
        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float dist = toEnemy.len();

                    if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(damage);

                        // Apply bleed effect
                        BleedEffect bleed = new BleedEffect(enemy, BLEED_DURATION, BLEED_DAMAGE_PER_TICK);
                        bleed.onApply();
                        gameProj.addStatusEffect(enemy, bleed);

                        enemiesHit++;
                    }
                }
            }
        }

        if (enemiesHit > 0) {
            System.out.println("Rend applied to " + enemiesHit + " enemies! Bleeding for " + BLEED_DURATION + " seconds");
        } else {
            System.out.println("Rend missed!");
        }
    }
}

/**
 * Prayer - Heal the player after a cast time
 * Visual: Prayer texture overlay during cast
 */
class PrayerAbility extends Ability {
    private Player targetPlayer;
    private AbilityVisual.Prayer prayerVisual;
    private static final int HEAL_AMOUNT = 80;

    public PrayerAbility(Texture iconTexture) {
        super(
                "Prayer",
                "Channel divine energy to restore 80 health",
                15.0f, // cooldown
                0,     // damage
                1.0f,  // cast time
                0f,    // duration
                0f,    // distance (not used)
                AbilityType.HEALING,
                iconTexture
        );
    }

    @Override
    protected void onCastStart(Player player, GameProj gameProj) {
        super.onCastStart(player, gameProj);
        this.targetPlayer = player;

        // Create prayer visual during cast
        prayerVisual = new AbilityVisual.Prayer(player, castTime);
        player.addAbilityVisual(prayerVisual);

        System.out.println("Praying for healing...");
    }

    @Override
    protected void onCastComplete() {
        super.onCastComplete();
        execute(targetPlayer, null);
        currentCooldown = cooldown;

        if (prayerVisual != null) {
            prayerVisual.dispose();
            prayerVisual = null;
        }
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        player.getStats().heal(HEAL_AMOUNT);
    }

    @Override
    public void cancelCast() {
        super.cancelCast();
        if (prayerVisual != null) {
            prayerVisual.dispose();
            prayerVisual = null;
        }
    }
}

/**
 * Blink - Teleport to mouse position
 * Visual: White dissipating circle at destination
 */
class BlinkAbility extends Ability {

    public BlinkAbility(Texture iconTexture) {
        super(
                "Blink",
                "Teleport a distance towards the mouse position",
                15.0f, // cooldown
                0,     // damage
                0f,    // cast time
                0f,    // duration
                100f,  // distance (teleport range)
                AbilityType.CROWD_CONTROL,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        Vector3 mousePosition3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);

        // Teleport the player
        player.getBody().setTransform(mousePosition, 0f);

        // Create blink visual at destination
        AbilityVisual.Blink blinkVisual = new AbilityVisual.Blink(mousePosition);
        player.addAbilityVisual(blinkVisual);

        System.out.println("Blink activated!");
    }
}