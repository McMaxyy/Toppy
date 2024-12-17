package entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class Enemy {
	public Rectangle bounds;
    private Texture texture;
    private Body body;
    private Player player;
    private final float detectionRadius = 150f;
    private final float speed = 60f; 
    private boolean markForRemoval = false;
    
    public Enemy(Rectangle bounds, Texture texture, Body body, Player player) {
        this.bounds = bounds;
        this.texture = texture;
        this.body = body;
        this.player = player;
    }

    public void update() {   	
    	 if (markForRemoval) {
             dispose();
             return;
         }
    	     	 
    	 if (!Player.gameStarted) {
             body.setLinearVelocity(0, 0); 
             return;
         }
    	
        bounds.setPosition(body.getPosition().x - bounds.width / 2f, 
                           body.getPosition().y - bounds.height / 2f);
        
        if (isPlayerInRadius()) {
            moveTowardsPlayer();
        }
    }

    public void render(SpriteBatch batch) {
    	if (!markForRemoval) {
            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    public void dispose() {
    	if (body != null && markForRemoval) {
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
}
