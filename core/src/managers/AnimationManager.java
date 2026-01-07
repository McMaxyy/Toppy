package managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import config.Storage;

public class AnimationManager {
	private Animation<TextureRegion> playerIdleAnimation;
    private Animation<TextureRegion> playerRunningAnimation;
    private Animation<TextureRegion> playerDyingAnimation;
    private Animation<TextureRegion> mushieIdleAnimation;
    private Animation<TextureRegion> mushieRunningAnimation;
    private Animation<TextureRegion> mushieDyingAnimation;
    private Animation<TextureRegion> bossKittyRunningAnimation;
    private Animation<TextureRegion> bossKittyDyingAnimation;
    
    private float playerAnimationTime = 0f;
    private float mushieAnimationTime = 0f;
    private float bossKittyAnimationTime = 0f;
    
    public enum State {
        IDLE, RUNNING, ATTACKING, DYING
    }
    
    private State playerCurrentState = State.IDLE;
    private State mushieCurrentState = State.IDLE;
    private State bossKittyCurrentState = State.RUNNING;
    
    public AnimationManager() {
    	loadAnimations();
    }

	private void loadAnimations() {
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
	    
	    Texture playerDyingTexture = Storage.assetManager.get("character/Dying.png", Texture.class);
	    playerDyingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] playerDyingFrames = TextureRegion.split(playerDyingTexture, playerDyingTexture.getWidth() / 4, playerDyingTexture.getHeight());
	    Array<TextureRegion> playerDyingFrame = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        playerDyingFrame.add(playerDyingFrames[0][i]);
	    }
	    playerDyingAnimation = new Animation<>(0.6f, playerDyingFrame, Animation.PlayMode.NORMAL);
	    
	    Texture mushieWalkingTexture = Storage.assetManager.get("enemies/Mushie/Walking.png", Texture.class);
	    mushieWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] mushieWalkFrames = TextureRegion.split(mushieWalkingTexture, mushieWalkingTexture.getWidth() / 4, mushieWalkingTexture.getHeight());
	    Array<TextureRegion> mushieWalkingFrames = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        mushieWalkingFrames.add(mushieWalkFrames[0][i]);
	    }
	    mushieRunningAnimation = new Animation<>(0.4f, mushieWalkingFrames, Animation.PlayMode.LOOP);

	    Texture mushieIdleTexture = Storage.assetManager.get("enemies/Mushie/Idle.png", Texture.class);
	    mushieIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] mushieIdleFrames = TextureRegion.split(mushieIdleTexture, mushieIdleTexture.getWidth() / 4, mushieIdleTexture.getHeight());
	    Array<TextureRegion> mushieIdleFrame = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        mushieIdleFrame.add(mushieIdleFrames[0][i]);
	    }
	    mushieIdleAnimation = new Animation<>(0.4f, mushieIdleFrame, Animation.PlayMode.LOOP); 
	    
	    Texture mushieDyingTexture = Storage.assetManager.get("enemies/Mushie/Dying.png", Texture.class);
	    mushieDyingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] mushieDyingFrames = TextureRegion.split(mushieDyingTexture, mushieDyingTexture.getWidth() / 4, mushieDyingTexture.getHeight());
	    Array<TextureRegion> mushieDyingFrame = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        mushieDyingFrame.add(mushieDyingFrames[0][i]);
	    }
	    mushieDyingAnimation = new Animation<>(1f, mushieDyingFrame, Animation.PlayMode.NORMAL); 
	
	    Texture bossKittyWalkingTexture = Storage.assetManager.get("enemies/BossKitty/Walking.png", Texture.class);
	    bossKittyWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] bossKittyWalkFrames = TextureRegion.split(bossKittyWalkingTexture, bossKittyWalkingTexture.getWidth() / 4, bossKittyWalkingTexture.getHeight());
	    Array<TextureRegion> bossKittyWalkingFrames = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        bossKittyWalkingFrames.add(bossKittyWalkFrames[0][i]);
	    }
	    bossKittyRunningAnimation = new Animation<>(0.5f, bossKittyWalkingFrames, Animation.PlayMode.LOOP);

	    Texture bossKittyDyingTexture = Storage.assetManager.get("enemies/BossKitty/Dying.png", Texture.class);
	    bossKittyDyingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
	    TextureRegion[][] bossKittyDyingFrames = TextureRegion.split(bossKittyDyingTexture, bossKittyDyingTexture.getWidth() / 4, bossKittyDyingTexture.getHeight());
	    Array<TextureRegion> bossKittyDyingFrame = new Array<>();
	    for (int i = 0; i < 4; i++) {
	        bossKittyDyingFrame.add(bossKittyDyingFrames[0][i]);
	    }
	    bossKittyDyingAnimation = new Animation<>(1f, bossKittyDyingFrame, Animation.PlayMode.NORMAL); 
	}
	
	public void update(float delta) {		
		playerAnimationTime += delta;
		mushieAnimationTime += delta;
		bossKittyAnimationTime += delta;
	}

	public void setState(State newState, String entity) {
		switch (entity) {
		case "Player":
			if(newState != playerCurrentState){
				playerCurrentState = newState;
				playerAnimationTime = 0f;
			}
			break;
		case "Mushie":
			if(newState != mushieCurrentState){
				mushieCurrentState = newState;
				mushieAnimationTime = 0f;
			}
			break;
		case "BossKitty":
			if(newState != bossKittyCurrentState){
				bossKittyCurrentState = newState;
				bossKittyAnimationTime = 0f;
			}
			break;
		}
		
	}
	
	public State getState(String entity) {
		switch(entity) {
		case "Player":
			return playerCurrentState;
		case "Mushie":
			return mushieCurrentState;
		case "BossKitty":
			return bossKittyCurrentState;
		default:
			return null;
		}		
	}
	
	public TextureRegion getCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		
		switch (playerCurrentState) {
        case RUNNING:
            currentAnimation = playerRunningAnimation;
            break;
        case DYING:
            currentAnimation = playerDyingAnimation;
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
		switch (playerCurrentState) {
        case RUNNING:
            return playerRunningAnimation.isAnimationFinished(playerAnimationTime);
        case DYING:
            return playerDyingAnimation.isAnimationFinished(playerAnimationTime);
        case IDLE:
        default:
            return playerIdleAnimation.isAnimationFinished(playerAnimationTime);
		}
	}
	
	public TextureRegion getMushieCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		
		switch (mushieCurrentState) {
        case RUNNING:
            currentAnimation = mushieRunningAnimation;
            break;
        case DYING:
            currentAnimation = mushieDyingAnimation;
            break;
        case IDLE:
        default:
            currentAnimation = mushieIdleAnimation;
            break;
    }
		
        TextureRegion currentFrame = currentAnimation.getKeyFrame(playerAnimationTime);
        
        return currentFrame;
	}
	
	public boolean isMushieAnimationFinished() {		
		switch (mushieCurrentState) {
        case RUNNING:
            return mushieRunningAnimation.isAnimationFinished(mushieAnimationTime);
        case DYING:
            return mushieDyingAnimation.isAnimationFinished(mushieAnimationTime);
        case IDLE:
        default:
            return mushieIdleAnimation.isAnimationFinished(mushieAnimationTime);
		}
	}
	
	public TextureRegion getBossKittyCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		
		switch (bossKittyCurrentState) {
        case DYING:
            currentAnimation = bossKittyDyingAnimation;
            break;
        case RUNNING:
        default:
            currentAnimation = bossKittyRunningAnimation;
            break;
    }
		
        TextureRegion currentFrame = currentAnimation.getKeyFrame(bossKittyAnimationTime);
        
        return currentFrame;
	}
	
	public boolean isBossKittyAnimationFinished() {		
		switch (bossKittyCurrentState) {
        case DYING:
            return bossKittyDyingAnimation.isAnimationFinished(bossKittyAnimationTime);
        case RUNNING:
        default:
            return bossKittyRunningAnimation.isAnimationFinished(bossKittyAnimationTime);
		}
	}
}
