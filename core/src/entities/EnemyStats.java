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
    private String lootTableType;

    // Attack attributes
    private float attackCooldown;
    private float attackRange;
    private float attackSpeed;
    private AttackType attackType;
    private float attackConeAngle;
    private float aoeRadius;
    private float projectileSpeed;
    private float chargeSpeed;

    /**
     * Create enemy stats with default values based on level
     */
    public EnemyStats(String enemyName, int level) {
        this(enemyName, level, AttackType.MELEE, "basic_enemy");
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

        /**
         * Create a basic Mushie enemy - RANGED attacker for overworld
         * Mushie fires green projectiles at the player
         */
        public static EnemyStats createBasicEnemy(int level) {
            return createMushieEnemy(level);
        }

        /**
         * Create Mushie enemy - Ranged attacker
         * Fires green circle projectiles at the player
         */
        public static EnemyStats createMushieEnemy(int level) {
            return new EnemyStats(
                    "Mushie",
                    15 + (level * 10),  // Less health (ranged enemy)
                    3 + (level * 2),    // Less damage per hit
                    0 + level,          // Less defense
                    15 + (level * 8),   // Medium exp
                    level,
                    AttackType.RANGED,  // RANGED - fires projectiles
                    "ranged_enemy",
                    2.0f,               // Attack cooldown (2 seconds between attacks)
                    120f,               // Long attack range
                    1.0f,               // Attack animation duration (1 second)
                    0f,                 // Not conal
                    0f,                 // Not AOE
                    80f,                // Projectile speed
                    0f                  // Not charge
            );
        }

        /**
         * Create Wolfie enemy - MELEE attacker for overworld
         * Has a circular attack indicator
         */
        public static EnemyStats createWolfieEnemy(int level) {
            return new EnemyStats(
                    "Wolfie",
                    30 + (level * 20),  // More health (melee enemy needs to get close)
                    8 + (level * 4),    // More damage
                    2 + level,          // More defense
                    20 + (level * 10),  // More exp
                    level,
                    AttackType.MELEE,   // MELEE - circular attack zone
                    "basic_enemy",
                    1.5f,               // Attack cooldown
                    25f,                // Short attack range (melee)
                    1.0f,               // Attack animation duration
                    0f,                 // Not conal
                    0f,                 // Not AOE
                    0f,                 // Not ranged
                    0f                  // Not charge
            );
        }

        /**
         * Create Skeleton enemy - CONAL attacker for dungeons
         * Has a cone-shaped attack indicator in front
         */
        public static EnemyStats createSkeletonEnemy(int level) {
            return new EnemyStats(
                    "Skeleton",
                    30 + (level * 20),  // Decent health
                    8 + (level * 4),    // Good damage
                    2 + level,          // Some defense
                    20 + (level * 10),  // Good exp
                    level,
                    AttackType.CONAL,   // CONAL - cone attack in front
                    "dungeon_enemy",
                    1.5f,               // Attack cooldown
                    35f,                // Medium attack range
                    1.0f,               // Attack animation duration
                    60f,                // 60 degree cone angle (30 degrees each side)
                    0f,                 // Not AOE
                    0f,                 // Not ranged
                    0f                  // Not charge
            );
        }

        /**
         * Create AOE enemy
         */
        public static EnemyStats createAOEEnemy(int level) {
            return new EnemyStats(
                    "AOE Mushie",
                    40 + (level * 25),
                    6 + (level * 3),
                    3 + level,
                    25 + (level * 12),
                    level,
                    AttackType.AOE,
                    "aoe_enemy",
                    3.0f,
                    40f,
                    0.8f,
                    0f,
                    50f,                // AOE radius
                    0f,
                    0f
            );
        }

        /**
         * Create Boss Kitty - powerful boss enemy
         */
        public static EnemyStats createBoss(int level) {
            return new EnemyStats(
                    "Boss Kitty",
                    200 + (level * 50),
                    20 + (level * 5),
                    5 + (level * 2),
                    100 + (level * 50),
                    level,
                    AttackType.CONAL,
                    "boss",
                    2.0f,
                    50f,
                    1.0f,
                    90f,                // Wide 90 degree cone
                    0f,
                    0f,
                    0f
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