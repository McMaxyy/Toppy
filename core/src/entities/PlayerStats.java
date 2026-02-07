package entities;

public class PlayerStats {
    private int maxHealth;
    private int currentHealth;
    private int baseDamage;
    private int defense;
    private float baseSpeed;
    private float allocatedAttackSpeed;
    private float gearAttackSpeed;
    private float coinMultiplier = 1.0f;

    private static final int BASE_VIT = 30;
    private static final int BASE_DEX = 45;
    private static final int BASE_DAMAGE = 20;
    private static final int BASE_DEFENSE = 1;

    private static final int HEALTH_PER_VIT = 10;
    private static final float SPEED_PER_DEX = 100f;

    private int allocatedHealthPoints;
    private int allocatedAttackPoints;
    private int allocatedDefensePoints;
    private int allocatedDexPoints;
    private int availableStatPoints;

    private int availableSkillPoints;
    private int totalSkillPointsEarned;
    private static final int STAT_POINTS_PER_LEVEL = 5;
    private static final int ATTACK_PER_POINT = 1;
    private static final int DEFENSE_PER_POINT = 1;

    private int gearDamage;
    private int gearDefense;
    private int gearVitality;
    private int gearDex;

    private int level;
    private int experience;
    private int experienceToNextLevel;

    private float healthRegenRate;
    private float regenTimer;

    private SpeedChangeListener speedChangeListener;
    private HealthChangeListener healthChangeListener;

    public interface SpeedChangeListener {
        void onSpeedChanged(float newSpeed);
    }

    public interface HealthChangeListener {
        void onHealthChanged(int amount);
    }

    public PlayerStats() {
        this.level = 3;
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
        this.availableSkillPoints = 5;
        this.totalSkillPointsEarned = 5;
        this.allocatedAttackSpeed = 0f;
        this.gearAttackSpeed = 0f;

        this.allocatedDexPoints = 5;
        this.allocatedHealthPoints = 5;
        this.allocatedAttackPoints = 5;
        this.allocatedDefensePoints = 5;
        this.availableStatPoints = 0;

        this.maxHealth = getTotalVit() * HEALTH_PER_VIT;
        this.currentHealth = maxHealth;
        this.baseSpeed = getTotalDex() * SPEED_PER_DEX;
    }

    public void setSpeedChangeListener(SpeedChangeListener listener) {
        this.speedChangeListener = listener;
    }

    public void setHealthChangeListener(HealthChangeListener listener) {
        this.healthChangeListener = listener;
    }

    public void update(float delta) {
//        if (currentHealth < maxHealth) {
//            regenTimer += delta;
//            if (regenTimer >= 1f) {
//                heal((int) healthRegenRate);
//                regenTimer = 0f;
//            }
//        }
    }

