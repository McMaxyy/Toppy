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
import java.util.HashSet;
import java.util.Set;

/**
 * All Paladin class abilities in one file.
 */
public abstract class PaladinAbilities {}

/**
 * Blink - Teleport to mouse position (copied from MercenaryAbilities)
 * Visual: White dissipating circle at destination
 */
class PaladinBlinkAbility extends Ability {

    public PaladinBlinkAbility(Texture iconTexture) {
        super(
                "Blink",
                "Teleport a distance towards the mouse position",
                15.0f,
                0,
                0f,
                0f,
                100f,
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

        player.getBody().setTransform(mousePosition, 0f);

        AbilityVisual.Blink blinkVisual = new AbilityVisual.Blink(mousePosition);
        player.addAbilityVisual(blinkVisual);
    }
}

/**
 * Bubble - Protective shield that blocks all damage (copied from MercenaryAbilities)
 * Visual: Bubble texture overlay with physics body
 */
class PaladinBubbleAbility extends Ability {

    public PaladinBubbleAbility(Texture iconTexture) {
        super(
                "Bubble",
                "Gain a shield that blocks all damage for 2 seconds",
                12.0f,
                0,
                0f,
                2f,
                0f,
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        player.setInvulnerable(true);

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
 * Pull - Creates a visual circle that shrinks, pulling enemies toward the player
 * Visual: Expanding then contracting circle
 */
class PullAbility extends Ability {
    private static final float PULL_RADIUS = 100f;
    private static final float PULL_DURATION = 0.5f;

    private Player pullingPlayer;
    private GameProj currentGameProj;
    private AbilityVisual.PullCircle pullVisual;
    private Set<Object> affectedEnemies;
    private float pullTimer;
    private boolean isPulling;

    public PullAbility(Texture iconTexture) {
        super(
                "Pull",
                "Create a divine vortex that pulls all nearby enemies toward you",
                10.0f,
                20,
                0f,
                PULL_DURATION,
                PULL_RADIUS,
                AbilityType.CROWD_CONTROL,
                iconTexture
        );
        this.affectedEnemies = new HashSet<>();
        this.isPulling = false;
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        pullingPlayer = player;
        currentGameProj = gameProj;
        affectedEnemies.clear();
        pullTimer = 0f;
        isPulling = true;

        pullVisual = new AbilityVisual.PullCircle(player, PULL_RADIUS, PULL_DURATION);
        player.addAbilityVisual(pullVisual);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (isPulling && pullingPlayer != null && currentGameProj != null) {
            pullTimer += delta;

            if (pullTimer < PULL_DURATION) {
                pullEnemies(delta);
            } else {
                isPulling = false;
                pullingPlayer = null;
                currentGameProj = null;
                affectedEnemies.clear();
            }
        }
    }

    private void pullEnemies(float delta) {
        if (pullingPlayer == null || currentGameProj == null) return;

        Vector2 playerPos = pullingPlayer.getPosition();

        // Calculate progress (0 to 1)
        float progress = pullTimer / PULL_DURATION;

        // Pull overworld enemies
        for (Chunk chunk : currentGameProj.getChunks().values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    float dist = playerPos.dst(enemyPos);

                    if (dist < PULL_RADIUS) {
                        // Calculate direction from enemy to player
                        Vector2 direction = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();

                        // Calculate target position (player pos + 10 units away)
                        Vector2 targetPos = new Vector2(
                                playerPos.x - direction.x * 10f,
                                playerPos.y - direction.y * 10f
                        );

                        // Lerp enemy position toward target based on progress
                        Vector2 newPos = new Vector2(
                                enemyPos.x + (targetPos.x - enemyPos.x) * progress * 0.1f,
                                enemyPos.y + (targetPos.y - enemyPos.y) * progress * 0.1f
                        );

                        enemy.getBody().setTransform(newPos, enemy.getBody().getAngle());
                        enemy.getBody().setLinearVelocity(0, 0);

                        if (!affectedEnemies.contains(enemy)) {
                            enemy.takeDamage(damage);
                            affectedEnemies.add(enemy);
                        }
                    }
                }
            }

            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    Vector2 enemyPos = boss.getBody().getPosition();
                    float dist = playerPos.dst(enemyPos);

                    if (dist < PULL_RADIUS) {
                        Vector2 direction = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();
                        Vector2 targetPos = new Vector2(
                                playerPos.x - direction.x * 15f,
                                playerPos.y - direction.y * 15f
                        );

                        Vector2 newPos = new Vector2(
                                enemyPos.x + (targetPos.x - enemyPos.x) * progress * 0.05f,
                                enemyPos.y + (targetPos.y - enemyPos.y) * progress * 0.05f
                        );

                        boss.getBody().setTransform(newPos, boss.getBody().getAngle());
                        boss.getBody().setLinearVelocity(0, 0);

                        if (!affectedEnemies.contains(boss)) {
                            boss.takeDamage(damage);
                            affectedEnemies.add(boss);
                        }
                    }
                }
            }

            for (Cyclops cyclops : new ArrayList<>(chunk.getCyclopsList())) {
                if (cyclops.getBody() != null) {
                    Vector2 enemyPos = cyclops.getBody().getPosition();
                    float dist = playerPos.dst(enemyPos);

                    if (dist < PULL_RADIUS) {
                        Vector2 direction = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();
                        Vector2 targetPos = new Vector2(
                                playerPos.x - direction.x * 20f,
                                playerPos.y - direction.y * 20f
                        );

                        Vector2 newPos = new Vector2(
                                enemyPos.x + (targetPos.x - enemyPos.x) * progress * 0.03f,
                                enemyPos.y + (targetPos.y - enemyPos.y) * progress * 0.03f
                        );

                        cyclops.getBody().setTransform(newPos, cyclops.getBody().getAngle());
                        cyclops.getBody().setLinearVelocity(0, 0);

                        if (!affectedEnemies.contains(cyclops)) {
                            cyclops.takeDamage(damage);
                            affectedEnemies.add(cyclops);
                        }
                    }
                }
            }
        }

