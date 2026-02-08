package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
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
import entities.*;

public class Chunk {
    private final int chunkX, chunkY;
    private final int chunkSize, tileSize;

    private final List<Obstacle> obstacles;
    private final List<ObstacleInfo> pendingObstacles;
    private final List<Enemy> enemies;
    private final List<EnemyInfo> pendingEnemies;
    private final List<BossKitty> bossKitty;
    private final List<Cyclops> cyclopsList;
    private final List<Lemmy> lemmys;
    private final List<Decoration> decorations;
    private final World world;
    private final Player player;
    private final AnimationManager animationManager;
    private boolean bodiesAdded = false;

    private static final List<DecorationType> DECORATION_TYPES = new ArrayList<>();

    static {
        initializeDecorationTypes();
    }

    private static void initializeDecorationTypes() {
        DECORATION_TYPES.clear();

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/grass1.png", Texture.class),
                16, 16, 50
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/grass2.png", Texture.class),
                16, 16, 50
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/grass3.png", Texture.class),
                16, 16, 50
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/grass4.png", Texture.class),
                16, 16, 50
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/grass5.png", Texture.class),
                16, 16, 50
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/flower1.png", Texture.class),
                32, 32, 25
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/flower2.png", Texture.class),
                32, 32, 25
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/flower3.png", Texture.class),
                32, 32, 25
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/flower4.png", Texture.class),
                32, 32, 25
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/bush1.png", Texture.class),
                32, 32, 30
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/bush2.png", Texture.class),
                32, 32, 20
        ));

        DECORATION_TYPES.add(new DecorationType(
                Storage.assetManager.get("tiles/bush3.png", Texture.class),
                32, 32, 30
        ));
    }

    public static void reinitializeDecorationTypes() {
        initializeDecorationTypes();
    }

    public Chunk(int chunkX, int chunkY, int chunkSize, int tileSize, Random random, World world, Player player, AnimationManager animationManager) {
        this.animationManager = animationManager;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkSize = chunkSize;
        this.tileSize = tileSize;
        this.world = world;
        this.obstacles = new ArrayList<>();
        this.pendingObstacles = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.pendingEnemies = new ArrayList<>();
        this.bossKitty = new ArrayList<>();
        this.cyclopsList = new ArrayList<>();
        this.lemmys = new ArrayList<>();
        this.decorations = new ArrayList<>();
        this.player = player;

        generateDecorations(random);
        generateObstacles(random);
        generateEnemyClumps(random);
    }

    private void generateDecorations(Random random) {
        for (DecorationType decorationType : DECORATION_TYPES) {
            int count = decorationType.getDensity();

            for (int i = 0; i < count; i++) {
                int padding = (int) Math.max(decorationType.getWidth(), decorationType.getHeight());
                int availableSpace = chunkSize * tileSize - (padding * 2);

                if (availableSpace <= 0) {
                    continue;
                }

                // Calculate bounds to keep decorations within chunk
                float minX = chunkX * chunkSize * tileSize;
                float minY = chunkY * chunkSize * tileSize;
                float maxX = (chunkX + 1) * chunkSize * tileSize - decorationType.getWidth();
                float maxY = (chunkY + 1) * chunkSize * tileSize - decorationType.getHeight();

                // Generate position within safe bounds
                float x = minX + random.nextFloat() * (maxX - minX);
                float y = minY + random.nextFloat() * (maxY - minY);

                Decoration decoration = new Decoration(
                        decorationType.getTexture(),
                        x,
                        y,
                        decorationType.getWidth(),
                        decorationType.getHeight()
                );

                decorations.add(decoration);
            }
        }
    }

    private void generateObstacles(Random random) {
        int obstacleCount = random.nextInt(10) + 50;
        for (int i = 0; i < obstacleCount; i++) {
            float x = random.nextInt(chunkSize * tileSize) + chunkX * chunkSize * tileSize;
            float y = random.nextInt(chunkSize * tileSize) + chunkY * chunkSize * tileSize;

            Rectangle newObstacleBounds = new Rectangle(x, y, tileSize, tileSize);
            if (isOverlapping(newObstacleBounds) || isOutOfBounds(newObstacleBounds)) {
                i--;
                continue;
            }

            ObstacleInfo obstacleInfo = getRandomObstacle(random, x, y);
            newObstacleBounds.setSize(obstacleInfo.width, obstacleInfo.height);

            pendingObstacles.add(new ObstacleInfo(obstacleInfo.texture, x, y, obstacleInfo.width, obstacleInfo.height));
        }
    }

    private void generateEnemyClumps(Random random) {
        int clumpCount = 6 + random.nextInt(5);

        for (int clump = 0; clump < clumpCount; clump++) {
            float centerX = chunkX * chunkSize * tileSize +
                    (random.nextInt(chunkSize - 4) + 2) * tileSize;
            float centerY = chunkY * chunkSize * tileSize +
                    (random.nextInt(chunkSize - 4) + 2) * tileSize;

            int enemiesInClump = 8 + random.nextInt(5);

            EnemyType clumpEnemyType;
            int typeRoll = random.nextInt(100);
            if (typeRoll < 33) {
                clumpEnemyType = EnemyType.MUSHIE;
            } else if (typeRoll < 66) {
                clumpEnemyType = EnemyType.WOLFIE;
            } else {
                clumpEnemyType = EnemyType.HEDGEHOG;
            }

            for (int i = 0; i < enemiesInClump; i++) {
                float offsetX = (random.nextFloat() * 3f - 1.5f) * tileSize;
                float offsetY = (random.nextFloat() * 3f - 1.5f) * tileSize;

                float x = centerX + offsetX;
                float y = centerY + offsetY;

                do {
                    x = Math.max(chunkX * chunkSize * tileSize + 16,
                            Math.min((chunkX + 1) * chunkSize * tileSize - 16, x));
                    y = Math.max(chunkY * chunkSize * tileSize + 16,
                            Math.min((chunkY + 1) * chunkSize * tileSize - 16, y));

                    Rectangle enemyBounds = new Rectangle(x, y, 16, 16);

                    if (!isOverlapping(enemyBounds)) {
                        Texture enemyTexture = null;
                        int enemyLevel = 1;

                        if (Storage.isStageClear()) {
                            enemyLevel = 2 + random.nextInt(2);
                            enemyTexture = Storage.assetManager.get("enemy_dark.png", Texture.class);
                        } else {
                            enemyLevel = 1 + random.nextInt(2);
                            enemyTexture = Storage.assetManager.get("enemy.png", Texture.class);
                        }

                        // Add enemy info with type
                        pendingEnemies.add(new EnemyInfo(enemyTexture, x, y, enemyLevel, clumpEnemyType));
                    }
                } while (x + 10 == player.getPosition().x || x - 10 == player.getPosition().x ||
                        y + 10 == player.getPosition().y || y - 10 == player.getPosition().y);
            }

            clump += random.nextInt(2);
        }
    }

    private boolean isOutOfBounds(Rectangle bounds) {
        return bounds.x < chunkX * chunkSize * tileSize ||
                bounds.y < chunkY * chunkSize * tileSize ||
                bounds.x + bounds.width > (chunkX + 1) * chunkSize * tileSize ||
                bounds.y + bounds.height > (chunkY + 1) * chunkSize * tileSize;
    }

    private ObstacleInfo getRandomObstacle(Random random, float x, float y) {
        switch (random.nextInt(7)) {
            case 1:
                return new ObstacleInfo(
                        Storage.assetManager.get("tiles/newtree.png", Texture.class),
                        x, y, 26, 32
                );
            case 2:
                return new ObstacleInfo(
                        Storage.assetManager.get("tiles/newtree2.png", Texture.class),
                        x, y, 32, 36
                );
            case 3:
                return new ObstacleInfo(
                        Storage.assetManager.get("tiles/newtree3.png", Texture.class),
                        x, y, 32, 36
                );
            case 4:
                return new ObstacleInfo(
                        Storage.assetManager.get("tiles/newrock.png", Texture.class),
                        x, y, 16, 16
                );
            case 5:
                return new ObstacleInfo(
                        Storage.assetManager.get("tiles/newrock2.png", Texture.class),
                        x, y, 18, 10
                );
            case 6:
                return new ObstacleInfo(
                        Storage.assetManager.get("tiles/newrock3.png", Texture.class),
                        x, y, 20, 22
                );
            default:
                return new ObstacleInfo(
                        Storage.assetManager.get("tiles/newrock4.png", Texture.class),
                        x, y, 10, 10
                );
        }
    }

    private boolean isOverlapping(Rectangle newBounds) {
        for (Obstacle existing : obstacles) {
            if (existing.bounds.overlaps(newBounds)) return true;
        }
        for (Enemy existing : enemies) {
            if (existing.bounds.overlaps(newBounds)) return true;
        }
        for (Lemmy existing : lemmys) {
            if (existing.getBounds().overlaps(newBounds)) return true;
        }
        for (ObstacleInfo pending : pendingObstacles) {
            if (new Rectangle(pending.x, pending.y, pending.width, pending.height).overlaps(newBounds)) return true;
        }
        return false;
    }

    public void addBodiesToWorld(World world) {
        if (bodiesAdded) return;

        for (ObstacleInfo obstacleInfo : pendingObstacles) {
            Body body = createObstacleBody(world, obstacleInfo.x, obstacleInfo.y, obstacleInfo.width, obstacleInfo.height);
            obstacles.add(new Obstacle(new Rectangle(obstacleInfo.x, obstacleInfo.y, obstacleInfo.width, obstacleInfo.height), obstacleInfo.texture, body));
        }
        pendingObstacles.clear();

        if (!Storage.isStageClear()) {
            for (EnemyInfo enemyInfo : pendingEnemies) {
                Body body = createEnemyBody(world, enemyInfo.x, enemyInfo.y, 16, 16);

                EnemyStats stats;
                switch (enemyInfo.enemyType) {
                    case WOLFIE:
                        stats = EnemyStats.Factory.createWolfieEnemy(enemyInfo.level);
                        enemies.add(new Enemy(
                                new Rectangle(enemyInfo.x, enemyInfo.y, 20, 16),
                                enemyInfo.texture,
                                body,
                                player,
                                getAnimationManager(),
                                stats,
                                enemyInfo.enemyType
                        ));
                        break;
                    case HEDGEHOG:
                        stats = EnemyStats.Factory.createHedgehogEnemy(enemyInfo.level);
                        enemies.add(new Enemy(
                                new Rectangle(enemyInfo.x, enemyInfo.y, 16, 16),
                                enemyInfo.texture,
                                body,
                                player,
                                getAnimationManager(),
                                stats,
                                enemyInfo.enemyType
                        ));
                        break;
                    case MUSHIE:
                    default:
                        stats = EnemyStats.Factory.createMushieEnemy(enemyInfo.level);
                        enemies.add(new Enemy(
                                new Rectangle(enemyInfo.x, enemyInfo.y, 16, 16),
                                enemyInfo.texture,
                                body,
                                player,
                                getAnimationManager(),
                                stats,
                                enemyInfo.enemyType
                        ));
                        break;
                }

                body.setUserData(enemies.get(enemies.size() - 1));
            }
            pendingEnemies.clear();
        } else if (!Storage.isBossAlive()) {
            for (EnemyInfo enemyInfo : pendingEnemies) {
                Body body = createEnemyBody(world, enemyInfo.x, enemyInfo.y, 16, 16);
                bossKitty.add(new BossKitty(new Rectangle(enemyInfo.x, enemyInfo.y, 16, 16),
                        body, player, getAnimationManager(), 3));
            }
            pendingEnemies.clear();
        }

        bodiesAdded = true;
    }

    public void disableObstacles() {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.body != null) {
                obstacle.body.setActive(false);
            }
        }
    }

    public void enableObstacles() {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.body != null) {
                obstacle.body.setActive(true);
            }
        }
    }

    public void disableEnemies() {
        for (Enemy enemy : enemies) {
            if (enemy.getBody() != null) {
                enemy.getBody().setActive(false);
            }
        }
        for (BossKitty boss : bossKitty) {
            if (boss.getBody() != null) {
                boss.getBody().setActive(false);
            }
        }
        for (Cyclops cyclops : cyclopsList) {
            if (cyclops.getBody() != null) {
                cyclops.getBody().setActive(false);
            }
        }
        for (Lemmy lemmy : lemmys) {
            if (lemmy.getBody() != null) {
                lemmy.getBody().setActive(false);
            }
        }
    }

    public void enableEnemies() {
        for (Enemy enemy : enemies) {
            if (enemy.getBody() != null) {
                enemy.getBody().setActive(true);
            }
        }
        for (BossKitty boss : bossKitty) {
            if (boss.getBody() != null) {
                boss.getBody().setActive(true);
            }
        }
        for (Cyclops cyclops : cyclopsList) {
            if (cyclops.getBody() != null) {
                cyclops.getBody().setActive(true);
            }
        }
        for (Lemmy lemmy : lemmys) {
            if (lemmy.getBody() != null) {
                lemmy.getBody().setActive(true);
            }
        }
    }

    public void removeEnemies() {
        enemies.clear();
        lemmys.clear();
    }

    private Body createObstacleBody(World world, float x, float y, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + width / 2f, y + height / 2f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 4f, height / 4f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.3f;

        fixtureDef.filter.categoryBits = CollisionFilter.OBSTACLE;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);

        shape.dispose();
        return body;
    }

    private Body createEnemyBody(World world, float x, float y, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x + width / 2f, y + height / 2f);

        Body body = world.createBody(bodyDef);

        PolygonShape mainShape = new PolygonShape();
        mainShape.setAsBox(width / 3.5f, height / 4f);

        FixtureDef mainFixtureDef = new FixtureDef();
        mainFixtureDef.shape = mainShape;
        mainFixtureDef.density = 1f;
        mainFixtureDef.friction = 0.3f;
        mainFixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        mainFixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ABILITY;
        body.createFixture(mainFixtureDef);
        mainShape.dispose();

        PolygonShape enemyCollisionShape = new PolygonShape();
        enemyCollisionShape.setAsBox(width / 5f, height / 5f);

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

    public boolean isOverlappingAnyObstacle(Rectangle bounds) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.bounds.overlaps(bounds)) {
                return true;
            }
        }

        for (ObstacleInfo pending : pendingObstacles) {
            Rectangle pendingBounds = new Rectangle(pending.x, pending.y, pending.width, pending.height);
            if (pendingBounds.overlaps(bounds)) {
                return true;
            }
        }

        return false;
    }

    public void renderGround(SpriteBatch batch, Texture groundTexture) {
        for (int x = 0; x < chunkSize; x++) {
            for (int y = 0; y < chunkSize; y++) {
                float drawX = x * tileSize + chunkX * chunkSize * tileSize;
                float drawY = y * tileSize + chunkY * chunkSize * tileSize;
                batch.draw(groundTexture, drawX, drawY, tileSize, tileSize);
            }
        }

        for (Decoration decoration : decorations) {
            decoration.render(batch);
        }
    }

    public void renderObstacles(SpriteBatch batch, float playerY, boolean renderBehind) {
        for (Obstacle obstacle : obstacles) {
            float obstacleBaseY = obstacle.bounds.y + (obstacle.bounds.height * 0.15f);

            if ((renderBehind && obstacleBaseY < playerY) ||
                    (!renderBehind && obstacleBaseY > playerY)) {
                batch.draw(obstacle.texture, obstacle.bounds.x, obstacle.bounds.y, obstacle.bounds.width, obstacle.bounds.height);
            }
        }
    }

    public void renderEnemies(SpriteBatch batch, boolean isPaused) {
        float deltaTime = Gdx.graphics.getDeltaTime();

        for (Enemy enemy : enemies) {
            if (!isPaused) {
                enemy.update(deltaTime);
            }
            enemy.render(batch);
        }
    }

    public void dispose() {
        for (Obstacle obstacle : obstacles) {
            world.destroyBody(obstacle.body);
        }

        for (Enemy enemy : enemies) {
            enemy.dispose();
        }

        for (BossKitty boss : bossKitty) {
            boss.dispose();
        }

        for (Cyclops cyclops : cyclopsList) {
            cyclops.dispose();
        }

        for (Lemmy lemmy : new ArrayList<>(lemmys)) {
            if (lemmy.getBody() != null) {
                try {
                    world.destroyBody(lemmy.getBody());
                } catch (Exception e) {
                }
                lemmy.clearBody();
            }
            lemmy.dispose();
        }
        lemmys.clear();

        decorations.clear();
    }

    public void updateEnemies() {
        enemies.removeIf(enemy -> {
            enemy.update(Gdx.graphics.getDeltaTime());
            return enemy.isMarkedForRemoval();
        });
    }

    private static class Obstacle {
        final Rectangle bounds;
        final Texture texture;
        final Body body;

        public Obstacle(Rectangle bounds, Texture texture, Body body) {
            this.bounds = bounds;
            this.texture = texture;
            this.body = body;
        }
    }

    private static class ObstacleInfo {
        final Texture texture;
        final float x, y, width, height;

        public ObstacleInfo(Texture texture, float x, float y, float width, float height) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class EnemyInfo {
        final Texture texture;
        final float x, y;
        final int level;
        final EnemyType enemyType;

        public EnemyInfo(Texture texture, float x, float y, int level) {
            this(texture, x, y, level, EnemyType.MUSHIE);
        }

        public EnemyInfo(Texture texture, float x, float y, int level, EnemyType enemyType) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.level = level;
            this.enemyType = enemyType;
        }
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<BossKitty> getBossKitty() {
        return bossKitty;
    }

    public List<Cyclops> getCyclopsList() {
        return cyclopsList;
    }

    public List<Lemmy> getLemmys() {
        return lemmys;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }
}