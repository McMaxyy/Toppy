package managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;

import config.Storage;

public class Chunk {
    private final int chunkX, chunkY;
    private final int chunkSize, tileSize;

    private final List<Obstacle> obstacles;
    private final List<ObstacleInfo> pendingObstacles;
    private final Random random;
    private final World world;

    public Chunk(int chunkX, int chunkY, int chunkSize, int tileSize, Random random, World world) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkSize = chunkSize;
        this.tileSize = tileSize;
        this.random = random;
        this.world = world;
        this.obstacles = new ArrayList<>();
        this.pendingObstacles = new ArrayList<>();

        generateObstacles(random);
    }

    private void generateObstacles(Random random) {
        int obstacleCount = random.nextInt(10) + 50;
        for (int i = 0; i < obstacleCount; i++) {
            float x = random.nextInt(chunkSize * tileSize) + chunkX * chunkSize * tileSize;
            float y = random.nextInt(chunkSize * tileSize) + chunkY * chunkSize * tileSize;

            Rectangle newObstacleBounds = new Rectangle(x, y, tileSize, tileSize);
            if (isOverlapping(newObstacleBounds) || isOutOfBounds(newObstacleBounds)) {
                i--; // Retry obstacle generation
                continue;
            }

            ObstacleInfo obstacleInfo = getRandomObstacle(random, x, y);
            newObstacleBounds.setSize(obstacleInfo.width, obstacleInfo.height);

            pendingObstacles.add(new ObstacleInfo(obstacleInfo.texture, x, y, obstacleInfo.width, obstacleInfo.height));
        }
    }
    
    private boolean isOutOfBounds(Rectangle obstacleBounds) {
        return obstacleBounds.x < chunkX * chunkSize * tileSize || 
               obstacleBounds.y < chunkY * chunkSize * tileSize ||
               obstacleBounds.x + obstacleBounds.width > (chunkX + 1) * chunkSize * tileSize ||
               obstacleBounds.y + obstacleBounds.height > (chunkY + 1) * chunkSize * tileSize;
    }

    private ObstacleInfo getRandomObstacle(Random random, float x, float y) {
        switch (random.nextInt(7)) {
            case 1:
                return new ObstacleInfo(
                    Storage.assetManager.get("tiles/tree.png", Texture.class),
                    x, y, 26, 32
                );
            case 2:
                return new ObstacleInfo(
                    Storage.assetManager.get("tiles/tree2.png", Texture.class),
                    x, y, 32, 36
                );
            case 3:
                return new ObstacleInfo(
                    Storage.assetManager.get("tiles/tree3.png", Texture.class),
                    x, y, 32, 36
                );
            case 4:
                return new ObstacleInfo(
                    Storage.assetManager.get("tiles/rock.png", Texture.class),
                    x, y, 16, 16
                );
            case 5:
                return new ObstacleInfo(
                    Storage.assetManager.get("tiles/rock2.png", Texture.class),
                    x, y, 18, 10
                );
            case 6:
                return new ObstacleInfo(
                    Storage.assetManager.get("tiles/rock3.png", Texture.class),
                    x, y, 20, 22
                );
            default:
                return new ObstacleInfo(
                    Storage.assetManager.get("tiles/rock4.png", Texture.class),
                    x, y, 10, 10
                );
        }
    }

    private boolean isOverlapping(Rectangle newObstacle) {
        for (Obstacle existing : obstacles) {
            if (existing.bounds.overlaps(newObstacle)) {
                return true;
            }
        }

        for (ObstacleInfo pending : pendingObstacles) {
            Rectangle pendingBounds = new Rectangle(pending.x, pending.y, pending.width, pending.height);
            if (pendingBounds.overlaps(newObstacle)) {
                return true;
            }
        }

        return false;
    }

    public void addBodiesToWorld(World world) {
        for (ObstacleInfo obstacleInfo : pendingObstacles) {
            Body obstacleBody = createObstacleBody(world, obstacleInfo.x, obstacleInfo.y, obstacleInfo.width, obstacleInfo.height);
            Rectangle bounds = new Rectangle(obstacleInfo.x, obstacleInfo.y, obstacleInfo.width, obstacleInfo.height);
            obstacles.add(new Obstacle(bounds, obstacleInfo.texture, obstacleBody));
        }
        pendingObstacles.clear();
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
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);

        shape.dispose();
        return body;
    }

    public void render(SpriteBatch batch, Texture groundTexture) {
        for (int x = 0; x < chunkSize; x++) {
            for (int y = 0; y < chunkSize; y++) {
                float drawX = x * tileSize + chunkX * chunkSize * tileSize;
                float drawY = y * tileSize + chunkY * chunkSize * tileSize;
                batch.draw(groundTexture, drawX, drawY, tileSize, tileSize);
            }
        }

        for (Obstacle obstacle : obstacles) {
            batch.draw(obstacle.texture, obstacle.bounds.x, obstacle.bounds.y, obstacle.bounds.width, obstacle.bounds.height);
        }
    }

    public void dispose() {
        for (Obstacle obstacle : obstacles) {
            world.destroyBody(obstacle.body);
        }
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
}
