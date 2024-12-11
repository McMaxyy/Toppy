package entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import config.Storage;
import game.GameProj;
import managers.AnimationManager;
import managers.Box2DWorld;
import managers.CollisionFilter;

public class Player {
    private Body body, spearBody;
    private float speed, maxDistance = 100f, distanceTraveled;
    private Vector2 velocity;
    private AnimationManager animationManager;
    private static int direction = 0;
    private GameProj gameP;
    private boolean isAttacking = false;
    private Box2DWorld world;
    private boolean markedForRemoval = false;

    public Player(Box2DWorld world, AnimationManager animationManager, int size, GameProj gameP) {
        this.animationManager = animationManager;
        this.speed = 5000f;
        this.gameP = gameP;
        this.world = world;

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(0, 0);

        body = world.getWorld().createBody(bodyDef);
        body.setFixedRotation(true);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(size / 5f, size / 5f);

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
        
        if (spearBody != null) {
        	distanceTraveled += velocity.len() * delta;
        	
        	if (distanceTraveled >= maxDistance || markedForRemoval)
        		removeSpear();
        }
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }
    
    public void updateBounds() {}
    
    public void input(float delta) {
    	float moveX = 0;
        float moveY = 0;
        
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;
        
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && spearBody == null) {
        	getAnimationManager().setState(AnimationManager.State.ATTACKING);  
        	isAttacking = true;
        	createSpear();
        	return;
        }  
        
    	move(moveX, moveY, delta);

        updateBounds();
        
        gameP.generateChunksAroundPlayer();
    }
    
    private void updateAnimationState() {
    	 if (getAnimationManager().getState() == AnimationManager.State.ATTACKING) {
            if (getAnimationManager().isAnimationFinished(direction)) 
            	isAttacking = false;
            }
    }

    public void move(float dx, float dy, float delta) {
    	if (spearBody != null) {
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
    
    private void createSpear() {
        BodyDef spearBodyDef = new BodyDef();
        spearBodyDef.type = BodyDef.BodyType.DynamicBody;

        float offsetX = 0;
        float offsetY = 0;
        float spearSpeed = 150f;

        switch (direction) {
            case 1: // Facing up
                offsetY = 0.5f;
                velocity = new Vector2(0, spearSpeed);
                break;
            case 0: // Facing down
                offsetY = -0.5f;
                velocity = new Vector2(0, -spearSpeed);
                break;
            case 2: // Facing left
                offsetX = -0.5f;
                velocity = new Vector2(-spearSpeed, 0);
                break;
            case 3: // Facing right
                offsetX = 0.5f;
                velocity = new Vector2(spearSpeed, 0);
                break;
        }

        spearBodyDef.position.set(body.getPosition().x + offsetX, body.getPosition().y + offsetY);
        spearBodyDef.bullet = true;

        spearBody = world.getWorld().createBody(spearBodyDef);

        PolygonShape spearShape = new PolygonShape();
        spearShape.setAsBox(2f, 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = spearShape;
        fixtureDef.density = 1f;
        fixtureDef.isSensor = false;
        
        fixtureDef.filter.categoryBits = CollisionFilter.SPEAR;
        fixtureDef.filter.maskBits = CollisionFilter.OBSTACLE;

        spearBody.createFixture(fixtureDef);
        spearShape.dispose();

        spearBody.setLinearVelocity(velocity);
        spearBody.setUserData(this);
        distanceTraveled = 0f;
    }

    public void markForRemoval() {
    	markedForRemoval = true;
    }
    
    public void removeSpear() {
    	if(spearBody != null) {
    		world.getWorld().destroyBody(spearBody);
    		spearBody = null;
    		markedForRemoval = false;
    	}
    }

    public void render(SpriteBatch batch, int TILE_SIZE) {
        Vector2 position = body.getPosition();

        getAnimationManager().update(Gdx.graphics.getDeltaTime());
        batch.draw(getAnimationManager().getCurrentFrame(direction),
                   position.x - TILE_SIZE / 2f, 
                   position.y - TILE_SIZE / 2f, 
                   TILE_SIZE, TILE_SIZE);
        
        if(spearBody != null) {
            Texture spearTexture = Storage.assetManager.get("character/Spear.png");
            TextureRegion spearRegion = new TextureRegion(spearTexture);
            int rotationAngle = 45;
            int spearWidth = spearTexture.getWidth();
            int spearHeight = spearTexture.getHeight();
            float posX = 0;
            float posY = 0;           
            
            switch(direction) {
            case 1:
            	rotationAngle = 45;
            	posX = spearBody.getPosition().x - spearWidth / 2;
            	posY = spearBody.getPosition().y + spearHeight / 50f;
            	break;
            case 0:
            	rotationAngle = 225;
            	posX = spearBody.getPosition().x - spearWidth / 2;
            	posY = spearBody.getPosition().y - spearHeight * 1.05f;
            	break;
            case 2:
            	rotationAngle = 135;
            	posX = spearBody.getPosition().x - spearWidth * 1.05f;
            	posY = spearBody.getPosition().y - spearHeight / 2f;
            	break;
            case 3:
            	rotationAngle = 315;
            	posX = spearBody.getPosition().x + spearWidth / 50f;
            	posY = spearBody.getPosition().y - spearHeight / 2f;
            	break;
            }
            
            batch.draw(spearRegion, 
                    posX, posY, 
                    spearWidth / 2, spearHeight / 2, 
                    spearWidth / 4, spearHeight / 4, 
                    1, 1, 
                    rotationAngle);
        }
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