    public void addAndRemoveBuffs (String buff, boolean activate) {
        switch (buff) {
            case "Attack Potion":
                if (activate)
                    allocatedAttackPoints += 5;
                else
                    allocatedAttackPoints -= 5;
                recalculateStats();
                break;
            case "Defense Potion":
                if (activate)
                    allocatedDefensePoints += 5;
                else
                    allocatedDefensePoints -= 5;
                recalculateStats();
                break;
            case "Dex Potion":
                if (activate) {
                    allocatedDexPoints += 5;
                    gearAttackSpeed += 0.05f;
                }
                else {
                    allocatedDexPoints -= 5;
                    gearAttackSpeed -= 0.05f;
                }
                recalculateStats();
                break;
            case "Lucky Clover":
                if (activate)
                    coinMultiplier = 2.0f;
                else
                    coinMultiplier = 1.0f;
                break;
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
            allocatedAttackSpeed += 0.005f;
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
        allocatedAttackSpeed = 0f;
        recalculateStats();
    }

    private void recalculateStats() {
        int oldMaxHealth = maxHealth;

        maxHealth = getTotalVit() * HEALTH_PER_VIT;

        baseDamage = BASE_DAMAGE + (level - 1) + (allocatedAttackPoints * ATTACK_PER_POINT);

        defense = BASE_DEFENSE + (allocatedDefensePoints * DEFENSE_PER_POINT);

        float oldSpeed = baseSpeed;
        baseSpeed = getTotalDex() * SPEED_PER_DEX;

        if (oldMaxHealth > 0 && maxHealth != oldMaxHealth) {
            float healthPercent = (float) currentHealth / oldMaxHealth;
            currentHealth = Math.max(1, (int) (maxHealth * healthPercent));
        }

        currentHealth = Math.min(currentHealth, maxHealth);

        if (speedChangeListener != null && oldSpeed != baseSpeed) {
            speedChangeListener.onSpeedChanged(baseSpeed);
        }
    }

    public int getTotalVit() {
        return BASE_VIT + allocatedHealthPoints + gearVitality + (level - 1);
    }

    public int getTotalDex() {
        return BASE_DEX + allocatedDexPoints + gearDex;
    }

    public int getTotalDamage() {
        return baseDamage + gearDamage;
    }

    public int getTotalDefense() {
        return defense + gearDefense;
    }

    public float getTotalAttackSpeed() {
        return allocatedAttackSpeed + gearAttackSpeed;
    }

    public void takeDamage(int damage) {
        int actualDamage = Math.max(1, damage - (getTotalDefense() / 3));
        currentHealth = Math.max(0, currentHealth - actualDamage);

        if (healthChangeListener != null) {
            healthChangeListener.onHealthChanged(-actualDamage);
        }
    }

    public int getActualDamage() {
        return getTotalDamage() / 2;
    }

    public void heal(int amount) {
        int actualHeal = amount + getActualDamage();
        int oldHealth = currentHealth;
        currentHealth = Math.min(maxHealth, currentHealth + actualHeal);
        int healedAmount = currentHealth - oldHealth;

        if (healthChangeListener != null && healedAmount > 0) {
            healthChangeListener.onHealthChanged(healedAmount);
        }
    }

    public void fullHeal() {
        int oldHealth = currentHealth;
        currentHealth = maxHealth;
        int healedAmount = currentHealth - oldHealth;

        // Trigger health change callback
        if (healthChangeListener != null && healedAmount > 0) {
            healthChangeListener.onHealthChanged(healedAmount);
        }
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
        experienceToNextLevel = (int) (experienceToNextLevel * 2.5f);

        availableStatPoints += STAT_POINTS_PER_LEVEL;
        awardSkillPoint();

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
        this.gearAttackSpeed *= 0.001f;
        recalculateStats();
    }

    public void removeGearDex(int amount) {
        this.gearDex = Math.max(0, this.gearDex - amount);
        this.gearAttackSpeed -= 0.001f;
        recalculateStats();
    }

    public boolean useSkillPoint() {
        if (availableSkillPoints > 0) {
            availableSkillPoints--;
            return true;
        }
        return false;
    }

    public void refundSkillPoint() {
        availableSkillPoints++;
    }

    public void awardSkillPoint() {
        availableSkillPoints++;
        totalSkillPointsEarned++;
    }

    public float getHealthPercentage() {
        return (float) currentHealth / maxHealth;
    }
    public int getAvailableSkillPoints() { return availableSkillPoints; }

    public int getTotalSkillPointsEarned() { return totalSkillPointsEarned; }

    public int getDisplayVit() {
        return level + allocatedHealthPoints + gearVitality;
    }

    public int getDisplayAP() {
        return level + allocatedAttackPoints + gearDamage;
    }

    public int getDisplayDP() {
        return level + allocatedDefensePoints + gearDefense;
    }

    public int getDisplayDex() {
        return level + allocatedDexPoints + gearDex;
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
    public float getCoinMultiplier() { return coinMultiplier; }

    // Setters
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(maxHealth, health));
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = Math.min(currentHealth, maxHealth);
    }
}