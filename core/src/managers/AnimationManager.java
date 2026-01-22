package managers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import config.Storage;
import entities.EnemyType;
import entities.PlayerClass;

public class AnimationManager {
	// Mercenary (default) animations
	private Animation<TextureRegion> playerIdleAnimation;
	private Animation<TextureRegion> playerRunningAnimation;
	private Animation<TextureRegion> playerDyingAnimation;

	// Paladin animations
	private Animation<TextureRegion> paladinIdleAnimation;
	private Animation<TextureRegion> paladinRunningAnimation;
	private Animation<TextureRegion> paladinDyingAnimation;

	// Enemy animations
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
	private Animation<TextureRegion> cyclopsIdleAnimation;
	private Animation<TextureRegion> cyclopsRunningAnimation;
	private Animation<TextureRegion> cyclopsAttackingAnimation;

	private float playerAnimationTime = 0f;
	private float mushieAnimationTime = 0f;
	private float bossKittyAnimationTime = 0f;
	private float skeletonAnimationTime = 0f;
	private float wolfieAnimationTime = 0f;
	private float cyclopsAnimationTime = 0f;

	private PlayerClass currentPlayerClass = PlayerClass.MERCENARY;

	public enum State {
		IDLE, RUNNING, ATTACKING, DYING
	}

	private State playerCurrentState = State.IDLE;
	private State mushieCurrentState = State.IDLE;
	private State skeletonCurrentState = State.IDLE;
	private State wolfieCurrentState = State.IDLE;
	private State bossKittyCurrentState = State.RUNNING;
	private State cyclopsCurrentState = State.IDLE;

	public AnimationManager() {
		loadAnimations();
	}

	public void setPlayerClass(PlayerClass playerClass) {
		this.currentPlayerClass = playerClass;
	}

	public PlayerClass getPlayerClass() {
		return currentPlayerClass;
	}

	private void loadAnimations() {
		loadMercenaryAnimations();
		loadPaladinAnimations();
		loadEnemyAnimations();
	}

	private void loadMercenaryAnimations() {
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
	}

	private void loadPaladinAnimations() {
		try {
			Texture paladinWalkingTexture = Storage.assetManager.get("character/Paladin/Walking.png", Texture.class);
			paladinWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] paladinWalkFrames = TextureRegion.split(paladinWalkingTexture, paladinWalkingTexture.getWidth() / 4, paladinWalkingTexture.getHeight());
			Array<TextureRegion> paladinWalkingFrames = new Array<>();
			for (int i = 0; i < 4; i++) {
				paladinWalkingFrames.add(paladinWalkFrames[0][i]);
			}
			paladinRunningAnimation = new Animation<>(0.5f, paladinWalkingFrames, Animation.PlayMode.LOOP);

			Texture paladinIdleTexture = Storage.assetManager.get("character/Paladin/Idle.png", Texture.class);
			paladinIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] paladinIdleFrames = TextureRegion.split(paladinIdleTexture, paladinIdleTexture.getWidth() / 4, paladinIdleTexture.getHeight());
			Array<TextureRegion> paladinIdleFrame = new Array<>();
			for (int i = 0; i < 4; i++) {
				paladinIdleFrame.add(paladinIdleFrames[0][i]);
			}
			paladinIdleAnimation = new Animation<>(1.3f, paladinIdleFrame, Animation.PlayMode.LOOP);

