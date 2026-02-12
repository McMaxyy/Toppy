package abilities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import entities.*;
import game.GameProj;
import managers.Chunk;
import managers.SoundManager;

import java.util.ArrayList;

public abstract class PaladinAbilities { }

class SmiteAbility extends Ability {
    private static final float SMITE_RADIUS = 50f;
    private static final int SMITE_DAMAGE = 30;

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
        int actualDamage = player.getStats().getActualDamage();

        AbilityVisual.Smite smiteVisual = new AbilityVisual.Smite(player, SMITE_RADIUS, 0.5f);
        player.addAbilityVisual(smiteVisual);

        int enemiesHit = 0;

        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom() && !gameProj.isInEndlessRoom()) {
            for (Chunk chunk : gameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        float dist = playerPos.dst(enemy.getBody().getPosition());
                        if (dist < SMITE_RADIUS) {
                            enemy.takeDamage(damage + actualDamage);
                            enemiesHit++;
                        }
                    }
                }
            }

            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    herman.takeDamage(damage + actualDamage);
                    enemiesHit++;
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    hermanDuplicate.takeDamage(damage + actualDamage);
                    enemiesHit++;
                }
            }

            for (Lemmy lemmy : new ArrayList<>(gameProj.getGlobalLemmy())) {
                if (lemmy != null && lemmy.getBody() != null) {
                    float dist = playerPos.dst(lemmy.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        lemmy.takeDamage(damage + actualDamage);
                        enemiesHit++;
                    }
                }
            }
        }

        if (gameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        enemy.takeDamage(damage + actualDamage);
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
                    bossRoomBoss.takeDamage(damage + actualDamage);
                    enemiesHit++;
                }
            }

            Cyclops cyclopsRoomBoss = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                float dist = playerPos.dst(cyclopsRoomBoss.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    cyclopsRoomBoss.takeDamage(damage + actualDamage);
                    enemiesHit++;
                }
            }

            GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < SMITE_RADIUS) {
                    ghostBoss.takeDamage(damage + actualDamage);
                    enemiesHit++;
                }
            }
        }

        if (gameProj.getCurrentEndlessRoom() != null) {
            for (EndlessEnemy enemy : new ArrayList<>(gameProj.getCurrentEndlessRoom().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < SMITE_RADIUS) {
                        enemy.takeDamage(damage + actualDamage);
                        enemiesHit++;
                    }
                }
            }
        }
    }
}

class PaladinPrayerAbility extends Ability {
    private Player targetPlayer;
    private AbilityVisual.Prayer prayerVisual;
    private static final int BASE_HEAL_AMOUNT = 80;

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

