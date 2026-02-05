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

public abstract class MovementAbilities { }

class PaladinBlinkAbility extends Ability {

    public PaladinBlinkAbility(Texture iconTexture) {
        super(
                "Blink",
                "Teleport a distance towards the mouse position",
                3.0f,
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

class ChargeAbility extends Ability {
    private static final float CHARGE_SPEED = 500f;
    private static final float CHARGE_DURATION = 0.7f;
    private static final float STUN_DURATION = 1.5f;
    private static final int BASE_CHARGE_DAMAGE = 30;

    private Player chargingPlayer;
    private GameProj currentGameProj;
    private AbilityVisual.ChargeTrail trailVisual;

    public ChargeAbility(Texture iconTexture) {
        super(
                "Charge",
                "Dash forward, dealing damage and stunning enemies on impact",
                2.0f,
                BASE_CHARGE_DAMAGE,
                0f,
                CHARGE_DURATION,
                30f,
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
                movementDirection.x * CHARGE_SPEED,
                movementDirection.y * CHARGE_SPEED
        );

        player.setMovementAbility("Charge");
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

        Set<Object> hitEnemies = chargingPlayer.getChargeHitEnemies();

        // Check overworld enemies
        for (Chunk chunk : currentGameProj.getChunks().values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null && !hitEnemies.contains(enemy)) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < hitRadius) {
                        enemy.takeDamage(0);
                        StunEffect stun = new StunEffect(enemy, STUN_DURATION);
                        stun.onApply();
                        currentGameProj.addStatusEffect(enemy, stun);
                        hitEnemies.add(enemy);
                    }
                }
            }

            Herman herman = currentGameProj.getHerman();
            if (herman != null && herman.getBody() != null && !hitEnemies.contains(herman)) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < hitRadius) {
                    herman.takeDamage(0);
                    StunEffect stun = new StunEffect(herman, STUN_DURATION);
                    stun.onApply();
                    currentGameProj.addStatusEffect(herman, stun);
                    hitEnemies.add(herman);
                }
            }

            Herman hermanDuplicate = currentGameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null && !hitEnemies.contains(hermanDuplicate)) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < hitRadius) {
                    hermanDuplicate.takeDamage(0);
                    StunEffect stun = new StunEffect(hermanDuplicate, STUN_DURATION);
                    stun.onApply();
                    currentGameProj.addStatusEffect(hermanDuplicate, stun);
                    hitEnemies.add(hermanDuplicate);
                }
            }
        }

        // Check dungeon enemies
        if (currentGameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentGameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null && !hitEnemies.contains(enemy)) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < hitRadius) {
                        enemy.takeDamage(0);
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
                    bossRoomBoss.takeDamage(0);
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
                    cyclopsRoomBoss.takeDamage(0);
                    StunEffect stun = new StunEffect(cyclopsRoomBoss, STUN_DURATION);
                    stun.onApply();
                    currentGameProj.addStatusEffect(cyclopsRoomBoss, stun);
                    hitEnemies.add(cyclopsRoomBoss);
                }
            }

            GhostBoss ghostBoss = currentGameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null && !hitEnemies.contains(ghostBoss)) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < hitRadius) {
                    ghostBoss.takeDamage(0);
                    StunEffect stun = new StunEffect(ghostBoss, STUN_DURATION);
                    stun.onApply();
                    currentGameProj.addStatusEffect(ghostBoss, stun);
                    hitEnemies.add(ghostBoss);
                }
            }
        }
    }

    public boolean isCharging() {
        return chargingPlayer != null && chargingPlayer.isCharging();
    }
}

class ShadowStepAbility extends Ability {
    private static final float SHADOW_STEP_SPEED = 450f;
    private static final float SHADOW_STEP_DURATION = 0.5f;

    private Player steppingPlayer;
    private AbilityVisual.ShadowStepTrail trailVisual;

