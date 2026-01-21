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
        this.maxHealth = 100;
        this.currentHealth = 100;
        this.baseDamage = 20;
        this.defense = 0;
        this.baseSpeed = 5000f;
        this.weaponDamage = 0;
        this.armorDefense = 0;
        this.experience = 0;
        this.experienceToNextLevel = 100;
        this.healthRegenRate = 1f;
        this.regenTimer = 0f;
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
     * Level up and increase stats
     */
    private void levelUp() {
        level++;
        experience -= experienceToNextLevel;
        experienceToNextLevel = (int) (experienceToNextLevel * 1.5f);

        // Increase stats on level up
        maxHealth += 20;
        currentHealth = maxHealth; // Full heal on level up
        baseDamage += 2;
        defense += 1;

        System.out.println("LEVEL UP! Now level " + level);
        System.out.println("Max Health: " + maxHealth);
        System.out.println("Base Damage: " + baseDamage);
        System.out.println("Defense: " + defense);
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

    // Setters
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(maxHealth, health));
    }
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = Math.min(currentHealth, maxHealth);
    }


}