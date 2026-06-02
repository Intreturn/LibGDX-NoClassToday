package com.intreturn.noclasstoday;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

public class ChessScreen {
    private ShapeRenderer shape;
    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;

    private final int CELL_SIZE = 80;
    private final int BOARD_OFFSET = 100;
    private String[][] board;
    private int selectedRow = -1;
    private int selectedCol = -1;

    public ChessScreen() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(3.0f);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 800);
        createBoard();
    }

    private void createBoard() {
        board = new String[8][8];
        board[0][0] = "♜"; board[0][1] = "♞"; board[0][2] = "♝"; board[0][3] = "♛";
        board[0][4] = "♚"; board[0][5] = "♝"; board[0][6] = "♞"; board[0][7] = "♜";
        for (int i = 0; i < 8; i++) board[1][i] = "♟";
        for (int i = 0; i < 8; i++) board[6][i] = "♙";
        board[7][0] = "♖"; board[7][1] = "♘"; board[7][2] = "♗"; board[7][3] = "♕";
        board[7][4] = "♔"; board[7][5] = "♗"; board[7][6] = "♘"; board[7][7] = "♖";
    }

    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        drawChessBoard();
        drawSelectedHighlight();
        drawAllPieces();
        handleMouseInput();
    }

    private void drawChessBoard() {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isLight = (row + col) % 2 == 0;
                shape.setColor(isLight ? Color.valueOf("#f0d9b5") : Color.valueOf("#b58863"));
                shape.rect(BOARD_OFFSET + col * CELL_SIZE, BOARD_OFFSET + row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        shape.end();
    }

    private void drawSelectedHighlight() {
        if (selectedRow != -1 && selectedCol != -1) {
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.YELLOW);
            shape.rect(BOARD_OFFSET + selectedCol * CELL_SIZE, BOARD_OFFSET + selectedRow * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            shape.end();
        }
    }

    private void drawAllPieces() {
        batch.begin();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = board[row][col];
                if (piece != null) {
                    font.setColor(Color.WHITE);
                    font.draw(batch, piece, BOARD_OFFSET + col * CELL_SIZE + 20, BOARD_OFFSET + row * CELL_SIZE + 55);
                }
            }
        }
        batch.end();
    }

    private void handleMouseInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;
        int col = (Gdx.input.getX() - BOARD_OFFSET) / CELL_SIZE;
        int row = (Gdx.input.getY() - BOARD_OFFSET) / CELL_SIZE;

        if (col < 0 || row < 0 || col >= 8 || row >= 8) { selectedRow = -1; selectedCol = -1; return; }

        if (selectedRow == -1) {
            if (board[row][col] != null) { selectedRow = row; selectedCol = col; }
        } else {
            int dr = Math.abs(row - selectedRow);
            int dc = Math.abs(col - selectedCol);
            boolean canMove = (dr == 1 && dc == 0) || (dr == 0 && dc == 1);
            if (canMove && board[row][col] == null) {
                board[row][col] = board[selectedRow][selectedCol];
                board[selectedRow][selectedCol] = null;
            }
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    public void dispose() {
        shape.dispose();
        batch.dispose();
        font.dispose();
    }
}
