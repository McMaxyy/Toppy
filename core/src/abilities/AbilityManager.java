package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import config.Storage;
import entities.Herman;
import entities.Player;
import entities.PlayerClass;
import game.GameProj;
import items.Item;
import managers.Equipment;
import managers.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages player abilities, cooldowns, and status effects
 * Now integrated with SkillTree for dynamic ability slotting
 */
public class AbilityManager {
    private static final int NUM_ABILITY_SLOTS = 5;

    private Ability[] abilities;
    private Player player;
    private GameProj gameProj;
    private PlayerClass playerClass;
    private SkillTree skillTree;

    private Map<String, Ability> abilityRegistry;

    private Map<Object, List<StatusEffect>> activeEffects;

    private List<AbilityVisual> activeVisuals;

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    private static final int SLOT_SIZE = 45;
    private static final int SLOT_PADDING = 5;
    private static final int SEPARATOR_SPACE = 15;

    private float offhandCooldown = 0f;
    private static final float OFFHAND_COOLDOWN_TIME = 2.0f;
    private static final int SHIELD_BASH_DAMAGE = 40;

    private float swordCooldown = 0f;
    private static final float SWORD_COOLDOWN_TIME = 0.5f;
    private static final float SWORD_ATTACK_RANGE = 45f;

    public AbilityManager(Player player, GameProj gameProj, PlayerClass playerClass) {
        this.player = player;
        this.gameProj = gameProj;
        this.playerClass = playerClass;
        this.abilities = new Ability[NUM_ABILITY_SLOTS];
        this.activeEffects = new HashMap<>();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.activeVisuals = new ArrayList<>();
        this.abilityRegistry = new HashMap<>();

        this.skillTree = new SkillTree(player, playerClass);

        initializeAbilityRegistry();
    }

    private void initializeAbilityRegistry() {
        // Load textures
        Texture blinkIcon = loadIcon("icons/abilities/Blink.png");
        Texture chargeIcon = loadIcon("icons/abilities/Charge.png");
        Texture bubbleIcon = loadIcon("icons/abilities/Bubble.png");
        Texture pullIcon = loadIcon("icons/abilities/Pull.png");
        Texture holyAuraIcon = loadIcon("icons/abilities/HolyAura.png");
        Texture smiteIcon = loadIcon("icons/abilities/Smite.png");
        Texture prayerIcon = loadIcon("icons/abilities/Prayer.png");
        Texture consecrateIcon = loadIcon("icons/abilities/ConsecratedGround.png");
        Texture defaultIcon = loadIcon("icons/abilities/DoubleSwing.png");
        Texture rendIcon = loadIcon("icons/abilities/Rend.png");
        Texture doubleSwingIcon = loadIcon("icons/abilities/DoubleSwing.png");

        // Movement abilities
        abilityRegistry.put("blink", new PaladinBlinkAbility(blinkIcon));
        abilityRegistry.put("charge", new ChargeAbility(chargeIcon));
        abilityRegistry.put("shadow_step", new ShadowStepAbility(defaultIcon));
        abilityRegistry.put("vault", new VaultAbility(defaultIcon));

        // Utility abilities
        abilityRegistry.put("bubble", new PaladinBubbleAbility(bubbleIcon));
        abilityRegistry.put("pull", new PullAbility(pullIcon));
        abilityRegistry.put("sprint", new SprintAbility(defaultIcon));
        abilityRegistry.put("full_heal", new FullHealAbility(prayerIcon));
        abilityRegistry.put("smoke_bomb", new SmokeBombAbility(defaultIcon));
        abilityRegistry.put("life_leech", new LifeLeechAbility(defaultIcon));

        // Class abilities - Paladin
        if (playerClass == PlayerClass.PALADIN) {
            abilityRegistry.put("smite", new SmiteAbility(smiteIcon));
            abilityRegistry.put("prayer", new PaladinPrayerAbility(prayerIcon));
            abilityRegistry.put("consecrated_ground", new ConsecratedGroundAbility(consecrateIcon));
            abilityRegistry.put("holy_aura", new HolyAuraAbility(holyAuraIcon));
            abilityRegistry.put("holy_blessing", new HolyBlessingAbility(defaultIcon));
            abilityRegistry.put("holy_sword", new HolySwordAbility(defaultIcon));
        }

        // Class abilities - Mercenary
        if (playerClass == PlayerClass.MERCENARY) {
            abilityRegistry.put("double_swing", new DoubleSwingAbility(doubleSwingIcon));
            abilityRegistry.put("rend", new RendAbility(rendIcon));
        }
    }

    private Texture loadIcon(String path) {
        try {
            return Storage.assetManager.get(path, Texture.class);
        } catch (Exception e) {
            return Storage.assetManager.get("icons/abilities/DoubleSwing.png", Texture.class);
        }
    }

