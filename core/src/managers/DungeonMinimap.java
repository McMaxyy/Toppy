package managers;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import config.Storage;
import entities.Player;

public class DungeonMinimap {
    private final int dungeonWidth;
    private final int dungeonHeight;
    private final int tileSize;
    private final Player player;
    private final Dungeon dungeon;

    // Fog of war - tracks which tiles have been explored
    private final Set<String> exploredTiles;
    private final int EXPLORATION_RADIUS = 4; // Tiles around player that get revealed

    // Map display settings
    private boolean mapOpen = false;
    private final int MAP_PADDING = 50;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Texture pixelTexture;

    // Colors
    private final Color FOG_COLOR = new Color(0.05f, 0.05f, 0.05f, 0.95f);
    private final Color WALL_COLOR = new Color(0.3f, 0.3f, 0.35f, 1f);
    private final Color FLOOR_COLOR = new Color(0.4f, 0.3f, 0.2f, 1f);
    private final Color PLAYER_COLOR = new Color(0.2f, 0.8f, 1f, 1f);
    private final Color EXIT_COLOR = new Color(0.2f, 1f, 0.2f, 1f);
    private final Color BACKGROUND_COLOR = new Color(0.02f, 0.02f, 0.02f, 0.98f);

    public DungeonMinimap(int dungeonWidth, int dungeonHeight, int tileSize, Player player, Dungeon dungeon) {
        this.dungeonWidth = dungeonWidth;
        this.dungeonHeight = dungeonHeight;
        this.tileSize = tileSize;
        this.player = player;
        this.dungeon = dungeon;
        this.exploredTiles = new HashSet<>();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.pixelTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
    }

    public void toggleMap() {
        mapOpen = !mapOpen;
    }

    public boolean isMapOpen() {
        return mapOpen;
    }

    public void update() {
        updateExploration();
    }

    private void updateExploration() {
        Vector2 playerPos = player.getPosition();
        int playerTileX = (int) (playerPos.x / tileSize);
        int playerTileY = (int) (playerPos.y / tileSize);

        // Reveal tiles around player
        for (int dx = -EXPLORATION_RADIUS; dx <= EXPLORATION_RADIUS; dx++) {
            for (int dy = -EXPLORATION_RADIUS; dy <= EXPLORATION_RADIUS; dy++) {
                // Circular exploration radius
                if (dx * dx + dy * dy <= EXPLORATION_RADIUS * EXPLORATION_RADIUS) {
                    int tileX = playerTileX + dx;
                    int tileY = playerTileY + dy;
                    exploredTiles.add(tileX + "," + tileY);
                }
            }
        }
    }

    public void render(SpriteBatch batch, boolean batchIsActive) {
        if (!mapOpen) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Calculate map dimensions to fit screen with padding
        int availableWidth = screenWidth - MAP_PADDING * 2;
        int availableHeight = screenHeight - MAP_PADDING * 2;

        // Calculate tile size on minimap (scale to fit screen)
        float tileDisplaySize = Math.min(
                (float) availableWidth / dungeonWidth,
                (float) availableHeight / dungeonHeight
        );

        // Calculate map dimensions
        float mapWidth = dungeonWidth * tileDisplaySize;
        float mapHeight = dungeonHeight * tileDisplaySize;

        // Center the map on screen
        float mapStartX = (screenWidth - mapWidth) / 2f;
        float mapStartY = (screenHeight - mapHeight) / 2f;

        // End batch if it's active
        if (batchIsActive) {
            batch.end();
        }

        // Draw map background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(mapStartX - 5, mapStartY - 5, mapWidth + 10, mapHeight + 10);
        shapeRenderer.end();

        // Draw map border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3);
        shapeRenderer.setColor(WALL_COLOR);
        shapeRenderer.rect(mapStartX, mapStartY, mapWidth, mapHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        // Draw tiles
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int tileX = 0; tileX < dungeonWidth; tileX++) {
            for (int tileY = 0; tileY < dungeonHeight; tileY++) {
                String tileKey = tileX + "," + tileY;

                float displayX = mapStartX + tileX * tileDisplaySize;
                float displayY = mapStartY + tileY * tileDisplaySize;

                if (exploredTiles.contains(tileKey)) {
                    // Check what type of tile this is
                    int tileType = dungeon.getTileType(tileX, tileY);

                    if (tileType == 0) { // WALL
                        shapeRenderer.setColor(WALL_COLOR);
                    } else if (tileType == 1) { // FLOOR
                        shapeRenderer.setColor(FLOOR_COLOR);
                    } else if (tileType == 2) { // EXIT
                        // Draw floor color first
                        shapeRenderer.setColor(FLOOR_COLOR);
                        shapeRenderer.rect(displayX, displayY, tileDisplaySize, tileDisplaySize);
                        // Then draw exit marker
                        float pulse = 1f + (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.2f;
                        shapeRenderer.setColor(EXIT_COLOR.r, EXIT_COLOR.g, EXIT_COLOR.b, 0.8f);
                        float exitSize = tileDisplaySize * pulse;
                        float exitOffset = (exitSize - tileDisplaySize) / 2f;
                        shapeRenderer.rect(displayX - exitOffset, displayY - exitOffset, exitSize, exitSize);
                        continue; // Skip the normal rect draw below
                    }

                    shapeRenderer.rect(displayX, displayY, tileDisplaySize, tileDisplaySize);
                } else {
                    // Fog of war
                    shapeRenderer.setColor(FOG_COLOR);
                    shapeRenderer.rect(displayX, displayY, tileDisplaySize, tileDisplaySize);
                }
            }
        }

        // Draw player
        Vector2 playerPos = player.getPosition();
        int playerTileX = (int) (playerPos.x / tileSize);
        int playerTileY = (int) (playerPos.y / tileSize);

        float playerDisplayX = mapStartX + playerTileX * tileDisplaySize;
        float playerDisplayY = mapStartY + playerTileY * tileDisplaySize;

        shapeRenderer.setColor(PLAYER_COLOR);
        float playerMarkerSize = tileDisplaySize * 2.5f;
        float playerOffset = (playerMarkerSize - tileDisplaySize) / 2f;
        shapeRenderer.rect(
                playerDisplayX - playerOffset,
                playerDisplayY - playerOffset,
                playerMarkerSize,
                playerMarkerSize
        );

        shapeRenderer.end();

        // Draw UI text
        batch.begin();

        // Legend
        font.setColor(Color.WHITE);
        float legendX = mapStartX + mapWidth - 150;
        float legendY = mapStartY + mapHeight + 30;


        // Player indicator
        batch.setColor(PLAYER_COLOR);
        batch.draw(pixelTexture, legendX, legendY - 30, 15, 15);
        batch.setColor(Color.WHITE);

        // Exit indicator
        batch.setColor(EXIT_COLOR);
        batch.draw(pixelTexture, legendX, legendY - 55, 15, 15);

        // Fog indicator
        batch.setColor(FOG_COLOR);
        batch.draw(pixelTexture, legendX, legendY - 80, 15, 15);

        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}