package abilities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.Gdx;
import config.Storage;
import entities.Player;
import game.GameProj;
import managers.CollisionFilter;

/**
 * Abstract base class for all ability visual effects
 */
public abstract class AbilityVisual {
    protected float duration;
    protected float timer;
    protected boolean active;
    protected Texture whitePixelTexture;

    public AbilityVisual(float duration) {
        this.duration = duration;
        this.timer = 0f;
        this.active = true;
        this.whitePixelTexture = Storage.assetManager.get("white_pixel.png", Texture.class);
    }

    public void update(float delta) {
        if (!active) return;

        timer += delta;

        if (timer >= duration) {
            active = false;
        }

        onUpdate(delta);
    }

    /**
     * Override for custom update logic
     */
    protected void onUpdate(float delta) {}

    public abstract void render(SpriteBatch batch);

    public void dispose() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    protected void drawLine(SpriteBatch batch, float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        batch.draw(whitePixelTexture, x1, y1 - thickness / 2, 0, thickness / 2,
                dist, thickness, 1, 1, angle,
                0, 0, whitePixelTexture.getWidth(), whitePixelTexture.getHeight(),
                false, false);
    }

    // =========================================================================
    // BUBBLE VISUAL - Protective shield with physics body
    // =========================================================================
    public static class Bubble extends AbilityVisual {
        private Body body;
        private Texture texture;
        private Player player;
        private float size;
        private World world;

        private static final float BUBBLE_SIZE_MULTIPLIER = 1.2f;
        private static final float ALPHA = 0.5f;

        public Bubble(Player player, World world, float duration) {
            super(duration);
            this.player = player;
            this.world = world;
            this.size = 32;
            this.texture = Storage.assetManager.get("character/abilities/Bubble.png", Texture.class);

            createBody();
        }

        private void createBody() {
            Vector2 playerPos = player.getPosition();

            BodyDef bodyDef = new BodyDef();
            // Use KinematicBody - it can move but isn't affected by forces,
            // and will solidly block dynamic bodies (enemies)
            bodyDef.type = BodyDef.BodyType.KinematicBody;
            bodyDef.position.set(playerPos.x, playerPos.y);
            bodyDef.fixedRotation = true;

            body = world.createBody(bodyDef);

            CircleShape shape = new CircleShape();
            shape.setRadius(size / 2f);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.isSensor = false;
            fixtureDef.density = 1f;
            fixtureDef.friction = 0.8f;
            fixtureDef.restitution = 1f; // High restitution to bounce enemies back
            fixtureDef.filter.categoryBits = CollisionFilter.ABILITY;
            fixtureDef.filter.maskBits = CollisionFilter.ENEMY | CollisionFilter.PROJECTILE;

            body.createFixture(fixtureDef);
            shape.dispose();
        }

