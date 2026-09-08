package com.intreturn.noclasstoday;

import java.util.ArrayList;
import java.util.List;

/**
 * 国际象棋规则引擎 (纯逻辑, 不依赖 LibGDX, 可独立测试).
 *
 * 坐标约定: row 0 = 白方底线 (屏幕底部), row 7 = 黑方底线 (屏幕顶部); col 0-7 对应 a-h.
 * 棋子内部表示: 大写字母=黑方, 小写字母=白方 (与棋子图片命名一致).
 *
 * 走法编码: int[]{fromRow, fromCol, toRow, toCol, flag}
 *   flag: 0=普通, 1=兵两格, 2=吃过路兵, 3=王车易位, 否则为升变棋子字符 'q'/'r'/'b'/'n' (白) 或大写 (黑).
 */
public class ChessGame {
    private final String[][] board = new String[8][8];
    private boolean blackToMove;                                  // false=白方, true=黑方 (白方先行)
    private boolean castlingWK, castlingWQ, castlingBK, castlingBQ; // 易位权限
    private int enPassantRow = -1, enPassantCol = -1;             // 吃过路兵目标格 (吃子方落点)
    private int halfmoveClock;                                    // 50回合规则计数器 (吃子/兵走子清零)
    private int fullmoveNumber = 1;
    private final List<String> positionHistory = new ArrayList<>(); // 局面历史 (三次重复检测)
    private String result;                                        // null=进行中, "1-0"白胜, "0-1"黑胜, "1/2-1/2"和
    private String resultReason;

    private static final int[][] KNIGHT_OFFSETS = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public ChessGame() {
        reset();
    }

