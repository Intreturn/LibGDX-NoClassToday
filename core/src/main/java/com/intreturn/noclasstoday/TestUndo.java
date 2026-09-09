package com.intreturn.noclasstoday;

import java.util.List;

/**
 * 悔棋 (undo) 与代数记谱 (SAN) 回归测试.
 * 运行: java -cp core\build\classes\java\main com.intreturn.noclasstoday.TestUndo
 */
public class TestUndo {
    static int passed = 0, failed = 0;

    static void check(String name, boolean cond) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    static String[][] empty() {
        String[][] b = new String[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) b[r][c] = null;
        }
        return b;
    }

    static boolean hasMove(List<int[]> moves, int tr, int tc) {
        for (int[] m : moves) {
            if (m[2] == tr && m[3] == tc) return true;
        }
        return false;
    }

    static String lastSan(ChessGame g) {
        List<String> h = g.getSanHistory();
        return h.isEmpty() ? null : h.get(h.size() - 1);
    }

    /** 普通走子悔棋. */
    static void testUndoSimpleMove() {
        ChessGame g = new ChessGame();
        g.makeMove(1, 4, 3, 4, (char) 0); // e4
        check("e4后兵在e4", "p".equals(g.getPiece(3, 4)));
        check("e4记谱", "e4".equals(lastSan(g)));
        check("走子后黑方回合", g.isBlackToMove());

        check("悔棋成功", g.undo());
        check("悔棋后e4空", g.getPiece(3, 4) == null);
        check("兵回e2", "p".equals(g.getPiece(1, 4)));
        check("回到白方回合", !g.isBlackToMove());
        check("SAN历史清空", g.getSanHistory().isEmpty());

        // 连续悔两步: e4 e5
        g.makeMove(1, 4, 3, 4, (char) 0); // e4
        g.makeMove(6, 4, 4, 4, (char) 0); // e5
        g.undo();
        check("悔一步后白兵仍在e4", "p".equals(g.getPiece(3, 4)));
        check("悔一步后黑兵回e7", "P".equals(g.getPiece(6, 4)));
        g.undo();
        check("悔两步后e4空", g.getPiece(3, 4) == null);
        check("无历史再悔返回false", !g.undo());
    }

    /** 吃子悔棋: 被吃子恢复. */
    static void testUndoCapture() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][0] = "r";       // 白车 a1
        b[5][0] = "P";       // 黑兵 a6
        b[0][7] = "k";       // 白王 h1
        b[7][7] = "K";       // 黑王 h8
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.makeMove(0, 0, 5, 0, (char) 0); // Rxa6
        check("车吃到a6", "r".equals(g.getPiece(5, 0)));
        check("吃子记谱Rxa6", "Rxa6".equals(lastSan(g)));
        g.undo();
        check("悔棋后车回a1", "r".equals(g.getPiece(0, 0)));
        check("被吃黑兵恢复a6", "P".equals(g.getPiece(5, 0)));
    }

    /** 易位悔棋: 车回原位 + 易位权限恢复. */
    static void testUndoCastling() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";       // 白王 e1
        b[0][7] = "r";       // 白车 h1
        b[7][4] = "K";       // 黑王 e8
        g.loadPosition(b, false, true, false, false, false, -1, -1, 0);
        g.makeMove(0, 4, 0, 6, (char) 0); // O-O
        check("易位后王在g1", "k".equals(g.getPiece(0, 6)));
        check("易位后车在f1", "r".equals(g.getPiece(0, 5)));
        check("易位记谱O-O", "O-O".equals(lastSan(g)));
        g.undo();
        check("悔棋后王回e1", "k".equals(g.getPiece(0, 4)));
        check("悔棋后车回h1", "r".equals(g.getPiece(0, 7)));
        check("悔棋后f1空", g.getPiece(0, 5) == null);
        check("易位权限恢复可再易位", hasMove(g.getLegalMoves(0, 4), 0, 6));
    }

    /** 吃过路兵悔棋: 被吃兵恢复. */
    static void testUndoEnPassant() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[4][4] = "p";       // 白兵 e5
        b[4][3] = "P";       // 黑兵 d5 (刚走两格)
        b[0][4] = "k";       // 白王 e1
        b[7][4] = "K";       // 黑王 e8
        g.loadPosition(b, false, false, false, false, false, 5, 3, 0); // ep=d6
        g.makeMove(4, 4, 5, 3, (char) 0); // exd6 过路
        check("过路后白兵在d6", "p".equals(g.getPiece(5, 3)));
        check("黑兵被移除", g.getPiece(4, 3) == null);
        check("过路记谱exd6", "exd6".equals(lastSan(g)));
        g.undo();
        check("悔棋后白兵回e5", "p".equals(g.getPiece(4, 4)));
        check("被吃黑兵恢复d5", "P".equals(g.getPiece(4, 3)));
        check("d6空", g.getPiece(5, 3) == null);
    }

    /** 升变悔棋: 恢复为兵. */
    static void testUndoPromotion() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[6][0] = "p";       // 白兵 a7
        b[0][4] = "k";
        b[7][4] = "K";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.makeMove(6, 0, 7, 0, 'q'); // a8=Q+ (升变后沿第8行将军e8王)
        check("升变后白后在a8", "q".equals(g.getPiece(7, 0)));
        check("升变记谱a8=Q+", "a8=Q+".equals(lastSan(g)));
        g.undo();
        check("悔棋后兵回a7", "p".equals(g.getPiece(6, 0)));
        check("a8空", g.getPiece(7, 0) == null);
    }

    /** 将死后悔棋: 结果清空可继续. */
    static void testUndoAfterMate() {
        ChessGame g = new ChessGame();
        // 学者杀: 1.e4 e5 2.Qh5 Nc6 3.Bc4 Nf6 4.Qxf7#
        g.makeMove(1, 4, 3, 4, (char) 0);    // e4
        g.makeMove(6, 4, 4, 4, (char) 0);    // e5
        g.makeMove(0, 3, 4, 7, (char) 0);    // Qh5 (f7兵挡斜线, 不将军)
        g.makeMove(7, 1, 5, 2, (char) 0);    // Nc6
        g.makeMove(0, 5, 3, 2, (char) 0);    // Bc4
        g.makeMove(7, 6, 5, 5, (char) 0);    // Nf6
        g.makeMove(4, 7, 6, 5, (char) 0);    // Qxf7#
        check("将死结果", "1-0".equals(g.getResult()));
        check("将死记谱Qxf7#", "Qxf7#".equals(lastSan(g)));
        check("Qh5记谱无将军符号", "Qh5".equals(g.getSanHistory().get(2)));

        check("将死后可悔棋", g.undo());
        check("悔棋后结果清空", g.getResult() == null);
        check("悔棋后黑王在e8", "K".equals(g.getPiece(7, 4)));
        check("悔棋后可继续走子", g.hasAnyLegalMove(false));
    }

    /** SAN 基础记谱: 马/吃子/升变. */
    static void testSanBasics() {
        ChessGame g = new ChessGame();
        g.makeMove(0, 6, 2, 5, (char) 0); // Nf3 (g1马)
        check("Nf3记谱", "Nf3".equals(lastSan(g)));
        g.makeMove(6, 3, 5, 3, (char) 0); // d6
        g.undo();
        g.makeMove(0, 1, 2, 2, (char) 0); // Nc3
        check("Nc3记谱", "Nc3".equals(lastSan(g)));
    }

    /** SAN 消歧: 两马同到一格加文件; 同文件加行号. */
    static void testSanAmbiguity() {
        // 马 b1 + f3 都能到 d2 -> Nbd2
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][1] = "n";       // 白马 b1
        b[2][5] = "n";       // 白马 f3
        b[0][4] = "k";
        b[7][4] = "K";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.makeMove(0, 1, 1, 3, (char) 0); // Nbd2
        check("文件消歧Nbd2", "Nbd2".equals(lastSan(g)));

        // 马 a3 + a5 同文件都能到 c4 -> N3c4
        ChessGame g2 = new ChessGame();
        String[][] b2 = empty();
        b2[2][0] = "n";      // 白马 a3
        b2[4][0] = "n";      // 白马 a5
        b2[0][4] = "k";
        b2[7][4] = "K";
        g2.loadPosition(b2, false, false, false, false, false, -1, -1, 0);
        g2.makeMove(2, 0, 3, 2, (char) 0); // N3c4
        check("行号消歧N3c4", "N3c4".equals(lastSan(g2)));
    }

    /** 悔棋后 FEN/局面历史一致性: 三次重复检测不因悔棋破坏. */
    static void testUndoKeepsRepetition() {
        ChessGame g = new ChessGame();
        // 马来回跳: Nf3 Ng8 Nf3? 用 Nf3-Ng8 循环
        g.makeMove(0, 6, 2, 5, (char) 0);  // 1.Nf3
        g.makeMove(7, 6, 5, 5, (char) 0);  // 1...Nf6
        g.makeMove(2, 5, 0, 6, (char) 0);  // 2.Ng1
        g.makeMove(5, 5, 7, 6, (char) 0);  // 2...Ng8
        g.undo();
        check("悔棋后马回f6", "N".equals(g.getPiece(5, 5)));
        g.makeMove(5, 5, 7, 6, (char) 0);  // 重走 2...Ng8
        // 局面回到初始, 应检测出两次出现 (未到三次)
        check("重复两次不判和", g.getResult() == null);
    }

    /** 走法历史坐标访问器: 步序/坐标/棋子/升变字符. */
    static void testMoveHistory() {
        ChessGame g = new ChessGame();
        check("初始历史为空", g.getMoveHistory().isEmpty());
        g.makeMove(1, 4, 3, 4, (char) 0); // e4
        List<int[]> h = g.getMoveHistory();
        check("一步后历史长度1", h.size() == 1);
        check("e4起点", h.get(0)[0] == 1 && h.get(0)[1] == 4);
        check("e4终点", h.get(0)[2] == 3 && h.get(0)[3] == 4);
        check("e4棋子为兵", h.get(0)[4] == 'p' && h.get(0)[5] == 0);
        g.makeMove(6, 4, 4, 4, (char) 0); // e5
        h = g.getMoveHistory();
        check("两步后历史长度2", h.size() == 2);
        check("步序正确: 第2步为黑兵", h.get(1)[4] == 'P');
        check("第2步坐标e7-e5", h.get(1)[0] == 6 && h.get(1)[1] == 4
            && h.get(1)[2] == 4 && h.get(1)[3] == 4);
        g.undo();
        check("悔棋后历史缩短", g.getMoveHistory().size() == 1);
    }

    /** reset 重开: 棋盘/历史/记谱/回合全部回初始. */
    static void testReset() {
        ChessGame g = new ChessGame();
        g.makeMove(1, 4, 3, 4, (char) 0); // e4
        g.makeMove(6, 4, 4, 4, (char) 0); // e5
        g.makeMove(0, 3, 4, 7, (char) 0); // Qh5
        g.reset();
        check("reset后e4空", g.getPiece(3, 4) == null);
        check("reset后e5空", g.getPiece(4, 4) == null);
        check("reset后白兵回e2", "p".equals(g.getPiece(1, 4)));
        check("reset后白后在d1", "q".equals(g.getPiece(0, 3)));
        check("reset后走法历史空", g.getMoveHistory().isEmpty());
        check("reset后SAN历史空", g.getSanHistory().isEmpty());
        check("reset后白方回合", !g.isBlackToMove());
        check("reset后无终局结果", g.getResult() == null);
        check("reset后可正常走子", g.getMoveCount() == 0);
    }

    public static void main(String[] args) {
        testUndoSimpleMove();
        testUndoCapture();
        testUndoCastling();
        testUndoEnPassant();
        testUndoPromotion();
        testUndoAfterMate();
        testSanBasics();
        testSanAmbiguity();
        testUndoKeepsRepetition();
        testMoveHistory();
        testReset();

        System.out.println("===== TestUndo: passed=" + passed + " failed=" + failed + " =====");
        if (failed > 0) System.exit(1);
    }
}
