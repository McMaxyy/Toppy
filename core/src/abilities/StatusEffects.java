package abilities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import config.Storage;
import entities.*;
import managers.SoundManager;

public abstract class StatusEffects {}

class BubbleShieldEffect extends StatusEffect {
    private Player player;
    private boolean wasInvulnerable;

    public BubbleShieldEffect(Player player, float duration) {
        super("Bubble Shield", duration, EffectType.BUFF);
        this.player = player;
    }

    @Override
    public void onApply() {
        wasInvulnerable = player.isInvulnerable();
        player.setInvulnerable(true);
    }

    @Override
    public void onUpdate(float delta) {
        // Keep player invulnerable while active
    }

    @Override
    public void onExpire() {
        if (!wasInvulnerable) {
            player.setInvulnerable(false);
        }
    }
}

class BleedEffect extends StatusEffect {
    private Object target;
    private int damagePerTick;
    private float tickInterval;
    private float tickTimer;
    private static Texture bleedIcon;

    public BleedEffect(Object target, float duration, int damagePerTick) {
        super("Bleeding", duration, EffectType.DOT);
        this.target = target;
        this.damagePerTick = damagePerTick;
        this.tickInterval = 0.5f;
        this.tickTimer = 0f;

        if (bleedIcon == null) {
            try {
                bleedIcon = Storage.assetManager.get("icons/effects/Bleed.png", Texture.class);
            } catch (Exception e) {
                bleedIcon = null;
            }
        }
    }

    @Override
    public void onApply() {
    }

    @Override
    public void onUpdate(float delta) {
        tickTimer += delta;

        if (tickTimer >= tickInterval) {
            if (target instanceof Enemy) {
                ((Enemy) target).takeDamage(damagePerTick);
            } else if (target instanceof DungeonEnemy) {
                ((DungeonEnemy) target).takeDamage(damagePerTick);
            } else if (target instanceof BossKitty) {
                ((BossKitty) target).takeDamage(damagePerTick);
            } else if (target instanceof Cyclops) {
                ((Cyclops) target).takeDamage(damagePerTick);
            } else if (target instanceof GhostBoss) {
                ((GhostBoss) target).takeDamage(damagePerTick);
            } else if (target instanceof Herman) {
                ((Herman) target).takeDamage(damagePerTick);
            } else if (target instanceof EndlessEnemy) {
                ((EndlessEnemy) target).takeDamage(damagePerTick);
            }

            tickTimer = 0f;
        }
    }

    @Override
    public void onExpire() {
    }

    public Object getTarget() {
        return target;
    }

    public static Texture getIcon() {
        return bleedIcon;
    }
}

class StunEffect extends StatusEffect {
    private Object target;
    private static Texture stunIcon;

    public StunEffect(Object target, float duration) {
        super("Stunned", duration, EffectType.CROWD_CONTROL);
        this.target = target;

        if (stunIcon == null) {
            try {
                stunIcon = Storage.assetManager.get("icons/effects/Stunned.png", Texture.class);
            } catch (Exception e) {
                stunIcon = null;
            }
        }
    }

