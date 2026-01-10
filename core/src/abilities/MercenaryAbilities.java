package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import entities.*;
import game.GameProj;
import managers.Chunk;

import java.util.ArrayList;
import java.util.List;

/**
 * All Mercenary class abilities in one file.
 * Only ChargeAbility is public, rest are package-private.
 */

/**
 * Charge - Dash in a direction and stun/damage enemies on impact
 */
public abstract class MercenaryAbilities {}

class ChargeAbility extends Ability {
    private static final float CHARGE_DISTANCE = 100f;
    private static final float CHARGE_SPEED = 8000f;
    private static final float STUN_DURATION = 1.5f;
    public ChargeAbility(Texture iconTexture) {
        super(
                "Charge",
                "Dash forward, dealing damage and stunning enemies on impact",
                8.0f,  // 8 second cooldown
                50,    // 50 damage
                0f,    // Instant cast
                1f,    // Duration not used for this ability
                AbilityType.CROWD_CONTROL,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        // Get direction towards mouse
        Vector3 mousePosition3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);
        Vector2 playerPosition = player.getPosition();

        Vector2 direction = new Vector2(
                mousePosition.x - playerPosition.x,
                mousePosition.y - playerPosition.y
        ).nor();

        // Make player invulnerable during charge
        player.setInvulnerable(true);

        // Perform the charge
        performCharge(player, direction, gameProj);
    }

    private void performCharge(Player player, Vector2 direction, GameProj gameProj) {
        Vector2 startPos = player.getPosition().cpy();
        Vector2 targetPos = startPos.cpy().add(direction.cpy().scl(CHARGE_DISTANCE));

        // Apply velocity
        player.getBody().setLinearVelocity(direction.x * CHARGE_SPEED, direction.y * CHARGE_SPEED);

        // Schedule stopping the charge
        new Thread(() -> {
            try {
                Thread.sleep(200); // Charge lasts 0.2 seconds
                player.getBody().setLinearVelocity(0, 0);
                player.setInvulnerable(false);

                // Check for enemy hits
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

        // Check enemies in chunks
        for (Chunk chunk : gameProj.getChunks().values()) {
            // Check normal enemies
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    float distance = playerPos.dst(enemy.getBody().getPosition());
                    if (distance < hitRadius) {
                        enemy.takeDamage(damage);
                        // Apply stun
                        StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                        stun.onApply();
                        gameProj.addStatusEffect(enemy, stun);
                    }
                }
            }

            // Check boss enemies
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

        // Check dungeon enemies
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
 */
class DoubleSwingAbility extends Ability {
    public DoubleSwingAbility(Texture iconTexture) {
        super(
                "Double Swing",
                "Strike twice, each hit dealing 90% weapon damage",
                6.0f,  // 6 second cooldown
                0,     // Damage is based on player weapon
                0f,    // Instant cast
                0f,
                AbilityType.DAMAGE,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        int playerDamage = player.getStats().getTotalDamage();
        int swingDamage = (int)(playerDamage * 0.9f);

        // First swing
        dealDamageInArea(player, swingDamage, gameProj);

        // Second swing after 0.3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(300);
                dealDamageInArea(player, swingDamage, gameProj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Double Swing activated!");
    }

    private void dealDamageInArea(Player player, int damage, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();
        float attackRange = 25f;

        // Get mouse direction for attack direction
        Vector3 mousePos3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
        Vector2 attackDir = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();

        // Check enemies in chunks
        for (Chunk chunk : gameProj.getChunks().values()) {
            // Check normal enemies
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    // Check if in range and in front of player
                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(damage);
                    }
                }
            }

            // Check bosses
            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    Vector2 enemyPos = boss.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        boss.takeDamage(damage);
                    }
                }
            }
        }

        // Check dungeon enemies
        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(damage);
                    }
                }
            }
        }
    }
}

/**
 * Bubble - Protective shield that blocks all damage
 */
