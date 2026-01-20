package managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import config.Storage;
import entities.EnemyType;

public class AnimationManager {
	private Animation<TextureRegion> playerIdleAnimation;
	private Animation<TextureRegion> playerRunningAnimation;
	private Animation<TextureRegion> playerDyingAnimation;
	private Animation<TextureRegion> mushieIdleAnimation;
	private Animation<TextureRegion> mushieRunningAnimation;
	private Animation<TextureRegion> mushieAttackingAnimation;
	private Animation<TextureRegion> bossKittyRunningAnimation;
	private Animation<TextureRegion> bossKittyDyingAnimation;
	private Animation<TextureRegion> skeletonIdleAnimation;
	private Animation<TextureRegion> skeletonRunningAnimation;
	private Animation<TextureRegion> skeletonAttackingAnimation;
	private Animation<TextureRegion> wolfieIdleAnimation;
	private Animation<TextureRegion> wolfieRunningAnimation;
	private Animation<TextureRegion> wolfieAttackingAnimation;

	private float playerAnimationTime = 0f;
	private float mushieAnimationTime = 0f;
	private float bossKittyAnimationTime = 0f;
	private float skeletonAnimationTime = 0f;
	private float wolfieAnimationTime = 0f;

	public enum State {
		IDLE, RUNNING, ATTACKING, DYING
	}

	private State playerCurrentState = State.IDLE;
	private State mushieCurrentState = State.IDLE;
	private State skeletonCurrentState = State.IDLE;
	private State wolfieCurrentState = State.IDLE;
	private State bossKittyCurrentState = State.RUNNING;

	public AnimationManager() {
		loadAnimations();
	}

