package com.intreturn.noclasstoday;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessScreen {
    private ShapeRenderer shape;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    // 棋子图片纹理: key 为棋盘字母 (大写=黑方, 小写=白方)
    private Map<String, Texture> pieceTextures = new HashMap<>();
    // UI 提示纹理: 将军横幅 / 胜负结果 / 终局原因
    private Map<String, Texture> uiTextures = new HashMap<>();

    private float viewportW = 800;
    private float viewportH = 800;
    private float cellSize;
    private float offsetX;
    private float offsetY;
    private boolean isFullscreen = false;

    // 规则引擎 (走法合法性/吃子/将军/终局判定)
    private final ChessGame game = new ChessGame();
    // 选中状态
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<int[]> legalMoves = new ArrayList<>();
    // 升变待选: 兵已待命于目标格, 等待按键选择
    private int promoFromRow = -1, promoFromCol = -1;
    private int promoToRow = -1, promoToCol = -1;

    // 临时验证用:第 90 帧导出 framebuffer 截图
    private int frameCount = 0;

    public ChessScreen() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportW, viewportH);
        loadPieceTextures();
        loadUiTextures();
        updateLayout();
        updateWindowTitle();
    }

    private void loadPieceTextures() {
        // 外部图片: assets/pieces/ 下按颜色前缀命名 b_/w_
        for (char c : "RNBQKP".toCharArray()) {
            pieceTextures.put(String.valueOf(c), new Texture(Gdx.files.internal("pieces/b_" + c + ".png")));
        }
        for (char c : "rnbqkp".toCharArray()) {
            pieceTextures.put(String.valueOf(c), new Texture(Gdx.files.internal("pieces/w_" + c + ".png")));
        }
        for (Texture tex : pieceTextures.values()) {
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    private void loadUiTextures() {
        String[] names = {"check", "white_win", "black_win", "draw",
            "reason_checkmate", "reason_stalemate", "reason_50", "reason_threefold", "reason_material"};
        for (String n : names) {
            Texture t = new Texture(Gdx.files.internal("ui/ui_" + n + ".png"));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            uiTextures.put(n, t);
        }
    }

    private void updateLayout() {
        float boardSide = Math.min(viewportW, viewportH) * 0.9f;
        cellSize = boardSide / 8f;
        offsetX = (viewportW - boardSide) / 2f;
        offsetY = (viewportH - boardSide) / 2f;
    }

    public void resize(int width, int height) {
        // 窗口初始化/最小化时 GLFW 可能回调 0 尺寸,会导致 cellSize 为 0 布局错误
        if (width == 0 || height == 0) return;
        viewportW = width;
        viewportH = height;
        camera.setToOrtho(false, width, height);
        updateLayout();
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            Gdx.graphics.setWindowedMode(800, 800);
            isFullscreen = false;
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            isFullscreen = true;
        }
    }

    /** 窗口标题: 执棋方 / 将军 / 升变选择 / 对局结果. */
    private void updateWindowTitle() {
        if (promoFromRow != -1) {
            Gdx.graphics.setTitle("升变选择: 1=后 2=车 3=象 4=马");
            return;
        }
        if (game.getResult() != null) {
            String winner;
            if ("1-0".equals(game.getResult())) winner = "白方胜";
            else if ("0-1".equals(game.getResult())) winner = "黑方胜";
            else winner = "和棋";
            Gdx.graphics.setTitle("对局结束 - " + winner + " (" + game.getResultReason() + ")");
            return;
        }
        String title = "Chess Game - " + (game.isBlackToMove() ? "黑方回合" : "白方回合");
        if (game.isInCheck(game.isBlackToMove())) title += " (将军!)";
        Gdx.graphics.setTitle(title);
    }

    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            toggleFullscreen();
        }
        ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        drawChessBoard();
        drawSelectedHighlight();
        drawLegalMoveHighlights();
        drawAllPieces();
        drawStatusOverlay();
        handleMouseInput();

        // 临时验证用:第 90 帧导出 framebuffer 截图
        frameCount++;
        if (frameCount == 90) {
            Pixmap pm = Pixmap.createFromFrameBuffer(0, 0,
                Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(Gdx.files.absolute("d:/LibGDX/render_check.png"), pm);
            pm.dispose();
        }
    }

    private void drawChessBoard() {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isLight = (row + col) % 2 == 0;
                shape.setColor(isLight ? Color.valueOf("#D0D0D0") : Color.valueOf("#303030"));
                shape.rect(offsetX + col * cellSize, offsetY + row * cellSize, cellSize, cellSize);
            }
        }
        shape.end();
    }

    private void drawSelectedHighlight() {
        if (selectedRow != -1 && selectedCol != -1) {
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.YELLOW);
            shape.rect(offsetX + selectedCol * cellSize, offsetY + selectedRow * cellSize, cellSize, cellSize);
            shape.end();
        }
    }

    /** 合法走法提示: 空格画绿色小圆点, 吃子格画绿色大圆. */
    private void drawLegalMoveHighlights() {
        if (selectedRow == -1 || legalMoves.isEmpty()) return;
        // ShapeRenderer 会重置混合状态, 半透明高亮需要手动启用
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0.7f, 0f, 0.5f);
        for (int[] m : legalMoves) {
            int tr = m[2], tc = m[3];
            float cx = offsetX + tc * cellSize + cellSize / 2f;
            float cy = offsetY + tr * cellSize + cellSize / 2f;
            boolean capture = game.getPiece(tr, tc) != null || m[4] == 2;
            shape.circle(cx, cy, capture ? cellSize * 0.4f : cellSize * 0.15f);
        }
        shape.end();
    }

    /**
     * 画面提示: 将军时显示红底横幅; 对局结束时半透明遮罩 + 结果大字 + 原因小字.
     * (王无法移动时引擎已判定: 被将军→将死输棋, 未将军→困毙和棋)
     */
    private void drawStatusOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float boardSide = Math.min(viewportW, viewportH) * 0.9f;

        if (game.getResult() == null) {
            // 将军提示横幅 (棋盘上方)
            if (!game.isInCheck(game.isBlackToMove())) return;
            Texture t = uiTextures.get("check");
            if (t == null) return;
            float w = boardSide * 0.45f;
            float h = w * t.getHeight() / t.getWidth();
            batch.begin();
            batch.draw(t, (viewportW - w) / 2f, viewportH / 2f + boardSide * 0.32f, w, h);
            batch.end();
        } else {
            // 半透明遮罩
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(0f, 0f, 0f, 0.5f);
            shape.rect(0, 0, viewportW, viewportH);
            shape.end();
            // 结果大字
            String mainKey;
            String r = game.getResult();
            if ("1-0".equals(r)) mainKey = "white_win";
            else if ("0-1".equals(r)) mainKey = "black_win";
            else mainKey = "draw";
            Texture main = uiTextures.get(mainKey);
            if (main == null) return;
            float mw = boardSide * 0.6f;
            float mh = mw * main.getHeight() / main.getWidth();
            batch.begin();
            batch.draw(main, (viewportW - mw) / 2f, viewportH / 2f + mh * 0.15f, mw, mh);
            // 终局原因小字
            Texture reason = uiTextures.get(mapReason(game.getResultReason()));
            if (reason != null) {
                float rw = boardSide * 0.38f;
                float rh = rw * reason.getHeight() / reason.getWidth();
                batch.draw(reason, (viewportW - rw) / 2f, viewportH / 2f - mh * 0.72f, rw, rh);
            }
            batch.end();
        }
    }

    private String mapReason(String reason) {
        if ("将死".equals(reason)) return "reason_checkmate";
        if ("困毙".equals(reason)) return "reason_stalemate";
        if ("50回合规则".equals(reason)) return "reason_50";
        if ("三次重复局面".equals(reason)) return "reason_threefold";
        if ("子力不足".equals(reason)) return "reason_material";
        return null;
    }

    private void drawAllPieces() {
        // 修复 ShapeRenderer 对 OpenGL 混合状态的干扰
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = game.getPiece(row, col);
                // 升变待选: 兵临时显示在目标格
                if (promoFromRow != -1 && row == promoToRow && col == promoToCol) {
                    piece = game.getPiece(promoFromRow, promoFromCol);
                } else if (promoFromRow != -1 && row == promoFromRow && col == promoFromCol) {
                    piece = null;
                }
                if (piece != null) {
                    Texture tex = pieceTextures.get(piece);
                    if (tex != null) {
                        batch.draw(tex, offsetX + col * cellSize, offsetY + row * cellSize, cellSize, cellSize);
                    }
                }
            }
        }
        batch.end();
    }

    private void handleMouseInput() {
        handlePromotionInput();
        if (promoFromRow != -1) return; // 升变选择期间不处理鼠标
        if (game.getResult() != null) return; // 对局已结束

        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;
        int col = (int)((Gdx.input.getX() - offsetX) / cellSize);
        // 摄像头Y轴向上，需要翻转屏幕坐标 (getY()原点在屏幕顶部)
        int worldY = (int)(viewportH - Gdx.input.getY());
        int row = (int)((worldY - offsetY) / cellSize);

        if (col < 0 || row < 0 || col >= 8 || row >= 8) { clearSelection(); return; }

        if (selectedRow == -1) {
            // 选中: 只能选当前执棋方的棋子
            String piece = game.getPiece(row, col);
            if (piece != null && ChessGame.isBlackPiece(piece) == game.isBlackToMove()) {
                selectPiece(row, col);
            }
        } else {
            int[] matched = null;
            for (int[] m : legalMoves) {
                if (m[2] == row && m[3] == col) { matched = m; break; }
            }
            if (matched != null) {
                char flag = (char) matched[4];
                if (flag == 'q' || flag == 'r' || flag == 'b' || flag == 'n'
                    || flag == 'Q' || flag == 'R' || flag == 'B' || flag == 'N') {
                    // 升变: 记录待选, 兵临时移到目标格, 等待按键
                    promoFromRow = selectedRow;
                    promoFromCol = selectedCol;
                    promoToRow = row;
                    promoToCol = col;
                    clearSelection();
                } else {
                    game.makeMove(selectedRow, selectedCol, row, col, (char) 0);
                    clearSelection();
                }
                updateWindowTitle();
            } else {
                String piece = game.getPiece(row, col);
                if (piece != null && ChessGame.isBlackPiece(piece) == game.isBlackToMove()) {
                    // 换选己方另一棋子
                    selectPiece(row, col);
                } else {
                    clearSelection();
                }
            }
        }
    }

    /** 升变选择: 数字键 1=后 2=车 3=象 4=马. */
    private void handlePromotionInput() {
        if (promoFromRow == -1) return;
        char promo = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) promo = 'q';
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) promo = 'r';
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) promo = 'b';
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) promo = 'n';
        if (promo == 0) return;

        if (ChessGame.isBlackPiece(game.getPiece(promoFromRow, promoFromCol))) {
            promo = Character.toUpperCase(promo);
        }
        // 校验所选升变走法合法
        boolean ok = false;
        for (int[] m : game.getLegalMoves(promoFromRow, promoFromCol)) {
            if (m[2] == promoToRow && m[3] == promoToCol && m[4] == promo) { ok = true; break; }
        }
        if (ok) {
            game.makeMove(promoFromRow, promoFromCol, promoToRow, promoToCol, promo);
            promoFromRow = -1;
            promoToRow = -1;
            updateWindowTitle();
        }
    }

    private void selectPiece(int row, int col) {
        selectedRow = row;
        selectedCol = col;
        legalMoves = game.getLegalMoves(row, col);
    }

    private void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        legalMoves.clear();
    }

    public void dispose() {
        shape.dispose();
        batch.dispose();
        for (Texture tex : pieceTextures.values()) {
            tex.dispose();
        }
        for (Texture tex : uiTextures.values()) {
            tex.dispose();
        }
    }
}
