package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import config.Storage;
import entities.Player;
import game.GameProj;
import items.Item;
import managers.Equipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages player abilities, cooldowns, and status effects
 */
public class AbilityManager {
    private static final int NUM_ABILITY_SLOTS = 5;

    private Ability[] abilities;
    private Player player;
    private GameProj gameProj;

    // Status effects tracking
    private Map<Object, List<StatusEffect>> activeEffects;

    // Visual effects list (unified)
    private List<AbilityVisual> activeVisuals;

    // UI components
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    // Skill bar settings
    private static final int SLOT_SIZE = 45;
    private static final int SLOT_PADDING = 5;
    private static final int SEPARATOR_SPACE = 15;

    // Offhand attack cooldown
    private float offhandCooldown = 0f;
    private static final float OFFHAND_COOLDOWN_TIME = 2.0f;
    private static final int SHIELD_BASH_DAMAGE = 40;

    public AbilityManager(Player player, GameProj gameProj) {
        this.player = player;
        this.gameProj = gameProj;
        this.abilities = new Ability[NUM_ABILITY_SLOTS];
        this.activeEffects = new HashMap<>();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);

        this.activeVisuals = new ArrayList<>();

        initializeMercenaryAbilities();
    }

    /**
     * Initialize the 5 Mercenary abilities
     */
    private void initializeMercenaryAbilities() {
        abilities[0] = new ChargeAbility(Storage.assetManager.get("icons/abilities/Charge.png", Texture.class));
        abilities[1] = new DoubleSwingAbility(Storage.assetManager.get("icons/abilities/DoubleSwing.png", Texture.class));
        abilities[2] = new BubbleAbility(Storage.assetManager.get("icons/abilities/Bubble.png", Texture.class));
        abilities[3] = new RendAbility(Storage.assetManager.get("icons/abilities/Rend.png", Texture.class));
        abilities[4] = new PrayerAbility(Storage.assetManager.get("icons/abilities/Prayer.png", Texture.class));
    }

    /**
     * Update all abilities and status effects
     */
    public void update(float delta) {
        // Update ability cooldowns
        for (Ability ability : abilities) {
            if (ability != null) {
                ability.update(delta);
            }
        }

        // Update offhand cooldown
        if (offhandCooldown > 0) {
            offhandCooldown -= delta;
        }

        // Update all status effects
        for (Map.Entry<Object, List<StatusEffect>> entry : new HashMap<>(activeEffects).entrySet()) {
            List<StatusEffect> effects = entry.getValue();
            List<StatusEffect> toRemove = new ArrayList<>();

            for (StatusEffect effect : effects) {
                effect.onUpdate(delta);
                if (!effect.update(delta)) {
                    toRemove.add(effect);
                }
            }

            effects.removeAll(toRemove);

            // Remove empty lists
            if (effects.isEmpty()) {
                activeEffects.remove(entry.getKey());
            }
        }

        // Update visual effects
        updateVisualEffects(delta);
    }

    /**
     * Update all visual effects
     */
    private void updateVisualEffects(float delta) {
        // Update all visuals and remove inactive ones
        for (int i = activeVisuals.size() - 1; i >= 0; i--) {
            AbilityVisual visual = activeVisuals.get(i);
            visual.update(delta);
            if (!visual.isActive()) {
                visual.dispose();
                activeVisuals.remove(i);
            }
        }
    }

    /**
     * Handle ability input (keys 1-5, RMB)
     */
    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            useAbility(0);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            useAbility(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            useAbility(2);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            useAbility(3);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            useAbility(4);
        }

        // Check RMB for offhand attack
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            useOffhandAttack();
        }
    }

    /**
     * Use ability at slot index
     */
    private void useAbility(int slot) {
        if (slot >= 0 && slot < NUM_ABILITY_SLOTS && abilities[slot] != null) {
            boolean success = abilities[slot].use(player, gameProj);
            if (!success && abilities[slot].isOnCooldown()) {
                System.out.println(abilities[slot].getName() + " is on cooldown!");
            }
        }
    }

    /**
     * Use offhand attack (shield bash if shield equipped)
     */
    private void useOffhandAttack() {
        if (offhandCooldown > 0) { return; }

        Item offhand = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND);

        if (offhand != null && (offhand.getName().toLowerCase().contains("shield"))) {
            performShieldBash();
            offhandCooldown = OFFHAND_COOLDOWN_TIME;
        }
    }

    private void performShieldBash() {
        player.addAbilityVisual(AbilityVisual.ConalAttack.createWhite(player, gameProj, 0.1f, 30f));

        com.badlogic.gdx.math.Vector2 playerPos = player.getPosition();
        float attackRange = 30f;

        com.badlogic.gdx.math.Vector3 mousePos3D = gameProj.getCamera().unproject(
                new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        com.badlogic.gdx.math.Vector2 mousePos = new com.badlogic.gdx.math.Vector2(mousePos3D.x, mousePos3D.y);
        com.badlogic.gdx.math.Vector2 attackDir = new com.badlogic.gdx.math.Vector2(
                mousePos.x - playerPos.x, mousePos.y - playerPos.y
        ).nor();

        for (managers.Chunk chunk : gameProj.getChunks().values()) {
            for (entities.Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(SHIELD_BASH_DAMAGE);
                        StunEffect stun = new StunEffect(enemy, 0.5f);
                        stun.onApply();
                        addStatusEffect(enemy, stun);
                    }
                }
            }

            for (entities.BossKitty boss : new ArrayList<>(chunk.getBossKitty())) {
                if (boss.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = boss.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        boss.takeDamage(SHIELD_BASH_DAMAGE);
                        StunEffect stun = new StunEffect(boss, 0.5f);
                        stun.onApply();
                        addStatusEffect(boss, stun);
                    }
                }
            }
        }

        // Check dungeon enemies
        if (gameProj.getCurrentDungeon() != null) {
            for (entities.DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(SHIELD_BASH_DAMAGE);
                        StunEffect stun = new StunEffect(enemy, 0.5f);
                        stun.onApply();
                        addStatusEffect(enemy, stun);
                    }
                }
            }
        }
    }

    /**
     * Add status effect to target
     */
    public void addStatusEffect(Object target, StatusEffect effect) {
        if (!activeEffects.containsKey(target)) {
            activeEffects.put(target, new ArrayList<>());
        }
        activeEffects.get(target).add(effect);
    }

    /**
     * Add any ability visual effect
     */
    public void addAbilityVisual(AbilityVisual visual) {
        activeVisuals.add(visual);
    }

    /**
     * Render ability visual effects (called from world coordinates)
     */
    public void renderAbilityEffects(SpriteBatch batch) {
        for (AbilityVisual visual : activeVisuals) {
            visual.render(batch);
        }
    }

    /**
     * Render the skill bar UI
     */
    public void renderSkillBar(SpriteBatch batch) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Calculate total width: 5 abilities + space + 3 inventory + space + 2 attacks
        int totalSlots = 10;
        int totalWidth = (totalSlots * SLOT_SIZE) + ((totalSlots - 1) * SLOT_PADDING) + (2 * SEPARATOR_SPACE);

        float startX = (screenWidth - totalWidth) / 2f;
        float startY = 20; // Bottom of screen

        // End batch for shape rendering
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Render ability slots (1-5)
        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            float slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);
            renderAbilitySlot(slotX, startY, i);
        }

        // Separator space
        float inventoryStartX = startX + (NUM_ABILITY_SLOTS * (SLOT_SIZE + SLOT_PADDING)) + SEPARATOR_SPACE;

        // Render inventory quick slots (6-8)
        for (int i = 0; i < 3; i++) {
            float slotX = inventoryStartX + i * (SLOT_SIZE + SLOT_PADDING);
            renderInventorySlot(slotX, startY, i);
        }

        // Separator space
        float attackStartX = inventoryStartX + (3 * (SLOT_SIZE + SLOT_PADDING)) + SEPARATOR_SPACE;

        // Render LMB and RMB slots
        renderAttackSlot(attackStartX, startY, "LMB", player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.WEAPON));
        renderAttackSlot(attackStartX + SLOT_SIZE + SLOT_PADDING, startY, "RMB",
                player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND));

        shapeRenderer.end();

        // Resume batch for icon rendering
        batch.begin();

        // Render ability icons and cooldowns
        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            if (abilities[i] != null) {
                float slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);
                renderAbilityIcon(batch, slotX, startY, abilities[i], i + 1);
            }
        }

        // Render inventory item icons
        for (int i = 0; i < 3; i++) {
            Item item = player.getInventory().getItem(i);
            if (item != null) {
                float slotX = inventoryStartX + i * (SLOT_SIZE + SLOT_PADDING);
                item.renderIcon(batch, slotX + 3, startY + 3, SLOT_SIZE - 6);
            }
        }

        // Render equipped weapon/offhand icons
        Item weapon = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.WEAPON);
        if (weapon != null) {
            weapon.renderIcon(batch, attackStartX + 3, startY + 3, SLOT_SIZE - 6);
        }

        Item offhand = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND);
        if (offhand != null) {
            offhand.renderIcon(batch, attackStartX + SLOT_SIZE + SLOT_PADDING + 3, startY + 3, SLOT_SIZE - 6);

            // Render offhand cooldown
            if (offhandCooldown > 0) {
                renderCooldownOverlay(batch, attackStartX + SLOT_SIZE + SLOT_PADDING, startY,
                        offhandCooldown / OFFHAND_COOLDOWN_TIME);
            }
        }
    }

    private void renderAbilitySlot(float x, float y, int index) {
        Ability ability = abilities[index];

        // Background
        if (ability != null && ability.isCasting()) {
            shapeRenderer.setColor(0.5f, 0.8f, 0.5f, 0.8f); // Green while casting
        } else if (ability != null && ability.isOnCooldown()) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.8f); // Dark when on cooldown
        } else {
            shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 0.8f); // Normal
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);

        // Border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(0.6f, 0.6f, 0.7f, 1f);
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glLineWidth(1);
    }

    private void renderInventorySlot(float x, float y, int index) {
        // Background
        shapeRenderer.setColor(0.25f, 0.25f, 0.35f, 0.8f);
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);

        // Border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(0.5f, 0.5f, 0.6f, 1f);
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glLineWidth(1);
    }

    private void renderAttackSlot(float x, float y, String label, Item equippedItem) {
        // Background
        if (label.equals("RMB") && offhandCooldown > 0) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.8f); // Dark when on cooldown
        } else {
            shapeRenderer.setColor(0.35f, 0.3f, 0.3f, 0.8f);
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);

        // Border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(0.7f, 0.6f, 0.6f, 1f);
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glLineWidth(1);
    }

    private void renderAbilityIcon(SpriteBatch batch, float x, float y, Ability ability, int keyNumber) {
        // Render icon
        if (ability.getIconTexture() != null) {
            batch.draw(ability.getIconTexture(), x + 3, y + 3, SLOT_SIZE - 6, SLOT_SIZE - 6);
        }

        // Render cooldown overlay
        if (ability.isOnCooldown()) {
            renderCooldownOverlay(batch, x, y, ability.getCooldownPercentage());
        }

        // Render cast bar
        if (ability.isCasting()) {
            renderCastBar(batch, x, y, ability.getCastProgress());
        }

        // Render key number
        font.setColor(Color.WHITE);
        font.getData().setScale(0.6f);
        font.draw(batch, String.valueOf(keyNumber), x + 5, y + SLOT_SIZE - 5);
        font.getData().setScale(1.0f);

        // Render cooldown time
        if (ability.isOnCooldown()) {
            font.setColor(Color.YELLOW);
            font.getData().setScale(0.8f);
            String cooldownText = String.format("%.1f", ability.getCurrentCooldown());
            float textWidth = font.getSpaceXadvance() * cooldownText.length() * 0.8f;
            font.draw(batch, cooldownText, x + (SLOT_SIZE - textWidth) / 2, y + SLOT_SIZE / 2 + 5);
            font.getData().setScale(1.0f);
        }
    }

    private void renderCooldownOverlay(SpriteBatch batch, float x, float y, float percentage) {
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        float overlayHeight = SLOT_SIZE * percentage;
        shapeRenderer.rect(x, y, SLOT_SIZE, overlayHeight);
        shapeRenderer.end();
        batch.begin();
    }

    private void renderCastBar(SpriteBatch batch, float x, float y, float progress) {
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(x + 2, y - 8, SLOT_SIZE - 4, 6);

        // Progress
        shapeRenderer.setColor(0.3f, 0.8f, 0.3f, 1f);
        float barWidth = (SLOT_SIZE - 4) * progress;
        shapeRenderer.rect(x + 2, y - 8, barWidth, 6);

        shapeRenderer.end();
        batch.begin();
    }

    public void dispose() {
        shapeRenderer.dispose();

        // Dispose all visual effects
        for (AbilityVisual visual : activeVisuals) {
            visual.dispose();
        }
        activeVisuals.clear();
    }
}