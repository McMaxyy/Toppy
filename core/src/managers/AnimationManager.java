package managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import config.Storage;

public class AnimationManager {
//	private Array<Animation<TextureRegion>> idleAnimation = new Array<>();
//	private Array<Animation<TextureRegion>> runningAnimation = new Array<>();
//	private Array<Animation<TextureRegion>> attackingAnimation = new Array<>();
//	private Array<Animation<TextureRegion>> dyingAnimation = new Array<>();

	private Animation<TextureRegion> playerIdleAnimation;
    private Animation<TextureRegion> playerRunningAnimation;
    
//    private float animationTime = 0f;
    private float playerAnimationTime = 0f;
    
    public enum State {
        IDLE, RUNNING, ATTACKING, DYING
    }
    
//    private State currentState = State.IDLE;
    private State playerCurrentState = State.IDLE;
    
    public AnimationManager() {
    	loadPlayerAnimations();
    }

	private void loadPlayerAnimations() {
//		Array<TextureRegion> up = new Array<>();
//	    Array<TextureRegion> down = new Array<>();
//	    Array<TextureRegion> right = new Array<>();
//	    Array<TextureRegion> left = new Array<>();
//		
//		Texture RunningTex = Storage.assetManager.get("character/Running.png", Texture.class);
//		RunningTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
//        TextureRegion[][] RunningFrames = TextureRegion.split(RunningTex, RunningTex.getWidth() / 8, RunningTex.getHeight() / 4);
//        
//        for (int i = 0; i < 8; i++) {
//        	down.add(RunningFrames[0][i]);
//        	up.add(RunningFrames[1][i]);
//        	left.add(RunningFrames[2][i]);
//        	right.add(RunningFrames[3][i]);
//        }
//        runningAnimation.add(new Animation<>(0.1f, down, Animation.PlayMode.LOOP));  // Index 0
//        runningAnimation.add(new Animation<>(0.1f, up, Animation.PlayMode.LOOP));    // Index 1
//        runningAnimation.add(new Animation<>(0.1f, left, Animation.PlayMode.LOOP));  // Index 2
//        runningAnimation.add(new Animation<>(0.1f, right, Animation.PlayMode.LOOP)); // Index 3
//        
//        up.clear();
//        down.clear();
//        left.clear();
//        right.clear();
//        
//        // Load Dying Animation
//        Texture DyingTex = Storage.assetManager.get("character/Dying.png", Texture.class);
//        DyingTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        TextureRegion[][] DyingFrames = TextureRegion.split(DyingTex, DyingTex.getWidth() / 6, DyingTex.getHeight() / 4);
//
//        for (int i = 0; i < 6; i++) {
//            down.add(DyingFrames[0][i]); 
//            up.add(DyingFrames[1][i]);    
//            left.add(DyingFrames[2][i]);  
//            right.add(DyingFrames[3][i]); 
//        }
//
//        dyingAnimation.add(new Animation<>(0.05f, down, Animation.PlayMode.NORMAL));
//        dyingAnimation.add(new Animation<>(0.05f, up, Animation.PlayMode.NORMAL));
//        dyingAnimation.add(new Animation<>(0.05f, left, Animation.PlayMode.NORMAL));
//        dyingAnimation.add(new Animation<>(0.05f, right, Animation.PlayMode.NORMAL));
//
//        up.clear();
//        down.clear();
//        left.clear();
//        right.clear();
//
//        // Load Attacking Animation
//        Texture AttackingTex = Storage.assetManager.get("character/Attacking.png", Texture.class);
//        AttackingTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        TextureRegion[][] AttackFrames = TextureRegion.split(AttackingTex, AttackingTex.getWidth() / 5, AttackingTex.getHeight() / 4);
//
//        for (int i = 0; i < 5; i++) {
//            down.add(AttackFrames[0][i]);  
//            up.add(AttackFrames[1][i]);    
//            left.add(AttackFrames[2][i]);  
//            right.add(AttackFrames[3][i]); 
//        }
//
//        attackingAnimation.add(new Animation<>(0.1f, down, Animation.PlayMode.NORMAL));
//        attackingAnimation.add(new Animation<>(0.1f, up, Animation.PlayMode.NORMAL));
//        attackingAnimation.add(new Animation<>(0.1f, left, Animation.PlayMode.NORMAL));
//        attackingAnimation.add(new Animation<>(0.1f, right, Animation.PlayMode.NORMAL));
//
//        up.clear();
//        down.clear();
//        left.clear();
//        right.clear();
//
//        // Load Idle Animation
//        Texture IdleTex = Storage.assetManager.get("character/Idle.png", Texture.class);
//        IdleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        TextureRegion[][] IdleFrames = TextureRegion.split(IdleTex, IdleTex.getWidth() / 4, IdleTex.getHeight() / 4);
//
//        for (int i = 0; i < 4; i++) {
//            down.add(IdleFrames[0][i]);  
//            up.add(IdleFrames[1][i]);    
//            left.add(IdleFrames[2][i]);  
//            right.add(IdleFrames[3][i]); 
//        }
//
//        idleAnimation.add(new Animation<>(0.2f, down, Animation.PlayMode.LOOP));
//        idleAnimation.add(new Animation<>(0.2f, up, Animation.PlayMode.LOOP));
//        idleAnimation.add(new Animation<>(0.2f, left, Animation.PlayMode.LOOP));
//        idleAnimation.add(new Animation<>(0.2f, right, Animation.PlayMode.LOOP));
//
//        up.clear();
//        down.clear();
//        left.clear();
//        right.clear();	
        
		Texture playerWalkingTexture = Storage.assetManager.get("character/Walking.png", Texture.class);
	    playerWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] playerWalkFrames = TextureRegion.split(playerWalkingTexture, playerWalkingTexture.getWidth() / 4, playerWalkingTexture.getHeight());
	    Array<TextureRegion> playerWalkingFrames = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        playerWalkingFrames.add(playerWalkFrames[0][i]);
	    }
	    playerRunningAnimation = new Animation<>(0.5f, playerWalkingFrames, Animation.PlayMode.LOOP);

	    Texture playerIdleTexture = Storage.assetManager.get("character/Idle2.png", Texture.class);
	    playerIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] playerIdleFrames = TextureRegion.split(playerIdleTexture, playerIdleTexture.getWidth() / 4, playerIdleTexture.getHeight());
	    Array<TextureRegion> playerIdleFrame = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        playerIdleFrame.add(playerIdleFrames[0][i]);
	    }
	    playerIdleAnimation = new Animation<>(1.3f, playerIdleFrame, Animation.PlayMode.LOOP); 
	}
	
	public void update(float delta) {
//		animationTime += delta;
		playerAnimationTime += delta;
	}

	public void setState(State newState) {
		if(newState != playerCurrentState){
			playerCurrentState = newState;
			playerAnimationTime = 0f;
		}
	}
	
	public State getState() {
		return playerCurrentState;
	}
	
	public TextureRegion getCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		
