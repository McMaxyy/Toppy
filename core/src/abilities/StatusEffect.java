package abilities;

public abstract class StatusEffect {
    protected String name;
    protected float duration;
    protected float elapsed;
    protected boolean isActive;
    protected EffectType type;

    public enum EffectType {
        BUFF,
        DEBUFF,
        CROWD_CONTROL,
        DOT
    }

    public StatusEffect(String name, float duration, EffectType type) {
        this.name = name;
        this.duration = duration;
        this.type = type;
        this.elapsed = 0f;
        this.isActive = true;
    }

    public boolean update(float delta) {
        if (!isActive) return false;

        elapsed += delta;

        onUpdate(delta);

        if (elapsed >= duration) {
            onExpire();
            isActive = false;
            return false;
        }

        return true;
    }

    public abstract void onApply();

    public abstract void onUpdate(float delta);

    public abstract void onExpire();

    public boolean isExpired() {
        return !isActive;
    }

    public void remove() {
        isActive = false;
    }

    public String getName() { return name; }
    public float getDuration() { return duration; }
    public float getElapsed() { return elapsed; }
    public float getTimeRemaining() { return duration - elapsed; }
    public boolean isActive() { return isActive; }
    public EffectType getType() { return type; }
}