package abilities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import config.Storage;
import entities.*;

import java.util.List;
import java.util.Map;

public class StatusEffectRenderer {

    private static Texture bleedIcon;
    private static Texture stunIcon;
    private static Texture consecratedIcon;

    private static final float ICON_SIZE = 12f;
    private static final float ICON_SPACING = 2f;
    private static final float ICON_OFFSET_Y = 8f;
    private static final float PULSE_SPEED = 4f;

    private static boolean iconsLoaded = false;

    public static void loadIcons() {
        if (iconsLoaded) return;

        try {
            bleedIcon = Storage.assetManager.get("icons/effects/Bleed.png", Texture.class);
        } catch (Exception e) {
            bleedIcon = null;
        }

        try {
            stunIcon = Storage.assetManager.get("icons/effects/Stunned.png", Texture.class);
        } catch (Exception e) {
            stunIcon = null;
        }

        try {
            consecratedIcon = Storage.assetManager.get("icons/effects/Consecrated.png", Texture.class);
        } catch (Exception e) {
            consecratedIcon = null;
        }

        iconsLoaded = true;
    }

    public static void render(SpriteBatch batch, Map<Object, List<StatusEffect>> statusEffects, float globalTimer) {
        if (!iconsLoaded) {
            loadIcons();
        }

        for (Map.Entry<Object, List<StatusEffect>> entry : statusEffects.entrySet()) {
            Object target = entry.getKey();
            List<StatusEffect> effects = entry.getValue();

            if (effects.isEmpty()) continue;

            Vector2 position = getEntityPosition(target);
            float entityHeight = getEntityHeight(target);

            if (position == null) continue;

            float iconY = position.y + entityHeight / 2f + ICON_OFFSET_Y;
            float totalWidth = effects.size() * ICON_SIZE + (effects.size() - 1) * ICON_SPACING;
            float startX = position.x - totalWidth / 2f;

            int iconIndex = 0;
            for (StatusEffect effect : effects) {
                Texture icon = getIconForEffect(effect);
                if (icon != null) {
                    float iconX = startX + iconIndex * (ICON_SIZE + ICON_SPACING);

                    float pulse = 0.8f + 0.2f * (float) Math.sin(globalTimer * PULSE_SPEED + iconIndex);
                    float scale = pulse;

                    if (effect instanceof ConsecratedEffect) {
                        float timeRemaining = effect.getTimeRemaining();
                        float urgency = 1f - (timeRemaining / effect.getDuration());
                        pulse = 0.7f + 0.3f * (float) Math.sin(globalTimer * (PULSE_SPEED + urgency * 8f));

                        if (timeRemaining < 0.5f) {
                            batch.setColor(1f, 1f, 1f, pulse);
                        } else {
                            batch.setColor(1f, 0.9f, 0.5f, pulse);
                        }
                    } else if (effect instanceof BleedEffect) {
                        batch.setColor(1f, 0.3f, 0.3f, pulse);
                    } else if (effect instanceof StunEffect) {
                        batch.setColor(1f, 1f, 0.3f, pulse);
                    } else {
                        batch.setColor(1f, 1f, 1f, pulse);
                    }

                    float drawSize = ICON_SIZE * scale;
                    float offsetX = (ICON_SIZE - drawSize) / 2f;
                    float offsetY = (ICON_SIZE - drawSize) / 2f;

                    batch.draw(icon,
                            iconX + offsetX,
                            iconY + offsetY,
                            drawSize,
                            drawSize);

                    batch.setColor(1f, 1f, 1f, 1f);
                    iconIndex++;
                }
            }
        }
    }

    public static void renderForEntity(SpriteBatch batch, Object entity, List<StatusEffect> effects, float globalTimer) {
        if (!iconsLoaded) {
            loadIcons();
        }

        if (effects == null || effects.isEmpty()) return;

        Vector2 position = getEntityPosition(entity);
        float entityHeight = getEntityHeight(entity);

        if (position == null) return;

        float iconY = position.y + entityHeight / 2f + ICON_OFFSET_Y;
        float totalWidth = effects.size() * ICON_SIZE + (effects.size() - 1) * ICON_SPACING;
        float startX = position.x - totalWidth / 2f;

        int iconIndex = 0;
        for (StatusEffect effect : effects) {
            Texture icon = getIconForEffect(effect);
            if (icon != null) {
                float iconX = startX + iconIndex * (ICON_SIZE + ICON_SPACING);

                float pulse = 0.8f + 0.2f * (float) Math.sin(globalTimer * PULSE_SPEED + iconIndex);

                if (effect instanceof ConsecratedEffect) {
                    float timeRemaining = effect.getTimeRemaining();
                    float urgency = 1f - (timeRemaining / effect.getDuration());
                    pulse = 0.7f + 0.3f * (float) Math.sin(globalTimer * (PULSE_SPEED + urgency * 8f));

                    if (timeRemaining < 0.5f) {
                        batch.setColor(1f, 1f, 1f, pulse);
                    } else {
                        batch.setColor(1f, 0.9f, 0.5f, pulse);
                    }
                } else if (effect instanceof BleedEffect) {
                    batch.setColor(1f, 0.3f, 0.3f, pulse);
                } else if (effect instanceof StunEffect) {
                    batch.setColor(1f, 1f, 0.3f, pulse);
                } else {
                    batch.setColor(1f, 1f, 1f, pulse);
                }

                float drawSize = ICON_SIZE * pulse;
                float offsetX = (ICON_SIZE - drawSize) / 2f;
                float offsetY = (ICON_SIZE - drawSize) / 2f;

                batch.draw(icon, iconX + offsetX, iconY + offsetY, drawSize, drawSize);

                batch.setColor(1f, 1f, 1f, 1f);
                iconIndex++;
            }
        }
    }

    private static Vector2 getEntityPosition(Object entity) {
        if (entity instanceof Enemy) {
            Enemy e = (Enemy) entity;
            if (e.getBody() != null) {
                return e.getBody().getPosition();
            }
        } else if (entity instanceof DungeonEnemy) {
            DungeonEnemy e = (DungeonEnemy) entity;
            if (e.getBody() != null) {
                return e.getBody().getPosition();
            }
        } else if (entity instanceof BossKitty) {
            BossKitty e = (BossKitty) entity;
            if (e.getBody() != null) {
                return e.getBody().getPosition();
            }
        } else if (entity instanceof Cyclops) {
            Cyclops e = (Cyclops) entity;
            if (e.getBody() != null) {
                return e.getBody().getPosition();
            }
        }
        return null;
    }

    private static float getEntityHeight(Object entity) {
        if (entity instanceof Enemy) {
            return ((Enemy) entity).bounds.height;
        } else if (entity instanceof DungeonEnemy) {
            return ((DungeonEnemy) entity).bounds.height;
        } else if (entity instanceof BossKitty) {
            return ((BossKitty) entity).bounds.height;
        } else if (entity instanceof Cyclops) {
            return ((Cyclops) entity).bounds.height;
        }
        return 16f;
    }

    private static Texture getIconForEffect(StatusEffect effect) {
        if (effect instanceof BleedEffect) {
            return bleedIcon;
        } else if (effect instanceof StunEffect) {
            return stunIcon;
        } else if (effect instanceof ConsecratedEffect) {
            return consecratedIcon;
        }
        return null;
    }
}