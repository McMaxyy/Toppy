package config;

import com.badlogic.gdx.Input;

import java.util.HashMap;
import java.util.Map;

public class SaveData {

    public int version = 1;

    public float musicVolume = 0.7f;
    public float sfxVolume = 0.3f;
    public boolean musicEnabled = true;
    public boolean sfxEnabled = true;
    public boolean fullscreen = true;
    public int windowWidth = 1920;
    public int windowHeight = 1080;

    public boolean screenShakeEnabled = true;
    public boolean legendEnabled = true;

    public Map<String, int[]> keybindings;

    public String storageSlot1 = null;
    public String storageSlot2 = null;
    public String[] safeStorageSlots;
    public String[] safeStashSlots;

    public SaveData() {
        keybindings = new HashMap<>();
        setDefaultKeybindings();
        safeStorageSlots = new String[4];
        safeStashSlots = new String[12];
    }

    public void setDefaultKeybindings() {
        keybindings.clear();

        keybindings.put("ability1", new int[]{Input.Keys.SPACE});
        keybindings.put("ability2", new int[]{Input.Keys.NUM_2});
        keybindings.put("ability3", new int[]{Input.Keys.NUM_3});
        keybindings.put("ability4", new int[]{Input.Keys.NUM_4});
        keybindings.put("ability5", new int[]{Input.Keys.NUM_5});

        keybindings.put("consumable1", new int[]{Input.Keys.E});
        keybindings.put("consumable2", new int[]{Input.Keys.Q});
    }

    public int[] getKeybinding(String action) {
        return keybindings.get(action);
    }

    public void setKeybinding(String action, int[] keys) {
        if (keys != null && keys.length > 0 && keys.length <= 2) {
            keybindings.put(action, keys);
        }
    }

    public boolean isKeybindingPressed(String action, int[] pressedKeys) {
        int[] binding = keybindings.get(action);
        if (binding == null || pressedKeys == null) return false;

        if (binding.length != pressedKeys.length) return false;

        for (int key : binding) {
            boolean found = false;
            for (int pressed : pressedKeys) {
                if (key == pressed) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    public String getKeybindingDisplayString(String action) {
        int[] keys = keybindings.get(action);
        if (keys == null || keys.length == 0) {
            return "None";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(Input.Keys.toString(keys[i]));
        }
        return sb.toString();
    }

    public static String getActionDisplayName(String action) {
        switch (action) {
            case "ability1": return "Ability 1";
            case "ability2": return "Ability 2";
            case "ability3": return "Ability 3";
            case "ability4": return "Ability 4";
            case "ability5": return "Ability 5";
            case "consumable1": return "Consumable 1";
            case "consumable2": return "Consumable 2";
            default: return action;
        }
    }

    public static String[] getAllActions() {
        return new String[]{
                "ability1", "ability2", "ability3", "ability4", "ability5",
                "consumable1", "consumable2"
        };
    }
}
