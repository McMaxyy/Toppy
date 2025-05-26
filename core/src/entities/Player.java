package entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;

import config.GameScreen;
import config.Storage;
import game.GameProj;
import managers.AnimationManager;
import managers.Box2DWorld;
import managers.CollisionFilter;
import managers.ProjectileManager;

public class Player {
    private final Body body;
    private Array<Body> spearBodies = new Array<>();
    private Array<Vector2> spearVelocities = new Array<>();
    private Array<Vector2> spearStartPositions = new Array<>();
    private float speed, maxDistance = 80f;
    private final AnimationManager animationManager;
    private int direction;
    private final GameProj gameP;
    private final Box2DWorld world;
    private Array<Boolean> spearMarkedForRemoval = new Array<>();
    private float spearCooldown = 0f;
    private final Texture whitePixel;
    private boolean playerDeath;
    private final GameScreen gameScreen;
    public static boolean gameStarted = false;
    private float dashCooldown = 1f;
    private float dashTimer = 0f;
    private float dashDuration = 0.5f;
    private float dashCooldownTimer = 0f;
    private boolean isDashing = false; 
    private Vector2 dashDirection = new Vector2();
    private boolean invulnerable;
    private final short originalMaskBits;
    private final ProjectileManager projectileManager;
    
    private class Trail {
        Vector2 position;
        float lifetime;

        public Trail(Vector2 position, float lifetime) {
            this.position = new Vector2(position);
            this.lifetime = lifetime;
        }
    }

    private Array<Trail> trails = new Array<>();
    private float trailSpawnInterval = 0.05f;
    private float trailTimer = 0f;
    private float trailLifetime = 0.5f;

    public Player(Box2DWorld world, AnimationManager animationManager, int size, GameProj gameP, GameScreen gameScreen) {
        this.animationManager = animationManager;
        this.speed = 5000f;
        this.gameP = gameP;
        this.world = world;
    	this.gameScreen = gameScreen;
        whitePixel = Storage.assetManager.get("tiles/green_tile.png", Texture.class);

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
        fixtureDef.filter.maskBits = CollisionFilter.OBSTACLE | CollisionFilter.ENEMY;
        originalMaskBits = fixtureDef.filter.maskBits;

        body.createFixture(fixtureDef);
        shape.dispose();

        projectileManager = new ProjectileManager(this.gameP, this.body, this.world, this);
    }
    
    public void update(float delta) {
    	if (spearCooldown > 0) {
            spearCooldown -= delta;
        }
    	
    	input(delta);
        updateAnimationState();
        getAnimationManager().update(delta);
        
        for (int i = spearBodies.size - 1; i >= 0; i--) {
            Body spearBody = spearBodies.get(i);
            Vector2 startPosition = spearStartPositions.get(i);
            
            if (spearMarkedForRemoval.get(i) || spearBody == null || 
        		spearBody.getPosition().dst(startPosition) >= maxDistance) {
                	removeSpear(spearBody, i);
            }
        }
        
        if(playerDeath && !world.getWorld().isLocked())
        	die();
        
        for (int i = trails.size - 1; i >= 0; i--) {
            Trail trail = trails.get(i);
            trail.lifetime -= delta;
            if (trail.lifetime <= 0) {
                trails.removeIndex(i);
            }
        }
    }
    
    private void updateMaskBits() {
        if (body != null && body.getFixtureList().size > 0) {
            Fixture fixture = body.getFixtureList().first();

            Filter filter = fixture.getFilterData();

            filter.maskBits = invulnerable ? (short) 0 : originalMaskBits;

            fixture.setFilterData(filter);
        }
    }

    public Vector2 getPosition() {
    	if(body != null)
    		return body.getPosition();
    	else
    		return null;
    }
    
    public void updateBounds() {}
    
    public void input(float delta) {
        float moveX = 0;
        float moveY = 0;

        if (!isDashing) {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;

            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldownTimer <= 0) {
            	setInvulnerable(true);
            	isDashing = true;
                dashDuration = 0.5f;
                dashTimer = dashDuration;
                dashDirection.set(moveX, moveY).nor();
                dashCooldownTimer = dashCooldown;
            }

            move(moveX, moveY, delta);
        } else {
        	if (isDashing) {
                float dashSpeed = 3000f;
                body.setLinearVelocity(dashDirection.x * dashSpeed * 25, dashDirection.y * dashSpeed * 25);
        	    dashTimer -= delta;

        	    trailTimer += delta;
        	    if (trailTimer >= trailSpawnInterval) {
        	        trails.add(new Trail(body.getPosition(), trailLifetime));
        	        trailTimer = 0f;
        	    }

        	    if (dashTimer <= 0) {
        	        isDashing = false;
        	        body.setLinearVelocity(0, 0);
        	        setInvulnerable(false);
    	        }
        	}
        }
        
        if(Gdx.input.isButtonPressed(Input.Buttons.LEFT) && spearCooldown <= 0) {
            projectileManager.createSpear();
        	spearCooldown = 0.5f;
        }  

        if (dashCooldownTimer > 0) {
            dashCooldownTimer -= delta;
        }

