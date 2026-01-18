package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import config.GameScreen;
import config.Storage;
import entities.*;
import managers.*;
import abilities.StatusEffect;
import ui.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class GameProj implements Screen, ContactListener {
    private Skin skin;
    private Viewport viewport, hudViewport;
    public Stage stage, hudStage;
    private Game game;
    private GameScreen gameScreen;
    private Storage storage;
    private SpriteBatch batch;
    private AnimationManager animationManager;

    private OrthographicCamera camera, hudCamera;
    private Box2DWorld world;

    private Texture groundTexture;
    private Player player;
    private ConcurrentHashMap<Vector2, Chunk> chunks = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<Chunk> pendingChunks = new ConcurrentLinkedQueue<>();
    private Random random;
    private ExecutorService chunkGenerator;
    private Label hudLabel;
    private int enemiesKilled = 0;
    private boolean bossSpawned = false;

    private final int MAP_SIZE_CHUNKS = 5;
    private final int CHUNK_SIZE = 32;
    private final int TILE_SIZE = 16;
    private final int PLAYER_TILE_SIZE = 32;

    private boolean inDungeon = false;
    private Dungeon currentDungeon;
    private List<Portal> dungeonPortals = new ArrayList<>();
    private Vector2 overworldPlayerPosition;
    private final int NUM_DUNGEONS = 5;

    private MapBoundary mapBoundary;
    private Minimap minimap;
    private DungeonMinimap dungeonMinimap;
    private ItemSpawner itemSpawner;
    private ItemRegistry itemRegistry;
    private LootTableRegistry lootTableRegistry;
    private Settings settings;
    private boolean isPaused = false;

    // Status effects tracking
    private Map<Object, List<StatusEffect>> statusEffects;

    public GameProj(Viewport viewport, Game game, GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        this.game = game;
        this.viewport = viewport;

        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        storage = Storage.getInstance();
        storage.createFont();
        skin = storage.skin;
        animationManager = new AnimationManager();
        settings = new Settings();
        settings.setGameProj(this); // Pass reference for safe exit

        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), hudCamera);
        hudCamera.setToOrtho(false, hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
        hudCamera.update();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewport.getWorldWidth() / (TILE_SIZE / 4), viewport.getWorldHeight() / (TILE_SIZE / 4));
        camera.update();
        world = new Box2DWorld(this);
        random = new Random();
        world.getWorld().setContactListener(this);

        chunkGenerator = Executors.newFixedThreadPool(2);

        createComponents();
        itemSpawner.spawnItem("iron_helmet", player.getPosition());
        itemSpawner.spawnItem("iron_armor", player.getPosition());
        itemSpawner.spawnItem("iron_gloves", player.getPosition());
        itemSpawner.spawnItem("iron_boots", player.getPosition());
        itemSpawner.spawnItem("iron_spear", player.getPosition());
        itemSpawner.spawnItem("iron_shield", player.getPosition());
    }

    private void createComponents() {
        groundTexture = Storage.assetManager.get("tiles/grass.png", Texture.class);

        player = new Player(world, animationManager, PLAYER_TILE_SIZE, this, this.gameScreen);
        batch = new SpriteBatch();

        hudStage = new Stage(hudViewport, batch);

        hudLabel = new Label("", skin);
        hudLabel.setPosition(10, hudViewport.getWorldHeight() - 30);
        hudStage.addActor(hudLabel);

        mapBoundary = new MapBoundary(world.getWorld(), MAP_SIZE_CHUNKS, CHUNK_SIZE, TILE_SIZE);
        minimap = new Minimap(MAP_SIZE_CHUNKS, CHUNK_SIZE, TILE_SIZE, player);

        itemRegistry = ItemRegistry.getInstance();
        lootTableRegistry = LootTableRegistry.getInstance();
        itemSpawner = new ItemSpawner(world.getWorld());
        player.setItemSpawner(itemSpawner);

        // Initialize status effects tracking
        statusEffects = new HashMap<>();

        // Initialize ability manager for player
        player.initializeAbilityManager(this);

        setRandomPlayerSpawn();
        spawnAllDungeonPortals();
    }

    private void setRandomPlayerSpawn() {
        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        int minChunk = -halfMapChunks + 1;
        int maxChunk = halfMapChunks - 1;

        int spawnChunkX = minChunk + random.nextInt((maxChunk - minChunk) + 1);
        int spawnChunkY = minChunk + random.nextInt((maxChunk - minChunk) + 1);

        float offsetX = random.nextFloat() * CHUNK_SIZE * TILE_SIZE;
        float offsetY = random.nextFloat() * CHUNK_SIZE * TILE_SIZE;

        float spawnX = spawnChunkX * CHUNK_SIZE * TILE_SIZE + offsetX;
        float spawnY = spawnChunkY * CHUNK_SIZE * TILE_SIZE + offsetY;

        player.getBody().setTransform(spawnX, spawnY, 0);
    }

    private void spawnAllDungeonPortals() {
        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        int minChunk = -halfMapChunks;

        Vector2 playerSpawn = player.getPosition();

        // Calculate 1/4 of the map's diagonal distance as minimum spawn distance
        float mapWidth = MAP_SIZE_CHUNKS * CHUNK_SIZE * TILE_SIZE;
        float mapHeight = MAP_SIZE_CHUNKS * CHUNK_SIZE * TILE_SIZE;
        float mapDiagonal = (float) Math.sqrt(mapWidth * mapWidth + mapHeight * mapHeight);
        float minDistanceFromSpawn = mapDiagonal * 0.25f;

        int attempts = 0;
        int maxAttempts = 100;

        for (int i = 0; i < NUM_DUNGEONS; i++) {
            boolean validPosition = false;
            float portalX = 0, portalY = 0;

            while (!validPosition && attempts < maxAttempts) {
                attempts++;

                int portalChunkX = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);
                int portalChunkY = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);

                float offsetX = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;
                float offsetY = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;

                portalX = portalChunkX * CHUNK_SIZE * TILE_SIZE + offsetX;
                portalY = portalChunkY * CHUNK_SIZE * TILE_SIZE + offsetY;

                float distanceFromSpawn = (float) Math.sqrt(
                        Math.pow(portalX - playerSpawn.x, 2) +
                                Math.pow(portalY - playerSpawn.y, 2)
                );

                boolean tooCloseToOtherPortals = false;
                for (Portal existingPortal : dungeonPortals) {
                    Vector2 existingPos = new Vector2(
                            existingPortal.getBounds().x + existingPortal.getBounds().width / 2f,
                            existingPortal.getBounds().y + existingPortal.getBounds().height / 2f
                    );
                    float distanceToExisting = (float) Math.sqrt(
                            Math.pow(portalX - existingPos.x, 2) +
                                    Math.pow(portalY - existingPos.y, 2)
                    );

                    if (distanceToExisting < CHUNK_SIZE * TILE_SIZE) {
                        tooCloseToOtherPortals = true;
                        break;
                    }
                }

                // Check for obstacle overlap in the chunk
                boolean overlapsObstacle = isPortalOverlappingObstacles(portalX, portalY, 32);

                if (distanceFromSpawn >= minDistanceFromSpawn && !tooCloseToOtherPortals && !overlapsObstacle) {
                    validPosition = true;
                }
            }

            Portal portal = new Portal(portalX, portalY, 32, world.getWorld(), false);
            dungeonPortals.add(portal);
            attempts = 0;
        }

        if (minimap != null) {
            for (Portal portal : dungeonPortals) {
                minimap.setPortal(portal);
            }
        }
    }

    /**
     * Check if a portal position overlaps with any obstacles in loaded chunks
     */
    private boolean isPortalOverlappingObstacles(float portalX, float portalY, float portalSize) {
        Rectangle portalBounds = new Rectangle(portalX, portalY, portalSize, portalSize);

        // Calculate which chunk this portal would be in
        int chunkX = (int) Math.floor(portalX / (CHUNK_SIZE * TILE_SIZE));
        int chunkY = (int) Math.floor(portalY / (CHUNK_SIZE * TILE_SIZE));

        // Check the portal's chunk and adjacent chunks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Vector2 chunkCoord = new Vector2(chunkX + dx, chunkY + dy);
                Chunk chunk = chunks.get(chunkCoord);

                if (chunk != null && chunk.isOverlappingAnyObstacle(portalBounds)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void enterDungeon() {
        for (Portal portal: dungeonPortals) {
            if (portal.getBounds().contains(player.getPosition())) {
                portal.setIsCleared(true);
            }
        }

        inDungeon = true;
        itemSpawner.clear();
        overworldPlayerPosition = new Vector2(player.getPosition());

        for (Chunk chunk : chunks.values()) {
            chunk.disableObstacles();
            chunk.disableEnemies();
        }

        if (mapBoundary != null) {
            mapBoundary.disable();
        }

        int dungeonTileSize = (int) (TILE_SIZE / 1.2f);
        currentDungeon = new Dungeon(100, 100, dungeonTileSize, random, world.getWorld(), player, animationManager);
        dungeonMinimap = new DungeonMinimap(100, 100, dungeonTileSize, player, currentDungeon);

        Vector2 spawnPoint = currentDungeon.getSpawnPoint();
        player.getBody().setTransform(spawnPoint.x, spawnPoint.y, 0);

        hudLabel.setText("Find the exit!");
    }

    private void exitDungeon() {
        inDungeon = false;
        itemSpawner.clear();

        if (currentDungeon != null) {
            currentDungeon.dispose();
            currentDungeon = null;
        }

        if (dungeonMinimap != null) {
            dungeonMinimap.dispose();
            dungeonMinimap = null;
        }

        for (Chunk chunk : chunks.values()) {
            chunk.enableObstacles();
            chunk.enableEnemies();
        }

        if (mapBoundary != null) {
            mapBoundary.enable();
        }

        if (overworldPlayerPosition != null) {
            player.getBody().setTransform(overworldPlayerPosition.x, overworldPlayerPosition.y, 0);
        }

        hudLabel.setText("Enemied killed: " + enemiesKilled);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        // Handle ESC key for pause menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isPaused) {
                unpauseGame();
            } else {
                pauseGame();
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.F5) && !isPaused) {
            gameScreen.switchToNewState(GameScreen.START);
            return;
        }

        // Update settings menu
        settings.update(delta);

        if (settings != null && isPaused && !settings.isOpen()) {
            unpauseGame();
        }

        // Only update game logic if not paused
        if (!isPaused) {
            world.getWorld().step(1 / 60f, 6, 2);

            if (!inDungeon) {
                if (minimap != null) {
                    minimap.update();
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
                    minimap.toggleMap();
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.P) && minimap.isMapOpen()) {
                    minimap.togglePortalDisplay();
                }
            } else {
                if (dungeonMinimap != null) {
                    dungeonMinimap.update();
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
                    dungeonMinimap.toggleMap();
                }
            }

            if (!inDungeon) {
                renderOverworld(delta);
            } else {
                renderDungeon(delta);
            }
        } else {
            // When paused, still render the game but don't update logic
            if (!inDungeon) {
                renderOverworld(0); // Pass 0 delta to prevent updates
            } else {
                renderDungeon(0);
            }
        }

        if (delta > 0) {
            checkForDeadEnemies();
        }

        // Always render HUD
        if (hudStage != null) {
            hudStage.act(delta);
            hudStage.draw();
        }


        // Render settings menu on top of everything if open
        if (settings != null && settings.isOpen()) {
            settings.render(batch, false);
        }

        if (world != null) {
            Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
//            debugRenderer.render(world.getWorld(), camera.combined);
        }
    }

    /**
     * Pause the game and open settings menu
     */
    private void pauseGame() {
        isPaused = true;
        settings.open();
        player.setPaused(true); // Disable player input
    }

    /**
     * Unpause the game and close settings menu
     */
    private void unpauseGame() {
        isPaused = false;
        settings.close();
        player.setPaused(false); // Re-enable player input
    }

    private void renderOverworld(float delta) {
        if (batch == null) return;

        if (!inDungeon) {
            while (!pendingChunks.isEmpty()) {
                Chunk chunk = pendingChunks.poll();
                if (chunk != null) {
                    chunk.addBodiesToWorld(world.getWorld());
                }
            }
        }

        if (delta > 0) {
            checkForDeadEnemies();
        }

        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        float mapMinX = -halfMapChunks * CHUNK_SIZE * TILE_SIZE;
        float mapMaxX = (halfMapChunks + 1) * CHUNK_SIZE * TILE_SIZE;
        float mapMinY = -halfMapChunks * CHUNK_SIZE * TILE_SIZE;
        float mapMaxY = (halfMapChunks + 1) * CHUNK_SIZE * TILE_SIZE;

        float cameraHalfWidth = camera.viewportWidth / 2f;
        float cameraHalfHeight = camera.viewportHeight / 2f;

        float targetX = player.getPosition().x;
        float targetY = player.getPosition().y;

        float clampedX = Math.max(mapMinX + cameraHalfWidth, Math.min(targetX, mapMaxX - cameraHalfWidth));
        float clampedY = Math.max(mapMinY + cameraHalfHeight, Math.min(targetY, mapMaxY - cameraHalfHeight));

        camera.position.set(clampedX, clampedY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (mapBoundary != null) {
            mapBoundary.render(batch);
        }

        for (Chunk chunk : chunks.values()){
            chunk.renderGround(batch, groundTexture);
        }

        for (Chunk chunk : chunks.values()) {
            chunk.renderObstacles(batch, player.getPosition().y, false);
        }

        itemSpawner.render(batch);

        for (Chunk chunk : chunks.values()) {
            chunk.renderEnemies(batch);
        }

        for (Portal portal : dungeonPortals) {
            if (delta > 0) {
                portal.update(delta);
            }
            portal.render(batch);

            if (!isPaused && portal.isPlayerNear(player.getPosition(), 20f) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E) && !portal.getIsCleared()) {
                enterDungeon();
                break;
            }
        }

        if (Storage.isStageClear() && !bossSpawned) {
            Vector2 playerChunkCoord = new Vector2(
                    (int) Math.floor(player.getPosition().x / (CHUNK_SIZE * TILE_SIZE)),
                    (int) Math.floor(player.getPosition().y / (CHUNK_SIZE * TILE_SIZE))
            );

            Chunk playerChunk = chunks.get(playerChunkCoord);

            if (playerChunk != null) {
                playerChunk.spawnBossKitty(150f);
                bossSpawned = true;
            }
        }

        if (bossSpawned) {
            for (Chunk chunk : chunks.values()) {
                chunk.addBodiesToWorld(world.getWorld());
                chunk.renderBossKitty(batch);
            }
        }

        player.render(batch, PLAYER_TILE_SIZE);
        player.renderAbilityEffects(batch);

        if (delta > 0) {
            player.update(delta);
            updateStatusEffects(delta);
        }

        for (Chunk chunk : chunks.values()) {
            chunk.renderObstacles(batch, player.getPosition().y, true);
        }

        if (batch != null)
            batch.end();

        // Only update item spawner if not paused
        if (delta > 0 && player != null) {
            itemSpawner.update(delta);
            itemSpawner.checkPickups(player, player.getInventory());

            if (minimap != null) {
                minimap.update(); // This updates exploration and portal positions
            }
        }

        // Render UI elements with HUD camera
        if (batch != null) {
            batch.setProjectionMatrix(hudCamera.combined);
            player.getInventory().render(batch, false);

            // Render skill bar
            batch.begin();
            player.renderSkillBar(batch);
            batch.end();

            // Render minimap if open
            if (minimap != null && minimap.isMapOpen()) {
                minimap.render(batch, false);
            }

            // Reset projection matrix back
            batch.setProjectionMatrix(camera.combined);
        }
    }

    private void renderDungeon(float delta) {
        if (batch == null) return;

        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.update();

        if (delta > 0) {
            checkForDeadEnemies();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (currentDungeon != null) {
            currentDungeon.render(batch);
            itemSpawner.render(batch);

            // Only update dungeon if not paused
            if (delta > 0) {
                currentDungeon.renderEnemies(batch, delta);
                currentDungeon.updateEnemies();
            } else {
                currentDungeon.renderEnemies(batch, 0);
            }

            if (!isPaused && currentDungeon.isPlayerAtExit(player.getPosition()) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                exitDungeon();
            }
        }

        player.render(batch, PLAYER_TILE_SIZE);
        player.renderAbilityEffects(batch);

        // Only update player if not paused
        if (delta > 0) {
            player.update(delta);
            // Update all status effects
            updateStatusEffects(delta);
        }

        if (batch != null) {
            batch.end();

            // Only update item spawner if not paused
            if (delta > 0) {
                itemSpawner.update(delta);
                itemSpawner.checkPickups(player, player.getInventory());
            }

            batch.setProjectionMatrix(hudCamera.combined);
            player.getInventory().render(batch, false);

            // Render skill bar
            batch.begin();
            player.renderSkillBar(batch);
            batch.end();

            // Render dungeon minimap if open
            if (dungeonMinimap != null && dungeonMinimap.isMapOpen()) {
                dungeonMinimap.render(batch, false);
            }
        }
    }

    private void scheduleChunkGeneration(int chunkX, int chunkY) {
        if (inDungeon) return;

        chunkGenerator.submit(() -> {
            Vector2 chunkCoord = new Vector2(chunkX, chunkY);
            Chunk newChunk = new Chunk(chunkX, chunkY, CHUNK_SIZE, TILE_SIZE, random, world.getWorld(), player, animationManager);
            pendingChunks.add(newChunk);
            chunks.put(chunkCoord, newChunk);
        });
    }

    public void generateChunksAroundPlayer() {
        if (inDungeon || isPaused) return; // Don't generate chunks when paused

        int playerChunkX = (int) Math.floor(player.getPosition().x / (CHUNK_SIZE * TILE_SIZE));
        int playerChunkY = (int) Math.floor(player.getPosition().y / (CHUNK_SIZE * TILE_SIZE));

        int minChunk = -MAP_SIZE_CHUNKS / 2;
        int maxChunk = MAP_SIZE_CHUNKS / 2;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                int chunkX = playerChunkX + x;
                int chunkY = playerChunkY + y;

                if (chunkX < minChunk || chunkX > maxChunk ||
                        chunkY < minChunk || chunkY > maxChunk) {
                    continue;
                }

                Vector2 chunkCoord = new Vector2(chunkX, chunkY);
                if (!chunks.containsKey(chunkCoord)) {
                    scheduleChunkGeneration(chunkX, chunkY);
                }
            }
        }
    }

    public int getTileSize(){
        return TILE_SIZE;
    }

    public Dungeon getCurrentDungeon(){
        return currentDungeon;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void handleEnemyDeath(Object enemy, Vector2 enemyPosition, boolean isBoss) {
        if (enemy instanceof Enemy) {
            Enemy regularEnemy = (Enemy) enemy;

            String lootTable = regularEnemy.getStats().getLootTableType();
            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPosition);

            player.getStats().addExperience(regularEnemy.getStats().getExpReward());

            enemiesKilled++;
            hudLabel.setText("Enemies killed: " + enemiesKilled);

        } else if (enemy instanceof BossKitty) {
            BossKitty boss = (BossKitty) enemy;

            itemSpawner.spawnBossLoot(enemyPosition);

            player.getStats().addExperience(boss.getStats().getExpReward());

            bossSpawned = false;

        } else if (enemy instanceof DungeonEnemy) {
            DungeonEnemy dungeonEnemy = (DungeonEnemy) enemy;

            String lootTable = dungeonEnemy.getStats().getLootTableType();
            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPosition);

            player.getStats().addExperience(dungeonEnemy.getStats().getExpReward());
        }
    }

    public void checkForDeadEnemies() {
        // Collect bodies to destroy after iteration
        List<Body> bodiesToDestroy = new ArrayList<>();

        for (Chunk chunk : chunks.values()) {
            for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.isMarkedForRemoval() && enemy.getBody() != null) {
                    handleEnemyDeath(enemy, enemy.getBody().getPosition(), false);
                    bodiesToDestroy.add(enemy.getBody());
                    enemy.clearBody();
                    chunk.getEnemies().remove(enemy);
                }
            }

            for (BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.isMarkedForRemoval() && boss.getBody() != null) {
                    handleEnemyDeath(boss, boss.getBody().getPosition(), true);
                    bodiesToDestroy.add(boss.getBody());
                    boss.clearBody();
                    chunk.getBossKitty().remove(boss);
                }
            }
        }

        if (currentDungeon != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentDungeon.getEnemies())) {
                if (enemy.isMarkedForRemoval() && enemy.getBody() != null) {
                    handleEnemyDeath(enemy, enemy.getBody().getPosition(), false);
                    bodiesToDestroy.add(enemy.getBody());
                    enemy.clearBody();
                    currentDungeon.getEnemies().remove(enemy);
                }
            }
        }

        // Destroy all bodies AFTER iteration is complete
        for (Body body : bodiesToDestroy) {
            if (body != null) {
                world.getWorld().destroyBody(body);
            }
        }
    }

    /**
     * Safely exit the game with proper cleanup
     */
    public void safeExit() {
        try {
            dispose();
        } catch (Exception e) {
            System.err.println("Error during safe exit: " + e.getMessage());
        } finally {
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.setToOrtho(false, viewport.getWorldWidth(), viewport.getWorldHeight());
        camera.update();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        try {
            // Clean up player spears first
            if (player != null) {
                player.cleanupSpears();
                player = null;
            }

            // Shutdown executor before disposing world
            if (chunkGenerator != null && !chunkGenerator.isShutdown()) {
                chunkGenerator.shutdownNow();
                try {
                    // Wait for tasks to terminate
                    if (!chunkGenerator.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.err.println("Chunk generator thread pool did not terminate in time");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Clear pending chunks
            if (pendingChunks != null) {
                pendingChunks.clear();
            }

            // Clear active chunks
            if (chunks != null) {
                for (Chunk chunk : chunks.values()) {
                    if (chunk != null) {
                        chunk.dispose(); // Make sure Chunk has a dispose method
                    }
                }
                chunks.clear();
            }

            // Dispose settings first
            if (settings != null) {
                settings.dispose();
                settings = null;
            }

            // Dispose minimaps
            if (dungeonMinimap != null) {
                dungeonMinimap.dispose();
                dungeonMinimap = null;
            }
            if (minimap != null) {
                minimap.dispose();
                minimap = null;
            }

            // Dispose dungeon
            if (currentDungeon != null) {
                currentDungeon.dispose();
                currentDungeon = null;
            }

            // Dispose portals before world
            if (dungeonPortals != null) {
                for (Portal portal : dungeonPortals) {
                    if (portal != null) {
                        portal.dispose(world.getWorld());
                    }
                }
                dungeonPortals.clear();
            }

            // Clear status effects
            if (statusEffects != null) {
                statusEffects.clear();
            }

            // Dispose stages
            if (hudStage != null) {
                hudStage.dispose();
                hudStage = null;
            }
            if (stage != null) {
                stage.dispose();
                stage = null;
            }

            // Dispose batch
            if (batch != null) {
                batch.dispose();
                batch = null;
            }

            // Dispose world last (after all bodies are removed)
            if (world != null) {
                world.dispose();
                world = null;
            }

            // Force garbage collection to clean up any remaining resources
            System.gc();

        } catch (Exception e) {
            System.err.println("Error during disposal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void beginContact(Contact contact) {
        if (isPaused) return; // Don't process collisions when paused

        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        short categoryA = fixtureA.getFilterData().categoryBits;
        short categoryB = fixtureB.getFilterData().categoryBits;

        // ==== SPEAR HIT ENEMY ====
        if ((categoryA == CollisionFilter.SPEAR && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.SPEAR && categoryA == CollisionFilter.ENEMY)) {

            Body spearBody = (categoryA == CollisionFilter.SPEAR) ? fixtureA.getBody() : fixtureB.getBody();
            Body enemyBody = categoryA == CollisionFilter.ENEMY ? fixtureA.getBody() : fixtureB.getBody();

            // Get player damage
            int playerDamage = player.getStats().getTotalDamage();

            if (!inDungeon) {
                for (Chunk chunk : chunks.values()) {
                    if (!bossSpawned) {
                        // Normal enemies
                        for (Enemy enemy : chunk.getEnemies()) {
                            if (enemy.getBody() == enemyBody) {
                                enemy.takeDamage(playerDamage);
                                break;
                            }
                        }
                    } else {
                        // Boss enemies
                        for (BossKitty boss : chunk.getBossKitty()) {
                            if (boss.getBody() == enemyBody) {
                                boss.takeDamage(playerDamage);
                                break;
                            }
                        }
                    }
                }
            } else if (currentDungeon != null) {
                // Dungeon enemies
                for (entities.DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    if (enemy.getBody() == enemyBody) {
                        enemy.takeDamage(playerDamage);
                        break;
                    }
                }
            }

            player.markSpearForRemoval(spearBody);
        }

        // ==== SPEAR HIT OBSTACLE ====
        if ((categoryA == CollisionFilter.SPEAR && categoryB == CollisionFilter.OBSTACLE) ||
                (categoryB == CollisionFilter.SPEAR && categoryA == CollisionFilter.OBSTACLE)) {
            Body spearBody = (categoryA == CollisionFilter.SPEAR) ? fixtureA.getBody() : fixtureB.getBody();
            player.markSpearForRemoval(spearBody);
        }

        // ==== SPEAR HIT WALL ====
        if ((categoryA == CollisionFilter.SPEAR && categoryB == CollisionFilter.WALL) ||
                (categoryB == CollisionFilter.SPEAR && categoryA == CollisionFilter.WALL)) {
            Body spearBody = (categoryA == CollisionFilter.SPEAR) ? fixtureA.getBody() : fixtureB.getBody();
            player.markSpearForRemoval(spearBody);
        }

        // ==== PLAYER HIT ENEMY ====
        if ((categoryA == CollisionFilter.PLAYER && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.PLAYER && categoryA == CollisionFilter.ENEMY)) {
            Body enemyBody = categoryA == CollisionFilter.ENEMY ? fixtureA.getBody() : fixtureB.getBody();

            if (!inDungeon) {
                for (Chunk chunk : chunks.values()) {
                    if(!bossSpawned) {
                        // Normal enemies damage player
                        for (Enemy enemy : chunk.getEnemies()) {
                            if (enemy.getBody() == enemyBody && !player.isInvulnerable()) {
                                enemy.damagePlayer();

                                // Check if player died
                                if (player.getStats().isDead()) {
                                    player.playerDie();
                                }
                                break;
                            }
                        }
                    } else {
                        // Boss damages player
                        for (BossKitty boss : chunk.getBossKitty()) {
                            if (boss.getBody() == enemyBody) {
                                boss.damagePlayer();

                                if (player.getStats().isDead()) {
                                    player.playerDie();
                                }
                                break;
                            }
                        }
                    }
                }
            } else if (currentDungeon != null) {
                // Dungeon enemies damage player
                for (entities.DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    if (enemy.getBody() == enemyBody) {
                        enemy.damagePlayer();

                        if (player.getStats().isDead()) {
                            player.playerDie();
                        }
                        break;
                    }
                }
            }
        }

        // ==== ABILITY (BUBBLE) HIT ENEMY ====
        if ((categoryA == CollisionFilter.ABILITY && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.ABILITY && categoryA == CollisionFilter.ENEMY)) {

            Body abilityBody = (categoryA == CollisionFilter.ABILITY) ? fixtureA.getBody() : fixtureB.getBody();
            Body enemyBody = (categoryA == CollisionFilter.ENEMY) ? fixtureA.getBody() : fixtureB.getBody();

            // Push enemy away from the bubble
            Vector2 abilityPos = abilityBody.getPosition();
            Vector2 enemyPos = enemyBody.getPosition();
            Vector2 pushDirection = new Vector2(enemyPos.x - abilityPos.x, enemyPos.y - abilityPos.y).nor();

            float pushForce = 500f;
            enemyBody.applyLinearImpulse(
                    pushDirection.x * pushForce,
                    pushDirection.y * pushForce,
                    enemyPos.x,
                    enemyPos.y,
                    true
            );
        }

        // ==== ABILITY (BUBBLE) HIT PROJECTILE - Reflect projectile ====
        if ((categoryA == CollisionFilter.ABILITY && categoryB == CollisionFilter.PROJECTILE) ||
                (categoryB == CollisionFilter.ABILITY && categoryA == CollisionFilter.PROJECTILE)) {

            Body abilityBody = (categoryA == CollisionFilter.ABILITY) ? fixtureA.getBody() : fixtureB.getBody();
            Body projectileBody = (categoryA == CollisionFilter.PROJECTILE) ? fixtureA.getBody() : fixtureB.getBody();
            Fixture projectileFixture = (categoryA == CollisionFilter.PROJECTILE) ? fixtureA : fixtureB;

            // Push projectile away from the bubble
            Vector2 abilityPos = abilityBody.getPosition();
            Vector2 projectilePos = projectileBody.getPosition();
            Vector2 pushDirection = new Vector2(projectilePos.x - abilityPos.x, projectilePos.y - abilityPos.y).nor();

            float pushForce = 500f;
            projectileBody.applyLinearImpulse(
                    pushDirection.x * pushForce,
                    pushDirection.y * pushForce,
                    projectilePos.x,
                    projectilePos.y,
                    true
            );

            // Change to REFLECT category so it can damage enemies
            Filter newFilter = new Filter();
            newFilter.categoryBits = CollisionFilter.REFLECT;
            newFilter.maskBits = CollisionFilter.OBSTACLE | CollisionFilter.ENEMY;
            projectileFixture.setFilterData(newFilter);
        }

        // ==== REFLECTED PROJECTILE HIT ENEMY ====
        if ((categoryA == CollisionFilter.REFLECT && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.REFLECT && categoryA == CollisionFilter.ENEMY)) {

            Body reflectBody = (categoryA == CollisionFilter.REFLECT) ? fixtureA.getBody() : fixtureB.getBody();
            Body enemyBody = (categoryA == CollisionFilter.ENEMY) ? fixtureA.getBody() : fixtureB.getBody();

            // Find the projectile and enemy, deal damage, mark projectile for removal
            if (!inDungeon) {
                for (Chunk chunk : chunks.values()) {
                    // Find the projectile from any enemy's projectile list
                    for (Enemy enemy : chunk.getEnemies()) {
                        for (Projectile projectile : enemy.getProjectiles()) {
                            if (projectile.getBody() == reflectBody) {
                                // Find the enemy that was hit
                                for (Enemy targetEnemy : chunk.getEnemies()) {
                                    if (targetEnemy.getBody() == enemyBody) {
                                        targetEnemy.takeDamage(projectile.getDamage());
                                        projectile.markForRemoval();
                                        break;
                                    }
                                }
                                // Also check bosses
                                for (BossKitty boss : chunk.getBossKitty()) {
                                    if (boss.getBody() == enemyBody) {
                                        boss.takeDamage(projectile.getDamage());
                                        projectile.markForRemoval();
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            } else if (currentDungeon != null) {
                // Check dungeon enemies
                for (DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    for (Projectile projectile : enemy.getProjectiles()) {
                        if (projectile.getBody() == reflectBody) {
                            // Find the enemy that was hit
                            for (DungeonEnemy targetEnemy : currentDungeon.getEnemies()) {
                                if (targetEnemy.getBody() == enemyBody) {
                                    targetEnemy.takeDamage(projectile.getDamage());
                                    projectile.markForRemoval();
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        // ==== REFLECTED PROJECTILE HIT OBSTACLE ====
        if ((categoryA == CollisionFilter.REFLECT && categoryB == CollisionFilter.OBSTACLE) ||
                (categoryB == CollisionFilter.REFLECT && categoryA == CollisionFilter.OBSTACLE)) {

            Body reflectBody = (categoryA == CollisionFilter.REFLECT) ? fixtureA.getBody() : fixtureB.getBody();

            // Find and mark the projectile for removal
            if (!inDungeon) {
                for (Chunk chunk : chunks.values()) {
                    for (Enemy enemy : chunk.getEnemies()) {
                        for (Projectile projectile : enemy.getProjectiles()) {
                            if (projectile.getBody() == reflectBody) {
                                projectile.markForRemoval();
                                break;
                            }
                        }
                    }
                }
            } else if (currentDungeon != null) {
                for (DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    for (Projectile projectile : enemy.getProjectiles()) {
                        if (projectile.getBody() == reflectBody) {
                            projectile.markForRemoval();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void endContact(Contact contact) {}

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {}

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}

    public OrthographicCamera getCamera() {
        return camera;
    }

    // Getters for ability system
    public ConcurrentHashMap<Vector2, Chunk> getChunks() {
        return chunks;
    }
    /**
     * Add status effect to target (called by abilities)
     */
    public void addStatusEffect(Object target, StatusEffect effect) {
        if (!statusEffects.containsKey(target)) {
            statusEffects.put(target, new java.util.ArrayList<>());
        }
        statusEffects.get(target).add(effect);
    }

    /**
     * Update all status effects and remove expired ones
     */
    public void updateStatusEffects(float delta) {
        java.util.Iterator<java.util.Map.Entry<Object, List<StatusEffect>>> it = statusEffects.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Object, List<StatusEffect>> entry = it.next();
            List<StatusEffect> effects = entry.getValue();

            // Update each effect and remove expired ones
            java.util.Iterator<StatusEffect> effectIt = effects.iterator();
            while (effectIt.hasNext()) {
                StatusEffect effect = effectIt.next();
                effect.update(delta);

                if (effect.isExpired()) {
                    effect.onExpire();
                    effectIt.remove();
                }
            }

            // Remove empty effect lists
            if (effects.isEmpty()) {
                it.remove();
            }
        }
    }
}