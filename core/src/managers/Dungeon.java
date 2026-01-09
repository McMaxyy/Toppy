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
import entities.Player;

public class Dungeon {
    private final int width;
    private final int height;
    private final int tileSize;
    private final int[][] tiles; // 0 = wall, 1 = floor, 2 = exit
    private final List<Wall> walls;
    private final List<DungeonEnemy> enemies;
    private final Player player;
    private final AnimationManager animationManager;
    private final World world;
    private Vector2 spawnPoint;
    private Vector2 exitPoint;

    private static final int FLOOR = 1;
    private static final int WALL = 0;
    private static final int EXIT = 2;

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
        int tileWidth = wallTexture.getWidth() / 15; // Changed from 12 to 15
        int tileHeight = wallTexture.getHeight();

        TextureRegion[][] wallFrames = TextureRegion.split(wallTexture, tileWidth, tileHeight);

        // Extract each wall type from the sprite sheet
        wallSingleTexture = wallFrames[0][0];           // Position 0
        wallHorizontalTexture = wallFrames[0][1];       // Position 1
        wallVerticalTexture = wallFrames[0][2];         // Position 2
        wallCornerTLTexture = wallFrames[0][3];         // Position 3
        wallCornerTRTexture = wallFrames[0][4];         // Position 4
        wallCornerBLTexture = wallFrames[0][5];         // Position 5
        wallCornerBRTexture = wallFrames[0][6];         // Position 6
        wallEndTTexture = wallFrames[0][7];            // Position 7 - renamed
        wallEndBTexture = wallFrames[0][8];            // Position 8 - renamed
        wallEndLTexture = wallFrames[0][9];            // Position 9 - renamed
        wallEndRTexture = wallFrames[0][10];           // Position 10 - renamed

        // New T-junction textures
        wallTJunctionTTexture = wallFrames[0][11];     // Position 11 - walls: left, right, bottom
        wallTJunctionRTexture = wallFrames[0][12];     // Position 12 - walls: top, bottom, left
        wallTJunctionBTexture = wallFrames[0][13];     // Position 13 - walls: left, right, top
        wallTJunctionLTexture = wallFrames[0][14];     // Position 14 - walls: top, bottom, right

