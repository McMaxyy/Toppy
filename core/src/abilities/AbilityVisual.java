package abilities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import config.Storage;
import entities.Player;
import game.GameProj;
import managers.CollisionFilter;

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
            fixtureDef.restitution = 1f;
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
    // PRAYER VISUAL
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
    // CONAL ATTACK INDICATOR
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

        private static Texture coneTexture;

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

            if (coneTexture == null) {
                try {
                    coneTexture = Storage.assetManager.get("icons/abilities/Cone.png", Texture.class);
                } catch (Exception e) {
                    coneTexture = null;
                }
            }
        }

        public static ConalAttack createWhite(Player player, GameProj gameProj, float duration, float range) {
            return new ConalAttack(player, gameProj, duration, range, Color.WHITE);
        }

        public static ConalAttack createRed(Player player, GameProj gameProj, float duration, float range) {
            return new ConalAttack(player, gameProj, duration, range, new Color(1f, 0.2f, 0.2f, 1f));
        }

        public static ConalAttack createGolden(Player player, GameProj gameProj, float duration, float range) {
            return new ConalAttack(player, gameProj, duration, range, new Color(1f, 0.85f, 0.2f, 1f));
        }

        @Override
        protected void onUpdate(float delta) {
            super.onUpdate(delta);

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

        private void renderTexturedConeStatic(SpriteBatch batch, Vector2 playerPos, float facingAngle) {
            float coneWidth = range;
            float coneHeight = range * 0.8f;

            batch.setColor(indicatorColor.r, indicatorColor.g, indicatorColor.b, 0.5f);

            batch.draw(coneTexture,
                    playerPos.x, playerPos.y - coneHeight / 2f,
                    0, coneHeight / 2f,
                    coneWidth, coneHeight,
                    1f, 1f,
                    facingAngle,
                    0, 0,
                    coneTexture.getWidth(), coneTexture.getHeight(),
                    false, false);

            batch.setColor(1f, 1f, 1f, 1f);
        }

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

    public static class SwordSlash extends AbilityVisual {
        private Player player;
        private GameProj gameProj;
        private Texture swordTexture;
        private float range;
        private Color slashColor;

        private float slashAngle;
        private float targetAngle;
        private float slashArcDegrees;
        private float startAngleOffset;

        private static final float SWORD_LENGTH = 40f;
        private static final float TEXTURE_ANGLE_OFFSET = 45f;
        private static final float PIVOT_X_RATIO = 0.15f;
        private static final float PIVOT_Y_RATIO = 0.15f;
        private static final Color HOLY_SLASH_COLOR = new Color(0.988f, 0.969f, 0.529f, 1f);

        public SwordSlash(Player player, GameProj gameProj, float duration, float range, Color color) {
            super(duration);
            this.player = player;
            this.gameProj = gameProj;
            this.range = range;
            this.slashColor = color;
            this.slashArcDegrees = 120f;
            this.startAngleOffset = slashArcDegrees / 2f;

            try {
                if (this.slashColor.equals(HOLY_SLASH_COLOR))
                    this.swordTexture = Storage.assetManager.get("character/abilities/HolySword.png", Texture.class);
                else
                    this.swordTexture = Storage.assetManager.get("character/abilities/SwordAttack.png", Texture.class);
            } catch (Exception e) {
                this.swordTexture = null;
            }



            Vector3 mousePos3D = gameProj.getCamera().unproject(
                    new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
            );
            Vector2 playerPos = player.getPosition();
            Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
            Vector2 direction = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();
            this.targetAngle = direction.angleDeg();

            this.slashAngle = targetAngle + startAngleOffset;
        }

        public static SwordSlash createGolden(Player player, GameProj gameProj, float duration, float range) {
            return new SwordSlash(player, gameProj, duration, range, new Color(1f, 0.85f, 0.2f, 1f));
        }

        public static SwordSlash createWhite(Player player, GameProj gameProj, float duration, float range) {
            return new SwordSlash(player, gameProj, duration, range, Color.WHITE);
        }

        public static SwordSlash createRed(Player player, GameProj gameProj, float duration, float range) {
            return new SwordSlash(player, gameProj, duration, range, new Color(1f, 0.3f, 0.3f, 1f));
        }

        public static SwordSlash createHoly(Player player, GameProj gameProj, float duration, float range) {
            SwordSlash slash = new SwordSlash(player, gameProj, duration, range, new Color(0.988f, 0.969f, 0.529f, 1f));
            slash.slashArcDegrees = 140f;
            slash.startAngleOffset = slash.slashArcDegrees / 2f;
            slash.slashAngle = slash.targetAngle + slash.startAngleOffset;
            return slash;
        }

        @Override
        protected void onUpdate(float delta) {
            float progress = timer / duration;

            slashAngle = targetAngle + startAngleOffset - (slashArcDegrees * progress);
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 playerPos = player.getPosition();
            float progress = timer / duration;

            float alpha = 0.9f;
            if (progress > 0.7f) {
                alpha = 0.9f * (1f - (progress - 0.7f) / 0.3f);
            }

            if (swordTexture != null) {
                renderTexturedSword(batch, playerPos, alpha);
            } else {
                renderLineSword(batch, playerPos, alpha);
            }
        }

        private void renderTexturedSword(SpriteBatch batch, Vector2 playerPos, float alpha) {
            batch.setColor(slashColor.r, slashColor.g, slashColor.b, alpha);

            float textureWidth = swordTexture.getWidth();
            float textureHeight = swordTexture.getHeight();

            float scale = SWORD_LENGTH / Math.max(textureWidth, textureHeight);
            float drawWidth = textureWidth * scale;
            float drawHeight = textureHeight * scale;

            float pivotX = drawWidth * PIVOT_X_RATIO;
            float pivotY = drawHeight * PIVOT_Y_RATIO;

            float offsetDistance = 8f;
            float angleRad = (float) Math.toRadians(slashAngle);
            float offsetX = (float) Math.cos(angleRad) * offsetDistance;
            float offsetY = (float) Math.sin(angleRad) * offsetDistance;

            float drawX = playerPos.x + offsetX - pivotX;
            float drawY = playerPos.y + offsetY - pivotY;

            float rotationAngle = slashAngle - TEXTURE_ANGLE_OFFSET;

            batch.draw(swordTexture,
                    drawX, drawY,
                    pivotX, pivotY,
                    drawWidth, drawHeight,
                    1f, 1f,
                    rotationAngle,
                    0, 0,
                    (int)textureWidth, (int)textureHeight,
                    false, false);

            batch.setColor(1f, 1f, 1f, 1f);
        }

        private void renderLineSword(SpriteBatch batch, Vector2 playerPos, float alpha) {
            batch.setColor(slashColor.r, slashColor.g, slashColor.b, alpha);

            float angleRad = (float) Math.toRadians(slashAngle);
            float endX = playerPos.x + (float) Math.cos(angleRad) * range;
            float endY = playerPos.y + (float) Math.sin(angleRad) * range;

            drawLine(batch, playerPos.x, playerPos.y, endX, endY, 4f);

            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    public static class SpearJab extends AbilityVisual {
        private Player player;
        private GameProj gameProj;
        private float duration;
        private float elapsed;
        private float attackRange;
        private float rotationOffset;
        private Texture spearTexture;
        private Color color;

        private static final float SHOW_PHASE = 0.16f;
        private static final float STRETCH_PHASE = 0.44f;

        private SpearJab(Player player, GameProj gameProj, float duration, float attackRange, float rotationOffset, Color color) {
            super(duration);
            this.player = player;
            this.gameProj = gameProj;
            this.duration = duration;
            this.attackRange = attackRange;
            this.rotationOffset = rotationOffset;
            this.elapsed = 0f;
            this.color = new Color(color);

            try {
                this.spearTexture = Storage.assetManager.get("icons/gear/spearVisual.png", Texture.class);
            } catch (Exception e) {
                System.err.println("Failed to load spearVisual.png: " + e.getMessage());
                this.spearTexture = null;
            }
        }

        public static SpearJab createWhite(Player player, GameProj gameProj, float duration, float attackRange, float rotationOffset) {
            return new SpearJab(player, gameProj, duration, attackRange, rotationOffset, Color.WHITE);
        }

        public static SpearJab createRed(Player player, GameProj gameProj, float duration, float attackRange, float rotationOffset) {
            return new SpearJab(player, gameProj, duration, attackRange, rotationOffset, new Color(1f, 0.2f, 0.2f, 1f));
        }

        @Override
        public void update(float delta) {
            elapsed += delta;
        }

        @Override
        public boolean isActive() {
            return elapsed < duration;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (spearTexture == null || player == null || player.getBody() == null) return;

            Color originalColor = batch.getColor().cpy();

            Vector2 playerPos = player.getPosition();

            Vector3 mousePos3D = gameProj.getCamera().unproject(
                    new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
            );
            Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);

            float targetAngle = (float) Math.toDegrees(
                    Math.atan2(mousePos.y - playerPos.y, mousePos.x - playerPos.x)
            );

            float angle = targetAngle + rotationOffset;

            float progress = elapsed / duration;

            float scaleX = 1f;
            float scaleY = 0.6f;

            if (progress < SHOW_PHASE) {
                scaleX = 0.5f;
            } else if (progress < STRETCH_PHASE) {
                float stretchProgress = (progress - SHOW_PHASE) / (STRETCH_PHASE - SHOW_PHASE);
                scaleX = 0.5f + (attackRange / 300f) * stretchProgress;
            } else {
                float shrinkProgress = (progress - STRETCH_PHASE) / (1f - STRETCH_PHASE);
                float maxStretch = 1f + (attackRange / 300f);
                scaleX = maxStretch * (1f - shrinkProgress) + 1f * shrinkProgress;
            }

            batch.setColor(color.r, color.g, color.b, 0.8f);

            TextureRegion spearRegion = new TextureRegion(spearTexture);

            float width = spearTexture.getWidth();
            float height = spearTexture.getHeight();

            float originX = 0f;
            float originY = height / 2f;

            float offsetDistance = 15f;
            float offsetX = (float) Math.cos(Math.toRadians(angle)) * offsetDistance;
            float offsetY = (float) Math.sin(Math.toRadians(angle)) * offsetDistance;

            batch.draw(
                    spearRegion,
                    playerPos.x + offsetX,
                    playerPos.y + offsetY - originY,
                    originX,
                    originY,
                    width,
                    height,
                    scaleX,
                    scaleY,
                    angle
            );

            batch.setColor(originalColor);
        }

        @Override
        public void dispose() {
        }
    }

    // =========================================================================
    // SHIELD BASH - Quick popping shield attack visual
    // =========================================================================
    public static class ShieldBash extends AbilityVisual {
        private Player player;
        private GameProj gameProj;
        private Texture shieldTexture;
        private float targetAngle;
        private Color bashColor;

        // Shield dimensions
        private static final float SHIELD_SIZE = 28f;
        private static final float SHIELD_DISTANCE = 18f;

        private static final float TEXTURE_ANGLE_OFFSET = 0f;

        private float currentScale;
        private static final float MIN_SCALE = 0.6f;
        private static final float MAX_SCALE = 1.4f;
        private static final float POP_PEAK_TIME = 0.3f;  // When max scale is reached (as ratio of duration)

        public ShieldBash(Player player, GameProj gameProj, float duration, Color color) {
            super(duration);
            this.player = player;
            this.gameProj = gameProj;
            this.bashColor = color;
            this.currentScale = MIN_SCALE;

            try {
                this.shieldTexture = Storage.assetManager.get("character/abilities/ShieldAttack.png", Texture.class);
            } catch (Exception e) {
                this.shieldTexture = null;
            }

            // Calculate target angle based on mouse position
            Vector3 mousePos3D = gameProj.getCamera().unproject(
                    new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
            );
            Vector2 playerPos = player.getPosition();
            Vector2 mousePos = new Vector2(mousePos3D.x, mousePos3D.y);
            Vector2 direction = new Vector2(mousePos.x - playerPos.x, mousePos.y - playerPos.y).nor();
            this.targetAngle = direction.angleDeg();
        }

        // Factory methods
        public static ShieldBash createWhite(Player player, GameProj gameProj, float duration) {
            return new ShieldBash(player, gameProj, duration, Color.WHITE);
        }

        public static ShieldBash createGolden(Player player, GameProj gameProj, float duration) {
            return new ShieldBash(player, gameProj, duration, new Color(1f, 0.9f, 0.5f, 1f));
        }

        public static ShieldBash createBlue(Player player, GameProj gameProj, float duration) {
            return new ShieldBash(player, gameProj, duration, new Color(0.5f, 0.7f, 1f, 1f));
        }

        @Override
        protected void onUpdate(float delta) {
            float progress = timer / duration;

            if (progress < POP_PEAK_TIME) {
                float scaleProgress = progress / POP_PEAK_TIME;
                float eased = 1f - (1f - scaleProgress) * (1f - scaleProgress);  // Ease out quad
                currentScale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * eased;
            } else {
                float scaleProgress = (progress - POP_PEAK_TIME) / (1f - POP_PEAK_TIME);
                float eased = scaleProgress * scaleProgress;  // Ease in quad
                currentScale = MAX_SCALE - (MAX_SCALE - MIN_SCALE * 0.8f) * eased;
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 playerPos = player.getPosition();
            float progress = timer / duration;

            float alpha = 1f;
            if (progress > 0.7f) {
                alpha = 1f - (progress - 0.7f) / 0.3f;
            }

            if (shieldTexture != null)
                renderTexturedShield(batch, playerPos, alpha);

            if (progress > POP_PEAK_TIME * 0.8f && progress < POP_PEAK_TIME + 0.2f)
                renderImpactRing(batch, playerPos, alpha * 0.5f);
        }

        private void renderTexturedShield(SpriteBatch batch, Vector2 playerPos, float alpha) {
            batch.setColor(bashColor.r, bashColor.g, bashColor.b, alpha);

            float textureWidth = shieldTexture.getWidth();
            float textureHeight = shieldTexture.getHeight();

            float scaledSize = SHIELD_SIZE * currentScale;
            float aspectRatio = textureHeight / textureWidth;
            float drawWidth = scaledSize;
            float drawHeight = scaledSize * aspectRatio;

            // Position the shield in front of the player
            float angleRad = (float) Math.toRadians(targetAngle);
            float shieldX = playerPos.x + (float) Math.cos(angleRad) * SHIELD_DISTANCE;
            float shieldY = playerPos.y + (float) Math.sin(angleRad) * SHIELD_DISTANCE;

            boolean flipX = (targetAngle > 90 && targetAngle < 270);

            float rotationAngle;
            if (flipX) {
                rotationAngle = targetAngle - 180f;
            } else {
                rotationAngle = targetAngle;
            }

            float drawX = shieldX - drawWidth / 2f;
            float drawY = shieldY - drawHeight / 2f;

            batch.draw(shieldTexture,
                    drawX, drawY,
                    drawWidth / 2f, drawHeight / 2f,
                    drawWidth, drawHeight,
                    1f, 1f,
                    rotationAngle,
                    0, 0,
                    (int)textureWidth, (int)textureHeight,
                    flipX, false);

            batch.setColor(1f, 1f, 1f, 1f);
        }

        private void renderImpactRing(SpriteBatch batch, Vector2 playerPos, float alpha) {
            batch.setColor(bashColor.r, bashColor.g, bashColor.b, alpha);

            float angleRad = (float) Math.toRadians(targetAngle);
            float ringX = playerPos.x + (float) Math.cos(angleRad) * SHIELD_DISTANCE;
            float ringY = playerPos.y + (float) Math.sin(angleRad) * SHIELD_DISTANCE;

            // Expanding ring
            float progress = timer / duration;
            float ringProgress = (progress - POP_PEAK_TIME * 0.8f) / 0.3f;
            ringProgress = Math.min(1f, Math.max(0f, ringProgress));

            float ringRadius = SHIELD_SIZE * 0.5f + (SHIELD_SIZE * 0.8f * ringProgress);
            float ringAlpha = alpha * (1f - ringProgress);

            batch.setColor(bashColor.r, bashColor.g, bashColor.b, ringAlpha);

            int segments = 16;
            float angleStep = 360f / segments;

            for (int i = 0; i < segments; i++) {
                float angle1 = i * angleStep;
                float angle2 = (i + 1) * angleStep;
                float rad1 = (float) Math.toRadians(angle1);
                float rad2 = (float) Math.toRadians(angle2);

                float x1 = ringX + (float) Math.cos(rad1) * ringRadius;
                float y1 = ringY + (float) Math.sin(rad1) * ringRadius;
                float x2 = ringX + (float) Math.cos(rad2) * ringRadius;
                float y2 = ringY + (float) Math.sin(rad2) * ringRadius;

                drawLine(batch, x1, y1, x2, y2, 2f);
            }

            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // BLINK VISUAL
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
    // CHARGE TRAIL VISUAL
    // =========================================================================
    public static class ChargeTrail extends AbilityVisual {
        private Player player;
        private Array<TrailPoint> trailPoints;
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
            this.trailPoints = new Array<>();
        }

        @Override
        protected void onUpdate(float delta) {
            spawnTimer += delta;
            if (spawnTimer >= spawnInterval && timer < duration * 0.8f) {
                trailPoints.add(new TrailPoint(player.getPosition(), trailPointLifetime));
                spawnTimer = 0f;
            }

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

            TextureRegion frame = player.getAnimationManager().getCurrentFrame();
            float size = 16f;

            for (TrailPoint point : trailPoints) {
                float alpha = TRAIL_ALPHA * (point.lifetime / point.maxLifetime);

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
    // PULL CIRCLE VISUAL
    // =========================================================================
    public static class PullCircle extends AbilityVisual {
        private Player player;
        private float maxRadius;
        private float currentRadius;
        private Texture pullTexture;

        private static final int CIRCLE_SEGMENTS = 32;
        private static final Color PULL_COLOR = new Color(0.6f, 0.3f, 1f, 1f);

        public PullCircle(Player player, float radius, float duration) {
            super(duration);
            this.player = player;
            this.maxRadius = radius;
            this.currentRadius = radius;

            try {
                pullTexture = Storage.assetManager.get("character/abilities/Pull.png", Texture.class);
            } catch (Exception e) {
                pullTexture = null;
            }
        }

        @Override
        protected void onUpdate(float delta) {
            float progress = timer / duration;
            currentRadius = maxRadius * (1f - progress);
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float progress = timer / duration;
            float alpha = 0.7f * (1f - progress * 0.3f);

            if (pullTexture != null) {
                float size = currentRadius * 2f;
                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(pullTexture,
                        pos.x - size / 2f,
                        pos.y - size / 2f,
                        size, size);
                batch.setColor(1f, 1f, 1f, 0.3f);
            } else {
                renderCircle(batch, pos, currentRadius, alpha, PULL_COLOR);

                if (currentRadius > 10f) {
                    renderFilledCircle(batch, pos, currentRadius * 0.3f, alpha * 0.3f, PULL_COLOR);
                }

                renderPullLines(batch, pos, currentRadius, alpha);
            }
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
                float angle = i * angleStep + (timer * 180f);
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
    // SMITE VISUAL
    // =========================================================================
    public static class Smite extends AbilityVisual {
        private Player player;
        private Texture texture;
        private float radius;
        private float flashTimer = 0f;

        private static final Color SMITE_COLOR = new Color(1f, 0.9f, 0.4f, 1f);
        private static final float FLASH_SPEED = 15f;

        public Smite(Player player, float radius, float duration) {
            super(duration);
            this.player = player;
            this.radius = radius;

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

            float baseAlpha = 0.7f * (1f - progress);
            float flashAlpha = baseAlpha + 0.2f * (float) Math.sin(flashTimer);

            if (texture != null) {
                batch.setColor(1f, 1f, 1f, flashAlpha);
                float size = radius * 2f;
                batch.draw(texture, pos.x - size / 2f, pos.y - size / 2f, size, size);
                batch.setColor(1f, 1f, 1f, 1f);
            } else {
                renderSmiteCircle(batch, pos, radius, flashAlpha);
            }

            renderSmiteRing(batch, pos, radius, baseAlpha);
        }

        private void renderSmiteCircle(SpriteBatch batch, Vector2 center, float radius, float alpha) {
            batch.setColor(SMITE_COLOR.r, SMITE_COLOR.g, SMITE_COLOR.b, alpha * 0.5f);

            int segments = 32;
            float angleStep = 360f / segments;

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

    // =========================================================================
    // CONSECRATED GROUND VISUAL
    // =========================================================================
    public static class ConsecratedGround extends AbilityVisual {
        private Vector2 position;
        private Texture texture;
        private float radius;

        private static final float VISUAL_DURATION = 0.5f;

        public ConsecratedGround(Player player, float radius, float delay) {
            super(VISUAL_DURATION);
            this.position = new Vector2(player.getPosition());
            this.radius = radius;

            try {
                this.texture = Storage.assetManager.get("character/abilities/Consecrate.png", Texture.class);
            } catch (Exception e) {
                this.texture = null;
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active || texture == null) return;

            float size = radius * 2f;
            batch.draw(texture, position.x - size / 2f, position.y - size / 2f, size, size);
        }
    }

    // =========================================================================
    // SHADOW STEP TRAIL
    // =========================================================================
    public static class ShadowStepTrail extends AbilityVisual {
        private Player player;
        private Array<TrailPoint> trailPoints;
        private float spawnInterval = 0.02f;
        private float spawnTimer = 0f;
        private float trailPointLifetime = 0.25f;

        private static final float TRAIL_ALPHA = 0.5f;
        private static final Color SHADOW_COLOR = new Color(0.3f, 0.1f, 0.4f, 1f);

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

        public ShadowStepTrail(Player player, float duration) {
            super(duration);
            this.player = player;
            this.trailPoints = new Array<>();
        }

        @Override
        protected void onUpdate(float delta) {
            spawnTimer += delta;
            if (spawnTimer >= spawnInterval && timer < duration * 0.8f) {
                trailPoints.add(new TrailPoint(player.getPosition(), trailPointLifetime));
                spawnTimer = 0f;
            }

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

            TextureRegion frame = player.getAnimationManager().getCurrentFrame();
            float size = 16f;

            for (TrailPoint point : trailPoints) {
                float alpha = TRAIL_ALPHA * (point.lifetime / point.maxLifetime);
                batch.setColor(SHADOW_COLOR.r, SHADOW_COLOR.g, SHADOW_COLOR.b, alpha);
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
    }

    // =========================================================================
    // VAULT TRAIL
    // =========================================================================
    public static class VaultTrail extends AbilityVisual {
        private Player player;
        private Array<TrailPoint> trailPoints;
        private float spawnInterval = 0.015f;
        private float spawnTimer = 0f;
        private float trailPointLifetime = 0.3f;

        private static final float TRAIL_ALPHA = 0.6f;
        private static final Color VAULT_COLOR = new Color(1f, 0.9f, 0.5f, 1f);

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

        public VaultTrail(Player player, float duration) {
            super(duration);
            this.player = player;
            this.trailPoints = new Array<>();
        }

        @Override
        protected void onUpdate(float delta) {
            spawnTimer += delta;
            if (spawnTimer >= spawnInterval && timer < duration * 0.9f) {
                trailPoints.add(new TrailPoint(player.getPosition(), trailPointLifetime));
                spawnTimer = 0f;
            }

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

            TextureRegion frame = player.getAnimationManager().getCurrentFrame();
            float size = 16f;

            for (TrailPoint point : trailPoints) {
                float alpha = TRAIL_ALPHA * (point.lifetime / point.maxLifetime);
                batch.setColor(VAULT_COLOR.r, VAULT_COLOR.g, VAULT_COLOR.b, alpha);
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
    }

    // =========================================================================
    // SPRINT AURA
    // =========================================================================
    public static class SprintAura extends AbilityVisual {
        private Player player;
        private float pulseTimer = 0f;

        private static final Color SPRINT_COLOR = new Color(0.3f, 0.5f, 1f, 1f);
        private static final float PULSE_SPEED = 4f;

        public SprintAura(Player player, float duration) {
            super(duration);
            this.player = player;
        }

        @Override
        protected void onUpdate(float delta) {
            pulseTimer += delta * PULSE_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float pulseAlpha = 0.3f + 0.15f * (float) Math.sin(pulseTimer);
            float size = 20f;

            TextureRegion currentFrame = player.getAnimationManager().getCurrentFrame();
            TextureRegion frame = new TextureRegion(currentFrame);

            if (player.isPlayerFlipped()) {
                if (!frame.isFlipX()) {
                    frame.flip(true, false);
                }
            } else {
                if (frame.isFlipX()) {
                    frame.flip(true, false);
                }
            }

            batch.setColor(SPRINT_COLOR.r, SPRINT_COLOR.g, SPRINT_COLOR.b, pulseAlpha);
            batch.draw(frame,
                    pos.x - size / 2f,
                    pos.y - size / 2f,
                    size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // SMOKE BOMB ZONE
    // =========================================================================
    public static class SmokeBombZone extends AbilityVisual {
        private Vector2 position;
        private float radius;
        private Texture smokeTexture;
        private float rotationTimer = 0f;

        private static final Color SMOKE_COLOR = new Color(0.4f, 0.4f, 0.4f, 1f);
        private static final float ROTATION_SPEED = 15f;

        public SmokeBombZone(Vector2 position, float radius, float duration) {
            super(duration);
            this.position = new Vector2(position);
            this.radius = radius;

            try {
                this.smokeTexture = Storage.assetManager.get("character/abilities/SmokeBomb.png", Texture.class);
            } catch (Exception e) {
                this.smokeTexture = null;
            }
        }

        public boolean contains(Vector2 point) {
            if (!active) return false;
            if (point == null) return false;
            return position.dst(point) <= radius;
        }

        public Vector2 getPosition() {
            return position;
        }

        @Override
        protected void onUpdate(float delta) {
            rotationTimer += delta * ROTATION_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            float progress = timer / duration;

            float alpha;
            if (progress < 0.1f) {
                alpha = 0.7f * (progress / 0.1f);
            } else if (progress > 0.7f) {
                alpha = 0.7f * (1f - (progress - 0.7f) / 0.3f);
            } else {
                alpha = 0.7f;
            }

            float pulse = 1f + 0.05f * (float) Math.sin(timer * 3f);
            float currentRadius = radius * pulse;

            if (smokeTexture != null) {
                renderTexturedSmoke(batch, currentRadius, alpha);
            }
        }

        private void renderTexturedSmoke(SpriteBatch batch, float currentRadius, float alpha) {
            float size = currentRadius * 2f;

            // Main smoke layer - slow rotation
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(smokeTexture,
                    position.x - size / 2f,
                    position.y - size / 2f,
                    size / 2f, size / 2f,
                    size, size,
                    1f, 1f,
                    rotationTimer,
                    0, 0,
                    smokeTexture.getWidth(), smokeTexture.getHeight(),
                    false, false);

            float innerSize = size * 0.75f;
            batch.setColor(SMOKE_COLOR.r, SMOKE_COLOR.g, SMOKE_COLOR.b, alpha * 0.5f);
            batch.draw(smokeTexture,
                    position.x - innerSize / 2f,
                    position.y - innerSize / 2f,
                    innerSize / 2f, innerSize / 2f,
                    innerSize, innerSize,
                    1f, 1f,
                    -rotationTimer * 1.5f,
                    0, 0,
                    smokeTexture.getWidth(), smokeTexture.getHeight(),
                    false, false);
        }
    }

    // =========================================================================
    // LIFE LEECH AURA
    // =========================================================================
    public static class LifeLeechAura extends AbilityVisual {
        private Player player;
        private float pulseTimer = 0f;

        private static final Color LEECH_COLOR = new Color(0.89f, 0.1f, 0.2f, 1f);
        private static final Color HEAL_COLOR = new Color(0.2f, 0.8f, 0.3f, 1f);
        private static final float PULSE_SPEED = 5f;

        public LifeLeechAura(Player player, float duration) {
            super(duration);
            this.player = player;
        }

        @Override
        protected void onUpdate(float delta) {
            pulseTimer += delta * PULSE_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float pulse = (float) Math.sin(pulseTimer);
            float alpha = 0.25f + 0.1f * Math.abs(pulse);
            float size = 22f;

            float t = (pulse + 1f) / 2f;
            Color currentColor = new Color(
                    LEECH_COLOR.r * (1 - t) + HEAL_COLOR.r * t,
                    LEECH_COLOR.g * (1 - t) + HEAL_COLOR.g * t,
                    LEECH_COLOR.b * (1 - t) + HEAL_COLOR.b * t,
                    1f
            );

            TextureRegion currentFrame = player.getAnimationManager().getCurrentFrame();
            TextureRegion frame = new TextureRegion(currentFrame);

            if (player.isPlayerFlipped()) {
                if (!frame.isFlipX()) {
                    frame.flip(true, false);
                }
            } else {
                if (frame.isFlipX()) {
                    frame.flip(true, false);
                }
            }

            batch.setColor(currentColor.r, currentColor.g, currentColor.b, alpha);
            batch.draw(frame,
                    pos.x - size / 2f,
                    pos.y - size / 2f,
                    size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // HOLY AURA
    // =========================================================================
    public static class HolyAura extends AbilityVisual {
        private Player player;
        private float radius;
        private float pulseTimer = 0f;
        private Texture holyAuraTexture;

        private static final Color HOLY_COLOR = new Color(1f, 0.9f, 0.4f, 1f);
        private static final int CIRCLE_SEGMENTS = 32;
        private static final float PULSE_SPEED = 3f;

        public HolyAura(Player player, float radius, float duration) {
            super(duration);
            this.player = player;
            this.radius = radius;

            try {
                this.holyAuraTexture = Storage.assetManager.get("character/abilities/HolyAura.png", Texture.class);
            } catch (Exception e) {
                this.holyAuraTexture = null;
            }
        }

        @Override
        protected void onUpdate(float delta) {
            pulseTimer += delta * PULSE_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float progress = timer / duration;
            float pulse = 0.6f + 0.2f * (float) Math.sin(pulseTimer);
            float alpha = pulse * (0.5f - progress * 0.3f);

            float currentRadius = radius * (0.95f + 0.05f * (float) Math.sin(pulseTimer * 2f));

            if (holyAuraTexture != null) {
                float size = currentRadius * 2f;

                batch.setColor(1f, 1f, 1f, alpha);
                batch.draw(holyAuraTexture,
                        pos.x - size / 2f,
                        pos.y - size / 2f,
                        size / 2f, size / 2f,  // origin for rotation
                        size, size,
                        1f, 1f,
                        timer * 30f,  // slow rotation
                        0, 0,
                        holyAuraTexture.getWidth(), holyAuraTexture.getHeight(),
                        false, false);

                batch.setColor(HOLY_COLOR.r, HOLY_COLOR.g, HOLY_COLOR.b, alpha * 0.2f);
                float innerSize = size * 0.7f;
                batch.draw(holyAuraTexture,
                        pos.x - innerSize / 2f,
                        pos.y - innerSize / 2f,
                        innerSize / 2f, innerSize / 2f,
                        innerSize, innerSize,
                        1f, 1f,
                        -timer * 45f,
                        0, 0,
                        holyAuraTexture.getWidth(), holyAuraTexture.getHeight(),
                        false, false);

                batch.setColor(1f, 1f, 1f, 1f);
            } else {
                renderCircle(batch, pos, currentRadius, alpha);
                renderFilledCircle(batch, pos, currentRadius * 0.3f, alpha * 0.3f);
                renderHolyRays(batch, pos, currentRadius, alpha * 0.5f);
            }
        }

        private void renderCircle(SpriteBatch batch, Vector2 center, float radius, float alpha) {
            batch.setColor(HOLY_COLOR.r, HOLY_COLOR.g, HOLY_COLOR.b, alpha);

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

        private void renderFilledCircle(SpriteBatch batch, Vector2 center, float radius, float alpha) {
            batch.setColor(HOLY_COLOR.r, HOLY_COLOR.g, HOLY_COLOR.b, alpha);

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

        private void renderHolyRays(SpriteBatch batch, Vector2 center, float radius, float alpha) {
            batch.setColor(HOLY_COLOR.r, HOLY_COLOR.g, HOLY_COLOR.b, alpha);

            int numRays = 8;
            float angleStep = 360f / numRays;

            for (int i = 0; i < numRays; i++) {
                float angle = i * angleStep + (timer * 30f);
                float rad = (float) Math.toRadians(angle);

                float innerRadius = radius * 0.4f;
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
    // HOLY BLESSING AURA
    // =========================================================================
    public static class HolyBlessingAura extends AbilityVisual {
        private Player player;
        private float pulseTimer = 0f;

        private static final Color BLESSING_COLOR = new Color(0.89f, 0.29f, 0.14f, 1f);
        private static final float PULSE_SPEED = 6f;

        public HolyBlessingAura(Player player, float duration) {
            super(duration);
            this.player = player;
        }

        @Override
        protected void onUpdate(float delta) {
            pulseTimer += delta * PULSE_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float pulse = 0.4f + 0.2f * (float) Math.sin(pulseTimer);
            float size = 24f + 4f * (float) Math.sin(pulseTimer * 0.5f);

            TextureRegion currentFrame = player.getAnimationManager().getCurrentFrame();
            TextureRegion frame = new TextureRegion(currentFrame);

            if (player.isPlayerFlipped()) {
                if (!frame.isFlipX()) {
                    frame.flip(true, false);
                }
            } else {
                if (frame.isFlipX()) {
                    frame.flip(true, false);
                }
            }

            batch.setColor(BLESSING_COLOR.r, BLESSING_COLOR.g, BLESSING_COLOR.b, pulse);
            batch.draw(frame,
                    pos.x - size / 2f,
                    pos.y - size / 2f,
                    size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // HOLY SWORD AURA
    // =========================================================================
    public static class HolySwordAura extends AbilityVisual {
        private Player player;
        private float pulseTimer = 0f;

        private static final Color SWORD_COLOR = new Color(1f, 0.85f, 0.2f, 1f);
        private static final float PULSE_SPEED = 8f;

        public HolySwordAura(Player player, float duration) {
            super(duration);
            this.player = player;
        }

        @Override
        protected void onUpdate(float delta) {
            pulseTimer += delta * PULSE_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();
            float pulse = 0.3f + 0.15f * (float) Math.sin(pulseTimer);
            float size = 20f;

            TextureRegion currentFrame = player.getAnimationManager().getCurrentFrame();
            TextureRegion frame = new TextureRegion(currentFrame);

            if (player.isPlayerFlipped()) {
                if (!frame.isFlipX()) {
                    frame.flip(true, false);
                }
            } else {
                if (frame.isFlipX()) {
                    frame.flip(true, false);
                }
            }

            batch.setColor(SWORD_COLOR.r, SWORD_COLOR.g, SWORD_COLOR.b, pulse);
            batch.draw(frame,
                    pos.x - size / 2f,
                    pos.y - size / 2f,
                    size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // GROUND SLAM VISUAL
    // =========================================================================
    public static class GroundSlam extends AbilityVisual {
        private Player player;
        private Texture texture;
        private float radius;
        private float flashTimer = 0f;

        private static final float FLASH_SPEED = 10f;

        public GroundSlam(Player player, float radius, float duration) {
            super(duration);
            this.player = player;
            this.radius = radius;

            try {
                this.texture = Storage.assetManager.get("character/abilities/GroundSlam.png", Texture.class);
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
            if (!active || texture == null) return;

            Vector2 pos = player.getPosition();
            float progress = timer / duration;

            // Flash in quickly, then fade out
            float alpha;
            if (progress < 0.2f) {
                alpha = 0.9f * (progress / 0.2f);
            } else {
                alpha = 0.9f * (1f - (progress - 0.2f) / 0.8f);
            }

            float size = radius * 2f;

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(texture, pos.x - size / 2f, pos.y - size / 2f, size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    // =========================================================================
    // WHIRLWIND VISUAL - Rotating spear around player
    // =========================================================================
    public static class Whirlwind extends AbilityVisual {
        private Player player;
        private GameProj gameProj;
        private Texture spearTexture;
        private float radius;
        private float rotationAngle = 0f;

        private static final float ROTATIONS_PER_SECOND = 3f;
        private static final float SPEAR_DISTANCE = 20f;

        public Whirlwind(Player player, float radius, float duration) {
            super(duration);
            this.player = player;
            this.radius = radius;

            try {
                this.spearTexture = Storage.assetManager.get("icons/gear/spearVisual.png", Texture.class);
            } catch (Exception e) {
                System.err.println("Failed to load spearVisual.png for Whirlwind");
                this.spearTexture = null;
            }
        }

        @Override
        protected void onUpdate(float delta) {
            // Rotate spear - 3 full rotations per second = 1080 degrees per second
            rotationAngle += 360f * ROTATIONS_PER_SECOND * delta;
            if (rotationAngle >= 360f) {
                rotationAngle -= 360f;
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active || spearTexture == null || player == null || player.getBody() == null) return;

            Color originalColor = batch.getColor().cpy();

            Vector2 playerPos = player.getPosition();

            // Calculate spear position in circular orbit
            float angleRad = (float) Math.toRadians(rotationAngle);
            float spearX = playerPos.x + (float) Math.cos(angleRad) * SPEAR_DISTANCE;
            float spearY = playerPos.y + (float) Math.sin(angleRad) * SPEAR_DISTANCE;

            float width = spearTexture.getWidth();
            float height = spearTexture.getHeight();

            float scaleX = 0.8f; // Normal size spear
            float scaleY = 0.6f; // Keep it thinner

            float originX = 0f;
            float originY = height / 2f;

            // Fade in and out
            float progress = timer / duration;
            float alpha;
            if (progress < 0.2f) {
                alpha = progress / 0.2f;
            } else if (progress > 0.8f) {
                alpha = 1f - (progress - 0.8f) / 0.2f;
            } else {
                alpha = 1f;
            }

            batch.setColor(1f, 1f, 1f, alpha * 0.9f);

            TextureRegion spearRegion = new TextureRegion(spearTexture);

            batch.draw(
                    spearRegion,
                    spearX,
                    spearY - originY,
                    originX,
                    originY,
                    width,
                    height,
                    scaleX,
                    scaleY,
                    rotationAngle // The spear points in the direction it's traveling
            );

            batch.setColor(originalColor);
        }
    }

    // =========================================================================
    // BLAZING FURY AURA - Flashing red aura on player
    // =========================================================================
    public static class BlazingFuryAura extends AbilityVisual {
        private Player player;
        private float pulseTimer = 0f;

        private static final Color FURY_COLOR = new Color(1f, 0.2f, 0.1f, 1f); // Bright red
        private static final float PULSE_SPEED = 8f; // Fast pulsing

        public BlazingFuryAura(Player player, float duration) {
            super(duration);
            this.player = player;
        }

        @Override
        protected void onUpdate(float delta) {
            pulseTimer += delta * PULSE_SPEED;
        }

        @Override
        public void render(SpriteBatch batch) {
            if (!active) return;

            Vector2 pos = player.getPosition();

            // Fast flashing pulse between 0.3 and 0.5 alpha
            float pulse = 0.4f + 0.2f * (float) Math.sin(pulseTimer);
            float size = 22f;

            TextureRegion currentFrame = player.getAnimationManager().getCurrentFrame();
            TextureRegion frame = new TextureRegion(currentFrame);

            // Handle sprite flipping
            if (player.isPlayerFlipped()) {
                if (!frame.isFlipX()) {
                    frame.flip(true, false);
                }
            } else {
                if (frame.isFlipX()) {
                    frame.flip(true, false);
                }
            }

            batch.setColor(FURY_COLOR.r, FURY_COLOR.g, FURY_COLOR.b, pulse);
            batch.draw(frame,
                    pos.x - size / 2f,
                    pos.y - size / 2f,
                    size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }
}