package managers;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;

public class Box2DWorld {
    private World world;

    public Box2DWorld(ContactListener listener) {
        world = new World(new Vector2(0, 0), true);
        world.setContactListener(listener);
    }

    public void step(float delta) {
        world.step(delta, 6, 2);
    }

    public World getWorld() {
        return world;
    }

    public void dispose() {
        world.dispose();
    }
}

