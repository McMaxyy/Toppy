package entities;

/**
 * Enum representing different enemy types in the game
 */
public enum EnemyType {
    MUSHIE("Mushie"),
    WOLFIE("Wolfie"),
    SKELETON("Skeleton"),
    BOSS_KITTY("BossKitty"),
    CYCLOPS("Cyclops"),
    MERCHANT("Merchant"),
    SKELETON_ROGUE("Skeleton Rogue"),
    SKELETON_MAGE("Skeleton Mage");

    private final String animationKey;

    EnemyType(String animationKey) {
        this.animationKey = animationKey;
    }

    public String getAnimationKey() {
        return animationKey;
    }
}