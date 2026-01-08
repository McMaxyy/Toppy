package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
        wallTexture = Storage.assetManager.get("tiles/newrock.png", Texture.class);
        floorTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
        exitTexture = Storage.assetManager.get("enemy.png", Texture.class);
    }

    private void generateDungeon(Random random) {
        // Initialize all tiles as walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = WALL;
            }
        }

        // Drunkard's Walk algorithm
        int centerX = width / 2;
        int centerY = height / 2;
        int currentX = centerX;
        int currentY = centerY;

        spawnPoint = new Vector2(centerX * tileSize, centerY * tileSize);

        int floorTiles = 0;
        int targetFloorTiles = (int) ((width * height) * 0.4f); // 40% floor coverage

        while (floorTiles < targetFloorTiles) {
            // Carve floor tile
            if (tiles[currentX][currentY] == WALL) {
                tiles[currentX][currentY] = FLOOR;
                floorTiles++;
            }

            // Random walk direction
            int direction = random.nextInt(4);
            switch (direction) {
                case 0: // Up
                    if (currentY < height - 2) currentY++;
                    break;
                case 1: // Down
                    if (currentY > 1) currentY--;
                    break;
                case 2: // Right
                    if (currentX < width - 2) currentX++;
                    break;
                case 3: // Left
                    if (currentX > 1) currentX--;
                    break;
            }
        }

        // Place exit portal at a far floor tile
        exitPoint = findFarthestFloorTile(centerX, centerY);
        if (exitPoint != null) {
            int exitX = (int) (exitPoint.x / tileSize);
            int exitY = (int) (exitPoint.y / tileSize);
            tiles[exitX][exitY] = EXIT;
        }
    }

    private Vector2 findFarthestFloorTile(int startX, int startY) {
        float maxDistance = 0;
        Vector2 farthest = null;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == FLOOR) {
                    float distance = (float) Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        farthest = new Vector2(x * tileSize, y * tileSize);
                    }
                }
            }
        }

        return farthest;
    }

    private void createWalls() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == WALL) {
                    float worldX = x * tileSize;
                    float worldY = y * tileSize;

                    Body body = createWallBody(worldX, worldY);
                    walls.add(new Wall(new Rectangle(worldX, worldY, tileSize, tileSize), wallTexture, body));
                }
            }
        }
    }

    private Body createWallBody(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + tileSize / 2f, y + tileSize / 2f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(tileSize / 2f, tileSize / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.OBSTACLE;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ENEMY;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        shape.dispose();

        return body;
    }

    private void spawnEnemies(Random random) {
        int enemyCount = 15 + random.nextInt(10);
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
                    enemies.add(new DungeonEnemy(enemyBounds, body, player, animationManager, this));
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
            batch.draw(wall.texture, wall.bounds.x, wall.bounds.y, wall.bounds.width, wall.bounds.height);
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
        final Texture texture;
        final Body body;

        public Wall(Rectangle bounds, Texture texture, Body body) {
            this.bounds = bounds;
            this.texture = texture;
            this.body = body;
        }
    }
}
