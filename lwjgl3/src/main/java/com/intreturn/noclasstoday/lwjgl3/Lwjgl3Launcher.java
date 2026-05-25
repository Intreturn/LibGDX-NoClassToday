package com.intreturn.noclasstoday.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.intreturn.noclasstoday.MyGdxGame;

public class Lwjgl3Launcher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Chess Game");
        config.setWindowedMode(800, 600);
        new Lwjgl3Application(new MyGdxGame(), config);
    }
}