        floorTexture = Storage.assetManager.get("tiles/stoneFloor4.png", Texture.class);
        exitTexture = Storage.assetManager.get("enemy.png", Texture.class);
    }

    private void generateDungeon(Random random) {
        // Initialize all tiles as walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = WALL;
            }
        }

        // Generate rooms
        List<Room> rooms = new ArrayList<>();
        int numRooms = 15 + random.nextInt(6); // 12-17 rooms (increased from 8-12)
        int attempts = 0;
        int maxAttempts = 100;

        while (rooms.size() < numRooms && attempts < maxAttempts) {
            attempts++;

            // Random room size
            int roomWidth = 7 + random.nextInt(10);  // 5-10 tiles wide (reduced from 5-12)
            int roomHeight = 7 + random.nextInt(10); // 5-10 tiles tall (reduced from 5-12)

            // Random position
            int roomX = 2 + random.nextInt(width - roomWidth - 4);
            int roomY = 2 + random.nextInt(height - roomHeight - 4);

            Room newRoom = new Room(roomX, roomY, roomWidth, roomHeight);

            // Check if room overlaps with existing rooms (reduced spacing for shorter corridors)
            boolean overlaps = false;
            for (Room existingRoom : rooms) {
                if (newRoom.intersects(existingRoom)) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                // Carve out the room
                carveRoom(newRoom);

                // Connect to previous room with corridor
                if (!rooms.isEmpty()) {
                    Room prevRoom = rooms.get(rooms.size() - 1);
                    createCorridor(prevRoom.centerX(), prevRoom.centerY(),
                            newRoom.centerX(), newRoom.centerY(), random);
                }

                rooms.add(newRoom);
            }
        }

        // Set spawn point in first room
        if (!rooms.isEmpty()) {
            Room firstRoom = rooms.get(0);
            spawnPoint = new Vector2(firstRoom.centerX() * tileSize, firstRoom.centerY() * tileSize);
        }

        // Place exit portal in furthest room from spawn
        if (rooms.size() > 1) {
            Room furthestRoom = findFurthestRoom(rooms);
            exitPoint = new Vector2(furthestRoom.centerX() * tileSize, furthestRoom.centerY() * tileSize);
            tiles[furthestRoom.centerX()][furthestRoom.centerY()] = EXIT;
        }
    }

    private Room findFurthestRoom(List<Room> rooms) {
        if (rooms.isEmpty()) return null;

        Room firstRoom = rooms.get(0);
        Room furthestRoom = rooms.get(rooms.size() - 1); // Default to last room
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
        int corridorWidth = 2 + random.nextInt(2); // 2-3 tiles wide

        // Randomly choose horizontal-then-vertical or vertical-then-horizontal
        if (random.nextBoolean()) {
            // Horizontal then vertical
            carveTunnel(x1, x2, y1, true, corridorWidth);
            carveTunnel(y1, y2, x2, false, corridorWidth);
        } else {
            // Vertical then horizontal
            carveTunnel(y1, y2, x1, false, corridorWidth);
            carveTunnel(x1, x2, y2, true, corridorWidth);
        }
    }

    private void carveTunnel(int start, int end, int fixed, boolean horizontal, int tunnelWidth) {
        int min = Math.min(start, end);
        int max = Math.max(start, end);

        for (int i = min; i <= max; i++) {
            for (int w = 0; w < tunnelWidth; w++) {
                int x, y;
                if (horizontal) {
                    x = i;
                    y = fixed + w - tunnelWidth / 2;
                } else {
                    x = fixed + w - tunnelWidth / 2;
                    y = i;
                }

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    tiles[x][y] = FLOOR;
                }
            }
        }
    }

    // Helper class for rooms
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
            return x < other.x + other.width + 1 &&  // Reduced spacing from 2 to 1
                    x + width + 1 > other.x &&
                    y < other.y + other.height + 1 &&
                    y + height + 1 > other.y;
        }
    }

    private void createWalls() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == WALL && isEdgeWall(x, y)) {  // ONLY create edge walls
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

            // If adjacent to floor/exit
            if (tiles[nx][ny] == FLOOR || tiles[nx][ny] == EXIT) {
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
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ENEMY;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    private void spawnEnemies(Random random) {
        int enemyCount = 35 + random.nextInt(20); // 35-54 enemies (increased from 15-24)
        int spawned = 0;

        while (spawned < enemyCount) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            if (tiles[x][y] == FLOOR) {
                float worldX = x * tileSize;
                float worldY = y * tileSize;

                Rectangle enemyBounds = new Rectangle(worldX, worldY, 16, 16);
                if (!isNearSpawn(worldX, worldY, 50f)) {
                    Body body = createEnemyBody(worldX, worldY);
                    enemies.add(new DungeonEnemy(enemyBounds, body, player, animationManager, this, 2));
                    spawned++;
                }
            }
        }
    }

    private boolean isNearSpawn(float x, float y, float minDistance) {
        float dx = x - spawnPoint.x;
        float dy = y - spawnPoint.y;
        return Math.sqrt(dx * dx + dy * dy) < minDistance;
    }

    private Body createEnemyBody(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x + 8f, y + 8f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(16 / 3f, 16 / 3f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.OBSTACLE | CollisionFilter.ENEMY;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        body.setFixedRotation(true);
        shape.dispose();

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

            // Check 4 directions
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
        // Render floor
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == FLOOR) {
                    batch.draw(floorTexture, x * tileSize, y * tileSize, tileSize, tileSize);
                } else if (tiles[x][y] == EXIT) {
                    batch.draw(floorTexture, x * tileSize, y * tileSize, tileSize, tileSize);
                    // Draw exit portal with pulsing effect
                    float pulse = 1f + (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.2f;
                    batch.setColor(0.5f, 1f, 0.5f, 0.8f);
                    batch.draw(exitTexture, x * tileSize, y * tileSize, tileSize * pulse, tileSize * pulse);
                    batch.setColor(1, 1, 1, 1);
                }
            }
        }

        // Render walls
        for (Wall wall : walls) {
            if (wall.textureRegion != null) {
                // Edge wall - draw texture region
                batch.draw(wall.textureRegion, wall.bounds.x, wall.bounds.y, wall.bounds.width, wall.bounds.height);
            }
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

    public boolean isPlayerAtExit(Vector2 playerPos) {
        if (exitPoint == null) return false;

        float dx = playerPos.x - exitPoint.x;
        float dy = playerPos.y - exitPoint.y;
        return Math.sqrt(dx * dx + dy * dy) < tileSize;
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

    public void dispose() {
        for (Wall wall : walls) {
            world.destroyBody(wall.body);
        }
        for (DungeonEnemy enemy : enemies) {
            enemy.dispose();
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