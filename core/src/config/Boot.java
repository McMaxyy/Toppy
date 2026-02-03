package config;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

public class Boot extends Game {
	
	@Override
	public void create () {
		Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
		setScreen(new GameScreen(this, displayMode.width, displayMode.height, true));
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose () {
		super.dispose();
	}
}
