package com.intreturn.noclasstoday;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
    // 格子坐标小字 (如 C4), ASCII 字符可直接用默认 BitmapFont
    private BitmapFont coordFont;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private float viewportW = 800;
    private float viewportH = 800;
    private float cellSize;
    private float offsetX;
    private float offsetY;
    // 布局常量: 左侧走法记录面板宽 / 底部按钮区高 / 面板与棋盘间距
    private final float panelW = 200f;
    private final float btnH = 72f;
    private final float gap = 12f;
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
    // 选边状态: 开局选择执白/执黑 (执黑时棋盘上下翻转)
    private boolean sideSelected = false;
    private boolean humanIsBlack = false;
    // 底部按钮矩形 (逻辑坐标, 每次绘制时更新)
    private final float[] undoBtnRect = new float[4];
    private final float[] restartBtnRect = new float[4];
    // 时限选择 (选边画面): 0=古典90+30 1=快棋15+10(默认) 2=超快3+2 3=超快5+0
    private int selectedTC = 1;
    private static final int[][] TIME_CONTROLS = {{5400, 30}, {900, 10}, {180, 2}, {300, 0}};
    private static final String[] TC_KEYS = {"tc_classical", "tc_rapid", "tc_blitz32", "tc_blitz50"};
    private final float[][] tcBtnRect = new float[4][4];

    public ChessScreen() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportW, viewportH);
        loadPieceTextures();
        loadUiTextures();
        coordFont = new BitmapFont();
        coordFont.getData().setScale(1.2f);
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
        String[] names = {"white_win", "black_win", "draw", "pick_title", "pick_white", "pick_black",
            "reason_checkmate", "reason_stalemate", "reason_50", "reason_threefold", "reason_material",
            "reason_timeout", "reason_timeout_draw",
            "tc_classical", "tc_rapid", "tc_blitz32", "tc_blitz50",
            "undo", "restart", "arrow",
            "name_k", "name_q", "name_r", "name_b", "name_n", "name_p"};
        for (String n : names) {
            Texture t = new Texture(Gdx.files.internal("ui/ui_" + n + ".png"));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            uiTextures.put(n, t);
        }
    }

    private void updateLayout() {
        // 棋盘避开左侧记录面板与底部按钮区
        float availW = viewportW - panelW - gap * 2;
        float availH = viewportH - btnH - gap * 2;
        float boardSide = Math.min(availW, availH) * 0.95f;
        cellSize = boardSide / 8f;
        offsetX = panelW + gap + (availW - boardSide) / 2f;
        offsetY = btnH + gap + (availH - boardSide) / 2f;
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

    /** 棋盘行 -> 显示行 (执黑时棋盘上下翻转). 翻转函数自逆. */
    private int displayRow(int boardRow) {
        return humanIsBlack ? 7 - boardRow : boardRow;
    }

    /** 窗口标题: 选边 / 执棋方 / 将军 / 升变选择 / 对局结果. */
    private void updateWindowTitle() {
        if (!sideSelected) {
            Gdx.graphics.setTitle("Chess Game - 选择执子方 (点击按钮开始)");
            return;
        }
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

        if (!sideSelected) {
            // 开局选边画面
            drawSidePicker();
            handleSidePickerInput();
            return;
        }

        // 棋钟推进: 对局进行中仅当前行棋方扣时, 超时触发的终局在此检测
        if (game.getResult() == null) {
            game.updateClock(delta);
            if (game.getResult() != null) updateWindowTitle();
        }

        drawChessBoard();
        drawSelectedHighlight();
        drawLegalMoveHighlights();
        drawAllPieces();
        drawBoardLabels();
        drawClockDisplay();
        drawStatusOverlay();
        drawMoveLogPanel();
        drawBottomButtons();
        handleBottomButtonInput();
        handleKeyboardShortcuts();
        handleMouseInput();
    }

    // ---------------- 选边画面 ----------------

    /** 选边画面: 标题 + 时限选择行 + 执白/执黑两个按钮. */
    private void drawSidePicker() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float side = Math.min(viewportW, viewportH);
        batch.begin();
        Texture title = uiTextures.get("pick_title");
        if (title != null) {
            float tw = side * 0.5f;
            float th = tw * title.getHeight() / title.getWidth();
            batch.draw(title, (viewportW - tw) / 2f, viewportH * 0.72f, tw, th);
        }
        drawTcButtons(side);
        drawPickButton("pick_white", viewportH * 0.30f, side * 0.32f);
        drawPickButton("pick_black", viewportH * 0.12f, side * 0.32f);
        batch.end();
        drawTcSelectionFrame();
    }

    /** 时限选择按钮 (横排 4 个): 古典/快棋/超快3+2/超快5+0. */
    private void drawTcButtons(float side) {
        float bw = side * 0.13f;
        float gapX = side * 0.02f;
        float totalW = bw * 4 + gapX * 3;
        float x0 = (viewportW - totalW) / 2f;
        float centerY = viewportH * 0.56f;
        for (int i = 0; i < 4; i++) {
            Texture t = uiTextures.get(TC_KEYS[i]);
            if (t == null) continue;
            float h = bw * t.getHeight() / t.getWidth();
            float x = x0 + i * (bw + gapX);
            float y = centerY - h / 2f;
            batch.draw(t, x, y, bw, h);
            tcBtnRect[i][0] = x;
            tcBtnRect[i][1] = y;
            tcBtnRect[i][2] = bw;
            tcBtnRect[i][3] = h;
        }
    }

    /** 选中时限按钮黄色描边. */
    private void drawTcSelectionFrame() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shape.setColor(1f, 0.84f, 0.25f, 1f);
        shape.rect(tcBtnRect[selectedTC][0], tcBtnRect[selectedTC][1], tcBtnRect[selectedTC][2], tcBtnRect[selectedTC][3]);
        shape.end();
    }

    private void drawPickButton(String key, float centerY, float width) {
        Texture t = uiTextures.get(key);
        if (t == null) return;
        float h = width * t.getHeight() / t.getWidth();
        batch.draw(t, (viewportW - width) / 2f, centerY - h / 2f, width, h);
    }

    private void handleSidePickerInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;
        float mx = Gdx.input.getX();
        float my = viewportH - Gdx.input.getY();
        float side = Math.min(viewportW, viewportH);
        // 时限选择 (点击切换并应用到引擎)
        for (int i = 0; i < 4; i++) {
            if (inRect(mx, my, tcBtnRect[i])) {
                if (selectedTC != i) {
                    selectedTC = i;
                    game.setTimeControl(TIME_CONTROLS[i][0], TIME_CONTROLS[i][1]);
                }
                return;
            }
        }
        if (clickInButton(mx, my, viewportH * 0.30f, side * 0.32f, "pick_white")) {
            humanIsBlack = false;
            sideSelected = true;
            updateWindowTitle();
        } else if (clickInButton(mx, my, viewportH * 0.12f, side * 0.32f, "pick_black")) {
            humanIsBlack = true;
            sideSelected = true;
            updateWindowTitle();
        }
    }

    /** 判断 (mx,my) 是否落在按钮矩形内 (按钮中心 centerY, 宽 width). */
    private boolean clickInButton(float mx, float my, float centerY, float width, String key) {
        Texture t = uiTextures.get(key);
        if (t == null) return false;
        float h = width * t.getHeight() / t.getWidth();
        float x = (viewportW - width) / 2f;
        float y = centerY - h / 2f;
        return mx >= x && mx <= x + width && my >= y && my <= y + h;
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

    /** 将军时王所在格红色高亮; 选中棋子双层光晕. */
    private void drawSelectedHighlight() {
        // 将军提示: 王底下的格子直接变红
        if (game.getResult() == null && game.isInCheck(game.isBlackToMove())) {
            int[] king = game.getKingPosition(game.isBlackToMove());
            if (king != null) {
                int dr = displayRow(king[0]);
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(0.85f, 0.08f, 0.08f, 0.85f);
                shape.rect(offsetX + king[1] * cellSize, offsetY + dr * cellSize, cellSize, cellSize);
                shape.end();
            }
        }
        // 选中棋子: 黄色双层光晕 (棋子本身在 drawAllPieces 中放大加重)
        if (selectedRow != -1 && selectedCol != -1) {
            int dr = displayRow(selectedRow);
            float cx = offsetX + selectedCol * cellSize + cellSize / 2f;
            float cy = offsetY + dr * cellSize + cellSize / 2f;
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(1f, 0.84f, 0.25f, 0.18f);
            shape.circle(cx, cy, cellSize * 0.62f);
            shape.setColor(1f, 0.84f, 0.25f, 0.4f);
            shape.circle(cx, cy, cellSize * 0.46f);
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
            int dr = displayRow(tr);
            float cx = offsetX + tc * cellSize + cellSize / 2f;
            float cy = offsetY + dr * cellSize + cellSize / 2f;
            boolean capture = game.getPiece(tr, tc) != null || m[4] == 2;
            shape.circle(cx, cy, capture ? cellSize * 0.4f : cellSize * 0.15f);
        }
        shape.end();
    }

    /** 每个格子右上角绘制坐标小字 (如 C4): 列 a-h, 行 1-8 (row 0 = 白方底线). 统一淡黄色, 清晰不扎眼. */
    private void drawBoardLabels() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int br = displayRow(row);  // 显示行 -> 棋盘行 (决定坐标数字)
                String label = String.valueOf((char) ('A' + col)) + (br + 1);
                glyphLayout.setText(coordFont, label);
                float pad = cellSize * 0.05f;
                float x = offsetX + (col + 1) * cellSize - glyphLayout.width - pad;
                float y = offsetY + (row + 1) * cellSize - pad;
                coordFont.setColor(1f, 0.9f, 0.55f, 0.7f);
                coordFont.draw(batch, glyphLayout, x, y);
            }
        }
        batch.end();
    }

    /**
     * 画面提示: 对局结束时半透明遮罩 + 结果大字 + 原因小字.
     * (将军提示已改为王格红色高亮, 见 drawSelectedHighlight)
     */
    private void drawStatusOverlay() {
        if (game.getResult() == null) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float boardSide = Math.min(viewportW, viewportH) * 0.9f;

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

    private String mapReason(String reason) {
        if ("将死".equals(reason)) return "reason_checkmate";
        if ("困毙".equals(reason)) return "reason_stalemate";
        if ("50回合规则".equals(reason)) return "reason_50";
        if ("三次重复局面".equals(reason)) return "reason_threefold";
        if ("子力不足".equals(reason)) return "reason_material";
        if ("超时".equals(reason)) return "reason_timeout";
        if ("超时判和".equals(reason)) return "reason_timeout_draw";
        return null;
    }

    // ---------------- 棋钟显示 ----------------

    /** 双方棋钟: 黑方在棋盘上方居中, 白方在底部按钮区左侧; 当前行棋方亮色. */
    private void drawClockDisplay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float boardSide = cellSize * 8f;
        batch.begin();
        // 黑方: 棋盘上方居中 (底边离棋盘顶 13px, 不重叠)
        drawClockRow("K", game.isBlackToMove(), game.getRemainBlack(),
            offsetX + boardSide / 2f, offsetY + boardSide + 13f, true);
        // 白方: 底部按钮区左侧
        drawClockRow("k", !game.isBlackToMove(), game.getRemainWhite(),
            panelW + gap + 6f, btnH / 2f, false);
        batch.end();
    }

    /** 单行棋钟: 棋子符号 + 时间文字 (mm:ss, 超1小时 h:mm:ss); centered 时以 x 为中心. */
    private void drawClockRow(String pieceKey, boolean active, double remain, float x, float yCenter, boolean centered) {
        Texture sym = pieceTextures.get(pieceKey);
        String t = formatTime(remain);
        glyphLayout.setText(coordFont, t);
        float sh = 22f;
        float startX = centered ? x - (sh + 6f + glyphLayout.width) / 2f : x;
        if (sym != null) batch.draw(sym, startX, yCenter - sh / 2f, sh, sh);
        float textX = startX + sh + 6f;
        if (active) coordFont.setColor(1f, 1f, 1f, 1f);
        else coordFont.setColor(0.6f, 0.6f, 0.6f, 1f);
        coordFont.draw(batch, glyphLayout, textX, yCenter + glyphLayout.height / 2f);
    }

    /** 剩余时间格式化: mm:ss; 超过 1 小时 h:mm:ss. 秒向上取整 (棋钟惯例). */
    private static String formatTime(double remain) {
        int s = (int) Math.ceil(remain);
        int h = s / 3600, m = (s % 3600) / 60, ss = s % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, ss);
        return String.format("%d:%02d", m, ss);
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
                        int dr = displayRow(row);
                        // 选中的棋子放大加重渲染
                        if (row == selectedRow && col == selectedCol) {
                            float s = cellSize * 1.14f;
                            batch.draw(tex, offsetX + col * cellSize - (s - cellSize) / 2f,
                                offsetY + dr * cellSize - (s - cellSize) / 2f, s, s);
                        } else {
                            batch.draw(tex, offsetX + col * cellSize, offsetY + dr * cellSize, cellSize, cellSize);
                        }
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
        float mx = Gdx.input.getX();
        float my = viewportH - Gdx.input.getY();
        // 点击落在底部按钮上时不作为棋盘点击
        if (inRect(mx, my, undoBtnRect) || inRect(mx, my, restartBtnRect)) return;
        int col = (int)((Gdx.input.getX() - offsetX) / cellSize);
        // 摄像头Y轴向上，需要翻转屏幕坐标 (getY()原点在屏幕顶部)
        int worldY = (int)(viewportH - Gdx.input.getY());
        int row = displayRow((int)((worldY - offsetY) / cellSize));

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
                    logMove();
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
            logMove();
            updateWindowTitle();
        }
    }

    /** 快捷键: U=悔棋 (与底部按钮等效). */
    private void handleKeyboardShortcuts() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            if (promoFromRow != -1) return; // 升变选择中不悔棋
            if (game.undo()) {
                clearSelection();
                updateWindowTitle();
            }
        }
    }

    /** 每步走子追加写入对局记录文件 (SAN 记谱). */
    private void logMove() {
        List<String> sans = game.getSanHistory();
        if (sans.isEmpty()) return;
        try {
            Gdx.files.absolute("d:/LibGDX/chess_game_log.txt")
                .writeString(sans.get(sans.size() - 1) + "\n", true, "UTF-8");
        } catch (Exception ignored) {
            // 写文件失败不影响对局
        }
    }

    /** 左侧走法记录面板: 常显, 每行 = 步号 + 棋子名 + 符号 + E1→E2, 最新在底部. */
    private void drawMoveLogPanel() {
        List<int[]> moves = game.getMoveHistory();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float px = gap;
        float pw = panelW - gap;
        float py = btnH + gap;
        float ph = viewportH - py - gap;
        // 面板底色
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.55f);
        shape.rect(px, py, pw, ph);
        shape.end();
        if (moves.isEmpty()) return;

        float lineH = 30f;
        int maxLines = (int)((ph - 10f) / lineH);
        int start = Math.max(0, moves.size() - maxLines);

        batch.begin();
        for (int i = moves.size() - 1; i >= start; i--) {
            int[] m = moves.get(i);
            // 最新在底部, 倒序从底向上排
            float yCenter = py + 8f + (moves.size() - 1 - i) * lineH + lineH / 2f;
            drawMoveLogRow(i, m, px + 8f, yCenter);
        }
        batch.end();
    }

    /** 记录面板单行: 步号 + 名字 + 符号 + 起点→终点. 白方行为亮色, 黑方行为暗色. */
    private void drawMoveLogRow(int index, int[] m, float x, float yCenter) {
        boolean black = ChessGame.isBlackPiece(String.valueOf((char) m[4]));
        // 步号
        String num = (index + 1) + ".";
        coordFont.setColor(black ? 0.72f : 1f, 0.92f, 0.7f, 1f);
        glyphLayout.setText(coordFont, num);
        coordFont.draw(batch, glyphLayout, x, yCenter + glyphLayout.height / 2f);
        x += 22f;
        // 棋子名小图 (升变用升变后棋子)
        char shown = (m[5] != 0) ? (char) m[5] : (char) m[4];
        String nameKey = "name_" + Character.toLowerCase(shown);
        Texture nameTex = uiTextures.get(nameKey);
        if (nameTex != null) {
            float nh = 15f;
            float nw = nh * nameTex.getWidth() / nameTex.getHeight();
            batch.draw(nameTex, x, yCenter - nh / 2f, nw, nh);
            x += nw + 3f;
        }
        // 棋子符号图
        Texture pieceTex = pieceTextures.get(String.valueOf(shown));
        if (pieceTex != null) {
            float sh = 24f;
            batch.draw(pieceTex, x, yCenter - sh / 2f, sh, sh);
            x += sh + 4f;
        }
        // 起点坐标 (白方视角: col=a-h, row+1=1-8)
        String fromLabel = "" + (char) ('A' + m[1]) + (m[0] + 1);
        glyphLayout.setText(coordFont, fromLabel);
        coordFont.draw(batch, glyphLayout, x, yCenter + glyphLayout.height / 2f);
        x += glyphLayout.width + 3f;
        // 箭头图
        Texture arrow = uiTextures.get("arrow");
        if (arrow != null) {
            float ah = 13f;
            float aw = ah * arrow.getWidth() / arrow.getHeight();
            batch.draw(arrow, x, yCenter - ah / 2f, aw, ah);
            x += aw + 3f;
        }
        // 终点坐标
        String toLabel = "" + (char) ('A' + m[3]) + (m[2] + 1);
        glyphLayout.setText(coordFont, toLabel);
        coordFont.draw(batch, glyphLayout, x, yCenter + glyphLayout.height / 2f);
    }

    // ---------------- 底部操作按钮 ----------------

    /** 棋盘下侧按钮: 悔棋 + 重新开始, 居中于棋盘区域. */
    private void drawBottomButtons() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Texture undoT = uiTextures.get("undo");
        Texture restT = uiTextures.get("restart");
        if (undoT == null || restT == null) return;
        float bh = btnH * 0.55f;
        float bw1 = bh * undoT.getWidth() / undoT.getHeight();
        float bw2 = bh * restT.getWidth() / restT.getHeight();
        float totalW = bw1 + 20f + bw2;
        float x0 = panelW + gap + (viewportW - panelW - gap - totalW) / 2f;
        float y0 = (btnH - bh) / 2f;
        batch.begin();
        batch.draw(undoT, x0, y0, bw1, bh);
        batch.draw(restT, x0 + bw1 + 20f, y0, bw2, bh);
        batch.end();
        undoBtnRect[0] = x0;
        undoBtnRect[1] = y0;
        undoBtnRect[2] = bw1;
        undoBtnRect[3] = bh;
        restartBtnRect[0] = x0 + bw1 + 20f;
        restartBtnRect[1] = y0;
        restartBtnRect[2] = bw2;
        restartBtnRect[3] = bh;
    }

    /** 按钮点击: 悔棋 (升变选择中禁用) / 重新开始. */
    private void handleBottomButtonInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;
        float mx = Gdx.input.getX();
        float my = viewportH - Gdx.input.getY();
        if (inRect(mx, my, undoBtnRect)) {
            if (promoFromRow != -1) return; // 升变选择中不悔棋
            if (game.undo()) {
                clearSelection();
                updateWindowTitle();
            }
        } else if (inRect(mx, my, restartBtnRect)) {
            restartGame();
        }
    }

    /** 重新开始: 重置引擎与界面状态 (执子方保持不变). */
    private void restartGame() {
        game.reset();
        clearSelection();
        promoFromRow = -1;
        promoFromCol = -1;
        promoToRow = -1;
        promoToCol = -1;
        updateWindowTitle();
    }

    private boolean inRect(float mx, float my, float[] r) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
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
        coordFont.dispose();
        for (Texture tex : pieceTextures.values()) {
            tex.dispose();
        }
        for (Texture tex : uiTextures.values()) {
            tex.dispose();
        }
    }
}
