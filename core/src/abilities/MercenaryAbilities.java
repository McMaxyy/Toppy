package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import config.Storage;
import entities.*;
import game.GameProj;
import managers.Chunk;
import managers.SoundManager;

import java.util.ArrayList;

public abstract class MercenaryAbilities { }

class DoubleSwingAbility extends Ability {
    private static final float ATTACK_RANGE = 45f; // Same as sword

    public DoubleSwingAbility(Texture iconTexture) {
        super(
                "Double Swing",
                "Strike twice, each hit dealing 90% weapon damage",
                1.0f,
                0,
                0f,
                0f,
                ATTACK_RANGE,
                AbilityType.DAMAGE,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        int playerDamage = player.getStats().getTotalDamage();
        int swingDamage = (int)(playerDamage * 0.9f);

        // First swing - rotated 25 degrees up
        AbilityVisual.SpearJab firstSwing = AbilityVisual.SpearJab.createWhite(player, gameProj, 0.15f, distance, 25f);
        player.addAbilityVisual(firstSwing);
        dealDamageInArea(player, swingDamage, gameProj);

        // Second swing after 0.15s - rotated 50 degrees down from first (so -25 degrees from neutral)
        new Thread(() -> {
            try {
                Thread.sleep(150);

                Gdx.app.postRunnable(() -> {
                    if (player != null) {
                        AbilityVisual.SpearJab secondSwing = AbilityVisual.SpearJab.createWhite(player, gameProj, 0.15f, distance, -25f);
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

        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom()) {
            for (Chunk chunk : gameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        Vector2 enemyPos = enemy.getBody().getPosition();
                        Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                        if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                            enemy.takeDamage(damage + (player.getLevel() * 5));
                        }
                    }
                }
            }

            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                Vector2 enemyPos = herman.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    herman.takeDamage(damage + (player.getLevel() * 5));
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                Vector2 enemyPos = hermanDuplicate.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    hermanDuplicate.takeDamage(damage + (player.getLevel() * 5));
                }
            }
        }


        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(damage + (player.getLevel() * 5));
                    }
                }
            }
        }

        // Check boss room bosses
        if (gameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = gameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null) {
                Vector2 enemyPos = bossRoomBoss.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    bossRoomBoss.takeDamage(damage + (player.getLevel() * 5));
                }
            }

            Cyclops cyclopsRoomBoss = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                Vector2 enemyPos = cyclopsRoomBoss.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    cyclopsRoomBoss.takeDamage(damage + (player.getLevel() * 5));
                }
            }

            GhostBoss ghostBossRoom = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBossRoom != null && ghostBossRoom.getBody() != null) {
                Vector2 enemyPos = ghostBossRoom.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                if (toEnemy.len() < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    ghostBossRoom.takeDamage(damage + (player.getLevel() * 5));
                }
            }
        }
    }
}

class RendAbility extends Ability {
    private static final float BLEED_DURATION = 5.0f;
    private static final int BLEED_DAMAGE_PER_TICK = 10;
    private static final float ATTACK_RANGE = 45f; // Same as sword/DoubleSwing

    public RendAbility(Texture iconTexture) {
        super(
                "Rend",
                "Inflict bleeding wounds on all enemies in a cone, dealing damage over time",
                10.0f,
                0,
                0f,
                BLEED_DURATION,
                ATTACK_RANGE,
                AbilityType.DEBUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();

        // Red spear jab visual
        AbilityVisual.SpearJab indicator = AbilityVisual.SpearJab.createRed(player, gameProj, 0.3f, distance, 0f);
        player.addAbilityVisual(indicator);

        Vector3 mousePos3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
        Vector2 attackDir = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();

        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom()) {
            for (Chunk chunk : gameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        Vector2 enemyPos = enemy.getBody().getPosition();
                        Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                        float dist = toEnemy.len();

                        if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                            enemy.takeDamage(damage);

                            BleedEffect bleed = new BleedEffect(enemy, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                            bleed.onApply();
                            gameProj.addStatusEffect(enemy, bleed);
                        }
                    }
                }
            }

            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                Vector2 enemyPos = herman.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                float dist = toEnemy.len();

                if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    herman.takeDamage(damage);

