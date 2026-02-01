package managers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import config.Storage;
import entities.Herman;
import entities.Merchant;
import entities.Player;
import entities.Portal;

public class Minimap {
    private final int mapSizeChunks;
    private final int chunkSize;
    private final int tileSize;
    private final Player player;
    private List<Portal> portals;
    private Merchant merchant;
    private Herman herman;
    private Herman hermanDuplicate;

    private final Set<String> exploredTiles;
    private final int EXPLORATION_RADIUS = 50;

    private boolean mapOpen = false;
    private boolean showPortal = true;
    private boolean showMerchant = true;
    private boolean showHerman = true;
    private final int MAP_PADDING = 50;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final Texture pixelTexture;

    private final Color FOG_COLOR = new Color(0.05f, 0.05f, 0.05f, 0.95f);
    private final Color EXPLORED_COLOR = new Color(0.3f, 0.4f, 0.3f, 1f);
    private final Color PLAYER_COLOR = new Color(0.2f, 0.8f, 1f, 1f);
    private final Color PORTAL_COLOR = new Color(0.8f, 0.2f, 1f, 1f);
    private final Color MERCHANT_COLOR = new Color(1f, 0.84f, 0f, 1f); // Gold color for merchant
    private final Color HERMAN_COLOR = new Color(0.4f, 0.8f, 0.2f, 1f); // Green color for Herman (tree boss)
    private final Color HERMAN_DUPLICATE_COLOR = new Color(0.6f, 0.9f, 0.4f, 1f); // Lighter green for duplicate
    private final Color WALL_COLOR = new Color(0.5f, 0.5f, 0.5f, 1f);
    private final Color BACKGROUND_COLOR = new Color(0.02f, 0.02f, 0.02f, 0.98f);

    public Minimap(int mapSizeChunks, int chunkSize, int tileSize, Player player) {
        this.mapSizeChunks = mapSizeChunks;
        this.chunkSize = chunkSize;
        this.tileSize = tileSize;
        this.player = player;
        this.portals = new ArrayList<>();
        this.exploredTiles = new HashSet<>();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.pixelTexture = Storage.assetManager.get("tiles/green_tile.png", Texture.class);
    }

