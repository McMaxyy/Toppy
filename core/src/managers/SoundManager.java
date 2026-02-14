package managers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

import config.GameScreen;
import config.SaveManager;

import java.util.HashMap;
import java.util.Map;

public class SoundManager implements Disposable {

    private static SoundManager instance;

    private Map<String, Music> musicTracks;
    private Music currentMusic;
    private String currentMusicKey;

    private Map<String, Sound> soundEffects;

    private float musicVolume = 0.0f;
    private float sfxVolume = 0.3f;
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;

    private float sfxCooldown = 0f;
    private static final float SFX_SOUND_COOLDOWN_TIME = 0.05f;
    private float hitSoundCooldown = 0f;
    private static final float HIT_SOUND_COOLDOWN_TIME = 0.1f;

    public static final String MUSIC_FOREST = "forest";
    public static final String MUSIC_DUNGEON = "dungeon";
    public static final String MUSIC_BOSS = "boss";
    public static final String MUSIC_MENU = "menu";
    public static final String MUSIC_ENDLESS = "endless";

    public static final String SFX_ENTITY_HIT = "entity_hit";
    public static final String SFX_CONSECRATE = "consecrated_ground";
    public static final String SFX_PICKUP_ITEM = "pickup_item";
    public static final String SFX_PICKUP_COIN = "pickup_coin";
    public static final String SFX_PULL_ABILITY = "pull_sound";
    public static final String SFX_SMITE_ABILITY = "smite_sound";
    public static final String SFX_BLINK_ABILITY = "blink_sound";
    public static final String SFX_PRAYER_ABILITY = "prayer_sound";
    public static final String SFX_SHIELD_BASH = "shield_sound";
    public static final String SFX_SMOKEBOMB_ABILITY = "smoke_sound";
    public static final String SFX_SPRINT_ABILITY = "sprint_sound";
    public static final String SFX_USE_POTION = "potion_sound";
    public static final String SFX_LEMMY = "lemmy_sound";
    public static final String SFX_SPEAR = "spear_sound";
    public static final String SFX_WHIRLWIND_ABILITY = "whirlwind_sound";
    public static final String SFX_DOUBLE_SWING_ABILITY= "doubleSwing_sound";
    public static final String SFX_GROUND_SLAM_ABILITY = "groundSlam_sound";
    public static final String SFX_BLAZING_FURY_ABILITY = "blazingFury_sound";
    public static final String SFX_BUBBLE_ABILITY= "bubble_sound";
    public static final String SFX_LIFE_LEECH_ABILITY = "lifeLeech_sound";
    public static final String SFX_HOLY_AURA_ABILITY= "holyAura_sound";
    public static final String SFX_HOLY_BLESSING_ABILITY = "holyBlessing_sound";
    public static final String SFX_BUTTON = "button_sound";
    public static final String SFX_ENEMY_HIT = "enemyHit_sound";
    public static final String SFX_LEVEL_UP = "levelUp_sound";

    private Music grassRunningSound;
    private boolean isGrassRunningPlaying = false;
    private Music stoneRunningSound;
    private boolean isStoneRunningPlaying = false;

    private SoundManager() {
        musicTracks = new HashMap<>();
        soundEffects = new HashMap<>();
        loadAudio();

        syncWithSaveData();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public void syncWithSaveData() {
        this.musicVolume = SaveManager.getMusicVolume();
        this.sfxVolume = SaveManager.getSfxVolume();
        this.musicEnabled = SaveManager.isMusicEnabled();
        this.sfxEnabled = SaveManager.isSfxEnabled();

        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
        }

        if (grassRunningSound != null) {
            grassRunningSound.setVolume(sfxVolume * 0.6f);
        }
        if (stoneRunningSound != null) {
            stoneRunningSound.setVolume(sfxVolume * 0.4f);
        }
    }