                    BleedEffect bleed = new BleedEffect(herman, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                    bleed.onApply();
                    gameProj.addStatusEffect(herman, bleed);
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                Vector2 enemyPos = hermanDuplicate.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                float dist = toEnemy.len();

                if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    hermanDuplicate.takeDamage(damage);

                    BleedEffect bleed = new BleedEffect(hermanDuplicate, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                    bleed.onApply();
                    gameProj.addStatusEffect(hermanDuplicate, bleed);
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

                        BleedEffect bleed = new BleedEffect(enemy, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                        bleed.onApply();
                        gameProj.addStatusEffect(enemy, bleed);
                    }
                }
            }
        }

        if (gameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = gameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null) {
                Vector2 enemyPos = bossRoomBoss.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                float dist = toEnemy.len();

                if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    bossRoomBoss.takeDamage(damage);

                    BleedEffect bleed = new BleedEffect(bossRoomBoss, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                    bleed.onApply();
                    gameProj.addStatusEffect(bossRoomBoss, bleed);
                }
            }

            Cyclops cyclopsRoomBoss = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                Vector2 enemyPos = cyclopsRoomBoss.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                float dist = toEnemy.len();

                if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    cyclopsRoomBoss.takeDamage(damage);

                    BleedEffect bleed = new BleedEffect(cyclopsRoomBoss, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                    bleed.onApply();
                    gameProj.addStatusEffect(cyclopsRoomBoss, bleed);
                }
            }

            GhostBoss ghostRoomBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostRoomBoss != null && ghostRoomBoss.getBody() != null) {
                Vector2 enemyPos = ghostRoomBoss.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                float dist = toEnemy.len();

                if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                    ghostRoomBoss.takeDamage(damage);

                    BleedEffect bleed = new BleedEffect(ghostRoomBoss, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                    bleed.onApply();
                    gameProj.addStatusEffect(ghostRoomBoss, bleed);
                }
            }
        }
    }
}

class GroundSlamAbility extends Ability {
    private static final float SLAM_RADIUS = 45f;
    private static final float STUN_DURATION = 1f;

    public GroundSlamAbility(Texture iconTexture) {
        super(
                "Ground Slam",
                "Slam the ground, stunning all enemies within 60 units for 1 second",
                10.0f,
                0,
                0f,
                STUN_DURATION,
                SLAM_RADIUS,
                AbilityType.CROWD_CONTROL,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("Smite");

        Vector2 playerPos = player.getPosition();

        // Ground slam visual
        AbilityVisual.GroundSlam slamVisual = new AbilityVisual.GroundSlam(player, SLAM_RADIUS, 0.5f);
        player.addAbilityVisual(slamVisual);

        // Stun enemies in overworld
        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom()) {
            for (Chunk chunk : gameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        float dist = playerPos.dst(enemy.getBody().getPosition());
                        if (dist < SLAM_RADIUS) {
                            StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                            stun.onApply();
                            gameProj.addStatusEffect(enemy, stun);
                        }
                    }
                }
            }

            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < SLAM_RADIUS) {
                    StunEffect stun = new StunEffect(herman, STUN_DURATION);
                    stun.onApply();
                    gameProj.addStatusEffect(herman, stun);
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < SLAM_RADIUS) {
                    StunEffect stun = new StunEffect(hermanDuplicate, STUN_DURATION);
                    stun.onApply();
                    gameProj.addStatusEffect(hermanDuplicate, stun);
                }
            }
        }

        // Stun dungeon enemies
        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < SLAM_RADIUS) {
                        StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                        stun.onApply();
                        gameProj.addStatusEffect(enemy, stun);
                    }
                }
            }
        }

        // Stun boss room enemies
        if (gameProj.getCurrentBossRoom() != null) {
            BossKitty boss = gameProj.getCurrentBossRoom().getBoss();
            if (boss != null && boss.getBody() != null) {
                float dist = playerPos.dst(boss.getBody().getPosition());
                if (dist < SLAM_RADIUS) {
                    StunEffect stun = new StunEffect(boss, STUN_DURATION * 0.5f); // Bosses get reduced stun
                    stun.onApply();
                    gameProj.addStatusEffect(boss, stun);
                }
            }

            Cyclops cyclops = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclops != null && cyclops.getBody() != null) {
                float dist = playerPos.dst(cyclops.getBody().getPosition());
                if (dist < SLAM_RADIUS) {
                    StunEffect stun = new StunEffect(cyclops, STUN_DURATION * 0.5f);
                    stun.onApply();
                    gameProj.addStatusEffect(cyclops, stun);
                }
            }

            GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < SLAM_RADIUS) {
                    StunEffect stun = new StunEffect(ghostBoss, STUN_DURATION * 0.5f);
                    stun.onApply();
                    gameProj.addStatusEffect(ghostBoss, stun);
                }
            }
        }
    }
}

