package managers;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import config.Storage;

public class MapBoundary {
    private final List<BoundaryWall> walls;
    private final List<Body> boundaryBodies;
    private final Texture wallTexture;
    private final int mapSizeChunks;
    private final int chunkSize;
    private final int tileSize;

    public MapBoundary(World world, int mapSizeChunks, int chunkSize, int tileSize) {
        this.mapSizeChunks = mapSizeChunks;
        this.chunkSize = chunkSize;
        this.tileSize = tileSize;
        this.walls = new ArrayList<>();
        this.boundaryBodies = new ArrayList<>();
        this.wallTexture = Storage.assetManager.get("tiles/newrock.png", Texture.class);

        createBoundaryWalls(world);
    }

    private void createBoundaryWalls(World world) {
        int halfMapChunks = mapSizeChunks / 2;
        int worldSize = mapSizeChunks * chunkSize * tileSize;
        int minBound = -halfMapChunks * chunkSize * tileSize;
        int maxBound = (halfMapChunks + 1) * chunkSize * tileSize;

        int wallThickness = tileSize * 2;

        // North wall
        createWallSegment(world, minBound, maxBound - wallThickness, worldSize, wallThickness);

        // South wall
        createWallSegment(world, minBound, minBound - wallThickness, worldSize, wallThickness);

        // East wall
        createWallSegment(world, maxBound - wallThickness, minBound, wallThickness, worldSize);

        // West wall
        createWallSegment(world, minBound - wallThickness, minBound, wallThickness, worldSize);
    }

    private void createWallSegment(World world, float x, float y, float width, float height) {
        // Create visual tiles for the wall
        int tilesX = (int) Math.ceil(width / tileSize);
        int tilesY = (int) Math.ceil(height / tileSize);

        for (int i = 0; i < tilesX; i++) {
            for (int j = 0; j < tilesY; j++) {
                float tileX = x + i * tileSize;
                float tileY = y + j * tileSize;
                walls.add(new BoundaryWall(new Rectangle(tileX, tileY, tileSize, tileSize), wallTexture));
            }
        }

        // Create one large physics body for the entire wall segment
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x + width / 2f, y + height / 2f);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.filter.categoryBits = CollisionFilter.WALL;
        fixtureDef.filter.maskBits = CollisionFilter.PLAYER | CollisionFilter.SPEAR | CollisionFilter.ENEMY;

        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);
        shape.dispose();

        boundaryBodies.add(body);
    }

    public void disable() {
        for (Body body : boundaryBodies) {
            if (body != null) {
                body.setActive(false);
            }
        }
    }

    public void enable() {
        for (Body body : boundaryBodies) {
            if (body != null) {
                body.setActive(true);
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (BoundaryWall wall : walls) {
            batch.draw(wall.texture, wall.bounds.x, wall.bounds.y, wall.bounds.width, wall.bounds.height);
        }
    }

    private static class BoundaryWall {
        final Rectangle bounds;
        final Texture texture;

        public BoundaryWall(Rectangle bounds, Texture texture) {
            this.bounds = bounds;
            this.texture = texture;
        }
    }
}