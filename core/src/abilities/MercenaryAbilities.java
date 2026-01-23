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

class ChargeAbility extends Ability {
    private static final float CHARGE_SPEED = 3000f;
    private static final float CHARGE_DURATION = 0.5f;
    private static final float STUN_DURATION = 1.5f;

    private Player chargingPlayer;
    private GameProj currentGameProj;
    private AbilityVisual.ChargeTrail trailVisual;

    public ChargeAbility(Texture iconTexture) {
        super(
                "Charge",
                "Dash forward, dealing damage and stunning enemies on impact",
                1.0f,
                20,
                0f,
                CHARGE_DURATION,
                60f,
                AbilityType.CROWD_CONTROL,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        player.setInvulnerable(true);
        Vector2 movementDirection = getPlayerMovementDirection();

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

        Vector2 chargeVelocity = new Vector2(
                movementDirection.x * CHARGE_SPEED * 25,
                movementDirection.y * CHARGE_SPEED * 25
        );

        player.startCharge(chargeVelocity, CHARGE_DURATION);
        chargingPlayer = player;
        currentGameProj = gameProj;

        trailVisual = new AbilityVisual.ChargeTrail(player, CHARGE_DURATION);
        player.addAbilityVisual(trailVisual);

        new Thread(() -> {
            try {
                Thread.sleep((long)(duration * 1000 * 2));

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

    private Vector2 getPlayerMovementDirection() {
        Vector2 direction = new Vector2(0, 0);

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) direction.y += 1;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) direction.y -= 1;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) direction.x -= 1;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) direction.x += 1;

        if (!direction.isZero()) {
            direction.nor();
        }

        return direction;
    }

    private void checkForEnemyHits() {
        if (chargingPlayer == null || currentGameProj == null) return;

        Vector2 playerPos = chargingPlayer.getPosition();
        float hitRadius = distance;

        java.util.Set<Object> hitEnemies = chargingPlayer.getChargeHitEnemies();

        // Check overworld enemies
        for (Chunk chunk : currentGameProj.getChunks().values()) {
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

            for (Cyclops cyclops : new ArrayList<>(chunk.getCyclopsList())) {
                if (cyclops.getBody() != null && !hitEnemies.contains(cyclops)) {
                    float dist = playerPos.dst(cyclops.getBody().getPosition());
                    if (dist < hitRadius) {
                        cyclops.takeDamage(damage);
                        StunEffect stun = new StunEffect(cyclops, STUN_DURATION);
                        stun.onApply();
                        currentGameProj.addStatusEffect(cyclops, stun);
                        hitEnemies.add(cyclops);
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

        // Check boss room bosses
        if (currentGameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = currentGameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null && !hitEnemies.contains(bossRoomBoss)) {
                float dist = playerPos.dst(bossRoomBoss.getBody().getPosition());
                if (dist < hitRadius) {
                    bossRoomBoss.takeDamage(damage);
                    StunEffect stun = new StunEffect(bossRoomBoss, STUN_DURATION);
                    stun.onApply();
                    currentGameProj.addStatusEffect(bossRoomBoss, stun);
                    hitEnemies.add(bossRoomBoss);
                }
            }

            Cyclops cyclopsRoomBoss = currentGameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null && !hitEnemies.contains(cyclopsRoomBoss)) {
                float dist = playerPos.dst(cyclopsRoomBoss.getBody().getPosition());
                if (dist < hitRadius) {
                    cyclopsRoomBoss.takeDamage(damage);
                    StunEffect stun = new StunEffect(cyclopsRoomBoss, STUN_DURATION);
                    stun.onApply();
                    currentGameProj.addStatusEffect(cyclopsRoomBoss, stun);
                    hitEnemies.add(cyclopsRoomBoss);
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

class BubbleAbility extends Ability {

    public BubbleAbility(Texture iconTexture) {
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

class PrayerAbility extends Ability {
    private Player targetPlayer;
    private AbilityVisual.Prayer prayerVisual;
    private static final int HEAL_AMOUNT = 80;

    public PrayerAbility(Texture iconTexture) {
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

class BlinkAbility extends Ability {

    public BlinkAbility(Texture iconTexture) {
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