class WhirlwindAbility extends Ability {
    private static final float WHIRLWIND_RADIUS = 65f;
    private static final float WHIRLWIND_DURATION = 3.0f;
    private static final int DAMAGE_PER_TICK = 10;
    private static final float TICK_INTERVAL = 0.5f;

    private Player whirlwindPlayer;
    private GameProj currentGameProj;
    private float whirlwindTimer;
    private float tickTimer;
    private boolean isActive;
    private AbilityVisual.Whirlwind whirlwindVisual;

    public WhirlwindAbility(Texture iconTexture) {
        super(
                "Whirlwind",
                "Spin your spear, damaging nearby enemies every second for 3 seconds",
                6.0f,
                DAMAGE_PER_TICK,
                0f,
                WHIRLWIND_DURATION,
                WHIRLWIND_RADIUS,
                AbilityType.DAMAGE,
                iconTexture
        );
        this.isActive = false;
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        whirlwindPlayer = player;
        currentGameProj = gameProj;
        whirlwindTimer = 0f;
        tickTimer = 0f;
        isActive = true;

        whirlwindVisual = new AbilityVisual.Whirlwind(player, WHIRLWIND_RADIUS, WHIRLWIND_DURATION);
        player.addAbilityVisual(whirlwindVisual);
        damageEnemiesInRange();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (isActive && whirlwindPlayer != null && currentGameProj != null) {
            whirlwindTimer += delta;
            tickTimer += delta;

            if (tickTimer >= TICK_INTERVAL) {
                damageEnemiesInRange();
                tickTimer = 0f;
            }

            if (whirlwindTimer >= WHIRLWIND_DURATION) {
                isActive = false;
                whirlwindPlayer = null;
                currentGameProj = null;
            }
        }
    }

    private void damageEnemiesInRange() {
        if (whirlwindPlayer == null || currentGameProj == null) return;

        Vector2 playerPos = whirlwindPlayer.getPosition();
        int actualDamage = whirlwindPlayer.getStats().getActualDamage();
        int scaledDamage = damage + actualDamage;

        if (!currentGameProj.isInDungeon() && !currentGameProj.isInBossRoom()) {
            for (Chunk chunk : currentGameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        float dist = playerPos.dst(enemy.getBody().getPosition());
                        if (dist < WHIRLWIND_RADIUS) {
                            enemy.takeDamage(scaledDamage);
                        }
                    }
                }
            }

            Herman herman = currentGameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < WHIRLWIND_RADIUS) {
                    herman.takeDamage(scaledDamage);
                }
            }

            Herman hermanDuplicate = currentGameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < WHIRLWIND_RADIUS) {
                    hermanDuplicate.takeDamage(scaledDamage);
                }
            }
        }

        if (currentGameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentGameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < WHIRLWIND_RADIUS) {
                        enemy.takeDamage(scaledDamage);
                    }
                }
            }
        }

        if (currentGameProj.getCurrentBossRoom() != null) {
            BossKitty boss = currentGameProj.getCurrentBossRoom().getBoss();
            if (boss != null && boss.getBody() != null) {
                float dist = playerPos.dst(boss.getBody().getPosition());
                if (dist < WHIRLWIND_RADIUS) {
                    boss.takeDamage(scaledDamage);
                }
            }

            Cyclops cyclops = currentGameProj.getCurrentBossRoom().getCyclops();
            if (cyclops != null && cyclops.getBody() != null) {
                float dist = playerPos.dst(cyclops.getBody().getPosition());
                if (dist < WHIRLWIND_RADIUS) {
                    cyclops.takeDamage(scaledDamage);
                }
            }

            GhostBoss ghostBoss = currentGameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < WHIRLWIND_RADIUS) {
                    ghostBoss.takeDamage(scaledDamage);
                }
            }
        }
    }
}

