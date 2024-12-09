package entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

import game.GameProj;
import managers.AnimationManager;
import managers.Box2DWorld;
import managers.CollisionFilter;

public class Player {
    private Body body;
    private float speed;
    private AnimationManager animationManager;
    private int direction = 0;
    private GameProj gameP;
    private boolean isAttacking = false;

    public Player(Box2DWorld world, AnimationManager animationManager, int size, GameProj gameP) {
        this.animationManager = animationManager;
        this.speed = 5000f;
        this.gameP = gameP;

        // Create the Box2D body for the player
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(0, 0);

        body = world.getWorld().createBody(bodyDef);
        body.setFixedRotation(true);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(size / 4f, size / 4f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.3f;

        fixtureDef.filter.categoryBits = CollisionFilter.PLAYER;
        fixtureDef.filter.maskBits = CollisionFilter.OBSTACLE;

        body.createFixture(fixtureDef);
        shape.dispose();
    }
    
    public void update(float delta) {
    	input(delta);
        updateAnimationState();
        getAnimationManager().update(Gdx.graphics.getDeltaTime());
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }
    
    public void updateBounds() {}
    
    public void input(float delta) {
    	if(isAttacking)
    		return;
    	
    	float moveX = 0;
        float moveY = 0;
        
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;
        
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
        	getAnimationManager().setState(AnimationManager.State.ATTACKING);  
        	isAttacking = true;
        	return;
        }  
        
    	move(moveX, moveY, delta);

        updateBounds();
        
        gameP.generateChunksAroundPlayer();
    }
    
    private void updateAnimationState() {
    	 if (getAnimationManager().getState() == AnimationManager.State.ATTACKING) {
            if (getAnimationManager().isAnimationFinished(direction)) 
            	getAnimationManager().setState(AnimationManager.State.IDLE);
            	isAttacking = false;
            }
    }

    public void move(float dx, float dy, float delta) {
    	if (isAttacking) {
            body.setLinearVelocity(0, 0);
            return;
        }
    	
        float magnitude = (float) Math.sqrt(dx * dx + dy * dy);

        if (magnitude > 0) {
            dx /= magnitude;
            dy /= magnitude;

            if (Math.abs(dx) > Math.abs(dy)) {
                direction = dx > 0 ? 3 : 2;
            } else {
                direction = dy > 0 ? 1 : 0;
            }

            getAnimationManager().setState(AnimationManager.State.RUNNING);
        } else {
        	getAnimationManager().setState(AnimationManager.State.IDLE);
        }

        float velocityX = dx * speed * delta;
        float velocityY = dy * speed * delta;

        body.setLinearVelocity(velocityX, velocityY);
    }

    public void render(SpriteBatch batch, int TILE_SIZE) {
        Vector2 position = body.getPosition();

        // Get the current animation frame based on the state and direction
        getAnimationManager().update(Gdx.graphics.getDeltaTime());
        batch.draw(getAnimationManager().getCurrentFrame(direction),
                   position.x - TILE_SIZE / 2f, 
                   position.y - TILE_SIZE / 2f, 
                   TILE_SIZE, TILE_SIZE);
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }
    
    public AnimationManager getAnimationManager() {
        return animationManager;
    }
}