        // Pull dungeon enemies
        if (currentGameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentGameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    float dist = playerPos.dst(enemyPos);

                    if (dist < PULL_RADIUS) {
                        Vector2 direction = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();
                        Vector2 targetPos = new Vector2(
                                playerPos.x - direction.x * 10f,
                                playerPos.y - direction.y * 10f
                        );

                        Vector2 newPos = new Vector2(
                                enemyPos.x + (targetPos.x - enemyPos.x) * progress * 0.1f,
                                enemyPos.y + (targetPos.y - enemyPos.y) * progress * 0.1f
                        );

                        enemy.getBody().setTransform(newPos, enemy.getBody().getAngle());
                        enemy.getBody().setLinearVelocity(0, 0);

                        if (!affectedEnemies.contains(enemy)) {
                            enemy.takeDamage(damage);
                            affectedEnemies.add(enemy);
                        }
                    }
                }
            }
        }

        // Pull boss room bosses
        if (currentGameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = currentGameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null) {
                Vector2 enemyPos = bossRoomBoss.getBody().getPosition();
                float dist = playerPos.dst(enemyPos);

                if (dist < PULL_RADIUS) {
                    Vector2 direction = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();
                    Vector2 targetPos = new Vector2(
                            playerPos.x - direction.x * 15f,
                            playerPos.y - direction.y * 15f
                    );

                    Vector2 newPos = new Vector2(
                            enemyPos.x + (targetPos.x - enemyPos.x) * progress * 0.05f,
                            enemyPos.y + (targetPos.y - enemyPos.y) * progress * 0.05f
                    );

                    bossRoomBoss.getBody().setTransform(newPos, bossRoomBoss.getBody().getAngle());
                    bossRoomBoss.getBody().setLinearVelocity(0, 0);

                    if (!affectedEnemies.contains(bossRoomBoss)) {
                        bossRoomBoss.takeDamage(damage);
                        affectedEnemies.add(bossRoomBoss);
                    }
                }
            }

            Cyclops cyclopsRoomBoss = currentGameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                Vector2 enemyPos = cyclopsRoomBoss.getBody().getPosition();
                float dist = playerPos.dst(enemyPos);

                if (dist < PULL_RADIUS) {
                    Vector2 direction = new Vector2(playerPos.x - enemyPos.x, playerPos.y - enemyPos.y).nor();
                    Vector2 targetPos = new Vector2(
                            playerPos.x - direction.x * 20f,
                            playerPos.y - direction.y * 20f
                    );

                    Vector2 newPos = new Vector2(
                            enemyPos.x + (targetPos.x - enemyPos.x) * progress * 0.03f,
                            enemyPos.y + (targetPos.y - enemyPos.y) * progress * 0.03f
                    );

                    cyclopsRoomBoss.getBody().setTransform(newPos, cyclopsRoomBoss.getBody().getAngle());
                    cyclopsRoomBoss.getBody().setLinearVelocity(0, 0);

                    if (!affectedEnemies.contains(cyclopsRoomBoss)) {
                        cyclopsRoomBoss.takeDamage(damage);
                        affectedEnemies.add(cyclopsRoomBoss);
                    }
                }
            }
        }
    }

    public boolean isPulling() {
        return isPulling;
    }
}