    public ShadowStepAbility(Texture iconTexture) {
        super(
                "Shadow Step",
                "Dash backwards away from danger",
                1.0f,
                0,
                0f,
                SHADOW_STEP_DURATION,
                0f,
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
        Vector2 playerPosition = player.getPosition();

        Vector2 awayDirection = new Vector2(
                playerPosition.x - mousePosition.x,
                playerPosition.y - mousePosition.y
        ).nor();

        Vector2 stepVelocity = new Vector2(
                awayDirection.x * SHADOW_STEP_SPEED,
                awayDirection.y * SHADOW_STEP_SPEED
        );

        player.setMovementAbility("ShadowStep");
        player.setInvulnerable(true);
        player.startCharge(stepVelocity, SHADOW_STEP_DURATION);
        steppingPlayer = player;

        trailVisual = new AbilityVisual.ShadowStepTrail(player, SHADOW_STEP_DURATION);
        player.addAbilityVisual(trailVisual);

        new Thread(() -> {
            try {
                Thread.sleep((long)(SHADOW_STEP_DURATION * 1000 + 200));
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

        if (steppingPlayer != null && !steppingPlayer.isCharging()) {
            steppingPlayer = null;
        }
    }
}

class VaultAbility extends Ability {
    private static final float VAULT_SPEED = 600f;
    private static final float VAULT_DURATION = 0.6f;
    private static final int BASE_VAULT_DAMAGE = 25;

    private Player vaultingPlayer;
    private GameProj currentGameProj;
    private Set<Object> hitEnemies;
    private AbilityVisual.VaultTrail trailVisual;

    public VaultAbility(Texture iconTexture) {
        super(
                "Vault",
                "Leap through enemies, damaging all in your path",
                2.0f,
                BASE_VAULT_DAMAGE,
                0f,
                VAULT_DURATION,
                40f,
                AbilityType.DAMAGE,
                iconTexture
        );
        this.hitEnemies = new HashSet<>();
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("Blink");
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

        Vector2 vaultVelocity = new Vector2(
                movementDirection.x * VAULT_SPEED,
                movementDirection.y * VAULT_SPEED
        );

        player.setMovementAbility("Vault");
        player.startVault(vaultVelocity, VAULT_DURATION);
        vaultingPlayer = player;
        currentGameProj = gameProj;
        hitEnemies.clear();

        trailVisual = new AbilityVisual.VaultTrail(player, VAULT_DURATION);
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

        if (vaultingPlayer != null && vaultingPlayer.isVaulting()) {
            checkForEnemyHits();
        } else if (vaultingPlayer != null && !vaultingPlayer.isVaulting()) {
            vaultingPlayer = null;
            currentGameProj = null;
            hitEnemies.clear();
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
        if (vaultingPlayer == null || currentGameProj == null) return;

        Vector2 playerPos = vaultingPlayer.getPosition();
        float hitRadius = distance;
        int actualDamage = vaultingPlayer.getStats().getActualDamage();
        int totalDamage = damage + actualDamage;

        for (Chunk chunk : currentGameProj.getChunks().values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null && !hitEnemies.contains(enemy)) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < hitRadius) {
                        enemy.takeDamage(totalDamage);
                        hitEnemies.add(enemy);
                    }
                }
            }

            Herman herman = currentGameProj.getHerman();
            if (herman != null && herman.getBody() != null && !hitEnemies.contains(herman)) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < hitRadius) {
                    herman.takeDamage(totalDamage);
                    hitEnemies.add(herman);
                }
            }

            Herman hermanDuplicate = currentGameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null && !hitEnemies.contains(hermanDuplicate)) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < hitRadius) {
                    hermanDuplicate.takeDamage(totalDamage);
                    hitEnemies.add(hermanDuplicate);
                }
            }
        }

        if (currentGameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentGameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null && !hitEnemies.contains(enemy)) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < hitRadius) {
                        enemy.takeDamage(totalDamage);
                        hitEnemies.add(enemy);
                    }
                }
            }
        }

        if (currentGameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = currentGameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null && !hitEnemies.contains(bossRoomBoss)) {
                float dist = playerPos.dst(bossRoomBoss.getBody().getPosition());
                if (dist < hitRadius) {
                    bossRoomBoss.takeDamage(totalDamage);
                    hitEnemies.add(bossRoomBoss);
                }
            }

            Cyclops cyclopsRoomBoss = currentGameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null && !hitEnemies.contains(cyclopsRoomBoss)) {
                float dist = playerPos.dst(cyclopsRoomBoss.getBody().getPosition());
                if (dist < hitRadius) {
                    cyclopsRoomBoss.takeDamage(totalDamage);
                    hitEnemies.add(cyclopsRoomBoss);
                }
            }

            GhostBoss ghostBoss = currentGameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null && !hitEnemies.contains(ghostBoss)) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < hitRadius) {
                    ghostBoss.takeDamage(totalDamage);
                    hitEnemies.add(ghostBoss);
                }
            }
        }
    }

    public boolean isVaulting() {
        return vaultingPlayer != null && vaultingPlayer.isVaulting();
    }
}