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
import entities.BossKitty;
import entities.Enemy;
import entities.EnemyStats;
import entities.Player;

public class Chunk {
    private final int chunkX, chunkY;
    private final int chunkSize, tileSize;

    private final List<Obstacle> obstacles;
    private final List<ObstacleInfo> pendingObstacles;
    private final List<Enemy> enemies;
    private final List<EnemyInfo> pendingEnemies;
    private final List<BossKitty> bossKitty;

    private final World world;
    private final Player player;
    private final AnimationManager animationManager;
    private boolean bodiesAdded = false;

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
        this.player = player;

        generateObstacles(random);
        generateEnemyClumps(random);
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
        // Generate 3-5 clumps per chunk
        int clumpCount = 8 + random.nextInt(5); // 3 to 5 clumps

        for (int clump = 0; clump < clumpCount; clump++) {
            // Choose a random clump center within the chunk (with margin)
            float centerX = chunkX * chunkSize * tileSize +
                    (random.nextInt(chunkSize - 4) + 2) * tileSize;
            float centerY = chunkY * chunkSize * tileSize +
                    (random.nextInt(chunkSize - 4) + 2) * tileSize;

            // Determine number of enemies in this clump (4-10)
            int enemiesInClump = 8 + random.nextInt(5); // 4 to 10 enemies

            // Generate enemies around the center
            for (int i = 0; i < enemiesInClump; i++) {
                // Calculate position with some random offset from center
                float offsetX = (random.nextFloat() * 3f - 1.5f) * tileSize; // -1.5 to +1.5 tiles
                float offsetY = (random.nextFloat() * 3f - 1.5f) * tileSize; // -1.5 to +1.5 tiles

                float x = centerX + offsetX;
                float y = centerY + offsetY;

                // Ensure the enemy is within chunk bounds
                x = Math.max(chunkX * chunkSize * tileSize + 16,
                        Math.min((chunkX + 1) * chunkSize * tileSize - 16, x));
                y = Math.max(chunkY * chunkSize * tileSize + 16,
                        Math.min((chunkY + 1) * chunkSize * tileSize - 16, y));

                Rectangle enemyBounds = new Rectangle(x, y, 16, 16);

                // Check if position is valid
                if (!isOverlapping(enemyBounds)) {
                    // Randomly choose enemy type based on game stage
                    Texture enemyTexture = null;
                    int enemyLevel = 1;

                    if (Storage.isStageClear()) {
                        // Post-boss stage - more difficult enemies
                        enemyLevel = 2 + random.nextInt(2); // Level 2-3
                        enemyTexture = Storage.assetManager.get("enemy_dark.png", Texture.class);
                    } else {
                        // Normal stage
                        enemyLevel = 1 + random.nextInt(2); // Level 1-2
                        enemyTexture = Storage.assetManager.get("enemy.png", Texture.class);
                    }

                    // Add enemy to pending list
                    pendingEnemies.add(new EnemyInfo(enemyTexture, x, y, enemyLevel));
                }
            }

            // Optional: Add a small gap between clumps by skipping some iterations
            clump += random.nextInt(2); // Sometimes skip to create more spread
        }
    }

    public void spawnBossKitty(float playerRadius) {
        Vector2 playerPosition = player.getPosition();
        Vector2 spawnPosition = getRandomPositionWithinRadius(playerPosition, playerRadius);

        Rectangle bossBounds = new Rectangle(spawnPosition.x, spawnPosition.y, 32, 32);

        if (!isOverlapping(bossBounds) && !isOutOfBounds(bossBounds)) {
            Body bossBody = createEnemyBody(world, spawnPosition.x, spawnPosition.y, 32, 32);
            bossKitty.add(new BossKitty(bossBounds, bossBody, player, getAnimationManager(), 3));
        } else {
            Vector2 fallbackPosition = getFallbackPosition(playerPosition);
            Body bossBody = createEnemyBody(world, fallbackPosition.x, fallbackPosition.y, 32, 32);
            bossKitty.add(new BossKitty(new Rectangle(fallbackPosition.x, fallbackPosition.y, 32, 32),
                    bossBody, player, getAnimationManager(), 3));
        }
    }

    private Vector2 getFallbackPosition(Vector2 playerPosition) {
        float fallbackOffset = 50f;
        return new Vector2(playerPosition.x + fallbackOffset, playerPosition.y + fallbackOffset);
    }

    private Vector2 getRandomPositionWithinRadius(Vector2 center, float radius) {
        Random random = new Random();
        float angle = random.nextFloat() * 360f;
        float distance = random.nextFloat() * radius;

        float offsetX = (float) Math.cos(Math.toRadians(angle)) * distance;
        float offsetY = (float) Math.sin(Math.toRadians(angle)) * distance;

        return new Vector2(center.x + offsetX, center.y + offsetY);
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
        for (ObstacleInfo pending : pendingObstacles) {
            if (new Rectangle(pending.x, pending.y, pending.width, pending.height).overlaps(newBounds)) return true;
        }
        return false;
    }

    public void addBodiesToWorld(World world) {
        if (bodiesAdded) return; // Skip if bodies already added

        // Add obstacle bodies
        for (ObstacleInfo obstacleInfo : pendingObstacles) {
            Body body = createObstacleBody(world, obstacleInfo.x, obstacleInfo.y, obstacleInfo.width, obstacleInfo.height);
            obstacles.add(new Obstacle(new Rectangle(obstacleInfo.x, obstacleInfo.y, obstacleInfo.width, obstacleInfo.height), obstacleInfo.texture, body));
        }
        pendingObstacles.clear();

        // Add enemy bodies if not in boss stage
        if(!Storage.isStageClear()) {
            for (EnemyInfo enemyInfo : pendingEnemies) {
                Body body = createEnemyBody(world, enemyInfo.x, enemyInfo.y, 16, 16);

                // Create enemy stats based on level
                EnemyStats stats = EnemyStats.Factory.createBasicEnemy(enemyInfo.level);

                // Randomly choose enemy type for variety
//                Random random = new Random();
//                if (random.nextInt(100) < 20) { // 20% chance for fast enemy
//                    stats = EnemyStats.Factory.createFastEnemy(enemyInfo.level);
//                } else if (random.nextInt(100) < 10) { // 10% chance for ranged enemy
//                    stats = EnemyStats.Factory.createRangedEnemy(enemyInfo.level);
//                }

                stats = EnemyStats.Factory.createFastEnemy(enemyInfo.level);

                enemies.add(new Enemy(new Rectangle(enemyInfo.x, enemyInfo.y, 16, 16),
                        enemyInfo.texture, body, player, getAnimationManager(), stats));
            }
            pendingEnemies.clear();
        }
        else if (!Storage.isBossAlive()){
            // Boss stage
            for (EnemyInfo enemyInfo : pendingEnemies) {
                Body body = createEnemyBody(world, enemyInfo.x, enemyInfo.y, 16, 16);
                bossKitty.add(new BossKitty(new Rectangle(enemyInfo.x, enemyInfo.y, 16, 16),
                        body, player, getAnimationManager(), 3));
            }
            pendingEnemies.clear();
        }

        bodiesAdded = true; // Mark that bodies have been added
    }

    // Add these methods to your Chunk class:
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
    }

    public void enableEnemies() {
        for (Enemy enemy : enemies) {
            if (enemy.getBody() != null) {
                enemy.getBody().setActive(true);
            }
        }
    }

    public void removeEnemies() {
        enemies.clear();
    }

    private Body createObstacleBody(World world, float x, float y, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + width / 2f, y + height / 2f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 3f, height / 3f);

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

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 3f, height / 3f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.3f;

        fixtureDef.filter.categoryBits = CollisionFilter.ENEMY;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ENEMY;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        body.setFixedRotation(true);

        shape.dispose();
        return body;
    }

    public void renderGround(SpriteBatch batch, Texture groundTexture) {
        for (int x = 0; x < chunkSize; x++) {
            for (int y = 0; y < chunkSize; y++) {
                float drawX = x * tileSize + chunkX * chunkSize * tileSize;
                float drawY = y * tileSize + chunkY * chunkSize * tileSize;
                batch.draw(groundTexture, drawX, drawY, tileSize, tileSize);
            }
        }
    }

    public void renderObstacles(SpriteBatch batch, float playerY, boolean renderBehind) {
        for (Obstacle obstacle : obstacles) {
            if ((renderBehind && obstacle.bounds.y < playerY) ||
                    (!renderBehind && obstacle.bounds.y >= playerY)) {
                batch.draw(obstacle.texture, obstacle.bounds.x, obstacle.bounds.y, obstacle.bounds.width, obstacle.bounds.height);
            }
        }
    }

    public void renderEnemies(SpriteBatch batch) {
        for (Enemy enemy : enemies) {
            enemy.update(Gdx.graphics.getDeltaTime());
            enemy.render(batch);
        }
    }

    public void renderBossKitty(SpriteBatch batch) {
        for (BossKitty enemy : bossKitty) {
            enemy.update(Gdx.graphics.getDeltaTime());
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
    }

    public void updateEnemies() {
        enemies.removeIf(enemy -> {
            enemy.update(Gdx.graphics.getDeltaTime());
            return enemy.isMarkedForRemoval();
        });
    }

    public void updateBossKitty() {
        bossKitty.removeIf(bossKitty -> {
            bossKitty.update(Gdx.graphics.getDeltaTime());
            return bossKitty.isMarkedForRemoval();
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

        public EnemyInfo(Texture texture, float x, float y, int level) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.level = level;
        }
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<BossKitty> getBossKitty(){
        return bossKitty;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }
}