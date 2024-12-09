package com.html.template;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import config.Boot;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setTitle("Cascad");
		
		config.setWindowedMode(1280, 720);
		config.setDecorated(true);
		config.setResizable(false);
		
		int samples = 32;
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, samples);
		
		new Lwjgl3Application(new Boot(), config);
	}
}
