package managers;

public class CollisionFilter {
    public static final short PLAYER = 0x0001;
    public static final short OBSTACLE = 0x0002;
    public static final short SPEAR = 0x0004;
    public static final short ENEMY = 0x0008;
    public static final short WALL = 0x0010;
    public static final short PROJECTILE = 0x0020;
    public static final short ABILITY = 0x0040;
    public static final short REFLECT = 0x0080;
    public static final short ENEMY_ENEMY = 0x0100;
    public static final short ITEM = 0x0200;
    public static final short DESTRUCTIBLE = 0x0400;
}