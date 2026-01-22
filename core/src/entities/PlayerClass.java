package entities;

public enum PlayerClass {
    MERCENARY("Mercenary", "A fierce warrior skilled with spears and aggressive combat"),
    PALADIN("Paladin", "A holy knight with divine powers and defensive abilities");

    private final String displayName;
    private final String description;

    PlayerClass(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
