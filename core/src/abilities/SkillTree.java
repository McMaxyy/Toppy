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
import entities.PlayerClass;
import game.GameProj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTree {

    // Skill tree categories
    public enum SkillCategory {
        CLASS,
        MOVEMENT,
        UTILITY
    }

    // Skill data class
    public static class Skill {
        public String id;
        public String name;
        public String description;
        public SkillCategory category;
        public Texture icon;
        public boolean unlocked;
        public int slottedPosition;

        public Skill(String id, String name, String description, SkillCategory category, Texture icon) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.icon = icon;
            this.unlocked = false;
            this.slottedPosition = -1;
        }
    }

    // Constants
    private static final int MAX_ABILITY_SLOTS = 5;
    private static final int MAX_MOVEMENT_ABILITIES = 1;
    private static final int MAX_UTILITY_ABILITIES = 2;
    private static final int MAX_CLASS_ABILITIES = 3;

    // UI Constants
    private static final int SKILL_SLOT_SIZE = 50;
    private static final int SKILL_SLOT_PADDING = 15;
    private static final int UI_PADDING = 20;
    private static final int SECTION_HEADER_HEIGHT = 40;

    // Colors
    private static final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.15f, 0.95f);
    private static final Color SECTION_COLOR = new Color(0.15f, 0.15f, 0.2f, 0.9f);
    private static final Color SLOT_COLOR = new Color(0.3f, 0.3f, 0.4f, 0.9f);
    private static final Color SLOT_LOCKED_COLOR = new Color(0.2f, 0.2f, 0.25f, 0.9f);
    private static final Color SLOT_UNLOCKED_COLOR = new Color(0.3f, 0.4f, 0.3f, 0.9f);
    private static final Color SLOT_SLOTTED_COLOR = new Color(0.3f, 0.5f, 0.6f, 0.9f);
    private static final Color SLOT_BORDER_COLOR = new Color(0.6f, 0.6f, 0.7f, 1f);
    private static final Color SELECTED_BORDER_COLOR = new Color(1f, 1f, 0.5f, 1f);
    private static final Color CLASS_COLOR = new Color(1f, 0.85f, 0.3f, 1f);
    private static final Color MOVEMENT_COLOR = new Color(0.3f, 0.7f, 1f, 1f);
    private static final Color UTILITY_COLOR = new Color(0.5f, 1f, 0.5f, 1f);
    private static final Color LIMIT_REACHED_COLOR = new Color(0.5f, 0.2f, 0.2f, 0.9f);

    // State
    private boolean isOpen = false;
    private Skill hoveredSkill = null;

    // Drag state
    private Skill draggingSkill = null;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    // Skills data
    private List<Skill> classSkills;
    private List<Skill> movementSkills;
    private List<Skill> utilitySkills;
    private Map<String, Skill> allSkillsById;

    // Slotted abilities (indices 0-4)
    private Skill[] slottedAbilities;

    // References
    private Player player;
    private PlayerClass playerClass;

    // UI Components
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    // Textures
    private Texture defaultIcon;

    public SkillTree(Player player, PlayerClass playerClass) {
        this.player = player;
        this.playerClass = playerClass;
        this.slottedAbilities = new Skill[MAX_ABILITY_SLOTS];
        this.allSkillsById = new HashMap<>();

        this.shapeRenderer = new ShapeRenderer();
        this.font = Storage.assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        this.defaultIcon = Storage.assetManager.get("icons/abilities/DoubleSwing.png", Texture.class);

        initializeSkills();
    }

    private void initializeSkills() {
        classSkills = new ArrayList<>();
        movementSkills = new ArrayList<>();
        utilitySkills = new ArrayList<>();

        if (playerClass == PlayerClass.PALADIN) {
            initializePaladinSkills();
        } else {
            initializeMercenarySkills();
        }
    }

    private void initializePaladinSkills() {
        // Class abilities
        addSkill(new Skill("smite", "Smite", "Call down divine judgment, damaging all enemies around you",
                SkillCategory.CLASS, loadIcon("icons/abilities/Smite.png")));
        addSkill(new Skill("prayer", "Prayer", "Channel divine energy to restore 80 health",
                SkillCategory.CLASS, loadIcon("icons/abilities/Prayer.png")));
        addSkill(new Skill("consecrated_ground", "Consecrated Ground", "Mark enemies with holy light. After 3s, they take massive damage",
                SkillCategory.CLASS, loadIcon("icons/abilities/ConsecratedGround.png")));
        addSkill(new Skill("holy_aura", "Holy Aura", "Damages nearby enemies every second for 10 seconds",
                SkillCategory.CLASS, loadIcon("icons/abilities/HolyAura.png")));
        addSkill(new Skill("holy_blessing", "Holy Blessing", "+20 defense, +10 attack, +100 health for 6 seconds",
                SkillCategory.CLASS, defaultIcon));
        addSkill(new Skill("holy_sword", "Holy Sword", "Bigger sword attacks and +10 damage for 6 seconds",
                SkillCategory.CLASS, loadIcon("icons/abilities/HolySword.png")));

        // Movement abilities
        addSkill(new Skill("blink", "Blink", "Teleport to mouse position",
                SkillCategory.MOVEMENT, loadIcon("icons/abilities/Blink.png")));
        addSkill(new Skill("charge", "Charge", "Dash forward, dealing damage and stunning enemies",
                SkillCategory.MOVEMENT, loadIcon("icons/abilities/Charge.png")));
        addSkill(new Skill("shadow_step", "Shadow Step", "Dash backwards away from danger",
                SkillCategory.MOVEMENT, defaultIcon));
        addSkill(new Skill("vault", "Vault", "Leap through enemies, damaging all in your path",
                SkillCategory.MOVEMENT, defaultIcon));

        // Utility abilities
        addSkill(new Skill("bubble", "Bubble", "Gain a shield that blocks all damage for 2 seconds",
                SkillCategory.UTILITY, loadIcon("icons/abilities/Bubble.png")));
        addSkill(new Skill("pull", "Pull", "Create a vortex that pulls nearby enemies toward you",
                SkillCategory.UTILITY, loadIcon("icons/abilities/Pull.png")));
        addSkill(new Skill("sprint", "Sprint", "Gain +15 DEX for 10 seconds",
                SkillCategory.UTILITY, defaultIcon));
        addSkill(new Skill("full_heal", "Full Heal", "Instantly restore all health",
                SkillCategory.UTILITY, loadIcon("icons/abilities/Prayer.png")));
        addSkill(new Skill("smoke_bomb", "Smoke Bomb", "Create a zone that protects you from damage for 3 seconds",
                SkillCategory.UTILITY, loadIcon("icons/abilities/SmokeBomb.png")));
        addSkill(new Skill("life_leech", "Life Leech", "Your attacks heal you for 5 HP for 5 seconds",
                SkillCategory.UTILITY, defaultIcon));
    }

    private void initializeMercenarySkills() {
        // Class abilities
        addSkill(new Skill("double_swing", "Double Swing", "Strike twice, each hit dealing 90% weapon damage",
                SkillCategory.CLASS, loadIcon("icons/abilities/DoubleSwing.png")));
        addSkill(new Skill("rend", "Rend", "Inflict bleeding wounds on enemies in a cone",
                SkillCategory.CLASS, loadIcon("icons/abilities/Rend.png")));
        addSkill(new Skill("prayer_merc", "Prayer", "Channel to restore 80 health",
                SkillCategory.CLASS, loadIcon("icons/abilities/Prayer.png")));

        // Movement abilities
        addSkill(new Skill("blink", "Blink", "Teleport to mouse position",
                SkillCategory.MOVEMENT, loadIcon("icons/abilities/Blink.png")));
        addSkill(new Skill("charge", "Charge", "Dash forward, dealing damage and stunning enemies",
                SkillCategory.MOVEMENT, loadIcon("icons/abilities/Charge.png")));
        addSkill(new Skill("shadow_step", "Shadow Step", "Dash backwards away from danger",
                SkillCategory.MOVEMENT, defaultIcon));
        addSkill(new Skill("vault", "Vault", "Leap through enemies, damaging all in your path",
                SkillCategory.MOVEMENT, defaultIcon));

        // Utility abilities
        addSkill(new Skill("bubble", "Bubble", "Gain a shield that blocks all damage for 2 seconds",
                SkillCategory.UTILITY, loadIcon("icons/abilities/Bubble.png")));
        addSkill(new Skill("pull", "Pull", "Create a vortex that pulls nearby enemies toward you",
                SkillCategory.UTILITY, loadIcon("icons/abilities/Pull.png")));
        addSkill(new Skill("sprint", "Sprint", "Gain +15 DEX for 10 seconds",
                SkillCategory.UTILITY, defaultIcon));
        addSkill(new Skill("full_heal", "Full Heal", "Instantly restore all health",
                SkillCategory.UTILITY, loadIcon("icons/abilities/Prayer.png")));
        addSkill(new Skill("smoke_bomb", "Smoke Bomb", "Create a zone that protects you from damage for 3 seconds",
                SkillCategory.UTILITY, defaultIcon));
        addSkill(new Skill("life_leech", "Life Leech", "Your attacks heal you for 5 HP for 5 seconds",
                SkillCategory.UTILITY, defaultIcon));
    }

    private void addSkill(Skill skill) {
        allSkillsById.put(skill.id, skill);
        switch (skill.category) {
            case CLASS:
                classSkills.add(skill);
                break;
            case MOVEMENT:
                movementSkills.add(skill);
                break;
            case UTILITY:
                utilitySkills.add(skill);
                break;
        }
    }

    private Texture loadIcon(String path) {
        try {
            return Storage.assetManager.get(path, Texture.class);
        } catch (Exception e) {
            return defaultIcon;
        }
    }

    public void toggle() {
        isOpen = !isOpen;
        if (!isOpen) {
            draggingSkill = null;
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isDragging() {
        return draggingSkill != null;
    }

    public Skill getDraggingSkill() {
        return draggingSkill;
    }

    public void update(float delta, GameProj gameProj) {
        if (!isOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
                toggle();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.K) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            draggingSkill = null;
            toggle();
            return;
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        handleSkillTreeInput(mouseX, mouseY, screenWidth, screenHeight);
    }

    private void handleSkillTreeInput(float mouseX, float mouseY, int screenWidth, int screenHeight) {
        int panelWidth = 750;
        int panelHeight = 500;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        hoveredSkill = null;

        float sectionWidth = (panelWidth - UI_PADDING * 4) / 3f;

        float classX = panelX + UI_PADDING;
        float classY = panelY + panelHeight - UI_PADDING - SECTION_HEADER_HEIGHT;
        checkSkillsInSection(classSkills, classX, classY, sectionWidth, mouseX, mouseY);

        float movementX = classX + sectionWidth + UI_PADDING;
        checkSkillsInSection(movementSkills, movementX, classY, sectionWidth, mouseX, mouseY);

        float utilityX = movementX + sectionWidth + UI_PADDING;
        checkSkillsInSection(utilitySkills, utilityX, classY, sectionWidth, mouseX, mouseY);
    }

    private void checkSkillsInSection(List<Skill> skills, float sectionX, float sectionY,
                                      float sectionWidth, float mouseX, float mouseY) {
        int skillsPerRow = 3;
        float startY = sectionY - SECTION_HEADER_HEIGHT - SKILL_SLOT_PADDING - 60f;

        for (int i = 0; i < skills.size(); i++) {
            int row = i / skillsPerRow;
            int col = i % skillsPerRow;

            float slotX = sectionX + SKILL_SLOT_PADDING + col * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);
            float slotY = startY - row * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);

            if (mouseX >= slotX && mouseX <= slotX + SKILL_SLOT_SIZE &&
                    mouseY >= slotY && mouseY <= slotY + SKILL_SLOT_SIZE) {

                Skill skill = skills.get(i);
                hoveredSkill = skill;

                // Left click - unlock skill OR start dragging if already unlocked
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    if (!skill.unlocked && player.getStats().getAvailableSkillPoints() > 0) {
                        if (canUnlockSkill(skill)) {
                            skill.unlocked = true;
                            player.getStats().useSkillPoint();
                        }
                    } else if (skill.unlocked) {
                        // Start dragging
                        draggingSkill = skill;
                        dragOffsetX = mouseX - slotX;
                        dragOffsetY = mouseY - slotY;
                    }
                }

                // Right click - lock skill back
                if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                    if (skill.unlocked) {
                        // If slotted, remove from slot first
                        if (skill.slottedPosition >= 0) {
                            slottedAbilities[skill.slottedPosition] = null;
                            skill.slottedPosition = -1;
                        }
                        skill.unlocked = false;
                        player.getStats().refundSkillPoint();
                    }
                }
            }
        }
    }

    /**
     * Count unlocked abilities by category
     */
    private int countUnlockedByCategory(SkillCategory category) {
        int count = 0;
        List<Skill> skills;
        switch (category) {
            case CLASS: skills = classSkills; break;
            case MOVEMENT: skills = movementSkills; break;
            case UTILITY: skills = utilitySkills; break;
            default: return 0;
        }

        for (Skill skill : skills) {
            if (skill.unlocked) {
                count++;
            }
        }
        return count;
    }

    private boolean canUnlockSkill(Skill skill) {
        // Check category limits for UNLOCKING
        int currentUnlocked = countUnlockedByCategory(skill.category);

        switch (skill.category) {
            case MOVEMENT:
                if (currentUnlocked >= MAX_MOVEMENT_ABILITIES) return false;
                break;
            case UTILITY:
                if (currentUnlocked >= MAX_UTILITY_ABILITIES) return false;
                break;
            case CLASS:
                if (currentUnlocked >= MAX_CLASS_ABILITIES) return false;
                break;
        }

        return true;
    }

    public boolean trySlotSkill(Skill skill, int slot) {
        if (skill == null || slot < 0 || slot >= MAX_ABILITY_SLOTS) return false;
        if (!skill.unlocked) return false;

        // Remove from previous slot if any
        if (skill.slottedPosition >= 0) {
            slottedAbilities[skill.slottedPosition] = null;
        }

        // Remove existing skill from target slot if any
        if (slottedAbilities[slot] != null) {
            slottedAbilities[slot].slottedPosition = -1;
        }

        // Slot the skill
        slottedAbilities[slot] = skill;
        skill.slottedPosition = slot;

        return true;
    }
    public void clearDragging() {
        draggingSkill = null;
    }

    private int countSlottedByCategory(SkillCategory category) {
        int count = 0;
        for (Skill skill : slottedAbilities) {
            if (skill != null && skill.category == category) {
                count++;
            }
        }
        return count;
    }

    private int countTotalSlotted() {
        int count = 0;
        for (Skill skill : slottedAbilities) {
            if (skill != null) count++;
        }
        return count;
    }

    public void render(SpriteBatch batch) {
        if (!isOpen) return;

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        int panelWidth = 750;
        int panelHeight = 500;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.setColor(SLOT_BORDER_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);

        float sectionWidth = (panelWidth - UI_PADDING * 4) / 3f;
        float sectionHeight = panelHeight - UI_PADDING * 2 - 50;

        float classX = panelX + UI_PADDING;
        float sectionY = panelY + UI_PADDING;

        renderSection(batch, "Class Abilities", classSkills, classX, sectionY, sectionWidth, sectionHeight, CLASS_COLOR, MAX_CLASS_ABILITIES);

        float movementX = classX + sectionWidth + UI_PADDING;
        renderSection(batch, "Movement", movementSkills, movementX, sectionY, sectionWidth, sectionHeight, MOVEMENT_COLOR, MAX_MOVEMENT_ABILITIES);

        float utilityX = movementX + sectionWidth + UI_PADDING;
        renderSection(batch, "Utility", utilitySkills, utilityX, sectionY, sectionWidth, sectionHeight, UTILITY_COLOR, MAX_UTILITY_ABILITIES);

        batch.begin();

        // Draw title
        font.setColor(Color.WHITE);
        font.getData().setScale(0.8f);
        font.draw(batch, "Skill Tree", panelX + UI_PADDING, panelY + panelHeight - 10);

        // Draw skill points
        font.getData().setScale(0.6f);
        font.setColor(Color.YELLOW);
        font.draw(batch, "Skill Points: " + player.getStats().getAvailableSkillPoints(),
                panelX + panelWidth - 200, panelY + panelHeight - 15);

        renderSectionContent(batch, "Class Abilities", classSkills, classX, sectionY, sectionWidth, sectionHeight, CLASS_COLOR, MAX_CLASS_ABILITIES);
        renderSectionContent(batch, "Movement", movementSkills, movementX, sectionY, sectionWidth, sectionHeight, MOVEMENT_COLOR, MAX_MOVEMENT_ABILITIES);
        renderSectionContent(batch, "Utility", utilitySkills, utilityX, sectionY, sectionWidth, sectionHeight, UTILITY_COLOR, MAX_UTILITY_ABILITIES);

        // Draw tooltip for hovered skill (not while dragging)
        if (hoveredSkill != null && draggingSkill == null) {
            renderTooltip(batch, hoveredSkill, screenWidth, screenHeight);
        }

        // Draw controls hint
        font.getData().setScale(0.5f);
        font.setColor(Color.GRAY);
        font.draw(batch, "LMB: Unlock/Drag | RMB: Lock | Drag to ability bar to slot", panelX + UI_PADDING, panelY + 15);

        // Draw dragged skill
        if (draggingSkill != null) {
            float mouseX = Gdx.input.getX();
            float mouseY = screenHeight - Gdx.input.getY();

            batch.setColor(1f, 1f, 1f, 0.8f);
            batch.draw(draggingSkill.icon,
                    mouseX - SKILL_SLOT_SIZE / 2f,
                    mouseY - SKILL_SLOT_SIZE / 2f,
                    SKILL_SLOT_SIZE, SKILL_SLOT_SIZE);
            batch.setColor(1f, 1f, 1f, 1f);
        }

        batch.end();

        batch.begin();
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void renderSection(SpriteBatch batch, String title, List<Skill> skills,
                               float x, float y, float width, float height, Color headerColor, int maxUnlocked) {
        // Section background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(SECTION_COLOR);
        shapeRenderer.rect(x, y, width, height);

        // Header background
        shapeRenderer.setColor(headerColor.r * 0.3f, headerColor.g * 0.3f, headerColor.b * 0.3f, 0.9f);
        shapeRenderer.rect(x, y + height - SECTION_HEADER_HEIGHT, width, SECTION_HEADER_HEIGHT);
        shapeRenderer.end();

        // Section border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(headerColor);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();

        // Draw skill slots
        int skillsPerRow = 3;
        float startY = y + height - SECTION_HEADER_HEIGHT - SKILL_SLOT_PADDING - SKILL_SLOT_SIZE;

        // Get category for limit checking
        SkillCategory category = skills.isEmpty() ? SkillCategory.CLASS : skills.get(0).category;
        int currentUnlocked = countUnlockedByCategory(category);
        boolean limitReached = currentUnlocked >= maxUnlocked;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < skills.size(); i++) {
            int row = i / skillsPerRow;
            int col = i % skillsPerRow;

            float slotX = x + SKILL_SLOT_PADDING + col * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);
            float slotY = startY - row * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);

            Skill skill = skills.get(i);

            if (skill.slottedPosition >= 0) {
                shapeRenderer.setColor(SLOT_SLOTTED_COLOR);
            } else if (skill.unlocked) {
                shapeRenderer.setColor(SLOT_UNLOCKED_COLOR);
            } else if (limitReached) {
                shapeRenderer.setColor(LIMIT_REACHED_COLOR);
            } else {
                shapeRenderer.setColor(SLOT_LOCKED_COLOR);
            }

            shapeRenderer.rect(slotX, slotY, SKILL_SLOT_SIZE, SKILL_SLOT_SIZE);
        }
        shapeRenderer.end();

        // Draw slot borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < skills.size(); i++) {
            int row = i / skillsPerRow;
            int col = i % skillsPerRow;

            float slotX = x + SKILL_SLOT_PADDING + col * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);
            float slotY = startY - row * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);

            Skill skill = skills.get(i);

            if (skill == hoveredSkill) {
                Gdx.gl.glLineWidth(3);
                shapeRenderer.setColor(SELECTED_BORDER_COLOR);
            } else {
                Gdx.gl.glLineWidth(2);
                shapeRenderer.setColor(SLOT_BORDER_COLOR);
            }

            shapeRenderer.rect(slotX, slotY, SKILL_SLOT_SIZE, SKILL_SLOT_SIZE);
        }
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1);
    }

    private void renderSectionContent(SpriteBatch batch, String title, List<Skill> skills,
                                      float x, float y, float width, float height, Color headerColor, int maxUnlocked) {
        SkillCategory category = skills.isEmpty() ? SkillCategory.CLASS : skills.get(0).category;
        int currentUnlocked = countUnlockedByCategory(category);

        // Section title with limit
        font.getData().setScale(0.7f);
        font.setColor(headerColor);
        font.draw(batch, title, x + 10, y + height - 10);

        // Skill icons
        int skillsPerRow = 3;
        float startY = y + height - SECTION_HEADER_HEIGHT - SKILL_SLOT_PADDING - SKILL_SLOT_SIZE;

        for (int i = 0; i < skills.size(); i++) {
            int row = i / skillsPerRow;
            int col = i % skillsPerRow;

            float slotX = x + SKILL_SLOT_PADDING + col * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);
            float slotY = startY - row * (SKILL_SLOT_SIZE + SKILL_SLOT_PADDING);

            Skill skill = skills.get(i);

            // Don't draw icon if it's being dragged
            if (skill == draggingSkill) {
                continue;
            }

            // Draw icon (dimmed if locked)
            if (skill.unlocked) {
                batch.setColor(1f, 1f, 1f, 1f);
            } else {
                batch.setColor(0.4f, 0.4f, 0.4f, 0.8f);
            }

            batch.draw(skill.icon, slotX + 3, slotY + 3, SKILL_SLOT_SIZE - 6, SKILL_SLOT_SIZE - 6);

            // Draw slot number if slotted
            if (skill.slottedPosition >= 0) {
                font.getData().setScale(0.6f);
                font.setColor(Color.YELLOW);
                String slotNum = String.valueOf(skill.slottedPosition + 1);
                font.draw(batch, slotNum, slotX + SKILL_SLOT_SIZE - 12, slotY + 15);
            }
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderTooltip(SpriteBatch batch, Skill skill, int screenWidth, int screenHeight) {
        float mouseX = Gdx.input.getX();
        float mouseY = screenHeight - Gdx.input.getY();

        float tooltipWidth = 250;
        float tooltipHeight = 150;
        float tooltipX = mouseX + 15;
        float tooltipY = mouseY - tooltipHeight - 10;

        if (tooltipX + tooltipWidth > screenWidth) {
            tooltipX = mouseX - tooltipWidth - 15;
        }
        if (tooltipY < 0) {
            tooltipY = mouseY + 10;
        }

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.95f);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapeRenderer.end();

        Color borderColor = getCategoryColor(skill.category);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapeRenderer.end();

        batch.begin();

        font.getData().setScale(0.6f);
        font.setColor(borderColor);
        font.draw(batch, skill.name, tooltipX + 10, tooltipY + tooltipHeight - 10);

        font.getData().setScale(0.45f);
        font.setColor(Color.LIGHT_GRAY);

        String desc = skill.description;
        int maxCharsPerLine = 25;
        float lineY = tooltipY + tooltipHeight - 30;

        while (desc.length() > 0) {
            String line;
            if (desc.length() <= maxCharsPerLine) {
                line = desc;
                desc = "";
            } else {
                int breakPoint = desc.lastIndexOf(' ', maxCharsPerLine);
                if (breakPoint <= 0) breakPoint = maxCharsPerLine;
                line = desc.substring(0, breakPoint);
                desc = desc.substring(breakPoint).trim();
            }
            font.draw(batch, line, tooltipX + 10, lineY);
            lineY -= 12;
        }
    }

    public void renderTooltipAt(SpriteBatch batch, Skill skill, float tooltipX, float tooltipY, int screenWidth, int screenHeight) {
        float tooltipWidth = 250;
        float tooltipHeight = 150;

        if (tooltipX + tooltipWidth > screenWidth) {
            tooltipX = screenWidth - tooltipWidth - 10;
        }
        if (tooltipY + tooltipHeight > screenHeight) {
            tooltipY = screenHeight - tooltipHeight - 10;
        }
        if (tooltipY < 0) {
            tooltipY = 10;
        }

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.95f);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapeRenderer.end();

        Color borderColor = getCategoryColor(skill.category);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        shapeRenderer.end();

        batch.begin();

        font.getData().setScale(0.6f);
        font.setColor(borderColor);
        font.draw(batch, skill.name, tooltipX + 10, tooltipY + tooltipHeight - 10);

        font.getData().setScale(0.45f);
        font.setColor(Color.LIGHT_GRAY);

        String desc = skill.description;
        int maxCharsPerLine = 25;
        float lineY = tooltipY + tooltipHeight - 30;

        while (desc.length() > 0) {
            String line;
            if (desc.length() <= maxCharsPerLine) {
                line = desc;
                desc = "";
            } else {
                int breakPoint = desc.lastIndexOf(' ', maxCharsPerLine);
                if (breakPoint <= 0) breakPoint = maxCharsPerLine;
                line = desc.substring(0, breakPoint);
                desc = desc.substring(breakPoint).trim();
            }
            font.draw(batch, line, tooltipX + 10, lineY);
            lineY -= 12;
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private Color getCategoryColor(SkillCategory category) {
        switch (category) {
            case CLASS: return CLASS_COLOR;
            case MOVEMENT: return MOVEMENT_COLOR;
            case UTILITY: return UTILITY_COLOR;
            default: return Color.WHITE;
        }
    }

    // Methods to get slotted abilities for AbilityManager integration

    public Skill getSlottedSkill(int slot) {
        if (slot >= 0 && slot < MAX_ABILITY_SLOTS) {
            return slottedAbilities[slot];
        }
        return null;
    }

    public String getSlottedSkillId(int slot) {
        Skill skill = getSlottedSkill(slot);
        return skill != null ? skill.id : null;
    }

    public Skill[] getSlottedAbilities() {
        return slottedAbilities;
    }

    public boolean hasSlottedAbility(int slot) {
        return slottedAbilities[slot] != null;
    }

    public int getSlottedCount() {
        return countTotalSlotted();
    }

    public Skill getHoveredSkill() {
        return hoveredSkill;
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}