package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
    private static final float OBJECTIVE_TEXT_SCALE = 0.6f;
    private static final float OBJECTIVE_TEXT_OFFSET_Y = -25f;

    private final Viewport hudViewport;
    private final OrthographicCamera worldCamera;
    private final Texture levelUpTexture;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Vector3 tmpWorldPos = new Vector3();
    private final List<Popup> popups = new ArrayList<>();
    private final List<PopupText> popupTexts = new ArrayList<>();
    private final List<PopupShortText> popupShortTexts = new ArrayList<>();
    private String objectiveText;
    private float objectiveAnchorX;
    private float objectiveAnchorY;


    public PopupIndicator(Viewport hudViewport, OrthographicCamera worldCamera) {
        this.hudViewport = hudViewport;
        this.worldCamera = worldCamera;
        this.levelUpTexture = Storage.assetManager.get("ui/LevelUp.png", Texture.class);
        this.font = Storage.assetManager.get("fonts/CascadiaBold.fnt", BitmapFont.class);
        this.objectiveAnchorX = 10f;
        this.objectiveAnchorY = hudViewport.getWorldHeight() - 150f;
    }

    public void showLevelUp(Vector2 followTarget) {
        show(levelUpTexture, followTarget, 0f, 0f, LEVEL_UP_DURATION, 1.0f);
    }

    public void showObjective(Vector2 followTarget, float offsetX, float offsetY, String text) {
        this.objectiveText = text;
        showShortText(text, followTarget, offsetX, offsetY, 3f, 1.0f);
    }

    public void showTips(Vector2 followTarget, float offsetX, float offsetY, String text) {
        showTips(text, followTarget, offsetX, offsetY, 3f, 1.0f);
    }

    public void showTips(String text, Vector2 followTarget, float offsetX, float offsetY, float duration, float scale) {
        popupShortTexts.add(new PopupShortText(text, followTarget, offsetX, offsetY, duration, scale));
    }

    public void showShortText(String text, Vector2 followTarget, float offsetX, float offsetY, float duration, float scale) {
        popupShortTexts.add(new PopupShortText(text, followTarget, offsetX, offsetY, duration, scale));
    }

    public void show(Texture texture, Vector2 followTarget, float offsetX, float offsetY, float duration, float scale) {
        popups.add(new Popup(texture, followTarget, offsetX, offsetY + LEVEL_UP_OFFSET_Y, duration, scale));
    }

    public void setObjectiveAnchor(float x, float y) {
        this.objectiveAnchorX = x;
        this.objectiveAnchorY = y;
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

        Iterator<PopupText> iteratorText = popupTexts.iterator();
        while (iteratorText.hasNext()) {
            PopupText popupText = iteratorText.next();
            popupText.timer += delta;
            if (popupText.timer >= popupText.duration) {
                iteratorText.remove();
            }
        }

        Iterator<PopupShortText> iteratorShortText = popupShortTexts.iterator();
        while (iteratorShortText.hasNext()) {
            PopupShortText popupShortText = iteratorShortText.next();
            popupShortText.timer += delta;
            if (popupShortText.timer >= popupShortText.duration) {
                iteratorShortText.remove();
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (Popup popup : popups) {
            float progress = popup.timer / popup.duration;
            float alpha = 1f - progress;
            batch.setColor(1f, 1f, 1f, alpha);

            float screenX = popup.x;
            float screenY = popup.y;
            if (popup.followTarget != null) {
                tmpWorldPos.set(popup.followTarget.x + popup.offsetX, popup.followTarget.y + popup.offsetY, 0f);
                worldCamera.project(tmpWorldPos, hudViewport.getScreenX(), hudViewport.getScreenY(),
                        hudViewport.getScreenWidth(), hudViewport.getScreenHeight());
                screenX = tmpWorldPos.x;
                screenY = tmpWorldPos.y;
            }

            float width = popup.texture.getWidth() * popup.scale;
            float height = popup.texture.getHeight() * popup.scale;
            batch.draw(popup.texture, screenX - width / 2f, screenY / 2 + 150f, width, height);
        }
        batch.setColor(Color.WHITE);

        for (PopupText popupText : popupTexts) {
            float progress = popupText.timer / popupText.duration;
            float alpha = 1f - progress;
            font.getData().setScale(popupText.scale);
            font.setColor(1f, 1f, 1f, alpha);

            float worldX = popupText.followTarget != null ? popupText.followTarget.x + popupText.offsetX : popupText.worldX;
            float worldY = popupText.followTarget != null ? popupText.followTarget.y + popupText.offsetY : popupText.worldY;
            tmpWorldPos.set(worldX, worldY, 0f);
            worldCamera.project(tmpWorldPos, hudViewport.getScreenX(), hudViewport.getScreenY(),
                    hudViewport.getScreenWidth(), hudViewport.getScreenHeight());
            float screenX = tmpWorldPos.x;
            float screenY = tmpWorldPos.y + LEVEL_UP_OFFSET_Y;

            glyphLayout.setText(font, popupText.text);
            font.draw(batch, popupText.text, screenX - glyphLayout.width / 2f, screenY);
        }
        if (objectiveText != null && !objectiveText.isEmpty()) {
            font.getData().setScale(OBJECTIVE_TEXT_SCALE);
            font.setColor(Color.YELLOW);
            font.draw(batch, objectiveText, objectiveAnchorX + 5, objectiveAnchorY + OBJECTIVE_TEXT_OFFSET_Y);
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);

        for (PopupShortText popupShortText : popupShortTexts) {
            float progress = popupShortText.timer / popupShortText.duration;
            float alpha = 1f - progress;
            font.getData().setScale(popupShortText.scale);
            font.setColor(1f, 1f, 1f, alpha);

            float worldX = popupShortText.followTarget != null ? popupShortText.followTarget.x + popupShortText.offsetX : popupShortText.worldX;
            float worldY = popupShortText.followTarget != null ? popupShortText.followTarget.y + popupShortText.offsetY : popupShortText.worldY;
            tmpWorldPos.set(worldX, worldY, 0f);
            worldCamera.project(tmpWorldPos, hudViewport.getScreenX(), hudViewport.getScreenY(),
                    hudViewport.getScreenWidth(), hudViewport.getScreenHeight());
            float screenX = tmpWorldPos.x;
            float screenY = tmpWorldPos.y + LEVEL_UP_OFFSET_Y;

            glyphLayout.setText(font, popupShortText.text);
            font.draw(batch, popupShortText.text, screenX - glyphLayout.width / 2f, screenY);
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
    }

    private static class Popup {
        private final Texture texture;
        private final Vector2 followTarget;
        private final float offsetX;
        private final float offsetY;
        private final float x;
        private final float y;
        private final float duration;
        private final float scale;
        private float timer;

        private Popup(Texture texture, Vector2 followTarget, float offsetX, float offsetY, float duration, float scale) {
            this.texture = texture;
            this.followTarget = followTarget;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.x = 0f;
            this.y = 0f;
            this.duration = duration;
            this.scale = scale;
            this.timer = 0f;
        }
    }

    private static class PopupText {
        private final String text;
        private final Vector2 followTarget;
        private final float offsetX;
        private final float offsetY;
        private final float worldX;
        private final float worldY;
        private final float duration;
        private final float scale;
        private float timer;

        private PopupText(String text, float worldX, float worldY, float duration, float scale) {
            this.text = text;
            this.followTarget = null;
            this.offsetX = 0f;
            this.offsetY = 0f;
            this.worldX = worldX;
            this.worldY = worldY;
            this.duration = duration;
            this.scale = scale;
            this.timer = 0f;
        }
    }

    private static class PopupShortText {
        private final String text;
        private final Vector2 followTarget;
        private final float offsetX;
        private final float offsetY;
        private final float worldX;
        private final float worldY;
        private final float duration;
        private final float scale;
        private float timer;

        private PopupShortText(String text, Vector2 followTarget, float offsetX, float offsetY, float duration, float scale) {
            this.text = text;
            this.followTarget = followTarget;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.worldX = 0f;
            this.worldY = 0f;
            this.duration = duration;
            this.scale = scale;
            this.timer = 0f;
        }
    }
}
