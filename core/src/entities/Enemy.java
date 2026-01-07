package entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import managers.AnimationManager;
import managers.AnimationManager.State;

public class Enemy {
	public Rectangle bounds;
    private final Texture texture;
    private Body body;
    private final Player player;
    private final float detectionRadius = 150f;
    private final float speed = 60f; 
    private boolean markForRemoval = false, isMoving = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    
    public Enemy(Rectangle bounds, Texture texture, Body body, Player player, AnimationManager animationManager) {
        this.animationManager = animationManager;
    	this.bounds = bounds;
        this.texture = texture;
        this.body = body;
        this.player = player;
        
        getAnimationManager().setState(AnimationManager.State.IDLE, "Mushie");
    }

    public void update(float delta) {   	   	
    	if (markForRemoval) {
            dispose();
            return;
        }
    	     	 
    	if (!Player.gameStarted) {
    		body.setLinearVelocity(0, 0); 
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
        
        if (isMoving && getAnimationManager().getState("Mushie") != State.RUNNING)
        	getAnimationManager().setState(State.RUNNING, "Mushie");
        
//        animationManager.update(delta);
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            TextureRegion currentFrame = new TextureRegion(getAnimationManager().getMushieCurrentFrame());
            currentFrame.flip(isFlipped, false);
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    public void dispose() {
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
    
    public boolean emptyBody(Body body) {
    	if (texture == null)
    		return false;
    	else
    		return true;
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
