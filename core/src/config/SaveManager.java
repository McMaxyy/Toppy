package config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

public class SaveManager {

    private static final String SAVE_FILE_NAME = "savegame.json";
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

        // For single key bindings
        if (binding.length == 1) {
            return binding[0] == keycode;
        }

        // For combo bindings, check if this is one of the keys
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

    public static void resetAllData() {
        currentSaveData = new SaveData();
        save();
    }
}