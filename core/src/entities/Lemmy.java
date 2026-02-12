package entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import config.Storage;
import managers.AnimationManager;
import managers.AnimationManager.State;
import managers.SoundManager;
import ui.ScreenShake;

public class Lemmy {
    private Rectangle bounds;
    private Body body;
    private final Player player;
    private boolean markForRemoval = false;
    private final AnimationManager animationManager;
    private boolean isFlipped = false;
    private boolean isStunned = false;

    private EnemyStats stats;
    private Texture healthBarTexture;

    private float animationTime = 0f;
    private State currentState = State.IDLE;

    private boolean isJustHit = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.3f;

    private float currentSpeed = 65f;
    private static final float SPEED_BOOST_PER_HIT = 5f;
    private static final float MAX_SPEED = 100f;

    private Vector2 randomDirection = new Vector2();
    private float directionChangeTimer = 0f;
    private static final float DIRECTION_CHANGE_INTERVAL = 2.0f;
    private static final float PANIC_RADIUS = 120f;

    public Lemmy(Rectangle bounds, Body body, Player player,
                 AnimationManager animationManager, EnemyStats stats) {
        this.animationManager = animationManager;
        this.bounds = bounds;
        this.body = body;
        this.player = player;
        this.stats = stats;

        this.healthBarTexture = Storage.assetManager.get("tiles/hpBar.png", Texture.class);

        pickRandomDirection();

        this.currentState = State.IDLE;
        this.animationTime = 0f;
    }

    private void setState(State newState) {
        if (currentState != newState) {
            currentState = newState;
            animationTime = 0f;
        }
    }

    private TextureRegion getCurrentFrame() {
        Animation<TextureRegion> animation = animationManager.getAnimationForState(EnemyType.LEMMY, currentState);
        if (animation != null) {
            boolean loop = (currentState == State.IDLE || currentState == State.RUNNING);
            return animation.getKeyFrame(animationTime, loop);
        }
        return null;
    }

    private void pickRandomDirection() {
        float angle = (float) (Math.random() * Math.PI * 2);
        randomDirection.set((float) Math.cos(angle), (float) Math.sin(angle)).nor();
    }

    public void update(float delta) {
        if (markForRemoval || !Player.gameStarted) {
            body.setLinearVelocity(0, 0);
            return;
        }

        if (isStunned) {
            if (currentState != State.IDLE) {
                setState(State.IDLE);
            }
            animationTime += delta;
            body.setLinearVelocity(0, 0);
            return;
        }

        animationTime += delta;

        if (isJustHit) {
            hitFlashTimer -= delta;
            if (hitFlashTimer <= 0) {
                isJustHit = false;
            }
        }

        directionChangeTimer += delta;
        if (directionChangeTimer >= DIRECTION_CHANGE_INTERVAL) {
            directionChangeTimer = 0f;
            pickRandomDirection();
        }

        Vector2 playerPos = player.getPosition();
        Vector2 lemmyPos = new Vector2(body.getPosition().x, body.getPosition().y);
        float distanceToPlayer = lemmyPos.dst(playerPos);

        Vector2 moveDirection;
        if (distanceToPlayer < PANIC_RADIUS) {
            Vector2 awayFromPlayer = new Vector2(
                    lemmyPos.x - playerPos.x,
                    lemmyPos.y - playerPos.y
            );

            if (awayFromPlayer.len() > 0.01f) {
                moveDirection = awayFromPlayer.nor();
            } else {
                pickRandomDirection();
                moveDirection = randomDirection;
            }

            setState(State.RUNNING);
        } else {
            moveDirection = randomDirection;
            setState(State.RUNNING);
        }

        body.setLinearVelocity(moveDirection.scl(currentSpeed));

        bounds.setPosition(body.getPosition().x - bounds.width / 2f,
                body.getPosition().y - bounds.height / 2f);

        isFlipped = body.getLinearVelocity().x < 0;
    }

    public void render(SpriteBatch batch) {
        if (!markForRemoval) {
            TextureRegion currentFrame = getCurrentFrame();
            if (currentFrame != null) {
                TextureRegion frame = new TextureRegion(currentFrame);
                if (isFlipped && !frame.isFlipX()) {
                    frame.flip(true, false);
                } else if (!isFlipped && frame.isFlipX()) {
                    frame.flip(true, false);
                }

                if (isJustHit) {
                    batch.setColor(1f, 1f, 0.3f, 1f);
                }
                batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
                if (isJustHit) {
                    batch.setColor(1f, 1f, 1f, 1f);
                }
            }

            renderHealthBar(batch);
        }
    }

    private void renderHealthBar(SpriteBatch batch) {
        if (stats.getHealthPercentage() >= 1.0f) {
            return;
        }

        float barWidth = bounds.width;
        float barHeight = 3f;
        float barX = bounds.x;
        float barY = bounds.y + bounds.height + 2f;

        batch.setColor(0.8f, 0.1f, 0.1f, 1f);
        batch.draw(healthBarTexture, barX, barY, barWidth, barHeight);

        float healthWidth = barWidth * stats.getHealthPercentage();
        batch.setColor(1f, 0.84f, 0f, 1f);
        batch.draw(healthBarTexture, barX, barY, healthWidth, barHeight);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void takeDamage(int damage) {
        if (damage > 0) {
            stats.takeDamage(damage);
            isJustHit = true;
            hitFlashTimer = HIT_FLASH_DURATION;

            currentSpeed = Math.min(currentSpeed + SPEED_BOOST_PER_HIT, MAX_SPEED);

            Vector2 playerPos = player.getPosition();
            Vector2 lemmyPos = new Vector2(body.getPosition().x, body.getPosition().y);
            Vector2 awayFromPlayer = new Vector2(
                    lemmyPos.x - playerPos.x,
                    lemmyPos.y - playerPos.y
            );

            if (awayFromPlayer.len() > 0.01f) {
                randomDirection.set(awayFromPlayer.nor());
            } else {
                pickRandomDirection();
            }
            directionChangeTimer = 0f;

            SoundManager.getInstance().playLemmyHitSound();

            ScreenShake.rumble(0.6f, 0.15f);
            SoundManager.getInstance().playEnemyHitSound();
        }

        if (stats.isDead()) {
            markForRemoval();
        }
    }

    public void clearBody() {
        this.body = null;
    }

    public void dispose() {
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

    public EnemyStats getStats() {
        return stats;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setStunned(boolean stunned) {
        this.isStunned = stunned;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }
}