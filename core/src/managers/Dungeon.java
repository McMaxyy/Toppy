package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.HashSet;

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
import entities.DungeonEnemy;
import entities.EnemyStats;
import entities.EnemyType;
import entities.Player;
import entities.Portal;

public class Dungeon {
    private final int width;
    private final int height;
    private final int tileSize;
    private final int[][] tiles; // 0 = wall, 1 = floor, 2 = boss portal location
    private final List<Wall> walls;
    private final List<DungeonEnemy> enemies;
    private final Player player;
    private final AnimationManager animationManager;
    private final World world;
    private Vector2 spawnPoint;

    // Boss room portal (leads to boss room, not the exit)
    private Portal bossRoomPortal;
    private Vector2 bossPortalPoint;
    private Room bossPortalRoom;

    private static final int FLOOR = 1;
    private static final int WALL = 0;
    private static final int BOSS_PORTAL = 2;

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

    public Dungeon(int width, int height, int tileSize, Random random, World world, Player player, AnimationManager animationManager) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.world = world;
        this.player = player;
        this.animationManager = animationManager;
        this.tiles = new int[width][height];
        this.walls = new ArrayList<>();
        this.enemies = new ArrayList<>();

        loadTextures();
        generateDungeon(random);
        createWalls();
        spawnEnemies(random);
    }

    private void loadTextures() {
        // Load wall sprite sheet
        wallTexture = Storage.assetManager.get("tiles/wallSprite2.png", Texture.class);
        wallTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Split the sprite sheet - now 15 tiles horizontally (0-14)
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

    private void generateDungeon(Random random) {
        // Initialize all tiles as walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = WALL;
            }
        }

        List<Room> rooms = new ArrayList<>();
        int numRooms = 15 + random.nextInt(6);
        int attempts = 0;
        int maxAttempts = 100;

        while (rooms.size() < numRooms && attempts < maxAttempts) {
            attempts++;

            int roomWidth = 12 + random.nextInt(10);
            int roomHeight = 12 + random.nextInt(10);

            int roomX = 2 + random.nextInt(width - roomWidth - 4);
            int roomY = 2 + random.nextInt(height - roomHeight - 4);

            Room newRoom = new Room(roomX, roomY, roomWidth, roomHeight);

            boolean overlaps = false;
            for (Room existingRoom : rooms) {
                if (newRoom.intersects(existingRoom)) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                carveRoom(newRoom);

                if (!rooms.isEmpty()) {
                    Room prevRoom = rooms.get(rooms.size() - 1);
                    createCorridor(prevRoom.centerX(), prevRoom.centerY(),
                            newRoom.centerX(), newRoom.centerY(), random);
                }

                rooms.add(newRoom);
            }
        }

        if (!rooms.isEmpty()) {
            Room firstRoom = rooms.get(0);
            spawnPoint = new Vector2(firstRoom.centerX() * tileSize, firstRoom.centerY() * tileSize);
        }

        if (rooms.size() > 1) {
            Room furthestRoom = findFurthestRoom(rooms);
            placeBossPortal(furthestRoom);
        }
    }

    private void placeBossPortal(Room room) {
        bossPortalRoom = room;
        bossPortalPoint = new Vector2(room.centerX() * tileSize, room.centerY() * tileSize);

        int tileX = room.centerX();
        int tileY = room.centerY();
        if (tileX >= 0 && tileX < width && tileY >= 0 && tileY < height) {
            tiles[tileX][tileY] = BOSS_PORTAL;
        }

        bossRoomPortal = new Portal(
                bossPortalPoint.x - 16,
                bossPortalPoint.y - 16,
                32,
                world,
                false
        );
    }

    private Room findFurthestRoom(List<Room> rooms) {
        if (rooms.isEmpty()) return null;

        Room firstRoom = rooms.get(0);
        Room furthestRoom = rooms.get(rooms.size() - 1);
        float maxDistance = 0;

        for (Room room : rooms) {
            float dx = room.centerX() - firstRoom.centerX();
            float dy = room.centerY() - firstRoom.centerY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance > maxDistance) {
                maxDistance = distance;
                furthestRoom = room;
            }
        }

        return furthestRoom;
    }

    private void carveRoom(Room room) {
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    tiles[x][y] = FLOOR;
                }
            }
        }
    }

    private void createCorridor(int x1, int y1, int x2, int y2, Random random) {
        int corridorWidth = 2 + random.nextInt(2);

        if (random.nextBoolean()) {
            carveTunnel(x1, x2, y1, true, corridorWidth);
            carveTunnel(y1, y2, x2, false, corridorWidth);
        } else {
            carveTunnel(y1, y2, x1, false, corridorWidth);
            carveTunnel(x1, x2, y2, true, corridorWidth);
        }
    }

    private void carveTunnel(int start, int end, int fixedCoord, boolean horizontal, int width) {
        int min = Math.min(start, end);
        int max = Math.max(start, end);

        for (int i = min; i <= max; i++) {
            for (int w = 0; w < width; w++) {
                if (horizontal) {
                    if (fixedCoord + w >= 0 && fixedCoord + w < height && i >= 0 && i < this.width) {
                        tiles[i][fixedCoord + w] = FLOOR;
                    }
                } else {
                    if (fixedCoord + w >= 0 && fixedCoord + w < this.width && i >= 0 && i < height) {
                        tiles[fixedCoord + w][i] = FLOOR;
                    }
                }
            }
        }
    }

    private void createWalls() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == WALL && isEdgeWall(x, y)) {
                    float worldX = x * tileSize;
                    float worldY = y * tileSize;

                    TextureRegion wallTile = getWallTextureForTile(x, y, true);

                    Body body = createWallBody(worldX, worldY);
                    walls.add(new Wall(new Rectangle(worldX, worldY, tileSize, tileSize),
                            wallTile, body));
                }
            }
        }
    }

    private TextureRegion getWallTextureForTile(int x, int y, boolean isEdge) {
        if (!isEdge) return null;

        boolean hasWallUp = isEdgeWallNeighbor(x, y + 1);
        boolean hasWallDown = isEdgeWallNeighbor(x, y - 1);
        boolean hasWallLeft = isEdgeWallNeighbor(x - 1, y);
        boolean hasWallRight = isEdgeWallNeighbor(x + 1, y);

        int edgeWallCount = (hasWallUp ? 1 : 0) + (hasWallDown ? 1 : 0) +
                (hasWallLeft ? 1 : 0) + (hasWallRight ? 1 : 0);

        // === T-JUNCTIONS ===
        if (edgeWallCount == 3) {
            if (hasWallLeft && hasWallRight && hasWallDown) return wallTJunctionTTexture;
            if (hasWallUp && hasWallDown && hasWallLeft) return wallTJunctionRTexture;
            if (hasWallLeft && hasWallRight && hasWallUp) return wallTJunctionBTexture;
            if (hasWallUp && hasWallDown && hasWallRight) return wallTJunctionLTexture;
        }

        // === CORNERS ===
        if (hasWallDown && hasWallRight) return wallCornerTLTexture;
        if (hasWallDown && hasWallLeft) return wallCornerTRTexture;
        if (hasWallUp && hasWallRight) return wallCornerBLTexture;
        if (hasWallUp && hasWallLeft) return wallCornerBRTexture;

        // === STRAIGHT WALLS ===
        if (hasWallLeft && hasWallRight) return wallHorizontalTexture;
        if (hasWallUp && hasWallDown) return wallVerticalTexture;

        // === DEAD ENDS ===
        if (hasWallRight) return wallEndLTexture;
        if (hasWallLeft) return wallEndRTexture;
        if (hasWallDown) return wallEndTTexture;
        if (hasWallUp) return wallEndBTexture;

        // === SINGLE WALL ===
        return wallSingleTexture;
    }

    private boolean isWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return true;
        return tiles[x][y] == WALL;
    }

    private boolean isEdgeWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        if (tiles[x][y] != WALL) return false;

        // Check 8 directions (including diagonals)
        int[][] allDirections = {
                {-1, 1}, {0, 1}, {1, 1},
                {-1, 0},         {1, 0},
                {-1, -1}, {0, -1}, {1, -1}
        };

        for (int[] dir : allDirections) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            // If out of bounds, this is an edge (outside corner touching void)
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                return true;
            }

            // If adjacent to floor or boss portal
            if (tiles[nx][ny] == FLOOR || tiles[nx][ny] == BOSS_PORTAL) {
                return true;
            }
        }

        return false;
    }

    private boolean isEdgeWallNeighbor(int x, int y) {
        // Only check for edge walls, not interior walls
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
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ENEMY | CollisionFilter.ITEM;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    private void spawnEnemies(Random random) {
        int clumpCount = 24 + random.nextInt(6);
        int totalSkeletonsSpawned = 0;
        int totalRoguesSpawned = 0;

        for (int clump = 0; clump < clumpCount; clump++) {
            int attempts = 0;
            boolean clumpPlaced = false;

            while (!clumpPlaced && attempts < 20) {
                attempts++;

                int centerX = random.nextInt(width);
                int centerY = random.nextInt(height);

                if (tiles[centerX][centerY] == FLOOR) {
                    float centerWorldX = centerX * tileSize;
                    float centerWorldY = centerY * tileSize;

                    if (!isNearSpawn(centerWorldX, centerWorldY, 100f) &&
                            !isNearBossPortal(centerWorldX, centerWorldY, 80f)) {
                        int enemiesInClump = 4 + random.nextInt(7);

                        for (int i = 0; i < enemiesInClump; i++) {
                            int enemyAttempts = 0;
                            boolean enemyPlaced = false;

                            while (!enemyPlaced && enemyAttempts < 10) {
                                enemyAttempts++;

                                int offsetX = random.nextInt(5) - 2;
                                int offsetY = random.nextInt(5) - 2;

                                int enemyX = centerX + offsetX;
                                int enemyY = centerY + offsetY;

                                if (enemyX >= 0 && enemyX < width && enemyY >= 0 && enemyY < height &&
                                        tiles[enemyX][enemyY] == FLOOR) {

                                    float worldX = enemyX * tileSize;
                                    float worldY = enemyY * tileSize;

                                    Body body = createEnemyBody(worldX, worldY);

                                    // 40% chance to spawn Skeleton Rogue, 60% regular Skeleton
                                    if (random.nextFloat() < 0.4f) {
                                        EnemyStats stats = EnemyStats.Factory.createSkeletonRogueEnemy(2);
                                        enemies.add(new DungeonEnemy(
                                                new Rectangle(worldX, worldY, 16, 16),
                                                body,
                                                player,
                                                animationManager,
                                                this,
                                                stats,
                                                EnemyType.SKELETON_ROGUE
                                        ));
                                        totalRoguesSpawned++;
                                    } else {
                                        EnemyStats stats = EnemyStats.Factory.createSkeletonEnemy(2);
                                        enemies.add(new DungeonEnemy(
                                                new Rectangle(worldX, worldY, 16, 16),
                                                body,
                                                player,
                                                animationManager,
                                                this,
                                                stats,
                                                EnemyType.SKELETON
                                        ));
                                        totalSkeletonsSpawned++;
                                    }

                                    enemyPlaced = true;
                                }
                            }
                        }

                        clumpPlaced = true;
                    }
                }
            }
        }
    }

    private boolean isNearSpawn(float x, float y, float minDistance) {
        float dx = x - spawnPoint.x;
        float dy = y - spawnPoint.y;
        return Math.sqrt(dx * dx + dy * dy) < minDistance;
    }

    private boolean isNearBossPortal(float x, float y, float minDistance) {
        if (bossPortalPoint == null) return false;
        float dx = x - bossPortalPoint.x;
        float dy = y - bossPortalPoint.y;
        return Math.sqrt(dx * dx + dy * dy) < minDistance;
    }

    private Body createEnemyBody(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x + 8f, y + 8f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(16 / 4f, 16 / 4f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR |
                CollisionFilter.OBSTACLE | CollisionFilter.ABILITY | CollisionFilter.WALL;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        body.setFixedRotation(true);
        shape.dispose();

        PolygonShape enemyCollisionShape = new PolygonShape();
        enemyCollisionShape.setAsBox(width / 50f, height / 50f);

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

    // A* Pathfinding implementation
    public List<Vector2> findPath(Vector2 start, Vector2 end) {
        int startX = (int) (start.x / tileSize);
        int startY = (int) (start.y / tileSize);
        int endX = (int) (end.x / tileSize);
        int endY = (int) (end.y / tileSize);

        if (!isWalkable(startX, startY) || !isWalkable(endX, endY)) {
            return new ArrayList<>();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        HashSet<String> closedSet = new HashSet<>();
        HashMap<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startX, startY, 0, heuristic(startX, startY, endX, endY), null);
        openSet.add(startNode);
        allNodes.put(startX + "," + startY, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.x == endX && current.y == endY) {
                return reconstructPath(current);
            }

            closedSet.add(current.x + "," + current.y);

            int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

            for (int[] dir : directions) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                if (!isWalkable(nx, ny) || closedSet.contains(nx + "," + ny)) {
                    continue;
                }

                float newG = current.g + 1;
                String key = nx + "," + ny;

                Node neighbor = allNodes.get(key);
                if (neighbor == null) {
                    neighbor = new Node(nx, ny, newG, heuristic(nx, ny, endX, endY), current);
                    allNodes.put(key, neighbor);
                    openSet.add(neighbor);
                } else if (newG < neighbor.g) {
                    neighbor.g = newG;
                    neighbor.parent = current;
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return new ArrayList<>();
    }

    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        return tiles[x][y] != WALL;
    }

    public boolean isWalkableWorld(float worldX, float worldY) {
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);
        return isWalkable(tileX, tileY);
    }

    private float heuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private List<Vector2> reconstructPath(Node node) {
        List<Vector2> path = new ArrayList<>();
        while (node != null) {
            path.add(0, new Vector2(node.x * tileSize + tileSize / 2f, node.y * tileSize + tileSize / 2f));
            node = node.parent;
        }
        return path;
    }

    private static class Node implements Comparable<Node> {
        int x, y;
        float g, h;
        Node parent;

        Node(int x, int y, float g, float h, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        float f() {
            return g + h;
        }

        @Override
        public int compareTo(Node other) {
            return Float.compare(this.f(), other.f());
        }
    }

    public void render(SpriteBatch batch) {
        Texture pixel = Storage.assetManager.get("white_pixel.png", Texture.class);
        batch.setColor(0, 0, 0, 1);
        batch.draw(pixel, 0, 0, width * tileSize, height * tileSize);
        batch.setColor(1, 1, 1, 1);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == FLOOR) {
                    batch.draw(floorTexture, x * tileSize, y * tileSize, tileSize, tileSize);
                } else if (tiles[x][y] == BOSS_PORTAL) {
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

    public void renderPortal(SpriteBatch batch, float delta) {
        if (bossRoomPortal != null) {
            bossRoomPortal.update(delta);
            bossRoomPortal.render(batch);
        }
    }

    public void renderEnemies(SpriteBatch batch, float delta) {
        for (DungeonEnemy enemy : enemies) {
            enemy.update(delta);
            enemy.render(batch);
        }
    }

    public void updateEnemies() {
        enemies.removeIf(DungeonEnemy::isMarkedForRemoval);
    }

    /**
     * Check if player is at the boss room portal
     */
    public boolean isPlayerAtBossPortal(Vector2 playerPos) {
        if (bossPortalPoint == null || bossRoomPortal == null) return false;

        float dx = playerPos.x - bossPortalPoint.x;
        float dy = playerPos.y - bossPortalPoint.y;
        return Math.sqrt(dx * dx + dy * dy) < tileSize;
    }

    /**
     * Get the boss room portal
     */
    public Portal getBossRoomPortal() {
        return bossRoomPortal;
    }

    public Vector2 getSpawnPoint() {
        return spawnPoint;
    }

    public List<DungeonEnemy> getEnemies() {
        return enemies;
    }

    public int getTileType(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return WALL;
        }
        return tiles[x][y];
    }

    public int getTileSize() {
        return tileSize;
    }

    public void dispose() {
        // Destroy wall bodies
        for (Wall wall : walls) {
            if (wall.body != null) {
                world.destroyBody(wall.body);
            }
        }
        walls.clear();

        // Destroy enemy bodies and dispose enemies
        for (DungeonEnemy enemy : enemies) {
            if (enemy.getBody() != null) {
                world.destroyBody(enemy.getBody());
                enemy.clearBody();
            }
            enemy.dispose();
        }
        enemies.clear();

        // Dispose boss room portal if exists
        if (bossRoomPortal != null) {
            bossRoomPortal.dispose(world);
            bossRoomPortal = null;
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

    private static class Room {
        int x, y, width, height;

        Room(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int centerX() {
            return x + width / 2;
        }

        int centerY() {
            return y + height / 2;
        }

        boolean intersects(Room other) {
            return x < other.x + other.width + 2 &&
                    x + width + 2 > other.x &&
                    y < other.y + other.height + 2 &&
                    y + height + 2 > other.y;
        }
    }
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}