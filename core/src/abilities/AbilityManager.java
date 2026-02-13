package abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import config.Storage;
import entities.DestructibleObject;
import entities.Herman;
import entities.Player;
import entities.PlayerClass;
import game.GameProj;
import items.Item;
import managers.Equipment;
import managers.SoundManager;
import config.SaveManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbilityManager {
    private static final int NUM_ABILITY_SLOTS = 5;
    private static final int NUM_CONSUMABLE_SLOTS = 2;

    private final Ability[] abilities;
    private final Player player;
    private final GameProj gameProj;
    private final PlayerClass playerClass;
    private final SkillTree skillTree;

    private final Map<String, Ability> abilityRegistry;
    private final Map<Object, List<StatusEffect>> activeEffects;
    private final List<AbilityVisual> activeVisuals;

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    private static final int SLOT_SIZE = 55;
    private static final int SLOT_PADDING = 5;
    private static final int SEPARATOR_SPACE = 15;

    private final Item[] consumableSlots;
    private Item draggingConsumable = null;
    private int hoveredConsumableSlot = -1;

    private float offhandCooldown = 0f;
    private static final float BASE_OFFHAND_COOLDOWN_TIME = 2.0f;
    private static final int BASE_SHIELD_BASH_DAMAGE = 10;

    private float swordCooldown = 0f;
    private static final float BASE_SWORD_COOLDOWN_TIME = 0.5f;
    private static final float SWORD_ATTACK_RANGE = 45f;

    private float spearCooldown = 0f;
    private static final float BASE_SPEAR_COOLDOWN_TIME = 0.5f;
    private static final float SPEAR_ATTACK_RANGE = 65f;

    private int hoveredSlotIndex = -1;
    private SkillTree.Skill hoveredSlotSkill = null;

    public AbilityManager(Player player, GameProj gameProj, PlayerClass playerClass) {
        this.player = player;
        this.gameProj = gameProj;
        this.playerClass = playerClass;
        this.abilities = new Ability[NUM_ABILITY_SLOTS];
        this.consumableSlots = new Item[NUM_CONSUMABLE_SLOTS];
        this.activeEffects = new HashMap<>();
        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.activeVisuals = new ArrayList<>();
        this.abilityRegistry = new HashMap<>();

        this.skillTree = new SkillTree(player, playerClass);

        initializeAbilityRegistry();
    }

    private void initializeAbilityRegistry() {
        Texture blinkIcon = loadIcon("icons/abilities/Blink.png");
        Texture chargeIcon = loadIcon("icons/abilities/Charge.png");
        Texture bubbleIcon = loadIcon("icons/abilities/Bubble.png");
        Texture pullIcon = loadIcon("icons/abilities/Pull.png");
        Texture holyAuraIcon = loadIcon("icons/abilities/HolyAura.png");
        Texture smiteIcon = loadIcon("icons/abilities/Smite.png");
        Texture prayerIcon = loadIcon("icons/abilities/Prayer.png");
        Texture consecrateIcon = loadIcon("icons/abilities/ConsecratedGround.png");
        Texture rendIcon = loadIcon("icons/abilities/Rend.png");
        Texture doubleSwingIcon = loadIcon("icons/abilities/DoubleSwing.png");
        Texture smokeBombIcon = loadIcon(("icons/abilities/SmokeBomb.png"));
        Texture holySwordIcon = loadIcon(("icons/abilities/HolySword.png"));
        Texture sprintIcon = loadIcon(("icons/abilities/Sprint.png"));
        Texture groundSlamIcon = loadIcon(("icons/abilities/GroundSlam.png"));
        Texture whirlwindIcon = loadIcon(("icons/abilities/Whirlwind.png"));
        Texture executeIcon = loadIcon(("icons/abilities/Execute.png"));
        Texture blazingFuryIcon = loadIcon(("icons/abilities/BlazingFury.png"));
        Texture shadowStepIcon = loadIcon(("icons/abilities/ShadowStep.png"));
        Texture vaultIcon = loadIcon(("icons/abilities/Vault.png"));
        Texture lifeLeechIcon = loadIcon(("icons/abilities/LifeLeech.png"));
        Texture healIcon = loadIcon(("icons/abilities/Heal.png"));
        Texture holyBlessingIcon = loadIcon(("icons/abilities/HolyBlessing.png"));

        // Movement abilities
        abilityRegistry.put("shadow_step", new ShadowStepAbility(shadowStepIcon));
        abilityRegistry.put("blink", new PaladinBlinkAbility(blinkIcon));
        abilityRegistry.put("charge", new ChargeAbility(chargeIcon));
        abilityRegistry.put("vault", new VaultAbility(vaultIcon));

        // Utility abilities
        abilityRegistry.put("bubble", new PaladinBubbleAbility(bubbleIcon));
        abilityRegistry.put("pull", new PullAbility(pullIcon));
        abilityRegistry.put("sprint", new SprintAbility(sprintIcon));
        abilityRegistry.put("full_heal", new FullHealAbility(healIcon));
        abilityRegistry.put("smoke_bomb", new SmokeBombAbility(smokeBombIcon));
        abilityRegistry.put("life_leech", new LifeLeechAbility(lifeLeechIcon));

        // Class abilities - Paladin
        if (playerClass == PlayerClass.PALADIN) {
            abilityRegistry.put("smite", new SmiteAbility(smiteIcon));
            abilityRegistry.put("prayer", new PaladinPrayerAbility(prayerIcon));
            abilityRegistry.put("consecrated_ground", new ConsecratedGroundAbility(consecrateIcon));
            abilityRegistry.put("holy_aura", new HolyAuraAbility(holyAuraIcon));
            abilityRegistry.put("holy_blessing", new HolyBlessingAbility(holyBlessingIcon));
            abilityRegistry.put("holy_sword", new HolySwordAbility(holySwordIcon));
        }

        // Class abilities - Mercenary
        if (playerClass == PlayerClass.MERCENARY) {
            abilityRegistry.put("double_swing", new DoubleSwingAbility(doubleSwingIcon));
            abilityRegistry.put("rend", new RendAbility(rendIcon));
            abilityRegistry.put("ground_slam", new GroundSlamAbility(groundSlamIcon));
            abilityRegistry.put("whirlwind", new WhirlwindAbility(whirlwindIcon));
            abilityRegistry.put("execute", new ExecuteAbility(executeIcon));
            abilityRegistry.put("blazing_fury", new BlazingFuryAbility(blazingFuryIcon));
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

    private float getEffectiveCooldown(float baseCooldown) {
        float attackSpeedReduction = player.getStats().getTotalAttackSpeed();
        float newCooldown = Math.max(0.1f, baseCooldown - attackSpeedReduction);
        if (newCooldown >= baseCooldown / 1.5f)
            return newCooldown;
        else
            return baseCooldown / 1.5f;
    }

    public void update(float delta) {
        syncAbilitiesWithSkillTree();

        for (Ability ability : abilities) {
            if (ability != null) {
                ability.update(delta);
            }
        }

        if (offhandCooldown > 0) offhandCooldown -= delta;
        if (swordCooldown > 0) swordCooldown -= delta;
        if (spearCooldown > 0) spearCooldown -= delta;

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
        skillTree.update(delta, gameProj);
        handleDragDrop();
        updateSlotBarHover();
        handleConsumableDragDrop();
        updateConsumableSlotHover();
    }

    private void handleDragDrop() {
        if (!skillTree.isOpen()) return;

        SkillTree.Skill draggingSkill = skillTree.getDraggingSkill();
        if (draggingSkill == null) return;

        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            int screenHeight = Gdx.graphics.getHeight();
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();

            int droppedSlot = getSlotAtPosition(mouseX, mouseY);
            if (droppedSlot >= 0) {
                skillTree.trySlotSkill(draggingSkill, droppedSlot);
            }

            skillTree.clearDragging();
        }
    }

    private void handleConsumableDragDrop() {
        if (draggingConsumable == null) return;

        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            int screenHeight = Gdx.graphics.getHeight();
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();

            int droppedSlot = getConsumableSlotAtPosition(mouseX, mouseY);
            if (droppedSlot >= 0) {
                consumableSlots[droppedSlot] = draggingConsumable;
            }

            draggingConsumable = null;
        }
    }

    private int getSlotAtPosition(float mouseX, float mouseY) {
        int screenWidth = Gdx.graphics.getWidth();

        int totalSlots = 9;
        int totalWidth = (totalSlots * SLOT_SIZE) + ((totalSlots - 1) * SLOT_PADDING) + (4 * SEPARATOR_SPACE);
        float startX = (screenWidth - totalWidth) / 2f;
        float startY = 35f;

        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            float slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);

            if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                    mouseY >= startY && mouseY <= startY + SLOT_SIZE) {
                return i;
            }
        }

        return -1;
    }

    private int getConsumableSlotAtPosition(float mouseX, float mouseY) {
        int screenWidth = Gdx.graphics.getWidth();

        int totalSlots = 9;
        int totalWidth = (totalSlots * SLOT_SIZE) + ((totalSlots - 1) * SLOT_PADDING) + (4 * SEPARATOR_SPACE);
        float startX = (screenWidth - totalWidth) / 2f;
        float startY = 35f;

        float consumableStartX = startX + (NUM_ABILITY_SLOTS * (SLOT_SIZE + SLOT_PADDING)) + SEPARATOR_SPACE +
                (2 * (SLOT_SIZE + SLOT_PADDING)) + SEPARATOR_SPACE;

        for (int i = 0; i < NUM_CONSUMABLE_SLOTS; i++) {
            float slotX = consumableStartX + i * (SLOT_SIZE + SLOT_PADDING);

            if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                    mouseY >= startY && mouseY <= startY + SLOT_SIZE) {
                return i;
            }
        }

        return -1;
    }

    private void updateSlotBarHover() {
        if (skillTree.isDragging()) {
            hoveredSlotIndex = -1;
            hoveredSlotSkill = null;
            return;
        }

        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        int slot = getSlotAtPosition(mouseX, mouseY);

        if (slot >= 0) {
            hoveredSlotIndex = slot;
            hoveredSlotSkill = skillTree.getSlottedSkill(slot);
        } else {
            hoveredSlotIndex = -1;
            hoveredSlotSkill = null;
        }
    }

    private void updateConsumableSlotHover() {
        if (draggingConsumable != null) {
            hoveredConsumableSlot = -1;
            return;
        }

        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        hoveredConsumableSlot = getConsumableSlotAtPosition(mouseX, mouseY);
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
        if (skillTree.isOpen() || !Player.gameStarted) return;

        if (SaveManager.isActionJustPressed("ability1")) {
            useAbility(0);
        } else if (SaveManager.isActionJustPressed("ability2")) {
            useAbility(1);
        } else if (SaveManager.isActionJustPressed("ability3")) {
            useAbility(2);
        } else if (SaveManager.isActionJustPressed("ability4")) {
            useAbility(3);
        } else if (SaveManager.isActionJustPressed("ability5")) {
            useAbility(4);
        }

        if (SaveManager.isActionJustPressed("consumable1")) {
            useConsumable(0);
        } else if (SaveManager.isActionJustPressed("consumable2")) {
            useConsumable(1);
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            useOffhandAttack();
        }

        if (playerClass == PlayerClass.PALADIN && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (!gameProj.isPaused() && !player.getInventory().isOpen() && !skillTree.isOpen()) {
                performSwordAttack();
            }
        }

        if (playerClass == PlayerClass.MERCENARY && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (!gameProj.isPaused() && !player.getInventory().isOpen() && !skillTree.isOpen()) {
                performSpearAttack();
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

    private void useConsumable(int slot) {
        if (slot < 0 || slot >= NUM_CONSUMABLE_SLOTS) return;

        Item consumable = consumableSlots[slot];
        if (consumable == null) return;

        player.getInventory().tryUseConsumable(consumable, player);
    }


    public void startDraggingConsumable(Item item) {
        if (item != null && item.getType() == Item.ItemType.CONSUMABLE) {
            draggingConsumable = item;
        }
    }

    private void useOffhandAttack() {
        if (offhandCooldown > 0) return;

        Item offhand = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND);

        if (offhand != null && offhand.getName().toLowerCase().contains("shield")) {
            performShieldBash();
            offhandCooldown = getEffectiveCooldown(BASE_OFFHAND_COOLDOWN_TIME);
        }
    }

    private void performSpearAttack() {
        if (spearCooldown > 0) return;

        SoundManager.getInstance().playHitSound("Spear");

        float attackRange = SPEAR_ATTACK_RANGE;
        player.addAbilityVisual(AbilityVisual.SpearJab.createWhite(player, gameProj, 0.3f, attackRange, 0f));

        com.badlogic.gdx.math.Vector2 playerPos = player.getPosition();
        int playerDamage = player.getStats().getTotalDamage();

        com.badlogic.gdx.math.Vector3 mousePos3D = gameProj.getCamera().unproject(
                new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        com.badlogic.gdx.math.Vector2 mousePos = new com.badlogic.gdx.math.Vector2(mousePos3D.x, mousePos3D.y);
        com.badlogic.gdx.math.Vector2 attackDir = new com.badlogic.gdx.math.Vector2(
                mousePos.x - playerPos.x, mousePos.y - playerPos.y
        ).nor();

        // Damage overworld enemies
        for (managers.Chunk chunk : gameProj.getChunks().values()) {
            for (entities.Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    float distanceAlongLine = toEnemy.dot(attackDir);
                    if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                        com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                        float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                        if (perpDistance < 15f) {
                            enemy.takeDamage(playerDamage);
                            player.onBasicAttackHit();
                        }
                    }
                }
            }

            for (entities.Lemmy enemy : new ArrayList<>(gameProj.getGlobalLemmy())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    float distanceAlongLine = toEnemy.dot(attackDir);
                    if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                        com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                        float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                        if (perpDistance < 15f) {
                            enemy.takeDamage(playerDamage);
                            player.onBasicAttackHit();
                        }
                    }
                }
            }
        }

        // Damage Herman
        if (gameProj.isHermanSpawned()) {
            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = herman.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                    com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < 15f) {
                        herman.takeDamage(playerDamage);
                        player.onBasicAttackHit();
                    }
                }
            }

            Herman hermanDuplicate = gameProj.getHermanDuplicate();
            if (hermanDuplicate != null && hermanDuplicate.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = hermanDuplicate.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                    com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < 15f) {
                        hermanDuplicate.takeDamage(playerDamage);
                        player.onBasicAttackHit();
                    }
                }
            }
        }

        // Damage dungeon enemies
        if (gameProj.getCurrentDungeon() != null) {
            for (entities.DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    float distanceAlongLine = toEnemy.dot(attackDir);
                    if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                        com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                        float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                        if (perpDistance < 15f) {
                            enemy.takeDamage(playerDamage);
                            player.onBasicAttackHit();
                        }
                    }
                }
            }

            for (DestructibleObject obj : new ArrayList<>(gameProj.getCurrentDungeon().getDestructables())) {
                if (obj == null || obj.isMarkedForRemoval()) continue;

                com.badlogic.gdx.math.Rectangle b = obj.getBounds();
                com.badlogic.gdx.math.Vector2 objCenter = new com.badlogic.gdx.math.Vector2(b.x + b.width / 2f, b.y + b.height / 2f);

                com.badlogic.gdx.math.Vector2 toObj = new com.badlogic.gdx.math.Vector2(
                        objCenter.x - playerPos.x, objCenter.y - playerPos.y
                );

                float distanceAlongLine = toObj.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                    com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toObj.cpy().sub(projectedPoint).len();

                    if (perpDistance < 15f) {
                        obj.takeDamage(playerDamage);
                        player.onBasicAttackHit();
                    }
                }
            }
        }

        // Damage boss room enemies
        if (gameProj.getCurrentBossRoom() != null) {
            entities.BossKitty boss = gameProj.getCurrentBossRoom().getBoss();
            if (boss != null && boss.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = boss.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                    com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < 15f) {
                        boss.takeDamage(playerDamage);
                        player.onBasicAttackHit();
                    }
                }
            }

            entities.Cyclops cyclops = gameProj.getCurrentBossRoom().getCyclops();
            if (cyclops != null && cyclops.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = cyclops.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                    com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < 15f) {
                        cyclops.takeDamage(playerDamage);
                        player.onBasicAttackHit();
                    }
                }
            }

            entities.GhostBoss ghostBoss = gameProj.getCurrentBossRoom().getGhostBoss();
            if (ghostBoss != null && ghostBoss.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = ghostBoss.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );

                float distanceAlongLine = toEnemy.dot(attackDir);
                if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                    com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                    float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                    if (perpDistance < 15f) {
                        ghostBoss.takeDamage(playerDamage);
                        player.onBasicAttackHit();
                    }
                }
            }
        }

        if (gameProj.getCurrentEndlessRoom() != null) {
            for (entities.EndlessEnemy enemy : new ArrayList<>(gameProj.getCurrentEndlessRoom().getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );

                    float distanceAlongLine = toEnemy.dot(attackDir);
                    if (distanceAlongLine > 0 && distanceAlongLine < attackRange) {
                        com.badlogic.gdx.math.Vector2 projectedPoint = new com.badlogic.gdx.math.Vector2(attackDir).scl(distanceAlongLine);
                        float perpDistance = toEnemy.cpy().sub(projectedPoint).len();

                        if (perpDistance < 15f) {
                            enemy.takeDamage(playerDamage);
                            player.onBasicAttackHit();
                        }
                    }
                }
            }
        }

        spearCooldown = getEffectiveCooldown(BASE_SPEAR_COOLDOWN_TIME);
    }

    private void performSwordAttack() {
        if (swordCooldown > 0) return;

        SoundManager.getInstance().playHitSound("Sword");

        float attackRange = SWORD_ATTACK_RANGE;
        if (player.isHolySwordActive()) {
            attackRange *= player.getHolySwordConeMultiplier();
            player.addAbilityVisual(AbilityVisual.SwordSlash.createHoly(player, gameProj, 0.15f, attackRange));
        } else {
            player.addAbilityVisual(AbilityVisual.SwordSlash.createWhite(player, gameProj, 0.15f, attackRange));
        }

        com.badlogic.gdx.math.Vector2 playerPos = player.getPosition();
        int playerDamage = player.getStats().getTotalDamage();

        com.badlogic.gdx.math.Vector3 mousePos3D = gameProj.getCamera().unproject(
                new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        com.badlogic.gdx.math.Vector2 mousePos = new com.badlogic.gdx.math.Vector2(mousePos3D.x, mousePos3D.y);
        com.badlogic.gdx.math.Vector2 attackDir = new com.badlogic.gdx.math.Vector2(
                mousePos.x - playerPos.x, mousePos.y - playerPos.y
        ).nor();

        // Damage overworld enemies
        for (managers.Chunk chunk : gameProj.getChunks().values()) {
            for (entities.Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
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

        for (entities.Lemmy enemy : new ArrayList<>(gameProj.getGlobalLemmy())) {
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

        // Damage Herman
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

        // Damage dungeon enemies
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

            for (DestructibleObject obj : new ArrayList<>(gameProj.getCurrentDungeon().getDestructables())) {
                if (obj == null || obj.isMarkedForRemoval()) continue;

                com.badlogic.gdx.math.Rectangle b = obj.getBounds();
                com.badlogic.gdx.math.Vector2 objCenter = new com.badlogic.gdx.math.Vector2(b.x + b.width / 2f, b.y + b.height / 2f);
                com.badlogic.gdx.math.Vector2 toObj = new com.badlogic.gdx.math.Vector2(objCenter.x - playerPos.x, objCenter.y - playerPos.y);

                if (toObj.len() < attackRange && toObj.nor().dot(attackDir) > 0.5f) {
                    obj.takeDamage(playerDamage);
                    player.onBasicAttackHit();
                }
            }
        }

        // Damage boss room enemies
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

        if (gameProj.getCurrentEndlessRoom() != null) {
            for (entities.EndlessEnemy enemy : new ArrayList<>(gameProj.getCurrentEndlessRoom().getEnemies())) {
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

        swordCooldown = getEffectiveCooldown(BASE_SWORD_COOLDOWN_TIME);
    }

    private void performShieldBash() {
        SoundManager.getInstance().playHitSound("Shield");
        player.addAbilityVisual(AbilityVisual.ShieldBash.createWhite(player, gameProj, 0.15f));

        com.badlogic.gdx.math.Vector2 playerPos = player.getPosition();
        float attackRange = 45f;
        int shieldDamage = BASE_SHIELD_BASH_DAMAGE + player.getStats().getActualDamage();

        com.badlogic.gdx.math.Vector3 mousePos3D = gameProj.getCamera().unproject(
                new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        com.badlogic.gdx.math.Vector2 mousePos = new com.badlogic.gdx.math.Vector2(mousePos3D.x, mousePos3D.y);
        com.badlogic.gdx.math.Vector2 attackDir = new com.badlogic.gdx.math.Vector2(
                mousePos.x - playerPos.x, mousePos.y - playerPos.y
        ).nor();

        // Damage and stun overworld enemies
        for (managers.Chunk chunk : gameProj.getChunks().values()) {
            for (entities.Enemy enemy : new ArrayList<>(chunk.getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );
                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(shieldDamage);
                        StunEffect stun = new StunEffect(enemy, 1f);
                        stun.onApply();
                        gameProj.addStatusEffect(enemy, stun);
                        player.onBasicAttackHit();
                    }
                }
            }
        }

        for (entities.Lemmy enemy : new ArrayList<>(gameProj.getGlobalLemmy())) {
            if (enemy.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );
                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    enemy.takeDamage(shieldDamage);
                    StunEffect stun = new StunEffect(enemy, 1f);
                    stun.onApply();
                    gameProj.addStatusEffect(enemy, stun);
                    player.onBasicAttackHit();
                }
            }
        }

        // Herman
        if (gameProj.isHermanSpawned()) {
            Herman herman = gameProj.getHerman();
            if (herman != null && herman.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = herman.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );
                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    herman.takeDamage(shieldDamage);
                    StunEffect stun = new StunEffect(herman, 1f);
                    stun.onApply();
                    gameProj.addStatusEffect(herman, stun);
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
                    hermanDuplicate.takeDamage(shieldDamage);
                    StunEffect stun = new StunEffect(hermanDuplicate, 1f);
                    stun.onApply();
                    gameProj.addStatusEffect(hermanDuplicate, stun);
                    player.onBasicAttackHit();
                }
            }
        }

        // Dungeon enemies
        if (gameProj.getCurrentDungeon() != null) {
            for (entities.DungeonEnemy enemy : new ArrayList<>(gameProj.getCurrentDungeon().getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );
                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(shieldDamage);
                        StunEffect stun = new StunEffect(enemy, 1f);
                        stun.onApply();
                        gameProj.addStatusEffect(enemy, stun);
                        player.onBasicAttackHit();
                    }
                }
            }

            for (DestructibleObject obj : new ArrayList<>(gameProj.getCurrentDungeon().getDestructables())) {
                if (obj == null || obj.isMarkedForRemoval()) continue;

                com.badlogic.gdx.math.Rectangle b = obj.getBounds();
                com.badlogic.gdx.math.Vector2 objCenter = new com.badlogic.gdx.math.Vector2(b.x + b.width / 2f, b.y + b.height / 2f);
                com.badlogic.gdx.math.Vector2 toObj = new com.badlogic.gdx.math.Vector2(objCenter.x - playerPos.x, objCenter.y - playerPos.y);

                if (toObj.len() < attackRange && toObj.nor().dot(attackDir) > 0.5f) {
                    obj.takeDamage(shieldDamage);
                    player.onBasicAttackHit();
                }
            }

        }

        // Boss room
        if (gameProj.getCurrentBossRoom() != null) {
            entities.BossKitty boss = gameProj.getCurrentBossRoom().getBoss();
            if (boss != null && boss.getBody() != null) {
                com.badlogic.gdx.math.Vector2 enemyPos = boss.getBody().getPosition();
                com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                        enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                );
                if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                    boss.takeDamage(shieldDamage);
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
                    cyclops.takeDamage(shieldDamage);
                    StunEffect stun = new StunEffect(cyclops, 0.5f);
                    stun.onApply();
                    gameProj.addStatusEffect(cyclops, stun);
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
                    ghostBoss.takeDamage(shieldDamage);
                    StunEffect stun = new StunEffect(ghostBoss, 0.5f);
                    stun.onApply();
                    gameProj.addStatusEffect(ghostBoss, stun);
                    player.onBasicAttackHit();
                }
            }
        }

        if (gameProj.getCurrentEndlessRoom() != null) {
            for (entities.EndlessEnemy enemy : new ArrayList<>(gameProj.getCurrentEndlessRoom().getEnemies())) {
                if (enemy.getBody() != null) {
                    com.badlogic.gdx.math.Vector2 enemyPos = enemy.getBody().getPosition();
                    com.badlogic.gdx.math.Vector2 toEnemy = new com.badlogic.gdx.math.Vector2(
                            enemyPos.x - playerPos.x, enemyPos.y - playerPos.y
                    );
                    if (toEnemy.len() < attackRange && toEnemy.nor().dot(attackDir) > 0.5f) {
                        enemy.takeDamage(shieldDamage);
                        StunEffect stun = new StunEffect(enemy, 1f);
                        stun.onApply();
                        gameProj.addStatusEffect(enemy, stun);
                        player.onBasicAttackHit();
                    }
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

    public PlayerClass getPlayerClass() { return playerClass; }
    public SkillTree getSkillTree() { return skillTree; }

    public void renderSkillTree(SpriteBatch batch) {
        skillTree.render(batch);
    }

    public void renderSkillBar(SpriteBatch batch) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        int totalSlots = 9; // 5 abilities + 2 weapons + 2 consumables
        int totalWidth = (totalSlots * SLOT_SIZE) + ((totalSlots - 1) * SLOT_PADDING) + (4 * SEPARATOR_SPACE);

        float startX = (screenWidth - totalWidth) / 2f;
        float startY = 35f;

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Render ability slots
        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            float slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);
            renderAbilitySlot(slotX, startY, i);
        }

        float attackStartX = startX + (NUM_ABILITY_SLOTS * (SLOT_SIZE + SLOT_PADDING)) + SEPARATOR_SPACE;

        batch.begin();
        renderAttackSlot(attackStartX, startY, "LMB",
                player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.WEAPON));
        renderAttackSlot(attackStartX + SLOT_SIZE + SLOT_PADDING, startY, "RMB",
                player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND));
        batch.end();
        // Render consumable slots
        float consumableStartX = attackStartX + (2 * (SLOT_SIZE + SLOT_PADDING)) + SEPARATOR_SPACE;
        for (int i = 0; i < NUM_CONSUMABLE_SLOTS; i++) {
            float slotX = consumableStartX + i * (SLOT_SIZE + SLOT_PADDING);
            renderConsumableSlot(slotX, startY, i);
        }

        shapeRenderer.end();

        batch.begin();

        // Render ability icons
        for (int i = 0; i < NUM_ABILITY_SLOTS; i++) {
            float slotX = startX + i * (SLOT_SIZE + SLOT_PADDING);
            renderAbilityIcon(batch, slotX, startY, i);
        }

        // Render weapon icons
        Item weapon = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.WEAPON);
        if (weapon != null) {
            weapon.renderIcon(batch, attackStartX + 3, startY + 3, SLOT_SIZE - 6);
        }

        // Render cooldown for LMB based on class
        if (playerClass == PlayerClass.PALADIN && swordCooldown > 0) {
            float effectiveSwordCooldown = getEffectiveCooldown(BASE_SWORD_COOLDOWN_TIME);
            renderCooldownOverlay(batch, attackStartX, startY, swordCooldown / effectiveSwordCooldown);
        } else if (playerClass == PlayerClass.MERCENARY && spearCooldown > 0) {
            float effectiveSpearCooldown = getEffectiveCooldown(BASE_SPEAR_COOLDOWN_TIME);
            renderCooldownOverlay(batch, attackStartX, startY, spearCooldown / effectiveSpearCooldown);
        }

        Item offhand = player.getInventory().getEquipment().getEquippedItem(Equipment.EquipmentSlot.OFFHAND);
        if (offhand != null) {
            offhand.renderIcon(batch, attackStartX + SLOT_SIZE + SLOT_PADDING + 3, startY + 3, SLOT_SIZE - 6);
            float effectiveOffhandCooldown = getEffectiveCooldown(BASE_OFFHAND_COOLDOWN_TIME);
            if (offhandCooldown > 0) {
                renderCooldownOverlay(batch, attackStartX + SLOT_SIZE + SLOT_PADDING, startY, offhandCooldown / effectiveOffhandCooldown);
            }
        }

        renderAttackLabel(batch, attackStartX, startY, "LMB");
        renderAttackLabel(batch, attackStartX + SLOT_SIZE + SLOT_PADDING, startY, "RMB");

        // Render consumable icons and counts
        for (int i = 0; i < NUM_CONSUMABLE_SLOTS; i++) {
            float slotX = consumableStartX + i * (SLOT_SIZE + SLOT_PADDING);
            renderConsumableIcon(batch, slotX, startY, i);
        }

        // Render tooltip for hovered ability slot
        if (hoveredSlotSkill != null && !skillTree.isDragging()) {
            float slotX = startX + hoveredSlotIndex * (SLOT_SIZE + SLOT_PADDING);
            float tooltipY = startY + SLOT_SIZE + 10;
            skillTree.renderTooltipAt(batch, hoveredSlotSkill, slotX, tooltipY, screenWidth, screenHeight);
        }

        // Highlight slot when dragging skill over it
        if (skillTree.isDragging()) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();
            int targetSlot = getSlotAtPosition(mouseX, mouseY);

            if (targetSlot >= 0) {
                batch.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                Gdx.gl.glLineWidth(3);
                shapeRenderer.setColor(Color.GREEN);
                float slotX = startX + targetSlot * (SLOT_SIZE + SLOT_PADDING);
                shapeRenderer.rect(slotX, startY, SLOT_SIZE, SLOT_SIZE);
                shapeRenderer.end();
                Gdx.gl.glLineWidth(1);
                batch.begin();
            }
        }

        // Highlight consumable slot when dragging consumable over it
        if (draggingConsumable != null) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();
            int targetSlot = getConsumableSlotAtPosition(mouseX, mouseY);

            if (targetSlot >= 0) {
                batch.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                Gdx.gl.glLineWidth(3);
                shapeRenderer.setColor(Color.GREEN);
                float slotX = consumableStartX + targetSlot * (SLOT_SIZE + SLOT_PADDING);
                shapeRenderer.rect(slotX, startY, SLOT_SIZE, SLOT_SIZE);
                shapeRenderer.end();
                Gdx.gl.glLineWidth(1);
                batch.begin();
            }

            // Draw the dragging consumable
            batch.setColor(1f, 1f, 1f, 0.8f);
            draggingConsumable.renderIcon(batch, mouseX - SLOT_SIZE / 2f, mouseY - SLOT_SIZE / 2f, SLOT_SIZE);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    private void renderAbilitySlot(float x, float y, int index) {
        Ability ability = abilities[index];
        boolean isHovered = (index == hoveredSlotIndex);
        boolean isDragTarget = skillTree.isDragging() && isHovered;

        if (isDragTarget) {
            shapeRenderer.setColor(0.4f, 0.6f, 0.4f, 0.9f);
        } else if (ability != null && ability.isCasting()) {
            shapeRenderer.setColor(0.5f, 0.8f, 0.5f, 0.8f);
        } else if (ability != null && ability.isOnCooldown()) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.8f);
        } else if (ability != null) {
            shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 0.8f);
        } else {
            shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 0.6f);
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        if (isHovered && !skillTree.isDragging()) {
            shapeRenderer.setColor(1f, 1f, 0.5f, 1f);
        } else if (ability != null) {
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
        boolean onCooldown = false;

        if (label.equals("RMB") && offhandCooldown > 0) {
            onCooldown = true;
        } else if (label.equals("LMB")) {
            if (playerClass == PlayerClass.PALADIN && swordCooldown > 0) {
                onCooldown = true;
            } else if (playerClass == PlayerClass.MERCENARY && spearCooldown > 0) {
                onCooldown = true;
            }
        }

        if (onCooldown) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.8f);
        } else {
            shapeRenderer.setColor(0.35f, 0.3f, 0.3f, 0.8f);
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(0.7f, 0.6f, 0.6f, 1f);
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glLineWidth(1);

    }

    private void renderAttackLabel(SpriteBatch batch, float x, float y, String label) {
        font.setColor(Color.WHITE);
        font.getData().setScale(0.6f);
        font.draw(batch, label, x + 3, y + SLOT_SIZE - 3);
        font.getData().setScale(1.0f);
    }

    private void renderConsumableSlot(float x, float y, int index) {
        Item consumable = consumableSlots[index];
        boolean isHovered = (index == hoveredConsumableSlot);
        boolean isDragTarget = draggingConsumable != null && isHovered;

        if (isDragTarget) {
            shapeRenderer.setColor(0.4f, 0.6f, 0.4f, 0.9f);
        } else if (consumable != null) {
            int count = player.getInventory().getItemCountForConsumable(consumable);
            if (count > 0) {
                shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 0.8f);
            } else {
                // Grayed out
                shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 0.6f);
            }
        } else {
            shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 0.6f);
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        if (isHovered && draggingConsumable != null) {
            shapeRenderer.setColor(0.5f, 1f, 0.5f, 1f);
        } else if (consumable != null) {
            shapeRenderer.setColor(0.6f, 0.6f, 0.7f, 1f);
        } else {
            shapeRenderer.setColor(0.4f, 0.4f, 0.5f, 0.6f);
        }
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glLineWidth(1);
    }

    private void renderAbilityIcon(SpriteBatch batch, float x, float y, int index) {
        Ability ability = abilities[index];

        if (ability == null) {
            font.setColor(0.5f, 0.5f, 0.5f, 0.5f);
            font.getData().setScale(1.2f);
            font.draw(batch, "+", x + SLOT_SIZE / 2f - 8, y + SLOT_SIZE / 2f + 10);
            font.getData().setScale(1.0f);
        } else {
            if (ability.getIconTexture() != null) {
                batch.draw(ability.getIconTexture(), x + 3, y + 3, SLOT_SIZE - 6, SLOT_SIZE - 6);
            }

            if (ability.isOnCooldown()) {
                renderCooldownOverlay(batch, x, y, ability.getCooldownPercentage());
            }

            if (ability.isCasting()) {
                renderCastBar(batch, x, y, ability.getCastProgress());
            }

            if (ability.isOnCooldown()) {
                font.setColor(Color.YELLOW);
                font.getData().setScale(0.8f);
                String cooldownText = String.format("%.1f", ability.getCurrentCooldown());
                float textWidth = font.getSpaceXadvance() * cooldownText.length() * 0.8f;
                font.draw(batch, cooldownText, x + (SLOT_SIZE - textWidth) / 2, y + SLOT_SIZE / 2 + 5);
                font.getData().setScale(1.0f);
            }
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(0.6f);
        String keyLabel = getActionKeyLabel(getAbilityAction(index), String.valueOf(index + 1));
        font.draw(batch, keyLabel, x + 3, y + SLOT_SIZE - 3);
        font.getData().setScale(1.0f);
    }

    private void renderConsumableIcon(SpriteBatch batch, float x, float y, int index) {
        Item consumable = consumableSlots[index];

        font.setColor(Color.WHITE);
        font.getData().setScale(0.6f);
        String keyLabel = getActionKeyLabel(getConsumableAction(index), index == 0 ? "E" : "Q");
        font.draw(batch, keyLabel, x + 3, y + SLOT_SIZE - 3);
        font.getData().setScale(1.0f);

        if (consumable == null) {
            font.setColor(0.5f, 0.5f, 0.5f, 0.5f);
            font.getData().setScale(1.2f);
            font.draw(batch, "+", x + SLOT_SIZE / 2f - 8, y + SLOT_SIZE / 2f + 10);
            font.getData().setScale(1.0f);
            return;
        }

        int count = player.getInventory().getItemCountForConsumable(consumable);

        if (count > 0) {
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            batch.setColor(0.4f, 0.4f, 0.4f, 0.6f);
        }
        consumable.renderIcon(batch, x + 3, y + 3, SLOT_SIZE - 6);
        batch.setColor(1f, 1f, 1f, 1f);

        // Draw count in top-right corner
        if (count > 0) {
            font.setColor(Color.YELLOW);
        } else {
            font.setColor(Color.GRAY);
        }
        font.getData().setScale(0.6f);
        String countText = String.valueOf(count);
        float textWidth = font.getSpaceXadvance() * countText.length() * 0.6f;
        font.draw(batch, countText, x + SLOT_SIZE - textWidth - 5, y + SLOT_SIZE - 3);
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);

        if (consumable.getName() != null && consumable.getName().contains("Health Potion")) {
            float remaining = player.getInventory().getHealthPotionCooldownRemaining();
            float total = player.getInventory().getHealthPotionCooldownTotal();

            if (remaining > 0f && total > 0f) {
                float pct = remaining / total; // 1 -> full overlay, 0 -> none
                renderCooldownOverlay(batch, x, y, pct);

                font.setColor(Color.YELLOW);
                font.getData().setScale(0.8f);
                String t = String.format("%.1f", remaining);
                float cdTextWidth = font.getSpaceXadvance() * t.length() * 0.8f;
                font.draw(batch, t, x + (SLOT_SIZE - cdTextWidth) / 2f, y + SLOT_SIZE / 2f + 5);
                font.getData().setScale(1.0f);
                font.setColor(Color.WHITE);
            }
        }
    }

    private void renderCooldownOverlay(SpriteBatch batch, float x, float y, float percentage) {
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE * percentage);
        shapeRenderer.end();
        batch.begin();
    }

    private String getAbilityAction(int index) {
        return "ability" + (index + 1);
    }

    private String getConsumableAction(int index) {
        return index == 0 ? "consumable1" : "consumable2";
    }

    private String getActionKeyLabel(String action, String fallback) {
        int[] keys = SaveManager.getKeybinding(action);
        if (keys == null || keys.length == 0) {
            return fallback;
        }

        if (keys.length == 1) {
            return keyToLabel(keys[0], fallback);
        }

        if (keys.length == 2) {
            int first = keys[0];
            int second = keys[1];

            if (isShiftKey(first) || isShiftKey(second)) {
                int other = isShiftKey(first) ? second : first;
                return "S+" + firstCharLabel(other, fallback);
            }

            if (isCtrlKey(first) || isCtrlKey(second)) {
                int other = isCtrlKey(first) ? second : first;
                return "C+" + firstCharLabel(other, fallback);
            }

            if (isAltKey(first) || isAltKey(second)) {
                int other = isAltKey(first) ? second : first;
                return "A+" + firstCharLabel(other, fallback);
            }

            return firstCharLabel(first, fallback) + "+" + firstCharLabel(second, fallback);
        }

        return keyToLabel(keys[0], fallback);
    }

    private String keyToLabel(int key, String fallback) {
        String label = keyToShortLabel(key);
        return (label == null || label.isEmpty()) ? fallback : label;
    }

    private boolean isShiftKey(int key) {
        return key == Input.Keys.SHIFT_LEFT || key == Input.Keys.SHIFT_RIGHT;
    }

    private boolean isCtrlKey(int key) {
        return key == Input.Keys.CONTROL_LEFT || key == Input.Keys.CONTROL_RIGHT;
    }

    private boolean isAltKey(int key) {
        return key == Input.Keys.ALT_LEFT || key == Input.Keys.ALT_RIGHT;
    }

    private String firstCharLabel(int key, String fallback) {
        String label = keyToShortLabel(key);
        if (label == null || label.isEmpty()) {
            label = fallback;
        }
        if (label == null || label.isEmpty()) {
            return "?";
        }
        return label.substring(0, 1);
    }

    private String keyToShortLabel(int key) {
        switch (key) {
            case Input.Keys.SPACE: return "SP";
            case Input.Keys.SHIFT_LEFT:
            case Input.Keys.SHIFT_RIGHT: return "ST";
            case Input.Keys.CONTROL_LEFT:
            case Input.Keys.CONTROL_RIGHT: return "CL";
            case Input.Keys.ALT_LEFT:
            case Input.Keys.ALT_RIGHT: return "ALT";
            case Input.Keys.TAB: return "TAB";
            case Input.Keys.ENTER: return "ENT";
            case Input.Keys.BACKSPACE: return "BSP";
            case Input.Keys.ESCAPE: return "ESC";
            case Input.Keys.CAPS_LOCK: return "CAP";
            case Input.Keys.NUM_LOCK: return "NUM";
            case Input.Keys.SCROLL_LOCK: return "SCR";
            case Input.Keys.INSERT: return "INS";
            case Input.Keys.HOME: return "HOM";
            case Input.Keys.END: return "END";
            case Input.Keys.PAGE_UP: return "PGU";
            case Input.Keys.PAGE_DOWN: return "PGD";
            case Input.Keys.UP: return "UP";
            case Input.Keys.DOWN: return "DN";
            case Input.Keys.LEFT: return "LT";
            case Input.Keys.RIGHT: return "RT";
            case Input.Keys.F1: return "F1";
            case Input.Keys.F2: return "F2";
            case Input.Keys.F3: return "F3";
            case Input.Keys.F4: return "F4";
            case Input.Keys.F5: return "F5";
            case Input.Keys.F6: return "F6";
            case Input.Keys.F7: return "F7";
            case Input.Keys.F8: return "F8";
            case Input.Keys.F9: return "F9";
            case Input.Keys.F10: return "F10";
            case Input.Keys.F11: return "F11";
            case Input.Keys.F12: return "F12";
            case Input.Keys.NUM_0: return "0";
            case Input.Keys.NUM_1: return "1";
            case Input.Keys.NUM_2: return "2";
            case Input.Keys.NUM_3: return "3";
            case Input.Keys.NUM_4: return "4";
            case Input.Keys.NUM_5: return "5";
            case Input.Keys.NUM_6: return "6";
            case Input.Keys.NUM_7: return "7";
            case Input.Keys.NUM_8: return "8";
            case Input.Keys.NUM_9: return "9";
            case Input.Keys.NUMPAD_0: return "N0";
            case Input.Keys.NUMPAD_1: return "N1";
            case Input.Keys.NUMPAD_2: return "N2";
            case Input.Keys.NUMPAD_3: return "N3";
            case Input.Keys.NUMPAD_4: return "N4";
            case Input.Keys.NUMPAD_5: return "N5";
            case Input.Keys.NUMPAD_6: return "N6";
            case Input.Keys.NUMPAD_7: return "N7";
            case Input.Keys.NUMPAD_8: return "N8";
            case Input.Keys.NUMPAD_9: return "N9";
            case Input.Keys.PLUS: return "+";
            case Input.Keys.MINUS: return "-";
            case Input.Keys.EQUALS: return "=";
            case Input.Keys.LEFT_BRACKET: return "[";
            case Input.Keys.RIGHT_BRACKET: return "]";
            case Input.Keys.BACKSLASH: return "\\";
            case Input.Keys.SEMICOLON: return ";";
            case Input.Keys.APOSTROPHE: return "'";
            case Input.Keys.COMMA: return ",";
            case Input.Keys.PERIOD: return ".";
            case Input.Keys.SLASH: return "/";
            case Input.Keys.GRAVE: return "`";
            case Input.Keys.AT: return "@";
        }

        String label = Input.Keys.toString(key);
        if (label == null || label.isEmpty()) {
            return null;
        }
        label = label.toUpperCase();
        if (label.length() <= 3) {
            return label;
        }
        return label.substring(0, 3);
    }

    private void renderCastBar(SpriteBatch batch, float x, float y, float progress) {
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(x + 2, y - 8, SLOT_SIZE - 4, 6);
        shapeRenderer.setColor(0.3f, 0.8f, 0.3f, 1f);
        shapeRenderer.rect(x + 2, y - 8, (SLOT_SIZE - 4) * progress, 6);
        shapeRenderer.end();
        batch.begin();
    }

    public Map<Object, List<StatusEffect>> getActiveEffects() { return activeEffects; }

    public List<AbilityVisual> getActiveVisuals() {
        return activeVisuals;
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
