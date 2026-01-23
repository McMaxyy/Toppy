package entities;

public class PlayerStats {
    // Core stats
    private int maxHealth;
    private int currentHealth;
    private int baseDamage;
    private int defense;
    private float baseSpeed;

    private static final int BASE_MAX_HEALTH = 1000;
    private static final int BASE_DAMAGE = 20;
    private static final int BASE_DEFENSE = 1;
    private static final float BASE_SPEED = 5000f;
    private static final float SPEED_PER_DEX_POINT = 100f;

    private int allocatedHealthPoints;
    private int allocatedAttackPoints;
    private int allocatedDefensePoints;
    private int allocatedDexPoints;
    private int availableStatPoints;
    private static final int STAT_POINTS_PER_LEVEL = 5;
    private static final int HEALTH_PER_POINT = 10;
    private static final int ATTACK_PER_POINT = 2;
    private static final int DEFENSE_PER_POINT = 1;

    private int weaponDamage;
    private int armorDefense;

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
        // Default starting stats
        this.level = 1;
        this.maxHealth = BASE_MAX_HEALTH;
        this.currentHealth = BASE_MAX_HEALTH;
        this.baseDamage = BASE_DAMAGE;
        this.defense = BASE_DEFENSE;
        this.baseSpeed = BASE_SPEED;
        this.weaponDamage = 0;
        this.armorDefense = 0;
        this.experience = 0;
        this.experienceToNextLevel = 1000;
        this.healthRegenRate = 1f;
        this.regenTimer = 0f;

        // Stat allocation
        this.allocatedDexPoints = 0;
        this.allocatedHealthPoints = 0;
        this.allocatedAttackPoints = 0;
        this.allocatedDefensePoints = 0;
        this.availableStatPoints = 0;
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
        int levelBonus = (level - 1) * 5;

        int oldMaxHealth = maxHealth;
        maxHealth = BASE_MAX_HEALTH + levelBonus + (allocatedHealthPoints * HEALTH_PER_POINT);
        baseDamage = BASE_DAMAGE + (level - 1) + (allocatedAttackPoints * ATTACK_PER_POINT);
        defense = BASE_DEFENSE + (allocatedDefensePoints * DEFENSE_PER_POINT);

        // Calculate speed based on DEX points
        float oldSpeed = baseSpeed;
        baseSpeed = BASE_SPEED + (allocatedDexPoints * SPEED_PER_DEX_POINT);

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

    public int getTotalDamage() {
        return baseDamage + weaponDamage;
    }

    public int getTotalDefense() {
        return defense + armorDefense;
    }

    public void takeDamage(int damage) {
        int actualDamage = Math.max(1, damage - getTotalDefense());
        currentHealth = Math.max(0, currentHealth - actualDamage);
    }

    public void heal(int amount) {
        int oldHealth = currentHealth;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
        int actualHeal = currentHealth - oldHealth;
        if (actualHeal > 0) {
        }
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

    public void setWeaponDamage(int damage) {
        this.weaponDamage = damage;
    }

    public void setArmorDefense(int defense) {
        this.armorDefense = defense;
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
    public int getWeaponDamage() { return weaponDamage; }
    public int getArmorDefense() { return armorDefense; }
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