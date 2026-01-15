package abilities;

import com.badlogic.gdx.graphics.Texture;
import entities.Player;

/**
 * Base class for all player abilities
 */
public abstract class Ability {
    protected String name;
    protected String description;
    protected float cooldown;
    protected float currentCooldown;
    protected int damage;
    protected float castTime;
    protected AbilityType type;
    protected Texture iconTexture;
    protected boolean isCasting;
    protected float castTimer;
    protected float duration;
    protected float distance;

    public enum AbilityType {
        DAMAGE,
        BUFF,
        DEBUFF,
        CROWD_CONTROL,
        HEALING
    }

    public Ability(String name, String description, float cooldown, int damage,
                   float castTime, float duration, float distance, AbilityType type, Texture iconTexture) {
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
        this.currentCooldown = 0f;
        this.damage = damage;
        this.castTime = castTime;
        this.type = type;
        this.iconTexture = iconTexture;
        this.isCasting = false;
        this.castTimer = 0f;
        this.duration = duration;
        this.distance = distance;
    }

    /**
     * Update ability cooldown and cast time
     */
    public void update(float delta) {
        if (currentCooldown > 0) {
            currentCooldown -= delta;
            if (currentCooldown < 0) currentCooldown = 0;
        }

        if (isCasting) {
            castTimer += delta;
            if (castTimer >= castTime) {
                onCastComplete();
                isCasting = false;
                castTimer = 0f;
            }
        }
    }

    /**
     * Attempt to use the ability
     * @return true if ability was successfully activated
     */
    public boolean use(Player player, game.GameProj gameProj) {
        if (currentCooldown > 0 || isCasting) {
            return false;
        }

        if (castTime > 0) {
            isCasting = true;
            castTimer = 0f;
            onCastStart(player, gameProj);
            return true;
        } else {
            execute(player, gameProj);
            currentCooldown = cooldown;
            return true;
        }
    }

    /**
     * Called when cast starts (for abilities with cast time)
     */
    protected void onCastStart(Player player, game.GameProj gameProj) {
        System.out.println("Casting " + name + "...");
    }

    /**
     * Called when cast completes
     */
    protected void onCastComplete() {
        System.out.println(name + " cast complete!");
    }

    /**
     * Execute the ability effect
     */
    protected abstract void execute(Player player, game.GameProj gameProj);

    /**
     * Cancel casting
     */
    public void cancelCast() {
        isCasting = false;
        castTimer = 0f;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public float getCooldown() { return cooldown; }
    public float getCurrentCooldown() { return currentCooldown; }
    public float getCooldownPercentage() {
        return currentCooldown > 0 ? currentCooldown / cooldown : 0f;
    }
    public int getDamage() { return damage; }
    public float getCastTime() { return castTime; }
    public float getDistance() { return distance; }
    public AbilityType getType() { return type; }
    public Texture getIconTexture() { return iconTexture; }
    public boolean isOnCooldown() { return currentCooldown > 0; }
    public boolean isCasting() { return isCasting; }
    public float getCastProgress() {
        return castTime > 0 ? castTimer / castTime : 0f;
    }
}