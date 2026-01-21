package managers;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import config.Storage;
import entities.BossKitty;
import entities.Cyclops;
import entities.EnemyStats;
import entities.Player;
import entities.Portal;

/**
 * A dedicated boss room that players enter from the dungeon portal.
 * Contains the dungeon boss (either BossKitty or Cyclops) and spawns an exit portal upon boss defeat.
 */
public class BossRoom {
    private final int width;
    private final int height;
    private final int tileSize;
    private final int[][] tiles; // 0 = wall, 1 = floor, 2 = exit
    private final List<Wall> walls;
    private final World world;
    private final Player player;
    private final AnimationManager animationManager;

    private Vector2 spawnPoint;
    private Vector2 exitPoint;
    private BossKitty boss;
    private Cyclops cyclops;
    private Portal exitPortal;
    private boolean bossDefeated = false;

    private static final int FLOOR = 1;
    private static final int WALL = 0;
    private static final int EXIT = 2;

    // Room dimensions
    private static final int ROOM_WIDTH = 20;
    private static final int ROOM_HEIGHT = 15;
    private static final int WALL_THICKNESS = 2;

    // Boss type enum
    public enum BossType {
        BOSS_KITTY,
        CYCLOPS
    }
    private BossType currentBossType;

    // Textures
    private Texture wallTexture;
    private Texture floorTexture;
    private Texture exitTexture;

    // Wall segment textures
    private TextureRegion wallSingleTexture;
    private TextureRegion wallHorizontalTexture;
    private TextureRegion wallVerticalTexture;
    private TextureRegion wallCornerTLTexture;
    private TextureRegion wallCornerTRTexture;
    private TextureRegion wallCornerBLTexture;
    private TextureRegion wallCornerBRTexture;
    private TextureRegion wallTJunctionTTexture;
    private TextureRegion wallTJunctionBTexture;
    private TextureRegion wallTJunctionLTexture;
    private TextureRegion wallTJunctionRTexture;
    private TextureRegion wallEndTTexture;
    private TextureRegion wallEndBTexture;
    private TextureRegion wallEndLTexture;
    private TextureRegion wallEndRTexture;

    // Constructor with default boss (BossKitty)
    public BossRoom(int tileSize, World world, Player player, AnimationManager animationManager) {
        this(tileSize, world, player, animationManager, BossType.BOSS_KITTY);
    }

    // Constructor with specified boss type
    public BossRoom(int tileSize, World world, Player player, AnimationManager animationManager, BossType bossType) {
        this.width = ROOM_WIDTH + (WALL_THICKNESS * 2);
        this.height = ROOM_HEIGHT + (WALL_THICKNESS * 2);
        this.tileSize = tileSize;
        this.world = world;
        this.player = player;
        this.animationManager = animationManager;
        this.tiles = new int[width][height];
        this.walls = new ArrayList<>();
        this.currentBossType = bossType;

        loadTextures();
        generateRoom();
        createWalls();
        spawnBoss();
    }

    private void loadTextures() {
        // Load wall sprite sheet
        wallTexture = Storage.assetManager.get("tiles/wallSprite2.png", Texture.class);
        wallTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Split the sprite sheet - 15 tiles horizontally
        int tileWidth = wallTexture.getWidth() / 15;
        int tileHeight = wallTexture.getHeight();

        TextureRegion[][] wallFrames = TextureRegion.split(wallTexture, tileWidth, tileHeight);

        // Extract each wall type from the sprite sheet
        wallSingleTexture = wallFrames[0][0];
        wallHorizontalTexture = wallFrames[0][1];
        wallVerticalTexture = wallFrames[0][2];
        wallCornerTLTexture = wallFrames[0][3];
        wallCornerTRTexture = wallFrames[0][4];
        wallCornerBLTexture = wallFrames[0][5];
        wallCornerBRTexture = wallFrames[0][6];
        wallEndTTexture = wallFrames[0][7];
        wallEndBTexture = wallFrames[0][8];
        wallEndLTexture = wallFrames[0][9];
        wallEndRTexture = wallFrames[0][10];

        // T-junction textures
        wallTJunctionTTexture = wallFrames[0][11];
        wallTJunctionRTexture = wallFrames[0][12];
        wallTJunctionBTexture = wallFrames[0][13];
        wallTJunctionLTexture = wallFrames[0][14];

        floorTexture = Storage.assetManager.get("tiles/stoneFloor4.png", Texture.class);
        exitTexture = Storage.assetManager.get("tiles/Portal.png", Texture.class);
    }

