package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
    private static final int WALL_THICKNESS = 1;

    private static final float SPAWN_INTERVAL = 1.5f;
    private static final int MIN_ENEMIES_PER_CLUMP = 7;
    private static final int MAX_ENEMIES_PER_CLUMP = 10;
    private static final int CLUMPS_PER_WAVE = 10;
    private static final int GRACE_PERIOD_INTERVAL = 2;

    private float spawnTimer = 0f;
    private int clumpsSpawned = 0;
    private int currentWave = 1;
    private int totalEnemiesKilled = 0;
    private boolean waveInProgress = true;
    private boolean waveSpawningComplete = false;

    private boolean inGracePeriod = false;

    private ShapeRenderer shapeRenderer;
    private BitmapFont buttonFont;
    private Rectangle nextWaveButtonBounds;
    private boolean buttonHovered = false;
    private Texture woodTexture;

    // Button styling
    private static final float BUTTON_WIDTH = 200f;
    private static final float BUTTON_HEIGHT = 50f;
    private static final Color BUTTON_COLOR = new Color(0.55f, 0.35f, 0.15f, 1f); // Wood brown
    private static final Color BUTTON_BORDER_COLOR = new Color(0.35f, 0.2f, 0.1f, 1f); // Dark wood
    private static final Color BUTTON_HOVER_COLOR = new Color(0.65f, 0.45f, 0.2f, 1f); // Lighter wood
    private static final Color BUTTON_TEXT_COLOR = new Color(1f, 0.95f, 0.8f, 1f); // Parchment text

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
        WOLFIE, MUSHIE, SKELETON, SKELETON_MAGE, SKELETON_ROGUE, GHOST, HEDGEHOG
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

        this.shapeRenderer = new ShapeRenderer();
        this.buttonFont = Storage.assetManager.get("fonts/CascadiaBold.fnt", BitmapFont.class);
        this.nextWaveButtonBounds = new Rectangle();

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
        if (inGracePeriod) {
            updateGracePeriod(delta);
            return;
        }

        if (waveInProgress && !waveSpawningComplete) {
            spawnTimer += delta;

            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnEnemyClump();
                spawnTimer = 0f;
                clumpsSpawned++;

                if (clumpsSpawned >= CLUMPS_PER_WAVE) {
                    waveSpawningComplete = true;
                }
            }
        }

        if (waveSpawningComplete && enemies.isEmpty()) {
            onWaveComplete();
        }

        for (EndlessEnemy enemy : new ArrayList<>(enemies)) {
            if (!enemy.isMarkedForRemoval()) {
                enemy.update(delta);
            }
        }
    }

    private void onWaveComplete() {
        if (currentWave % GRACE_PERIOD_INTERVAL == 0) {
            startGracePeriod();
        } else {
            startNextWave();
        }
    }

    private void startGracePeriod() {
        inGracePeriod = true;
    }

    private void updateGracePeriod(float delta) {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (nextWaveButtonBounds.contains(mouseX, mouseY)) {
                endGracePeriod();
                return;
            }
        }

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        buttonHovered = nextWaveButtonBounds.contains(mouseX, mouseY);
    }

    private void endGracePeriod() {
        inGracePeriod = false;
        startNextWave();
    }

    private void startNextWave() {
        currentWave++;
        clumpsSpawned = 0;
        spawnTimer = 0f;
        waveSpawningComplete = false;
        waveInProgress = true;
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
            case HEDGEHOG:
                stats = EnemyStats.Factory.createHedgehogEnemy(currentWave);
                type = EnemyType.HEDGEHOG;
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

    public void renderHUD(SpriteBatch batch) {
        if (inGracePeriod) {
            renderNextWaveButton(batch);
        }
    }

    private void renderNextWaveButton(SpriteBatch batch) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        float buttonX = (screenWidth - BUTTON_WIDTH) / 2f;
        float buttonY = screenHeight - BUTTON_HEIGHT - 30f;

        nextWaveButtonBounds.set(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (buttonHovered) {
            shapeRenderer.setColor(BUTTON_HOVER_COLOR);
        } else {
            shapeRenderer.setColor(BUTTON_COLOR);
        }
        shapeRenderer.rect(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);

        shapeRenderer.setColor(new Color(0.7f, 0.5f, 0.25f, 1f));
        shapeRenderer.rect(buttonX + 3, buttonY + BUTTON_HEIGHT - 6, BUTTON_WIDTH - 6, 3);

        shapeRenderer.setColor(new Color(0.3f, 0.18f, 0.08f, 1f));
        shapeRenderer.rect(buttonX + 3, buttonY + 3, BUTTON_WIDTH - 6, 3);

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3);
        shapeRenderer.setColor(BUTTON_BORDER_COLOR);
        shapeRenderer.rect(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);

        shapeRenderer.setColor(new Color(0.25f, 0.15f, 0.05f, 1f));
        shapeRenderer.rect(buttonX - 2, buttonY - 2, BUTTON_WIDTH + 4, BUTTON_HEIGHT + 4);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.8f, 0.6f, 0.2f, 1f)); // Gold-ish
        float cornerSize = 8f;

        shapeRenderer.rect(buttonX - 2, buttonY + BUTTON_HEIGHT - cornerSize + 2, cornerSize, cornerSize);
        shapeRenderer.rect(buttonX + BUTTON_WIDTH - cornerSize + 2, buttonY + BUTTON_HEIGHT - cornerSize + 2, cornerSize, cornerSize);
        shapeRenderer.rect(buttonX - 2, buttonY - 2, cornerSize, cornerSize);
        shapeRenderer.rect(buttonX + BUTTON_WIDTH - cornerSize + 2, buttonY - 2, cornerSize, cornerSize);
        shapeRenderer.end();

        batch.begin();

        buttonFont.setColor(BUTTON_TEXT_COLOR);
        buttonFont.getData().setScale(0.8f);

        String buttonText = "Next Wave";
        GlyphLayout layout = new GlyphLayout(buttonFont, buttonText);
        float textX = buttonX + (BUTTON_WIDTH - layout.width) / 2f;
        float textY = buttonY + (BUTTON_HEIGHT + layout.height) / 2f;

        buttonFont.setColor(new Color(0.2f, 0.1f, 0.05f, 0.8f));
        buttonFont.draw(batch, buttonText, textX + 2, textY - 2);

        buttonFont.setColor(BUTTON_TEXT_COLOR);
        buttonFont.draw(batch, buttonText, textX, textY);

        buttonFont.getData().setScale(0.5f);
        buttonFont.setColor(Color.WHITE);
        String waveInfo = "Wave " + currentWave + " Complete!";
        GlyphLayout waveLayout = new GlyphLayout(buttonFont, waveInfo);
        buttonFont.draw(batch, waveInfo, (screenWidth - waveLayout.width) / 2f, buttonY - 10f);

        buttonFont.setColor(Color.WHITE);
        buttonFont.getData().setScale(1f);
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

        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
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