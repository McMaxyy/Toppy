package entities;

public class PlayerStats {
    // Core stats
    private int maxHealth;
    private int currentHealth;
    private int baseDamage;
    private int defense;
    private float baseSpeed;

    // Base stats as points (what's displayed)
    private static final int BASE_VIT = 100;  // Starting VIT points
    private static final int BASE_DEX = 50;   // Starting DEX points
    private static final int BASE_DAMAGE = 20;
    private static final int BASE_DEFENSE = 1;

    // Conversion rates
    private static final int HEALTH_PER_VIT = 10;      // 1 VIT = 10 health
    private static final float SPEED_PER_DEX = 100f;   // 1 DEX = 100 speed

    private int allocatedHealthPoints;
    private int allocatedAttackPoints;
    private int allocatedDefensePoints;
    private int allocatedDexPoints;
    private int availableStatPoints;
    private static final int STAT_POINTS_PER_LEVEL = 5;
    private static final int ATTACK_PER_POINT = 2;
    private static final int DEFENSE_PER_POINT = 1;

    // Gear bonuses (unified for all equipment)
    private int gearDamage;
    private int gearDefense;
    private int gearVitality;  // This is in VIT points
    private int gearDex;     // This is in DEX points

    private int level;
    private int experience;
    private int experienceToNextLevel;

    private float healthRegenRate;
    private float regenTimer;

    private SpeedChangeListener speedChangeListener;

    public interface SpeedChangeListener {
        void onSpeedChanged(float newSpeed);
    }

    public PlayerStats() {
        this.level = 1;
        this.baseDamage = BASE_DAMAGE;
        this.defense = BASE_DEFENSE;
        this.gearDamage = 0;
        this.gearDefense = 0;
        this.gearVitality = 0;
        this.gearDex = 0;
        this.experience = 0;
        this.experienceToNextLevel = 1000;
        this.healthRegenRate = 1f;
        this.regenTimer = 0f;

        this.allocatedDexPoints = 0;
        this.allocatedHealthPoints = 0;
        this.allocatedAttackPoints = 0;
        this.allocatedDefensePoints = 0;
        this.availableStatPoints = 0;

        // Calculate initial health and speed from base stats
        this.maxHealth = getTotalVit() * HEALTH_PER_VIT;
        this.currentHealth = maxHealth;
        this.baseSpeed = getTotalDex() * SPEED_PER_DEX;
    }

    public void setSpeedChangeListener(SpeedChangeListener listener) {
        this.speedChangeListener = listener;
    }

    public void update(float delta) {
        if (currentHealth < maxHealth) {
            regenTimer += delta;
            if (regenTimer >= 1f) {
                heal((int) healthRegenRate);
                regenTimer = 0f;
            }
        }
    }

    public boolean allocateHealthPoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            allocatedHealthPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    public boolean allocateAttackPoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            allocatedAttackPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    public boolean allocateDefensePoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            allocatedDefensePoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    public boolean allocateDexPoint() {
        if (availableStatPoints > 0) {
            availableStatPoints--;
            allocatedDexPoints++;
            recalculateStats();
            return true;
        }
        return false;
    }

    public void resetStatPoints() {
        availableStatPoints += allocatedHealthPoints + allocatedAttackPoints + allocatedDefensePoints + allocatedDexPoints;
        allocatedHealthPoints = 0;
        allocatedAttackPoints = 0;
        allocatedDefensePoints = 0;
        allocatedDexPoints = 0;
        recalculateStats();
    }

