package entities;

/**
 * Player statistics and attributes
 */
public class PlayerStats {
    // Core stats
    private int maxHealth;
    private int currentHealth;
    private int baseDamage;
    private int defense;
    private float baseSpeed;

    // Base values (for reset)
    private static final int BASE_MAX_HEALTH = 1000;
    private static final int BASE_DAMAGE = 20;
    private static final int BASE_DEFENSE = 0;

    // Stat point allocation
    private int allocatedHealthPoints;
    private int allocatedAttackPoints;
    private int allocatedDefensePoints;
    private int availableStatPoints;
    private static final int STAT_POINTS_PER_LEVEL = 5;
    private static final int HEALTH_PER_POINT = 10;
    private static final int ATTACK_PER_POINT = 2;
    private static final int DEFENSE_PER_POINT = 1;

    // Equipment bonuses
    private int weaponDamage;
    private int armorDefense;

    // Level and progression
    private int level;
    private int experience;
    private int experienceToNextLevel;

    // Regeneration
    private float healthRegenRate;
    private float regenTimer;

    public PlayerStats() {
        // Default starting stats
        this.level = 1;
        this.maxHealth = BASE_MAX_HEALTH;
        this.currentHealth = BASE_MAX_HEALTH;
        this.baseDamage = BASE_DAMAGE;
        this.defense = BASE_DEFENSE;
        this.baseSpeed = 5000f;
        this.weaponDamage = 0;
        this.armorDefense = 0;
        this.experience = 0;
        this.experienceToNextLevel = 100;
        this.healthRegenRate = 1f;
        this.regenTimer = 0f;

        // Stat allocation
        this.allocatedHealthPoints = 0;
        this.allocatedAttackPoints = 0;
        this.allocatedDefensePoints = 0;
        this.availableStatPoints = 0;
    }

    /**
     * Update stats (for regeneration, etc.)
     */
    public void update(float delta) {
        // Health regeneration
        if (currentHealth < maxHealth) {
            regenTimer += delta;
            if (regenTimer >= 1f) {
                heal((int) healthRegenRate);
                regenTimer = 0f;
            }
        }
    }

    /**
     * Allocate a stat point to health
     */
    public boolean allocateHealthPoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            allocatedHealthPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Allocate a stat point to attack
     */
    public boolean allocateAttackPoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            allocatedAttackPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Allocate a stat point to defense
     */
    public boolean allocateDefensePoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            allocatedDefensePoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Deallocate a stat point from health
     */
    public boolean deallocateHealthPoint() {
        if (allocatedHealthPoints > 0) {
            allocatedHealthPoints--;
            availableStatPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Deallocate a stat point from attack
     */
    public boolean deallocateAttackPoint() {
        if (allocatedAttackPoints > 0) {
            allocatedAttackPoints--;
            availableStatPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Deallocate a stat point from defense
     */
    public boolean deallocateDefensePoint() {
        if (allocatedDefensePoints > 0) {
            allocatedDefensePoints--;
            availableStatPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    /**
     * Reset all allocated stat points
     */
    public void resetStatPoints() {
        availableStatPoints += allocatedHealthPoints + allocatedAttackPoints + allocatedDefensePoints;
        allocatedHealthPoints = 0;
        allocatedAttackPoints = 0;
        allocatedDefensePoints = 0;
        recalculateStats();
    }

    /**
     * Recalculate stats based on level and allocated points
     */
    private void recalculateStats() {
        // Base stats + level bonuses + allocated points
        int levelBonus = (level - 1) * 5; // Small bonus per level

        int oldMaxHealth = maxHealth;
        maxHealth = BASE_MAX_HEALTH + levelBonus + (allocatedHealthPoints * HEALTH_PER_POINT);
        baseDamage = BASE_DAMAGE + (level - 1) + (allocatedAttackPoints * ATTACK_PER_POINT);
        defense = BASE_DEFENSE + (allocatedDefensePoints * DEFENSE_PER_POINT);

        // Adjust current health proportionally if max health changed
        if (oldMaxHealth > 0 && maxHealth != oldMaxHealth) {
            float healthPercent = (float) currentHealth / oldMaxHealth;
            currentHealth = Math.max(1, (int) (maxHealth * healthPercent));
        }

        // Cap current health at max
        currentHealth = Math.min(currentHealth, maxHealth);
    }

    /**
     * Get total damage (base + weapon)
     */
    public int getTotalDamage() {
        return baseDamage + weaponDamage;
    }

    /**
     * Get total defense (base + armor)
     */
    public int getTotalDefense() {
        return defense + armorDefense;
    }

    /**
     * Take damage with defense reduction
     */
    public void takeDamage(int damage) {
        int actualDamage = Math.max(1, damage - getTotalDefense());
        currentHealth = Math.max(0, currentHealth - actualDamage);
        System.out.println("Player took " + actualDamage + " damage! Health: " + currentHealth + "/" + maxHealth);
    }

    /**
     * Heal the player
     */
    public void heal(int amount) {
        int oldHealth = currentHealth;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
        int actualHeal = currentHealth - oldHealth;
        if (actualHeal > 0) {
            System.out.println("Player healed " + actualHeal + " HP! Health: " + currentHealth + "/" + maxHealth);
        }
    }

    /**
     * Fully restore health
     */
    public void fullHeal() {
        currentHealth = maxHealth;
    }

    /**
     * Add experience and check for level up
     */
    public boolean addExperience(int exp) {
        experience += exp;
        System.out.println("Gained " + exp + " XP! (" + experience + "/" + experienceToNextLevel + ")");

        if (experience >= experienceToNextLevel) {
            levelUp();
            return true;
        }
        return false;
    }

    /**
     * Level up and grant stat points
     */
    private void levelUp() {
        level++;
        experience -= experienceToNextLevel;
        experienceToNextLevel = (int) (experienceToNextLevel * 1.5f);

        // Grant stat points instead of automatic stat increases
        availableStatPoints += STAT_POINTS_PER_LEVEL;

        // Recalculate base stats for level
        recalculateStats();

        // Full heal on level up
        currentHealth = maxHealth;
    }

    /**
     * Check if player is dead
     */
    public boolean isDead() {
        return currentHealth <= 0;
    }

    /**
     * Set weapon damage bonus
     */
    public void setWeaponDamage(int damage) {
        this.weaponDamage = damage;
    }

    /**
     * Set armor defense bonus
     */
    public void setArmorDefense(int defense) {
        this.armorDefense = defense;
    }

    /**
     * Get health percentage (0-1)
     */
    public float getHealthPercentage() {
        return (float) currentHealth / maxHealth;
    }

    // Getters
    public int getMaxHealth() { return maxHealth; }
    public int getCurrentHealth() { return currentHealth; }
    public int getBaseDamage() { return baseDamage; }
    public int getDefense() { return defense; }
    public float getBaseSpeed() { return baseSpeed; }
    public int getWeaponDamage() { return weaponDamage; }
    public int getArmorDefense() { return armorDefense; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getExperienceToNextLevel() { return experienceToNextLevel; }
    public int getAvailableStatPoints() { return availableStatPoints; }
    public int getAllocatedHealthPoints() { return allocatedHealthPoints; }
    public int getAllocatedAttackPoints() { return allocatedAttackPoints; }
    public int getAllocatedDefensePoints() { return allocatedDefensePoints; }

    // Setters
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(maxHealth, health));
    }
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = Math.min(currentHealth, maxHealth);
    }

    public int getExpToNextLevel() {
        return experienceToNextLevel;
    }
}