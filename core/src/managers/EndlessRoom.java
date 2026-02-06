package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import entities.*;

public class EndlessRoom {
    private final int width;
    private final int height;
    private final int tileSize;
    private final int[][] tiles;
    private final List<Wall> walls;
    private final World world;
    private final Player player;
    private final AnimationManager animationManager;
    private final Random random;

    private Vector2 spawnPoint;
    private List<EndlessEnemy> enemies;

    private static final int FLOOR = 1;
    private static final int WALL = 0;

    private static final int ROOM_WIDTH = 40;
    private static final int ROOM_HEIGHT = 30;
    private static final int WALL_THICKNESS = 2;

    private static final float SPAWN_INTERVAL = 1.5f;
    private static final int MIN_ENEMIES_PER_CLUMP = 7;
    private static final int MAX_ENEMIES_PER_CLUMP = 10;
    private static final int CLUMPS_PER_WAVE = 10;

    private float spawnTimer = 0f;
    private int clumpsSpawned = 0;
    private int currentWave = 1;
    private int totalEnemiesKilled = 0;

    // Corner spawn positions
    private Vector2[] cornerPositions;

    private Texture wallTexture;
    private Texture floorTexture;

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

    private enum SpawnableEnemyType {
        WOLFIE, MUSHIE, SKELETON, SKELETON_MAGE, SKELETON_ROGUE, GHOST
    }

    public EndlessRoom(int tileSize, World world, Player player, AnimationManager animationManager) {
        this.width = ROOM_WIDTH + (WALL_THICKNESS * 2);
        this.height = ROOM_HEIGHT + (WALL_THICKNESS * 2);
        this.tileSize = tileSize;
        this.world = world;
        this.player = player;
        this.animationManager = animationManager;
        this.tiles = new int[width][height];
        this.walls = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.random = new Random();

        loadTextures();
        generateRoom();
        createWalls();
        calculateCornerPositions();
    }

    private void loadTextures() {
        wallTexture = Storage.assetManager.get("tiles/wallSprite2.png", Texture.class);
        wallTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        int tileWidth = wallTexture.getWidth() / 15;
        int tileHeight = wallTexture.getHeight();

        TextureRegion[][] wallFrames = TextureRegion.split(wallTexture, tileWidth, tileHeight);

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

        wallTJunctionTTexture = wallFrames[0][11];
        wallTJunctionRTexture = wallFrames[0][12];
        wallTJunctionBTexture = wallFrames[0][13];
        wallTJunctionLTexture = wallFrames[0][14];

        floorTexture = Storage.assetManager.get("tiles/stoneFloor4.png", Texture.class);
    }

