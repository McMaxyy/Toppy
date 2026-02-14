package entities;

public enum PlayerClass {
    MERCENARY("Mercenary", ""),
    PALADIN("Paladin", "");

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
