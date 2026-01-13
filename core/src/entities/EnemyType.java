package entities;

/**
 * Enum representing different enemy types in the game
 */
public enum EnemyType {
    MUSHIE("Mushie"),
    WOLFIE("Wolfie"),
    SKELETON("Skeleton"),
    BOSS_KITTY("BossKitty");

    private final String animationKey;

    EnemyType(String animationKey) {
        this.animationKey = animationKey;
    }

    /**
     * Get the animation key used by AnimationManager
     */
    public String getAnimationKey() {
        return animationKey;
    }
}