    private void loadAudio() {
        try {
            Music forestMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/ForestSong.mp3"));
            forestMusic.setLooping(true);
            musicTracks.put(MUSIC_FOREST, forestMusic);

            Music dungeonMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/DungeonSong.mp3"));
            dungeonMusic.setLooping(true);
            musicTracks.put(MUSIC_DUNGEON, dungeonMusic);

            Music bossMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/BossSong.mp3"));
            bossMusic.setLooping(true);
            musicTracks.put(MUSIC_BOSS, bossMusic);

            Music menuMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/MainMenuSong.mp3"));
            menuMusic.setLooping(true);
            musicTracks.put(MUSIC_MENU, menuMusic);

            Music endlessMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/EndlessSong.mp3"));
            endlessMusic.setLooping(true);
            musicTracks.put(MUSIC_ENDLESS, endlessMusic);

            Sound hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/SwordSwing.mp3"));
            soundEffects.put(SFX_ENTITY_HIT, hitSound);

            Sound pickupSound = Gdx.audio.newSound(Gdx.files.internal("sounds/ItemPickup.mp3"));
            soundEffects.put(SFX_PICKUP_ITEM, pickupSound);

            Sound pullSound = Gdx.audio.newSound(Gdx.files.internal("sounds/PullSound.mp3"));
            soundEffects.put(SFX_PULL_ABILITY, pullSound);

            Sound smiteSound = Gdx.audio.newSound(Gdx.files.internal("sounds/HolyBlast.mp3"));
            soundEffects.put(SFX_SMITE_ABILITY, smiteSound);

            Sound blinkSound = Gdx.audio.newSound(Gdx.files.internal("sounds/BlinkSound.mp3"));
            soundEffects.put(SFX_BLINK_ABILITY, blinkSound);

            Sound prayerSound = Gdx.audio.newSound(Gdx.files.internal("sounds/PrayerSound.mp3"));
            soundEffects.put(SFX_PRAYER_ABILITY, prayerSound);

            Sound coinSound = Gdx.audio.newSound(Gdx.files.internal("sounds/CoinPickup.wav"));
            soundEffects.put(SFX_PICKUP_COIN, coinSound);

            Sound consecrateSound = Gdx.audio.newSound(Gdx.files.internal("sounds/ConsecratedGround.wav"));
            soundEffects.put(SFX_CONSECRATE, consecrateSound);

            Sound shieldSound = Gdx.audio.newSound(Gdx.files.internal("sounds/ShieldBash.wav"));
            soundEffects.put(SFX_SHIELD_BASH, shieldSound);

            Sound smokebombSound = Gdx.audio.newSound(Gdx.files.internal("sounds/SmokeBomb.mp3"));
            soundEffects.put(SFX_SMOKEBOMB_ABILITY, smokebombSound);

            Sound sprintSound = Gdx.audio.newSound(Gdx.files.internal("sounds/SprintAbility.mp3"));
            soundEffects.put(SFX_SPRINT_ABILITY, sprintSound);

            Sound potionSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Health.mp3"));
            soundEffects.put(SFX_USE_POTION, potionSound);

            Sound lemmySound = Gdx.audio.newSound(Gdx.files.internal("sounds/Lemmy.mp3"));
            soundEffects.put(SFX_LEMMY, lemmySound);

            Sound spearSound = Gdx.audio.newSound(Gdx.files.internal("sounds/SpearAttack.mp3"));
            soundEffects.put(SFX_SPEAR, spearSound);

            Sound whirlwindSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Whirlwind.mp3"));
            soundEffects.put(SFX_WHIRLWIND_ABILITY, whirlwindSound);

            Sound doubleSwingSound = Gdx.audio.newSound(Gdx.files.internal("sounds/DoubleSwing.mp3"));
            soundEffects.put(SFX_DOUBLE_SWING_ABILITY, doubleSwingSound);

            Sound groundSlamSound = Gdx.audio.newSound(Gdx.files.internal("sounds/GroundSlam.mp3"));
            soundEffects.put(SFX_GROUND_SLAM_ABILITY, groundSlamSound);

            Sound blazingFurySound = Gdx.audio.newSound(Gdx.files.internal("sounds/BlazingFury.mp3"));
            soundEffects.put(SFX_BLAZING_FURY_ABILITY, blazingFurySound);

            Sound bubbleSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Bubble.mp3"));
            soundEffects.put(SFX_BUBBLE_ABILITY, bubbleSound);

            Sound lifeLeechSound = Gdx.audio.newSound(Gdx.files.internal("sounds/LifeLeech.mp3"));
            soundEffects.put(SFX_LIFE_LEECH_ABILITY, lifeLeechSound);

            Sound holyAuraSound = Gdx.audio.newSound(Gdx.files.internal("sounds/HolyAura.mp3"));
            soundEffects.put(SFX_HOLY_AURA_ABILITY, holyAuraSound);

            Sound holyBlessingSound = Gdx.audio.newSound(Gdx.files.internal("sounds/HolyBlessing.mp3"));
            soundEffects.put(SFX_HOLY_BLESSING_ABILITY, holyBlessingSound);

            Sound buttonSound = Gdx.audio.newSound(Gdx.files.internal("sounds/ButtonPress.mp3"));
            soundEffects.put(SFX_BUTTON, buttonSound);

            Sound enemyHitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/EnemyHit.mp3"));
            soundEffects.put(SFX_ENEMY_HIT, enemyHitSound);

            Sound levelUpSound = Gdx.audio.newSound(Gdx.files.internal("sounds/LevelUp.mp3"));
            soundEffects.put(SFX_LEVEL_UP, levelUpSound);

            grassRunningSound = Gdx.audio.newMusic(Gdx.files.internal("sounds/GrassRunning.mp3"));
            grassRunningSound.setLooping(true);

            stoneRunningSound = Gdx.audio.newMusic(Gdx.files.internal("sounds/StoneWalking.mp3"));
            stoneRunningSound.setLooping(true);

            System.out.println("SoundManager: All audio loaded successfully");
        } catch (Exception e) {
            System.err.println("SoundManager: Error loading audio files - " + e.getMessage());
        }
    }

