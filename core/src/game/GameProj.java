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
import entities.Enemy;
import entities.Player;
import managers.AnimationManager;
import managers.Box2DWorld;
import managers.Chunk;
import managers.CollisionFilter;

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
        
        hudLabel = new Label("0/20", skin);
        hudLabel.setPosition(10, hudViewport.getWorldHeight() - 30);
        hudStage.addActor(hudLabel);
        
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
        
        for (Chunk chunk : chunks.values()){
        	chunk.renderGround(batch, groundTexture);
        }
        
        for (Chunk chunk : chunks.values()) {
            chunk.renderObstacles(batch, player.getPosition().y, false);
        }
        
        for (Chunk chunk : chunks.values()) {
            chunk.renderEnemies(batch);
        }

        player.render(batch, PLAYER_TILE_SIZE);
        player.update(delta);
        
        for (Chunk chunk : chunks.values()) {
            chunk.renderObstacles(batch, player.getPosition().y, true);
        }
        
        batch.end();
        
        Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
        debugRenderer.render(world.getWorld(), camera.combined);
        
        if (Gdx.input.isKeyPressed(Input.Keys.F5)) gameScreen.switchToNewState(GameScreen.HOME);;
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) Player.gameStarted = false;;
   
        hudStage.act(delta);
        hudStage.draw();
    }
    
    private void cullDistantChunks() {
        Vector2 playerChunkCoord = new Vector2(
            (int) Math.floor(player.getPosition().x / (CHUNK_SIZE * TILE_SIZE)),
            (int) Math.floor(player.getPosition().y / (CHUNK_SIZE * TILE_SIZE))
        );

        chunks.entrySet().removeIf(entry -> {
            Vector2 chunkCoord = entry.getKey();
            double distance = Math.sqrt(Math.pow(playerChunkCoord.x - chunkCoord.x, 2) + Math.pow(playerChunkCoord.y - chunkCoord.y, 2));

            if (distance > 2) { 
                Chunk chunk = entry.getValue();
                chunk.dispose(); 
                return true; 
            }
            return false; 
        });
    }


    private void scheduleChunkGeneration(int chunkX, int chunkY) {
        chunkGenerator.submit(() -> {
            Vector2 chunkCoord = new Vector2(chunkX, chunkY);
            Chunk newChunk = new Chunk(chunkX, chunkY, CHUNK_SIZE, TILE_SIZE, random, world.getWorld(), player);
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
        chunks.put(chunkCoord, new Chunk(chunkX, chunkY, CHUNK_SIZE, TILE_SIZE, random, world.getWorld(), player));
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

            for (Chunk chunk : chunks.values()) {
                for (Enemy enemy : chunk.getEnemies()) { 
                    if (enemy.getBody() == enemyBody) {
                        enemy.markForRemoval();
                        enemiesKilled++;
                        hudLabel.setText("Enemies: " + enemiesKilled + "/" + 20);
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
        	
        	for (Chunk chunk : chunks.values()) {
                for (Enemy enemy : chunk.getEnemies()) { 
                    if (enemy.getBody() == enemyBody) {
                        if (enemy.emptyBody(enemyBody)) {
                        	player.playerDie();
                        }
                    }
                }
            }      	
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

	public OrthographicCamera getCamera() {
		return camera;
	}
}