    private void generateRoom() {
        // Initialize all tiles as walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = WALL;
            }
        }

        // Carve out the floor area (leave walls around edges)
        for (int x = WALL_THICKNESS; x < width - WALL_THICKNESS; x++) {
            for (int y = WALL_THICKNESS; y < height - WALL_THICKNESS; y++) {
                tiles[x][y] = FLOOR;
            }
        }

        // Set spawn point at bottom center of room
        int spawnX = width / 2;
        int spawnY = WALL_THICKNESS + 1;
        spawnPoint = new Vector2(spawnX * tileSize, spawnY * tileSize);

        // Exit point will be at center of room (spawns after boss defeat)
        exitPoint = new Vector2((width / 2) * tileSize, (height / 2) * tileSize);
    }

    private void createWalls() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == WALL && isEdgeWall(x, y)) {
                    float worldX = x * tileSize;
                    float worldY = y * tileSize;

                    TextureRegion wallTile = getWallTextureForTile(x, y);

                    Body body = createWallBody(worldX, worldY);
                    walls.add(new Wall(new Rectangle(worldX, worldY, tileSize, tileSize), wallTile, body));
                }
            }
        }
    }

    private TextureRegion getWallTextureForTile(int x, int y) {
        boolean hasWallUp = isEdgeWallNeighbor(x, y + 1);
        boolean hasWallDown = isEdgeWallNeighbor(x, y - 1);
        boolean hasWallLeft = isEdgeWallNeighbor(x - 1, y);
        boolean hasWallRight = isEdgeWallNeighbor(x + 1, y);

        int edgeWallCount = (hasWallUp ? 1 : 0) + (hasWallDown ? 1 : 0) +
                (hasWallLeft ? 1 : 0) + (hasWallRight ? 1 : 0);

        // T-JUNCTIONS
        if (edgeWallCount == 3) {
            if (hasWallLeft && hasWallRight && hasWallDown) return wallTJunctionTTexture;
            if (hasWallUp && hasWallDown && hasWallLeft) return wallTJunctionRTexture;
            if (hasWallLeft && hasWallRight && hasWallUp) return wallTJunctionBTexture;
            if (hasWallUp && hasWallDown && hasWallRight) return wallTJunctionLTexture;
        }

        // CORNERS
        if (hasWallDown && hasWallRight) return wallCornerTLTexture;
        if (hasWallDown && hasWallLeft) return wallCornerTRTexture;
        if (hasWallUp && hasWallRight) return wallCornerBLTexture;
        if (hasWallUp && hasWallLeft) return wallCornerBRTexture;

        // STRAIGHT WALLS
        if (hasWallLeft && hasWallRight) return wallHorizontalTexture;
        if (hasWallUp && hasWallDown) return wallVerticalTexture;

        // DEAD ENDS
        if (hasWallRight) return wallEndLTexture;
        if (hasWallLeft) return wallEndRTexture;
        if (hasWallDown) return wallEndTTexture;
        if (hasWallUp) return wallEndBTexture;

        // SINGLE WALL
        return wallSingleTexture;
    }

    private boolean isEdgeWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        if (tiles[x][y] != WALL) return false;

        int[][] allDirections = {
                {-1, 1}, {0, 1}, {1, 1},
                {-1, 0},         {1, 0},
                {-1, -1}, {0, -1}, {1, -1}
        };

        for (int[] dir : allDirections) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                return true;
            }

            if (tiles[nx][ny] == FLOOR || tiles[nx][ny] == EXIT) {
                return true;
            }
        }

        return false;
    }

    private boolean isEdgeWallNeighbor(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        return isEdgeWall(x, y);
    }

    private Body createWallBody(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + tileSize / 2f, y + tileSize / 2f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(tileSize / 2f, tileSize / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.WALL;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ENEMY;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    private void spawnBoss() {
        // Spawn boss at center of room
        float bossX = (width / 2) * tileSize;
        float bossY = (height / 2 + 2) * tileSize; // Slightly above center

        Body bossBody = createBossBody(bossX, bossY);

        switch (currentBossType) {
            case CYCLOPS:
                EnemyStats cyclopsStats = EnemyStats.Factory.createCyclops(3);
                cyclops = new Cyclops(
                        new Rectangle(bossX - 18, bossY - 18, 36, 36), // Slightly larger than BossKitty
                        bossBody,
                        player,
                        animationManager,
                        cyclopsStats
                );
                bossBody.setUserData(cyclops);
                System.out.println("Cyclops spawned in boss room at: " + bossX + ", " + bossY);
                break;

            case BOSS_KITTY:
            default:
                EnemyStats bossStats = EnemyStats.Factory.createBoss(3);
                boss = new BossKitty(
                        new Rectangle(bossX - 16, bossY - 16, 32, 32),
                        bossBody,
                        player,
                        animationManager,
                        bossStats
                );
                bossBody.setUserData(boss);
                System.out.println("Boss Kitty spawned in boss room at: " + bossX + ", " + bossY);
                break;
        }
    }

    private Body createBossBody(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        PolygonShape shape = new PolygonShape();
        // Cyclops is slightly larger
        float boxSize = (currentBossType == BossType.CYCLOPS) ? 14f : 12f;
        shape.setAsBox(boxSize, boxSize);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR |
                CollisionFilter.OBSTACLE | CollisionFilter.ENEMY | CollisionFilter.ABILITY;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        body.setFixedRotation(true);
        shape.dispose();

        return body;
    }

    /**
     * Called when the boss is defeated - spawns the exit portal
     */
    public void onBossDefeated() {
        if (bossDefeated) return;

        bossDefeated = true;
        boss = null;
        cyclops = null;

        // Mark exit tile
        int tileX = (int) (exitPoint.x / tileSize);
        int tileY = (int) (exitPoint.y / tileSize);

        if (tileX >= 0 && tileX < width && tileY >= 0 && tileY < height) {
            tiles[tileX][tileY] = EXIT;
        }

        // Create the exit portal
        exitPortal = new Portal(
                exitPoint.x - 16,
                exitPoint.y - 16,
                32,
                world,
                true // already cleared
        );

        System.out.println("Exit portal spawned in boss room!");
    }

    public void render(SpriteBatch batch) {
        // Render floor
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == FLOOR) {
                    batch.draw(floorTexture, x * tileSize, y * tileSize, tileSize, tileSize);
                } else if (tiles[x][y] == EXIT) {
                    batch.draw(floorTexture, x * tileSize, y * tileSize, tileSize, tileSize);
                    batch.draw(exitTexture, x * tileSize, y * tileSize, tileSize, tileSize);
                }
            }
        }

        // Render walls
        for (Wall wall : walls) {
            if (wall.textureRegion != null) {
                batch.draw(wall.textureRegion, wall.bounds.x, wall.bounds.y, wall.bounds.width, wall.bounds.height);
            }
        }
    }

    public void renderBoss(SpriteBatch batch, float delta) {
        if (boss != null && !boss.isMarkedForRemoval()) {
            if (delta > 0) {
                boss.update(delta);
            }
            boss.render(batch);
        }
        if (cyclops != null && !cyclops.isMarkedForRemoval()) {
            if (delta > 0) {
                cyclops.update(delta);
            }
            cyclops.render(batch);
        }
    }

    public void update(float delta) {
        // Update boss
        if (boss != null) {
            boss.update(delta);
        }
        if (cyclops != null) {
            cyclops.update(delta);
        }

        // Update exit portal if it exists
        if (exitPortal != null) {
            exitPortal.update(delta);
        }
    }

    public boolean isPlayerAtExit(Vector2 playerPos) {
        if (!bossDefeated || exitPoint == null) return false;

        float dx = playerPos.x - exitPoint.x;
        float dy = playerPos.y - exitPoint.y;
        return Math.sqrt(dx * dx + dy * dy) < tileSize;
    }

    public Vector2 getSpawnPoint() {
        return spawnPoint;
    }

    public BossKitty getBoss() {
        return boss;
    }

    public Cyclops getCyclops() {
        return cyclops;
    }

    public boolean hasBoss() {
        return (boss != null || cyclops != null) && !bossDefeated;
    }

    public boolean isBossDefeated() {
        return bossDefeated;
    }

    public void setBoss(BossKitty boss) {
        this.boss = boss;
    }

    public void setCyclops(Cyclops cyclops) {
        this.cyclops = cyclops;
    }

    public BossType getCurrentBossType() {
        return currentBossType;
    }

    public Portal getExitPortal() {
        return exitPortal;
    }

    public void dispose() {
        for (Wall wall : walls) {
            if (wall.body != null) {
                world.destroyBody(wall.body);
            }
        }

        if (boss != null) {
            if (boss.getBody() != null) {
                world.destroyBody(boss.getBody());
            }
            boss.dispose();
            boss = null;
        }

        if (cyclops != null) {
            if (cyclops.getBody() != null) {
                world.destroyBody(cyclops.getBody());
            }
            cyclops.dispose();
            cyclops = null;
        }

        if (exitPortal != null) {
            exitPortal.dispose(world);
            exitPortal = null;
        }
    }

    private static class Wall {
        final Rectangle bounds;
        final TextureRegion textureRegion;
        final Body body;

        public Wall(Rectangle bounds, TextureRegion textureRegion, Body body) {
            this.bounds = bounds;
            this.textureRegion = textureRegion;
            this.body = body;
        }
    }
}