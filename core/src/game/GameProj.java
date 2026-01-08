package game;

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
import managers.AnimationManager;
import managers.Box2DWorld;
import managers.Chunk;
import managers.CollisionFilter;
import managers.Dungeon;
import managers.MapBoundary;
import managers.Minimap;

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
    private int enemiesKilled = 0, enemyGoal = 5;
    private boolean bossSpawned = false;

    // Map boundaries
    private final int MAP_SIZE_CHUNKS = 5; // 5x5 chunks map
    private final int CHUNK_SIZE = 32;
    private final int TILE_SIZE = 18;
    private final int PLAYER_TILE_SIZE = 32;

    // Dungeon system
    private boolean inDungeon = false;
    private Dungeon currentDungeon;
    private Portal dungeonPortal;
    private Vector2 overworldPlayerPosition;

    // Map boundaries
    private MapBoundary mapBoundary;

    // Minimap system
    private Minimap minimap;

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
    }

    private void createComponents() {
        groundTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

        player = new Player(world, animationManager, PLAYER_TILE_SIZE, this, this.gameScreen);
        batch = new SpriteBatch();

        hudStage = new Stage(hudViewport, batch);

        hudLabel = new Label("0/" + enemyGoal, skin);
        hudLabel.setPosition(10, hudViewport.getWorldHeight() - 30);
        hudStage.addActor(hudLabel);

        // Create map boundaries
        mapBoundary = new MapBoundary(world.getWorld(), MAP_SIZE_CHUNKS, CHUNK_SIZE, TILE_SIZE);

        // Create minimap
        minimap = new Minimap(MAP_SIZE_CHUNKS, CHUNK_SIZE, TILE_SIZE, player);

        // Spawn initial portal
        spawnDungeonPortal();
    }

    private void spawnDungeonPortal() {
        if (dungeonPortal != null) return;

        // Spawn portal away from spawn point
        float portalX = (random.nextInt(MAP_SIZE_CHUNKS - 2) + 1) * CHUNK_SIZE * TILE_SIZE;
        float portalY = (random.nextInt(MAP_SIZE_CHUNKS - 2) + 1) * CHUNK_SIZE * TILE_SIZE;

        dungeonPortal = new Portal(portalX, portalY, 32, world.getWorld());

        // Update minimap with new portal location
        if (minimap != null) {
            minimap.setPortal(dungeonPortal);
        }
    }

    private void enterDungeon() {
        inDungeon = true;
        overworldPlayerPosition = new Vector2(player.getPosition());

        // Disable overworld physics bodies
        for (Chunk chunk : chunks.values()) {
            chunk.disableObstacles();
            chunk.disableEnemies();
        }

        // Generate dungeon
        currentDungeon = new Dungeon(50, 50, TILE_SIZE, random, world.getWorld(), player, animationManager);

        // Teleport player to dungeon spawn
        Vector2 spawnPoint = currentDungeon.getSpawnPoint();
        player.getBody().setTransform(spawnPoint.x, spawnPoint.y, 0);

        hudLabel.setText("Find the exit!");
    }

    private void exitDungeon() {
        inDungeon = false;

        // Clear dungeon enemies
        if (currentDungeon != null) {
            currentDungeon.dispose();
            currentDungeon = null;
        }

        // Re-enable overworld physics bodies
        for (Chunk chunk : chunks.values()) {
            chunk.enableObstacles();
            chunk.enableEnemies();
        }

        // Teleport player back to overworld
        if (overworldPlayerPosition != null) {
            player.getBody().setTransform(overworldPlayerPosition.x, overworldPlayerPosition.y, 0);
        }

        // Respawn portal at new location
        if (dungeonPortal != null) {
            dungeonPortal.dispose(world.getWorld());
        }
        spawnDungeonPortal();

        hudLabel.setText("Goal: " + enemiesKilled + "/" + enemyGoal);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        world.getWorld().step(1 / 60f, 6, 2);

        // Update minimap
        if (minimap != null && !inDungeon) {
            minimap.update();
        }

        // Handle map toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) && !inDungeon) {
            minimap.toggleMap();
        }

        // Handle portal display toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && !inDungeon && minimap.isMapOpen()) {
            minimap.togglePortalDisplay();
        }

        if (!inDungeon) {
            renderOverworld(delta);
        } else {
            renderDungeon(delta);
        }

        // Render minimap on top of everything (batch should be ended by now)
        if (minimap != null && minimap.isMapOpen() && !inDungeon) {
            minimap.render(batch, false);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.F5)) gameScreen.switchToNewState(GameScreen.HOME);
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) Player.gameStarted = false;

        hudStage.act(delta);
        hudStage.draw();
    }

    private void renderOverworld(float delta) {
        // Only process pending chunks when in overworld
        if (!inDungeon) {
            while (!pendingChunks.isEmpty()) {
                Chunk chunk = pendingChunks.poll();
                if (chunk != null) {
                    chunk.addBodiesToWorld(world.getWorld());
                }
            }
        }

        if(enemiesKilled >= enemyGoal && !Storage.isStageClear()) {
            Storage.setStageClear(true);

            for (Chunk chunk : chunks.values()) {
                for (Enemy enemy : chunk.getEnemies()) {
                    enemy.removeEnemies();
                }
            }

            for (Chunk chunk : chunks.values()){
                chunk.removeEnemies();
            }
        }

        // Calculate camera bounds based on map size
        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        float mapMinX = -halfMapChunks * CHUNK_SIZE * TILE_SIZE;
        float mapMaxX = (halfMapChunks + 1) * CHUNK_SIZE * TILE_SIZE;
        float mapMinY = -halfMapChunks * CHUNK_SIZE * TILE_SIZE;
        float mapMaxY = (halfMapChunks + 1) * CHUNK_SIZE * TILE_SIZE;

        // Get camera viewport size in world units
        float cameraHalfWidth = camera.viewportWidth / 2f;
        float cameraHalfHeight = camera.viewportHeight / 2f;

        // Clamp camera position to map boundaries
        float targetX = player.getPosition().x;
        float targetY = player.getPosition().y;

        float clampedX = Math.max(mapMinX + cameraHalfWidth, Math.min(targetX, mapMaxX - cameraHalfWidth));
        float clampedY = Math.max(mapMinY + cameraHalfHeight, Math.min(targetY, mapMaxY - cameraHalfHeight));

        camera.position.set(clampedX, clampedY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Render map boundaries
        if (mapBoundary != null) {
            mapBoundary.render(batch);
        }

        for (Chunk chunk : chunks.values()){
            chunk.renderGround(batch, groundTexture);
        }

        for (Chunk chunk : chunks.values()) {
            chunk.renderObstacles(batch, player.getPosition().y, false);
        }

        for (Chunk chunk : chunks.values()) {
            chunk.renderEnemies(batch);
        }

        // Render portal
        if (dungeonPortal != null) {
            dungeonPortal.update(delta);
            dungeonPortal.render(batch);

            // Check if player is near portal
            if (dungeonPortal.isPlayerNear(player.getPosition(), 20f) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                enterDungeon();
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
        player.update(delta);

        for (Chunk chunk : chunks.values()) {
            chunk.renderObstacles(batch, player.getPosition().y, true);
        }

        batch.end();

        Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
        debugRenderer.render(world.getWorld(), camera.combined);
    }

    private void renderDungeon(float delta) {
        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (currentDungeon != null) {
            currentDungeon.render(batch);
            currentDungeon.renderEnemies(batch, delta);
            currentDungeon.updateEnemies();

            // Check if player reached exit
            if (currentDungeon.isPlayerAtExit(player.getPosition()) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                exitDungeon();
            }
        }

        player.render(batch, PLAYER_TILE_SIZE);
        player.update(delta);

        batch.end();

        Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
        debugRenderer.render(world.getWorld(), camera.combined);
    }

    private void scheduleChunkGeneration(int chunkX, int chunkY) {
        if (inDungeon) return; // Don't generate chunks while in dungeon

        chunkGenerator.submit(() -> {
            Vector2 chunkCoord = new Vector2(chunkX, chunkY);
            Chunk newChunk = new Chunk(chunkX, chunkY, CHUNK_SIZE, TILE_SIZE, random, world.getWorld(), player, animationManager);
            pendingChunks.add(newChunk);
            chunks.put(chunkCoord, newChunk);
        });
    }

    public void generateChunksAroundPlayer() {
        if (inDungeon) return; // Don't generate chunks in dungeon

        int playerChunkX = (int) Math.floor(player.getPosition().x / (CHUNK_SIZE * TILE_SIZE));
        int playerChunkY = (int) Math.floor(player.getPosition().y / (CHUNK_SIZE * TILE_SIZE));

        // Limit to map boundaries
        int minChunk = -MAP_SIZE_CHUNKS / 2;
        int maxChunk = MAP_SIZE_CHUNKS / 2;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                int chunkX = playerChunkX + x;
                int chunkY = playerChunkY + y;

                // Check boundaries
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
        world.dispose();
        batch.dispose();
        chunkGenerator.shutdown();
        if (currentDungeon != null) {
            currentDungeon.dispose();
        }
        if (dungeonPortal != null) {
            dungeonPortal.dispose(world.getWorld());
        }
        if (minimap != null) {
            minimap.dispose();
        }
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        short categoryA = fixtureA.getFilterData().categoryBits;
        short categoryB = fixtureB.getFilterData().categoryBits;

        if ((categoryA == CollisionFilter.SPEAR && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.SPEAR && categoryA == CollisionFilter.ENEMY)) {

            Body spearBody = (categoryA == CollisionFilter.SPEAR) ? fixtureA.getBody() : fixtureB.getBody();
            Body enemyBody = categoryA == CollisionFilter.ENEMY ? fixtureA.getBody() : fixtureB.getBody();

            if (!inDungeon) {
                for (Chunk chunk : chunks.values()) {
                    if (!bossSpawned) {
                        for (Enemy enemy : chunk.getEnemies()) {
                            if (enemy.getBody() == enemyBody) {
                                enemy.markForRemoval();
                                enemiesKilled++;
                                hudLabel.setText("Goal: " + enemiesKilled + "/" + enemyGoal);
                                break;
                            }
                        }
                    } else {
                        for (BossKitty enemy : chunk.getBossKitty()) {
                            if (enemy.getBody() == enemyBody) {
                                enemy.markForRemoval();
                                bossSpawned = false;
                                Storage.setStageClear(false);
                                enemiesKilled = 0;
                                gameScreen.switchToNewState(GameScreen.HOME);
                                break;
                            }
                        }
                    }
                }
            } else if (currentDungeon != null) {
                for (entities.DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    if (enemy.getBody() == enemyBody) {
                        enemy.markForRemoval();
                        break;
                    }
                }
            }

            player.markSpearForRemoval(spearBody);
        }

        if ((categoryA == CollisionFilter.SPEAR && categoryB == CollisionFilter.OBSTACLE) ||
                (categoryB == CollisionFilter.SPEAR && categoryA == CollisionFilter.OBSTACLE)) {
            Body spearBody = (categoryA == CollisionFilter.SPEAR) ? fixtureA.getBody() : fixtureB.getBody();
            player.markSpearForRemoval(spearBody);
        }

        if ((categoryA == CollisionFilter.PLAYER && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.PLAYER && categoryA == CollisionFilter.ENEMY)) {
            Body enemyBody = categoryA == CollisionFilter.ENEMY ? fixtureA.getBody() : fixtureB.getBody();

            if (!inDungeon) {
                for (Chunk chunk : chunks.values()) {
                    if(!bossSpawned) {
                        for (Enemy enemy : chunk.getEnemies()) {
                            if (enemy.getBody() == enemyBody) {
                                player.playerDie();
                            }
                        }
                    } else {
                        for (BossKitty enemy : chunk.getBossKitty()) {
                            if (enemy.getBody() == enemyBody) {
                                enemy.markForRemoval();
                                bossSpawned = false;
                                Storage.setStageClear(false);
                                enemiesKilled = 0;
                                player.playerDie();
                                break;
                            }
                        }
                    }
                }
            } else if (currentDungeon != null) {
                for (entities.DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    if (enemy.getBody() == enemyBody) {
                        player.playerDie();
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
}