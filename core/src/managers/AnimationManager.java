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
	private Animation<TextureRegion> merchantIdleAnimation;
	private Animation<TextureRegion> skeletonRogueIdleAnimation;
	private Animation<TextureRegion> skeletonRogueRunningAnimation;
	private Animation<TextureRegion> skeletonRogueAttackingAnimation;
	private Animation<TextureRegion> skeletonMageIdleAnimation;
	private Animation<TextureRegion> skeletonMageRunningAnimation;
	private Animation<TextureRegion> skeletonMageAttackingAnimation;

	// Ghost animations
	private Animation<TextureRegion> ghostIdleAnimation;
	private Animation<TextureRegion> ghostRunningAnimation;
	private Animation<TextureRegion> ghostAttackingAnimation;

	// GhostBoss animations
	private Animation<TextureRegion> ghostBossIdleAnimation;
	private Animation<TextureRegion> ghostBossRunningAnimation;
	private Animation<TextureRegion> ghostBossAttackingAnimation;

	// Herman animations
	private Animation<TextureRegion> hermanIdleAnimation;
	private Animation<TextureRegion> hermanAttackingAnimation;
	private Animation<TextureRegion> hermanSpecialAttackAnimation;

	private float playerAnimationTime = 0f;
	private float mushieAnimationTime = 0f;
	private float bossKittyAnimationTime = 0f;
	private float skeletonAnimationTime = 0f;
	private float wolfieAnimationTime = 0f;
	private float cyclopsAnimationTime = 0f;
	private float merchantAnimationTime = 0f;
	private float skeletonRogueAnimationTime = 0f;
	private float skeletonMageAnimationTime = 0f;
	private float ghostAnimationTime = 0f;
	private float ghostBossAnimationTime = 0f;
	private float hermanAnimationTime = 0f;

	private PlayerClass currentPlayerClass = PlayerClass.MERCENARY;

	public enum State {
		IDLE, RUNNING, ATTACKING, DYING, SPECIAL_ATTACK
	}

	private State playerCurrentState = State.IDLE;
	private State mushieCurrentState = State.IDLE;
	private State skeletonCurrentState = State.IDLE;
	private State wolfieCurrentState = State.IDLE;
	private State bossKittyCurrentState = State.RUNNING;
	private State cyclopsCurrentState = State.IDLE;
	private State merchantCurrentState = State.IDLE;
	private State skeletonRogueCurrentState = State.IDLE;
	private State skeletonMageCurrentState = State.IDLE;
	private State ghostCurrentState = State.IDLE;
	private State ghostBossCurrentState = State.IDLE;
	private State hermanCurrentState = State.IDLE;

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
		playerIdleAnimation = new Animation<>(0.4f, playerIdleFrame, Animation.PlayMode.LOOP);

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
			paladinIdleAnimation = new Animation<>(0.4f, paladinIdleFrame, Animation.PlayMode.LOOP);

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
		loadMushieAnimations();
		loadBossKittyAnimations();
		loadSkeletonAnimations();
		loadWolfieAnimations();
		loadCyclopsAnimations();
		loadMerchantAnimations();
		loadSkeletonRogueAnimations();
		loadSkeletonMageAnimations();
		loadGhostAnimations();
		loadGhostBossAnimations();
		loadHermanAnimations();
	}

	private void loadMushieAnimations() {
		Texture mushieWalkingTexture = Storage.assetManager.get("enemies/Mushie/Walking.png", Texture.class);
		mushieWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] mushieWalkFrames = TextureRegion.split(mushieWalkingTexture, mushieWalkingTexture.getWidth() / 4, mushieWalkingTexture.getHeight());
		Array<TextureRegion> mushieWalkingFrames = new Array<>();
		for (int i = 0; i < 4; i++) mushieWalkingFrames.add(mushieWalkFrames[0][i]);
		mushieRunningAnimation = new Animation<>(0.4f, mushieWalkingFrames, Animation.PlayMode.LOOP);

		Texture mushieIdleTexture = Storage.assetManager.get("enemies/Mushie/Idle.png", Texture.class);
		mushieIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] mushieIdleFrames = TextureRegion.split(mushieIdleTexture, mushieIdleTexture.getWidth() / 4, mushieIdleTexture.getHeight());
		Array<TextureRegion> mushieIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) mushieIdleFrame.add(mushieIdleFrames[0][i]);
		mushieIdleAnimation = new Animation<>(0.4f, mushieIdleFrame, Animation.PlayMode.LOOP);

		Texture mushieAttackingTexture = Storage.assetManager.get("enemies/Mushie/Attacking.png", Texture.class);
		mushieAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] mushieAttackingFrames = TextureRegion.split(mushieAttackingTexture, mushieAttackingTexture.getWidth() / 4, mushieAttackingTexture.getHeight());
		Array<TextureRegion> mushieAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) mushieAttackingFrame.add(mushieAttackingFrames[0][i]);
		mushieAttackingAnimation = new Animation<>(0.25f, mushieAttackingFrame, Animation.PlayMode.NORMAL);
	}

	private void loadBossKittyAnimations() {
		Texture bossKittyWalkingTexture = Storage.assetManager.get("enemies/BossKitty/Walking.png", Texture.class);
		bossKittyWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] bossKittyWalkFrames = TextureRegion.split(bossKittyWalkingTexture, bossKittyWalkingTexture.getWidth() / 4, bossKittyWalkingTexture.getHeight());
		Array<TextureRegion> bossKittyWalkingFrames = new Array<>();
		for (int i = 0; i < 4; i++) bossKittyWalkingFrames.add(bossKittyWalkFrames[0][i]);
		bossKittyRunningAnimation = new Animation<>(0.5f, bossKittyWalkingFrames, Animation.PlayMode.LOOP);

		Texture bossKittyDyingTexture = Storage.assetManager.get("enemies/BossKitty/Dying.png", Texture.class);
		bossKittyDyingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] bossKittyDyingFrames = TextureRegion.split(bossKittyDyingTexture, bossKittyDyingTexture.getWidth() / 4, bossKittyDyingTexture.getHeight());
		Array<TextureRegion> bossKittyDyingFrame = new Array<>();
		for (int i = 0; i < 4; i++) bossKittyDyingFrame.add(bossKittyDyingFrames[0][i]);
		bossKittyDyingAnimation = new Animation<>(1f, bossKittyDyingFrame, Animation.PlayMode.NORMAL);
	}

	private void loadSkeletonAnimations() {
		Texture skeletonWalkingTexture = Storage.assetManager.get("enemies/Skeleton/Walking.png", Texture.class);
		skeletonWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonWalkFrames = TextureRegion.split(skeletonWalkingTexture, skeletonWalkingTexture.getWidth() / 4, skeletonWalkingTexture.getHeight());
		Array<TextureRegion> skeletonWalkFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonWalkFrame.add(skeletonWalkFrames[0][i]);
		skeletonRunningAnimation = new Animation<>(0.4f, skeletonWalkFrame, Animation.PlayMode.LOOP);

		Texture skeletonIdleTexture = Storage.assetManager.get("enemies/Skeleton/Idle.png", Texture.class);
		skeletonIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonIdleFrames = TextureRegion.split(skeletonIdleTexture, skeletonIdleTexture.getWidth() / 4, skeletonIdleTexture.getHeight());
		Array<TextureRegion> skeletonIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonIdleFrame.add(skeletonIdleFrames[0][i]);
		skeletonIdleAnimation = new Animation<>(0.4f, skeletonIdleFrame, Animation.PlayMode.LOOP);

		Texture skeletonAttackingTexture = Storage.assetManager.get("enemies/Skeleton/Attacking.png", Texture.class);
		skeletonAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonAttackingFrames = TextureRegion.split(skeletonAttackingTexture, skeletonAttackingTexture.getWidth() / 4, skeletonAttackingTexture.getHeight());
		Array<TextureRegion> skeletonAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonAttackingFrame.add(skeletonAttackingFrames[0][i]);
		skeletonAttackingAnimation = new Animation<>(0.2f, skeletonAttackingFrame, Animation.PlayMode.NORMAL);
	}

	private void loadWolfieAnimations() {
		Texture wolfieWalkingTexture = Storage.assetManager.get("enemies/Wolfie/Walking.png", Texture.class);
		wolfieWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] wolfieWalkFrames = TextureRegion.split(wolfieWalkingTexture, wolfieWalkingTexture.getWidth() / 4, wolfieWalkingTexture.getHeight());
		Array<TextureRegion> wolfieWalkingFrames = new Array<>();
		for (int i = 0; i < 4; i++) wolfieWalkingFrames.add(wolfieWalkFrames[0][i]);
		wolfieRunningAnimation = new Animation<>(0.25f, wolfieWalkingFrames, Animation.PlayMode.LOOP);

		Texture wolfieIdleTexture = Storage.assetManager.get("enemies/Wolfie/Idle.png", Texture.class);
		wolfieIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] wolfieIdleFrames = TextureRegion.split(wolfieIdleTexture, wolfieIdleTexture.getWidth() / 4, wolfieIdleTexture.getHeight());
		Array<TextureRegion> wolfieIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) wolfieIdleFrame.add(wolfieIdleFrames[0][i]);
		wolfieIdleAnimation = new Animation<>(0.4f, wolfieIdleFrame, Animation.PlayMode.LOOP);

		Texture wolfieAttackingTexture = Storage.assetManager.get("enemies/Wolfie/Attacking.png", Texture.class);
		wolfieAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] wolfieAttackingFrames = TextureRegion.split(wolfieAttackingTexture, wolfieAttackingTexture.getWidth() / 4, wolfieAttackingTexture.getHeight());
		Array<TextureRegion> wolfieAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) wolfieAttackingFrame.add(wolfieAttackingFrames[0][i]);
		wolfieAttackingAnimation = new Animation<>(0.25f, wolfieAttackingFrame, Animation.PlayMode.NORMAL);
	}

	private void loadCyclopsAnimations() {
		Texture cyclopsWalkingTexture = Storage.assetManager.get("enemies/Cyclops/Walking.png", Texture.class);
		cyclopsWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] cyclopsWalkFrames = TextureRegion.split(cyclopsWalkingTexture, cyclopsWalkingTexture.getWidth() / 4, cyclopsWalkingTexture.getHeight());
		Array<TextureRegion> cyclopsWalkingFrames = new Array<>();
		for (int i = 0; i < 4; i++) cyclopsWalkingFrames.add(cyclopsWalkFrames[0][i]);
		cyclopsRunningAnimation = new Animation<>(0.5f, cyclopsWalkingFrames, Animation.PlayMode.LOOP);

		Texture cyclopsIdleTexture = Storage.assetManager.get("enemies/Cyclops/Idle.png", Texture.class);
		cyclopsIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] cyclopsIdleFrames = TextureRegion.split(cyclopsIdleTexture, cyclopsIdleTexture.getWidth() / 4, cyclopsIdleTexture.getHeight());
		Array<TextureRegion> cyclopsIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) cyclopsIdleFrame.add(cyclopsIdleFrames[0][i]);
		cyclopsIdleAnimation = new Animation<>(0.5f, cyclopsIdleFrame, Animation.PlayMode.LOOP);

		Texture cyclopsAttackingTexture = Storage.assetManager.get("enemies/Cyclops/Attacking.png", Texture.class);
		cyclopsAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] cyclopsAttackingFrames = TextureRegion.split(cyclopsAttackingTexture, cyclopsAttackingTexture.getWidth() / 4, cyclopsAttackingTexture.getHeight());
		Array<TextureRegion> cyclopsAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) cyclopsAttackingFrame.add(cyclopsAttackingFrames[0][i]);
		cyclopsAttackingAnimation = new Animation<>(0.2f, cyclopsAttackingFrame, Animation.PlayMode.NORMAL);
	}

	private void loadMerchantAnimations() {
		Texture merchantIdleTexture = Storage.assetManager.get("enemies/Merchant/Idle.png", Texture.class);
		merchantIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] merchantIdleFrames = TextureRegion.split(merchantIdleTexture, merchantIdleTexture.getWidth() / 4, merchantIdleTexture.getHeight());
		Array<TextureRegion> merchantIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) merchantIdleFrame.add(merchantIdleFrames[0][i]);
		merchantIdleAnimation = new Animation<>(0.4f, merchantIdleFrame, Animation.PlayMode.LOOP);
	}

	private void loadSkeletonRogueAnimations() {
		Texture skeletonRogueWalkingTexture = Storage.assetManager.get("enemies/SkeletonRogue/Walking.png", Texture.class);
		skeletonRogueWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonRogueWalkFrames = TextureRegion.split(skeletonRogueWalkingTexture, skeletonRogueWalkingTexture.getWidth() / 4, skeletonRogueWalkingTexture.getHeight());
		Array<TextureRegion> skeletonRogueWalkFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonRogueWalkFrame.add(skeletonRogueWalkFrames[0][i]);
		skeletonRogueRunningAnimation = new Animation<>(0.3f, skeletonRogueWalkFrame, Animation.PlayMode.LOOP);

		Texture skeletonRogueIdleTexture = Storage.assetManager.get("enemies/SkeletonRogue/Idle.png", Texture.class);
		skeletonRogueIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonRogueIdleFrames = TextureRegion.split(skeletonRogueIdleTexture, skeletonRogueIdleTexture.getWidth() / 4, skeletonRogueIdleTexture.getHeight());
		Array<TextureRegion> skeletonRogueIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonRogueIdleFrame.add(skeletonRogueIdleFrames[0][i]);
		skeletonRogueIdleAnimation = new Animation<>(0.4f, skeletonRogueIdleFrame, Animation.PlayMode.LOOP);

		Texture skeletonRogueAttackingTexture = Storage.assetManager.get("enemies/SkeletonRogue/Attacking.png", Texture.class);
		skeletonRogueAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonRogueAttackingFrames = TextureRegion.split(skeletonRogueAttackingTexture, skeletonRogueAttackingTexture.getWidth() / 4, skeletonRogueAttackingTexture.getHeight());
		Array<TextureRegion> skeletonRogueAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonRogueAttackingFrame.add(skeletonRogueAttackingFrames[0][i]);
		skeletonRogueAttackingAnimation = new Animation<>(0.15f, skeletonRogueAttackingFrame, Animation.PlayMode.NORMAL);
	}

	private void loadSkeletonMageAnimations() {
		Texture skeletonMageWalkingTexture = Storage.assetManager.get("enemies/SkeletonMage/Walking.png", Texture.class);
		skeletonMageWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonMageWalkFrames = TextureRegion.split(skeletonMageWalkingTexture, skeletonMageWalkingTexture.getWidth() / 4, skeletonMageWalkingTexture.getHeight());
		Array<TextureRegion> skeletonMageWalkFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonMageWalkFrame.add(skeletonMageWalkFrames[0][i]);
		skeletonMageRunningAnimation = new Animation<>(0.4f, skeletonMageWalkFrame, Animation.PlayMode.LOOP);

		Texture skeletonMageIdleTexture = Storage.assetManager.get("enemies/SkeletonMage/Idle.png", Texture.class);
		skeletonMageIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonMageIdleFrames = TextureRegion.split(skeletonMageIdleTexture, skeletonMageIdleTexture.getWidth() / 4, skeletonMageIdleTexture.getHeight());
		Array<TextureRegion> skeletonMageIdleFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonMageIdleFrame.add(skeletonMageIdleFrames[0][i]);
		skeletonMageIdleAnimation = new Animation<>(0.4f, skeletonMageIdleFrame, Animation.PlayMode.LOOP);

		Texture skeletonMageAttackingTexture = Storage.assetManager.get("enemies/SkeletonMage/Attacking.png", Texture.class);
		skeletonMageAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		TextureRegion[][] skeletonMageAttackingFrames = TextureRegion.split(skeletonMageAttackingTexture, skeletonMageAttackingTexture.getWidth() / 4, skeletonMageAttackingTexture.getHeight());
		Array<TextureRegion> skeletonMageAttackingFrame = new Array<>();
		for (int i = 0; i < 4; i++) skeletonMageAttackingFrame.add(skeletonMageAttackingFrames[0][i]);
		skeletonMageAttackingAnimation = new Animation<>(0.3f, skeletonMageAttackingFrame, Animation.PlayMode.NORMAL);
	}

	private void loadGhostAnimations() {
		try {
			Texture ghostWalkingTexture = Storage.assetManager.get("enemies/Ghost/Walking.png", Texture.class);
			ghostWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] ghostWalkFrames = TextureRegion.split(ghostWalkingTexture, ghostWalkingTexture.getWidth() / 4, ghostWalkingTexture.getHeight());
			Array<TextureRegion> ghostWalkingFrames = new Array<>();
			for (int i = 0; i < 4; i++) ghostWalkingFrames.add(ghostWalkFrames[0][i]);
			ghostRunningAnimation = new Animation<>(0.3f, ghostWalkingFrames, Animation.PlayMode.LOOP);

			Texture ghostIdleTexture = Storage.assetManager.get("enemies/Ghost/Idle.png", Texture.class);
			ghostIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] ghostIdleFrames = TextureRegion.split(ghostIdleTexture, ghostIdleTexture.getWidth() / 4, ghostIdleTexture.getHeight());
			Array<TextureRegion> ghostIdleFrame = new Array<>();
			for (int i = 0; i < 4; i++) ghostIdleFrame.add(ghostIdleFrames[0][i]);
			ghostIdleAnimation = new Animation<>(0.4f, ghostIdleFrame, Animation.PlayMode.LOOP);

			Texture ghostAttackingTexture = Storage.assetManager.get("enemies/Ghost/Attacking.png", Texture.class);
			ghostAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] ghostAttackingFrames = TextureRegion.split(ghostAttackingTexture, ghostAttackingTexture.getWidth() / 4, ghostAttackingTexture.getHeight());
			Array<TextureRegion> ghostAttackingFrame = new Array<>();
			for (int i = 0; i < 4; i++) ghostAttackingFrame.add(ghostAttackingFrames[0][i]);
			ghostAttackingAnimation = new Animation<>(0.25f, ghostAttackingFrame, Animation.PlayMode.NORMAL);
		} catch (Exception e) {
			System.err.println("Failed to load Ghost animations, using Mushie as fallback: " + e.getMessage());
			ghostIdleAnimation = mushieIdleAnimation;
			ghostRunningAnimation = mushieRunningAnimation;
			ghostAttackingAnimation = mushieAttackingAnimation;
		}
	}

	private void loadGhostBossAnimations() {
		try {
			Texture ghostBossWalkingTexture = Storage.assetManager.get("enemies/GhostBoss/Walking.png", Texture.class);
			ghostBossWalkingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] ghostBossWalkFrames = TextureRegion.split(ghostBossWalkingTexture, ghostBossWalkingTexture.getWidth() / 4, ghostBossWalkingTexture.getHeight());
			Array<TextureRegion> ghostBossWalkingFrames = new Array<>();
			for (int i = 0; i < 4; i++) ghostBossWalkingFrames.add(ghostBossWalkFrames[0][i]);
			ghostBossRunningAnimation = new Animation<>(0.4f, ghostBossWalkingFrames, Animation.PlayMode.LOOP);

			Texture ghostBossIdleTexture = Storage.assetManager.get("enemies/GhostBoss/Idle.png", Texture.class);
			ghostBossIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] ghostBossIdleFrames = TextureRegion.split(ghostBossIdleTexture, ghostBossIdleTexture.getWidth() / 4, ghostBossIdleTexture.getHeight());
			Array<TextureRegion> ghostBossIdleFrame = new Array<>();
			for (int i = 0; i < 4; i++) ghostBossIdleFrame.add(ghostBossIdleFrames[0][i]);
			ghostBossIdleAnimation = new Animation<>(0.4f, ghostBossIdleFrame, Animation.PlayMode.LOOP);

			Texture ghostBossAttackingTexture = Storage.assetManager.get("enemies/GhostBoss/Attacking.png", Texture.class);
			ghostBossAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] ghostBossAttackingFrames = TextureRegion.split(ghostBossAttackingTexture, ghostBossAttackingTexture.getWidth() / 4, ghostBossAttackingTexture.getHeight());
			Array<TextureRegion> ghostBossAttackingFrame = new Array<>();
			for (int i = 0; i < 4; i++) ghostBossAttackingFrame.add(ghostBossAttackingFrames[0][i]);
			ghostBossAttackingAnimation = new Animation<>(0.3f, ghostBossAttackingFrame, Animation.PlayMode.NORMAL);
		} catch (Exception e) {
			System.err.println("Failed to load GhostBoss animations, using BossKitty as fallback: " + e.getMessage());
			ghostBossIdleAnimation = bossKittyRunningAnimation;
			ghostBossRunningAnimation = bossKittyRunningAnimation;
			ghostBossAttackingAnimation = bossKittyDyingAnimation;
		}
	}

	private void loadHermanAnimations() {
		try {
			Texture hermanIdleTexture = Storage.assetManager.get("enemies/Herman/Idle.png", Texture.class);
			hermanIdleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] hermanIdleFrames = TextureRegion.split(hermanIdleTexture, hermanIdleTexture.getWidth() / 4, hermanIdleTexture.getHeight());
			Array<TextureRegion> hermanIdleFrame = new Array<>();
			for (int i = 0; i < 4; i++) hermanIdleFrame.add(hermanIdleFrames[0][i]);
			hermanIdleAnimation = new Animation<>(0.5f, hermanIdleFrame, Animation.PlayMode.LOOP);

			Texture hermanAttackingTexture = Storage.assetManager.get("enemies/Herman/Attacking.png", Texture.class);
			hermanAttackingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] hermanAttackingFrames = TextureRegion.split(hermanAttackingTexture, hermanAttackingTexture.getWidth() / 4, hermanAttackingTexture.getHeight());
			Array<TextureRegion> hermanAttackingFrame = new Array<>();
			for (int i = 0; i < 4; i++) hermanAttackingFrame.add(hermanAttackingFrames[0][i]);
			hermanAttackingAnimation = new Animation<>(0.25f, hermanAttackingFrame, Animation.PlayMode.NORMAL);

			Texture hermanSpecialTexture = Storage.assetManager.get("enemies/Herman/SpecialAttack.png", Texture.class);
			hermanSpecialTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
			TextureRegion[][] hermanSpecialFrames = TextureRegion.split(hermanSpecialTexture, hermanSpecialTexture.getWidth() / 4, hermanSpecialTexture.getHeight());
			Array<TextureRegion> hermanSpecialFrame = new Array<>();
			for (int i = 0; i < 4; i++) hermanSpecialFrame.add(hermanSpecialFrames[0][i]);
			hermanSpecialAttackAnimation = new Animation<>(0.3f, hermanSpecialFrame, Animation.PlayMode.NORMAL);
		} catch (Exception e) {
			System.err.println("Failed to load Herman animations, using Cyclops as fallback: " + e.getMessage());
			hermanIdleAnimation = cyclopsIdleAnimation;
			hermanAttackingAnimation = cyclopsAttackingAnimation;
			hermanSpecialAttackAnimation = cyclopsAttackingAnimation;
		}
	}

	public void update(float delta) {
		playerAnimationTime += delta;
		mushieAnimationTime += delta;
		bossKittyAnimationTime += delta;
		wolfieAnimationTime += delta;
		skeletonAnimationTime += delta;
		cyclopsAnimationTime += delta;
		merchantAnimationTime += delta;
		skeletonRogueAnimationTime += delta;
		skeletonMageAnimationTime += delta;
		ghostAnimationTime += delta;
		ghostBossAnimationTime += delta;
		hermanAnimationTime += delta;
	}

	public Animation<TextureRegion> getAnimationForState(EnemyType enemyType, State state) {
		switch (enemyType) {
			case MUSHIE:
				switch (state) {
					case RUNNING: return mushieRunningAnimation;
					case ATTACKING: return mushieAttackingAnimation;
					default: return mushieIdleAnimation;
				}
			case WOLFIE:
				switch (state) {
					case RUNNING: return wolfieRunningAnimation;
					case ATTACKING: return wolfieAttackingAnimation;
					default: return wolfieIdleAnimation;
				}
			case SKELETON:
				switch (state) {
					case RUNNING: return skeletonRunningAnimation;
					case ATTACKING: return skeletonAttackingAnimation;
					default: return skeletonIdleAnimation;
				}
			case BOSS_KITTY:
				switch (state) {
					case DYING: return bossKittyDyingAnimation;
					default: return bossKittyRunningAnimation;
				}
			case CYCLOPS:
				switch (state) {
					case RUNNING: return cyclopsRunningAnimation;
					case ATTACKING: return cyclopsAttackingAnimation;
					default: return cyclopsIdleAnimation;
				}
			case MERCHANT:
				return merchantIdleAnimation;
			case SKELETON_ROGUE:
				switch (state) {
					case RUNNING: return skeletonRogueRunningAnimation;
					case ATTACKING: return skeletonRogueAttackingAnimation;
					default: return skeletonRogueIdleAnimation;
				}
			case SKELETON_MAGE:
				switch (state) {
					case RUNNING: return skeletonMageRunningAnimation;
					case ATTACKING: return skeletonMageAttackingAnimation;
					default: return skeletonMageIdleAnimation;
				}
			case GHOST:
				switch (state) {
					case RUNNING: return ghostRunningAnimation;
					case ATTACKING: return ghostAttackingAnimation;
					default: return ghostIdleAnimation;
				}
			case GHOST_BOSS:
				switch (state) {
					case RUNNING: return ghostBossRunningAnimation;
					case ATTACKING: return ghostBossAttackingAnimation;
					default: return ghostBossIdleAnimation;
				}
			case HERMAN:
				switch (state) {
					case ATTACKING: return hermanAttackingAnimation;
					case SPECIAL_ATTACK: return hermanSpecialAttackAnimation;
					default: return hermanIdleAnimation;
				}
			default:
				return mushieIdleAnimation;
		}
	}

	public void setState(State newState, String entity) {
		switch (entity) {
			case "Player":
				if (playerCurrentState != newState) { playerCurrentState = newState; playerAnimationTime = 0f; }
				break;
			case "Mushie":
				if (mushieCurrentState != newState) { mushieCurrentState = newState; mushieAnimationTime = 0f; }
				break;
			case "BossKitty":
				if (bossKittyCurrentState != newState) { bossKittyCurrentState = newState; bossKittyAnimationTime = 0f; }
				break;
			case "Skeleton":
				if (skeletonCurrentState != newState) { skeletonCurrentState = newState; skeletonAnimationTime = 0f; }
				break;
			case "Wolfie":
				if (wolfieCurrentState != newState) { wolfieCurrentState = newState; wolfieAnimationTime = 0f; }
				break;
			case "Cyclops":
				if (cyclopsCurrentState != newState) { cyclopsCurrentState = newState; cyclopsAnimationTime = 0f; }
				break;
			case "Merchant":
				if (merchantCurrentState != newState) { merchantCurrentState = newState; merchantAnimationTime = 0f; }
				break;
			case "SkeletonRogue":
				if (skeletonRogueCurrentState != newState) { skeletonRogueCurrentState = newState; skeletonRogueAnimationTime = 0f; }
				break;
			case "SkeletonMage":
				if (skeletonMageCurrentState != newState) { skeletonMageCurrentState = newState; skeletonMageAnimationTime = 0f; }
				break;
			case "Ghost":
				if (ghostCurrentState != newState) { ghostCurrentState = newState; ghostAnimationTime = 0f; }
				break;
			case "GhostBoss":
				if (ghostBossCurrentState != newState) { ghostBossCurrentState = newState; ghostBossAnimationTime = 0f; }
				break;
			case "Herman":
				if (hermanCurrentState != newState) { hermanCurrentState = newState; hermanAnimationTime = 0f; }
				break;
		}
	}

	public State getState(String entity) {
		switch(entity) {
			case "Player": return playerCurrentState;
			case "Mushie": return mushieCurrentState;
			case "BossKitty": return bossKittyCurrentState;
			case "Skeleton": return skeletonCurrentState;
			case "Wolfie": return wolfieCurrentState;
			case "Cyclops": return cyclopsCurrentState;
			case "Merchant": return merchantCurrentState;
			case "SkeletonRogue": return skeletonRogueCurrentState;
			case "SkeletonMage": return skeletonMageCurrentState;
			case "Ghost": return ghostCurrentState;
			case "GhostBoss": return ghostBossCurrentState;
			case "Herman": return hermanCurrentState;
			default: return null;
		}
	}

	public TextureRegion getCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		if (currentPlayerClass == PlayerClass.PALADIN) {
			switch (playerCurrentState) {
				case RUNNING: currentAnimation = paladinRunningAnimation; break;
				case DYING: currentAnimation = paladinDyingAnimation; break;
				default: currentAnimation = paladinIdleAnimation; break;
			}
		} else {
			switch (playerCurrentState) {
				case RUNNING: currentAnimation = playerRunningAnimation; break;
				case DYING: currentAnimation = playerDyingAnimation; break;
				default: currentAnimation = playerIdleAnimation; break;
			}
		}
		return currentAnimation.getKeyFrame(playerAnimationTime);
	}

	public boolean isAnimationFinished() {
		Animation<TextureRegion> currentAnimation;
		if (currentPlayerClass == PlayerClass.PALADIN) {
			switch (playerCurrentState) {
				case RUNNING: currentAnimation = paladinRunningAnimation; break;
				case DYING: currentAnimation = paladinDyingAnimation; break;
				default: currentAnimation = paladinIdleAnimation; break;
			}
		} else {
			switch (playerCurrentState) {
				case RUNNING: currentAnimation = playerRunningAnimation; break;
				case DYING: currentAnimation = playerDyingAnimation; break;
				default: currentAnimation = playerIdleAnimation; break;
			}
		}
		return currentAnimation.isAnimationFinished(playerAnimationTime);
	}

	public TextureRegion getMushieCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (mushieCurrentState) {
			case RUNNING: currentAnimation = mushieRunningAnimation; break;
			case ATTACKING: currentAnimation = mushieAttackingAnimation; break;
			default: currentAnimation = mushieIdleAnimation; break;
		}
		return currentAnimation.getKeyFrame(mushieAnimationTime);
	}

	public TextureRegion getBossKittyCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (bossKittyCurrentState) {
			case DYING: currentAnimation = bossKittyDyingAnimation; break;
			default: currentAnimation = bossKittyRunningAnimation; break;
		}
		return currentAnimation.getKeyFrame(bossKittyAnimationTime);
	}

	public TextureRegion getSkeletonCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (skeletonCurrentState) {
			case RUNNING: currentAnimation = skeletonRunningAnimation; break;
			case ATTACKING: currentAnimation = skeletonAttackingAnimation; break;
			default: currentAnimation = skeletonIdleAnimation; break;
		}
		return currentAnimation.getKeyFrame(skeletonAnimationTime);
	}

	public TextureRegion getWolfieCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (wolfieCurrentState) {
			case RUNNING: currentAnimation = wolfieRunningAnimation; break;
			case ATTACKING: currentAnimation = wolfieAttackingAnimation; break;
			default: currentAnimation = wolfieIdleAnimation; break;
		}
		return currentAnimation.getKeyFrame(wolfieAnimationTime);
	}

	public TextureRegion getCyclopsCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (cyclopsCurrentState) {
			case RUNNING: currentAnimation = cyclopsRunningAnimation; break;
			case ATTACKING: currentAnimation = cyclopsAttackingAnimation; break;
			default: currentAnimation = cyclopsIdleAnimation; break;
		}
		return currentAnimation.getKeyFrame(cyclopsAnimationTime);
	}

	public TextureRegion getMerchantCurrentFrame() {
		return merchantIdleAnimation.getKeyFrame(merchantAnimationTime);
	}

	public TextureRegion getGhostCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (ghostCurrentState) {
			case RUNNING: currentAnimation = ghostRunningAnimation; break;
			case ATTACKING: currentAnimation = ghostAttackingAnimation; break;
			default: currentAnimation = ghostIdleAnimation; break;
		}
		return currentAnimation.getKeyFrame(ghostAnimationTime);
	}

	public TextureRegion getGhostBossCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (ghostBossCurrentState) {
			case RUNNING: currentAnimation = ghostBossRunningAnimation; break;
			case ATTACKING: currentAnimation = ghostBossAttackingAnimation; break;
			default: currentAnimation = ghostBossIdleAnimation; break;
		}
		return currentAnimation.getKeyFrame(ghostBossAnimationTime);
	}

	public TextureRegion getHermanCurrentFrame() {
		Animation<TextureRegion> currentAnimation;
		switch (hermanCurrentState) {
			case ATTACKING: currentAnimation = hermanAttackingAnimation; break;
			case SPECIAL_ATTACK: currentAnimation = hermanSpecialAttackAnimation; break;
			default: currentAnimation = hermanIdleAnimation; break;
		}
		return currentAnimation.getKeyFrame(hermanAnimationTime);
	}

	public boolean isBossKittyAnimationFinished() {
		switch (bossKittyCurrentState) {
			case DYING: return bossKittyDyingAnimation.isAnimationFinished(bossKittyAnimationTime);
			default: return bossKittyRunningAnimation.isAnimationFinished(bossKittyAnimationTime);
		}
	}

	public boolean isCyclopsAnimationFinished() {
		switch (cyclopsCurrentState) {
			case RUNNING: return cyclopsRunningAnimation.isAnimationFinished(cyclopsAnimationTime);
			case ATTACKING: return cyclopsAttackingAnimation.isAnimationFinished(cyclopsAnimationTime);
			default: return cyclopsIdleAnimation.isAnimationFinished(cyclopsAnimationTime);
		}
	}

	public boolean isHermanAnimationFinished() {
		switch (hermanCurrentState) {
			case ATTACKING: return hermanAttackingAnimation.isAnimationFinished(hermanAnimationTime);
			case SPECIAL_ATTACK: return hermanSpecialAttackAnimation.isAnimationFinished(hermanAnimationTime);
			default: return hermanIdleAnimation.isAnimationFinished(hermanAnimationTime);
		}
	}
}