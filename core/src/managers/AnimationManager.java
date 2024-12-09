package managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import config.Storage;

public class AnimationManager {
	private Animation<TextureRegion>[] idleAnimation = new Animation[4];;
    private Animation<TextureRegion>[] runningAnimation = new Animation[4];
    private Animation<TextureRegion>[] attackingAnimation = new Animation[4];;
    private Animation<TextureRegion>[] dyingAnimation = new Animation[4];;
    
    private float animationTime = 0f;
    
    public enum State {
        IDLE, RUNNING, ATTACKING, DYING
    }
    
    private State currentState = State.IDLE;
    
    public AnimationManager() {
    	loadPlayerAnimations();
    }

	private void loadPlayerAnimations() {
		Array<TextureRegion> up = new Array<>();
	    Array<TextureRegion> down = new Array<>();
	    Array<TextureRegion> right = new Array<>();
	    Array<TextureRegion> left = new Array<>();
		
		Texture RunningTex = Storage.assetManager.get("character/Running.png", Texture.class);
		RunningTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);           
        TextureRegion[][] RunningFrames = TextureRegion.split(RunningTex, RunningTex.getWidth() / 8, RunningTex.getHeight() / 4);
        
        for (int i = 0; i < 8; i++) {
        	down.add(RunningFrames[0][i]);
        	up.add(RunningFrames[1][i]);
        	left.add(RunningFrames[2][i]);
        	right.add(RunningFrames[3][i]);
        }
        runningAnimation[0] = new Animation<>(0.1f, down, Animation.PlayMode.LOOP);
        runningAnimation[1] = new Animation<>(0.1f, up, Animation.PlayMode.LOOP);
        runningAnimation[2] = new Animation<>(0.1f, left, Animation.PlayMode.LOOP);
        runningAnimation[3] = new Animation<>(0.1f, right, Animation.PlayMode.LOOP);
        
        up.clear();
        down.clear();
        left.clear();
        right.clear();
        
        // Load Dying Animation
        Texture DyingTex = Storage.assetManager.get("character/Dying.png", Texture.class);
        DyingTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion[][] DyingFrames = TextureRegion.split(DyingTex, DyingTex.getWidth() / 6, DyingTex.getHeight() / 4);

        for (int i = 0; i < 6; i++) {
            down.add(DyingFrames[0][i]); 
            up.add(DyingFrames[1][i]);    
            left.add(DyingFrames[2][i]);  
            right.add(DyingFrames[3][i]); 
        }

        dyingAnimation[0] = new Animation<>(0.05f, down, Animation.PlayMode.NORMAL);
        dyingAnimation[1] = new Animation<>(0.05f, up, Animation.PlayMode.NORMAL);
        dyingAnimation[2] = new Animation<>(0.05f, left, Animation.PlayMode.NORMAL);
        dyingAnimation[3] = new Animation<>(0.05f, right, Animation.PlayMode.NORMAL);

        up.clear();
        down.clear();
        left.clear();
        right.clear();

        // Load Attacking Animation
        Texture AttackingTex = Storage.assetManager.get("character/Attacking.png", Texture.class);
        AttackingTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion[][] AttackFrames = TextureRegion.split(AttackingTex, AttackingTex.getWidth() / 5, AttackingTex.getHeight() / 4);

        for (int i = 0; i < 5; i++) {
            down.add(AttackFrames[0][i]);  
            up.add(AttackFrames[1][i]);    
            left.add(AttackFrames[2][i]);  
            right.add(AttackFrames[3][i]); 
        }

        attackingAnimation[0] = new Animation<>(0.1f, down, Animation.PlayMode.NORMAL);
        attackingAnimation[1] = new Animation<>(0.1f, up, Animation.PlayMode.NORMAL);
        attackingAnimation[2] = new Animation<>(0.1f, left, Animation.PlayMode.NORMAL);
        attackingAnimation[3] = new Animation<>(0.1f, right, Animation.PlayMode.NORMAL);

        up.clear();
        down.clear();
        left.clear();
        right.clear();

        // Load Idle Animation
        Texture IdleTex = Storage.assetManager.get("character/Idle.png", Texture.class);
        IdleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion[][] IdleFrames = TextureRegion.split(IdleTex, IdleTex.getWidth() / 4, IdleTex.getHeight() / 4);

        for (int i = 0; i < 4; i++) {
            down.add(IdleFrames[0][i]);  
            up.add(IdleFrames[1][i]);    
            left.add(IdleFrames[2][i]);  
            right.add(IdleFrames[3][i]); 
        }

        idleAnimation[0] = new Animation<>(0.2f, down, Animation.PlayMode.LOOP);
        idleAnimation[1] = new Animation<>(0.2f, up, Animation.PlayMode.LOOP);
        idleAnimation[2] = new Animation<>(0.2f, left, Animation.PlayMode.LOOP);
        idleAnimation[3] = new Animation<>(0.2f, right, Animation.PlayMode.LOOP);

        up.clear();
        down.clear();
        left.clear();
        right.clear();	
	}
	
	public void update(float delta) {
		animationTime += delta;
	}

	public void setState(State newState) {
		if(newState != currentState){
			currentState = newState;
			animationTime = 0f;
		}
	}
	
	public State getState() {
		return currentState;
	}
	
	public TextureRegion getCurrentFrame(int direction) {
		Animation<TextureRegion> currentAnimation;
		
		switch(currentState) {
		case DYING:
    		currentAnimation = dyingAnimation[direction];
    		break;
        case ATTACKING:
            currentAnimation = attackingAnimation[direction];
            break;
        case RUNNING:
            currentAnimation = runningAnimation[direction];
            break;
        case IDLE:
        default:
            currentAnimation = idleAnimation[direction];
            break;
		}
		
        TextureRegion currentFrame = currentAnimation.getKeyFrame(animationTime);
        
        return currentFrame;
	}
	
	public boolean isAnimationFinished(int direction) {
		switch(currentState) {
		case ATTACKING:
			return attackingAnimation[direction].isAnimationFinished(animationTime);
		case RUNNING:
			return runningAnimation[direction].isAnimationFinished(animationTime);
		case DYING:
			return dyingAnimation[direction].isAnimationFinished(animationTime);
		case IDLE:
		default:
			return idleAnimation[direction].isAnimationFinished(animationTime);
		}
	}
}
