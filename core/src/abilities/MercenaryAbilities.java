package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import entities.*;
import game.GameProj;
import managers.Chunk;

import java.util.ArrayList;

/**
 * All Mercenary class abilities in one file.
 */
public abstract class MercenaryAbilities {}

/**
 * Charge - Dash in a direction and stun/damage enemies on impact
 * (No visual indicator as requested)
 */
class ChargeAbility extends Ability {
    private static final float CHARGE_DISTANCE = 100f;
    private static final float CHARGE_SPEED = 8000f;
    private static final float STUN_DURATION = 1.5f;

    public ChargeAbility(Texture iconTexture) {
        super(
                "Charge",
                "Dash forward, dealing damage and stunning enemies on impact",
                8.0f,  // cooldown
                50,    // damage
                0f,    // cast time
                1f,    // duration
                30f,   // distance (hit radius)
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
        Vector2 playerPosition = player.getPosition();

        Vector2 direction = new Vector2(
                mousePosition.x - playerPosition.x,
                mousePosition.y - playerPosition.y
        ).nor();

        player.setInvulnerable(true);
        performCharge(player, direction, gameProj);
    }

    private void performCharge(Player player, Vector2 direction, GameProj gameProj) {
        player.getBody().setLinearVelocity(direction.x * CHARGE_SPEED, direction.y * CHARGE_SPEED);

        new Thread(() -> {
            try {
                Thread.sleep(200);
                player.getBody().setLinearVelocity(0, 0);
                player.setInvulnerable(false);
                checkForEnemyHits(player, gameProj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Charge activated!");
    }

    private void checkForEnemyHits(Player player, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();
        float hitRadius = 30f;

        for (Chunk chunk : gameProj.getChunks().values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    float distance = playerPos.dst(enemy.getBody().getPosition());
                    if (distance < hitRadius) {
                        enemy.takeDamage(damage);
                        StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                        stun.onApply();
                        gameProj.addStatusEffect(enemy, stun);
                    }
                }
            }

            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    float distance = playerPos.dst(boss.getBody().getPosition());
                    if (distance < hitRadius) {
                        boss.takeDamage(damage);
                        StunEffect stun = new StunEffect(boss, STUN_DURATION);
                        stun.onApply();
                        gameProj.addStatusEffect(boss, stun);
                    }
                }
            }
        }

        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float distance = playerPos.dst(enemy.getBody().getPosition());
                    if (distance < hitRadius) {
                        enemy.takeDamage(damage);
                        StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                        stun.onApply();
                        gameProj.addStatusEffect(enemy, stun);
                    }
                }
            }
        }
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