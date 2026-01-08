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
import entities.Portal;

public class Minimap {
    private final int mapSizeChunks;
    private final int chunkSize;
    private final int tileSize;
    private final Player player;
    private Portal portal;

    // Fog of war - tracks which tiles have been explored
    private final Set<String> exploredTiles;
    private final int EXPLORATION_RADIUS = 3; // Tiles around player that get revealed

    // Map display settings
    private boolean mapOpen = false;
    private boolean showPortal = true;
    private final int MAP_PADDING = 50;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Texture pixelTexture;

    // Colors
    private final Color FOG_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.9f);
    private final Color EXPLORED_COLOR = new Color(0.3f, 0.4f, 0.3f, 1f);
    private final Color PLAYER_COLOR = new Color(0.2f, 0.8f, 1f, 1f);
    private final Color PORTAL_COLOR = new Color(0.8f, 0.2f, 1f, 1f);
    private final Color WALL_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);

    public Minimap(int mapSizeChunks, int chunkSize, int tileSize, Player player) {
        this.mapSizeChunks = mapSizeChunks;
        this.chunkSize = chunkSize;
        this.tileSize = tileSize;
        this.player = player;
        this.exploredTiles = new HashSet<>();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.pixelTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
    }

    public void setPortal(Portal portal) {
        this.portal = portal;
    }

    public void toggleMap() {
        mapOpen = !mapOpen;
    }

    public void togglePortalDisplay() {
        showPortal = !showPortal;
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

        int totalTilesX = mapSizeChunks * chunkSize;
        int totalTilesY = mapSizeChunks * chunkSize;

        // Calculate tile size on minimap (scale to fit screen)
        float tileDisplaySize = Math.min(
                (float) availableWidth / totalTilesX,
                (float) availableHeight / totalTilesY
        );

        // Calculate map dimensions
        float mapWidth = totalTilesX * tileDisplaySize;
        float mapHeight = totalTilesY * tileDisplaySize;

        // Center the map on screen
        float mapStartX = (screenWidth - mapWidth) / 2f;
        float mapStartY = (screenHeight - mapHeight) / 2f;

        // Calculate world bounds
        int halfMapChunks = mapSizeChunks / 2;
        int worldMinTileX = -halfMapChunks * chunkSize;
        int worldMinTileY = -halfMapChunks * chunkSize;

        // End batch if it's active
        if (batchIsActive) {
            batch.end();
        }

        // Draw map background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.05f, 0.05f, 0.95f);
        shapeRenderer.rect(mapStartX - 5, mapStartY - 5, mapWidth + 10, mapHeight + 10);
        shapeRenderer.end();

        // Draw map border (walls)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3);
        shapeRenderer.setColor(WALL_COLOR);
        shapeRenderer.rect(mapStartX, mapStartY, mapWidth, mapHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        // Draw tiles
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int tileX = 0; tileX < totalTilesX; tileX++) {
            for (int tileY = 0; tileY < totalTilesY; tileY++) {
                int worldTileX = worldMinTileX + tileX;
                int worldTileY = worldMinTileY + tileY;
                String tileKey = worldTileX + "," + worldTileY;

                float displayX = mapStartX + tileX * tileDisplaySize;
                float displayY = mapStartY + tileY * tileDisplaySize;

                if (exploredTiles.contains(tileKey)) {
                    // Explored tile
                    shapeRenderer.setColor(EXPLORED_COLOR);
                    shapeRenderer.rect(displayX, displayY, tileDisplaySize, tileDisplaySize);
                } else {
                    // Fog of war
                    shapeRenderer.setColor(FOG_COLOR);
                    shapeRenderer.rect(displayX, displayY, tileDisplaySize, tileDisplaySize);
                }
            }
        }

        // Draw portal if enabled
        if (showPortal && portal != null) {
            Vector2 portalPos = new Vector2(
                    portal.getBounds().x + portal.getBounds().width / 2f,
                    portal.getBounds().y + portal.getBounds().height / 2f
            );
            int portalTileX = (int) (portalPos.x / tileSize) - worldMinTileX;
            int portalTileY = (int) (portalPos.y / tileSize) - worldMinTileY;

            float portalDisplayX = mapStartX + portalTileX * tileDisplaySize;
            float portalDisplayY = mapStartY + portalTileY * tileDisplaySize;

            // Draw pulsing portal marker
            float pulseSize = tileDisplaySize * (1.5f + (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.3f);
            float pulseOffset = (pulseSize - tileDisplaySize) / 2f;

            shapeRenderer.setColor(PORTAL_COLOR.r, PORTAL_COLOR.g, PORTAL_COLOR.b, 0.6f);
            shapeRenderer.rect(
                    portalDisplayX - pulseOffset,
                    portalDisplayY - pulseOffset,
                    pulseSize,
                    pulseSize
            );
        }

        // Draw player
        Vector2 playerPos = player.getPosition();
        int playerTileX = (int) (playerPos.x / tileSize) - worldMinTileX;
        int playerTileY = (int) (playerPos.y / tileSize) - worldMinTileY;

        float playerDisplayX = mapStartX + playerTileX * tileDisplaySize;
        float playerDisplayY = mapStartY + playerTileY * tileDisplaySize;

        shapeRenderer.setColor(PLAYER_COLOR);
        float playerMarkerSize = tileDisplaySize * 2;
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

        // Portal indicator
        if (showPortal) {
            batch.setColor(PORTAL_COLOR);
            batch.draw(pixelTexture, legendX, legendY - 55, 15, 15);
            batch.setColor(Color.WHITE);
        }

        // Fog indicator
        batch.setColor(FOG_COLOR);
        batch.draw(pixelTexture, legendX, legendY - 80, 15, 15);
        batch.setColor(Color.WHITE);

        batch.setColor(Color.WHITE);
        batch.end(); // End the batch we started for UI text
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}