    public void update(float delta) {
        if (sfxCooldown > 0) {
            sfxCooldown -= delta;
        }

        if (hitSoundCooldown > 0) {
            hitSoundCooldown -= delta;
        }
    }

    public void playMusic(String musicKey) {
        if (!musicEnabled) return;

        if (musicKey.equals(currentMusicKey) && currentMusic != null && currentMusic.isPlaying()) {
            return;
        }

        stopMusic();

        Music music = musicTracks.get(musicKey);
        if (music != null) {
            music.setVolume(musicVolume);
            music.play();
            currentMusic = music;
            currentMusicKey = musicKey;
        } else {
            System.err.println("SoundManager: Music not found - " + musicKey);
        }
    }

    public void playForestMusic() {
        if (musicEnabled)
            playMusic(MUSIC_FOREST);
    }

    public void playDungeonMusic() {
        if (musicEnabled)
            playMusic(MUSIC_DUNGEON);
    }

    public void playBossMusic() {
        if (musicEnabled)
            playMusic(MUSIC_BOSS);
    }

    public void playMenuMusic() {
        if (musicEnabled)
            playMusic(MUSIC_MENU);
    }

    public void playEndlessMusic() {
        if (musicEnabled)
            playMusic(MUSIC_ENDLESS);
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    public void resumeMusic() {
        if (musicEnabled && currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    public void playSound(String sfxKey) {
        if (!sfxEnabled) return;

        Sound sound = soundEffects.get(sfxKey);
        if (sound != null) {
            sound.play(sfxVolume);
        } else {
            System.err.println("SoundManager: Sound not found - " + sfxKey);
        }
    }

    public void playSound(String sfxKey, float volume) {
        if (!sfxEnabled) return;

        Sound sound = soundEffects.get(sfxKey);
        if (sound != null) {
            sound.play(volume * sfxVolume);
        }
    }

    public void playHitSound(String weapon) {
        if (!sfxEnabled) return;

        switch (weapon) {
            case "Sword":
                if (sfxCooldown <= 0) {
                    playSound(SFX_ENTITY_HIT);
                    sfxCooldown = SFX_SOUND_COOLDOWN_TIME;
                }
                break;
            case "Shield":
                if (sfxCooldown <= 0) {
                    playSound(SFX_SHIELD_BASH, 1.4f);
                    sfxCooldown = SFX_SOUND_COOLDOWN_TIME;
                }
                break;
            case "Spear":
                if (sfxCooldown <= 0) {
                    playSound(SFX_SPEAR, 0.6f);
                    sfxCooldown = SFX_SOUND_COOLDOWN_TIME;
                }
                break;
        }
    }

    public void playEnemyHitSound() {
        if (!sfxEnabled) return;

        if (hitSoundCooldown <= 0) {
            playSound(SFX_ENEMY_HIT, 0.3f);
            hitSoundCooldown = HIT_SOUND_COOLDOWN_TIME;
        }
    }

    public void playLevelUpSound() {
        if (!sfxEnabled) return;

        playSound(SFX_LEVEL_UP);
    }

    public void playButtonSound() {
        if (!sfxEnabled) return;

        playSound(SFX_BUTTON);
    }

    public void playLemmyHitSound() {
        if (!sfxEnabled) return;

        playSound(SFX_LEMMY, 1.5f);
    }

    public void playPotionSound() {
        if (!sfxEnabled) return;

        if (sfxCooldown <= 0) {
            playSound(SFX_USE_POTION);
            sfxCooldown = SFX_SOUND_COOLDOWN_TIME;
        }
    }

    public void playPickupSound() {
        if (!sfxEnabled) return;

        if (sfxCooldown <= 0) {
            playSound(SFX_PICKUP_ITEM);
            sfxCooldown = SFX_SOUND_COOLDOWN_TIME;
        }
    }

    public void playPickupCoinSound() {
        if (!sfxEnabled) return;

        if (sfxCooldown <= 0) {
            playSound(SFX_PICKUP_COIN);
            sfxCooldown = SFX_SOUND_COOLDOWN_TIME;
        }
    }

    public void playAbilitySound(String ability) {
        if (!sfxEnabled) return;

        switch (ability) {
            case "Smite":
                playSound(SFX_SMITE_ABILITY);
                break;
            case "Pull":
                playSound(SFX_PULL_ABILITY);
                break;
            case "Blink":
                playSound(SFX_BLINK_ABILITY, 0.2f);
                break;
            case "Prayer":
                playSound(SFX_PRAYER_ABILITY);
                break;
            case "Consecrate":
                if (sfxCooldown <= 0) {
                    playSound(SFX_CONSECRATE, 1.5f);
                    sfxCooldown = SFX_SOUND_COOLDOWN_TIME;
                }
                break;
            case "SmokeBomb":
                playSound(SFX_SMOKEBOMB_ABILITY);
                break;
            case "Sprint":
                playSound(SFX_SPRINT_ABILITY);
                break;
            case "Whirlwind":
                playSound(SFX_WHIRLWIND_ABILITY);
                break;
            case "DoubleSwing":
                playSound(SFX_DOUBLE_SWING_ABILITY, 0.6f);
                break;
            case "GroundSlam":
                playSound(SFX_GROUND_SLAM_ABILITY);
                break;
            case "BlazingFury":
                playSound(SFX_BLAZING_FURY_ABILITY);
                break;
            case "Bubble":
                playSound(SFX_BUBBLE_ABILITY);
                break;
            case "LifeLeech":
                playSound(SFX_LIFE_LEECH_ABILITY);
                break;
            case "HolyAura":
                playSound(SFX_HOLY_AURA_ABILITY);
                break;
            case "HolyBlessing":
                playSound(SFX_HOLY_BLESSING_ABILITY);
                break;
        }
    }

    public void startGrassRunning() {
        if (!sfxEnabled) return;

        if (grassRunningSound != null && !isGrassRunningPlaying) {
            grassRunningSound.setVolume(sfxVolume * 0.6f);
            grassRunningSound.play();
            isGrassRunningPlaying = true;
        }
    }

    public void stopGrassRunning() {
        if (grassRunningSound != null && isGrassRunningPlaying) {
            grassRunningSound.stop();
            isGrassRunningPlaying = false;
        }
    }

    public void startStoneRunning() {
        if (!sfxEnabled) return;

        if (stoneRunningSound != null && !isStoneRunningPlaying) {
            stoneRunningSound.setVolume(sfxVolume * 0.4f);
            stoneRunningSound.play();
            isStoneRunningPlaying = true;
        }
    }

    public void stopStoneRunning() {
        if (stoneRunningSound != null && isStoneRunningPlaying) {
            stoneRunningSound.stop();
            isStoneRunningPlaying = false;
        }
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0f, Math.min(1f, volume));
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
        }
        SaveManager.setMusicVolume(this.musicVolume);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0f, Math.min(1f, volume));

        if (grassRunningSound != null) {
            grassRunningSound.setVolume(sfxVolume * 0.6f);
        }

        if (stoneRunningSound != null) {
            stoneRunningSound.setVolume(sfxVolume * 0.6f);
        }

        SaveManager.setSfxVolume(this.sfxVolume);
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        }
        SaveManager.setMusicEnabled(enabled);
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setSfxEnabled(boolean enabled) {
        this.sfxEnabled = enabled;
        SaveManager.setSfxEnabled(enabled);
    }

