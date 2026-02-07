package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import config.Storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayerHealthPopup {
    private final List<HealthPopup> activePopups;
    private final BitmapFont font;
    private final Viewport hudViewport;
    private final OrthographicCamera worldCamera;

    private static final float POPUP_DURATION = 1.0f;
    private static final float FLOAT_SPEED = 200f;
    private static final float INITIAL_Y_OFFSET = 40f;

    public PlayerHealthPopup(Viewport hudViewport, OrthographicCamera worldCamera) {
        this.hudViewport = hudViewport;
        this.worldCamera = worldCamera;
        this.activePopups = new ArrayList<>();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
    }

    public void showHealthChange(int amount, float playerWorldX, float playerWorldY) {
        if (amount == 0) return;

        Vector3 worldPos = new Vector3(playerWorldX, playerWorldY, 0);
        worldCamera.project(worldPos, hudViewport.getScreenX(), hudViewport.getScreenY(),
                hudViewport.getScreenWidth(), hudViewport.getScreenHeight());

        Vector2 screenPos = new Vector2(worldPos.x, worldPos.y);

        float randomOffsetX = (float) (Math.random() * 20 - 10);

        HealthPopup popup = new HealthPopup(
                amount,
                screenPos.x + randomOffsetX,
                screenPos.y + INITIAL_Y_OFFSET,
                amount > 0
        );

        activePopups.add(popup);
    }

    public void update(float delta) {
        Iterator<HealthPopup> iterator = activePopups.iterator();
        while (iterator.hasNext()) {
            HealthPopup popup = iterator.next();
            popup.update(delta);
            if (popup.isFinished()) {
                iterator.remove();
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (HealthPopup popup : activePopups) {
            popup.render(batch, font);
        }
    }

    public void clear() {
        activePopups.clear();
    }

    private static class HealthPopup {
        private final String text;
        private final boolean isHeal;
        private float x;
        private float y;
        private float timer;

        public HealthPopup(int amount, float x, float y, boolean isHeal) {
            this.text = (isHeal ? "+" : "") + amount;
            this.isHeal = isHeal;
            this.x = x;
            this.y = y;
            this.timer = 0f;
        }

        public void update(float delta) {
            timer += delta;
            y += FLOAT_SPEED * delta;
        }

        public void render(SpriteBatch batch, BitmapFont font) {
            float progress = timer / POPUP_DURATION;
            float alpha = 1f - progress;

            Color color;
            if (isHeal) {
                color = new Color(0.2f, 1f, 0.2f, alpha);
            } else {
                color = new Color(1f, 0.2f, 0.2f, alpha);
            }

            font.getData().setScale(0.6f);

            float[] offsets = {
                    -1, 0,
                    1, 0,
                    0, -1,
                    0, 1,
                    -1, -1,
                    1, -1,
                    -1, 1,
                    1, 1
            };

            Color outlineColor = new Color(color.r * 0.3f, color.g * 0.3f, color.b * 0.3f, alpha * 0.8f);
            font.setColor(outlineColor);
            for (int i = 0; i < offsets.length; i += 2) {
                font.draw(batch, text, x + offsets[i], y + offsets[i + 1]);
            }

            font.setColor(color);
            font.draw(batch, text, x, y);

            font.setColor(Color.WHITE);
            font.getData().setScale(1f);
        }

        public boolean isFinished() {
            return timer >= POPUP_DURATION;
        }
    }
}