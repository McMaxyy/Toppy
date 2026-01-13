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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import config.GameScreen;
import config.Storage;
import entities.BossKitty;
import entities.Enemy;
import entities.Player;
import entities.Portal;
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
    private final int TILE_SIZE = 18;
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
        groundTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

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
        float minDistanceFromSpawn = mapDiagonal * 0.25f; // 25% of map diagonal

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

                if (distanceFromSpawn >= minDistanceFromSpawn && !tooCloseToOtherPortals) {
                    validPosition = true;
                }
            }

//            Portal portal = new Portal(portalX, portalY, 32, world.getWorld());
            Portal portal = new Portal(player.getPosition().x, player.getPosition().y, 32, world.getWorld());
            dungeonPortals.add(portal);
            attempts = 0;
        }

        if (minimap != null) {
            for (Portal portal : dungeonPortals) {
                minimap.setPortal(portal);
            }
        }
    }

    private void enterDungeon() {
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

        // Handle F5 to return to home (only if not paused)
        if (Gdx.input.isKeyPressed(Input.Keys.F5) && !isPaused) {
            gameScreen.switchToNewState(GameScreen.HOME);
            return;
        }

        // Update settings menu
        settings.update(delta);

        // If settings menu closes itself (e.g., via Resume button), sync pause state
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

        // Always render HUD
        if (hudStage != null) {
            hudStage.act(delta);
            hudStage.draw();
        }


        // Render settings menu on top of everything if open
        if (settings != null && settings.isOpen()) {
            settings.render(batch, false);
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

        // Only update portals if not paused
        for (Portal portal : dungeonPortals) {
            if (delta > 0) { // Only update if game is running
                portal.update(delta);
            }
            portal.render(batch);

            if (!isPaused && portal.isPlayerNear(player.getPosition(), 20f) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
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

        // Only update player if not paused
        if (delta > 0) {
            player.update(delta);
            // Update all status effects
            updateStatusEffects(delta);
        }

        for (Chunk chunk : chunks.values()) {
            chunk.renderObstacles(batch, player.getPosition().y, true);
        }

        batch.end();

        // Only update item spawner if not paused
        if (delta > 0) {
            itemSpawner.update(delta);
            itemSpawner.checkPickups(player, player.getInventory());

            if (minimap != null) {
                minimap.update(); // This updates exploration and portal positions
            }
        }

        // Render UI elements with HUD camera
        batch.setProjectionMatrix(hudCamera.combined);

        // Render inventory
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

    private void renderDungeon(float delta) {
        if (batch == null) return;

        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.update();

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

        // Only update player if not paused
        if (delta > 0) {
            player.update(delta);
            // Update all status effects
            updateStatusEffects(delta);
        }

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

            // Dispose chunks
            if (chunks != null) {
                chunks.clear();
            }
            if (pendingChunks != null) {
                pendingChunks.clear();
            }

            // Shutdown executor before disposing world
            if (chunkGenerator != null && !chunkGenerator.isShutdown()) {
                chunkGenerator.shutdownNow();
                try {
                    chunkGenerator.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
                                // Deal damage instead of instant kill
                                enemy.takeDamage(playerDamage);

                                // If enemy died from this hit, give XP and spawn loot
                                if (enemy.isMarkedForRemoval()) {
                                    Vector2 enemyPos = enemyBody.getPosition();
                                    String lootTable = enemy.getStats().getLootTableType();
                                    lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPos);

                                    // Give experience to player
                                    player.getStats().addExperience(enemy.getStats().getExpReward());

                                    enemiesKilled++;
                                    hudLabel.setText("Enemies killed: " + enemiesKilled);
                                }
                                break;
                            }
                        }
                    } else {
                        // Boss enemies
                        for (BossKitty boss : chunk.getBossKitty()) {
                            if (boss.getBody() == enemyBody) {
                                // Deal damage to boss
                                boss.takeDamage(playerDamage);

                                // If boss died, spawn loot and give XP
                                if (boss.isMarkedForRemoval()) {
                                    Vector2 bossPos = enemyBody.getPosition();
                                    itemSpawner.spawnBossLoot(bossPos);

                                    // Give lots of experience
                                    player.getStats().addExperience(boss.getStats().getExpReward());

                                    bossSpawned = false;
                                }
                                break;
                            }
                        }
                    }
                }
            } else if (currentDungeon != null) {
                // Dungeon enemies
                for (entities.DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    if (enemy.getBody() == enemyBody) {
                        // Deal damage
                        enemy.takeDamage(playerDamage);

                        // If enemy died, spawn loot and give XP
                        if (enemy.isMarkedForRemoval()) {
                            Vector2 enemyPos = enemyBody.getPosition();
                            String lootTable = enemy.getStats().getLootTableType();
                            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPos);
                            player.getStats().addExperience(enemy.getStats().getExpReward());
                        }
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
                            if (enemy.getBody() == enemyBody) {
                                enemy.damagePlayer(); // Damage with cooldown

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