package entities;

/**
 * Enemy statistics and attributes
 */
public class EnemyStats {
    private String enemyName;
    private int maxHealth;
    private int currentHealth;
    private int damage;
    private int defense;
    private int expReward;
    private int level;
    private String lootTableType; // Which loot table to use

    // Attack attributes
    private float attackCooldown;      // Time between attacks
    private float attackRange;         // Range at which enemy can attack
    private float attackSpeed;         // Duration of attack animation (wind-up time)
    private AttackType attackType;     // Type of attack
    private float attackConeAngle;     // For conal attacks (in degrees)
    private float aoeRadius;           // For AOE attacks
    private float projectileSpeed;     // For ranged attacks
    private float chargeSpeed;

    /**
     * Create enemy stats with default values based on level
     */
    public EnemyStats(String enemyName, int level) {
        this(enemyName, level, AttackType.MELEE,"basic_enemy"); // Default loot table
    }

    /**
     * Create enemy stats with custom loot table
     */
    public EnemyStats(String enemyName, int level, AttackType attackType, String lootTableType) {
        this.enemyName = enemyName;
        this.level = level;
        this.attackType = attackType;
        this.lootTableType = lootTableType;

        // Scale stats based on level
        this.maxHealth = 20 + (level * 15);
        this.currentHealth = maxHealth;
        this.damage = 5 + (level * 3);
        this.defense = 0 + level;
        this.expReward = 10 + (level * 5);

        setDefaultAttackValues();
    }

    private void setDefaultAttackValues() {
        switch (attackType) {
            case CONAL:
                this.attackCooldown = 1.5f;
                this.attackRange = 30f;
                this.attackSpeed = 0.6f;
                this.attackConeAngle = 60f;
                break;

            case RANGED:
                this.attackCooldown = 2.0f;
                this.attackRange = 150f;
                this.attackSpeed = 0.4f;
                this.projectileSpeed = 100f;
                break;

            case AOE:
                this.attackCooldown = 3.0f;
                this.attackRange = 40f;
                this.attackSpeed = 0.8f;
                this.aoeRadius = 50f;
                break;

            case CHARGE:
                this.attackCooldown = 3.5f;
                this.attackRange = 100f;
                this.attackSpeed = 1.0f;
                this.chargeSpeed = 120f;
                break;

            case DOT:
                this.attackCooldown = 2.5f;
                this.attackRange = 25f;
                this.attackSpeed = 0.5f;
                break;

            case MELEE:
            default:
                this.attackCooldown = 1.2f;
                this.attackRange = 25f;
                this.attackSpeed = 0.5f;
                break;
        }
    }
    /**
     * Create enemy stats with custom values
     */
    public EnemyStats(String enemyName, int maxHealth, int damage, int defense,
                      int expReward, int level, AttackType attackType, String lootTableType,
                      float attackCooldown, float attackRange, float attackSpeed,
                      float attackConeAngle, float aoeRadius, float projectileSpeed, float chargeSpeed) {
        this.enemyName = enemyName;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.damage = damage;
        this.defense = defense;
        this.expReward = expReward;
        this.level = level;
        this.attackType = attackType;
        this.lootTableType = lootTableType;
        this.attackCooldown = attackCooldown;
        this.attackRange = attackRange;
        this.attackSpeed = attackSpeed;
        this.attackConeAngle = attackConeAngle;
        this.aoeRadius = aoeRadius;
        this.projectileSpeed = projectileSpeed;
        this.chargeSpeed = chargeSpeed;
    }

    /**
     * Take damage with defense reduction
     */
    public void takeDamage(int incomingDamage) {
        int actualDamage = Math.max(1, incomingDamage - defense);
        currentHealth = Math.max(0, currentHealth - actualDamage);

        System.out.println(enemyName + " took " + actualDamage + " damage! Health: " + currentHealth + "/" + maxHealth);
    }

    /**
     * Check if enemy is dead
     */
    public boolean isDead() {
        return currentHealth <= 0;
    }

    /**
     * Get health percentage (0-1)
     */
    public float getHealthPercentage() {
        return (float) currentHealth / maxHealth;
    }

