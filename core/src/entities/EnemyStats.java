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

    /**
     * Create enemy stats with default values based on level
     */
    public EnemyStats(String enemyName, int level) {
        this(enemyName, level, "basic_enemy"); // Default loot table
    }

    /**
     * Create enemy stats with custom loot table
     */
    public EnemyStats(String enemyName, int level, String lootTableType) {
        this.enemyName = enemyName;
        this.level = level;
        this.lootTableType = lootTableType;

        // Scale stats based on level
        this.maxHealth = 20 + (level * 15);
        this.currentHealth = maxHealth;
        this.damage = 5 + (level * 3);
        this.defense = 0 + level;
        this.expReward = 10 + (level * 5);
    }

    /**
     * Create enemy stats with custom values
     */
    public EnemyStats(String enemyName, int maxHealth, int damage, int defense,
                      int expReward, int level, String lootTableType) {
        this.enemyName = enemyName;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.damage = damage;
        this.defense = defense;
        this.expReward = expReward;
        this.level = level;
        this.lootTableType = lootTableType;
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

    // Setters
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(maxHealth, health));
    }

    /**
     * Create stats for different enemy types
     */
    public static class Factory {

        public static EnemyStats createBasicEnemy(int level) {
            return new EnemyStats("Mushie", level, "basic_enemy");
        }

        public static EnemyStats createDungeonEnemy(int level) {
            return new EnemyStats(
                    "Dungeon Mushie",
                    30 + (level * 20),  // More health
                    8 + (level * 4),    // More damage
                    2 + level,          // More defense
                    20 + (level * 10),  // More exp
                    level,
                    "dungeon_enemy"     // Dungeon loot table
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
                    "boss"              // Boss loot table
            );
        }

        /**
         * Create a custom enemy with specific loot table
         */
        public static EnemyStats createCustomEnemy(String name, int level, String lootTableType) {
            return new EnemyStats(name, level, lootTableType);
        }
    }
}