	private void loadAnimations() {
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

		Texture mushieAttackingTexture = Storage.assetManager.get("enemies/Mushie/Attacking.png", Texture.class);
		mushieAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] mushieAttackingFrames = TextureRegion.split(mushieAttackingTexture, mushieAttackingTexture.getWidth() / 4, mushieAttackingTexture.getHeight());
		Array<TextureRegion> mushieAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			mushieAttackingFrame.add(mushieAttackingFrames[0][i]);
		}
		mushieAttackingAnimation = new Animation<>(0.25f, mushieAttackingFrame, Animation.PlayMode.NORMAL);

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

		Texture skeletonWalkingTexture = Storage.assetManager.get("enemies/Skeleton/Walking.png", Texture.class);
		skeletonWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonWalkFrames = TextureRegion.split(skeletonWalkingTexture, skeletonWalkingTexture.getWidth() / 4, skeletonWalkingTexture.getHeight());
		Array<TextureRegion> skeletonWalkFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			skeletonWalkFrame.add(skeletonWalkFrames[0][i]);
		}
		skeletonRunningAnimation = new Animation<>(0.4f, skeletonWalkFrame, Animation.PlayMode.LOOP);

		Texture skeletonIdleTexture = Storage.assetManager.get("enemies/Skeleton/Idle.png", Texture.class);
		skeletonIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonIdleFrames = TextureRegion.split(skeletonIdleTexture, skeletonIdleTexture.getWidth() / 4, skeletonIdleTexture.getHeight());
		Array<TextureRegion> skeletonIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			skeletonIdleFrame.add(skeletonIdleFrames[0][i]);
		}
		skeletonIdleAnimation = new Animation<>(0.4f, skeletonIdleFrame, Animation.PlayMode.LOOP);

		Texture skeletonAttackingTexture = Storage.assetManager.get("enemies/Skeleton/Attacking.png", Texture.class);
		skeletonAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonAttackingFrames = TextureRegion.split(skeletonAttackingTexture, skeletonAttackingTexture.getWidth() / 4, skeletonAttackingTexture.getHeight());
		Array<TextureRegion> skeletonAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			skeletonAttackingFrame.add(skeletonAttackingFrames[0][i]);
		}
		skeletonAttackingAnimation = new Animation<>(0.2f, skeletonAttackingFrame, Animation.PlayMode.NORMAL);

		Texture wolfieWalkingTexture = Storage.assetManager.get("enemies/Wolfie/Walking.png", Texture.class);
		wolfieWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] wolfieWalkFrames = TextureRegion.split(wolfieWalkingTexture, wolfieWalkingTexture.getWidth() / 4, wolfieWalkingTexture.getHeight());
		Array<TextureRegion> wolfieWalkingFrames = new Array<>();
		for (int i = 0; i < 4; i++) {
			wolfieWalkingFrames.add(wolfieWalkFrames[0][i]);
		}
		wolfieRunningAnimation = new Animation<>(0.25f, wolfieWalkingFrames, Animation.PlayMode.LOOP);

		Texture wolfieIdleTexture = Storage.assetManager.get("enemies/Wolfie/Idle.png", Texture.class);
		wolfieIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] wolfieIdleFrames = TextureRegion.split(wolfieIdleTexture, wolfieIdleTexture.getWidth() / 4, wolfieIdleTexture.getHeight());
		Array<TextureRegion> wolfieIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			wolfieIdleFrame.add(wolfieIdleFrames[0][i]);
		}
		wolfieIdleAnimation = new Animation<>(0.4f, wolfieIdleFrame, Animation.PlayMode.LOOP);

		Texture wolfieAttackingTexture = Storage.assetManager.get("enemies/Wolfie/Attacking.png", Texture.class);
		wolfieAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] wolfieAttackingFrames = TextureRegion.split(wolfieAttackingTexture, wolfieAttackingTexture.getWidth() / 4, wolfieAttackingTexture.getHeight());
		Array<TextureRegion> wolfieAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			wolfieAttackingFrame.add(wolfieAttackingFrames[0][i]);
		}
		wolfieAttackingAnimation = new Animation<>(0.25f, wolfieAttackingFrame, Animation.PlayMode.NORMAL);
	}

	public void update(float delta) {
		playerAnimationTime += delta;
		mushieAnimationTime += delta;
		bossKittyAnimationTime += delta;
		wolfieAnimationTime += delta;
		skeletonAnimationTime += delta;
	}

	/**
	 * Get the Animation object for a specific enemy type and state.
	 * This allows each enemy instance to track its own animation time.
	 */
	public Animation<TextureRegion> getAnimationForState(EnemyType enemyType, State state) {
		switch (enemyType) {
			case MUSHIE:
				switch (state) {
					case RUNNING:
						return mushieRunningAnimation;
					case ATTACKING:
						return mushieAttackingAnimation;
					case IDLE:
					default:
						return mushieIdleAnimation;
				}
			case WOLFIE:
				switch (state) {
					case RUNNING:
						return wolfieRunningAnimation;
					case ATTACKING:
						return wolfieAttackingAnimation;
					case IDLE:
					default:
						return wolfieIdleAnimation;
				}
			case SKELETON:
				switch (state) {
					case RUNNING:
						return skeletonRunningAnimation;
					case ATTACKING:
						return skeletonAttackingAnimation;
					case IDLE:
					default:
						return skeletonIdleAnimation;
				}
			case BOSS_KITTY:
				switch (state) {
					case DYING:
						return bossKittyDyingAnimation;
					case RUNNING:
					default:
						return bossKittyRunningAnimation;
				}
			default:
				return mushieIdleAnimation;
		}
	}

	public void setState(State newState, String entity) {
		switch (entity) {
			case "Player":
				if (playerCurrentState != newState) {
					playerCurrentState = newState;
					playerAnimationTime = 0f;
				}
				break;
			case "Mushie":
				if (mushieCurrentState != newState) {
					mushieCurrentState = newState;
					mushieAnimationTime = 0f;
				}
				break;
			case "BossKitty":
				if (bossKittyCurrentState != newState) {
					bossKittyCurrentState = newState;
					bossKittyAnimationTime = 0f;
				}
				break;
			case "Skeleton":
				if (skeletonCurrentState != newState) {
					skeletonCurrentState = newState;
					skeletonAnimationTime = 0f;
				}
				break;
			case "Wolfie":
				if (wolfieCurrentState != newState) {
					wolfieCurrentState = newState;
					wolfieAnimationTime = 0f;
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
			case "Skeleton":
				return skeletonCurrentState;
			case "Wolfie":
				return wolfieCurrentState;
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
			case ATTACKING:
				currentAnimation = mushieAttackingAnimation;
				break;
			case IDLE:
			default:
				currentAnimation = mushieIdleAnimation;
				break;
		}

		TextureRegion currentFrame = currentAnimation.getKeyFrame(mushieAnimationTime);

		return currentFrame;
	}

	public boolean isMushieAnimationFinished() {
		switch (mushieCurrentState) {
			case RUNNING:
				return mushieRunningAnimation.isAnimationFinished(mushieAnimationTime);
			case ATTACKING:
				return mushieAttackingAnimation.isAnimationFinished(mushieAnimationTime);
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

	public TextureRegion getSkeletonCurrentFrame() {
		Animation<TextureRegion> currentAnimation;

		switch (skeletonCurrentState) {
			case RUNNING:
				currentAnimation = skeletonRunningAnimation;
				break;
			case ATTACKING:
				currentAnimation = skeletonAttackingAnimation;
				break;
			case IDLE:
			default:
				currentAnimation = skeletonIdleAnimation;
				break;
		}

		TextureRegion currentFrame = currentAnimation.getKeyFrame(skeletonAnimationTime);

		return currentFrame;
	}

	public boolean isSkeletonAnimationFinished() {
		switch (skeletonCurrentState) {
			case RUNNING:
				return skeletonRunningAnimation.isAnimationFinished(skeletonAnimationTime);
			case ATTACKING:
				return skeletonAttackingAnimation.isAnimationFinished(skeletonAnimationTime);
			case IDLE:
			default:
				return skeletonIdleAnimation.isAnimationFinished(skeletonAnimationTime);
		}
	}

	public TextureRegion getWolfieCurrentFrame() {
		Animation<TextureRegion> currentAnimation;

		switch (wolfieCurrentState) {
			case RUNNING:
				currentAnimation = wolfieRunningAnimation;
				break;
			case ATTACKING:
				currentAnimation = wolfieAttackingAnimation;
				break;
			case IDLE:
			default:
				currentAnimation = wolfieIdleAnimation;
				break;
		}

		TextureRegion currentFrame = currentAnimation.getKeyFrame(wolfieAnimationTime);

		return currentFrame;
	}

	public boolean isWolfieAnimationFinished() {
		switch (wolfieCurrentState) {
			case RUNNING:
				return wolfieRunningAnimation.isAnimationFinished(wolfieAnimationTime);
			case ATTACKING:
				return wolfieAttackingAnimation.isAnimationFinished(wolfieAnimationTime);
			case IDLE:
			default:
				return wolfieIdleAnimation.isAnimationFinished(wolfieAnimationTime);
		}
	}
}