package entities;

public enum EnemyType {
    MUSHIE("Mushie"),
    WOLFIE("Wolfie"),
    SKELETON("Skeleton"),
    BOSS_KITTY("BossKitty"),
    CYCLOPS("Cyclops"),
    MERCHANT("Merchant"),
    SKELETON_ROGUE("Skeleton Rogue"),
    SKELETON_MAGE("Skeleton Mage"),
    GHOST("Ghost"),
    GHOST_BOSS("GhostBoss"),
    HERMAN("Herman"),
    HEDGEHOG("Hedgehog"),
    LEMMY("Lemmy"),
    DESTRUCTABLE_OBJECT("Destructable");

    private final String animationKey;

    EnemyType(String animationKey) {
        this.animationKey = animationKey;
    }

    public String getAnimationKey() {
        return animationKey;
    }
}