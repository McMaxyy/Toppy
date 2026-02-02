package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import config.Storage;
import entities.*;
import game.GameProj;
import managers.BossRoom;
import managers.Chunk;

import java.util.ArrayList;

public abstract class MercenaryAbilities {}

class DoubleSwingAbility extends Ability {
    private AbilityVisual.ConalAttack firstSwing;
    private AbilityVisual.ConalAttack secondSwing;
    private boolean firstSwingComplete = false;

    public DoubleSwingAbility(Texture iconTexture) {
        super(
                "Double Swing",
                "Strike twice, each hit dealing 90% weapon damage",
                1.0f,
                0,
                0f,
                0f,
                50f,
                AbilityType.DAMAGE,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        int playerDamage = player.getStats().getTotalDamage();
        int swingDamage = (int)(playerDamage * 0.9f);

        firstSwing = AbilityVisual.ConalAttack.createWhite(player, gameProj, 0.1f, distance);
        player.addAbilityVisual(firstSwing);

        dealDamageInArea(player, swingDamage, gameProj);

        new Thread(() -> {
            try {
                Thread.sleep(300);

                Gdx.app.postRunnable(() -> {
                    if (player != null) {
                        secondSwing = AbilityVisual.ConalAttack.createWhite(player, gameProj, 0.1f, distance);
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
                        enemy.takeDamage(damage + (player.getLevel() * 5));
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
        }
    }
}

class RendAbility extends Ability {
    private static final float BLEED_DURATION = 5.0f;
    private static final int BLEED_DAMAGE_PER_TICK = 10;

    public RendAbility(Texture iconTexture) {
        super(
                "Rend",
                "Inflict bleeding wounds on all enemies in a cone, dealing damage over time",
                10.0f,
                0,
                0f,
                BLEED_DURATION,
                50f,
                AbilityType.DEBUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        Vector2 playerPos = player.getPosition();
        int enemiesHit = 0;

        AbilityVisual.ConalAttack indicator = AbilityVisual.ConalAttack.createRed(player, gameProj, 0.2f, distance);
        player.addAbilityVisual(indicator);

        Vector3 mousePos3D = gameProj.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
        Vector2 attackDir = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();

        // Hit ALL enemies in overworld chunks
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

                        enemiesHit++;
                    }
                }
            }

            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    Vector2 enemyPos = boss.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float dist = toEnemy.len();

                    if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        boss.takeDamage(damage);

                        BleedEffect bleed = new BleedEffect(boss, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                        bleed.onApply();
                        gameProj.addStatusEffect(boss, bleed);

                        enemiesHit++;
                    }
                }
            }

            for (Cyclops cyclops : new ArrayList<>(chunk.getCyclopsList())) {
                if (cyclops.getBody() != null) {
                    Vector2 enemyPos = cyclops.getBody().getPosition();
                    Vector2 toEnemy = new Vector2(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
                    float dist = toEnemy.len();

                    if (dist < distance && toEnemy.nor().dot(attackDir) > 0.5f) {
                        cyclops.takeDamage(damage);

                        BleedEffect bleed = new BleedEffect(cyclops, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                        bleed.onApply();
                        gameProj.addStatusEffect(cyclops, bleed);

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

                        BleedEffect bleed = new BleedEffect(enemy, BLEED_DURATION, BLEED_DAMAGE_PER_TICK + (player.getLevel() * 3));
                        bleed.onApply();
                        gameProj.addStatusEffect(enemy, bleed);

                        enemiesHit++;
                    }
                }
            }
        }

        // Hit boss room bosses
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

                    enemiesHit++;
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

                    enemiesHit++;
                }
            }
        }
    }
}