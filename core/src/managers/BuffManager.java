package managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import config.Storage;
import entities.Player;

public class BuffManager {

    public enum BuffType {
        ATTACK_POTION(0, "Attack Potion", "icons/items/AttackBuff.png", "Big Damage", "Strong"),
        DEFENSE_POTION(1, "Defense Potion", "icons/items/DefenseBuff.png", "Big Defense", "Big"),
        DEX_POTION(2, "Dex Potion", "icons/items/DexBuff.png", "Fast af", "You are speed"),
        LUCKY_CLOVER(3, "Lucky Clover", "icons/items/LuckBuff.png", "Lucky", "2x Coin Drops");

        public final int index;
        public final String name;
        public final String iconPath;
        public final String displayName;
        public final String description;

        BuffType(int index, String name, String iconPath, String displayName, String description) {
            this.index = index;
            this.name = name;
            this.iconPath = iconPath;
            this.displayName = displayName;
            this.description = description;
        }
    }

    private static final float BUFF_DURATION = 30f;
    private static final int ICON_SIZE = 28;
    private static final int ICON_PADDING = 6;
    private static final int UI_MARGIN = 10;

    private float[] buffTimers;
    private boolean[] activeBuffs;
    private Texture[] buffIcons;
    private Player player;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    private int hoveredBuffIndex = -1;

    private static final float LUCK_COIN_MULTIPLIER = 2.0f;

    public BuffManager(Player player) {
        this.player = player;
        this.buffTimers = new float[4];
        this.activeBuffs = new boolean[4];
        this.buffIcons = new Texture[4];
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.glyphLayout = new GlyphLayout();

        for (BuffType buff : BuffType.values()) {
            buffIcons[buff.index] = Storage.assetManager.get(buff.iconPath, Texture.class);
        }
    }

    public void activateBuff(String buffName) {
        BuffType buffType = getBuffTypeByName(buffName);
        if (buffType != null) {
            activeBuffs[buffType.index] = true;
            buffTimers[buffType.index] = BUFF_DURATION;
        }
    }

    private BuffType getBuffTypeByName(String name) {
        for (BuffType buff : BuffType.values()) {
            if (buff.name.equals(name)) {
                return buff;
            }
        }
        return null;
    }

    public void update(float delta) {
        updateHoverState();

        for (BuffType buff : BuffType.values()) {
            if (activeBuffs[buff.index]) {
                buffTimers[buff.index] -= delta;

                if (buffTimers[buff.index] <= 0) {
                    activeBuffs[buff.index] = false;
                    buffTimers[buff.index] = 0f;
                    player.removePlayerBuff(buff.name);
                }
            }
        }
    }

    private void updateHoverState() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY(); // Convert to screen coordinates

        float startX = screenWidth - UI_MARGIN - ICON_SIZE;
        float startY = screenHeight - UI_MARGIN - ICON_SIZE;

        hoveredBuffIndex = -1;

