package abilities;

import entities.Player;
import entities.Enemy;
import entities.DungeonEnemy;
import entities.BossKitty;

/**
 * All status effect implementations in one file.
 * Abstract wrapper class with all effects as package-private.
 */
public abstract class StatusEffects {}

/**
 * Bubble Shield - blocks all incoming damage for duration
 */
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
        System.out.println("Bubble Shield activated! Duration: " + duration + " seconds");
    }

    @Override
    public void onUpdate(float delta) {
        // Keep player invulnerable while active
        // The base StatusEffect class handles the duration countdown
    }

    @Override
    public void onExpire() {
        // Only remove invulnerability if it wasn't set before the bubble
        if (!wasInvulnerable) {
            player.setInvulnerable(false);
        }
        System.out.println("Bubble Shield expired!");
    }
}

/**
 * Bleed effect - deals damage over time
 */
class BleedEffect extends StatusEffect {
    private Object target; // Can be Enemy, DungeonEnemy, or BossKitty
    private int damagePerTick;
    private float tickInterval;
    private float tickTimer;

    public BleedEffect(Object target, float duration, int damagePerTick) {
        super("Bleeding", duration, EffectType.DOT);
        this.target = target;
        this.damagePerTick = damagePerTick;
        this.tickInterval = 0.5f; // Damage every 0.5 seconds
        this.tickTimer = 0f;
    }

    @Override
    public void onApply() {
        System.out.println("Target is bleeding! Will take " + damagePerTick + " damage every " +
                tickInterval + " seconds for " + duration + " seconds");
    }

    @Override
    public void onUpdate(float delta) {
        tickTimer += delta;

        if (tickTimer >= tickInterval) {
            // Deal damage based on target type
            if (target instanceof Enemy) {
                ((Enemy) target).takeDamage(damagePerTick);
            } else if (target instanceof DungeonEnemy) {
                ((DungeonEnemy) target).takeDamage(damagePerTick);
            } else if (target instanceof BossKitty) {
                ((BossKitty) target).takeDamage(damagePerTick);
            }

            tickTimer = 0f;
            System.out.println("Bleed tick! Remaining: " + getTimeRemaining() + "s");
        }
    }

    @Override
    public void onExpire() {
        System.out.println("Bleeding stopped!");
    }
}

/**
 * Stun effect - prevents enemy movement
 */
class StunEffect extends StatusEffect {
    private Object target;
    private com.badlogic.gdx.math.Vector2 originalVelocity;
    private boolean velocitySaved = false;

    public StunEffect(Object target, float duration) {
        super("Stunned", duration, EffectType.CROWD_CONTROL);
        this.target = target;
    }

    @Override
    public void onApply() {
        // Save and stop velocity
        if (target instanceof Enemy && ((Enemy) target).getBody() != null) {
            originalVelocity = new com.badlogic.gdx.math.Vector2(
                    ((Enemy) target).getBody().getLinearVelocity()
            );
            ((Enemy) target).getBody().setLinearVelocity(0, 0);
            velocitySaved = true;
        } else if (target instanceof DungeonEnemy && ((DungeonEnemy) target).getBody() != null) {
            originalVelocity = new com.badlogic.gdx.math.Vector2(
                    ((DungeonEnemy) target).getBody().getLinearVelocity()
            );
            ((DungeonEnemy) target).getBody().setLinearVelocity(0, 0);
            velocitySaved = true;
        } else if (target instanceof BossKitty && ((BossKitty) target).getBody() != null) {
            originalVelocity = new com.badlogic.gdx.math.Vector2(
                    ((BossKitty) target).getBody().getLinearVelocity()
            );
            ((BossKitty) target).getBody().setLinearVelocity(0, 0);
            velocitySaved = true;
        }
        System.out.println("Target is stunned for " + duration + " seconds!");
    }

    @Override
    public void onUpdate(float delta) {
        // Keep velocity at 0
        if (target instanceof Enemy && ((Enemy) target).getBody() != null) {
            ((Enemy) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof DungeonEnemy && ((DungeonEnemy) target).getBody() != null) {
            ((DungeonEnemy) target).getBody().setLinearVelocity(0, 0);
        } else if (target instanceof BossKitty && ((BossKitty) target).getBody() != null) {
            ((BossKitty) target).getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public void onExpire() {
        System.out.println("Stun expired!");
        // Velocity will naturally resume when enemy AI updates
    }
}