/**
 * Smite - AOE damage circle around the player
 * Visual: Smite texture overlay
 */
class SmiteAbility extends Ability {
    private static final float SMITE_RADIUS = 50f;
    private static final int SMITE_DAMAGE = 80;

    public SmiteAbility(Texture iconTexture) {
        super(
                "Smite",
                "Call down divine judgment, damaging all enemies around you",
                8.0f,
                SMITE_DAMAGE,
                0f,
                0.5f,
                SMITE_RADIUS,
                AbilityType.DAMAGE,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();

        // Create visual effect
        AbilityVisual.Smite smiteVisual = new AbilityVisual.Smite(player, SMITE_RADIUS, 0.5f);
        player.addAbilityVisual(smiteVisual);

        int enemiesHit = 0;

        // Damage overworld enemies
        for (Chunk chunk : gameProj.getChunks().values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        enemy.takeDamage(damage);
                        enemiesHit++;
                    }
                }
            }

            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    float dist = playerPos.dst(boss.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        boss.takeDamage(damage);
                        enemiesHit++;
                    }
                }
            }

            for (Cyclops cyclops : new ArrayList<>(chunk.getCyclopsList())) {
                if (cyclops.getBody() != null) {
                    float dist = playerPos.dst(cyclops.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        cyclops.takeDamage(damage);
                        enemiesHit++;
                    }
                }
            }
        }

        // Damage dungeon enemies
        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        enemy.takeDamage(damage);
                        enemiesHit++;
                    }
                }
            }
        }

        // Damage boss room bosses
        if (gameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = gameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null) {
                float dist = playerPos.dst(bossRoomBoss.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    bossRoomBoss.takeDamage(damage);
                    enemiesHit++;
                }
            }

            Cyclops cyclopsRoomBoss = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                float dist = playerPos.dst(cyclopsRoomBoss.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    cyclopsRoomBoss.takeDamage(damage);
                    enemiesHit++;
                }
            }
        }

        if (enemiesHit > 0) {
            System.out.println("Smite hit " + enemiesHit + " enemies for " + damage + " damage!");
        } else {
            System.out.println("Smite activated but no enemies in range!");
        }
    }
}

/**
 * Prayer - Heal the player after a cast time (copied from MercenaryAbilities)
 * Visual: Prayer texture overlay during cast
 */
class PaladinPrayerAbility extends Ability {
    private Player targetPlayer;
    private AbilityVisual.Prayer prayerVisual;
    private static final int HEAL_AMOUNT = 80;

    public PaladinPrayerAbility(Texture iconTexture) {
        super(
                "Prayer",
                "Channel divine energy to restore 80 health",
                15.0f,
                0,
                1.0f,
                0f,
                0f,
                AbilityType.HEALING,
                iconTexture
        );
    }

    @Override
    protected void onCastStart(Player player, GameProj gameProj) {
        super.onCastStart(player, gameProj);
        this.targetPlayer = player;

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