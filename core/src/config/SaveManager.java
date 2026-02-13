package config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

public class SaveManager {

    private static final String SAVE_FILE_NAME = "savegame.json";
    private static final int SAFE_STORAGE_SLOTS = 4;
    private static final int SAFE_STASH_SLOTS = 12;
    private static final int CURRENT_SAFE_STASH_KEY_VERSION = 2;
    private static final String[] SAFE_STASH_KEYS = new String[]{
            "9gXhRZ0c7vY2nQpL", // v1
            "W2nG7qZs5pLc1YhR"  // v2
    };
    private static SaveData currentSaveData = null;

    public static SaveData load() {
        try {
            FileHandle file = Gdx.files.local(SAVE_FILE_NAME);

            if (file.exists()) {
                String jsonString = file.readString();
                Json json = new Json();
                json.setIgnoreUnknownFields(true);

                currentSaveData = json.fromJson(SaveData.class, jsonString);

                if (currentSaveData == null) {
                    currentSaveData = new SaveData();
                }

                if (currentSaveData.keybindings == null) {
                    currentSaveData.keybindings = new java.util.HashMap<>();
                    currentSaveData.setDefaultKeybindings();
                }

                ensureAllKeybindingsExist();
                ensureSafeStorageSlotsExist();
                if (migrateSafeStashSlots()) {
                    save();
                }

                System.out.println("SaveManager: Successfully loaded save data");
            } else {
                System.out.println("SaveManager: No save file found, creating default");
                currentSaveData = new SaveData();
            }
        } catch (Exception e) {
            System.err.println("SaveManager: Error loading save data - " + e.getMessage());
            e.printStackTrace();
            currentSaveData = new SaveData();
        }

        return currentSaveData;
    }

    private static void ensureAllKeybindingsExist() {
        if (currentSaveData == null) return;

        SaveData defaults = new SaveData();

        for (String action : SaveData.getAllActions()) {
            if (!currentSaveData.keybindings.containsKey(action)) {
                currentSaveData.keybindings.put(action, defaults.getKeybinding(action));
            }
        }
    }

    private static void ensureSafeStorageSlotsExist() {
        if (currentSaveData == null) return;

        if (currentSaveData.safeStorageSlots == null || currentSaveData.safeStorageSlots.length != SAFE_STORAGE_SLOTS) {
            currentSaveData.safeStorageSlots = normalizeSlots(currentSaveData.safeStorageSlots, SAFE_STORAGE_SLOTS);
        }

        if (currentSaveData.safeStashSlots == null || currentSaveData.safeStashSlots.length != SAFE_STASH_SLOTS) {
            currentSaveData.safeStashSlots = normalizeSlots(currentSaveData.safeStashSlots, SAFE_STASH_SLOTS);
        }
    }

    private static boolean migrateSafeStashSlots() {
        if (currentSaveData == null || currentSaveData.safeStashSlots == null) return false;

        boolean migrated = false;
        for (int i = 0; i < currentSaveData.safeStashSlots.length; i++) {
            String value = currentSaveData.safeStashSlots[i];
            if (value == null) continue;

            if (value.startsWith("v2:")) {
                continue;
            }

            String decoded = null;
            if (value.startsWith("v1:")) {
                decoded = decodeSafeStashValue(value);
            } else {
                decoded = decodeSafeStashValue("v1:" + value);
                if (decoded == null) {
                    decoded = value;
                }
            }

            if (decoded != null && isLikelyItemId(decoded)) {
                currentSaveData.safeStashSlots[i] = encodeSafeStashValue(decoded);
                migrated = true;
            }
        }
        return migrated;
    }

    private static String[] normalizeSlots(String[] existing, int size) {
        String[] slots = new String[size];
        if (existing != null) {
            int copyLength = Math.min(existing.length, size);
            System.arraycopy(existing, 0, slots, 0, copyLength);
        }
        return slots;
    }