        updateBounds();
        gameP.generateChunksAroundPlayer();
    }
    
    private void updateAnimationState() {
        Vector3 mousePosition3D = gameP.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector2 mousePosition = new Vector2(mousePosition3D.x, mousePosition3D.y);
        Vector2 playerPosition = body.getPosition();

        float angle = (float) Math.toDegrees(Math.atan2(mousePosition.y - playerPosition.y, mousePosition.x - playerPosition.x));
        if (angle < 0) angle += 360;

        if (angle >= 45 && angle < 135) {
            direction = 1;
        } else if (angle >= 135 && angle < 225) {
            direction = 2;
        } else if (angle >= 225 && angle < 315) {
            direction = 0;
        } else {
            direction = 3;
        }
    }

    public void move(float dx, float dy, float delta) {    	
        float magnitude = (float) Math.sqrt(dx * dx + dy * dy);

        if (magnitude > 0) {
            dx /= magnitude;
            dy /= magnitude;

            if (Math.abs(dx) > Math.abs(dy)) {
                direction = dx > 0 ? 3 : 2;
            } else {
                direction = dy > 0 ? 1 : 0;
            }

            getAnimationManager().setState(AnimationManager.State.RUNNING, "Player");
            
            if(!gameStarted)
            	gameStarted = true;
        } else {
        	getAnimationManager().setState(AnimationManager.State.IDLE, "Player");
        }

        float velocityX = dx * speed * delta;
        float velocityY = dy * speed * delta;

        body.setLinearVelocity(velocityX, velocityY);
    }

    public void addSpearBodies(Body spearBody, Vector2 velocity) {
        spearBodies.add(spearBody);
        spearVelocities.add(velocity);
        spearStartPositions.add(new Vector2(spearBody.getPosition()));
        spearMarkedForRemoval.add(false);
    }

    public void markSpearForRemoval(Body spearBody) {
        for (int i = 0; i < spearBodies.size; i++) {
            if (spearBodies.get(i) == spearBody) {
                spearMarkedForRemoval.set(i, true);
                break;
            }
        }
    }
    
    public void die() {
    	gameStarted = false;
    	playerDeath = false;
    	gameScreen.switchToNewState(GameScreen.HOME);
    }
    
    public void playerDie() {
    	playerDeath = true;
    }
    
    public void removeSpear(Body spearBody, int i) {
    	if (spearBody != null) {
            world.getWorld().destroyBody(spearBody);
        }
    	
        spearBodies.removeIndex(i);
        spearVelocities.removeIndex(i);
        spearStartPositions.removeIndex(i); 
        spearMarkedForRemoval.removeIndex(i);
    }

    public void render(SpriteBatch batch, int TILE_SIZE) {
        Vector2 position = body.getPosition();

        getAnimationManager().update(Gdx.graphics.getDeltaTime());
        batch.draw(getAnimationManager().getCurrentFrame(),
                   position.x - TILE_SIZE / 4f, 
                   position.y - TILE_SIZE / 4f, 
                   TILE_SIZE / 2f, TILE_SIZE / 2f);

        for (int i = 0; i < spearBodies.size; i++) {
            Body spearBody = spearBodies.get(i);

            if (spearBody != null) {
                Texture spearTexture = Storage.assetManager.get("character/Spear.png");
                TextureRegion spearRegion = new TextureRegion(spearTexture);

                float rotationAngle = (float) Math.toDegrees(spearBody.getAngle());
                float posX = spearBody.getPosition().x - spearTexture.getWidth() / 8f;
                float posY = spearBody.getPosition().y - spearTexture.getHeight() / 8f;

                batch.draw(spearRegion,
                        posX, posY,
                        spearTexture.getWidth() / 8f, spearTexture.getHeight() / 8f,
                        spearTexture.getWidth() / 4f, spearTexture.getHeight() / 4f,
                        1, 1,
                        rotationAngle - 45);
            }
        }
        
        renderCooldownBar(batch, TILE_SIZE);
        
        for (Trail trail : trails) {
            float alpha = trail.lifetime / trailLifetime;
            batch.setColor(1, 1, 1, alpha);
            batch.draw(getAnimationManager().getCurrentFrame(),
                       trail.position.x - TILE_SIZE / 4f,
                       trail.position.y - TILE_SIZE / 4f,
                       TILE_SIZE / 2f, TILE_SIZE / 2f);
        }
        batch.setColor(1, 1, 1, 1);
    }
    
    private void renderCooldownBar(SpriteBatch batch, float TILE_SIZE) {
        if (spearCooldown > 0) {
            float barWidth = TILE_SIZE / 2f;
            float barHeight = 2f;
            float cooldownPercentage = spearCooldown / 0.5f;
            float filledWidth = barWidth * (1 - cooldownPercentage);

            Vector2 position = body.getPosition();
            float barX = position.x - TILE_SIZE / 4f;
            float barY = position.y + TILE_SIZE / 4f;

            batch.setColor(1f, 0.4f, 0.7f, 1f);
            batch.draw(whitePixel, barX, barY, barWidth, barHeight);

            batch.setColor(0.1f, 0.1f, 0.1f, 1f);
            batch.draw(whitePixel, barX, barY, filledWidth, barHeight);

            batch.setColor(1, 1, 1, 1);
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
    
    public Body getBody() {
    	return body;
    }

	public boolean isInvulnerable() {
		return invulnerable;
	}
	
	public void setInvulnerable(boolean invulnerable) {
	    this.invulnerable = invulnerable;
	    updateMaskBits();
	}

}
