package managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import entities.Player;
import game.GameProj;

public class ProjectileManager {
    private final GameProj gameP;
    private final Body body;
    private final Box2DWorld world;
    private final Player player;

    public ProjectileManager(GameProj gameP, Body body, Box2DWorld world, Player player) {
        this.gameP = gameP;
        this.body = body;
        this.world = world;
        this.player = player;
    }

    public void createSpear() {
        Vector3 mousePosition3D = gameP.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);
        Vector2 playerPosition = body.getPosition();

        float angle = (float) Math.atan2(mousePosition.y - playerPosition.y, mousePosition.x - playerPosition.x);
        float spearSpeed = 150f;

        Vector2 velocity = new Vector2((float) Math.cos(angle) * spearSpeed, (float) Math.sin(angle) * spearSpeed);

        BodyDef spearBodyDef = new BodyDef();
        spearBodyDef.type = BodyDef.BodyType.DynamicBody;
        spearBodyDef.position.set(playerPosition.x + velocity.x / spearSpeed, playerPosition.y + velocity.y / spearSpeed);
        spearBodyDef.angle = angle;
        spearBodyDef.bullet = true;

        Body spearBody = world.getWorld().createBody(spearBodyDef);
        spearBody.setFixedRotation(true);

        PolygonShape spearShape = new PolygonShape();
        spearShape.setAsBox(2f, 2f, new Vector2(0, 0), angle);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = spearShape;
        fixtureDef.density = 1f;
        fixtureDef.isSensor = false;
        fixtureDef.filter.categoryBits = CollisionFilter.SPEAR;
        fixtureDef.filter.maskBits = CollisionFilter.OBSTACLE | CollisionFilter.ENEMY;

        spearBody.createFixture(fixtureDef);
        spearShape.dispose();

        spearBody.setLinearVelocity(velocity);
        spearBody.setUserData(this);

        player.addSpearBodies(spearBody, velocity);
    }
}
