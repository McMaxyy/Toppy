package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import abilities.SkillTree;
import abilities.StatusEffectRenderer;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
import ui.MerchantShop;
import ui.Settings;

import java.util.HashMap;
import java.util.Map;

public class GameProj implements Screen, ContactListener {
    private final Skin skin;
    private final Viewport viewport;
    private final Viewport hudViewport;
    public Stage stage, hudStage;
    private Game game;
    private final GameScreen gameScreen;
    private SpriteBatch batch;
    private final AnimationManager animationManager;

    private final OrthographicCamera camera;
    private final OrthographicCamera hudCamera;
    private Box2DWorld world;

    private Texture groundTexture;
    private Player player;
    private final ConcurrentHashMap<Vector2, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Chunk> pendingChunks = new ConcurrentLinkedQueue<>();
    private final Random random;
    private final ExecutorService chunkGenerator;
    private Label hudLabel;
    private int enemiesKilled = 0;
    private boolean bossSpawned = false;

    private final int MAP_SIZE_CHUNKS = 5;
    private final int CHUNK_SIZE = 32;
    private final int TILE_SIZE = 16;
    private final int PLAYER_TILE_SIZE = 32;

    private boolean inDungeon = false;
    private boolean inBossRoom = false;

    private Dungeon currentDungeon;
    private BossRoom currentBossRoom;
    private final List<Portal> dungeonPortals = new ArrayList<>();
    private Vector2 overworldPlayerPosition;
    private final int NUM_DUNGEONS = 5;

    private boolean bossKittyDefeated = false;
    private boolean cyclopsDefeated = false;
    private boolean ghostBossDefeated = false;
    private Herman herman;
    private Herman hermanDuplicate;
    private boolean hermanSpawned = false;
    private boolean hermanDefeated = false;
    private List<Body> hermanArenaBarriers = new ArrayList<>();

    private MapBoundary mapBoundary;
    private Minimap minimap;
    private DungeonMinimap dungeonMinimap;
    private ItemSpawner itemSpawner;
    private ItemRegistry itemRegistry;
    private LootTableRegistry lootTableRegistry;
    private Settings settings;
    private boolean isPaused = false;
    private float statusEffectTimer = 0f;

    private Merchant merchant;
    private MerchantShop merchantShop;
    private boolean merchantShopOpen = false;
    private Texture cursorTexture;
    private boolean useCustomCursor = true;
    private boolean cursorConfined = true;
    private float cursorX = 0;
    private float cursorY = 0;


    private Map<Object, List<StatusEffect>> statusEffects;

    public GameProj(Viewport viewport, Game game, GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        this.game = game;
        this.viewport = viewport;

        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        Storage storage = Storage.getInstance();
        storage.createFont();
        skin = storage.skin;
        animationManager = new AnimationManager();
        settings = new Settings();
        settings.setGameProj(this);

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

        itemSpawner.spawnItem("valkyries_iron_helmet", player.getPosition());
        itemSpawner.spawnItem("berserkers_iron_sword", player.getPosition());
        itemSpawner.spawnItem("deceptors_iron_boots", player.getPosition());
        itemSpawner.spawnItem("protectors_iron_armor", player.getPosition());
        itemSpawner.spawnItem("barbarians_iron_shield", player.getPosition());
        itemSpawner.spawnItem("deceptors_iron_gloves", player.getPosition());

        setupCursorConfinement();
    }

    private void setupCursorConfinement() {
        try {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0, 0, 0, 0); // Transparent
            pixmap.fill();
            Cursor emptyCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
            Gdx.graphics.setCursor(emptyCursor);
            pixmap.dispose();
        } catch (Exception e) {
            System.err.println("Failed to create empty cursor: " + e.getMessage());
        }

        int centerX = Gdx.graphics.getWidth() / 2;
        int centerY = Gdx.graphics.getHeight() / 2;
        Gdx.input.setCursorPosition(centerX, centerY);

        cursorX = centerX;
        cursorY = centerY;