    public static boolean save() {
        if (currentSaveData == null) {
            currentSaveData = new SaveData();
        }

        try {
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);

            String jsonString = json.prettyPrint(currentSaveData);

            FileHandle file = Gdx.files.local(SAVE_FILE_NAME);
            file.writeString(jsonString, false);

            return true;
        } catch (Exception e) {
            System.err.println("SaveManager: Error saving data - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static SaveData getSaveData() {
        if (currentSaveData == null) {
            load();
        }
        return currentSaveData;
    }

    public static void setSaveData(SaveData data) {
        currentSaveData = data;
        save();
    }

    public static float getMusicVolume() {
        return getSaveData().musicVolume;
    }

    public static void setMusicVolume(float volume) {
        getSaveData().musicVolume = Math.max(0f, Math.min(1f, volume));
        save();
    }

    public static float getSfxVolume() {
        return getSaveData().sfxVolume;
    }

    public static void setSfxVolume(float volume) {
        getSaveData().sfxVolume = Math.max(0f, Math.min(1f, volume));
        save();
    }

    public static boolean isMusicEnabled() {
        return getSaveData().musicEnabled;
    }

    public static void setMusicEnabled(boolean enabled) {
        getSaveData().musicEnabled = enabled;
        save();
    }

    public static boolean isSfxEnabled() {
        return getSaveData().sfxEnabled;
    }

    public static void setSfxEnabled(boolean enabled) {
        getSaveData().sfxEnabled = enabled;
        save();
    }

    public static boolean isFullscreen() {
        return getSaveData().fullscreen;
    }

    public static void setFullscreen(boolean fullscreen) {
        getSaveData().fullscreen = fullscreen;
        save();
    }

    public static int getWindowWidth() {
        return getSaveData().windowWidth;
    }

    public static int getWindowHeight() {
        return getSaveData().windowHeight;
    }

    public static void setWindowSize(int width, int height) {
        getSaveData().windowWidth = width;
        getSaveData().windowHeight = height;
        save();
    }

    public static boolean isScreenShakeEnabled() {
        return getSaveData().screenShakeEnabled;
    }

    public static void setScreenShakeEnabled(boolean enabled) {
        getSaveData().screenShakeEnabled = enabled;
        save();
    }

    public static boolean isLegendEnabled() {
        return getSaveData().legendEnabled;
    }

    public static void setLegendEnabled(boolean enabled) {
        getSaveData().legendEnabled = enabled;
        save();
    }

    public static int[] getKeybinding(String action) {
        return getSaveData().getKeybinding(action);
    }

    public static void setKeybinding(String action, int[] keys) {
        getSaveData().setKeybinding(action, keys);
        save();
    }

    public static String getKeybindingDisplayString(String action) {
        return getSaveData().getKeybindingDisplayString(action);
    }

    public static void resetKeybindingsToDefault() {
        getSaveData().setDefaultKeybindings();
        save();
    }

    public static boolean isActionKey(String action, int keycode) {
        int[] binding = getKeybinding(action);
        if (binding == null) return false;

        if (binding.length == 1) {
            return binding[0] == keycode;
        }

        for (int key : binding) {
            if (key == keycode) return true;
        }
        return false;
    }

    public static boolean isActionPressed(String action) {
        int[] binding = getKeybinding(action);
        if (binding == null || binding.length == 0) return false;

        for (int key : binding) {
            if (!Gdx.input.isKeyPressed(key)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isActionJustPressed(String action) {
        int[] binding = getKeybinding(action);
        if (binding == null || binding.length == 0) return false;

        if (binding.length == 1) {
            return Gdx.input.isKeyJustPressed(binding[0]);
        }

        boolean anyJustPressed = false;
        boolean allPressed = true;

        for (int key : binding) {
            if (Gdx.input.isKeyJustPressed(key)) {
                anyJustPressed = true;
            }
            if (!Gdx.input.isKeyPressed(key)) {
                allPressed = false;
            }
        }

        return anyJustPressed && allPressed;
    }

    public static String getStorageSlot1() {
        return getSaveData().storageSlot1;
    }

    public static void setStorageSlot1(String encodedItem) {
        getSaveData().storageSlot1 = encodedItem;
        save();
    }

    public static String getStorageSlot2() {
        return getSaveData().storageSlot2;
    }

    public static void setStorageSlot2(String encodedItem) {
        getSaveData().storageSlot2 = encodedItem;
        save();
    }

    public static String[] getSafeStorageSlots() {
        SaveData data = getSaveData();
        ensureSafeStorageSlotsExist();
        return data.safeStorageSlots;
    }

    public static void setSafeStorageSlots(String[] slots) {
        getSaveData().safeStorageSlots = normalizeSlots(slots, SAFE_STORAGE_SLOTS);
        save();
    }

    public static String[] getSafeStashSlots() {
        SaveData data = getSaveData();
        ensureSafeStorageSlotsExist();
        return decodeSafeStashSlots(data.safeStashSlots);
    }

    public static void setSafeStashSlots(String[] slots) {
        String[] normalized = normalizeSlots(slots, SAFE_STASH_SLOTS);
        getSaveData().safeStashSlots = encodeSafeStashSlots(normalized);
        save();
    }

    private static String[] encodeSafeStashSlots(String[] slots) {
        String[] encoded = new String[SAFE_STASH_SLOTS];
        if (slots == null) return encoded;

        for (int i = 0; i < SAFE_STASH_SLOTS; i++) {
            if (slots[i] != null) {
                encoded[i] = encodeSafeStashValue(slots[i]);
            }
        }
        return encoded;
    }

    private static String[] decodeSafeStashSlots(String[] encodedSlots) {
        String[] decoded = new String[SAFE_STASH_SLOTS];
        if (encodedSlots == null) return decoded;

        for (int i = 0; i < SAFE_STASH_SLOTS && i < encodedSlots.length; i++) {
            if (encodedSlots[i] != null) {
                decoded[i] = decodeSafeStashValue(encodedSlots[i]);
            }
        }
        return decoded;
    }

    private static String encodeSafeStashValue(String value) {
        try {
            byte[] input = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] key = getSafeStashKey(CURRENT_SAFE_STASH_KEY_VERSION)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] output = new byte[input.length];

            for (int i = 0; i < input.length; i++) {
                output[i] = (byte)(input[i] ^ key[i % key.length]);
            }

            return "v" + CURRENT_SAFE_STASH_KEY_VERSION + ":" +
                    java.util.Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            return null;
        }
    }

    private static String decodeSafeStashValue(String value) {
        try {
            int version = parseSafeStashVersion(value);
            String encoded = stripSafeStashPrefix(value, version);

            byte[] input = java.util.Base64.getDecoder().decode(encoded);
            byte[] key = getSafeStashKey(version).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] output = new byte[input.length];

            for (int i = 0; i < input.length; i++) {
                output[i] = (byte)(input[i] ^ key[i % key.length]);
            }

            String decoded = new String(output, java.nio.charset.StandardCharsets.UTF_8);
            return isLikelyItemId(decoded) ? decoded : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getSafeStashKey(int version) {
        int index = Math.max(1, version) - 1;
        if (index >= SAFE_STASH_KEYS.length) {
            index = SAFE_STASH_KEYS.length - 1;
        }
        return SAFE_STASH_KEYS[index];
    }

    private static int parseSafeStashVersion(String value) {
        if (value == null) return 1;
        if (value.startsWith("v2:")) return 2;
        if (value.startsWith("v1:")) return 1;
        return 1;
    }

    private static String stripSafeStashPrefix(String value, int version) {
        String prefix = "v" + version + ":";
        if (value != null && value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }

    private static boolean isLikelyItemId(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') ||
                    (ch >= '0' && ch <= '9') ||
                    ch == '_';
            if (!ok) return false;
        }
        return true;
    }

    public static void resetAllData() {
        currentSaveData = new SaveData();
        save();
    }
}
