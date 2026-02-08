package managers;

import com.badlogic.gdx.graphics.Texture;

public class DecorationType {
    private final Texture texture;
    private final float width;
    private final float height;
    private final int density;

    public DecorationType(Texture texture, float width, float height, int density) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.density = density;
    }

    public Texture getTexture() {
        return texture;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public int getDensity() {
        return density;
    }
}