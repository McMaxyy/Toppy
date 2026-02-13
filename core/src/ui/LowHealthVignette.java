package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class LowHealthVignette implements Disposable {
    private static final float HEALTH_THRESHOLD = 0.2f;
    private static final float PULSE_SPEED = 2.0f;
    private static final float BASE_ALPHA = 0.35f;
    private static final float PULSE_ALPHA = 0.15f;
    private static final float INNER_RADIUS = 0.55f;
    private static final float OUTER_RADIUS = 1.0f;

    private Texture vignetteTexture;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private float pulseTimer = 0f;
    private float alpha = 0f;

    public LowHealthVignette() {
    }

    public void update(float delta, float healthPercent) {
        if (healthPercent < HEALTH_THRESHOLD) {
            pulseTimer += delta * PULSE_SPEED;
            float pulse = (float) ((Math.sin(pulseTimer) + 1f) * 0.5f);
            alpha = BASE_ALPHA + pulse * PULSE_ALPHA;
        } else {
            alpha = 0f;
        }
    }

    public void render(SpriteBatch batch, OrthographicCamera hudCamera) {
        if (alpha <= 0f) return;

        int width = Math.max(1, (int)hudCamera.viewportWidth);
        int height = Math.max(1, (int)hudCamera.viewportHeight);
        ensureTexture(width, height);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.setColor(0.4f, 0f, 0f, alpha);
        batch.draw(vignetteTexture, 0, 0, width, height);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        if (vignetteTexture != null) {
            vignetteTexture.dispose();
            vignetteTexture = null;
        }
    }

    private void ensureTexture(int width, int height) {
        if (width == cachedWidth && height == cachedHeight && vignetteTexture != null) {
            return;
        }

        if (vignetteTexture != null) {
            vignetteTexture.dispose();
        }

        cachedWidth = width;
        cachedHeight = height;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radiusX = width / 2f;
        float radiusY = height / 2f;

        for (int y = 0; y < height; y++) {
            float dy = (y + 0.5f - centerY) / radiusY;
            for (int x = 0; x < width; x++) {
                float dx = (x + 0.5f - centerX) / radiusX;
                float r = (float)Math.sqrt(dx * dx + dy * dy);
                float edge = 0f;
                if (r > INNER_RADIUS) {
                    edge = Math.min(1f, (r - INNER_RADIUS) / (OUTER_RADIUS - INNER_RADIUS));
                }
                pixmap.setColor(1f, 1f, 1f, edge);
                pixmap.drawPixel(x, y);
            }
        }

        vignetteTexture = new Texture(pixmap);
        pixmap.dispose();
    }
}