class ExecuteAbility extends Ability {
    private static final int EXECUTE_DAMAGE = 80;
    private static final float EXECUTE_RANGE = 35f;
    private static final float LINE_WIDTH = 5f;

    public ExecuteAbility(Texture iconTexture) {
        super(
                "Execute",
                "Deliver a devastating precise strike dealing 80 damage",
                20.0f,
                EXECUTE_DAMAGE,
                0f,
                0f,
                EXECUTE_RANGE,
                AbilityType.DAMAGE,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playHitSound("Sword");

        Vector2 playerPos = player.getPosition();
        int actualDamage = player.getStats().getActualDamage();
        int totalDamage = damage + actualDamage;

        // Red spear visual for execute
        AbilityVisual.SpearJab executeVisual = AbilityVisual.SpearJab.createRed(player, gameProj, 0.3f, EXECUTE_RANGE, 0f);
        player.addAbilityVisual(executeVisual);

        Vector3 mousePos3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
        Vector2 attackDir = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();

        // Line attack with precise width
        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom()) {
            for (Chunk chunk : gameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        Vector2 enemyPos = enemy.getBody().getPosition();
                        Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                        float distanceAlongLine = toEnemy.dot(attackDir);
                        if (distanceAlongLine > 0 && distanceAlongLine < EXECUTE_RANGE) {
                            Vector2 projectedPoint = new Vector2(attackDir).scl(distanceAlongLine);
                            float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                            if (perpDistance < LINE_WIDTH) {
                                enemy.takeDamage(totalDamage);
                            }
                        }
                    }
                }
            }

            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                Vector2 enemyPos = herman.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < EXECUTE_RANGE) {
                    Vector2 projectedPoint = new Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < LINE_WIDTH) {
                        herman.takeDamage(totalDamage);
                    }
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                Vector2 enemyPos = hermanDuplicate.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < EXECUTE_RANGE) {
                    Vector2 projectedPoint = new Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < LINE_WIDTH) {
                        hermanDuplicate.takeDamage(totalDamage);
                    }
                }
            }
        }

        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                    float distanceAlongLine = toEnemy.dot(attackDir);
                    if (distanceAlongLine > 0 && distanceAlongLine < EXECUTE_RANGE) {
                        Vector2 projectedPoint = new Vector2(attackDir).scl(distanceAlongLine);
                        float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                        if (perpDistance < LINE_WIDTH) {
                            enemy.takeDamage(totalDamage);
                        }
                    }
                }
            }
        }

        if (gameProj.getCurrentBossRoom() != null) {
            BossKitty boss = gameProj.getCurrentBossRoom().getBoss();
            if (boss != null && boss.getBody() != null) {
                Vector2 enemyPos = boss.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < EXECUTE_RANGE) {
                    Vector2 projectedPoint = new Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < LINE_WIDTH) {
                        boss.takeDamage(totalDamage);
                    }
                }
            }

            Cyclops cyclops = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclops != null && cyclops.getBody() != null) {
                Vector2 enemyPos = cyclops.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < EXECUTE_RANGE) {
                    Vector2 projectedPoint = new Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < LINE_WIDTH) {
                        cyclops.takeDamage(totalDamage);
                    }
                }
            }

            GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                Vector2 enemyPos = ghostBoss.getBody().getPosition();
                Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < EXECUTE_RANGE) {
                    Vector2 projectedPoint = new Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < LINE_WIDTH) {
                        ghostBoss.takeDamage(totalDamage);
                    }
                }
            }
        }
    }
}

class BlazingFuryAbility extends Ability {
    private static final float FURY_DURATION = 6.0f;
    private static final int ATTACK_BONUS = 10;
    private static final int DEX_BONUS = 10;

    public BlazingFuryAbility(Texture iconTexture) {
        super(
                "Blazing Fury",
                "Enter a state of fury: +10 attack and +10 DEX for 6 seconds",
                18.0f,
                0,
                0f,
                FURY_DURATION,
                0f,
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("Smite");

        BlazingFuryEffect furyEffect = new BlazingFuryEffect(
                player, FURY_DURATION, ATTACK_BONUS, DEX_BONUS
        );
        furyEffect.onApply();
        gameProj.addStatusEffect(player, furyEffect);

        AbilityVisual.BlazingFuryAura furyVisual = new AbilityVisual.BlazingFuryAura(player, FURY_DURATION);
        player.addAbilityVisual(furyVisual);
    }
}