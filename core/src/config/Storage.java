package config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

public class Storage {
    private static Storage instance = null;
    public Skin skin;
    public TextButton.TextButtonStyle buttonStyle;
    public LabelStyle labelStyle;
    public TextFieldStyle textStyle;
    public BitmapFont font;
    public static AssetManager assetManager = new AssetManager();
    private static boolean newLoad = true;    
    
    public static synchronized Storage getInstance()  {
        if (instance == null) {
            instance = new Storage();
        }
        return instance;
    }
    
    public Storage() {
        skin = new Skin(Gdx.files.internal("buttons/newskin/newskin.json"));    
        if(newLoad) {
            newLoad = false;
            loadAssets();
        }        
    }    
    
    public static void loadAssets() {
        // Load the assets required for the game
        assetManager.load("tiles/tree.png", Texture.class);
        assetManager.load("tiles/rock.png", Texture.class);
        assetManager.load("tiles/tree2.png", Texture.class);
        assetManager.load("tiles/rock2.png", Texture.class);
        assetManager.load("tiles/tree3.png", Texture.class);
        assetManager.load("tiles/rock3.png", Texture.class);
        assetManager.load("tiles/rock4.png", Texture.class);
        assetManager.load("tiles/green_tile.png", Texture.class);
        
        assetManager.load("character/Running.png", Texture.class);
        assetManager.load("character/Idle.png", Texture.class);
        assetManager.load("character/Dying.png", Texture.class);
        assetManager.load("character/Attacking.png", Texture.class);
        assetManager.load("character/Spear.png", Texture.class);
        
        assetManager.load("enemy.png", Texture.class);
        assetManager.load("white_pixel.png", Texture.class);
        
        assetManager.load("fonts/Cascadia.fnt", BitmapFont.class);    
        
        assetManager.finishLoading();
    }
    
    public void createFont() {
        font = assetManager.get("fonts/Cascadia.fnt", BitmapFont.class);
        
        Texture borderTextureUp = new Texture(Gdx.files.internal("buttons/newskin/newskin_data/textbutton.9.png"));
        Texture borderTextureDown = new Texture(Gdx.files.internal("buttons/newskin/newskin_data/textbutton-down.9.png"));
        
        NinePatch borderPatchUp = new NinePatch(borderTextureUp, 1, 1, 1, 1);
        NinePatch borderPatchDown = new NinePatch(borderTextureDown, 1, 1, 1, 1);
      
        buttonStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        buttonStyle.up = new NinePatchDrawable(borderPatchUp);
        buttonStyle.down = new NinePatchDrawable(borderPatchDown);
        buttonStyle.font = font;
        
        labelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        labelStyle.font = font;  
        
        textStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        textStyle.font = font;  
    }
}