        @Override
        protected void onUpdate(float delta) {
            if (body != null && player.getBody() != null) {
                body.setTransform(player.getPosition(), 0);
                body.setLinearVelocity(0, 0);
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active || texture == null) return;

            Vector2 pos = player.getPosition();
            batch.setColor(1f, 1f, 1f, ALPHA);
            batch.draw(texture, pos.x - size / 2f, pos.y - size / 2f, size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void dispose() {
            if (body != null && world != null) {
                world.destroyBody(body);
                body = null;
            }
            super.dispose();
        }

        public Body getBody() {
            return body;
        }
    }

    // =========================================================================
    // PRAYER VISUAL - Healing aura overlay
    // =========================================================================
    public static class Prayer extends AbilityVisual {
        private Texture texture;
        private Player player;
        private float size;
        private float pulseTimer = 0f;

        private static final float PRAYER_SIZE_MULTIPLIER = 2.0f;
        private static final float BASE_ALPHA = 0.5f;
        private static final float PULSE_SPEED = 3f;

        public Prayer(Player player, float duration) {
            super(duration);
            this.player = player;
            this.size = 32;
            this.texture = Storage.assetManager.get("character/abilities/Prayer.png", Texture.class);
        }

        @Override
        protected void onUpdate(float delta) {
            pulseTimer += delta * PULSE_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active || texture == null) return;

            Vector2 pos = player.getPosition();
            float pulseAlpha = BASE_ALPHA + 0.2f * (float) Math.sin(pulseTimer);

            batch.setColor(1f, 1f, 1f, pulseAlpha);
            batch.draw(texture, pos.x - size / 2f, pos.y - size / 2f, size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
// CONAL ATTACK INDICATOR - For DoubleSwing (white) and Rend (red)
// Now with swinging animation for DoubleSwing
// =========================================================================
    public static class ConalAttack extends AbilityVisual {
        private Player player;
        private GameProj gameProj;
        private float range;
        private float coneAngle;
        private Color indicatorColor;
        private boolean isSwingAnimation; // NEW: Whether to use swing animation
        private float swingProgress;      // NEW: Progress of swing animation (0-1)
        private int swingCount;           // NEW: Current swing count (for DoubleSwing)
        private float swingSpeed = 2.5f;  // NEW: Speed of swing animation

        private static final float DEFAULT_RANGE = 25f;
        private static final float DEFAULT_CONE_ANGLE = 60f;

        public ConalAttack(Player player, GameProj gameProj, float duration, float range, Color color) {
            super(duration);
            this.player = player;
            this.gameProj = gameProj;
            this.range = range;
            this.coneAngle = DEFAULT_CONE_ANGLE;
            this.indicatorColor = color;
            this.isSwingAnimation = false;
            this.swingProgress = 0f;
            this.swingCount = 1;
        }

        /**
         * Create swinging white indicator for DoubleSwing with custom range
         */
        public static ConalAttack createSwingingWhite(Player player, GameProj gameProj, float duration, float range, int swingNumber) {
            ConalAttack attack = new ConalAttack(player, gameProj, duration, range, Color.WHITE);
            attack.isSwingAnimation = true;
            attack.swingProgress = 0f;
            attack.swingCount = swingNumber;
            return attack;
        }

        /**
         * Create static white indicator for other abilities
         */
        public static ConalAttack createWhite(Player player, GameProj gameProj, float duration, float range) {
            return new ConalAttack(player, gameProj, duration, range, Color.WHITE);
        }

        /**
         * Create red indicator for Rend with custom range
         */
        public static ConalAttack createRed(Player player, GameProj gameProj, float duration, float range) {
            return new ConalAttack(player, gameProj, duration, range, new Color(1f, 0.2f, 0.2f, 1f));
        }

        @Override
        protected void onUpdate(float delta) {
            super.onUpdate(delta);

            // Update swing animation progress
            if (isSwingAnimation && swingProgress < 1f) {
                swingProgress += delta * swingSpeed;
                if (swingProgress > 1f) {
                    swingProgress = 1f;
                }
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 playerPos = player.getPosition();

            Vector3 mousePos3D = gameProj.getCamera().unproject(
                    new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
            );
            Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
            Vector2 direction = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();
            float facingAngle = direction.angleDeg();

            if (isSwingAnimation) {
                renderSwingingCone(batch, playerPos, facingAngle);
            } else {
                renderStaticCone(batch, playerPos, facingAngle);
            }
        }

        /**
         * Render swinging cone animation (for DoubleSwing)
         */
        private void renderSwingingCone(SpriteBatch batch, Vector2 playerPos, float facingAngle) {
            int segments = 20;
            float halfCone = coneAngle / 3.5f;
            float angleStep = coneAngle / segments;

            // Calculate how many segments to draw based on swing progress
            int segmentsToDraw = (int) (segments * swingProgress);
            if (segmentsToDraw > segments) segmentsToDraw = segments;

            // Render the arc of the swing
            batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.4f);

            for (int i = 0; i < segmentsToDraw; i++) {
                float angle1 = facingAngle - halfCone + (i * angleStep);
                float angle2 = facingAngle - halfCone + ((i + 1) * angleStep);

                float rad1 = (float) Math.toRadians(angle1);
                float rad2 = (float) Math.toRadians(angle2);

                float x1 = playerPos.x + (float) Math.cos(rad1) * range;
                float y1 = playerPos.y + (float) Math.sin(rad1) * range;
                float x2 = playerPos.x + (float) Math.cos(rad2) * range;
                float y2 = playerPos.y + (float) Math.sin(rad2) * range;

                // Draw the radial line
                drawLine(batch, playerPos.x, playerPos.y, x1, y1, 2f);

                // Draw the arc segment (but only if we're drawing full cone)
                if (i < segmentsToDraw - 1) {
                    batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.2f);
                    drawLine(batch, x1, y1, x2, y2, 2f);
                    batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.4f);
                }
            }

            // Draw the final radial line if we're at full swing
            if (segmentsToDraw == segments) {
                float finalAngle = facingAngle + halfCone;
                float finalRad = (float) Math.toRadians(finalAngle);
                float finalX = playerPos.x + (float) Math.cos(finalRad) * range;
                float finalY = playerPos.y + (float) Math.sin(finalRad) * range;
                drawLine(batch, playerPos.x, playerPos.y, finalX, finalY, 2f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        /**
         * Render static cone (for Rend and other abilities)
         */
        private void renderStaticCone(SpriteBatch batch, Vector2 playerPos, float facingAngle) {
            batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.3f);

            int segments = 12;
            float halfCone = coneAngle / 2f;
            float angleStep = coneAngle / segments;

            for (int i = 0; i < segments; i++) {
                float angle1 = facingAngle - halfCone + (i * angleStep);
                float angle2 = facingAngle - halfCone + ((i + 1) * angleStep);

                float rad1 = (float) Math.toRadians(angle1);
                float rad2 = (float) Math.toRadians(angle2);

                float x1 = playerPos.x + (float) Math.cos(rad1) * range;
                float y1 = playerPos.y + (float) Math.sin(rad1) * range;
                float x2 = playerPos.x + (float) Math.cos(rad2) * range;
                float y2 = playerPos.y + (float) Math.sin(rad2) * range;

                drawLine(batch, playerPos.x, playerPos.y, x1, y1, 2f);

                batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.2f);
                drawLine(batch, x1, y1, x2, y2, 2f);
                batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.3f);
            }

            float finalAngle = facingAngle + halfCone;
            float finalRad = (float) Math.toRadians(finalAngle);
            float finalX = playerPos.x + (float) Math.cos(finalRad) * range;
            float finalY = playerPos.y + (float) Math.sin(finalRad) * range;
            drawLine(batch, playerPos.x, playerPos.y, finalX, finalY, 2f);

            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // BLINK VISUAL - Dissipating circle at teleport destination
    // =========================================================================
    public static class Blink extends AbilityVisual {
        private Vector2 position;

        private static final float INITIAL_RADIUS = 40f;
        private static final float DISSIPATE_DURATION = 0.4f;
        private static final int CIRCLE_SEGMENTS = 24;

        public Blink(Vector2 position) {
            super(DISSIPATE_DURATION);
            this.position = new Vector2(position);
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            float progress = timer / duration;
            float currentRadius = INITIAL_RADIUS * (1f - progress);
            float alpha = 0.8f * (1f - progress);

            renderCircle(batch, currentRadius, alpha);

            if (currentRadius > 5f) {
                renderFilledCircle(batch, currentRadius * 0.6f, alpha * 0.3f);
            }
        }

        private void renderCircle(SpriteBatch batch, float radius, float alpha) {
            batch.setColor(1f, 1f, 1f, alpha);

            float angleStep = 360f / CIRCLE_SEGMENTS;

            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                float angle = i * angleStep;
                float nextAngle = (i + 1) * angleStep;
                float rad = (float) Math.toRadians(angle);
                float nextRad = (float) Math.toRadians(nextAngle);

                float x1 = position.x + (float) Math.cos(rad) * radius;
                float y1 = position.y + (float) Math.sin(rad) * radius;
                float x2 = position.x + (float) Math.cos(nextRad) * radius;
                float y2 = position.y + (float) Math.sin(nextRad) * radius;

                drawLine(batch, x1, y1, x2, y2, 3f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        private void renderFilledCircle(SpriteBatch batch, float radius, float alpha) {
            batch.setColor(1f, 1f, 1f, alpha);

            float angleStep = 360f / CIRCLE_SEGMENTS;

            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                float angle = i * angleStep;
                float rad = (float) Math.toRadians(angle);

                float x = position.x + (float) Math.cos(rad) * radius;
                float y = position.y + (float) Math.sin(rad) * radius;

                drawLine(batch, position.x, position.y, x, y, 2f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
// CHARGE TRAIL VISUAL - Trail effect for Charge ability
// =========================================================================
    public static class ChargeTrail extends AbilityVisual {
        private Player player;
        private com.badlogic.gdx.utils.Array<TrailPoint> trailPoints;
        private float spawnInterval = 0.02f;
        private float spawnTimer = 0f;
        private float trailPointLifetime = 0.3f;

        private static final float TRAIL_ALPHA = 0.6f;

        private static class TrailPoint {
            Vector2 position;
            float lifetime;
            float maxLifetime;

            TrailPoint(Vector2 pos, float lifetime) {
                this.position = new Vector2(pos);
                this.lifetime = lifetime;
                this.maxLifetime = lifetime;
            }
        }

        public ChargeTrail(Player player, float duration) {
            super(duration);
            this.player = player;
            this.trailPoints = new com.badlogic.gdx.utils.Array<>();
        }

        @Override
        protected void onUpdate(float delta) {
            // Spawn new trail points while charging
            spawnTimer += delta;
            if (spawnTimer >= spawnInterval && timer < duration * 0.8f) {
                trailPoints.add(new TrailPoint(player.getPosition(), trailPointLifetime));
                spawnTimer = 0f;
            }

            // Update existing trail points
            for (int i = trailPoints.size - 1; i >= 0; i--) {
                TrailPoint point = trailPoints.get(i);
                point.lifetime -= delta;
                if (point.lifetime <= 0) {
                    trailPoints.removeIndex(i);
                }
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active && trailPoints.size == 0) return;

            // Get player's current animation frame for the trail
            com.badlogic.gdx.graphics.g2d.TextureRegion frame = player.getAnimationManager().getCurrentFrame();
            float size = 16f; // Trail sprite size

            for (TrailPoint point : trailPoints) {
                float alpha = TRAIL_ALPHA * (point.lifetime / point.maxLifetime);

                // Red tint for charge trail
                batch.setColor(1f, 0.3f, 0.3f, alpha);
                batch.draw(frame,
                        point.position.x - size / 2f,
                        point.position.y - size / 2f,
                        size, size);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        @Override
        public void dispose() {
            trailPoints.clear();
            super.dispose();
        }

        public boolean hasTrailPoints() {
            return trailPoints.size > 0;
        }
    }
}