    public void setPortal(Portal portal) {
        this.portals.add(portal);
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public void setHerman(Herman herman) {
        this.herman = herman;
    }

    public void setHermanDuplicate(Herman hermanDuplicate) {
        this.hermanDuplicate = hermanDuplicate;
    }

    public void toggleMap() {
        mapOpen = !mapOpen;
    }

    public void togglePortalDisplay() {
        showPortal = !showPortal;
    }

    public void toggleMerchantDisplay() {
        showMerchant = !showMerchant;
    }

    public void toggleHermanDisplay() {
        showHerman = !showHerman;
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

        for (int dx = -EXPLORATION_RADIUS; dx <= EXPLORATION_RADIUS; dx++) {
            for (int dy = -EXPLORATION_RADIUS; dy <= EXPLORATION_RADIUS; dy++) {
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

        int availableWidth = screenWidth - MAP_PADDING * 2;
        int availableHeight = screenHeight - MAP_PADDING * 2;

        int totalTilesX = mapSizeChunks * chunkSize;
        int totalTilesY = mapSizeChunks * chunkSize;

        float tileDisplaySize = Math.min(
                (float) availableWidth / totalTilesX,
                (float) availableHeight / totalTilesY
        );

        float mapWidth = totalTilesX * tileDisplaySize;
        float mapHeight = totalTilesY * tileDisplaySize;

        float mapStartX = (screenWidth - mapWidth) / 2f;
        float mapStartY = (screenHeight - mapHeight) / 2f;

        int halfMapChunks = mapSizeChunks / 2;
        int worldMinTileX = -halfMapChunks * chunkSize;
        int worldMinTileY = -halfMapChunks * chunkSize;

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

        for (int tileX = 0; tileX < totalTilesX; tileX++) {
            for (int tileY = 0; tileY < totalTilesY; tileY++) {
                int worldTileX = worldMinTileX + tileX;
                int worldTileY = worldMinTileY + tileY;
                String tileKey = worldTileX + "," + worldTileY;

                float displayX = mapStartX + tileX * tileDisplaySize;
                float displayY = mapStartY + tileY * tileDisplaySize;

                if (exploredTiles.contains(tileKey)) {
                    shapeRenderer.setColor(EXPLORED_COLOR);
                    shapeRenderer.rect(displayX, displayY, tileDisplaySize, tileDisplaySize);
                } else {
                    shapeRenderer.setColor(FOG_COLOR);
                    shapeRenderer.rect(displayX, displayY, tileDisplaySize, tileDisplaySize);
                }
            }
        }

        // Draw portals
        if (showPortal && !portals.isEmpty()) {
            for (Portal portal : portals) {
                Vector2 portalPos = new Vector2(
                        portal.getBounds().x,
                        portal.getBounds().y
                );

                int worldPortalTileX = (int) (portalPos.x / tileSize);
                int worldPortalTileY = (int) (portalPos.y / tileSize);
                String portalTileKey = worldPortalTileX + "," + worldPortalTileY;

                if (exploredTiles.contains(portalTileKey)) {
                    int portalTileX = worldPortalTileX - worldMinTileX;
                    int portalTileY = worldPortalTileY - worldMinTileY;

                    if (portalTileX >= 0 && portalTileX < totalTilesX &&
                            portalTileY >= 0 && portalTileY < totalTilesY) {

                        float portalDisplayX = mapStartX + portalTileX * tileDisplaySize;
                        float portalDisplayY = mapStartY + portalTileY * tileDisplaySize;

                        float pulse = 1f + (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.2f;
                        shapeRenderer.setColor(PORTAL_COLOR.r, PORTAL_COLOR.g, PORTAL_COLOR.b, 0.8f);
                        float pulseSize = tileDisplaySize * pulse;
                        float pulseOffset = (pulseSize - tileDisplaySize) / 2f;
                        shapeRenderer.rect(
                                portalDisplayX - pulseOffset,
                                portalDisplayY - pulseOffset,
                                pulseSize,
                                pulseSize
                        );
                    }
                }
            }
        }

        // Draw merchant
        if (showMerchant && merchant != null && merchant.isActive()) {
            Vector2 merchantPos = merchant.getPosition();

            int worldMerchantTileX = (int) (merchantPos.x / tileSize);
            int worldMerchantTileY = (int) (merchantPos.y / tileSize);
            String merchantTileKey = worldMerchantTileX + "," + worldMerchantTileY;

            if (exploredTiles.contains(merchantTileKey)) {
                int merchantTileX = worldMerchantTileX - worldMinTileX;
                int merchantTileY = worldMerchantTileY - worldMinTileY;

                if (merchantTileX >= 0 && merchantTileX < totalTilesX &&
                        merchantTileY >= 0 && merchantTileY < totalTilesY) {

                    float merchantDisplayX = mapStartX + merchantTileX * tileDisplaySize;
                    float merchantDisplayY = mapStartY + merchantTileY * tileDisplaySize;

                    // Pulsing gold marker for merchant
                    float pulse = 1f + (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.15f;
                    shapeRenderer.setColor(MERCHANT_COLOR.r, MERCHANT_COLOR.g, MERCHANT_COLOR.b, 0.9f);
                    float pulseSize = tileDisplaySize * 1.5f * pulse;
                    float pulseOffset = (pulseSize - tileDisplaySize) / 2f;
                    shapeRenderer.rect(
                            merchantDisplayX - pulseOffset,
                            merchantDisplayY - pulseOffset,
                            pulseSize,
                            pulseSize
                    );
                }
            }
        }

        // Draw Herman (main boss)
        if (showHerman && herman != null && !herman.isMarkedForRemoval() && herman.getBody() != null) {
            Vector2 hermanPos = herman.getBody().getPosition();

            int worldHermanTileX = (int) (hermanPos.x / tileSize);
            int worldHermanTileY = (int) (hermanPos.y / tileSize);
            String hermanTileKey = worldHermanTileX + "," + worldHermanTileY;

            if (exploredTiles.contains(hermanTileKey)) {
                int hermanTileX = worldHermanTileX - worldMinTileX;
                int hermanTileY = worldHermanTileY - worldMinTileY;

                if (hermanTileX >= 0 && hermanTileX < totalTilesX &&
                        hermanTileY >= 0 && hermanTileY < totalTilesY) {

                    float hermanDisplayX = mapStartX + hermanTileX * tileDisplaySize;
                    float hermanDisplayY = mapStartY + hermanTileY * tileDisplaySize;

                    // Pulsing green marker for Herman - larger to indicate boss
                    float pulse = 1f + (float) Math.sin(System.currentTimeMillis() / 250.0) * 0.25f;
                    shapeRenderer.setColor(HERMAN_COLOR.r, HERMAN_COLOR.g, HERMAN_COLOR.b, 0.9f);
                    float pulseSize = tileDisplaySize * 2.0f * pulse;
                    float pulseOffset = (pulseSize - tileDisplaySize) / 2f;
                    shapeRenderer.rect(
                            hermanDisplayX - pulseOffset,
                            hermanDisplayY - pulseOffset,
                            pulseSize,
                            pulseSize
                    );
                }
            }
        }

        // Draw Herman Duplicate
        if (showHerman && hermanDuplicate != null && !hermanDuplicate.isMarkedForRemoval() && hermanDuplicate.getBody() != null) {
            Vector2 duplicatePos = hermanDuplicate.getBody().getPosition();

            int worldDuplicateTileX = (int) (duplicatePos.x / tileSize);
            int worldDuplicateTileY = (int) (duplicatePos.y / tileSize);
            String duplicateTileKey = worldDuplicateTileX + "," + worldDuplicateTileY;

            if (exploredTiles.contains(duplicateTileKey)) {
                int duplicateTileX = worldDuplicateTileX - worldMinTileX;
                int duplicateTileY = worldDuplicateTileY - worldMinTileY;

                if (duplicateTileX >= 0 && duplicateTileX < totalTilesX &&
                        duplicateTileY >= 0 && duplicateTileY < totalTilesY) {

                    float duplicateDisplayX = mapStartX + duplicateTileX * tileDisplaySize;
                    float duplicateDisplayY = mapStartY + duplicateTileY * tileDisplaySize;

                    // Pulsing lighter green marker for duplicate - same size as main
                    float pulse = 1f + (float) Math.sin(System.currentTimeMillis() / 250.0 + Math.PI) * 0.25f; // Offset pulse
                    shapeRenderer.setColor(HERMAN_DUPLICATE_COLOR.r, HERMAN_DUPLICATE_COLOR.g, HERMAN_DUPLICATE_COLOR.b, 0.9f);
                    float pulseSize = tileDisplaySize * 2.0f * pulse;
                    float pulseOffset = (pulseSize - tileDisplaySize) / 2f;
                    shapeRenderer.rect(
                            duplicateDisplayX - pulseOffset,
                            duplicateDisplayY - pulseOffset,
                            pulseSize,
                            pulseSize
                    );
                }
            }
        }

        // Draw player
        Vector2 playerPos = player.getPosition();
        int playerTileX = (int) (playerPos.x / tileSize) - worldMinTileX;
        int playerTileY = (int) (playerPos.y / tileSize) - worldMinTileY;

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

        batch.begin();
        batch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}