package config;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import game.GameProj;
import game.PostGame;
import game.StartScreen;

public class GameScreen implements Screen {
	private Game game;
	private Viewport viewport;
	private GameProj gameP;
	private StartScreen startScreen;
	private PostGame postGameScreen;

	private static final int MIN_WIDTH = 1280;
	private static final int MIN_HEIGHT = 720;
	public static int SELECTED_WIDTH = MIN_WIDTH;
	public static int SELECTED_HEIGHT = MIN_HEIGHT;

	private int currentState;
	public static final int HOME = 1;
	public static final int START = 2;
	public static final int POSTGAME = 3;

	private boolean isDisposed = false;

	public GameScreen(Game game, int sWidth, int sHeight, boolean decorated) {
		this.game = game;
		viewport = new FitViewport(SELECTED_WIDTH, SELECTED_HEIGHT);

		Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
//    Gdx.graphics.setUndecorated(false);
//    Gdx.graphics.setWindowedMode(1600, 900);

		Gdx.graphics.setUndecorated(decorated);
		Gdx.graphics.setWindowedMode(sWidth, sHeight);

		setCurrentState(START);
	}

	public void setCurrentState(int newState) {
		if (isDisposed) return;

		// Clean up previous state
		if (currentState == HOME && newState != HOME) {
			if (gameP != null) {
				gameP.dispose();
				gameP = null;
			}
		} else if (currentState == START && newState != START) {
			if (startScreen != null) {
				startScreen.dispose();
				startScreen = null;
			}
		} else if (currentState == POSTGAME && newState != POSTGAME) {
			if (postGameScreen != null) {
				postGameScreen.dispose();
				postGameScreen = null;
			}
		}

		currentState = newState;

		switch (currentState) {
			case START:
				startScreen = new StartScreen(viewport, game, this);
				Gdx.input.setInputProcessor(startScreen.getStage());
				break;
			case HOME:
				gameP = new GameProj(viewport, game, this);
				Gdx.input.setInputProcessor(gameP.stage);
				break;
			case POSTGAME:
				// PostGame screen will be created by switchToNewState with stats
				break;
		}
	}

	public void switchToNewState(int scene) {
		if (isDisposed) return;

		if (scene == POSTGAME) {
			if (gameP != null) {
				float totalTime = gameP.getTotalGameTime();
				float endlessTime = gameP.getEndlessTimeSurvived();
				int playerLevel = gameP.getPlayer().getLevel();
				int endlessKills = gameP.getCurrentEndlessRoom() != null ?
						gameP.getCurrentEndlessRoom().getTotalEnemiesKilled() : 0;

				if (currentState == HOME && gameP != null) {
					gameP.dispose();
					gameP = null;
				}

				currentState = POSTGAME;
				postGameScreen = new PostGame(this, totalTime, endlessTime, playerLevel, endlessKills);

				Gdx.input.setInputProcessor(postGameScreen.stage);
			}
		} else {
			setCurrentState(scene);

			if (scene != POSTGAME && game.getScreen() != this) {
				game.setScreen(this);
			}
		}
	}

	@Override
	public void show() {
		if (currentState == START && startScreen != null) {
			Gdx.input.setInputProcessor(startScreen.getStage());
		} else if (currentState == HOME && gameP != null) {
			Gdx.input.setInputProcessor(gameP.stage);
		} else if (currentState == POSTGAME && postGameScreen != null) {
			Gdx.input.setInputProcessor(postGameScreen.stage);
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
			case POSTGAME:
				if (postGameScreen != null) {
					postGameScreen.render(delta);
				}
				break;
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
		viewport.apply();
		viewport.getCamera().update();

		if (currentState == START && startScreen != null && !startScreen.isDisposed()) {
			startScreen.resize(width, height);
		} else if (currentState == HOME && gameP != null) {
			gameP.resize(width, height);
		} else if (currentState == POSTGAME && postGameScreen != null) {
			postGameScreen.resize(width, height);
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

		if (postGameScreen != null) {
			postGameScreen.dispose();
			postGameScreen = null;
		}
	}
}