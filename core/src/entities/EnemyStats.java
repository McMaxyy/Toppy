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
                    35 + (level * 10),
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
                    90f,
                    0f
            );
        }

        public static EnemyStats createWolfieEnemy(int level) {
            return new EnemyStats(
                    "Wolfie",
                    40 + (level * 20),
                    8 + (level * 4),
                    2 + level,
                    10 + (level * 8),
                    level,
                    AttackType.MELEE,
                    "melee_enemy",
                    1f,
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
                    75 + (level * 20),
                    10 + (level * 5),
                    2 + level,
                    15 + (level * 10),
                    level,
                    AttackType.CONAL,
                    "dungeon_enemy",
                    1f,
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
                    55 + (level * 15),
                    12 + (level * 4),
                    1 + level,
                    12 + (level * 8),
                    level,
                    AttackType.MELEE,
                    "dungeon_enemy",
                    0.8f,
                    30f,
                    0.8f,
                    0f,
                    0f,
                    0f,
                    0f
            );
        }

        public static EnemyStats createSkeletonMageEnemy(int level) {
            return new EnemyStats(
                    "Skeleton Mage",
                    50 + (level * 12),
                    15 + (level * 6),
                    1 + level,
                    18 + (level * 12),
                    level,
                    AttackType.RANGED,
                    "dungeon_enemy",
                    2f,
                    150f,
                    1.0f,
                    0f,
                    0f,
                    130f,
                    0f
            );
        }

        public static EnemyStats createBoss(int level) {
            return new EnemyStats(
                    "Boss Kitty",
                    700 + (level * 50),
                    15 + (level * 5),
                    5 + (level * 3),
                    1500 + (level * 50),
                    level,
                    AttackType.MELEE,
                    "boss",
                    1f,
                    60f,
                    1f,
                    0f,
                    150f,
                    0f,
                    400f
            );
        }

        public static EnemyStats createCyclops(int level) {
            return new EnemyStats(
                    "Cyclops",
                    800 + (level * 50),
                    18 + (level * 3),
                    8 + (level * 2),
                    2500 + (level * 60),
                    level,
                    AttackType.AOE,
                    "boss",
                    1.4f,
                    60f,
                    1.4f,
                    0f,
                    120f,
                    0f,
                    0f
            );
        }

        public static EnemyStats createGhost(int level) {
            return new EnemyStats(
                    "Ghost",
                    25 + (level * 12),
                    12 + (level * 4),
                    1 + level,
                    15 + (level * 8),
                    level,
                    AttackType.AOE,
                    "ghost_enemy",
                    1.0f,
                    60f,
                    1.0f,
                    0f,
                    60f,
                    0f,
                    0f
            );
        }

        public static EnemyStats createGhostling(int level) {
            return new EnemyStats(
                    "Ghostling",
                    20 + (level * 8),
                    20 + (level * 3),
                    0,
                    5 + (level * 3),
                    level,
                    AttackType.AOE,
                    "ghost_enemy",
                    1.0f,
                    60f,
                    1.0f,
                    0f,
                    60f,
                    0f,
                    0f
            );
        }

        public static EnemyStats createGhostBoss(int level) {
            return new EnemyStats(
                    "Ghost Boss",
                    900 + (level * 50),
                    17 + (level * 3),
                    10 + (level * 2),
                    4500 + (level * 55),
                    level,
                    AttackType.RANGED,
                    "boss",
                    0.35f,
                    300f,
                    0.35f,
                    0f,
                    0f,
                    120f,
                    0f
            );
        }

        public static EnemyStats createHerman(int level) {
            return new EnemyStats(
                    "Herman",
                    1200 + (level * 60),
                    20 + (level * 5),
                    12 + (level * 4),
                    6000 + (level * 100),
                    level,
                    AttackType.RANGED,
                    "mega_boss",
                    0.25f,
                    400f,
                    0.25f,
                    0f,
                    40f,
                    150f,
                    0f
            );
        }
    }
}