    /** 初始化棋盘: 白方在 row 0/1 (下方), 黑方在 row 6/7 (上方). */
    public void reset() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) board[r][c] = null;
        }
        board[0] = new String[]{"r", "n", "b", "q", "k", "b", "n", "r"};
        board[1] = new String[]{"p", "p", "p", "p", "p", "p", "p", "p"};
        board[6] = new String[]{"P", "P", "P", "P", "P", "P", "P", "P"};
        board[7] = new String[]{"R", "N", "B", "Q", "K", "B", "N", "R"};
        blackToMove = false;
        castlingWK = castlingWQ = castlingBK = castlingBQ = true;
        enPassantRow = -1;
        enPassantCol = -1;
        halfmoveClock = 0;
        fullmoveNumber = 1;
        positionHistory.clear();
        positionHistory.add(toFen());
        result = null;
        resultReason = null;
    }

    // ---------------- 访问器 ----------------

    public String getPiece(int r, int c) {
        return board[r][c];
    }

    public boolean isBlackToMove() {
        return blackToMove;
    }

    public String getResult() {
        return result;
    }

    public String getResultReason() {
        return resultReason;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    public static boolean isBlackPiece(String piece) {
        return piece != null && Character.isUpperCase(piece.charAt(0));
    }

    /**
     * 加载自定义局面 (测试/读档用). pieces 为 8x8 棋子数组 (row 0=白方底线).
     */
    public void loadPosition(String[][] pieces, boolean blackToMove,
                             boolean wk, boolean wq, boolean bk, boolean bq,
                             int epRow, int epCol, int halfmove) {
        for (int r = 0; r < 8; r++) {
            System.arraycopy(pieces[r], 0, board[r], 0, 8);
        }
        this.blackToMove = blackToMove;
        castlingWK = wk;
        castlingWQ = wq;
        castlingBK = bk;
        castlingBQ = bq;
        enPassantRow = epRow;
        enPassantCol = epCol;
        halfmoveClock = halfmove;
        fullmoveNumber = 1;
        positionHistory.clear();
        positionHistory.add(toFen());
        result = null;
        resultReason = null;
    }

    // ---------------- 走法生成 ----------------

    /**
     * 生成 (r,c) 棋子的全部合法走法 (已过滤送王).
     * 仅当该棋子属于当前执棋方时返回非空.
     */
    public List<int[]> getLegalMoves(int r, int c) {
        List<int[]> moves = new ArrayList<>();
        String piece = board[r][c];
        if (piece == null || result != null) return moves;
        if (isBlackPiece(piece) != blackToMove) return moves;

        for (int[] m : pseudoMoves(r, c, piece)) {
            if (simulate(m, piece)) moves.add(m);
        }
        return moves;
    }

    /** 生成伪合法走法 (未过滤送王/易位安全性). */
    private List<int[]> pseudoMoves(int r, int c, String piece) {
        List<int[]> moves = new ArrayList<>();
        boolean black = isBlackPiece(piece);
        char type = Character.toLowerCase(piece.charAt(0));

        switch (type) {
            case 'p':
                addPawnMoves(moves, r, c, black);
                break;
            case 'n':
                for (int[] o : KNIGHT_OFFSETS) {
                    int nr = r + o[0], nc = c + o[1];
                    if (inBoard(nr, nc) && !sameColor(r, c, nr, nc)) moves.add(new int[]{r, c, nr, nc, 0});
                }
                break;
            case 'b':
                addRayMoves(moves, r, c, black, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
                break;
            case 'r':
                addRayMoves(moves, r, c, black, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
                break;
            case 'q':
                addRayMoves(moves, r, c, black, new int[][]{
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
                break;
            case 'k':
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (inBoard(nr, nc) && !sameColor(r, c, nr, nc)) moves.add(new int[]{r, c, nr, nc, 0});
                    }
                }
                addCastlingMoves(moves, r, c, black);
                break;
            default:
                break;
        }
        return moves;
    }

    /** 兵的走法: 前进 (初始两格)、斜吃、吃过路兵、升变. */
    private void addPawnMoves(List<int[]> moves, int r, int c, boolean black) {
        int dir = black ? -1 : 1;      // 黑兵向下 (row 减小), 白兵向上
        int startRow = black ? 6 : 1;  // 初始行
        int promoRow = black ? 0 : 7;  // 升变行

        // 前进一格 (可能升变)
        int nr = r + dir;
        if (inBoard(nr, c) && board[nr][c] == null) {
            if (nr == promoRow) {
                addPromotionMoves(moves, r, c, nr, c, black);
            } else {
                moves.add(new int[]{r, c, nr, c, 0});
                // 初始两格
                if (r == startRow && board[r + 2 * dir][c] == null) {
                    moves.add(new int[]{r, c, r + 2 * dir, c, 1});
                }
            }
        }

        // 斜吃 + 吃过路兵
        for (int dc : new int[]{-1, 1}) {
            int nc = c + dc;
            if (!inBoard(nr, nc)) continue;
            String target = board[nr][nc];
            if (target != null && isBlackPiece(target) != black) {
                if (nr == promoRow) {
                    addPromotionMoves(moves, r, c, nr, nc, black);
                } else {
                    moves.add(new int[]{r, c, nr, nc, 0});
                }
            } else if (target == null && nr == enPassantRow && nc == enPassantCol) {
                moves.add(new int[]{r, c, nr, nc, 2});
            }
        }
    }

    /** 升变走法: 后/车/象/马 四种选择. */
    private void addPromotionMoves(List<int[]> moves, int r, int c, int nr, int nc, boolean black) {
        char[] promos = black ? new char[]{'Q', 'R', 'B', 'N'} : new char[]{'q', 'r', 'b', 'n'};
        for (char p : promos) {
            moves.add(new int[]{r, c, nr, nc, p});
        }
    }

    /** 直线/斜线射线走法, 遇子即停. */
    private void addRayMoves(List<int[]> moves, int r, int c, boolean black, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBoard(nr, nc)) {
                if (board[nr][nc] == null) {
                    moves.add(new int[]{r, c, nr, nc, 0});
                } else {
                    if (isBlackPiece(board[nr][nc]) != black) moves.add(new int[]{r, c, nr, nc, 0});
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
    }

    /** 王车易位伪走法 (格间为空且车在位, 安全性在 simulate 中校验). */
    private void addCastlingMoves(List<int[]> moves, int r, int c, boolean black) {
        int homeRow = black ? 7 : 0;
        if (r != homeRow || c != 4) return;
        if (black) {
            // 黑方短易位 O-O
            if (castlingBK && board[7][5] == null && board[7][6] == null && "R".equals(board[7][7])) {
                moves.add(new int[]{7, 4, 7, 6, 3});
            }
            // 黑方长易位 O-O-O
            if (castlingBQ && board[7][3] == null && board[7][2] == null && board[7][1] == null && "R".equals(board[7][0])) {
                moves.add(new int[]{7, 4, 7, 2, 3});
            }
        } else {
            // 白方短易位 O-O
            if (castlingWK && board[0][5] == null && board[0][6] == null && "r".equals(board[0][7])) {
                moves.add(new int[]{0, 4, 0, 6, 3});
            }
            // 白方长易位 O-O-O
            if (castlingWQ && board[0][3] == null && board[0][2] == null && board[0][1] == null && "r".equals(board[0][0])) {
                moves.add(new int[]{0, 4, 0, 2, 3});
            }
        }
    }

    // ---------------- 合法性校验 ----------------

    /**
     * 模拟执行走法后, 校验己方王不处于被攻击状态 (含易位经过格校验).
     * 通过快照 + 还原实现, 不污染当前局面.
     */
    private boolean simulate(int[] m, String piece) {
        int fr = m[0], fc = m[1], tr = m[2], tc = m[3], flag = m[4];
        boolean black = isBlackPiece(piece);
        String[][] snapshot = new String[8][];
        for (int i = 0; i < 8; i++) snapshot[i] = board[i].clone();

        // 王车易位: 先校验王当前不在将军且经过格/落点安全
        if (flag == 3) {
            if (isInCheck(black)) return false;
            // 经过格 = 王起点与落点之间的所有格子
            int step = tc > fc ? 1 : -1;
            for (int cc = fc + step; cc != tc + step; cc += step) {
                if (isSquareAttacked(tr, cc, !black)) return false;
            }
            // 移动王和车
            board[tr][tc] = piece;
            board[fr][fc] = null;
            if (tc == 6) { // 短易位: 车 h->f
                board[tr][5] = board[tr][7];
                board[tr][7] = null;
            } else {       // 长易位: 车 a->d
                board[tr][3] = board[tr][0];
                board[tr][0] = null;
            }
        } else {
            board[tr][tc] = flag == 'q' || flag == 'r' || flag == 'b' || flag == 'n'
                || flag == 'Q' || flag == 'R' || flag == 'B' || flag == 'N'
                ? String.valueOf((char) flag) : piece;
            board[fr][fc] = null;
            // 吃过路兵: 移除被越过的兵 (白吃黑移除 tr-1, 黑吃白移除 tr+1)
            if (flag == 2) {
                board[black ? tr + 1 : tr - 1][tc] = null;
            }
        }

        boolean kingSafe = !isInCheck(black);

        // 还原
        for (int i = 0; i < 8; i++) board[i] = snapshot[i];
        return kingSafe;
    }

    // ---------------- 将军 / 攻击判定 ----------------

    /** 某方王是否被将军. */
    public boolean isInCheck(boolean black) {
        int[] king = findKing(black);
        if (king == null) return false;
        return isSquareAttacked(king[0], king[1], !black);
    }

    /** 格子 (r,c) 是否被 byBlack 方攻击. */
    public boolean isSquareAttacked(int r, int c, boolean byBlack) {
        // 兵攻击: 黑兵从 (r+1,c±1) 攻击 (r,c); 白兵从 (r-1,c±1) 攻击 (r,c)
        if (byBlack) {
            for (int dc : new int[]{-1, 1}) {
                if (inBoard(r + 1, c + dc) && "P".equals(board[r + 1][c + dc])) return true;
            }
        } else {
            for (int dc : new int[]{-1, 1}) {
                if (inBoard(r - 1, c + dc) && "p".equals(board[r - 1][c + dc])) return true;
            }
        }
        // 马
        for (int[] o : KNIGHT_OFFSETS) {
            int nr = r + o[0], nc = c + o[1];
            if (inBoard(nr, nc)) {
                String p = board[nr][nc];
                if (p != null && Character.toLowerCase(p.charAt(0)) == 'n' && isBlackPiece(p) == byBlack) return true;
            }
        }
        // 王
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBoard(nr, nc)) {
                    String p = board[nr][nc];
                    if (p != null && Character.toLowerCase(p.charAt(0)) == 'k' && isBlackPiece(p) == byBlack) return true;
                }
            }
        }
        // 车/后 直线, 象/后 斜线
        int[][] rookDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[][] bishopDirs = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        if (rayHit(r, c, rookDirs, byBlack, 'r') || rayHit(r, c, bishopDirs, byBlack, 'b')) return true;
        return false;
    }

    /** 沿 dirs 方向查找 byBlack 方的 type 棋子 (type 为 r/b 时也命中后). */
    private boolean rayHit(int r, int c, int[][] dirs, boolean byBlack, char type) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBoard(nr, nc)) {
                String p = board[nr][nc];
                if (p != null) {
                    char t = Character.toLowerCase(p.charAt(0));
                    if (isBlackPiece(p) == byBlack && (t == type || t == 'q')) return true;
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
        return false;
    }

    private int[] findKing(boolean black) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String p = board[r][c];
                if (p != null && Character.toLowerCase(p.charAt(0)) == 'k' && isBlackPiece(p) == black) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    // ---------------- 走子执行 ----------------

    /**
     * 执行走法. 调用方须保证该走法在 getLegalMoves 中.
     * promo 为升变棋子字符 (黑方大写/白方小写), 非升变传 0.
     */
    public void makeMove(int fr, int fc, int tr, int tc, char promo) {
        if (result != null) return;
        String piece = board[fr][fc];
        if (piece == null) return;
        boolean black = isBlackPiece(piece);
        char type = Character.toLowerCase(piece.charAt(0));

        String captured = board[tr][tc];
        boolean isEnPassant = type == 'p' && tc != fc && captured == null;
        boolean isCastling = type == 'k' && Math.abs(tc - fc) == 2;

        // 执行移动
        if (isCastling) {
            board[tr][tc] = piece;
            board[fr][fc] = null;
            if (tc == 6) {          // 短易位: 车 h->f
                board[tr][5] = board[tr][7];
                board[tr][7] = null;
            } else {                // 长易位: 车 a->d
                board[tr][3] = board[tr][0];
                board[tr][0] = null;
            }
        } else {
            board[tr][tc] = promo != 0 ? String.valueOf(promo) : piece;
            board[fr][fc] = null;
            if (isEnPassant) {      // 吃过路兵: 移除被越过的兵
                board[black ? tr + 1 : tr - 1][tc] = null;
            }
        }

        // 更新易位权限
        if (type == 'k') {
            if (black) { castlingBK = false; castlingBQ = false; }
            else { castlingWK = false; castlingWQ = false; }
        }
        castlingWK &= "r".equals(board[0][7]);
        castlingWQ &= "r".equals(board[0][0]);
        castlingBK &= "R".equals(board[7][7]);
        castlingBQ &= "R".equals(board[7][0]);

        // 更新吃过路兵目标: 兵走两格后设置, 否则清空
        if (type == 'p' && Math.abs(tr - fr) == 2) {
            enPassantRow = (fr + tr) / 2;
            enPassantCol = fc;
        } else {
            enPassantRow = -1;
            enPassantCol = -1;
        }

        // 50回合规则计数器: 吃子或兵走子清零
        if (type == 'p' || captured != null || isEnPassant) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        // 切换执棋权
        blackToMove = !blackToMove;
        if (!blackToMove) fullmoveNumber++;   // 白方走完后回合数+1 (黑刚走完切换为白)

        positionHistory.add(toFen());

        // 终局检测
        checkGameEnd(black);
    }

    /** 走子后检测将死/困毙/和棋. winner 为刚走完的一方. */
    private void checkGameEnd(boolean winnerBlack) {
        boolean inCheck = isInCheck(blackToMove);
        if (!hasAnyLegalMove(blackToMove)) {
            if (inCheck) {
                result = winnerBlack ? "0-1" : "1-0";
                resultReason = "将死";
            } else {
                result = "1/2-1/2";
                resultReason = "困毙";
            }
            return;
        }
        if (halfmoveClock >= 100) {
            result = "1/2-1/2";
            resultReason = "50回合规则";
            return;
        }
        if (isThreefoldRepetition()) {
            result = "1/2-1/2";
            resultReason = "三次重复局面";
            return;
        }
        if (isInsufficientMaterial()) {
            result = "1/2-1/2";
            resultReason = "子力不足";
        }
    }

    /** 某方是否有任意合法走法. */
    public boolean hasAnyLegalMove(boolean black) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String p = board[r][c];
                if (p != null && isBlackPiece(p) == black && !getLegalMoves(r, c).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------- 和棋判定 ----------------

    /** 三次重复局面: 相同局面 (含执棋方/易位权限/过路兵权限) 出现 3 次. */
    private boolean isThreefoldRepetition() {
        String fen = toFen();
        int count = 0;
        for (String h : positionHistory) {
            if (h.equals(fen) && ++count >= 3) return true;
        }
        return false;
    }

    /** 子力不足: 王vs王 / 王+单轻子vs王 / 双方单象且同色格. */
    private boolean isInsufficientMaterial() {
        List<String> white = new ArrayList<>();
        List<String> black = new ArrayList<>();
        int[] whiteBishop = null, blackBishop = null;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String p = board[r][c];
                if (p == null || Character.toLowerCase(p.charAt(0)) == 'k') continue;
                if (isBlackPiece(p)) {
                    black.add(p);
                    if (Character.toLowerCase(p.charAt(0)) == 'b') blackBishop = new int[]{r, c};
                } else {
                    white.add(p);
                    if (Character.toLowerCase(p.charAt(0)) == 'b') whiteBishop = new int[]{r, c};
                }
            }
        }
        // 双方都无子: 王 vs 王
        if (white.isEmpty() && black.isEmpty()) return true;
        // 一方无子, 另一方只有单象或单马
        if (white.isEmpty() && black.size() == 1) {
            char t = Character.toLowerCase(black.get(0).charAt(0));
            if (t == 'b' || t == 'n') return true;
        }
        if (black.isEmpty() && white.size() == 1) {
            char t = Character.toLowerCase(white.get(0).charAt(0));
            if (t == 'b' || t == 'n') return true;
        }
        // 双方各只有单象且同色格
        if (white.size() == 1 && black.size() == 1 && whiteBishop != null && blackBishop != null
            && (whiteBishop[0] + whiteBishop[1]) % 2 == (blackBishop[0] + blackBishop[1]) % 2) {
            return true;
        }
        return false;
    }

    // ---------------- FEN 序列化 (三次重复检测 + 存档) ----------------

    /** 标准 FEN 字符串 (白=大写, 黑=小写, 与内部表示相反, 需转换). */
    private String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                String p = board[r][c];
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    char ch = p.charAt(0);
                    sb.append(isBlackPiece(p) ? Character.toLowerCase(ch) : Character.toUpperCase(ch));
                }
            }
            if (empty > 0) sb.append(empty);
            if (r > 0) sb.append('/');
        }
        sb.append(' ').append(blackToMove ? 'b' : 'w').append(' ');
        if (castlingWK) sb.append('K');
        if (castlingWQ) sb.append('Q');
        if (castlingBK) sb.append('k');
        if (castlingBQ) sb.append('q');
        if (!castlingWK && !castlingWQ && !castlingBK && !castlingBQ) sb.append('-');
        sb.append(' ');
        if (enPassantRow >= 0) {
            sb.append((char) ('a' + enPassantCol)).append(enPassantRow + 1);
        } else {
            sb.append('-');
        }
        sb.append(' ').append(halfmoveClock).append(' ').append(fullmoveNumber);
        return sb.toString();
    }

    private boolean inBoard(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    /** (r,c) 与 (nr,nc) 是否有同色棋子. */
    private boolean sameColor(int r, int c, int nr, int nc) {
        String t = board[nr][nc];
        return t != null && isBlackPiece(t) == isBlackPiece(board[r][c]);
    }
}
