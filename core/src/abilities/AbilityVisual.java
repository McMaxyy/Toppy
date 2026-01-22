package abilities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    // CONAL ATTACK INDICATOR - Using Cone.png texture with segmented fill
    // =========================================================================
    public static class ConalAttack extends AbilityVisual {
        private Player player;
        private GameProj gameProj;
        private float range;
        private float coneAngle;
        private Color indicatorColor;
        private boolean isSwingAnimation;
        private float swingProgress;
        private int swingCount;
        private float swingSpeed = 2.5f;

        // Cone texture
        private static Texture coneTexture;
        private static final int NUM_SEGMENTS = 8; // Number of segments to divide cone into

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

            // Load cone texture if not already loaded
            if (coneTexture == null) {
                try {
                    coneTexture = Storage.assetManager.get("icons/abilities/Cone.png", Texture.class);
                } catch (Exception e) {
                    coneTexture = null;
                }
            }
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

        /**
         * Create golden indicator for Paladin sword attacks
         */
        public static ConalAttack createGolden(Player player, GameProj gameProj, float duration, float range) {
            return new ConalAttack(player, gameProj, duration, range, new Color(1f, 0.85f, 0.2f, 1f));
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

            if (coneTexture != null) {
                renderTexturedConeStatic(batch, playerPos, facingAngle);
            } else {
                renderStaticCone(batch, playerPos, facingAngle);
            }
        }

        /**
         * Render cone using texture (static, full cone)
         */
        private void renderTexturedConeStatic(SpriteBatch batch, Vector2 playerPos, float facingAngle) {
            // Draw the full cone texture
            float coneWidth = range;
            float coneHeight = range * 0.8f; // Adjust aspect ratio as needed

            batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.5f);

            // Draw cone with origin at left center (tip of cone)
            batch.draw(coneTexture,
                    playerPos.x, playerPos.y - coneHeight / 2f,  // position
                    0, coneHeight / 2f,                           // origin (left center - cone tip)
                    coneWidth, coneHeight,                        // size
                    1f, 1f,                                       // scale
                    facingAngle,                                  // rotation
                    0, 0,                                         // src position
                    coneTexture.getWidth(), coneTexture.getHeight(), // src size
                    false, false);                                // flip

            batch.setColor(1f, 1f, 1f, 1f);
        }


        /**
         * Render static cone (fallback line-based)
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

        /**
         * Set the cone texture (can be called to change texture at runtime)
         */
        public static void setConeTexture(Texture texture) {
            coneTexture = texture;
        }
    }

    // =========================================================================
    // ENEMY CONAL ATTACK - For enemy attacks (position-based, not player-based)
    // =========================================================================
    public static class EnemyConalAttack extends AbilityVisual {
        private Vector2 position;
        private float facingAngle;
        private float range;
        private float coneAngle;
        private Color indicatorColor;
        private float fillProgress;
        private float fillSpeed;

        // Cone texture
        private static Texture coneTexture;
        private static final int NUM_SEGMENTS = 8;

        public EnemyConalAttack(Vector2 position, float facingAngle, float duration,
                                float range, float coneAngle, Color color, float fillSpeed) {
            super(duration);
            this.position = new Vector2(position);
            this.facingAngle = facingAngle;
            this.range = range;
            this.coneAngle = coneAngle;
            this.indicatorColor = color;
            this.fillProgress = 0f;
            this.fillSpeed = fillSpeed;

            // Load cone texture if not already loaded
            if (coneTexture == null) {
                try {
                    coneTexture = Storage.assetManager.get("tiles/Cone.png", Texture.class);
                } catch (Exception e) {
                    coneTexture = null;
                }
            }
        }

        /**
         * Update the position and facing angle (for moving enemies)
         */
        public void updatePositionAndAngle(Vector2 newPosition, float newAngle) {
            this.position.set(newPosition);
            this.facingAngle = newAngle;
        }

        /**
         * Get current fill progress (0-1)
         */
        public float getFillProgress() {
            return fillProgress;
        }

        @Override
        protected void onUpdate(float delta) {
            if (fillProgress < 1f) {
                fillProgress += delta * fillSpeed;
                if (fillProgress > 1f) {
                    fillProgress = 1f;
                }
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            if (coneTexture != null) {
                renderTexturedCone(batch);
            } else {
                renderLineCone(batch);
            }
        }

        private void renderTexturedCone(SpriteBatch batch) {
            int segmentsToDraw = (int)(NUM_SEGMENTS * fillProgress);
            if (segmentsToDraw < 1 && fillProgress > 0) segmentsToDraw = 1;

            float segmentAngle = coneAngle / NUM_SEGMENTS;
            float startAngle = facingAngle - (coneAngle / 2f);

            int textureWidth = coneTexture.getWidth();
            int textureHeight = coneTexture.getHeight();
            int segmentWidth = textureWidth / NUM_SEGMENTS;

            float coneWidth = range;
            float coneHeight = range * ((float)textureHeight / textureWidth) * NUM_SEGMENTS;
            float segmentDrawWidth = coneWidth / NUM_SEGMENTS;

            for (int i = 0; i < segmentsToDraw; i++) {
                int srcX = i * segmentWidth;
                TextureRegion segmentRegion = new TextureRegion(coneTexture, srcX, 0, segmentWidth, textureHeight);

                float angle = startAngle + (i * segmentAngle) + (segmentAngle / 2f);

                float segmentAlpha = 0.5f;
                if (i == segmentsToDraw - 1) {
                    segmentAlpha = 0.7f;
                }

                batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, segmentAlpha);

                batch.draw(segmentRegion,
                        position.x, position.y - coneHeight / 2f,
                        0, coneHeight / 2f,
                        segmentDrawWidth, coneHeight,
                        1f, 1f,
                        angle);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        private void renderLineCone(SpriteBatch batch) {
            int segments = 12;
            int segmentsToDraw = (int)(segments * fillProgress);
            if (segmentsToDraw < 1 && fillProgress > 0) segmentsToDraw = 1;

            float halfCone = coneAngle / 2f;
            float angleStep = coneAngle / segments;

            batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.4f);

            for (int i = 0; i < segmentsToDraw; i++) {
                float angle1 = facingAngle - halfCone + (i * angleStep);
                float rad1 = (float) Math.toRadians(angle1);

                float x1 = position.x + (float) Math.cos(rad1) * range;
                float y1 = position.y + (float) Math.sin(rad1) * range;

                drawLine(batch, position.x, position.y, x1, y1, 2f);
            }

            // Draw arc
            batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.2f);
            for (int i = 0; i < segmentsToDraw - 1; i++) {
                float angle1 = facingAngle - halfCone + (i * angleStep);
                float angle2 = facingAngle - halfCone + ((i + 1) * angleStep);
                float rad1 = (float) Math.toRadians(angle1);
                float rad2 = (float) Math.toRadians(angle2);

                float x1 = position.x + (float) Math.cos(rad1) * range;
                float y1 = position.y + (float) Math.sin(rad1) * range;
                float x2 = position.x + (float) Math.cos(rad2) * range;
                float y2 = position.y + (float) Math.sin(rad2) * range;

                drawLine(batch, x1, y1, x2, y2, 2f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        public static void setConeTexture(Texture texture) {
            coneTexture = texture;
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

    // =========================================================================
    // PULL CIRCLE VISUAL - Shrinking circle for Pull ability
    // =========================================================================
    public static class PullCircle extends AbilityVisual {
        private Player player;
        private float maxRadius;
        private float currentRadius;

        private static final int CIRCLE_SEGMENTS = 32;
        private static final Color PULL_COLOR = new Color(0.6f, 0.3f, 1f, 1f); // Purple/divine color

        public PullCircle(Player player, float radius, float duration) {
            super(duration);
            this.player = player;
            this.maxRadius = radius;
            this.currentRadius = radius;
        }

        @Override
        protected void onUpdate(float delta) {
            // Shrink the circle over time
            float progress = timer / duration;
            currentRadius = maxRadius * (1f - progress);
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float progress = timer / duration;
            float alpha = 0.6f * (1f - progress * 0.5f);

            // Draw outer circle (shrinking)
            renderCircle(batch, pos, currentRadius, alpha, PULL_COLOR);

            // Draw inner glow
            if (currentRadius > 10f) {
                renderFilledCircle(batch, pos, currentRadius * 0.3f, alpha * 0.3f, PULL_COLOR);
            }

            // Draw radial lines (pull effect)
            renderPullLines(batch, pos, currentRadius, alpha);
        }

        private void renderCircle(SpriteBatch batch, Vector2 center, float radius, float alpha, Color color) {
            batch.setColor(color.r, color.g, color.b, alpha);

            float angleStep = 360f / CIRCLE_SEGMENTS;

            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                float angle = i * angleStep;
                float nextAngle = (i + 1) * angleStep;
                float rad = (float) Math.toRadians(angle);
                float nextRad = (float) Math.toRadians(nextAngle);

                float x1 = center.x + (float) Math.cos(rad) * radius;
                float y1 = center.y + (float) Math.sin(rad) * radius;
                float x2 = center.x + (float) Math.cos(nextRad) * radius;
                float y2 = center.y + (float) Math.sin(nextRad) * radius;

                drawLine(batch, x1, y1, x2, y2, 3f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        private void renderFilledCircle(SpriteBatch batch, Vector2 center, float radius, float alpha, Color color) {
            batch.setColor(color.r, color.g, color.b, alpha);

            float angleStep = 360f / CIRCLE_SEGMENTS;

            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                float angle = i * angleStep;
                float rad = (float) Math.toRadians(angle);

                float x = center.x + (float) Math.cos(rad) * radius;
                float y = center.y + (float) Math.sin(rad) * radius;

                drawLine(batch, center.x, center.y, x, y, 2f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        private void renderPullLines(SpriteBatch batch, Vector2 center, float radius, float alpha) {
            batch.setColor(PULL_COLOR.r, PULL_COLOR.g, PULL_COLOR.b, alpha * 0.5f);

            int numLines = 8;
            float angleStep = 360f / numLines;

            for (int i = 0; i < numLines; i++) {
                float angle = i * angleStep + (timer * 180f); // Rotate over time
                float rad = (float) Math.toRadians(angle);

                float innerRadius = radius * 0.2f;
                float x1 = center.x + (float) Math.cos(rad) * innerRadius;
                float y1 = center.y + (float) Math.sin(rad) * innerRadius;
                float x2 = center.x + (float) Math.cos(rad) * radius;
                float y2 = center.y + (float) Math.sin(rad) * radius;

                drawLine(batch, x1, y1, x2, y2, 2f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // SMITE VISUAL - Divine AOE damage circle
    // =========================================================================
    public static class Smite extends AbilityVisual {
        private Player player;
        private Texture texture;
        private float radius;
        private float flashTimer = 0f;

        private static final Color SMITE_COLOR = new Color(1f, 0.9f, 0.4f, 1f); // Golden divine color
        private static final float FLASH_SPEED = 15f;

        public Smite(Player player, float radius, float duration) {
            super(duration);
            this.player = player;
            this.radius = radius;

            // Try to load the smite texture
            try {
                this.texture = Storage.assetManager.get("character/abilities/Smite.png", Texture.class);
            } catch (Exception e) {
                this.texture = null;
            }
        }

        @Override
        protected void onUpdate(float delta) {
            flashTimer += delta * FLASH_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float progress = timer / duration;

            // Calculate alpha with flash effect
            float baseAlpha = 0.7f * (1f - progress);
            float flashAlpha = baseAlpha + 0.2f * (float) Math.sin(flashTimer);

            if (texture != null) {
                // Render texture
                batch.setColor(1f, 1f, 1f, flashAlpha);
                float size = radius * 2f;
                batch.draw(texture, pos.x - size / 2f, pos.y - size / 2f, size, size);
                batch.setColor(1f, 1f, 1f, 1f);
            } else {
                // Fallback: render circle effect
                renderSmiteCircle(batch, pos, radius, flashAlpha);
            }

            // Always render the golden ring
            renderSmiteRing(batch, pos, radius, baseAlpha);
        }

        private void renderSmiteCircle(SpriteBatch batch, Vector2 center, float radius, float alpha) {
            batch.setColor(SMITE_COLOR.r, SMITE_COLOR.g, SMITE_COLOR.b, alpha * 0.5f);

            int segments = 32;
            float angleStep = 360f / segments;

            // Draw filled circle using lines from center
            for (int i = 0; i < segments; i++) {
                float angle = i * angleStep;
                float rad = (float) Math.toRadians(angle);

                float x = center.x + (float) Math.cos(rad) * radius;
                float y = center.y + (float) Math.sin(rad) * radius;

                drawLine(batch, center.x, center.y, x, y, 3f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }

        private void renderSmiteRing(SpriteBatch batch, Vector2 center, float radius, float alpha) {
            batch.setColor(SMITE_COLOR.r, SMITE_COLOR.g, SMITE_COLOR.b, alpha);

            int segments = 32;
            float angleStep = 360f / segments;

            for (int i = 0; i < segments; i++) {
                float angle = i * angleStep;
                float nextAngle = (i + 1) * angleStep;
                float rad = (float) Math.toRadians(angle);
                float nextRad = (float) Math.toRadians(nextAngle);

                float x1 = center.x + (float) Math.cos(rad) * radius;
                float y1 = center.y + (float) Math.sin(rad) * radius;
                float x2 = center.x + (float) Math.cos(nextRad) * radius;
                float y2 = center.y + (float) Math.sin(nextRad) * radius;

                drawLine(batch, x1, y1, x2, y2, 4f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }
    }
}