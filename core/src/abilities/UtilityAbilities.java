package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import entities.*;
import game.GameProj;
import managers.Chunk;
import managers.SoundManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class UtilityAbilities { }

class PaladinBubbleAbility extends Ability {

    public PaladinBubbleAbility(Texture iconTexture) {
        super(
                "Bubble",
                "Gain a shield that blocks all damage for 2 seconds",
                6.0f,
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
                4.0f,
                0,
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
        SoundManager.getInstance().playAbilitySound("Pull");

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
        float progress = pullTimer / PULL_DURATION;

        if (!currentGameProj.isInDungeon() && !currentGameProj.isInBossRoom()) {
            for (Chunk chunk : currentGameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
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

            Herman herman = currentGameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                Vector2 enemyPos = herman.getBody().getPosition();
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

                    herman.getBody().setTransform(newPos, herman.getBody().getAngle());
                    herman.getBody().setLinearVelocity(0, 0);

                    if (!affectedEnemies.contains(herman)) {
                        herman.takeDamage(damage);
                        affectedEnemies.add(herman);
                    }
                }
            }

            Herman hermanDuplicate = currentGameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                Vector2 enemyPos = hermanDuplicate.getBody().getPosition();
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

                    hermanDuplicate.getBody().setTransform(newPos, hermanDuplicate.getBody().getAngle());
                    hermanDuplicate.getBody().setLinearVelocity(0, 0);

                    if (!affectedEnemies.contains(hermanDuplicate)) {
                        hermanDuplicate.takeDamage(damage);
                        affectedEnemies.add(hermanDuplicate);
                    }
                }
            }
        }

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

                        if (currentGameProj.getCurrentDungeon() != null) {
                            if (!currentGameProj.getCurrentDungeon().isWalkableWorld(newPos.x, newPos.y)) {
                                return;
                            }
                        }

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

            GhostBoss ghostBoss = currentGameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                Vector2 enemyPos = ghostBoss.getBody().getPosition();
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

                    ghostBoss.getBody().setTransform(newPos, ghostBoss.getBody().getAngle());
                    ghostBoss.getBody().setLinearVelocity(0, 0);

                    if (!affectedEnemies.contains(ghostBoss)) {
                        ghostBoss.takeDamage(damage);
                        affectedEnemies.add(ghostBoss);
                    }
                }
            }
        }
    }

    public boolean isPulling() {
        return isPulling;
    }
}

class SprintAbility extends Ability {
    private static final float SPRINT_DURATION = 10.0f;
    private static final int DEX_BONUS = 15;

    public SprintAbility(Texture iconTexture) {
        super(
                "Sprint",
                "Gain +15 DEX for 10 seconds",
                10.0f,
                0,
                0f,
                SPRINT_DURATION,
                0f,
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("Sprint");

        SprintEffect sprintEffect = new SprintEffect(player, SPRINT_DURATION, DEX_BONUS);
        sprintEffect.onApply();
        gameProj.addStatusEffect(player, sprintEffect);

        AbilityVisual.SprintAura sprintVisual = new AbilityVisual.SprintAura(player, SPRINT_DURATION);
        player.addAbilityVisual(sprintVisual);
    }
}

class FullHealAbility extends Ability {

    public FullHealAbility(Texture iconTexture) {
        super(
                "Full Heal",
                "Instantly restore all health",
                60.0f,
                0,
                1.5f,
                0f,
                0f,
                AbilityType.HEALING,
                iconTexture
        );
    }

    private Player targetPlayer;
    private AbilityVisual.Prayer prayerVisual;

    @Override
    protected void onCastStart(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("Prayer");
        super.onCastStart(player, gameProj);
        this.targetPlayer = player;

        prayerVisual = new AbilityVisual.Prayer(player, castTime);
        player.addAbilityVisual(prayerVisual);
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
        player.getStats().fullHeal();
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

class SmokeBombAbility extends Ability {
    private static final float SMOKE_RADIUS = 60f;
    private static final float SMOKE_DURATION = 3.0f;

    public SmokeBombAbility(Texture iconTexture) {
        super(
                "Smoke Bomb",
                "Create a smoke zone that protects you from damage for 3 seconds",
                10.0f,
                0,
                0f,
                SMOKE_DURATION,
                SMOKE_RADIUS,
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("SmokeBomb");
        Vector2 position = new Vector2(player.getPosition());

        SmokeBombEffect smokeEffect = new SmokeBombEffect(player, SMOKE_DURATION, position, SMOKE_RADIUS);
        smokeEffect.onApply();
        gameProj.addStatusEffect(player, smokeEffect);

        AbilityVisual.SmokeBombZone smokeVisual = new AbilityVisual.SmokeBombZone(position, SMOKE_RADIUS, SMOKE_DURATION);
        player.addAbilityVisual(smokeVisual);
    }
}

class LifeLeechAbility extends Ability {
    private static final float LEECH_DURATION = 5.0f;
    private static final int HEAL_PER_HIT = 5;

    public LifeLeechAbility(Texture iconTexture) {
        super(
                "Life Leech",
                "Your attacks heal you for 5 HP for 5 seconds",
                25.0f,
                0,
                0f,
                LEECH_DURATION,
                0f,
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        // Apply life leech effect
        LifeLeechEffect leechEffect = new LifeLeechEffect(player, LEECH_DURATION, HEAL_PER_HIT);
        leechEffect.onApply();
        gameProj.addStatusEffect(player, leechEffect);

        // Add visual effect - red/green aura
        AbilityVisual.LifeLeechAura leechVisual = new AbilityVisual.LifeLeechAura(player, LEECH_DURATION);
        player.addAbilityVisual(leechVisual);
    }
}