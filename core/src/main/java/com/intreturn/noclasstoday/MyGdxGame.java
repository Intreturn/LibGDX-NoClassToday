package com.intreturn.noclasstoday;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

public class MyGdxGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;

    private final int BOARD_SIZE = 8;
    private final int TILE_SIZE = 80;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1);

        // 绘制棋盘
        drawChessBoard();
    }

    private void drawChessBoard() {
        int startX = (Gdx.graphics.getWidth() - BOARD_SIZE * TILE_SIZE) / 2;
        int startY = (Gdx.graphics.getHeight() - BOARD_SIZE * TILE_SIZE) / 2;

        shape.begin(ShapeRenderer.ShapeType.Filled);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boolean isLight = (row + col) % 2 == 0;
                if (isLight) {
                    shape.setColor(0.9f, 0.85f, 0.75f, 1);
                } else {
                    shape.setColor(0.35f, 0.25f, 0.15f, 1);
                }

                float x = startX + col * TILE_SIZE;
                float y = startY + row * TILE_SIZE;
                shape.rect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }

        shape.end();

        // 绘制文字
        batch.begin();
        font.draw(batch, "国际象棋棋盘", 10, Gdx.graphics.getHeight() - 10);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        font.dispose();
    }
}