        Gdx.input.setCursorCatched(true);
    }

    public void updateCursorConfinement() {
        if (!cursorConfined || isPaused || merchantShopOpen) {
            cursorX = Gdx.input.getX();
            cursorY = Gdx.graphics.getHeight() - Gdx.input.getY();
            return;
        }

        int deltaX = Gdx.input.getDeltaX();
        int deltaY = Gdx.input.getDeltaY();

        cursorX += deltaX;
        cursorY -= deltaY;

        int windowWidth = Gdx.graphics.getWidth();
        int windowHeight = Gdx.graphics.getHeight();

        int borderMargin = 2;

        cursorX = Math.max(borderMargin, Math.min(cursorX, windowWidth - borderMargin));
        cursorY = Math.max(borderMargin, Math.min(cursorY, windowHeight - borderMargin));

        if (cursorX <= borderMargin || cursorX >= windowWidth - borderMargin ||
                cursorY <= borderMargin || cursorY >= windowHeight - borderMargin) {

            Gdx.input.setCursorPosition((int)cursorX, windowHeight - (int)cursorY);
        }

        int currentMouseX = Gdx.input.getX();
        int currentMouseY = Gdx.input.getY();
        int maxDelta = 50;

        if (Math.abs(currentMouseX - cursorX) > maxDelta ||
                Math.abs(windowHeight - currentMouseY - cursorY) > maxDelta) {

            cursorX = currentMouseX;
            cursorY = windowHeight - currentMouseY;

            cursorX = Math.max(borderMargin, Math.min(cursorX, windowWidth - borderMargin));
            cursorY = Math.max(borderMargin, Math.min(cursorY, windowHeight - borderMargin));

            Gdx.input.setCursorPosition((int)cursorX, windowHeight - (int)cursorY);
        }
    }

    public GameScreen getGameScreen() {
        return this.gameScreen;
    }

    private void setupCameraProjection() {
        camera.setToOrtho(false, viewport.getWorldWidth() / (TILE_SIZE / 4),
                viewport.getWorldHeight() / (TILE_SIZE / 4));
        camera.update();

        hudCamera.setToOrtho(false, hudViewport.getWorldWidth(), hudViewport.getWorldHeight());
        hudCamera.update();
    }

    private void createComponents() {
        cursorTexture = Storage.assetManager.get("mouse.png", Texture.class);

        groundTexture = Storage.assetManager.get("tiles/grass.png", Texture.class);

        player = new Player(world, animationManager, PLAYER_TILE_SIZE, this, this.gameScreen, Storage.getSelectedPlayerClass());
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

        statusEffects = new HashMap<>();

        player.initializeAbilityManager(this);

        merchantShop = new MerchantShop();
        merchantShop.setPlayer(player);

        setRandomPlayerSpawn();
        spawnAllDungeonPortals();
        spawnMerchant();
        SoundManager.getInstance().playForestMusic();
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

    public boolean isSkillTreeOpen() {
        if (player != null && player.getAbilityManager() != null) {
            SkillTree skillTree = player.getAbilityManager().getSkillTree();
            return skillTree != null && skillTree.isOpen();
        }
        return false;
    }

    private void spawnMerchant() {
        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        int minChunk = -halfMapChunks;

        Vector2 playerSpawn = player.getPosition();
        float minDistanceFromSpawn = CHUNK_SIZE * TILE_SIZE * 0.5f; // Closer than portals

        int attempts = 0;
        int maxAttempts = 100;
        boolean validPosition = false;
        float merchantX = 0, merchantY = 0;

        while (!validPosition && attempts < maxAttempts) {
            attempts++;

            int merchantChunkX = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);
            int merchantChunkY = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);

            float offsetX = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;
            float offsetY = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;

            merchantX = merchantChunkX * CHUNK_SIZE * TILE_SIZE + offsetX;
            merchantY = merchantChunkY * CHUNK_SIZE * TILE_SIZE + offsetY;

            float distanceFromSpawn = (float) Math.sqrt(
                    Math.pow(merchantX - playerSpawn.x, 2) +
                            Math.pow(merchantY - playerSpawn.y, 2)
            );

            // Check distance from portals
            boolean tooCloseToPortals = false;
            for (Portal portal : dungeonPortals) {
                Vector2 portalPos = new Vector2(
                        portal.getBounds().x + portal.getBounds().width / 2f,
                        portal.getBounds().y + portal.getBounds().height / 2f
                );
                float distanceToPortal = (float) Math.sqrt(
                        Math.pow(merchantX - portalPos.x, 2) +
                                Math.pow(merchantY - portalPos.y, 2)
                );
                if (distanceToPortal < CHUNK_SIZE * TILE_SIZE * 0.5f) {
                    tooCloseToPortals = true;
                    break;
                }
            }

            boolean overlapsObstacle = isMerchantOverlappingObstacles(merchantX, merchantY, 32);

            if (distanceFromSpawn >= minDistanceFromSpawn && !tooCloseToPortals && !overlapsObstacle) {
                validPosition = true;
            }
        }

        // Create merchant
        merchant = new Merchant(merchantX, merchantY, world.getWorld(), animationManager);

        // Add merchant to minimap
        if (minimap != null) {
            minimap.setMerchant(merchant);
        }
    }

    private boolean isMerchantOverlappingObstacles(float x, float y, float size) {
        Rectangle merchantBounds = new Rectangle(x, y, size, size);

        int chunkX = (int) Math.floor(x / (CHUNK_SIZE * TILE_SIZE));
        int chunkY = (int) Math.floor(y / (CHUNK_SIZE * TILE_SIZE));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Vector2 chunkCoord = new Vector2(chunkX + dx, chunkY + dy);
                Chunk chunk = chunks.get(chunkCoord);

                if (chunk != null && chunk.isOverlappingAnyObstacle(merchantBounds)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void relocateMerchant() {
        if (merchant != null) {
            merchant.dispose(world.getWorld());
        }

        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        int minChunk = -halfMapChunks;

        Vector2 playerSpawn = player.getPosition();
        float minDistanceFromSpawn = CHUNK_SIZE * TILE_SIZE * 0.5f;

        int attempts = 0;
        int maxAttempts = 100;
        boolean validPosition = false;
        float merchantX = 0, merchantY = 0;

        while (!validPosition && attempts < maxAttempts) {
            attempts++;

            int merchantChunkX = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);
            int merchantChunkY = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);

            float offsetX = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;
            float offsetY = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;

            merchantX = merchantChunkX * CHUNK_SIZE * TILE_SIZE + offsetX;
            merchantY = merchantChunkY * CHUNK_SIZE * TILE_SIZE + offsetY;

            float distanceFromSpawn = (float) Math.sqrt(
                    Math.pow(merchantX - playerSpawn.x, 2) +
                            Math.pow(merchantY - playerSpawn.y, 2)
            );

            boolean tooCloseToPortals = false;
            for (Portal portal : dungeonPortals) {
                Vector2 portalPos = new Vector2(
                        portal.getBounds().x + portal.getBounds().width / 2f,
                        portal.getBounds().y + portal.getBounds().height / 2f
                );
                float distanceToPortal = (float) Math.sqrt(
                        Math.pow(merchantX - portalPos.x, 2) +
                                Math.pow(merchantY - portalPos.y, 2)
                );
                if (distanceToPortal < CHUNK_SIZE * TILE_SIZE * 0.5f) {
                    tooCloseToPortals = true;
                    break;
                }
            }

            boolean overlapsObstacle = isMerchantOverlappingObstacles(merchantX, merchantY, 32);

            if (distanceFromSpawn >= minDistanceFromSpawn && !tooCloseToPortals && !overlapsObstacle) {
                validPosition = true;
            }
        }

        merchant = new Merchant(merchantX, merchantY, world.getWorld(), animationManager);

        if (minimap != null) {
            minimap.setMerchant(merchant);
        }
    }

    private void spawnAllDungeonPortals() {
        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        int minChunk = -halfMapChunks;

        Vector2 playerSpawn = player.getPosition();

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

    private boolean isPortalOverlappingObstacles(float portalX, float portalY, float portalSize) {
        Rectangle portalBounds = new Rectangle(portalX, portalY, portalSize, portalSize);

        int chunkX = (int) Math.floor(portalX / (CHUNK_SIZE * TILE_SIZE));
        int chunkY = (int) Math.floor(portalY / (CHUNK_SIZE * TILE_SIZE));

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

        SoundManager.getInstance().stopGrassRunning();
        SoundManager.getInstance().playDungeonMusic();

        inDungeon = true;
        inBossRoom = false;
        itemSpawner.clear();
        overworldPlayerPosition = new Vector2(player.getPosition());

        for (Chunk chunk : chunks.values()) {
            chunk.disableObstacles();
            chunk.disableEnemies();
        }

        if (mapBoundary != null) {
            mapBoundary.disable();
        }

        // Disable merchant while in dungeon
        if (merchant != null) {
            merchant.disable();
        }

        int dungeonTileSize = (int) (TILE_SIZE / 1.2f);
        currentDungeon = new Dungeon(100, 100, dungeonTileSize, random, world.getWorld(), player, animationManager);
        dungeonMinimap = new DungeonMinimap(100, 100, dungeonTileSize, player, currentDungeon);

        Vector2 spawnPoint = currentDungeon.getSpawnPoint();
        player.getBody().setTransform(spawnPoint.x, spawnPoint.y, 0);

        hudLabel.setText("Find the boss portal!");

        // Randomize merchant position and shop inventory for when player returns
        relocateMerchant();
        merchantShop.randomizeShopInventory();
    }

    private void enterBossRoom() {
        SoundManager.getInstance().playBossMusic();

        inBossRoom = true;
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
            chunk.disableObstacles();
            chunk.disableEnemies();
        }

        if (mapBoundary != null) {
            mapBoundary.disable();
        }

        int bossRoomTileSize = (int) (TILE_SIZE / 1.2f);
        String bossName;

        if (!bossKittyDefeated && !cyclopsDefeated) {
            currentBossRoom = new BossRoom(bossRoomTileSize, world.getWorld(), player, animationManager, BossRoom.BossType.GHOST_BOSS);
            bossName = "Vengeful Spirit";
        }
        else if (bossKittyDefeated) {
            currentBossRoom = new BossRoom(bossRoomTileSize, world.getWorld(), player, animationManager, BossRoom.BossType.CYCLOPS);
            bossName = "Cyclops";
        }
        else {
            currentBossRoom = new BossRoom(bossRoomTileSize, world.getWorld(), player, animationManager, BossRoom.BossType.BOSS_KITTY);
            bossName = "Freydis";
        }

        Vector2 spawnPoint = currentBossRoom.getSpawnPoint();
        player.getBody().setTransform(spawnPoint.x, spawnPoint.y, 0);

        hudLabel.setText("Defeat the " + bossName + "!");
    }

    private void exitBossRoom() {
        inBossRoom = false;
        inDungeon = false;
        itemSpawner.clear();

        SoundManager.getInstance().playForestMusic();

        if (currentBossRoom != null) {
            currentBossRoom.dispose();
            currentBossRoom = null;
        }

        for (Chunk chunk : chunks.values()) {
            chunk.enableObstacles();
            chunk.enableEnemies();
        }

        if (mapBoundary != null) {
            mapBoundary.enable();
        }

        if (merchant != null) {
            merchant.enable();
        }

        if (overworldPlayerPosition != null) {
            player.getBody().setTransform(overworldPlayerPosition.x, overworldPlayerPosition.y, 0);
        }

        if (!hermanSpawned && ghostBossDefeated) {
            spawnHerman();
        }

        hudLabel.setText("Enemies killed: " + enemiesKilled);
    }

    private void exitDungeon() {
        inDungeon = false;
        inBossRoom = false;
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

        // Re-enable merchant
        if (merchant != null) {
            merchant.enable();
        }

        if (overworldPlayerPosition != null) {
            player.getBody().setTransform(overworldPlayerPosition.x, overworldPlayerPosition.y, 0);
        }

        hudLabel.setText("Enemies killed: " + enemiesKilled);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (merchantShopOpen) {
                merchantShop.close();
                merchantShopOpen = false;
                return;
            }

            if (player.getInventory().isInventoryOpen())
                player.getInventory().toggleInventory();

            if (minimap.isMapOpen())
                minimap.toggleMap();

            if (isPaused) {
                unpauseGame();
            } else {
                pauseGame();
            }
        }

        SoundManager.getInstance().update(delta);

        if (Gdx.input.isKeyPressed(Input.Keys.F5) && !isPaused) {
            gameScreen.switchToNewState(GameScreen.START);
            return;
        }

        settings.update(delta);

        if (settings != null && isPaused && !settings.isOpen()) {
            unpauseGame();
        }

        if (merchantShopOpen && merchantShop != null) {
            merchantShop.update(delta);
            if (!merchantShop.isOpen()) {
                merchantShopOpen = false;
            }
        }

        updateCursorConfinement();

        if (!isPaused && !merchantShopOpen) {
            world.getWorld().step(1 / 60f, 6, 2);

            if (!inDungeon && !inBossRoom) {
                if (minimap != null) {
                    minimap.update();
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    minimap.toggleMap();
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.P) && minimap.isMapOpen()) {
                    minimap.togglePortalDisplay();
                }
            } else if (inDungeon) {
                if (dungeonMinimap != null) {
                    dungeonMinimap.update();
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    dungeonMinimap.toggleMap();
                }
            }

            if (!inDungeon && !inBossRoom) {
                renderOverworld(delta);
            } else if (inDungeon) {
                renderDungeon(delta);
            } else if (inBossRoom) {
                renderBossRoom(delta);
            }
        } else {
            if (!inDungeon && !inBossRoom) {
                renderOverworld(0);
            } else if (inDungeon) {
                renderDungeon(0);
            } else if (inBossRoom) {
                renderBossRoom(0);
            }
        }

        if (delta > 0) {
            checkForDeadEnemies();
        }

        if (hudStage != null) {
            hudStage.act(delta);
            hudStage.draw();
        }

        if (merchantShopOpen && merchantShop != null) {
            merchantShop.render(batch, false);
        }

        if (settings != null && settings.isOpen()) {
            settings.render(batch, false);
        }

        if (useCustomCursor && batch != null) {
            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();

            float cursorX = Math.max(0, Math.min(Gdx.input.getX(), Gdx.graphics.getWidth()));
            float cursorY = Math.max(0, Math.min(Gdx.graphics.getHeight() - Gdx.input.getY(), Gdx.graphics.getHeight()));

            batch.draw(cursorTexture, cursorX - cursorTexture.getWidth() / 4f, cursorY - cursorTexture.getHeight() / 3f, 32, 32);
            batch.end();
        }

        if (world != null) {
            Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
//            debugRenderer.render(world.getWorld(), camera.combined);
        }
    }

    private void pauseGame() {
        isPaused = true;
        settings.open();
        player.setPaused(true);
    }

    private void unpauseGame() {
        isPaused = false;
        settings.close();
        player.setPaused(false);
    }

    private void renderOverworld(float delta) {
        if (batch == null) return;

        if (!inDungeon && !inBossRoom) {
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

        if (merchant != null && merchant.isActive()) {
            if (delta > 0) {
                merchant.update(delta);
            }
            merchant.render(batch);

            if (!isPaused && !merchantShopOpen && merchant.isPlayerNear(player.getPosition()) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                merchantShop.open();
                merchantShopOpen = true;
            }
        }

        for (Portal portal : dungeonPortals) {
            if (delta > 0) {
                portal.update(delta);
            }
            portal.render(batch);

            if (!isPaused && !merchantShopOpen && portal.isPlayerNear(player.getPosition(), 20f) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E) && !portal.getIsCleared()) {
                enterDungeon();
                break;
            }
        }

        if (herman != null && !herman.isMarkedForRemoval()) {
            if (delta > 0) {
                herman.update(delta);
            }
            herman.render(batch);
        }

        if (hermanDuplicate != null && !hermanDuplicate.isMarkedForRemoval()) {
            if (delta > 0) {
                hermanDuplicate.update(delta);
            }
            hermanDuplicate.render(batch);
        }

        if (delta > 0) {
            statusEffectTimer += delta;
        }
        StatusEffectRenderer.render(batch, statusEffects, statusEffectTimer);

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

        if (delta > 0 && player != null) {
            itemSpawner.update(delta);
            itemSpawner.checkPickups(player, player.getInventory());

            if (minimap != null) {
                minimap.update();
            }
        }

        if (batch != null) {
            batch.setProjectionMatrix(hudCamera.combined);
            if (player.getInventory().isOpen()) {
                player.getInventory().render(batch, false, player);
            }

            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();
            if (player != null && player.getAbilityManager() != null) {
                player.getAbilityManager().renderSkillTree(batch);
            }
            player.renderSkillBar(batch);
            player.renderBuffIcons(batch);
            renderExpBar(batch);
            batch.end();

            if (minimap != null && minimap.isMapOpen()) {
                minimap.render(batch, false);
            }

            batch.setProjectionMatrix(camera.combined);
        }
    }

    private void renderDungeon(float delta) {
        if (batch == null) return;

        float dungeonMinX = 0;
        float dungeonMinY = 0;
        float dungeonMaxX = currentDungeon.getWidth() * currentDungeon.getTileSize();
        float dungeonMaxY = currentDungeon.getHeight() * currentDungeon.getTileSize();

        float cameraHalfWidth = camera.viewportWidth / 2f;
        float cameraHalfHeight = camera.viewportHeight / 2f;

        float targetX = player.getPosition().x;
        float targetY = player.getPosition().y;

        float clampedX = Math.max(dungeonMinX + cameraHalfWidth, Math.min(targetX, dungeonMaxX - cameraHalfWidth));
        float clampedY = Math.max(dungeonMinY + cameraHalfHeight, Math.min(targetY, dungeonMaxY - cameraHalfHeight));

        camera.position.set(clampedX, clampedY, 0);
        camera.update();

        if (delta > 0) {
            checkForDeadEnemies();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (currentDungeon != null) {
            currentDungeon.render(batch);
            currentDungeon.renderPortal(batch, delta);
            itemSpawner.render(batch);

            if (delta > 0) {
                currentDungeon.renderEnemies(batch, delta);
                currentDungeon.updateEnemies();
            } else {
                currentDungeon.renderEnemies(batch, 0);
            }

            if (!isPaused && currentDungeon.isPlayerAtBossPortal(player.getPosition()) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                batch.end();
                enterBossRoom();
                return;
            }
        }

        if (delta > 0) {
            statusEffectTimer += delta;
        }
        StatusEffectRenderer.render(batch, statusEffects, statusEffectTimer);

        player.render(batch, PLAYER_TILE_SIZE);
        player.renderAbilityEffects(batch);

        if (delta > 0) {
            player.update(delta);
            updateStatusEffects(delta);
        }

        if (batch != null) {
            batch.end();

            if (delta > 0) {
                itemSpawner.update(delta);
                itemSpawner.checkPickups(player, player.getInventory());
            }

            batch.setProjectionMatrix(hudCamera.combined);

            if (player.getInventory().isOpen()) {
                player.getInventory().render(batch, false, player);
            }

            if (player != null && player.getAbilityManager() != null) {
                player.getAbilityManager().renderSkillTree(batch);
            }

            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();
            player.renderSkillBar(batch);
            player.renderBuffIcons(batch);
            renderExpBar(batch);
            batch.end();

            if (dungeonMinimap != null && dungeonMinimap.isMapOpen()) {
                dungeonMinimap.render(batch, false);
            }
        }
    }

    private void renderBossRoom(float delta) {
        if (batch == null) return;

        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.update();

        if (delta > 0) {
            checkForDeadEnemies();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (currentBossRoom != null) {
            currentBossRoom.render(batch);
            itemSpawner.render(batch);

            if (delta > 0) {
                currentBossRoom.renderBoss(batch, delta);
            } else {
                currentBossRoom.renderBoss(batch, 0);
            }

            if (currentBossRoom.isBossDefeated() && currentBossRoom.getExitPortal() != null) {
                currentBossRoom.getExitPortal().render(batch);
            }

            if (!isPaused && currentBossRoom.isPlayerAtExit(player.getPosition()) &&
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                batch.end();
                exitBossRoom();
                return;
            }
        }

        if (delta > 0) {
            statusEffectTimer += delta;
        }
        StatusEffectRenderer.render(batch, statusEffects, statusEffectTimer);

        player.render(batch, PLAYER_TILE_SIZE);
        player.renderAbilityEffects(batch);

        if (delta > 0) {
            player.update(delta);
            updateStatusEffects(delta);
        }

        if (batch != null) {
            batch.end();

            if (delta > 0) {
                itemSpawner.update(delta);
                itemSpawner.checkPickups(player, player.getInventory());
            }

            batch.setProjectionMatrix(hudCamera.combined);

            if (player.getInventory().isOpen()) {
                player.getInventory().render(batch, false, player);
            }

            if (player != null && player.getAbilityManager() != null) {
                player.getAbilityManager().renderSkillTree(batch);
            }

            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();
            player.renderSkillBar(batch);
            player.renderBuffIcons(batch);
            renderExpBar(batch);
            batch.end();
        }
    }

    private void renderExpBar(SpriteBatch batch) {
        float barWidth = hudViewport.getWorldWidth() - 200f;
        float barHeight = 15f;
        float barX = (hudViewport.getWorldWidth() - barWidth) / 2f;
        float barY = 10f;

        Texture pixel = Storage.assetManager.get("white_pixel.png", Texture.class);

        batch.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        batch.draw(pixel, barX, barY, barWidth, barHeight);

        float xpPercent = (float) player.getStats().getExperience() / player.getStats().getExperienceToNextLevel();
        batch.setColor(0.6f, 0.2f, 0.8f, 1f);
        batch.draw(pixel, barX, barY, barWidth * xpPercent, barHeight);

        batch.setColor(1, 1, 1, 1);
        String xpText = player.getStats().getExperience() + "/" + player.getStats().getExperienceToNextLevel();
        BitmapFont font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        font.setColor(Color.WHITE);
        font.getData().setScale(0.45f);

        GlyphLayout layout = new GlyphLayout(font, xpText);
        font.draw(batch, xpText, barX + (barWidth - layout.width) / 2f, barY + barHeight / 2f + layout.height / 2f);

        font.getData().setScale(1f);
    }

    private void scheduleChunkGeneration(int chunkX, int chunkY) {
        if (inDungeon || inBossRoom) return;

        chunkGenerator.submit(() -> {
            Vector2 chunkCoord = new Vector2(chunkX, chunkY);
            Chunk newChunk = new Chunk(chunkX, chunkY, CHUNK_SIZE, TILE_SIZE, random, world.getWorld(), player, animationManager);
            pendingChunks.add(newChunk);
            chunks.put(chunkCoord, newChunk);
        });
    }

    public void generateChunksAroundPlayer() {
        if (inDungeon || inBossRoom || isPaused) return;

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

    public BossRoom getCurrentBossRoom() {
        return currentBossRoom;
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

            String lootTable = boss.getStats().getLootTableType();
            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPosition);

            player.getStats().addExperience(boss.getStats().getExpReward());

            if (inBossRoom && currentBossRoom != null && currentBossRoom.getBoss() == boss) {
                bossKittyDefeated = true;
                currentBossRoom.onBossDefeated();
                hudLabel.setText("Freydis defeated! Use the portal to return!");
            } else {
                bossSpawned = false;
            }

        } else if (enemy instanceof Cyclops) {
            Cyclops cyclopsEnemy = (Cyclops) enemy;

            String lootTable = cyclopsEnemy.getStats().getLootTableType();
            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPosition);

            player.getStats().addExperience(cyclopsEnemy.getStats().getExpReward());

            if (inBossRoom && currentBossRoom != null && currentBossRoom.getCyclops() == cyclopsEnemy) {
                cyclopsDefeated = true;
                currentBossRoom.onBossDefeated();
                hudLabel.setText("Cyclops defeated! Use the portal to return!");
            }

        } else if (enemy instanceof GhostBoss) {
            GhostBoss ghostBoss = (GhostBoss) enemy;

            String lootTable = ghostBoss.getStats().getLootTableType();
            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPosition);

            player.getStats().addExperience(ghostBoss.getStats().getExpReward());

            if (inBossRoom && currentBossRoom != null && currentBossRoom.getGhostBoss() == ghostBoss) {
                ghostBossDefeated = true;
                currentBossRoom.onBossDefeated();
                hudLabel.setText("Vengeful Spirit defeated! Use the portal to return");
            }
        }

        else if (enemy instanceof Herman) {
            Herman hermanBoss = (Herman) enemy;

            String lootTable = hermanBoss.getStats().getLootTableType();
            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPosition);

            player.getStats().addExperience(hermanBoss.getStats().getExpReward());
        }

        else if (enemy instanceof DungeonEnemy) {
            DungeonEnemy dungeonEnemy = (DungeonEnemy) enemy;

            String lootTable = dungeonEnemy.getStats().getLootTableType();
            lootTableRegistry.spawnLoot(lootTable, itemSpawner, enemyPosition);

            player.getStats().addExperience(dungeonEnemy.getStats().getExpReward());
        }
    }

    public void checkForDeadEnemies() {
        List<Body> bodiesToDestroy = new ArrayList<>();

        if (!inDungeon && !inBossRoom) {
            for (Chunk chunk : chunks.values()) {
                for (Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                    if (enemy.isMarkedForRemoval() && enemy.getBody() != null) {
                        handleEnemyDeath(enemy, enemy.getBody().getPosition(), false);
                        bodiesToDestroy.add(enemy.getBody());
                        enemy.clearBody();
                        chunk.getEnemies().remove(enemy);
                    }
                }

                if (herman != null) {
                    if (herman.isMarkedForRemoval() && herman.getBody() != null) {
                        handleEnemyDeath(herman, herman.getBody().getPosition(), true);
                        bodiesToDestroy.add(herman.getBody());
                        herman.clearBody();
                    }
                }

                if (hermanDuplicate != null) {
                    if (hermanDuplicate.isMarkedForRemoval() && hermanDuplicate.getBody() != null) {
                        handleEnemyDeath(hermanDuplicate, hermanDuplicate.getBody().getPosition(), true);
                        bodiesToDestroy.add(hermanDuplicate.getBody());
                        hermanDuplicate.clearBody();
                    }
                }

                checkHermanFullyDefeated();
            }
        }

        if (inDungeon && currentDungeon != null) {
            for (DungeonEnemy enemy : new ArrayList<>(currentDungeon.getEnemies())) {
                if (enemy.isMarkedForRemoval() && enemy.getBody() != null) {
                    handleEnemyDeath(enemy, enemy.getBody().getPosition(), false);
                    bodiesToDestroy.add(enemy.getBody());
                    enemy.clearBody();
                    currentDungeon.getEnemies().remove(enemy);
                }
            }
        }

        if (inBossRoom && currentBossRoom != null) {
            BossKitty bossRoomBoss = currentBossRoom.getBoss();
            if (bossRoomBoss != null && bossRoomBoss.isMarkedForRemoval() && bossRoomBoss.getBody() != null) {
                handleEnemyDeath(bossRoomBoss, bossRoomBoss.getBody().getPosition(), true);
                bodiesToDestroy.add(bossRoomBoss.getBody());
                bossRoomBoss.clearBody();
                currentBossRoom.setBoss(null);
            }

            Cyclops cyclopsRoomBoss = currentBossRoom.getCyclops();
            if (cyclopsRoomBoss != null && cyclopsRoomBoss.isMarkedForRemoval() && cyclopsRoomBoss.getBody() != null) {
                handleEnemyDeath(cyclopsRoomBoss, cyclopsRoomBoss.getBody().getPosition(), true);
                bodiesToDestroy.add(cyclopsRoomBoss.getBody());
                cyclopsRoomBoss.clearBody();
                currentBossRoom.setCyclops(null);
            }

            GhostBoss ghostBoss = currentBossRoom.getGhostBoss();
            if (ghostBoss != null && ghostBoss.isMarkedForRemoval() && ghostBoss.getBody() != null) {
                handleEnemyDeath(ghostBoss, ghostBoss.getBody().getPosition(), true);
                bodiesToDestroy.add(ghostBoss.getBody());
                ghostBoss.clearBody();
                currentBossRoom.setGhostBoss(null);
            }
        }

        for (Body body : bodiesToDestroy) {
            if (body != null) {
                world.getWorld().destroyBody(body);
            }
        }
    }

    private void spawnHerman() {
        if (hermanSpawned) return;

        int halfMapChunks = MAP_SIZE_CHUNKS / 2;
        int minChunk = -halfMapChunks;

        Vector2 playerSpawn = player.getPosition();
        float minDistanceFromSpawn = CHUNK_SIZE * TILE_SIZE * 1.5f;

        int attempts = 0;
        int maxAttempts = 100;
        boolean validPosition = false;
        float hermanX = 0, hermanY = 0;

        while (!validPosition && attempts < maxAttempts) {
            attempts++;

            int hermanChunkX = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);
            int hermanChunkY = minChunk + 1 + random.nextInt(MAP_SIZE_CHUNKS - 2);

            float offsetX = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;
            float offsetY = random.nextFloat() * CHUNK_SIZE * TILE_SIZE * 0.5f;

            hermanX = hermanChunkX * CHUNK_SIZE * TILE_SIZE + offsetX;
            hermanY = hermanChunkY * CHUNK_SIZE * TILE_SIZE + offsetY;

            float distanceFromPlayer = (float) Math.sqrt(
                    Math.pow(hermanX - playerSpawn.x, 2) +
                            Math.pow(hermanY - playerSpawn.y, 2)
            );

            boolean tooCloseToPortals = false;
            for (Portal portal : dungeonPortals) {
                Vector2 portalPos = new Vector2(
                        portal.getBounds().x + portal.getBounds().width / 2f,
                        portal.getBounds().y + portal.getBounds().height / 2f
                );
                float distanceToPortal = (float) Math.sqrt(
                        Math.pow(hermanX - portalPos.x, 2) +
                                Math.pow(hermanY - portalPos.y, 2)
                );
                if (distanceToPortal < CHUNK_SIZE * TILE_SIZE) {
                    tooCloseToPortals = true;
                    break;
                }
            }

            if (distanceFromPlayer >= minDistanceFromSpawn && !tooCloseToPortals) {
                validPosition = true;
            }
        }

        Body hermanBody = createHermanBody(hermanX, hermanY);

        EnemyStats hermanStats = EnemyStats.Factory.createHerman(4); // Level 4 mega boss
        herman = new Herman(
                new Rectangle(hermanX - 24, hermanY - 24, 48, 48),
                hermanBody,
                player,
                animationManager,
                hermanStats
        );
        hermanBody.setUserData(herman);

        herman.setDuplicateSpawnCallback((position, health) -> {
            spawnHermanDuplicate(position, health);
        });

        hermanSpawned = true;
        hudLabel.setText("A powerful presence awakens in the forest...");

        if (minimap != null) {
             minimap.setHerman(herman);
        }
    }

    private void spawnHermanDuplicate(Vector2 position, int health) {
        if (hermanDuplicate != null) return; // Already spawned

        Body duplicateBody = createHermanBody(position.x, position.y);

        EnemyStats duplicateStats = EnemyStats.Factory.createHerman(4);
        duplicateStats.setCurrentHealth(health);

        hermanDuplicate = new Herman(
                new Rectangle(position.x - 24, position.y - 24, 48, 48),
                duplicateBody,
                player,
                animationManager,
                duplicateStats,
                true,
                herman
        );
        duplicateBody.setUserData(hermanDuplicate);

        // Duplicate is already activated since it spawns during combat
        hudLabel.setText("Herman has split in two!");
    }

    private Body createHermanBody(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody; // Herman is stationary
        bodyDef.position.set(x, y);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(16f, 16f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ABILITY;

        Body body = world.getWorld().createBody(bodyDef);
        body.createFixture(fixtureDef);
        body.setFixedRotation(true);
        shape.dispose();

        return body;
    }

    private void onHermanFullyDefeated() {
        hermanDefeated = true;
        hudLabel.setText("Herman has been defeated! The forest is at peace.");

        // Remove arena barriers
        for (Body barrier : hermanArenaBarriers) {
            if (barrier != null) {
                world.getWorld().destroyBody(barrier);
            }
        }
        hermanArenaBarriers.clear();

         SoundManager.getInstance().playForestMusic();
    }

    private void checkHermanFullyDefeated() {
        if (!hermanSpawned || hermanDefeated) return;

        boolean mainDead = (herman == null) ||
                (herman.isMarkedForRemoval()) ||
                (herman.getStats().isDead());

        // If main hasn't duplicated yet, only check main
        boolean duplicateDead = true;
        if (herman != null && herman.hasDuplicated()) {
            duplicateDead = (hermanDuplicate == null) ||
                    (hermanDuplicate.isMarkedForRemoval()) ||
                    (hermanDuplicate.getStats().isDead());
        }

        if (mainDead && duplicateDead) {
            onHermanFullyDefeated();
            // Now we can null them out
            herman = null;
            hermanDuplicate = null;
        }
    }

    public Herman getHerman() {
        return herman;
    }

    public Herman getHermanDuplicate() {
        return hermanDuplicate;
    }

    public boolean isHermanSpawned () {
        return hermanSpawned;
    }

    public void safeExit() {
        try {
            dispose();
        } catch (Exception e) {
            System.err.println("Error during safe exit: " + e.getMessage());
        } finally {
            Gdx.app.exit();
        }
    }

    public boolean isInDungeon() {
        return inDungeon;
    }

    public boolean isInBossRoom() {
        return inBossRoom;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hudViewport.update(width, height, true);

        setupCameraProjection();

        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
        if (hudStage != null) {
            hudStage.getViewport().update(width, height, true);
        }
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
            if (player != null) {
                player.cleanupSpears();
                player = null;
            }

            if (chunkGenerator != null && !chunkGenerator.isShutdown()) {
                chunkGenerator.shutdownNow();
                try {
                    if (!chunkGenerator.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.err.println("Chunk generator thread pool did not terminate in time");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (pendingChunks != null) {
                pendingChunks.clear();
            }

            if (chunks != null) {
                for (Chunk chunk : chunks.values()) {
                    if (chunk != null) {
                        chunk.dispose();
                    }
                }
                chunks.clear();
            }

            if (settings != null) {
                settings.dispose();
                settings = null;
            }

            if (merchantShop != null) {
                merchantShop.dispose();
                merchantShop = null;
            }

            if (merchant != null) {
                merchant.dispose(world.getWorld());
                merchant = null;
            }

            if (dungeonMinimap != null) {
                dungeonMinimap.dispose();
                dungeonMinimap = null;
            }
            if (minimap != null) {
                minimap.dispose();
                minimap = null;
            }

            if (currentDungeon != null) {
                currentDungeon.dispose();
                currentDungeon = null;
            }

            if (currentBossRoom != null) {
                currentBossRoom.dispose();
                currentBossRoom = null;
            }

            if (dungeonPortals != null) {
                for (Portal portal : dungeonPortals) {
                    if (portal != null) {
                        portal.dispose(world.getWorld());
                    }
                }
                dungeonPortals.clear();
            }

            if (herman != null) {
                if (herman.getBody() != null) {
                    world.getWorld().destroyBody(herman.getBody());
                }
                herman.dispose();
                herman = null;
            }

            for (Body barrier : hermanArenaBarriers) {
                if (barrier != null) {
                    world.getWorld().destroyBody(barrier);
                }
            }
            hermanArenaBarriers.clear();

            if (statusEffects != null) {
                statusEffects.clear();
            }

            if (hudStage != null) {
                hudStage.dispose();
                hudStage = null;
            }
            if (stage != null) {
                stage.dispose();
                stage = null;
            }

            if (batch != null) {
                batch.dispose();
                batch = null;
            }

            if (world != null) {
                world.dispose();
                world = null;
            }

            if (herman != null) {
                if (herman.getBody() != null) {
                    world.getWorld().destroyBody(herman.getBody());
                }
                herman.dispose();
                herman = null;
            }

            if (hermanDuplicate != null) {
                if (hermanDuplicate.getBody() != null) {
                    world.getWorld().destroyBody(hermanDuplicate.getBody());
                }
                hermanDuplicate.dispose();
                hermanDuplicate = null;
            }

            SoundManager.getInstance().dispose();

            System.gc();

        } catch (Exception e) {
            System.err.println("Error during disposal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void beginContact(Contact contact) {
        if (isPaused) return;

        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        short categoryA = fixtureA.getFilterData().categoryBits;
        short categoryB = fixtureB.getFilterData().categoryBits;

        // ==== SPEAR HIT ENEMY ====
        if ((categoryA == CollisionFilter.SPEAR && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.SPEAR && categoryA == CollisionFilter.ENEMY)) {

            Body spearBody = (categoryA == CollisionFilter.SPEAR) ? fixtureA.getBody() : fixtureB.getBody();
            Body enemyBody = categoryA == CollisionFilter.ENEMY ? fixtureA.getBody() : fixtureB.getBody();

            int playerDamage = player.getStats().getTotalDamage();

            if (!inDungeon && !inBossRoom) {
                for (Chunk chunk : chunks.values()) {
                    if (!bossSpawned) {
                        for (Enemy enemy : chunk.getEnemies()) {
                            if (enemy.getBody() == enemyBody) {
                                enemy.takeDamage(playerDamage);
                                break;
                            }
                        }
                    } else {
                        for (BossKitty boss : chunk.getBossKitty()) {
                            if (boss.getBody() == enemyBody) {
                                boss.takeDamage(playerDamage);
                                break;
                            }
                        }
                    }
                }
            } else if (inDungeon && currentDungeon != null) {
                for (entities.DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    if (enemy.getBody() == enemyBody) {
                        enemy.takeDamage(playerDamage);
                        break;
                    }
                }
            } else if (inBossRoom && currentBossRoom != null) {
                BossKitty boss = currentBossRoom.getBoss();
                if (boss != null && boss.getBody() == enemyBody) {
                    boss.takeDamage(playerDamage);
                }

                Cyclops cyclops = currentBossRoom.getCyclops();
                if (cyclops != null && cyclops.getBody() == enemyBody) {
                    cyclops.takeDamage(playerDamage);
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

            if (!inDungeon && !inBossRoom) {
                for (Chunk chunk : chunks.values()) {
                    if(!bossSpawned) {
                        for (Enemy enemy : chunk.getEnemies()) {
                            if (enemy.getBody() == enemyBody && !player.isInvulnerable()) {
                                enemy.damagePlayer();
                                if (player.getStats().isDead()) {
                                    player.playerDie();
                                }
                                break;
                            }
                        }
                    } else {
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
            } else if (inDungeon && currentDungeon != null) {
                for (entities.DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    if (enemy.getBody() == enemyBody) {
                        enemy.damagePlayer();
                        if (player.getStats().isDead()) {
                            player.playerDie();
                        }
                        break;
                    }
                }
            } else if (inBossRoom && currentBossRoom != null) {
                BossKitty boss = currentBossRoom.getBoss();
                if (boss != null && boss.getBody() == enemyBody && !player.isInvulnerable()) {
                    boss.damagePlayer();
                    if (player.getStats().isDead()) {
                        player.playerDie();
                    }
                }

                Cyclops cyclops = currentBossRoom.getCyclops();
                if (cyclops != null && cyclops.getBody() == enemyBody && !player.isInvulnerable()) {
                    cyclops.damagePlayer();
                    if (player.getStats().isDead()) {
                        player.playerDie();
                    }
                }
            }
        }

        // ==== ABILITY (BUBBLE) HIT ENEMY ====
        if ((categoryA == CollisionFilter.ABILITY && categoryB == CollisionFilter.ENEMY) ||
                (categoryB == CollisionFilter.ABILITY && categoryA == CollisionFilter.ENEMY)) {

            Body abilityBody = (categoryA == CollisionFilter.ABILITY) ? fixtureA.getBody() : fixtureB.getBody();
            Body enemyBody = (categoryA == CollisionFilter.ENEMY) ? fixtureA.getBody() : fixtureB.getBody();

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

        // ==== ABILITY (BUBBLE) HIT PROJECTILE ====
        if ((categoryA == CollisionFilter.ABILITY && categoryB == CollisionFilter.PROJECTILE) ||
                (categoryB == CollisionFilter.ABILITY && categoryA == CollisionFilter.PROJECTILE)) {

            Body abilityBody = (categoryA == CollisionFilter.ABILITY) ? fixtureA.getBody() : fixtureB.getBody();
            Body projectileBody = (categoryA == CollisionFilter.PROJECTILE) ? fixtureA.getBody() : fixtureB.getBody();
            Fixture projectileFixture = (categoryA == CollisionFilter.PROJECTILE) ? fixtureA : fixtureB;

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

            if (!inDungeon && !inBossRoom) {
                for (Chunk chunk : chunks.values()) {
                    for (Enemy enemy : chunk.getEnemies()) {
                        for (Projectile projectile : enemy.getProjectiles()) {
                            if (projectile.getBody() == reflectBody) {
                                for (Enemy targetEnemy : chunk.getEnemies()) {
                                    if (targetEnemy.getBody() == enemyBody) {
                                        targetEnemy.takeDamage(projectile.getDamage());
                                        projectile.markForRemoval();
                                        break;
                                    }
                                }
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
            } else if (inDungeon && currentDungeon != null) {
                for (DungeonEnemy enemy : currentDungeon.getEnemies()) {
                    for (Projectile projectile : enemy.getProjectiles()) {
                        if (projectile.getBody() == reflectBody) {
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
            } else if (inBossRoom && currentBossRoom != null) {
                BossKitty boss = currentBossRoom.getBoss();
                Cyclops cyclops = currentBossRoom.getCyclops();
            }
        }

        // ==== REFLECTED PROJECTILE HIT OBSTACLE ====
        if ((categoryA == CollisionFilter.REFLECT && categoryB == CollisionFilter.OBSTACLE) ||
                (categoryB == CollisionFilter.REFLECT && categoryA == CollisionFilter.OBSTACLE)) {

            Body reflectBody = (categoryA == CollisionFilter.REFLECT) ? fixtureA.getBody() : fixtureB.getBody();

            if (!inDungeon && !inBossRoom) {
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
            } else if (inDungeon && currentDungeon != null) {
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

        if ((fixtureA.getFilterData().categoryBits & CollisionFilter.ENEMY_ENEMY) != 0 &&
                (fixtureB.getFilterData().categoryBits & CollisionFilter.ENEMY_ENEMY) != 0) {

            Body enemyBody2 = (categoryA == CollisionFilter.ENEMY_ENEMY) ? fixtureA.getBody() : fixtureB.getBody();
            Body enemyBody = (categoryA == CollisionFilter.ENEMY_ENEMY) ? fixtureA.getBody() : fixtureB.getBody();

            Vector2 enemyPos2 = enemyBody2.getPosition();
            Vector2 enemyPos = enemyBody.getPosition();
            Vector2 pushDirection = new Vector2(enemyPos.x - enemyPos2.x, enemyPos.y - enemyPos2.y).nor();

            float pushForce = 500f;
            enemyBody.applyLinearImpulse(
                    pushDirection.x * pushForce,
                    pushDirection.y * pushForce,
                    enemyPos.x,
                    enemyPos.y,
                    true
            );
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

    public ConcurrentHashMap<Vector2, Chunk> getChunks() {
        return chunks;
    }

    public void addStatusEffect(Object target, StatusEffect effect) {
        if (!statusEffects.containsKey(target)) {
            statusEffects.put(target, new java.util.ArrayList<>());
        }
        statusEffects.get(target).add(effect);
    }

    public void updateStatusEffects(float delta) {
        java.util.Iterator<java.util.Map.Entry<Object, List<StatusEffect>>> it = statusEffects.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Object, List<StatusEffect>> entry = it.next();
            List<StatusEffect> effects = entry.getValue();

            java.util.Iterator<StatusEffect> effectIt = effects.iterator();
            while (effectIt.hasNext()) {
                StatusEffect effect = effectIt.next();
                effect.update(delta);

                if (effect.isExpired()) {
                    effect.onExpire();
                    effectIt.remove();
                }
            }

            if (effects.isEmpty()) {
                it.remove();
            }
        }
    }
}