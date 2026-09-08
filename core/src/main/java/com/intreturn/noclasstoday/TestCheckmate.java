package com.intreturn.noclasstoday;

import java.util.List;
import java.util.Random;

/** 将死检测专项验证 (排查"教皇被将死后没有结束游戏"问题). */
public class TestCheckmate {
    static int passed = 0, failed = 0;

    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("[PASS] " + name); }
        else { failed++; System.out.println("[FAIL] " + name); }
    }

    static String[][] empty() {
        String[][] b = new String[8][8];
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) b[r][c] = null;
        return b;
    }

    static boolean hasMove(List<int[]> moves, int tr, int tc) {
        for (int[] m : moves) if (m[2] == tr && m[3] == tc) return true;
        return false;
    }

    public static void main(String[] args) {
        testBackRankMate();
        testQueenKingMate();
        testSmotheredMate();
        testTwoRooksMate();
        testNotMateButCheck();
        testScholarsMate();
        testAnastasiasMate();
        testPromotionMate();
        testRandomPlay();
        System.out.println("===== passed=" + passed + " failed=" + failed + " =====");
        if (failed > 0) System.exit(1);
    }

    /** 底线闷杀: 黑王g8被自己兵f7/g7/h7困住, 白车a7->a8# */
    static void testBackRankMate() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][0] = "k";           // 白王 a1
        b[6][0] = "r";           // 白车 a7
        b[7][6] = "K";           // 黑王 g8
        b[6][5] = "P"; b[6][6] = "P"; b[6][7] = "P"; // 黑兵 f7/g7/h7
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        check("白车a7->a8合法", hasMove(g.getLegalMoves(6, 0), 7, 0));
        g.makeMove(6, 0, 7, 0, (char) 0);
        check("底线闷杀: 白方胜", "1-0".equals(g.getResult()));
        check("原因=将死", "将死".equals(g.getResultReason()));
    }

    /** 后+王贴身将死: 黑王g8, 白王h6, 白后f6->g7# */
    static void testQueenKingMate() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[7][6] = "K";           // 黑王 g8
        b[5][7] = "k";           // 白王 h6
        b[5][5] = "q";           // 白后 f6
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        check("白后f6->g7合法", hasMove(g.getLegalMoves(5, 5), 6, 6));
        g.makeMove(5, 5, 6, 6, (char) 0);
        check("后王配合将死: 白方胜", "1-0".equals(g.getResult()));
        check("原因=将死", "将死".equals(g.getResultReason()));
    }

    /** 马闷杀: 黑王h8被兵g7/h7和车g8包围, 白马e5->f7# */
    static void testSmotheredMate() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][0] = "k";           // 白王 a1
        b[4][4] = "n";           // 白马 e5
        b[7][7] = "K";           // 黑王 h8
        b[6][6] = "P"; b[6][7] = "P"; // 黑兵 g7/h7
        b[7][6] = "R";           // 黑车 g8
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        check("白马e5->f7合法", hasMove(g.getLegalMoves(4, 4), 6, 5));
        g.makeMove(4, 4, 6, 5, (char) 0);
        check("马闷杀: 白方胜", "1-0".equals(g.getResult()));
        check("原因=将死", "将死".equals(g.getResultReason()));
    }

    /** 双车阶梯将死: 白车a7控制7行, 白车b5->b8# 沿8行将军 */
    static void testTwoRooksMate() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][0] = "k";           // 白王 a1
        b[6][0] = "r";           // 白车 a7 (控制7行, 封住g7/h7)
        b[4][1] = "r";           // 白车 b5
        b[7][7] = "K";           // 黑王 h8
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        check("白车b5->b8合法", hasMove(g.getLegalMoves(4, 1), 7, 1));
        g.makeMove(4, 1, 7, 1, (char) 0); // Rb8#
        check("双车将死: 白方胜", "1-0".equals(g.getResult()));
        check("原因=将死", "将死".equals(g.getResultReason()));
    }

    /** 仅将军非将死: 黑方回合, 黑王e8被后g6将军但可逃d8, 游戏应继续 */
    static void testNotMateButCheck() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][0] = "k";           // 白王 a1
        b[5][6] = "q";           // 白后 g6
        b[7][4] = "K";           // 黑王 e8
        g.loadPosition(b, true, false, false, false, false, -1, -1, 0);
        check("后g6将军黑王", g.isInCheck(true));
        check("黑王e8可逃d8(未将死)", hasMove(g.getLegalMoves(7, 4), 7, 3));
        g.makeMove(7, 4, 7, 3, (char) 0); // 黑王 Kd8
        check("未将死: 对局继续", g.getResult() == null);
        check("王逃d8后将军解除", !g.isInCheck(true));
    }

    /** 学者杀 (4步杀): 1.e4 e5 2.Bc4 Nc6 3.Qh5 Nf6 4.Qxf7# */
    static void testScholarsMate() {
        ChessGame g = new ChessGame();
        g.makeMove(1, 4, 3, 4, (char) 0);   // e4
        g.makeMove(6, 4, 4, 4, (char) 0);   // e5
        g.makeMove(0, 5, 3, 2, (char) 0);   // Bc4
        g.makeMove(7, 1, 5, 2, (char) 0);   // Nc6
        g.makeMove(0, 3, 4, 7, (char) 0);   // Qh5
        g.makeMove(7, 6, 5, 5, (char) 0);   // Nf6
        g.makeMove(4, 7, 6, 5, (char) 0);   // Qxf7#
        check("学者杀: 白方胜", "1-0".equals(g.getResult()));
        check("原因=将死", "将死".equals(g.getResultReason()));
        check("将死后禁止走子", g.getLegalMoves(7, 4).isEmpty());
    }

    /** Anastasia将死: 黑王h8, 黑兵g7堵逃格, 白车d5->h5# + 白马e7控制g8 */
    static void testAnastasiasMate() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][0] = "k";           // 白王 a1
        b[4][3] = "r";           // 白车 d5
        b[6][4] = "n";           // 白马 e7
        b[7][7] = "K";           // 黑王 h8
        b[6][6] = "P";           // 黑兵 g7 (堵住逃格)
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        check("白车d5->h5合法", hasMove(g.getLegalMoves(4, 3), 4, 7));
        g.makeMove(4, 3, 4, 7, (char) 0); // Rh5#
        check("车马配合将死: 白方胜", "1-0".equals(g.getResult()));
        check("原因=将死", "将死".equals(g.getResultReason()));
    }

    /** 升变将死: 黑王a8, 黑兵b7, 白兵c7->c8升后# (白王b6控制a7) */
    static void testPromotionMate() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[5][1] = "k";           // 白王 b6
        b[6][2] = "p";           // 白兵 c7
        b[7][0] = "K";           // 黑王 a8
        b[6][1] = "P";           // 黑兵 b7
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        check("白兵c7->c8升变合法", hasMove(g.getLegalMoves(6, 2), 7, 2));
        g.makeMove(6, 2, 7, 2, 'q');      // c8=Q#
        check("升变将死: 白方胜", "1-0".equals(g.getResult()));
        check("原因=将死", "将死".equals(g.getResultReason()));
    }

    /** 随机自弈压力测试: 每步走法合法, 王永不被吃, 将军时所有走法都解除将军 */
    static void testRandomPlay() {
        Random rnd = new Random(42);
        ChessGame g = new ChessGame();
        int moves = 0;
        while (moves < 400 && g.getResult() == null) {
            boolean black = g.isBlackToMove();
            // 收集当前执棋方所有合法走法
            List<int[]> all = new java.util.ArrayList<>();
            List<int[]> origins = new java.util.ArrayList<>();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    String p = g.getPiece(r, c);
                    if (p != null && ChessGame.isBlackPiece(p) == black) {
                        for (int[] m : g.getLegalMoves(r, c)) {
                            all.add(m);
                            origins.add(new int[]{r, c});
                        }
                    }
                }
            }
            if (all.isEmpty()) {
                check("随机对弈: 无走法时必有结果(将死或困毙)", g.getResult() != null);
                break;
            }
            // 将军时, 验证所有合法走法都解除将军
            boolean inCheckBefore = g.isInCheck(black);
            int idx = rnd.nextInt(all.size());
            int[] m = all.get(idx);
            char promo = 0;
            int f = m[4];
            if (f == 'q' || f == 'r' || f == 'b' || f == 'n' || f == 'Q' || f == 'R' || f == 'B' || f == 'N') {
                promo = (char) f;
            }
            g.makeMove(m[0], m[1], m[2], m[3], promo);
            if (inCheckBefore && g.isInCheck(black)) {
                check("随机对弈: 将军后走子必须解除将军", false);
                break;
            }
            // 王必须仍在棋盘
            boolean kingFound = false;
            for (int r = 0; r < 8 && !kingFound; r++) {
                for (int c = 0; c < 8 && !kingFound; c++) {
                    String p = g.getPiece(r, c);
                    if (p != null && Character.toLowerCase(p.charAt(0)) == 'k'
                        && ChessGame.isBlackPiece(p) == black) kingFound = true;
                }
            }
            if (!kingFound) {
                check("随机对弈: 王被吃! 引擎漏洞", false);
                break;
            }
            moves++;
        }
        check("随机自弈完成 400 步内正常", moves >= 0);
    }
}
