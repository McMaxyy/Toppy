package config;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import game.GameProj;
import game.StartScreen;

public class GameScreen implements Screen {
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

	private boolean isDisposed = false;

	public GameScreen(Game game) {
		this.game = game;
		viewport = new FitViewport(SELECTED_WIDTH, SELECTED_HEIGHT);

		Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
//		Gdx.graphics.setUndecorated(false);
//		Gdx.graphics.setWindowedMode(1600, 900);

		Gdx.graphics.setUndecorated(true);
		Gdx.graphics.setWindowedMode(displayMode.width, displayMode.height);

		setCurrentState(START);
	}

	public void setCurrentState(int newState) {
		if (isDisposed) return;

		// Dispose previous state resources BEFORE changing state
		if (currentState == HOME && newState == START) {
			// When switching from game to start screen, dispose game properly
			if (gameP != null) {
				gameP.dispose();
				gameP = null;
			}
		} else if (currentState == START && newState == HOME) {
			// When switching from start screen to game, dispose start screen
			if (startScreen != null) {
				startScreen.dispose();
				startScreen = null;
			}
		}

		currentState = newState;

		// Create new state resources AFTER disposing old ones
		switch (currentState) {
			case START:
				startScreen = new StartScreen(viewport, game, this);
				Gdx.input.setInputProcessor(startScreen.getStage());
				break;
			case HOME:
				gameP = new GameProj(viewport, game, this);
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
		if (currentState == START && startScreen != null) {
			Gdx.input.setInputProcessor(startScreen.getStage());
		} else if (currentState == HOME && gameP != null) {
			Gdx.input.setInputProcessor(gameP.stage);
		}
	}

	@Override
	public void render(float delta) {
		if (isDisposed) return;

		Gdx.gl.glClearColor(55 / 255f, 55 / 255f, 55 / 255f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		switch (currentState) {
			case START:
				if (startScreen != null && !startScreen.isDisposed()) {
					startScreen.render(delta);
				}
				break;
			case HOME:
				if (gameP != null) {
					gameP.render(delta);
				}
				break;
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
		viewport.apply();
		viewport.getCamera().update();

		// Only resize the CURRENT state
		if (currentState == START && startScreen != null && !startScreen.isDisposed()) {
			startScreen.resize(width, height);
		} else if (currentState == HOME && gameP != null) {
			gameP.resize(width, height);
		}
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
	}

	@Override
	public void dispose() {
		if (isDisposed) return;
		isDisposed = true;

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