    private void generateRoom() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = WALL;
            }
        }

        for (int x = WALL_THICKNESS; x < width - WALL_THICKNESS; x++) {
            for (int y = WALL_THICKNESS; y < height - WALL_THICKNESS; y++) {
                tiles[x][y] = FLOOR;
            }
        }

        int spawnX = width / 2;
        int spawnY = height / 2;
        spawnPoint = new Vector2(spawnX * tileSize, spawnY * tileSize);
    }

    private void calculateCornerPositions() {
        cornerPositions = new Vector2[4];

        float margin = tileSize * 3;

        cornerPositions[0] = new Vector2(WALL_THICKNESS * tileSize + margin,
                (height - WALL_THICKNESS) * tileSize - margin);
        cornerPositions[1] = new Vector2((width - WALL_THICKNESS) * tileSize - margin,
                (height - WALL_THICKNESS) * tileSize - margin);
        cornerPositions[2] = new Vector2(WALL_THICKNESS * tileSize + margin,
                WALL_THICKNESS * tileSize + margin);
        cornerPositions[3] = new Vector2((width - WALL_THICKNESS) * tileSize - margin,
                WALL_THICKNESS * tileSize + margin);
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

        if (edgeWallCount == 3) {
            if (hasWallLeft && hasWallRight && hasWallDown) return wallTJunctionTTexture;
            if (hasWallUp && hasWallDown && hasWallLeft) return wallTJunctionRTexture;
            if (hasWallLeft && hasWallRight && hasWallUp) return wallTJunctionBTexture;
            if (hasWallUp && hasWallDown && hasWallRight) return wallTJunctionLTexture;
        }

        if (hasWallDown && hasWallRight) return wallCornerTLTexture;
        if (hasWallDown && hasWallLeft) return wallCornerTRTexture;
        if (hasWallUp && hasWallRight) return wallCornerBLTexture;
        if (hasWallUp && hasWallLeft) return wallCornerBRTexture;

        if (hasWallLeft && hasWallRight) return wallHorizontalTexture;
        if (hasWallUp && hasWallDown) return wallVerticalTexture;

        if (hasWallRight) return wallEndLTexture;
        if (hasWallLeft) return wallEndRTexture;
        if (hasWallDown) return wallEndTTexture;
        if (hasWallUp) return wallEndBTexture;

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

            if (tiles[nx][ny] == FLOOR) {
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
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR |
                CollisionFilter.ENEMY | CollisionFilter.ITEM |
                CollisionFilter.PROJECTILE;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    public void update(float delta) {
        spawnTimer += delta;

        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnEnemyClump();
            spawnTimer = 0f;
            clumpsSpawned++;

            if (clumpsSpawned >= CLUMPS_PER_WAVE) {
                clumpsSpawned = 0;
                currentWave++;
            }
        }

        for (EndlessEnemy enemy : new ArrayList<>(enemies)) {
            if (!enemy.isMarkedForRemoval()) {
                enemy.update(delta);
            }
        }
    }

    private void spawnEnemyClump() {
        int clumpSize = MIN_ENEMIES_PER_CLUMP + random.nextInt(MAX_ENEMIES_PER_CLUMP - MIN_ENEMIES_PER_CLUMP + 1);

        Vector2 cornerPos = cornerPositions[random.nextInt(4)];

        for (int i = 0; i < clumpSize; i++) {
            float offsetX = random.nextFloat() * 60f - 30f;
            float offsetY = random.nextFloat() * 60f - 30f;

            float spawnX = cornerPos.x + offsetX;
            float spawnY = cornerPos.y + offsetY;

            spawnEnemy(spawnX, spawnY);
        }
    }

    private void spawnEnemy(float x, float y) {
        SpawnableEnemyType enemyType = SpawnableEnemyType.values()[random.nextInt(SpawnableEnemyType.values().length)];

        Body body = createEnemyBody(x, y);
        EnemyStats stats;
        EnemyType type;

        switch (enemyType) {
            case MUSHIE:
                stats = EnemyStats.Factory.createMushieEnemy(currentWave);
                type = EnemyType.MUSHIE;
                break;
            case SKELETON:
                stats = EnemyStats.Factory.createSkeletonEnemy(currentWave);
                type = EnemyType.SKELETON;
                break;
            case SKELETON_MAGE:
                stats = EnemyStats.Factory.createSkeletonMageEnemy(currentWave);
                type = EnemyType.SKELETON_MAGE;
                break;
            case SKELETON_ROGUE:
                stats = EnemyStats.Factory.createSkeletonRogueEnemy(currentWave);
                type = EnemyType.SKELETON_ROGUE;
                break;
            case GHOST:
                stats = EnemyStats.Factory.createGhost(currentWave);
                type = EnemyType.GHOST;
                break;
            case WOLFIE:
            default:
                stats = EnemyStats.Factory.createWolfieEnemy(currentWave);
                type = EnemyType.WOLFIE;
                break;
        }

        EndlessEnemy enemy = new EndlessEnemy(
                new Rectangle(x - 8, y - 8, 16, 16),
                body,
                player,
                animationManager,
                stats,
                type
        );

        body.setUserData(enemy);
        enemies.add(enemy);
    }

    private Body createEnemyBody(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        Body body = world.createBody(bodyDef);

        PolygonShape mainShape = new PolygonShape();
        mainShape.setAsBox(16 / 3.5f, 16 / 4f);

        FixtureDef mainFixtureDef = new FixtureDef();
        mainFixtureDef.shape = mainShape;
        mainFixtureDef.density = 1f;
        mainFixtureDef.friction = 0.3f;
        mainFixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        mainFixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR |
                CollisionFilter.ABILITY | CollisionFilter.WALL;
        body.createFixture(mainFixtureDef);
        mainShape.dispose();

        PolygonShape enemyCollisionShape = new PolygonShape();
        enemyCollisionShape.setAsBox(16 / 5f, 16 / 5f);

        FixtureDef enemyFixtureDef = new FixtureDef();
        enemyFixtureDef.shape = enemyCollisionShape;
        enemyFixtureDef.density = 0.5f;
        enemyFixtureDef.friction = 0.1f;
        enemyFixtureDef.filter.categoryBits = CollisionFilter.ENEMY_ENEMY;
        enemyFixtureDef.filter.maskBits = CollisionFilter.ENEMY_ENEMY;
        body.createFixture(enemyFixtureDef);
        enemyCollisionShape.dispose();

        body.setFixedRotation(true);
        return body;
    }

    public void render(SpriteBatch batch) {
        // Render floor
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == FLOOR) {
                    batch.draw(floorTexture, x * tileSize, y * tileSize, tileSize, tileSize);
                }
            }
        }

        for (Wall wall : walls) {
            if (wall.textureRegion != null) {
                batch.draw(wall.textureRegion, wall.bounds.x, wall.bounds.y, wall.bounds.width, wall.bounds.height);
            }
        }
    }

    public void renderEnemies(SpriteBatch batch, float delta) {
        for (EndlessEnemy enemy : new ArrayList<>(enemies)) {
            if (!enemy.isMarkedForRemoval()) {
                if (delta > 0) {
                    enemy.update(delta);
                }
                enemy.render(batch);
            }
        }
    }

    public Vector2 getSpawnPoint() {
        return spawnPoint;
    }

    public List<EndlessEnemy> getEnemies() {
        return enemies;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalEnemiesKilled() {
        return totalEnemiesKilled;
    }

    public void incrementKills() {
        totalEnemiesKilled++;
    }

    public void dispose() {
        for (Wall wall : walls) {
            if (wall.body != null) {
                try {
                    world.destroyBody(wall.body);
                } catch (Exception e) {
                }
            }
        }
        walls.clear();

        for (EndlessEnemy enemy : new ArrayList<>(enemies)) {
            if (enemy.getBody() != null) {
                try {
                    world.destroyBody(enemy.getBody());
                } catch (Exception e) {
                }
                enemy.clearBody();
            }
            enemy.dispose();
        }
        enemies.clear();
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