class BubbleAbility extends Ability {
    public BubbleAbility(Texture iconTexture) {
        super(
                "Bubble",
                "Gain a shield that blocks all damage for 2 seconds",
                12.0f, // 12 second cooldown
                0,     // No damage
                0f,    // Instant cast
                2f,    // 2 second duration
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        BubbleShieldEffect shield = new BubbleShieldEffect(player, 2.0f);
        shield.onApply();
        gameProj.addStatusEffect(player, shield);

        System.out.println("Bubble shield activated!");
    }
}

/**
 * Rend - Apply bleeding to an enemy
 */
class RendAbility extends Ability {
    private static final float BLEED_DURATION = 5.0f; // FIXED: Changed from 6.0f to 5.0f to match the ability description
    private static final int BLEED_DAMAGE_PER_TICK = 10;

    public RendAbility(Texture iconTexture) {
        super(
                "Rend",
                "Inflict a bleeding wound that deals damage over time",
                10.0f, // 10 second cooldown
                30,    // Initial damage
                0f,    // Instant cast
                BLEED_DURATION,  // Duration for the bleed effect
                AbilityType.DEBUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();
        float attackRange = 25f;
        boolean hitEnemy = false;

        // Get mouse direction
        Vector3 mousePos3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
        Vector2 attackDir = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();

        // Find closest enemy in direction
        Object closestEnemy = null;
        float closestDistance = Float.MAX_VALUE;

        // Check enemies in chunks
        for (Chunk chunk : gameProj.getChunks().values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float distance = toEnemy.len();

                    if (distance < attackRange && distance < closestDistance &&
                            toEnemy.nor().dot(attackDir) > 0.5f) {
                        closestEnemy = enemy;
                        closestDistance = distance;
                    }
                }
            }

            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    Vector2 enemyPos = boss.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float distance = toEnemy.len();

                    if (distance < attackRange && distance < closestDistance &&
                            toEnemy.nor().dot(attackDir) > 0.5f) {
                        closestEnemy = boss;
                        closestDistance = distance;
                    }
                }
            }
        }

        // Check dungeon enemies
        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float distance = toEnemy.len();

                    if (distance < attackRange && distance < closestDistance &&
                            toEnemy.nor().dot(attackDir) > 0.5f) {
                        closestEnemy = enemy;
                        closestDistance = distance;
                    }
                }
            }
        }

        // Apply damage and bleed to closest enemy
        if (closestEnemy != null) {
            if (closestEnemy instanceof Enemy) {
                ((Enemy) closestEnemy).takeDamage(damage);
            } else if (closestEnemy instanceof BossKitty) {
                ((BossKitty) closestEnemy).takeDamage(damage);
            } else if (closestEnemy instanceof DungeonEnemy) {
                ((DungeonEnemy) closestEnemy).takeDamage(damage);
            }

            BleedEffect bleed = new BleedEffect(closestEnemy, BLEED_DURATION, BLEED_DAMAGE_PER_TICK);
            bleed.onApply();
            gameProj.addStatusEffect(closestEnemy, bleed);

            hitEnemy = true;
        }

        if (hitEnemy) {
            System.out.println("Rend applied! Bleeding for " + BLEED_DURATION + " seconds");
        } else {
            System.out.println("Rend missed!");
        }
    }
}

/**
 * Prayer - Heal the player after a cast time
 */
class PrayerAbility extends Ability {
    private Player targetPlayer;
    private static final int HEAL_AMOUNT = 80;

    public PrayerAbility(Texture iconTexture) {
        super(
                "Prayer",
                "Channel divine energy to restore 80 health",
                15.0f, // 15 second cooldown
                0,     // No damage
                1.0f,  // 1 second cast time
                0f,
                AbilityType.HEALING,
                iconTexture
        );
    }

    @Override
    protected void onCastStart(Player player, GameProj gameProj) {
        super.onCastStart(player, gameProj);
        this.targetPlayer = player;
        System.out.println("Praying for healing...");
    }

    @Override
    protected void onCastComplete() {
        super.onCastComplete();
        execute(targetPlayer, null);
        currentCooldown = cooldown;
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        player.getStats().heal(HEAL_AMOUNT);
        System.out.println("Prayer healed for " + HEAL_AMOUNT + " HP!");
    }
}