			Texture paladinDyingTexture = Storage.assetManager.get("character/Paladin/Dying.png", Texture.class);
			paladinDyingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] paladinDyingFrames = TextureRegion.split(paladinDyingTexture, paladinDyingTexture.getWidth() / 4, paladinDyingTexture.getHeight());
			Array<TextureRegion> paladinDyingFrame = new Array<>();
			for (int i = 0; i < 4; i++) {
				paladinDyingFrame.add(paladinDyingFrames[0][i]);
			}
			paladinDyingAnimation = new Animation<>(0.6f, paladinDyingFrame, Animation.PlayMode.NORMAL);
		} catch (Exception e) {
			System.err.println("Failed to load Paladin animations, using Mercenary as fallback: " + e.getMessage());
			paladinIdleAnimation = playerIdleAnimation;
			paladinRunningAnimation = playerRunningAnimation;
			paladinDyingAnimation = playerDyingAnimation;
		}
	}

	private void loadEnemyAnimations() {
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

		Texture cyclopsWalkingTexture = Storage.assetManager.get("enemies/Cyclops/Walking.png", Texture.class);
		cyclopsWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] cyclopsWalkFrames = TextureRegion.split(cyclopsWalkingTexture, cyclopsWalkingTexture.getWidth() / 4, cyclopsWalkingTexture.getHeight());
		Array<TextureRegion> cyclopsWalkingFrames = new Array<>();
		for (int i = 0; i < 4; i++) {
			cyclopsWalkingFrames.add(cyclopsWalkFrames[0][i]);
		}
		cyclopsRunningAnimation = new Animation<>(0.5f, cyclopsWalkingFrames, Animation.PlayMode.LOOP);

		Texture cyclopsIdleTexture = Storage.assetManager.get("enemies/Cyclops/Idle.png", Texture.class);
		cyclopsIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] cyclopsIdleFrames = TextureRegion.split(cyclopsIdleTexture, cyclopsIdleTexture.getWidth() / 4, cyclopsIdleTexture.getHeight());
		Array<TextureRegion> cyclopsIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			cyclopsIdleFrame.add(cyclopsIdleFrames[0][i]);
		}
		cyclopsIdleAnimation = new Animation<>(0.5f, cyclopsIdleFrame, Animation.PlayMode.LOOP);

		Texture cyclopsAttackingTexture = Storage.assetManager.get("enemies/Cyclops/Attacking.png", Texture.class);
		cyclopsAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] cyclopsAttackingFrames = TextureRegion.split(cyclopsAttackingTexture, cyclopsAttackingTexture.getWidth() / 4, cyclopsAttackingTexture.getHeight());
		Array<TextureRegion> cyclopsAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) {
			cyclopsAttackingFrame.add(cyclopsAttackingFrames[0][i]);
		}
		cyclopsAttackingAnimation = new Animation<>(0.2f, cyclopsAttackingFrame, Animation.PlayMode.NORMAL);
	}

	public void update(float delta) {
		playerAnimationTime += delta;
		mushieAnimationTime += delta;
		bossKittyAnimationTime += delta;
		wolfieAnimationTime += delta;
		skeletonAnimationTime += delta;
		cyclopsAnimationTime += delta;
	}

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
			case CYCLOPS:
				switch (state) {
					case RUNNING:
						return cyclopsRunningAnimation;
					case ATTACKING:
						return cyclopsAttackingAnimation;
					case IDLE:
					default:
						return cyclopsIdleAnimation;
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
			case "Cyclops":
				if (cyclopsCurrentState != newState) {
					cyclopsCurrentState = newState;
					cyclopsAnimationTime = 0f;
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
			case "Cyclops":
				return cyclopsCurrentState;
			default:
				return null;
		}
	}

	public TextureRegion getCurrentFrame() {
		Animation<TextureRegion> currentAnimation;

		// Select animation based on player class
		if (currentPlayerClass == PlayerClass.PALADIN) {
			switch (playerCurrentState) {
				case RUNNING:
					currentAnimation = paladinRunningAnimation;
					break;
				case DYING:
					currentAnimation = paladinDyingAnimation;
					break;
				case IDLE:
				default:
					currentAnimation = paladinIdleAnimation;
					break;
			}
		} else {
			// Default to Mercenary
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
		}

		return currentAnimation.getKeyFrame(playerAnimationTime);
	}

	public boolean isAnimationFinished() {
		Animation<TextureRegion> currentAnimation;

		if (currentPlayerClass == PlayerClass.PALADIN) {
			switch (playerCurrentState) {
				case RUNNING:
					currentAnimation = paladinRunningAnimation;
					break;
				case DYING:
					currentAnimation = paladinDyingAnimation;
					break;
				case IDLE:
				default:
					currentAnimation = paladinIdleAnimation;
					break;
			}
		} else {
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
		}

		return currentAnimation.isAnimationFinished(playerAnimationTime);
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
		return currentAnimation.getKeyFrame(mushieAnimationTime);
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
		return currentAnimation.getKeyFrame(bossKittyAnimationTime);
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
		return currentAnimation.getKeyFrame(skeletonAnimationTime);
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
		return currentAnimation.getKeyFrame(wolfieAnimationTime);
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

	public TextureRegion getCyclopsCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (cyclopsCurrentState) {
			case RUNNING:
				currentAnimation = cyclopsRunningAnimation;
				break;
			case ATTACKING:
				currentAnimation = cyclopsAttackingAnimation;
				break;
			case IDLE:
			default:
				currentAnimation = cyclopsIdleAnimation;
				break;
		}
		return currentAnimation.getKeyFrame(cyclopsAnimationTime);
	}

	public boolean isCyclopsAnimationFinished() {
		switch (cyclopsCurrentState) {
			case RUNNING:
				return cyclopsRunningAnimation.isAnimationFinished(cyclopsAnimationTime);
			case ATTACKING:
				return cyclopsAttackingAnimation.isAnimationFinished(cyclopsAnimationTime);
			case IDLE:
			default:
				return cyclopsIdleAnimation.isAnimationFinished(cyclopsAnimationTime);
		}
	}
}