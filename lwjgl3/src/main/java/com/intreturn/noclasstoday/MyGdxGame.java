package com.intreturn.noclasstoday;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class MyGdxGame extends ApplicationAdapter {
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    private static final int BOARD_SIZE = 8;
    private final Color lightColor = Color.WHITE;
    private final Color darkColor  = Color.BLACK;
    private final Color borderColor = Color.BLACK;

    private float viewportWidth = 800;
    private float viewportHeight = 480;

    private final String[][] pieces = new String[BOARD_SIZE][BOARD_SIZE];
    private int selectedRow = -1;
    private int selectedCol = -1;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        //关闭内置字体映射bug，缩小缩放
        font = new BitmapFont();
        font.getData().setScale(1.9f);
        layout = new GlyphLayout();
        initPieces();
    }

    private void initPieces() {
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                pieces[r][c] = null;
        pieces[0] = new String[]{"r","n","b","q","k","b","n","r"};
        for (int c = 0; c < BOARD_SIZE; c++) pieces[1][c] = "p";
        for (int c = 0; c < BOARD_SIZE; c++) pieces[6][c] = "P";
        pieces[7] = new String[]{"R","N","B","Q","K","B","N","R"};
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        float boardSide = Math.min(viewportWidth, viewportHeight) * 0.9f;
        float cell = boardSide / BOARD_SIZE;
        float startX = (viewportWidth - boardSide) / 2f;
        float startY = (viewportHeight - boardSide) / 2f;

        handleMouseInput(startX, startY, cell, boardSide);
        drawBoard(startX, startY, cell, boardSide);
        drawSelectedHighlight(startX, startY, cell);
        drawPieces(startX, startY, cell);
    }

    private void handleMouseInput(float startX, float startY, float cell, float boardSide) {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;
        int mx = Gdx.input.getX();
        float flipY = viewportHeight - Gdx.input.getY();
        int col = (int) ((mx - startX) / cell);
        int row = (int) ((flipY - startY) / cell);
        if (col < 0 || row < 0 || col >= 8 || row >= 8) {
            selectedRow = -1;
            selectedCol = -1;
            return;
        }
        if (selectedRow == -1) {
            if (pieces[row][col] != null) {
                selectedRow = row;
                selectedCol = col;
            }
        } else {
            int dr = Math.abs(row - selectedRow);
            int dc = Math.abs(col - selectedCol);
            boolean canMove = (dr == 1 && dc == 0) || (dr == 0 && dc == 1);
            if (canMove && pieces[row][col] == null) {
                pieces[row][col] = pieces[selectedRow][selectedCol];
                pieces[selectedRow][selectedCol] = null;
            }
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    private void drawBoard(float startX, float startY, float cell, float boardSide) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                shapeRenderer.setColor(((row + col) % 2 == 0) ? lightColor : darkColor);
                shapeRenderer.rect(startX + col * cell, startY + row * cell, cell, cell);
            }
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(startX, startY, boardSide, boardSide);
        for (int i = 1; i < 8; i++) {
            shapeRenderer.line(startX + i * cell, startY, startX + i * cell, startY + boardSide);
            shapeRenderer.line(startX, startY + i * cell, startX + boardSide, startY + i * cell);
        }
        shapeRenderer.end();
    }

    private void drawSelectedHighlight(float startX, float startY, float cell) {
        if (selectedRow == -1 || selectedCol == -1) return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.YELLOW.r, Color.YELLOW.g, Color.YELLOW.b, 0.4f);
        shapeRenderer.rect(startX + selectedCol * cell, startY + selectedRow * cell, cell, cell);
        shapeRenderer.end();
    }

    //修正垂直偏移，整体往下微调
    private void drawPieces(float startX, float startY, float cell) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String p = pieces[row][col];
                if (p == null) continue;
                boolean lightGrid = (row + col) % 2 == 0;
                font.setColor(lightGrid ? Color.BLACK : Color.WHITE);
                layout.setText(font, p);
                float x = startX + col * cell + (cell - layout.width) / 2f;
                //向下偏移4，解决飞出上边
                float y = startY + row * cell + (cell + layout.height) / 2f - 4f;
                font.draw(batch, layout, x, y);
            }
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