        int renderedCount = 0;
        for (BuffType buff : BuffType.values()) {
            if (activeBuffs[buff.index]) {
                float x = startX - (renderedCount * (ICON_SIZE + ICON_PADDING));
                float y = startY;

                if (mouseX >= x - 2 && mouseX <= x + ICON_SIZE + 2 &&
                        mouseY >= y - 12 && mouseY <= y + ICON_SIZE + 2) {
                    hoveredBuffIndex = buff.index;
                    break;
                }

                renderedCount++;
            }
        }
    }

    public void render(SpriteBatch batch) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int activeBuffCount = 0;

        for (int i = 0; i < activeBuffs.length; i++) {
            if (activeBuffs[i]) {
                activeBuffCount++;
            }
        }

        if (activeBuffCount == 0) return;

        float startX = screenWidth - UI_MARGIN - ICON_SIZE;
        float startY = screenHeight - UI_MARGIN - ICON_SIZE;

        int renderedCount = 0;

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (BuffType buff : BuffType.values()) {
            if (activeBuffs[buff.index]) {
                float x = startX - (renderedCount * (ICON_SIZE + ICON_PADDING));
                float y = startY;

                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
                shapeRenderer.rect(x, y - 10, ICON_SIZE, 5);

                float timerPercent = buffTimers[buff.index] / BUFF_DURATION;
                Color barColor = getBuffBarColor(buff);
                shapeRenderer.setColor(barColor);
                shapeRenderer.rect(x, y - 10, ICON_SIZE * timerPercent, 5);

                renderedCount++;
            }
        }

        shapeRenderer.end();

        if (hoveredBuffIndex >= 0) {
            renderTooltip(startX, startY);
        }

        batch.begin();

        renderedCount = 0;
        for (BuffType buff : BuffType.values()) {
            if (activeBuffs[buff.index]) {
                float x = startX - (renderedCount * (ICON_SIZE + ICON_PADDING));
                float y = startY;

                batch.draw(buffIcons[buff.index], x, y - 3, ICON_SIZE, ICON_SIZE);
                renderedCount++;
            }
        }

        if (hoveredBuffIndex >= 0) {
            renderTooltipText(batch, startX, startY);
        }
    }

    private void renderTooltip(float startX, float startY) {
        BuffType hoveredBuff = null;
        for (BuffType buff : BuffType.values()) {
            if (buff.index == hoveredBuffIndex) {
                hoveredBuff = buff;
                break;
            }
        }

        if (hoveredBuff == null) return;

        float tooltipWidth = 150;
        float tooltipHeight = 60;
        float tooltipX = startX - tooltipWidth + ICON_SIZE;
        float tooltipY = startY - ICON_SIZE - tooltipHeight - 10;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.95f);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Color borderColor = getBuffBarColor(hoveredBuff);
        shapeRenderer.setColor(borderColor);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);
    }

    private void renderTooltipText(SpriteBatch batch, float startX, float startY) {
        BuffType hoveredBuff = null;
        for (BuffType buff : BuffType.values()) {
            if (buff.index == hoveredBuffIndex) {
                hoveredBuff = buff;
                break;
            }
        }

        if (hoveredBuff == null) return;

        float tooltipWidth = 150;
        float tooltipHeight = 60;
        float tooltipX = startX - tooltipWidth + ICON_SIZE;
        float tooltipY = startY - ICON_SIZE - tooltipHeight - 10;

        float textX = tooltipX + 8;
        float textY = tooltipY + tooltipHeight - 10;

        font.getData().setScale(0.5f);
        Color nameColor = getBuffBarColor(hoveredBuff);
        font.setColor(nameColor);
        font.draw(batch, hoveredBuff.displayName, textX, textY);

        font.getData().setScale(0.4f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, hoveredBuff.description, textX, textY - 18);

        String timeText = formatTime(buffTimers[hoveredBuff.index]);
        font.setColor(Color.WHITE);
        font.draw(batch, timeText, textX, textY - 36);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private Color getBuffBarColor(BuffType buff) {
        switch (buff) {
            case ATTACK_POTION:
                return new Color(0.9f, 0.3f, 0.2f, 1f);
            case DEFENSE_POTION:
                return new Color(0.2f, 0.5f, 0.9f, 1f);
            case DEX_POTION:
                return new Color(0.2f, 0.9f, 0.3f, 1f);
            case LUCKY_CLOVER:
                return new Color(0.9f, 0.8f, 0.2f, 1f);
            default:
                return Color.WHITE;
        }
    }

    private String formatTime(float seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", mins, secs);
    }

    public boolean isBuffActive(BuffType buff) {
        return activeBuffs[buff.index];
    }

    public boolean isBuffActive(String buffName) {
        BuffType buff = getBuffTypeByName(buffName);
        return buff != null && activeBuffs[buff.index];
    }

    public boolean isLuckyCloverActive() {
        return activeBuffs[BuffType.LUCKY_CLOVER.index];
    }

    public float getCoinMultiplier() {
        return isLuckyCloverActive() ? LUCK_COIN_MULTIPLIER : 1.0f;
    }

    public float getBuffRemainingTime(BuffType buff) {
        return buffTimers[buff.index];
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}