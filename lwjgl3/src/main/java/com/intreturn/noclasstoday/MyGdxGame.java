package com.intreturn.noclasstoday;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * 绘制 8x8 国际象棋棋盘（纯白/纯黑）并在初始位置用 Unicode 字符绘制棋子。
 * 兼容 JDK 11，使用默认 BitmapFont（无需额外资源）。如果默认字体缺失棋子符号，
 * 请参考下方的 FreeType 可选方案。
 */
public class MyGdxGame extends ApplicationAdapter {
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    private static final int BOARD_SIZE = 8;
    private final Color lightColor = Color.WHITE; // 浅格 -> 纯白
    private final Color darkColor  = Color.BLACK; // 深格 -> 纯黑
    private final Color borderColor = Color.BLACK;

    private float viewportWidth = 800;
    private float viewportHeight = 480;

    // 棋子数组：row 0 = 底行（白方后排），row 7 = 顶行（黑方后排）
    // 使用用户指定的 Unicode 符号
    private final String[][] pieces = new String[BOARD_SIZE][BOARD_SIZE];

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportWidth, viewportHeight);

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        // 使用系统默认 BitmapFont（在多数系统上能显示常用 Unicode；若显示为空白或方块，请见下方 FreeType 方案）
        font = new BitmapFont();
        layout = new GlyphLayout();

        initPieces();
    }

    private void initPieces() {
        // 清空
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                pieces[r][c] = null;
            }
        }

        // 白方后排（第 1 行 -> row 0）
        pieces[0] = new String[] {"♖", "♘", "♗", "♕", "♔", "♗", "♘", "♖"};
        // 白方兵（第 2 行 -> row 1）
        for (int c = 0; c < BOARD_SIZE; c++) pieces[1][c] = "♙";

        // 黑方兵（第 7 行 -> row 6）
        for (int c = 0; c < BOARD_SIZE; c++) pieces[6][c] = "♟";

        // 黑方后排（第 8 行 -> row 7）
        pieces[7] = new String[] {"♜", "♞", "♝", "♛", "♚", "♝", "♞", "♜"};
    }

    @Override
    public void resize(int width, int height) {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
        camera.setToOrtho(false, viewportWidth, viewportHeight);

        // 根据格子大小自适应字体缩放以增强清晰度（对默认 BitmapFont 做近似缩放）
        float boardSide = Math.min(viewportWidth, viewportHeight) * 0.9f;
        float cell = boardSide / (float) BOARD_SIZE;

        // 若使用默认 BitmapFont，需要通过 setScale 做粗略适配（默认字体像素基准不固定，这里取一个经验值）
        final float approxBaseFontPx = 32f; // 经验值：默认位图字体约为 32px 级别
        float scale = (cell * 0.7f) / approxBaseFontPx;
        if (scale <= 0) scale = 1f;
        font.getData().setScale(scale);
    }

    @Override
    public void render() {
        // 清屏
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // 计算棋盘尺寸与起始坐标（居中并保持方形）
        float boardSide = Math.min(viewportWidth, viewportHeight) * 0.9f;
        float cell = boardSide / (float) BOARD_SIZE;
        float startX = (viewportWidth - boardSide) / 2f;
        float startY = (viewportHeight - boardSide) / 2f;

        // 绘制方格
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boolean isLight = ((row + col) % 2 == 0);
                shapeRenderer.setColor(isLight ? lightColor : darkColor);

                float x = startX + col * cell;
                float y = startY + row * cell;
                shapeRenderer.rect(x, y, cell, cell);
            }
        }
        shapeRenderer.end();

        // 绘制边框和网格线
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(startX, startY, boardSide, boardSide);
        for (int i = 1; i < BOARD_SIZE; i++) {
            float xLine = startX + i * cell;
            shapeRenderer.line(xLine, startY, xLine, startY + boardSide);
            float yLine = startY + i * cell;
            shapeRenderer.line(startX, yLine, startX + boardSide, yLine);
        }
        shapeRenderer.end();

        // 使用 SpriteBatch + BitmapFont 绘制棋子（在格子中心）
        batch.begin();
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = pieces[row][col];
                if (piece == null) continue;

                float x = startX + col * cell;
                float y = startY + row * cell;

                // 根据格子颜色选择字体颜色以保证对比度（在白格上用黑字，在黑格上用白字）
                boolean isLight = ((row + col) % 2 == 0);
                font.setColor(isLight ? Color.BLACK : Color.WHITE);

                // 准备文本布局并居中绘制
                layout.setText(font, piece);
                float textX = x + (cell - layout.width) / 2f;
                // BitmapFont.draw 的 y 是基线，所以我们把基线放在格子中线偏上 layout.height/2 处
                float textY = y + (cell + layout.height) / 2f;
                font.draw(batch, layout, textX, textY);
            }
        }
        batch.end();
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}

