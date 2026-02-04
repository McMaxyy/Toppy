package managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

public class SoundManager implements Disposable {

    private static SoundManager instance;

    // Music tracks
    private Map<String, Music> musicTracks;
    private Music currentMusic;
    private String currentMusicKey;

    // Sound effects
    private Map<String, Sound> soundEffects;

    // Volume settings
    private float musicVolume = 0.0f;
    private float sfxVolume = 0.3f;
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;

    private float hitSoundCooldown = 0f;
    private static final float HIT_SOUND_COOLDOWN_TIME = 0.05f;

    // Music keys
    public static final String MUSIC_FOREST = "forest";
    public static final String MUSIC_DUNGEON = "dungeon";
    public static final String MUSIC_BOSS = "boss";

    // SFX keys
    public static final String SFX_ENTITY_HIT = "entity_hit";
    public static final String SFX_CONSECRATE = "consecrated_ground";
    public static final String SFX_PICKUP_ITEM = "pickup_item";
    public static final String SFX_PICKUP_COIN = "pickup_coin";
    public static final String SFX_PULL_ABILITY = "pull_sound";
    public static final String SFX_SMITE_ABILITY = "smite_sound";
    public static final String SFX_BLINK_ABILITY = "blink_sound";
    public static final String SFX_PRAYER_ABILITY = "prayer_sound";
    public static final String SFX_SHIELD_BASH = "shield_sound";



    private Music grassRunningSound;
    private boolean isGrassRunningPlaying = false;
    private Music stoneRunningSound;
    private boolean isStoneRunningPlaying = false;
    private SoundManager() {
        musicTracks = new HashMap<>();
        soundEffects = new HashMap<>();
        loadAudio();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
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
        playMusic(MUSIC_FOREST);
    }

    public void playDungeonMusic() {
        playMusic(MUSIC_DUNGEON);
    }

    public void playBossMusic() {
        playMusic(MUSIC_BOSS);
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
                if (hitSoundCooldown <= 0) {
                    playSound(SFX_ENTITY_HIT);
                    hitSoundCooldown = HIT_SOUND_COOLDOWN_TIME;
                }
                break;
            case "Shield":
                if (hitSoundCooldown <= 0) {
                    playSound(SFX_SHIELD_BASH, 1.4f);
                    hitSoundCooldown = HIT_SOUND_COOLDOWN_TIME;
                }
                break;
        }
    }

    public void playPickupSound() {
        if (!sfxEnabled) return;

        if (hitSoundCooldown <= 0) {
            playSound(SFX_PICKUP_ITEM);
            hitSoundCooldown = HIT_SOUND_COOLDOWN_TIME;
        }
    }

    public void playPickupCoinSound() {
        if (!sfxEnabled) return;

        if (hitSoundCooldown <= 0) {
            playSound(SFX_PICKUP_COIN);
            hitSoundCooldown = HIT_SOUND_COOLDOWN_TIME;
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
                if (hitSoundCooldown <= 0) {
                    playSound(SFX_CONSECRATE, 1.5f);
                    hitSoundCooldown = HIT_SOUND_COOLDOWN_TIME;
                }
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
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        }
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setSfxEnabled(boolean enabled) {
        this.sfxEnabled = enabled;
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
                playForestMusic();
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