    @Override
    public void onApply() {
        if (target instanceof Enemy) {
            ((Enemy) target).setStunned(true);
        } else if (target instanceof DungeonEnemy) {
            ((DungeonEnemy) target).setStunned(true);
        } else if (target instanceof BossKitty) {
            ((BossKitty) target).setStunned(true);
        } else if (target instanceof Cyclops) {
            ((Cyclops) target).setStunned(true);
        } else if (target instanceof GhostBoss) {
            ((GhostBoss) target).setStunned(true);
        } else if (target instanceof Herman) {
            ((Herman) target).setStunned(true);
        } else if (target instanceof EndlessEnemy) {
            ((EndlessEnemy) target).setStunned(true);
        }

        if (target instanceof Enemy && ((Enemy) target).getBody() != null) {
            ((Enemy) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof DungeonEnemy && ((DungeonEnemy) target).getBody() != null) {
            ((DungeonEnemy) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof BossKitty && ((BossKitty) target).getBody() != null) {
            ((BossKitty) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof Cyclops && ((Cyclops) target).getBody() != null) {
            ((Cyclops) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof GhostBoss && ((GhostBoss) target).getBody() != null) {
            ((GhostBoss) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof Herman && ((Herman) target).getBody() != null) {
            ((Herman) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof EndlessEnemy && ((EndlessEnemy) target).getBody() != null) {
            ((EndlessEnemy) target).getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public void onUpdate(float delta) {
        if (target instanceof Enemy && ((Enemy) target).getBody() != null) {
            ((Enemy) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof DungeonEnemy && ((DungeonEnemy) target).getBody() != null) {
            ((DungeonEnemy) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof BossKitty && ((BossKitty) target).getBody() != null) {
            ((BossKitty) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof Cyclops && ((Cyclops) target).getBody() != null) {
            ((Cyclops) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof GhostBoss && ((GhostBoss) target).getBody() != null) {
            ((GhostBoss) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof Herman && ((Herman) target).getBody() != null) {
            ((Herman) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof EndlessEnemy && ((EndlessEnemy) target).getBody() != null) {
            ((EndlessEnemy) target).getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public void onExpire() {
        if (target instanceof Enemy) {
            ((Enemy) target).setStunned(false);
        } else if (target instanceof DungeonEnemy) {
            ((DungeonEnemy) target).setStunned(false);
        } else if (target instanceof BossKitty) {
            ((BossKitty) target).setStunned(false);
        } else if (target instanceof Cyclops) {
            ((Cyclops) target).setStunned(false);
        } else if (target instanceof GhostBoss) {
            ((GhostBoss) target).setStunned(false);
        } else if (target instanceof Herman) {
            ((Herman) target).setStunned(false);
        } else if (target instanceof EndlessEnemy) {
            ((EndlessEnemy) target).setStunned(false);
        }
    }

    public Object getTarget() {
        return target;
    }

    public static Texture getIcon() {
        return stunIcon;
    }
}

class ConsecratedEffect extends StatusEffect {
    private Object target;
    private int damage;
    private boolean damageDealt = false;
    private static Texture consecratedIcon;
    private float pulseTimer = 0f;

    public ConsecratedEffect(Object target, float duration, int damage) {
        super("Consecrated", duration, EffectType.DEBUFF);
        this.target = target;
        this.damage = damage;

        if (consecratedIcon == null) {
            try {
                consecratedIcon = Storage.assetManager.get("icons/effects/Consecrated.png", Texture.class);
            } catch (Exception e) {
                consecratedIcon = null;
            }
        }
    }

    @Override
    public void onApply() {
    }

    @Override
    public void onUpdate(float delta) {
        pulseTimer += delta;
    }

    @Override
    public void onExpire() {
        if (!damageDealt) {
            if (target instanceof Enemy && !((Enemy) target).isMarkedForRemoval()) {
                ((Enemy) target).takeDamage(damage);
            } else if (target instanceof DungeonEnemy && !((DungeonEnemy) target).isMarkedForRemoval()) {
                ((DungeonEnemy) target).takeDamage(damage);
            } else if (target instanceof BossKitty && !((BossKitty) target).isMarkedForRemoval()) {
                ((BossKitty) target).takeDamage(damage);
            } else if (target instanceof Cyclops && !((Cyclops) target).isMarkedForRemoval()) {
                ((Cyclops) target).takeDamage(damage);
            } else if (target instanceof GhostBoss && !((GhostBoss) target).isMarkedForRemoval()) {
                ((GhostBoss) target).takeDamage(damage);
            } else if (target instanceof Herman && !((Herman) target).isMarkedForRemoval()) {
                ((Herman) target).takeDamage(damage);
            } else if (target instanceof EndlessEnemy && !((EndlessEnemy) target).isMarkedForRemoval()) {
                ((EndlessEnemy) target).takeDamage(damage);
            }
            damageDealt = true;

            SoundManager.getInstance().playAbilitySound("Consecrate");
        }
    }

    public Object getTarget() {
        return target;
    }

    public float getPulseTimer() {
        return pulseTimer;
    }

    public static Texture getIcon() {
        return consecratedIcon;
    }
}

class SprintEffect extends StatusEffect {
    private Player player;
    private int dexBonus;
    private boolean applied = false;

    public SprintEffect(Player player, float duration, int dexBonus) {
        super("Sprint", duration, EffectType.BUFF);
        this.player = player;
        this.dexBonus = dexBonus;
    }

    @Override
    public void onApply() {
        if (!applied) {
            player.getStats().addGearDex(dexBonus);
            player.setSprinting(true);
            applied = true;
        }
    }

    @Override
    public void onUpdate(float delta) {
        // Sprint effect is passive - just maintains the DEX bonus
    }

    @Override
    public void onExpire() {
        if (applied) {
            player.getStats().removeGearDex(dexBonus);
            player.setSprinting(false);
            applied = false;
        }
    }

    public Player getPlayer() {
        return player;
    }
}

/**
 * Smoke Bomb Effect - Player is invulnerable while inside the smoke zone
 */
class SmokeBombEffect extends StatusEffect {
    private Player player;
    private Vector2 zoneCenter;
    private float zoneRadius;
    private boolean wasInZone = false;

    public SmokeBombEffect(Player player, float duration, Vector2 zoneCenter, float zoneRadius) {
        super("Smoke Bomb", duration, EffectType.BUFF);
        this.player = player;
        this.zoneCenter = new Vector2(zoneCenter);
        this.zoneRadius = zoneRadius;
    }

    @Override
    public void onApply() {
        // Check if player starts in zone
        checkZoneStatus();
    }

    @Override
    public void onUpdate(float delta) {
        checkZoneStatus();
    }

    private void checkZoneStatus() {
        if (player.getPosition() == null) return;

        float distance = player.getPosition().dst(zoneCenter);
        boolean isInZone = distance <= zoneRadius;

        if (isInZone && !wasInZone) {
            // Entered zone
            player.setInvulnerable(true);
            wasInZone = true;
        } else if (!isInZone && wasInZone) {
            // Left zone
            player.setInvulnerable(false);
            wasInZone = false;
        }
    }

    @Override
    public void onExpire() {
        // Remove invulnerability when effect ends
        if (wasInZone) {
            player.setInvulnerable(false);
        }
    }

    public Vector2 getZoneCenter() {
        return zoneCenter;
    }

    public float getZoneRadius() {
        return zoneRadius;
    }
}

/**
 * Life Leech Effect - Heals player on each basic attack hit
 */
class LifeLeechEffect extends StatusEffect {
    private Player player;
    private int healPerHit;

    public LifeLeechEffect(Player player, float duration, int healPerHit) {
        super("Life Leech", duration, EffectType.BUFF);
        this.player = player;
        this.healPerHit = healPerHit;
    }

    @Override
    public void onApply() {
        player.setLifeLeechActive(true, healPerHit);
    }

    @Override
    public void onUpdate(float delta) {
        // Life leech is handled in Player's attack methods
    }

    @Override
    public void onExpire() {
        player.setLifeLeechActive(false, 0);
    }

    public Player getPlayer() {
        return player;
    }

    public int getHealPerHit() {
        return healPerHit;
    }
}

class HolyBlessingEffect extends StatusEffect {
    private Player player;
    private int defenseBonus;
    private int attackBonus;
    private int healthBonus;
    private boolean applied = false;

    public HolyBlessingEffect(Player player, float duration, int defenseBonus, int attackBonus, int healthBonus) {
        super("Holy Blessing", duration, EffectType.BUFF);
        this.player = player;
        this.defenseBonus = defenseBonus;
        this.attackBonus = attackBonus;
        this.healthBonus = healthBonus;
    }

    @Override
    public void onApply() {
        if (!applied) {
            player.getStats().addGearDefense(defenseBonus);
            player.getStats().addGearDamage(attackBonus);
            player.getStats().addGearVitality(healthBonus / 10); // VIT gives 10 HP per point
            player.setHolyBlessingActive(true);
            applied = true;
        }
    }

    @Override
    public void onUpdate(float delta) {
        // Blessing effect is passive
    }

    @Override
    public void onExpire() {
        if (applied) {
            player.getStats().removeGearDefense(defenseBonus);
            player.getStats().removeGearDamage(attackBonus);
            player.getStats().removeGearVitality(healthBonus / 10);
            player.setHolyBlessingActive(false);
            applied = false;
        }
    }

    public Player getPlayer() {
        return player;
    }
}

class HolySwordEffect extends StatusEffect {
    private Player player;
    private int attackBonus;
    private float coneSizeMultiplier;
    private boolean applied = false;

    public HolySwordEffect(Player player, float duration, int attackBonus, float coneSizeMultiplier) {
        super("Holy Sword", duration, EffectType.BUFF);
        this.player = player;
        this.attackBonus = attackBonus;
        this.coneSizeMultiplier = coneSizeMultiplier;
    }

    @Override
    public void onApply() {
        if (!applied) {
            player.getStats().addGearDamage(attackBonus);
            player.setHolySwordActive(true, coneSizeMultiplier);
            applied = true;
        }
    }

    @Override
    public void onUpdate(float delta) {
        // Holy sword effect is passive
    }

    @Override
    public void onExpire() {
        if (applied) {
            player.getStats().removeGearDamage(attackBonus);
            player.setHolySwordActive(false, 1.0f);
            applied = false;
        }
    }

    public Player getPlayer() {
        return player;
    }

    public float getConeSizeMultiplier() {
        return coneSizeMultiplier;
    }
}

class BlazingFuryEffect extends StatusEffect {
    private Player player;
    private int attackBonus;
    private int dexBonus;
    private boolean applied = false;

    public BlazingFuryEffect(Player player, float duration, int attackBonus, int dexBonus) {
        super("Blazing Fury", duration, EffectType.BUFF);
        this.player = player;
        this.attackBonus = attackBonus;
        this.dexBonus = dexBonus;
    }

    @Override
    public void onApply() {
        if (!applied) {
            player.getStats().addGearDex(dexBonus);
            player.getStats().addGearDamage(attackBonus);
            player.setHolyBlessingActive(true);
            applied = true;
        }
    }

    @Override
    public void onExpire() {
        if (applied) {
            player.getStats().removeGearDex(dexBonus);
            player.getStats().removeGearDamage(attackBonus);
            player.setHolyBlessingActive(false);
            applied = false;
        }
    }

    @Override
    public void onUpdate(float delta) {
        // No periodic updates needed
    }

    @Override
    public String getName() {
        return "Blazing Fury";
    }
}