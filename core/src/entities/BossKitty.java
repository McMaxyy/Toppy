package entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import managers.AnimationManager;
import managers.AnimationManager.State;

public class BossKitty {
	public Rectangle bounds;
    private Body body;
    private final Player player;
    private final float detectionRadius = 300f;
    private final float speed = 80f;
    private boolean markForRemoval = false, isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;

	public BossKitty(Rectangle bounds, Body body, Player player, AnimationManager animationManager) {
		this.animationManager = animationManager;
    	this.bounds = bounds;
        this.body = body;
        this.player = player;
	}
	
	public void update(float delta) {
		if (markForRemoval) {
            dispose();
            return;
        }
		
    	isFlipped = body.getPosition().x > player.getBody().getPosition().x;
    	
    	bounds.setPosition(body.getPosition().x - bounds.width / 2f, 
                			body.getPosition().y - bounds.height / 2f);
    	
    	if (isPlayerInRadius()) {
            moveTowardsPlayer();
            isMoving = true;
        } else {
            if (getAnimationManager().getState("Mushie") != State.IDLE) {
            	getAnimationManager().setState(State.IDLE, "Mushie");
                isMoving = false;
            }
        }
        
        if (isMoving && getAnimationManager().getState("BossKitty") != State.RUNNING)
        	getAnimationManager().setState(State.RUNNING, "BossKitty");
	}

	private void dispose() {
		if (body != null && markForRemoval) {
            body.getWorld().destroyBody(body);
            body = null;            
        }		
	}
	
	public void removeEnemies() {
    	if (body != null) {
    		body.getWorld().destroyBody(body);
    		body = null;
    	}
    }
	
	public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            TextureRegion currentFrame = new TextureRegion(getAnimationManager().getBossKittyCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
	
	public void markForRemoval() {
        markForRemoval = true;
    }
    
    public boolean isMarkedForRemoval() {
    	return markForRemoval;
    }
    
    public Body getBody() {
    	return body;
    }
    
    private boolean isPlayerInRadius() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        float distance = playerPosition.dst(enemyPosition);

        return distance <= detectionRadius;
    }
    
    private void moveTowardsPlayer() {
        Vector2 playerPosition = player.getPosition();
        Vector2 enemyPosition = new Vector2(body.getPosition().x, body.getPosition().y);

        Vector2 direction = new Vector2(playerPosition.x - enemyPosition.x, 
                                        playerPosition.y - enemyPosition.y).nor();

        body.setLinearVelocity(direction.scl(speed));
    }
    
    public AnimationManager getAnimationManager() {
        return animationManager;
    }
}
