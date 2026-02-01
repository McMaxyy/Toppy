package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import entities.*;
import game.GameProj;
import managers.Chunk;
import managers.SoundManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class PaladinAbilities { }

class PaladinBlinkAbility extends Ability {

    public PaladinBlinkAbility(Texture iconTexture) {
        super(
                "Blink",
                "Teleport a distance towards the mouse position",
                2.0f,
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
        SoundManager.getInstance().playAbilitySound("Blink");

        Vector3 mousePosition3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);

        if (gameProj.getCurrentDungeon() != null) {
            if (!gameProj.getCurrentDungeon().isWalkableWorld(mousePosition.x, mousePosition.y)) {
                return;
            }
        }

        if (gameProj.getCurrentBossRoom() != null) {
            if (!gameProj.getCurrentBossRoom().isWalkableWorld(mousePosition.x, mousePosition.y)) {
                return;
            }
        }

        player.getBody().setTransform(mousePosition, 0f);

        AbilityVisual.Blink blinkVisual = new AbilityVisual.Blink(mousePosition);
        player.addAbilityVisual(blinkVisual);
    }
}

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

class SmiteAbility extends Ability {
    private static final float SMITE_RADIUS = 50f;
    private static final int SMITE_DAMAGE = 80;

    public SmiteAbility(Texture iconTexture) {
        super(
                "Smite",
                "Call down divine judgment, damaging all enemies around you",
                4.0f,
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
        SoundManager.getInstance().playAbilitySound("Smite");

        Vector2 playerPos = player.getPosition();

        AbilityVisual.Smite smiteVisual = new AbilityVisual.Smite(player, SMITE_RADIUS, 0.5f);
        player.addAbilityVisual(smiteVisual);

        int enemiesHit = 0;

        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom()) {
            for (Chunk chunk : gameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        float dist = playerPos.dst(enemy.getBody().getPosition());
                        if (dist < SMITE_RADIUS) {
                            enemy.takeDamage(damage + (player.getLevel() * 5));
                            enemiesHit++;
                        }
                    }
                }
            }

            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    herman.takeDamage(damage + (player.getLevel() * 5));
                    enemiesHit++;
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    hermanDuplicate.takeDamage(damage + (player.getLevel() * 5));
                    enemiesHit++;
                }
            }
        }

        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        enemy.takeDamage(damage + (player.getLevel() * 5));
                        enemiesHit++;
                    }
                }
            }
        }

        if (gameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = gameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null) {
                float dist = playerPos.dst(bossRoomBoss.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    bossRoomBoss.takeDamage(damage + (player.getLevel() * 5));
                    enemiesHit++;
                }
            }

            Cyclops cyclopsRoomBoss = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                float dist = playerPos.dst(cyclopsRoomBoss.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    cyclopsRoomBoss.takeDamage(damage + (player.getLevel() * 5));
                    enemiesHit++;
                }
            }

            GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    ghostBoss.takeDamage(damage + (player.getLevel() * 5));
                    enemiesHit++;
                }
            }
        }
    }
}

class PaladinPrayerAbility extends Ability {
    private Player targetPlayer;
    private AbilityVisual.Prayer prayerVisual;
    private static int HEAL_AMOUNT = 80;

    public PaladinPrayerAbility(Texture iconTexture) {
        super(
                "Prayer",
                "Channel divine energy to restore 80 health",
                10.0f,
                0,
                2.0f,
                0f,
                0f,
                AbilityType.HEALING,
                iconTexture
        );
    }

    @Override
    protected void onCastStart(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("Prayer");

        super.onCastStart(player, gameProj);
        this.targetPlayer = player;
        HEAL_AMOUNT += (targetPlayer.getLevel() * 5);

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

class ConsecratedGroundAbility extends Ability {
    private static final float CONSECRATE_RADIUS = 80f;
    private static final float CONSECRATE_DELAY = 3.0f;
    private static final int CONSECRATE_DAMAGE = 120;

    public ConsecratedGroundAbility(Texture iconTexture) {
        super(
                "Consecrated Ground",
                "Mark all enemies in range with holy light. After 3 seconds, they take massive damage.",
                8.0f,
                CONSECRATE_DAMAGE,
                0f,
                CONSECRATE_DELAY,
                CONSECRATE_RADIUS,
                AbilityType.DAMAGE,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("Consecrate");

        Vector2 playerPos = player.getPosition();

        AbilityVisual.ConsecratedGround consecrateVisual = new AbilityVisual.ConsecratedGround(
                player, CONSECRATE_RADIUS, CONSECRATE_DELAY
        );
        player.addAbilityVisual(consecrateVisual);

        int scaledDamage = damage + (player.getLevel() * 10);

        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom()) {
            for (Chunk chunk : gameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        float dist = playerPos.dst(enemy.getBody().getPosition());
                        if (dist < CONSECRATE_RADIUS) {
                            ConsecratedEffect effect = new ConsecratedEffect(enemy, CONSECRATE_DELAY, scaledDamage);
                            effect.onApply();
                            gameProj.addStatusEffect(enemy, effect);
                        }
                    }
                }
            }

            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < CONSECRATE_RADIUS) {
                    ConsecratedEffect effect = new ConsecratedEffect(herman, CONSECRATE_DELAY, scaledDamage);
                    effect.onApply();
                    gameProj.addStatusEffect(herman, effect);
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < CONSECRATE_RADIUS) {
                    ConsecratedEffect effect = new ConsecratedEffect(hermanDuplicate, CONSECRATE_DELAY, scaledDamage);
                    effect.onApply();
                    gameProj.addStatusEffect(hermanDuplicate, effect);
                }
            }
        }

        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < CONSECRATE_RADIUS) {
                        ConsecratedEffect effect = new ConsecratedEffect(enemy, CONSECRATE_DELAY, scaledDamage);
                        effect.onApply();
                        gameProj.addStatusEffect(enemy, effect);
                    }
                }
            }
        }

        if (gameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = gameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null) {
                float dist = playerPos.dst(bossRoomBoss.getBody().getPosition());
                if (dist < CONSECRATE_RADIUS) {
                    ConsecratedEffect effect = new ConsecratedEffect(bossRoomBoss, CONSECRATE_DELAY, scaledDamage);
                    effect.onApply();
                    gameProj.addStatusEffect(bossRoomBoss, effect);
                }
            }

            Cyclops cyclopsRoomBoss = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                float dist = playerPos.dst(cyclopsRoomBoss.getBody().getPosition());
                if (dist < CONSECRATE_RADIUS) {
                    ConsecratedEffect effect = new ConsecratedEffect(cyclopsRoomBoss, CONSECRATE_DELAY, scaledDamage);
                    effect.onApply();
                    gameProj.addStatusEffect(cyclopsRoomBoss, effect);
                }
            }

            GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < CONSECRATE_RADIUS) {
                    ConsecratedEffect effect = new ConsecratedEffect(ghostBoss, CONSECRATE_DELAY, scaledDamage);
                    effect.onApply();
                    gameProj.addStatusEffect(ghostBoss, effect);
                }
            }
        }
    }
}