    private void recalculateStats() {
        int oldMaxHealth = maxHealth;

        // Calculate max health from total VIT
        maxHealth = getTotalVit() * HEALTH_PER_VIT;

        // Calculate damage
        baseDamage = BASE_DAMAGE + (level - 1) + (allocatedAttackPoints * ATTACK_PER_POINT);

        // Calculate defense
        defense = BASE_DEFENSE + (allocatedDefensePoints * DEFENSE_PER_POINT);

        // Calculate speed from total DEX
        float oldSpeed = baseSpeed;
        baseSpeed = getTotalDex() * SPEED_PER_DEX;

        // Maintain health percentage when max health changes
        if (oldMaxHealth > 0 && maxHealth != oldMaxHealth) {
            float healthPercent = (float) currentHealth / oldMaxHealth;
            currentHealth = Math.max(1, (int) (maxHealth * healthPercent));
        }

        currentHealth = Math.min(currentHealth, maxHealth);

        // Notify listener of speed change
        if (speedChangeListener != null && oldSpeed != baseSpeed) {
            speedChangeListener.onSpeedChanged(baseSpeed);
        }
    }

    // Get total VIT points (base + allocated + gear)
    public int getTotalVit() {
        return BASE_VIT + allocatedHealthPoints + gearVitality + ((level - 1) * 5);
    }

    // Get total DEX points (base + allocated + gear)
    public int getTotalDex() {
        return BASE_DEX + allocatedDexPoints + gearDex;
    }

    public int getTotalDamage() {
        return baseDamage + gearDamage;
    }

    public int getTotalDefense() {
        return defense + gearDefense;
    }

    public void takeDamage(int damage) {
        int actualDamage = Math.max(1, damage - getTotalDefense());
        currentHealth = Math.max(0, currentHealth - actualDamage);
    }

    public void heal(int amount) {
        int oldHealth = currentHealth;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    public void fullHeal() {
        currentHealth = maxHealth;
    }

    public boolean addExperience(int exp) {
        experience += exp;

        if (experience >= experienceToNextLevel) {
            levelUp();
            return true;
        }
        return false;
    }

    private void levelUp() {
        level++;
        experience -= experienceToNextLevel;
        experienceToNextLevel = (int) (experienceToNextLevel * 2f);

        availableStatPoints += STAT_POINTS_PER_LEVEL;

        recalculateStats();

        currentHealth = maxHealth;
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    // Gear stat modifiers
    public void addGearDamage(int amount) {
        this.gearDamage += amount;
    }

    public void removeGearDamage(int amount) {
        this.gearDamage = Math.max(0, this.gearDamage - amount);
    }

    public void addGearDefense(int amount) {
        this.gearDefense += amount;
    }

    public void removeGearDefense(int amount) {
        this.gearDefense = Math.max(0, this.gearDefense - amount);
    }

    public void addGearVitality(int amount) {
        this.gearVitality += amount;
        recalculateStats();
    }

    public void removeGearVitality(int amount) {
        this.gearVitality = Math.max(0, this.gearVitality - amount);
        recalculateStats();
    }

    public void addGearDex(int amount) {
        this.gearDex += amount;
        recalculateStats();
    }

    public void removeGearDex(int amount) {
        this.gearDex = Math.max(0, this.gearDex - amount);
        recalculateStats();
    }

    public float getHealthPercentage() {
        return (float) currentHealth / maxHealth;
    }

    // Getters
    public int getMaxHealth() { return maxHealth; }
    public int getCurrentHealth() { return currentHealth; }
    public int getBaseDamage() { return baseDamage; }
    public int getDefense() { return defense; }
    public float getBaseSpeed() { return baseSpeed; }
    public int getGearDamage() { return gearDamage; }
    public int getGearDefense() { return gearDefense; }
    public int getGearVitality() { return gearVitality; }
    public int getGearDex() { return gearDex; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getExperienceToNextLevel() { return experienceToNextLevel; }
    public int getAvailableStatPoints() { return availableStatPoints; }
    public int getAllocatedHealthPoints() { return allocatedHealthPoints; }
    public int getAllocatedAttackPoints() { return allocatedAttackPoints; }
    public int getAllocatedDefensePoints() { return allocatedDefensePoints; }
    public int getAllocatedDexPoints() { return allocatedDexPoints; }

    // Setters
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(maxHealth, health));
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = Math.min(currentHealth, maxHealth);
    }
}