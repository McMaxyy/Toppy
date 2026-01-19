package config;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import game.GameProj;
import game.StartScreen; // Import the StartScreen

public class GameScreen implements Screen{
	private Game game;
	private Viewport viewport;
	private GameProj gameP;
	private StartScreen startScreen;

	private static final int MIN_WIDTH = 1280;
	private static final int MIN_HEIGHT = 720;
	public static int SELECTED_WIDTH = MIN_WIDTH;
	public static int SELECTED_HEIGHT = MIN_HEIGHT;

	private int currentState;
	public static final int HOME = 1;
	public static final int START = 2;

	public GameScreen(Game game) {
		this.game = game;
		viewport = new FitViewport(SELECTED_WIDTH, SELECTED_HEIGHT);
//		Gdx.graphics.setUndecorated(false);
//		Gdx.graphics.setWindowedMode(1920, 1080);

		Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
		Gdx.graphics.setUndecorated(true);
		Gdx.graphics.setWindowedMode(displayMode.width, displayMode.height);

		setCurrentState(START);
	}

	public void setCurrentState(int newState) {
		if (currentState == HOME && newState == START) {
			// When switching from game to start screen, dispose game properly
			if (gameP != null) {
				gameP.dispose();
				gameP = null;
			}
		}

		currentState = newState;

		switch(currentState) {
			case START:
				startScreen = new StartScreen(viewport, game, this);
				Gdx.input.setInputProcessor(startScreen.getStage());
				break;
			case HOME:
				this.gameP = new GameProj(viewport, game, this);
				Gdx.input.setInputProcessor(gameP.stage);
				break;
		}
	}

	public void switchToNewState(int scene) {
		setCurrentState(scene);
	}

	@Override
	public void show() {
		// When screen is shown, set input processor based on current state
		if (currentState == START) {
			Gdx.input.setInputProcessor(startScreen.getStage());
		} else if (currentState == HOME) {
			Gdx.input.setInputProcessor(gameP.stage);
		}
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(55 / 255f, 55 / 255f, 55 / 255f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		switch(currentState) {
			case START:
				startScreen.render(delta);
				break;
			case HOME:
				gameP.render(delta);
				break;
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
		viewport.apply();
		viewport.getCamera().update();

		// Update StartScreen viewport
		if (startScreen != null) {
			startScreen.resize(width, height);
		}
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// Dispose StartScreen
		if (startScreen != null) {
			startScreen.dispose();
			startScreen = null;
		}

		// Dispose GameProj
		if (gameP != null) {
			gameP.dispose();
			gameP = null;
		}
	}
}