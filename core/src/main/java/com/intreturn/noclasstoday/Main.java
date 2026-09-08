package com.intreturn.noclasstoday;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private ChessScreen chessScreen;

    @Override
    public void create() {
        chessScreen = new ChessScreen();
    }

    @Override
    public void render() {
        chessScreen.render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void resize(int width, int height) {
        chessScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        chessScreen.dispose();
    }
}
