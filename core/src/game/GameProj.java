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
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.Viewport;

import config.GameScreen;
import config.Storage;
import entities.Player;
import managers.AnimationManager;
import managers.Box2DWorld;
import managers.Chunk;
import managers.CollisionFilter;

public class GameProj implements Screen, ContactListener {
    private Skin skin;
    private Viewport viewport;
    public Stage stage;
    private Game game;
    private GameScreen gameScreen;
    private Storage storage;
    private SpriteBatch batch;
    private AnimationManager animationManager;
    
    private OrthographicCamera camera;
    private Box2DWorld world;

    private Texture groundTexture, playerTexture;
    private Player player;
    private ConcurrentHashMap<Vector2, Chunk> chunks = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<Chunk> pendingChunks = new ConcurrentLinkedQueue<>();
    private Random random;
    private ExecutorService chunkGenerator;

    private final int CHUNK_SIZE = 32;
    private final int TILE_SIZE = 18;
    private final int PLAYER_TILE_SIZE = 32;

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
        playerTexture = Storage.assetManager.get("player.png", Texture.class);

        player = new Player(world, animationManager, PLAYER_TILE_SIZE, this);
        batch = new SpriteBatch();

        generateChunk(0, 0);
    }

    @Override
    public void show() {
        // Called when this screen becomes active
    }

    @Override
    public void render(float delta) {
    	world.getWorld().step(1 / 60f, 6, 2);
    	
    	cullDistantChunks();
    	
    	while (!pendingChunks.isEmpty()) {
            Chunk chunk = pendingChunks.poll();
            if (chunk != null) {
                chunk.addBodiesToWorld(world.getWorld());
            }
        }
    	
        camera.position.set(player.getPosition().x, player.getPosition().y, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        
        for (Chunk chunk : chunks.values()) {
            chunk.render(batch, groundTexture);
        }

        player.render(batch, PLAYER_TILE_SIZE);
        player.update(delta);
        
        batch.end();
        
        Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
        debugRenderer.render(world.getWorld(), camera.combined);
        
        if (Gdx.input.isKeyPressed(Input.Keys.F5)) gameScreen.switchToNewState(GameScreen.HOME);;
    }
    
    private void cullDistantChunks() {
        Vector2 playerChunkCoord = new Vector2(
            (int) Math.floor(player.getPosition().x / (CHUNK_SIZE * TILE_SIZE)),
            (int) Math.floor(player.getPosition().y / (CHUNK_SIZE * TILE_SIZE))
        );

        // Iterate through the chunk map and remove distant chunks
        chunks.entrySet().removeIf(entry -> {
            Vector2 chunkCoord = entry.getKey();
            double distance = Math.sqrt(Math.pow(playerChunkCoord.x - chunkCoord.x, 2) + Math.pow(playerChunkCoord.y - chunkCoord.y, 2));

            if (distance > 2) { // Cull chunks beyond a 2-chunk radius
                Chunk chunk = entry.getValue();
                chunk.dispose(); // Dispose of the chunk and its bodies
                return true; // Remove from the map
            }
            return false; // Keep the chunk
        });
    }


    private void scheduleChunkGeneration(int chunkX, int chunkY) {
        chunkGenerator.submit(() -> {
            Vector2 chunkCoord = new Vector2(chunkX, chunkY);
            Chunk newChunk = new Chunk(chunkX, chunkY, CHUNK_SIZE, TILE_SIZE, random, world.getWorld());
            pendingChunks.add(newChunk); 
            chunks.put(chunkCoord, newChunk);
        });
    }

    public void generateChunksAroundPlayer() {
		int playerChunkX = (int) Math.floor(player.getPosition().x / (CHUNK_SIZE * TILE_SIZE));
        int playerChunkY = (int) Math.floor(player.getPosition().y / (CHUNK_SIZE * TILE_SIZE));

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                Vector2 chunkCoord = new Vector2(playerChunkX + x, playerChunkY + y);
                if (!chunks.containsKey(chunkCoord)) {
                	scheduleChunkGeneration((int) chunkCoord.x, (int) chunkCoord.y);
                }
            }
        }				    		  	
    }

    private void generateChunk(int chunkX, int chunkY) {
        Vector2 chunkCoord = new Vector2(chunkX, chunkY);
        chunks.put(chunkCoord, new Chunk(chunkX, chunkY, CHUNK_SIZE, TILE_SIZE, random, world.getWorld()));
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
        playerTexture.dispose();
        world.dispose();
        batch.dispose();
        chunkGenerator.shutdown();
    }

    @Override
    public void beginContact(Contact contact) {
    	Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        
        if (isPlayerCollision(fixtureA, fixtureB)) {
            
        }
    }

    @Override
    public void endContact(Contact contact) {
        // Handle collision end
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {}

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {}

    private boolean isPlayerCollision(Fixture fixtureA, Fixture fixtureB) {
        return (fixtureA.getFilterData().categoryBits == CollisionFilter.PLAYER &&
                fixtureB.getFilterData().categoryBits == CollisionFilter.OBSTACLE) ||
               (fixtureB.getFilterData().categoryBits == CollisionFilter.PLAYER &&
                fixtureA.getFilterData().categoryBits == CollisionFilter.OBSTACLE);
    }
}