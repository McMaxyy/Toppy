package entities;

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

    public EnemyStats(String enemyName, int level) {
        this(enemyName, level, AttackType.MELEE, "basic_enemy");
    }

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

    public void takeDamage(int incomingDamage) {
        int actualDamage = Math.max(1, incomingDamage - defense);
        currentHealth = Math.max(0, currentHealth - actualDamage);
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public float getHealthPercentage() {
        return (float) currentHealth / maxHealth;
    }

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

    public static class Factory {
        public static EnemyStats createMushieEnemy(int level) {
            return new EnemyStats(
                    "Mushie",
                    15 + (level * 10),
                    3 + (level * 2),
                    0 + level,
                    10 + (level * 8),
                    level,
                    AttackType.RANGED,
                    "ranged_enemy",
                    2.0f,
                    120f,
                    1.0f,
                    0f,
                    0f,
                    80f,
                    0f
            );
        }

        public static EnemyStats createWolfieEnemy(int level) {
            return new EnemyStats(
                    "Wolfie",
                    30 + (level * 20),
                    8 + (level * 4),
                    2 + level,
                    10 + (level * 8),
                    level,
                    AttackType.MELEE,
                    "basic_enemy",
                    1.5f,
                    35f,
                    1.0f,
                    0f,
                    0f,
                    0f,
                    0f
            );
        }

        public static EnemyStats createSkeletonEnemy(int level) {
            return new EnemyStats(
                    "Skeleton",
                    45 + (level * 20),
                    10 + (level * 5),
                    2 + level,
                    15 + (level * 10),
                    level,
                    AttackType.CONAL,
                    "dungeon_enemy",
                    1.5f,
                    35f,
                    1.0f,
                    60f,
                    0f,
                    0f,
                    0f
            );
        }

        public static EnemyStats createSkeletonRogueEnemy(int level) {
            return new EnemyStats(
                    "Skeleton Rogue",
                    35 + (level * 15),
                    12 + (level * 4),
                    1 + level,
                    12 + (level * 8),
                    level,
                    AttackType.MELEE,
                    "dungeon_enemy",
                    1.0f,
                    30f,
                    0.8f,
                    0f,
                    0f,
                    0f,
                    0f
            );
        }

        public static EnemyStats createBoss(int level) {
            return new EnemyStats(
                    "Boss Kitty",
                    500 + (level * 50),
                    15 + (level * 10),
                    5 + (level * 3),
                    150 + (level * 50),
                    level,
                    AttackType.MELEE,
                    "boss",
                    1.5f,
                    75f,
                    0.6f,
                    0f,
                    150f,
                    0f,
                    400f
            );
        }

        public static EnemyStats createCyclops(int level) {
            return new EnemyStats(
                    "Cyclops",
                    600 + (level * 50),
                    18 + (level * 6),
                    7 + (level * 3),
                    200 + (level * 60),
                    level,
                    AttackType.AOE,
                    "boss",
                    1.8f,
                    60f,
                    0.8f,
                    0f,
                    120f,
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