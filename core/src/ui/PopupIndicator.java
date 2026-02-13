package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import config.Storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PopupIndicator {
    private static final float LEVEL_UP_DURATION = 2.0f;
    private static final float LEVEL_UP_OFFSET_Y = 60f;

    private final Viewport hudViewport;
    private final OrthographicCamera worldCamera;
    private final Texture levelUpTexture;
    private final List<Popup> popups = new ArrayList<>();

    public PopupIndicator(Viewport hudViewport, OrthographicCamera worldCamera) {
        this.hudViewport = hudViewport;
        this.worldCamera = worldCamera;
        this.levelUpTexture = Storage.assetManager.get("ui/LevelUp.png", Texture.class);
    }

    public void showLevelUp(float worldX, float worldY) {
        show(levelUpTexture, worldX, worldY, LEVEL_UP_DURATION, 1.0f);
    }

    public void show(Texture texture, float worldX, float worldY, float duration, float scale) {
        Vector3 worldPos = new Vector3(worldX, worldY, 0f);
        worldCamera.project(worldPos, hudViewport.getScreenX(), hudViewport.getScreenY(),
                hudViewport.getScreenWidth(), hudViewport.getScreenHeight());

        Vector2 screenPos = new Vector2(worldPos.x, worldPos.y);
        popups.add(new Popup(texture, screenPos.x, screenPos.y + LEVEL_UP_OFFSET_Y, duration, scale));
    }

    public void update(float delta) {
        Iterator<Popup> iterator = popups.iterator();
        while (iterator.hasNext()) {
            Popup popup = iterator.next();
            popup.timer += delta;
            if (popup.timer >= popup.duration) {
                iterator.remove();
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (Popup popup : popups) {
            float progress = popup.timer / popup.duration;
            float alpha = 1f - progress;
            batch.setColor(1f, 1f, 1f, alpha);

            float width = popup.texture.getWidth() * popup.scale;
            float height = popup.texture.getHeight() * popup.scale;
            batch.draw(popup.texture, popup.x - width / 2f, popup.y, width, height);
        }
        batch.setColor(Color.WHITE);
    }

    private static class Popup {
        private final Texture texture;
        private final float x;
        private final float y;
        private final float duration;
        private final float scale;
        private float timer;

        private Popup(Texture texture, float x, float y, float duration, float scale) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.duration = duration;
            this.scale = scale;
            this.timer = 0f;
        }
    }
}