        prayerVisual = new AbilityVisual.Prayer(player, castTime);
        player.addAbilityVisual(prayerVisual);
    }

    @Override
    protected void onCastComplete() {
        execute(targetPlayer, null);
        // Use parent's cooldown calculation with attack speed
        super.onCastComplete();

        if (prayerVisual != null) {
            prayerVisual.dispose();
            prayerVisual = null;
        }
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        // heal() in PlayerStats already handles the healing calculation
        player.getStats().heal(BASE_HEAL_AMOUNT);
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
        Vector2 playerPos = player.getPosition();
        int actualDamage = player.getStats().getActualDamage();

        AbilityVisual.ConsecratedGround consecrateVisual = new AbilityVisual.ConsecratedGround(
                player, CONSECRATE_RADIUS, CONSECRATE_DELAY
        );
        player.addAbilityVisual(consecrateVisual);

        int scaledDamage = damage + actualDamage;

        if (!gameProj.isInDungeon() && !gameProj.isInBossRoom() && !gameProj.isInEndlessRoom()) {
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

            for (Lemmy lemmy : new ArrayList<>(gameProj.getGlobalLemmy())) {
                if (lemmy != null && lemmy.getBody() != null) {
                    float dist = playerPos.dst(lemmy.getBody().getPosition());
                    if (dist < CONSECRATE_RADIUS) {
                        ConsecratedEffect effect = new ConsecratedEffect(lemmy, CONSECRATE_DELAY, scaledDamage);
                        effect.onApply();
                        gameProj.addStatusEffect(lemmy, effect);
                    }
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

        if (gameProj.getCurrentEndlessRoom() != null) {
            for (EndlessEnemy enemy : new ArrayList<>(gameProj.getCurrentEndlessRoom().getEnemies())) {
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
    }
}

class HolyAuraAbility extends Ability {
    private static final float AURA_RADIUS = 70f;
    private static final float AURA_DURATION = 8.0f;
    private static final int DAMAGE_PER_TICK = 10;
    private static final float TICK_INTERVAL = 1.0f;

    private Player auraPlayer;
    private GameProj currentGameProj;
    private float auraTimer;
    private float tickTimer;
    private boolean isActive;
    private AbilityVisual.HolyAura auraVisual;

    public HolyAuraAbility(Texture iconTexture) {
        super(
                "Holy Aura",
                "Create a holy aura that damages nearby enemies every second for 8 seconds",
                14.0f,
                DAMAGE_PER_TICK,
                0f,
                AURA_DURATION,
                AURA_RADIUS,
                AbilityType.DAMAGE,
                iconTexture
        );
        this.isActive = false;
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("HolyAura");

        auraPlayer = player;
        currentGameProj = gameProj;
        auraTimer = 0f;
        tickTimer = 0f;
        isActive = true;

        auraVisual = new AbilityVisual.HolyAura(player, AURA_RADIUS, AURA_DURATION);
        player.addAbilityVisual(auraVisual);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (isActive && auraPlayer != null && currentGameProj != null) {
            auraTimer += delta;
            tickTimer += delta;

            if (tickTimer >= TICK_INTERVAL) {
                damageEnemiesInRange();
                tickTimer = 0f;
            }

            if (auraTimer >= AURA_DURATION) {
                isActive = false;
                auraPlayer = null;
                currentGameProj = null;
            }
        }
    }

    private void damageEnemiesInRange() {
        if (auraPlayer == null || currentGameProj == null) return;

        Vector2 playerPos = auraPlayer.getPosition();
        int actualDamage = auraPlayer.getStats().getActualDamage();
        int scaledDamage = damage + actualDamage;

        if (!currentGameProj.isInDungeon() && !currentGameProj.isInBossRoom() && !currentGameProj.isInEndlessRoom()) {
            for (Chunk chunk : currentGameProj.getChunks().values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.getBody() != null) {
                        float dist = playerPos.dst(enemy.getBody().getPosition());
                        if (dist < AURA_RADIUS) {
                            enemy.takeDamage(scaledDamage);
                        }
                    }
                }
            }

            Herman herman = currentGameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                float dist = playerPos.dst(herman.getBody().getPosition());
                if (dist < AURA_RADIUS) {
                    herman.takeDamage(scaledDamage);
                }
            }

            Herman hermanDuplicate = currentGameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                float dist = playerPos.dst(hermanDuplicate.getBody().getPosition());
                if (dist < AURA_RADIUS) {
                    hermanDuplicate.takeDamage(scaledDamage);
                }
            }

            for (Lemmy lemmy : new ArrayList<>(currentGameProj.getGlobalLemmy())) {
                if (lemmy != null && lemmy.getBody() != null) {
                    float dist = playerPos.dst(lemmy.getBody().getPosition());
                    if (dist < AURA_RADIUS) {
                        lemmy.takeDamage(scaledDamage);
                    }
                }
            }
        }

        if (currentGameProj.getCurrentDungeon() != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentGameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < AURA_RADIUS) {
                        enemy.takeDamage(scaledDamage);
                    }
                }
            }
        }

        if (currentGameProj.getCurrentBossRoom() != null) {
            BossKitty bossRoomBoss = currentGameProj.getCurrentBossRoom().getBoss();
            if (bossRoomBoss != null && bossRoomBoss.getBody() != null) {
                float dist = playerPos.dst(bossRoomBoss.getBody().getPosition());
                if (dist < AURA_RADIUS) {
                    bossRoomBoss.takeDamage(scaledDamage);
                }
            }

            Cyclops cyclopsRoomBoss = currentGameProj.getCurrentBossRoom().getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.getBody() != null) {
                float dist = playerPos.dst(cyclopsRoomBoss.getBody().getPosition());
                if (dist < AURA_RADIUS) {
                    cyclopsRoomBoss.takeDamage(scaledDamage);
                }
            }

            GhostBoss ghostBoss = currentGameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                float dist = playerPos.dst(ghostBoss.getBody().getPosition());
                if (dist < AURA_RADIUS) {
                    ghostBoss.takeDamage(scaledDamage);
                }
            }
        }

        if (currentGameProj.getCurrentEndlessRoom() != null) {
            for (EndlessEnemy enemy : new ArrayList<>(currentGameProj.getCurrentEndlessRoom().getEnemies())) {
                if (enemy.getBody() != null) {
                    float dist = playerPos.dst(enemy.getBody().getPosition());
                    if (dist < AURA_RADIUS) {
                        enemy.takeDamage(scaledDamage);
                    }
                }
            }
        }
    }

    public boolean isAuraActive() {
        return isActive;
    }
}

class HolyBlessingAbility extends Ability {
    private static final float BLESSING_DURATION = 6.0f;
    private static final int DEFENSE_BONUS = 12;
    private static final int ATTACK_BONUS = 10;
    private static final int HEALTH_BONUS = 100;

    public HolyBlessingAbility(Texture iconTexture) {
        super(
                "Holy Blessing",
                "Gain +12 defense, +10 attack, and +100 health for 6 seconds",
                18.0f,
                0,
                0f,
                BLESSING_DURATION,
                0f,
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("HolyBlessing");

        HolyBlessingEffect blessingEffect = new HolyBlessingEffect(
                player, BLESSING_DURATION, DEFENSE_BONUS, ATTACK_BONUS, HEALTH_BONUS
        );
        blessingEffect.onApply();
        gameProj.addStatusEffect(player, blessingEffect);

        AbilityVisual.HolyBlessingAura blessingVisual = new AbilityVisual.HolyBlessingAura(player, BLESSING_DURATION);
        player.addAbilityVisual(blessingVisual);
    }
}

class HolySwordAbility extends Ability {
    private static final float HOLY_SWORD_DURATION = 6.0f;
    private static final int ATTACK_BONUS = 10;
    private static final float CONE_SIZE_MULTIPLIER = 1.5f;

    public HolySwordAbility(Texture iconTexture) {
        super(
                "Holy Sword",
                "Empower your sword with holy energy: bigger attacks and +10 damage for 6 seconds",
                12.0f,
                0,
                0f,
                HOLY_SWORD_DURATION,
                0f,
                AbilityType.BUFF,
                iconTexture
        );
    }

    @Override
    protected void execute(Player player, GameProj gameProj) {
        SoundManager.getInstance().playAbilitySound("BlazingFury");

        HolySwordEffect holySwordEffect = new HolySwordEffect(
                player, HOLY_SWORD_DURATION, ATTACK_BONUS, CONE_SIZE_MULTIPLIER
        );
        holySwordEffect.onApply();
        gameProj.addStatusEffect(player, holySwordEffect);

        AbilityVisual.HolySwordAura swordVisual = new AbilityVisual.HolySwordAura(player, HOLY_SWORD_DURATION);
        player.addAbilityVisual(swordVisual);
    }
}