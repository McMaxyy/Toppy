package abilities;

import entities.Enemy;
import entities.DungeonEnemy;
import entities.BossKitty;
import entities.Player;

/**
 * Base class for status effects (buffs, debuffs, etc.)
 */
public abstract class StatusEffect {
    protected String name;
    protected float duration;
    protected float elapsed;
    protected boolean isActive;
    protected EffectType type;

    public enum EffectType {
        BUFF,       // Positive effect on player
        DEBUFF,     // Negative effect on enemy
        CROWD_CONTROL, // Stun, slow, etc.
        DOT         // Damage over time
    }

    public StatusEffect(String name, float duration, EffectType type) {
        this.name = name;
        this.duration = duration;
        this.type = type;
        this.elapsed = 0f;
        this.isActive = true;
    }

    /**
     * Update the effect (called every frame)
     * @return true if effect should continue, false if it should be removed
     */
    public boolean update(float delta) {
        if (!isActive) return false;

        elapsed += delta;

        // Call the update logic for the specific effect
        onUpdate(delta);

        if (elapsed >= duration) {
            onExpire();
            isActive = false;
            return false;
        }

        return true;
    }

    /**
     * Called when the effect is applied
     */
    public abstract void onApply();

    /**
     * Called every frame while active
     */
    public abstract void onUpdate(float delta);

    /**
     * Called when the effect expires
     */
    public abstract void onExpire();

    public boolean isExpired() {
        return !isActive;
    }

    public void remove() {
        isActive = false;
    }

    public String getName() { return name; }
    public float getDuration() { return duration; }
    public float getElapsed() { return elapsed; }
    public float getTimeRemaining() { return duration - elapsed; }
    public boolean isActive() { return isActive; }
    public EffectType getType() { return type; }
}