    /**
     * Heal the enemy
     */
    public void heal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    // Getters
    public String getEnemyName() { return enemyName; }
    public int getMaxHealth() { return maxHealth; }
    public int getCurrentHealth() { return currentHealth; }
    public int getDamage() { return damage; }
    public int getDefense() { return defense; }
    public int getExpReward() { return expReward; }
    public int getLevel() { return level; }
    public String getLootTableType() { return lootTableType; }
    public float getAttackCooldown() { return attackCooldown; }
    public float getAttackRange() { return attackRange; }
    public float getAttackSpeed() { return attackSpeed; }
    public AttackType getAttackType() { return attackType; }
    public float getAttackConeAngle() { return attackConeAngle; }
    public float getAoeRadius() { return aoeRadius; }
    public float getProjectileSpeed() { return projectileSpeed; }
    public float getChargeSpeed() { return chargeSpeed; }

    // Setters
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(maxHealth, health));
    }

    public void setAttackCooldown(float attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    public void setAttackRange(float attackRange) {
        this.attackRange = attackRange;
    }

    public void setAttackSpeed(float attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public void setAttackType(AttackType attackType) {
        this.attackType = attackType;
    }

    public void setAttackConeAngle(float attackConeAngle) {
        this.attackConeAngle = attackConeAngle;
    }

    /**
     * Create stats for different enemy types
     */
    public static class Factory {

        public static EnemyStats createBasicEnemy(int level) {
            return new EnemyStats("Mushie", level, AttackType.CONAL, "basic_enemy");
        }

        public static EnemyStats createFastEnemy(int level) {
            EnemyStats stats = new EnemyStats("Fast Mushie", level, AttackType.MELEE, "fast_enemy");
            stats.setAttackCooldown(0.8f);
            stats.setAttackSpeed(0.3f);
            stats.setAttackRange(20f);
            return stats;
        }

        public static EnemyStats createDungeonEnemy(int level) {
            return new EnemyStats(
                    "Dungeon Mushie",
                    30 + (level * 20),  // More health
                    8 + (level * 4),    // More damage
                    2 + level,          // More defense
                    20 + (level * 10),  // More exp
                    level,
                    AttackType.CONAL,
                    "dungeon_enemy",    // Dungeon loot table
                    1.3f,               // Faster attack cooldown
                    35f,                // Slightly longer range
                    0.7f,               // Slower attack wind-up
                    75f,                // Wider cone angle
                    0f, 0f, 0f          // Not AOE, ranged, or charge
            );
        }

        public static EnemyStats createRangedEnemy(int level) {
            return new EnemyStats(
                    "Ranged Mushie",
                    15 + (level * 10),  // Less health
                    3 + (level * 2),    // Less damage
                    0 + level,          // Less defense
                    15 + (level * 8),   // Medium exp
                    level,
                    AttackType.RANGED,
                    "ranged_enemy",     // Ranged loot table
                    2.0f,               // Standard ranged cooldown
                    150f,               // Long range
                    0.4f,               // Fast wind-up
                    0f,                 // Not conal
                    0f,                 // Not AOE
                    100f,               // Projectile speed
                    0f                  // Not charge
            );
        }

        public static EnemyStats createAOEEnemy(int level) {
            return new EnemyStats(
                    "AOE Mushie",
                    40 + (level * 25),  // High health
                    6 + (level * 3),    // Medium damage
                    3 + level,          // High defense
                    25 + (level * 12),  // High exp
                    level,
                    AttackType.AOE,
                    "aoe_enemy",        // AOE loot table
                    3.0f,               // Long cooldown
                    40f,                // Medium range
                    0.8f,               // Slow wind-up
                    0f,                 // Not conal
                    50f,                // AOE radius
                    0f, 0f              // Not ranged or charge
            );
        }

        public static EnemyStats createBoss(int level) {
            return new EnemyStats(
                    "Boss Kitty",
                    200 + (level * 50), // Much more health
                    20 + (level * 5),   // High damage
                    5 + (level * 2),    // Good defense
                    100 + (level * 50), // Lots of exp
                    level,
                    AttackType.CONAL,   // Boss uses conal attack
                    "boss",             // Boss loot table
                    2.0f,               // Boss has slower but powerful attacks
                    50f,                // Longer range
                    1.0f,               // Slower wind-up
                    90f,                // Very wide cone
                    0f, 0f, 0f          // Not AOE, ranged, or charge
            );
        }

        /**
         * Create a custom enemy with specific attributes
         */
        public static EnemyStats createCustomEnemy(String name, int level, AttackType attackType, String lootTableType) {
            return new EnemyStats(name, level, attackType, lootTableType);
        }
    }
}