//		switch (currentState) {
//        case DYING:
//            currentAnimation = dyingAnimation.get(direction);
//            break;
//        case ATTACKING:
//            currentAnimation = attackingAnimation.get(direction);
//            break;
//        case RUNNING:
//            currentAnimation = runningAnimation.get(direction);
//            break;
//        case IDLE:
//        default:
//            currentAnimation = idleAnimation.get(direction);
//            break;
//		}
		
		switch (playerCurrentState) {
        case RUNNING:
            currentAnimation = playerRunningAnimation;
            break;
        case IDLE:
        default:
            currentAnimation = playerIdleAnimation;
            break;
    }
		
        TextureRegion currentFrame = currentAnimation.getKeyFrame(playerAnimationTime);
        
        return currentFrame;
	}
	
	public boolean isAnimationFinished() {
//		switch (currentState) {
//        case ATTACKING:
//            return attackingAnimation.get(direction).isAnimationFinished(animationTime);
//        case RUNNING:
//            return runningAnimation.get(direction).isAnimationFinished(animationTime);
//        case DYING:
//            return dyingAnimation.get(direction).isAnimationFinished(animationTime);
//        case IDLE:
//        default:
//            return idleAnimation.get(direction).isAnimationFinished(animationTime);
//		}
		
		switch (playerCurrentState) {
        case RUNNING:
            return playerRunningAnimation.isAnimationFinished(playerAnimationTime);
        case IDLE:
        default:
            return playerIdleAnimation.isAnimationFinished(playerAnimationTime);
		}
	}
}