    public boolean isSfxEnabled() {
        return sfxEnabled;
    }

    public void toggleMusic() {
        setMusicEnabled(!musicEnabled);
        if (musicEnabled) {
            if (currentMusicKey != null) {
                playMusic(currentMusicKey);
            } else {
                switch (GameScreen.getCurrentScreen()) {
                    case 0:
                        playMenuMusic();
                        break;
                    case 1:
                        playForestMusic();
                        break;
                    case 2:
                        playDungeonMusic();
                        break;
                    case 3:
                        playBossMusic();
                        break;
                    case 4:
                        playEndlessMusic();
                        break;
                }
            }
        }
    }

    public void toggleSfx() {
        setSfxEnabled(!sfxEnabled);
    }

    public String getCurrentMusicKey() {
        return currentMusicKey;
    }

    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    @Override
    public void dispose() {
        stopGrassRunning();
        if (grassRunningSound != null) {
            grassRunningSound.dispose();
            grassRunningSound = null;
        }

        stopStoneRunning();
        if (stoneRunningSound != null) {
            stoneRunningSound.dispose();
            stoneRunningSound = null;
        }

        for (Music music : musicTracks.values()) {
            if (music != null) {
                music.dispose();
            }
        }
        musicTracks.clear();

        for (Sound sound : soundEffects.values()) {
            if (sound != null) {
                sound.dispose();
            }
        }
        soundEffects.clear();

        currentMusic = null;
        currentMusicKey = null;
        instance = null;
    }

    public static void reset() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }
}