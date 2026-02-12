package ui;

import java.util.Random;
import com.badlogic.gdx.math.Vector3;
import config.SaveManager;

public class ScreenShake {
    private static float time = 0;
    private static float currentTime = 0;
    private static float power = 0;
    private static float currentPower = 0;
    private static Random random;
    private static Vector3 pos = new Vector3();

    // Cooldown system
    private static float cooldownTimer = 0;
    private static final float COOLDOWN_DURATION = 0.1f;

    public static void rumble(float rumblePower, float rumbleLength) {
        if (!SaveManager.isScreenShakeEnabled()) {
            return;
        }

        if (cooldownTimer <= 0) {
            random = new Random();
            power = rumblePower;
            time = rumbleLength;
            currentTime = 0;
            cooldownTimer = COOLDOWN_DURATION;
        }
    }

    public static Vector3 tick(float delta) {
        if (cooldownTimer > 0) {
            cooldownTimer -= delta;
        }

        if (currentTime <= time) {
            currentPower = power * ((time - currentTime) / time);

            if (random != null) {
                pos.x = (random.nextFloat() - 0.5f) * 2 * currentPower;
                pos.y = (random.nextFloat() - 0.5f) * 2 * currentPower;
            }

            currentTime += delta;
        } else {
            time = 0;
            pos.x = 0;
            pos.y = 0;
        }
        return pos;
    }

    public static float getRumbleTimeLeft() {
        return time;
    }

    public static Vector3 getPos() {
        return pos;
    }

    public static boolean isOnCooldown() {
        return cooldownTimer > 0;
    }

    public static void reset() {
        time = 0;
        currentTime = 0;
        power = 0;
        currentPower = 0;
        cooldownTimer = 0;
        pos.set(0, 0, 0);
    }
}