    private void syncAbilitiesWithSkillTree() {
        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            String skillId = skillTree.getSlottedSkillId(i);
            if (skillId != null) {
                abilities[i] = abilityRegistry.get(skillId);
            } else {
                abilities[i] = null;
            }
        }
    }

    public void update(float delta) {
        // Sync abilities with skill tree
        syncAbilitiesWithSkillTree();

        // Update abilities
        for (Ability ability : abilities) {
            if (ability != null) {
                ability.update(delta);
            }
        }

        if (offhandCooldown > 0) {
            offhandCooldown -= delta;
        }

        if (swordCooldown > 0) {
            swordCooldown -= delta;
        }

        // Update status effects
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

            if (effects.isEmpty()) {
                activeEffects.remove(entry.getKey());
            }
        }

        updateVisualEffects(delta);

        // Update skill tree
        skillTree.update(delta, gameProj);
    }

    private void updateVisualEffects(float delta) {
        for (int i = activeVisuals.size() - 1; i >= 0; i--) {
            AbilityVisual visual = activeVisuals.get(i);
            visual.update(delta);
            if (!visual.isActive()) {
                visual.dispose();
                activeVisuals.remove(i);
            }
        }
    }

    public void handleInput() {
        // Don't handle ability input if skill tree is open
        if (skillTree.isOpen()) {
            return;
        }

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

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            useOffhandAttack();
        }

        if (playerClass == PlayerClass.PALADIN && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (!gameProj.isPaused() && !player.getInventory().isOpen() && !skillTree.isOpen()) {
                performSwordAttack();
            }
        }
    }

    private void useAbility(int slot) {
        if (slot >= 0 && slot < NUM_ABILITY_SLOTS && abilities[slot] != null) {
            boolean success = abilities[slot].use(player, gameProj);
            if (!success && abilities[slot].isOnCooldown()) {
                System.out.println(abilities[slot].getName() + " is on cooldown!");
            }
        }
    }

    private void useOffhandAttack() {
        if (offhandCooldown > 0) { return; }

        Item offhand = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND);

        if (offhand != null && (offhand.getName().toLowerCase().contains("shield"))) {
            performShieldBash();
            offhandCooldown = OFFHAND_COOLDOWN_TIME;
        }
    }

    private void performSwordAttack() {
        if (swordCooldown > 0) { return; }

        SoundManager.getInstance().playHitSound();

        // Check for Holy Sword buff - bigger cone
        float attackRange = SWORD_ATTACK_RANGE;
        if (player.isHolySwordActive()) {
            attackRange *= player.getHolySwordConeMultiplier();
        }

        player.addAbilityVisual(AbilityVisual.ConalAttack.createGolden(player, gameProj, 0.15f, attackRange));

        com.badlogic.gdx.math.Vector2 playerPos = player.getPosition();
        int playerDamage = player.getStats().getTotalDamage();

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
                        enemy.takeDamage(playerDamage);
                        player.onBasicAttackHit(); // Life leech
                    }
                }
            }
        }

        if (gameProj.isHermanSpawned()) {
            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = herman.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    herman.takeDamage(playerDamage);
                    player.onBasicAttackHit();
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = hermanDuplicate.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    hermanDuplicate.takeDamage(playerDamage);
                    player.onBasicAttackHit();
                }
            }
        }

        if (gameProj.getCurrentDungeon() != null) {
            for (entities.DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(playerDamage);
                        player.onBasicAttackHit();
                    }
                }
            }
        }

        if (gameProj.getCurrentBossRoom() != null) {
            entities.BossKitty boss = gameProj.getCurrentBossRoom().getBoss();
            if (boss != null && boss.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = boss.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    boss.takeDamage(playerDamage);
                    player.onBasicAttackHit();
                }
            }

            entities.Cyclops cyclops = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclops != null && cyclops.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = cyclops.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    cyclops.takeDamage(playerDamage);
                    player.onBasicAttackHit();
                }
            }

            entities.GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = ghostBoss.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    ghostBoss.takeDamage(playerDamage);
                    player.onBasicAttackHit();
                }
            }
        }

        swordCooldown = SWORD_COOLDOWN_TIME;
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
                        player.onBasicAttackHit();
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
                        player.onBasicAttackHit();
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
                        player.onBasicAttackHit();
                    }
                }
            }
        }

        if (gameProj.getCurrentBossRoom() != null) {
            entities.BossKitty boss = gameProj.getCurrentBossRoom().getBoss();
            if (boss != null && boss.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = boss.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    boss.takeDamage(SHIELD_BASH_DAMAGE);
                    StunEffect stun = new StunEffect(boss, 0.5f);
                    stun.onApply();
                    addStatusEffect(boss, stun);
                    player.onBasicAttackHit();
                }
            }

            entities.Cyclops cyclops = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclops != null && cyclops.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = cyclops.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    cyclops.takeDamage(SHIELD_BASH_DAMAGE);
                    StunEffect stun = new StunEffect(cyclops, 0.5f);
                    stun.onApply();
                    addStatusEffect(cyclops, stun);
                    player.onBasicAttackHit();
                }
            }

            entities.GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = ghostBoss.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    ghostBoss.takeDamage(SHIELD_BASH_DAMAGE);
                    StunEffect stun = new StunEffect(ghostBoss, 0.5f);
                    stun.onApply();
                    addStatusEffect(ghostBoss, stun);
                    player.onBasicAttackHit();
                }
            }
        }
    }

    public void addStatusEffect(Object target, StatusEffect effect) {
        if (!activeEffects.containsKey(target)) {
            activeEffects.put(target, new ArrayList<>());
        }
        activeEffects.get(target).add(effect);
    }

    public void addAbilityVisual(AbilityVisual visual) {
        activeVisuals.add(visual);
    }

    public void renderAbilityEffects(SpriteBatch batch) {
        for (AbilityVisual visual : activeVisuals) {
            visual.render(batch);
        }
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public void renderSkillTree(SpriteBatch batch) {
        skillTree.render(batch);
    }

    public void renderSkillBar(SpriteBatch batch) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        int totalSlots = 7;
        int totalWidth = (totalSlots * SLOT_SIZE) + ((totalSlots - 1) * SLOT_PADDING) + (2 * SEPARATOR_SPACE);

        float startX = (screenWidth - totalWidth) / 2f;
        float startY = 35f;

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Render ability slots (1-5)
        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            float slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);
            renderAbilitySlot(slotX, startY, i);
        }

        // Separator space
        float inventoryStartX = startX + (NUM_ABILITY_SLOTS * (SLOT_SIZE + SLOT_PADDING)) + SEPARATOR_SPACE;
        float attackStartX = inventoryStartX;

        // Render LMB and RMB slots
        renderAttackSlot(attackStartX, startY, "LMB", player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.WEAPON));
        renderAttackSlot(attackStartX + SLOT_SIZE + SLOT_PADDING, startY, "RMB",
                player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND));

        shapeRenderer.end();

        batch.begin();

        // Render ability icons and cooldowns
        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            float slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);
            renderAbilityIcon(batch, slotX, startY, i);
        }

        // Render equipped weapon/offhand icons
        Item weapon = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.WEAPON);
        if (weapon != null) {
            weapon.renderIcon(batch, attackStartX + 3, startY + 3, SLOT_SIZE - 6);
        }

        // Render sword cooldown for Paladin
        if (playerClass == PlayerClass.PALADIN && swordCooldown > 0) {
            renderCooldownOverlay(batch, attackStartX, startY, swordCooldown / SWORD_COOLDOWN_TIME);
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
        } else if (ability != null) {
            shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 0.8f); // Normal
        } else {
            shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 0.6f); // Empty slot
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);

        // Border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        if (ability != null) {
            shapeRenderer.setColor(0.6f, 0.6f, 0.7f, 1f);
        } else {
            shapeRenderer.setColor(0.4f, 0.4f, 0.5f, 0.6f);
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glLineWidth(1);
    }

    private void renderAttackSlot(float x, float y, String label, Item equippedItem) {
        // Background
        if (label.equals("RMB") && offhandCooldown > 0) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.8f);
        } else if (label.equals("LMB") && playerClass == PlayerClass.PALADIN && swordCooldown > 0) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.8f);
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

    private void renderAbilityIcon(SpriteBatch batch, float x, float y, int index) {
        Ability ability = abilities[index];
        int keyNumber = index == 0 ? 1 : index + 1; // Slot 0 = key 1 (SPACE), but display as 1

        // Render key number (always show, even for empty slots)
        font.setColor(ability != null ? Color.WHITE : Color.GRAY);
        font.getData().setScale(0.6f);

        // Display key binding
        String keyLabel = index == 0 ? "SP" : String.valueOf(index + 1);
        font.draw(batch, keyLabel, x + 3, y + SLOT_SIZE - 3);
        font.getData().setScale(1.0f);

        if (ability == null) {
            // Empty slot - show "+" or empty indicator
            font.setColor(0.5f, 0.5f, 0.5f, 0.5f);
            font.getData().setScale(1.2f);
            font.draw(batch, "+", x + SLOT_SIZE / 2f - 8, y + SLOT_SIZE / 2f + 10);
            font.getData().setScale(1.0f);
            return;
        }

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

    public Map<Object, List<StatusEffect>> getActiveEffects() {
        return activeEffects;
    }

    public void dispose() {
        shapeRenderer.dispose();
        skillTree.dispose();

        for (AbilityVisual visual : activeVisuals) {
            visual.dispose();
        }
        activeVisuals.clear();
    }
}